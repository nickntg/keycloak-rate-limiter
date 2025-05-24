package gr.ntg.keycloak.authenticators;

import gr.ntg.keycloak.Constants;
import org.keycloak.Config;
import org.keycloak.authentication.Authenticator;
import org.keycloak.authentication.AuthenticatorFactory;
import org.keycloak.models.AuthenticationExecutionModel;
import org.keycloak.models.KeycloakSession;
import org.keycloak.models.KeycloakSessionFactory;
import org.keycloak.provider.ProviderConfigProperty;

import java.util.Arrays;
import java.util.List;

public class RateLimiterAuthenticatorFactory implements AuthenticatorFactory {
    public static final String PROVIDER_ID = "rate-limiter-authenticator";
    public static final String DEFAULT_TOKENS_PER_SECOND = "1";
    public static final String DEFAULT_TOKENS_PER_MINUTE = "3";
    private static final RateLimiterAuthenticator SINGLETON = new RateLimiterAuthenticator();

    @Override
    public String getId() {
        return PROVIDER_ID;
    }

    @Override
    public String getDisplayType() {
        return "Rate Limiter Authenticator";
    }

    @Override
    public String getReferenceCategory() {
        return null;
    }

    @Override
    public boolean isConfigurable() {
        return true;
    }

    @Override
    public AuthenticationExecutionModel.Requirement[] getRequirementChoices() {
        return new AuthenticationExecutionModel.Requirement[]{
                AuthenticationExecutionModel.Requirement.DISABLED,
                AuthenticationExecutionModel.Requirement.REQUIRED
        };
    }

    @Override
    public boolean isUserSetupAllowed() {
        return false;
    }

    @Override
    public String getHelpText() {
        return "Forbids the creation of tokens after certain limits.";
    }

    @Override
    public List<ProviderConfigProperty> getConfigProperties() {
        return Arrays.asList(
                new ProviderConfigProperty(Constants.CONFIG_TOKEN_LIMIT_PER_SEC, "Tokens per second", "Number of tokens allowed per second",
                        ProviderConfigProperty.STRING_TYPE, DEFAULT_TOKENS_PER_SECOND),
                new ProviderConfigProperty(Constants.CONFIG_TOKEN_LIMIT_PER_MIN, "Tokens per minute", "Number of tokens allowed per minute",
                        ProviderConfigProperty.STRING_TYPE, DEFAULT_TOKENS_PER_MINUTE),
                new ProviderConfigProperty(Constants.CONFIG_TOKEN_LIMIT_USE_CLIENT_NAME, "Use the client name when constructing the window key", "Setting this to false results in a global token rate limiting per email",
                        ProviderConfigProperty.BOOLEAN_TYPE, true));
    }

    @Override
    public void init(Config.Scope scope) {

    }

    @Override
    public void postInit(KeycloakSessionFactory keycloakSessionFactory) {

    }

    @Override
    public void close() {

    }

    @Override
    public Authenticator create(KeycloakSession session) {
        return SINGLETON;
    }
}