package sixue.naviereader.provider;

import android.content.Context;
import android.text.TextUtils;

import java.io.File;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import sixue.naviereader.Utils;

public class NetProviderCollections {
    private static Map<String, NetProvider> providers;

    public static Collection<NetProvider> getProviders() {
        return getProviders(null);
    }

    public static Collection<NetProvider> getProviders(Context context) {
        initProviders();
        if (context == null) {
            return providers.values();
        } else {
            loadSettings(context);
            List<NetProvider> activeProviders = new ArrayList<>();
            for (NetProvider netProvider : providers.values()) {
                if (netProvider.isActive()) {
                    activeProviders.add(netProvider);
                }
            }
            return activeProviders;
        }
    }

    private static void loadSettings(Context context) {
        File saveRoot = context.getExternalFilesDir("books");
        if (saveRoot == null) {
            return;
        }

        String disabledProviders = Utils.readText(saveRoot.getAbsolutePath() + "/.PROVIDERS");
        if (disabledProviders == null) {
            return;
        }

        for (String disabledProviderId : disabledProviders.split("::")) {
            NetProvider netProvider = providers.get(disabledProviderId);
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
        String s = TextUtils.join("::", list);

        File saveRoot = context.getExternalFilesDir("books");
        if (saveRoot == null) {
            return;
        }

        Utils.writeText(s, saveRoot.getAbsolutePath() + "/.PROVIDERS");
    }

    private static void initProviders() {
        if (providers == null) {
            providers = new HashMap<>();
            addProvider(new WlzwProvider());
            addProvider(new PtwxProvider());
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
