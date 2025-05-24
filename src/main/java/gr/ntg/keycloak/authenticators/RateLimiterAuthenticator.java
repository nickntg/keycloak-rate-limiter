package gr.ntg.keycloak.authenticators;

import gr.ntg.keycloak.Constants;
import gr.ntg.keycloak.Utils;
import gr.ntg.keycloak.dataaccess.repositories.RateLimitRepository;
import gr.ntg.keycloak.models.ErrorResponse;
import jakarta.ws.rs.core.Response;
import org.keycloak.authentication.AuthenticationFlowContext;
import org.keycloak.authentication.AuthenticationFlowError;
import org.keycloak.authentication.Authenticator;
import org.keycloak.connections.jpa.JpaConnectionProvider;
import org.keycloak.models.KeycloakSession;
import org.keycloak.models.RealmModel;
import org.keycloak.models.UserModel;

public class RateLimiterAuthenticator implements Authenticator {

    @Override
    public void authenticate(AuthenticationFlowContext context) {
        var config = context.getAuthenticatorConfig();
        var user = context.getUser();

        var rateLimiterRepository = new RateLimitRepository(context
                .getSession()
                .getProvider(JpaConnectionProvider.class)
                .getEntityManager());

        var maxPerSecond = config != null && config.getConfig() != null
                ? Utils.str(config, Constants.CONFIG_TOKEN_LIMIT_PER_SEC)
                : RateLimiterAuthenticatorFactory.DEFAULT_TOKENS_PER_SECOND;
        var intMaxPerSecond = Integer.parseInt(maxPerSecond);

        var useClientName = config == null || config.getConfig() == null || Utils.bool(config, Constants.CONFIG_TOKEN_LIMIT_USE_CLIENT_NAME);

        var maxPerMinute = config != null && config.getConfig() != null
                ? Utils.str(config, Constants.CONFIG_TOKEN_LIMIT_PER_MIN)
                : RateLimiterAuthenticatorFactory.DEFAULT_TOKENS_PER_MINUTE;
        var intMaxPerMinute = Integer.parseInt(maxPerMinute);

        var key = user.getEmail().toLowerCase();

        if (useClientName) {
            var clientName = context.getAuthenticationSession().getClient().getClientId();
            key = key + "_" + clientName;
        }

        var wl = rateLimiterRepository.upsertAndGet(key);

        if (wl.getSecCounter() > intMaxPerSecond) {
            ConstructErrorResponse(context, maxPerSecond, "second");
            return;
        }

        if (wl.getMinCounter() > intMaxPerMinute) {
            ConstructErrorResponse(context, maxPerMinute, "minute");
            return;
        }

        context.success();
    }

    @Override
    public void action(AuthenticationFlowContext authenticationFlowContext) {
    }

    @Override
    public boolean requiresUser() {
        return true;
    }

    @Override
    public boolean configuredFor(KeycloakSession keycloakSession, RealmModel realmModel, UserModel userModel) {
        return true;
    }

    @Override
    public void setRequiredActions(KeycloakSession keycloakSession, RealmModel realmModel, UserModel userModel) {
    }

    @Override
    public void close() {
    }

    private void ConstructErrorResponse(AuthenticationFlowContext context, String maxPer, String maxPerError) {
        var rsp = new ErrorResponse();
        rsp.error = "invalid_request";
        rsp.error_description = "Rate limit exceeded: max " + maxPer + " token requests per " + maxPerError + ".";
        context.failureChallenge(
                AuthenticationFlowError.INVALID_CLIENT_SESSION,
                Response.status(429)
                        .entity(rsp)
                        .build());
    }
}
