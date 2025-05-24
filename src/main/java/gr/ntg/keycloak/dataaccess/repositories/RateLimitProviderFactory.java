package gr.ntg.keycloak.dataaccess.repositories;

import org.keycloak.Config;
import org.keycloak.connections.jpa.entityprovider.JpaEntityProvider;
import org.keycloak.connections.jpa.entityprovider.JpaEntityProviderFactory;
import org.keycloak.models.KeycloakSession;
import org.keycloak.models.KeycloakSessionFactory;

public class RateLimitProviderFactory implements JpaEntityProviderFactory {
    static final String ID = "rate-limit-provider-factory-id";

    @Override
    public JpaEntityProvider create(KeycloakSession session) {
        return new RateLimitProvider();
    }

    @Override
    public void init(Config.Scope config) {
    }

    @Override
    public void postInit(KeycloakSessionFactory factory) {
    }

    @Override
    public void close() {
    }

    @Override
    public String getId() {
        return ID;
    }
}
