package gr.ntg.keycloak.dataaccess.repositories;

import gr.ntg.keycloak.dataaccess.entities.RateLimit;
import jakarta.persistence.EntityManager;

public class RateLimitRepository {
    private final EntityManager entityManager;

    private static final String UPSERT_POSTGRESQL =
            "WITH upsert AS ( " +
                    "  INSERT INTO rate_limits " +
                    "    (id, sec_window_start, sec_counter, min_window_start, min_counter) " +
                    "  VALUES (?1, now(), 1, now(), 1) " +
                    "  ON CONFLICT (id) DO UPDATE SET " +
                    "    sec_window_start = CASE " +
                    "      WHEN rate_limits.sec_window_start < now() - interval '1 second' THEN now() " +
                    "      ELSE rate_limits.sec_window_start " +
                    "    END, " +
                    "    sec_counter = CASE " +
                    "      WHEN rate_limits.sec_window_start < now() - interval '1 second' THEN 1 " +
                    "      ELSE rate_limits.sec_counter + 1 " +
                    "    END, " +
                    "    min_window_start = CASE " +
                    "      WHEN rate_limits.min_window_start < now() - interval '1 minute' THEN now() " +
                    "      ELSE rate_limits.min_window_start " +
                    "    END, " +
                    "    min_counter = CASE " +
                    "      WHEN rate_limits.min_window_start < now() - interval '1 minute' THEN 1 " +
                    "      ELSE rate_limits.min_counter + 1 " +
                    "    END " +
                    "  RETURNING sec_counter, min_counter " +
                    ") " +
                    "SELECT sec_counter, min_counter FROM upsert;";

    private static final String PRUNE_POSTGRESQL =
            "DELETE FROM rate_limits " +
                    "WHERE sec_window_start < now() - interval '2 seconds' " +
                    "   AND min_window_start < now() - interval '2 minutes';";

    private static final String UPSERT_MYSQL =
            "INSERT INTO rate_limits" +
                    "  (id, sec_window_start, sec_counter, min_window_start, min_counter) " +
                    "VALUES " +
                    "  (?1, NOW(), 1, NOW(), 1) " +
                    "ON DUPLICATE KEY UPDATE " +
                    "  sec_counter      = IF(sec_window_start < NOW() - INTERVAL 1 SECOND, " +
                    "                                     1, " +
                    "                                     sec_counter + 1), " +
                    "  sec_window_start = IF(sec_window_start < NOW() - INTERVAL 1 SECOND, " +
                    "                        NOW(), " +
                    "                        sec_window_start), " +
                    "  min_counter      = IF(min_window_start < NOW() - INTERVAL 1 MINUTE, " +
                    "                                     1, " +
                    "                                     min_counter + 1), " +
                    "  min_window_start = IF(min_window_start < NOW() - INTERVAL 1 MINUTE, " +
                    "                        NOW(), " +
                    "                        min_window_start); ";
    private static final String SELECT_MYSQL =
                    "SELECT sec_counter, min_counter " +
                    "  FROM rate_limits " +
                    " WHERE id = ?1;";

    private static final String PRUNE_MYSQL =
            "DELETE FROM rate_limits " +
                    "WHERE sec_window_start < NOW() - INTERVAL 2 SECOND " +
                    "   AND min_window_start < NOW() - INTERVAL 2 MINUTE";

    private static final String UPSERT_MSSQL =
            "MERGE rate_limits AS target " +
                    "USING (VALUES (?1)) AS src(id) " +
                    "  ON target.id = src.id " +
                    "WHEN NOT MATCHED THEN " +
                    "  INSERT (id, sec_window_start, sec_counter, min_window_start, min_counter) " +
                    "  VALUES (src.id, SYSUTCDATETIME(), 1, SYSUTCDATETIME(), 1) " +
                    "WHEN MATCHED THEN " +
                    "  UPDATE SET " +
                    "    sec_window_start = CASE " +
                    "      WHEN target.sec_window_start < DATEADD(SECOND, -1, SYSUTCDATETIME()) THEN SYSUTCDATETIME() " +
                    "      ELSE target.sec_window_start END, " +
                    "    sec_counter = CASE " +
                    "      WHEN target.sec_window_start < DATEADD(SECOND, -1, SYSUTCDATETIME()) THEN 1 " +
                    "      ELSE target.sec_counter + 1 END, " +
                    "    min_window_start = CASE " +
                    "      WHEN target.min_window_start < DATEADD(MINUTE, -1, SYSUTCDATETIME()) THEN SYSUTCDATETIME() " +
                    "      ELSE target.min_window_start END, " +
                    "    min_counter = CASE " +
                    "      WHEN target.min_window_start < DATEADD(MINUTE, -1, SYSUTCDATETIME()) THEN 1 " +
                    "      ELSE target.min_counter + 1 END " +
                    "OUTPUT " +
                    "  inserted.sec_counter, inserted.min_counter;";

    private static final String PRUNE_MSSQL =
            "  DELETE FROM rate_limits " +
                    " WHERE sec_window_start < DATEADD(SECOND, -2, SYSUTCDATETIME()) " +
                    "    AND min_window_start < DATEADD(MINUTE, -2, SYSUTCDATETIME());";

    public RateLimitRepository(EntityManager entityManager) {
        this.entityManager = entityManager;
    }

    public RateLimit upsertAndGet(String id, String dbProductName) {
        if ("postgresql".equalsIgnoreCase(dbProductName)) {
            return upsertAndGetInternal(id, UPSERT_POSTGRESQL);
        }
        else if ("mysql".equalsIgnoreCase(dbProductName)) {
            return upsertAndGetInternal(id, UPSERT_MYSQL, SELECT_MYSQL);
        }
        else if ("microsoft sql server".equalsIgnoreCase(dbProductName)) {
            return upsertAndGetInternal(id, UPSERT_MSSQL);
        }

        throw new RuntimeException("Detected db " + dbProductName + " but currently only PostgreSQL, Microsoft SQL Server and MySQL are supported.");
    }

    private RateLimit upsertAndGetInternal(String id, String sql) {
        Object[] result = (Object[]) entityManager.createNativeQuery(sql)
                .setParameter(1, id)
                .getSingleResult();

        int secCount = ((Number) result[0]).intValue();
        int minCount = ((Number) result[1]).intValue();

        var wl = new RateLimit();
        wl.setId(id);
        wl.setSecCounter(secCount);
        wl.setMinCounter(minCount);

        return wl;
    }

    private RateLimit upsertAndGetInternal(String id, String upsertSql, String selectSql) {
        entityManager.createNativeQuery(upsertSql)
                .setParameter(1, id)
                .executeUpdate();

        Object[] result = (Object[]) entityManager.createNativeQuery(selectSql)
                .setParameter(1, id)
                .getSingleResult();

        int secCount = ((Number) result[0]).intValue();
        int minCount = ((Number) result[1]).intValue();

        var wl = new RateLimit();
        wl.setId(id);
        wl.setSecCounter(secCount);
        wl.setMinCounter(minCount);

        return wl;
    }

    public void prune(String dbProductName) {
        if ("postgresql".equalsIgnoreCase(dbProductName)) {
            prune(PRUNE_POSTGRESQL);
        }
        else if ("mysql".equalsIgnoreCase(dbProductName)) {
            prune(PRUNE_MYSQL);
        }
        else if ("microsoft sql server".equalsIgnoreCase(dbProductName)) {
            prune(PRUNE_MSSQL);
        }

        throw new RuntimeException("Detected db " + dbProductName + " but currently only PostgreSQL, Microsoft SQL Server and MySQL are supported.");
    }

    private void pruneInternal(String sql) {
        entityManager.createNativeQuery(PRUNE_POSTGRESQL)
                .executeUpdate();
    }
}
