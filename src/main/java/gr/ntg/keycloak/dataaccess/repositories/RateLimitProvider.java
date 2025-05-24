package gr.ntg.keycloak.dataaccess.repositories;

import gr.ntg.keycloak.dataaccess.entities.RateLimit;
import org.keycloak.connections.jpa.entityprovider.JpaEntityProvider;

import java.util.Collections;
import java.util.List;

public class RateLimitProvider implements JpaEntityProvider {
    @Override
    public List<Class<?>> getEntities() {
        return Collections.singletonList(RateLimit.class);
    }

    @Override
    public String getChangelogLocation() {
        return "META-INF/RateLimitChangelog.xml";
    }

    @Override
    public String getFactoryId() {
        return RateLimitProviderFactory.ID;
    }

    @Override
    public void close() {
    }
}