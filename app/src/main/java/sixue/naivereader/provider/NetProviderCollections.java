package sixue.naivereader.provider;

import android.content.Context;
import android.text.TextUtils;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import sixue.naivereader.Utils;

public class NetProviderCollections {
    private static Map<String, NetProvider> providers;

    public static Collection<NetProvider> getProviders(Context context) {
        initProviders();
        loadSettings(context);
        return providers.values();
    }

    private static void loadSettings(Context context) {
        String saveRootPath = Utils.getSavePathRoot(context);
        String disabledProviders = Utils.readText(saveRootPath + "/.PROVIDERS");
        if (disabledProviders == null) {
            return;
        }

        for (String disabledProviderId : disabledProviders.split("\n")) {
            NetProvider netProvider = providers.get(disabledProviderId.trim());
            if (netProvider != null) {
                netProvider.setActive(false);
            }
        }
    }

    public static void saveSettings(Context context) {
        List<String> list = new ArrayList<>();
        for (NetProvider netProvider : providers.values()) {
            if (!netProvider.isActive()) {
                list.add(netProvider.getProviderId());
            }
        }
        String s = TextUtils.join("\n", list);

        String saveRootPath = Utils.getSavePathRoot(context);
        Utils.writeText(s, saveRootPath + "/.PROVIDERS");
    }

    private static void initProviders() {
        if (providers == null) {
            providers = new HashMap<>();
            addProvider(new WlzwProvider());
            addProvider(new PtwxProvider());
            addProvider(new FpzwProvider());
            addProvider(new XbqgProvider());
        }
    }

    private static void addProvider(NetProvider netProvider) {
        providers.put(netProvider.getProviderId(), netProvider);
    }

    public static NetProvider findProviders(String providerId) {
        return findProviders(providerId, null);
    }

    public static NetProvider findProviders(String providerId, Context context) {
        initProviders();
        if (context == null) {
            return providers.get(providerId);
        } else {
            loadSettings(context);
            NetProvider netProvider = providers.get(providerId);
            if (netProvider != null && netProvider.isActive()) {
                return netProvider;
            } else {
                return null;
            }
        }
    }
}
