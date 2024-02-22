package sixue.naivereader.provider

import android.content.Context
import android.text.TextUtils
import sixue.naivereader.Utils
import java.util.*

object NetProviderCollections {
    private lateinit var providers: MutableMap<String, NetProvider>
    @JvmStatic
    fun getProviders(context: Context): Collection<NetProvider> {
        initProviders()
        loadSettings(context)
        return providers.values
    }

    private fun loadSettings(context: Context) {
        val saveRootPath = Utils.getSavePathRoot(context)
        val disabledProviders = Utils.readText("$saveRootPath/.PROVIDERS") ?: return
        disabledProviders.split("\n".toRegex()).toTypedArray().forEach { disabledProviderId ->
            val netProvider = providers[disabledProviderId.trim { it <= ' ' }]
            if (netProvider != null) {
                netProvider.isActive = false
            }
        }
    }

    @JvmStatic
    fun saveSettings(context: Context) {
        val list: MutableList<String> = ArrayList()
        for (netProvider in providers.values) {
            if (!netProvider.isActive) {
                list.add(netProvider.providerId)
            }
        }
        val s = TextUtils.join("\n", list)
        val saveRootPath = Utils.getSavePathRoot(context)
        Utils.writeText(s, "$saveRootPath/.PROVIDERS")
    }

    private fun initProviders() {
        if (!this::providers.isInitialized) {
            providers = HashMap()
            addProvider(WlzwProvider())
            addProvider(PtwxProvider())
            addProvider(FpzwProvider())
            addProvider(XbqgProvider())
            addProvider(QbxsProvider())
            addProvider(HtshuProvider())
        }
    }

    private fun addProvider(netProvider: NetProvider) {
        providers[netProvider.providerId] = netProvider
    }

    @JvmStatic
    @JvmOverloads
    fun findProviders(providerId: String, context: Context? = null): NetProvider? {
        initProviders()
        return if (context == null) {
            providers[providerId]
        } else {
            loadSettings(context = context)
            val netProvider = providers[providerId]
            if (netProvider?.isActive == true) {
                netProvider
            } else {
                null
            }
        }
    }
}