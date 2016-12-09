package sixue.naviereader.provider;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Enumeration;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class NetProviderCollections {
    private static Map<String, NetProvider> providers;

    public static Collection<NetProvider> getProviders() {
        initProviders();
        return providers.values();
    }

    private static void initProviders() {
        if (providers == null) {
            providers = new HashMap<>();
            addProvider(new WlzwProvider());
            addProvider(new PtwxProvider());
        }
    }

    public static NetProvider findProviders(String providerId) {
        initProviders();
        return providers.get(providerId);
    }

    private static void addProvider(NetProvider netProvider) {
        providers.put(netProvider.getProviderId(), netProvider);
    }
}
