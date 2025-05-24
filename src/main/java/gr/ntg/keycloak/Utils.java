package gr.ntg.keycloak;

import org.keycloak.models.AuthenticatorConfigModel;

public class Utils {
    public static boolean bool(AuthenticatorConfigModel config, String key) {
        return Boolean.parseBoolean(config.getConfig().get(key));
    }

    public static String str(AuthenticatorConfigModel config, String key) {
        return config.getConfig().get(key);
    }
}
