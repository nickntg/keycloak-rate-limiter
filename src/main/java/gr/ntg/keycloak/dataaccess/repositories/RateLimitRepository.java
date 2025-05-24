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

    public RateLimitRepository(EntityManager entityManager) {
        this.entityManager = entityManager;
    }

    public RateLimit upsertAndGet(String id) {
        Object[] result = (Object[]) entityManager.createNativeQuery(UPSERT_POSTGRESQL)
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

    public void prune() {
        entityManager.createNativeQuery(PRUNE_POSTGRESQL)
                .executeUpdate();
    }
}
