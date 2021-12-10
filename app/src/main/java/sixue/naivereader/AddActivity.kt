package sixue.naivereader

import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import androidx.fragment.app.Fragment
import androidx.fragment.app.FragmentManager
import androidx.fragment.app.FragmentPagerAdapter
import androidx.viewpager.widget.ViewPager
import com.google.android.material.tabs.TabLayout

class AddActivity : AppCompatActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_add)
        val myAdapter = MyFragmentPagerAdapter(supportFragmentManager)
        val viewPager = findViewById<ViewPager>(R.id.viewpager)
        viewPager.adapter = myAdapter
        val tabLayout = findViewById<TabLayout>(R.id.tabs)
        tabLayout.setupWithViewPager(viewPager)
        tabLayout.tabMode = TabLayout.MODE_SCROLLABLE
    }

    private inner class MyFragmentPagerAdapter(fm: FragmentManager?) : FragmentPagerAdapter(fm!!) {
        override fun getItem(position: Int): Fragment {
            return when (position) {
                0 -> {
                    AddNetBookFragment()
                }
                1 -> {
                    AddLocalBookFragment()
                }
                else -> {
                    AddPacketFragment()
                }
            }
        }

        override fun getCount(): Int {
            return 3
        }

        override fun getPageTitle(position: Int): CharSequence {
            return when (position) {
                0 -> {
                    getString(R.string.tab_add_net_book)
                }
                1 -> {
                    getString(R.string.tab_import_txt_flat)
                }
                else -> {
                    getString(R.string.tab_import_packet)
                }
            }
        }
    }
}