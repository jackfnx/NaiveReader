package sixue.naivereader

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.*
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.widget.SwitchCompat
import sixue.naivereader.provider.NetProvider
import sixue.naivereader.provider.NetProviderCollections.getProviders
import sixue.naivereader.provider.NetProviderCollections.saveSettings
import java.util.*

class NetProviderManagerActivity : AppCompatActivity() {
    private lateinit var netProviders: List<NetProvider>
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_net_provider_manager)
        netProviders = ArrayList(getProviders(this))
        val listView = findViewById<ListView>(R.id.list_net_providers)
        listView.adapter = object : BaseAdapter() {
            override fun getCount(): Int {
                return netProviders.size
            }

            override fun getItem(i: Int): Any {
                return ""
            }

            override fun getItemId(i: Int): Long {
                return 0
            }

            override fun getView(i: Int, convertView: View?, viewGroup: ViewGroup): View {
                val view : View?
                if (convertView == null){
                    view = LayoutInflater.from(this@NetProviderManagerActivity).inflate(R.layout.listviewitem_provider, viewGroup, false)
                    val name = view.findViewById<TextView>(R.id.name)
                    val sw = view.findViewById<SwitchCompat>(R.id.sw)
                    view.tag = ViewHolder(name, sw)
                } else {
                    view = convertView
                }
                val viewHolder: ViewHolder = view!!.tag as ViewHolder
                val netProvider = netProviders[i]
                viewHolder.name.text = netProvider.providerName
                viewHolder.sw.isChecked = netProvider.isActive
                viewHolder.sw.setOnClickListener {
                    netProvider.isActive = viewHolder.sw.isChecked
                    saveSettings(this@NetProviderManagerActivity)
                }
                viewHolder.sw.isChecked = netProvider.isActive
                return view
            }

            inner class ViewHolder(val name: TextView, val sw: SwitchCompat)
        }
        val cg = findViewById<Button>(R.id.garbage_button)
        cg.setOnClickListener {
            val n = BookLoader.clearGarbage {
                Toast.makeText(this, "[GARBAGE] $it", Toast.LENGTH_SHORT).show()
            }
            Toast.makeText(this@NetProviderManagerActivity, "[GARBAGE] Clear <$n>.", Toast.LENGTH_SHORT)
                .show()
        }
    }
}