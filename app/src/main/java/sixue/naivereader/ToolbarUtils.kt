package sixue.naivereader

import android.text.SpannableString
import android.text.style.ForegroundColorSpan
import android.util.TypedValue
import android.view.Menu
import android.view.View
import android.view.ViewGroup
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.widget.Toolbar
import androidx.core.graphics.ColorUtils
import androidx.core.view.ViewCompat
import androidx.core.view.WindowCompat
import androidx.core.view.WindowInsetsCompat
import androidx.core.view.WindowInsetsControllerCompat
import androidx.core.view.get
import androidx.core.view.size
import androidx.core.view.updateLayoutParams

fun toolbarUtils(activity: AppCompatActivity, rootId: Int, toolbarId: Int, action: (insets: WindowInsetsCompat) -> Unit) {

    val root = activity.findViewById<View>(rootId)
    val toolbar = activity.findViewById<Toolbar>(toolbarId)

    activity.setSupportActionBar(toolbar)

    WindowCompat.setDecorFitsSystemWindows(activity.window, false)
    WindowCompat.getInsetsController(activity.window, activity.window.decorView).apply {
        isAppearanceLightStatusBars = false

        systemBarsBehavior = WindowInsetsControllerCompat.BEHAVIOR_SHOW_TRANSIENT_BARS_BY_SWIPE
    }

    ViewCompat.setOnApplyWindowInsetsListener(root) { _, insets ->
        val statusBarHeight = insets.getInsets(WindowInsetsCompat.Type.statusBars()).top

        val typedValue = TypedValue()
        activity.theme.resolveAttribute(android.R.attr.actionBarSize, typedValue, true)
        val originalToolbarHeight = TypedValue.complexToDimensionPixelSize(
            typedValue.data,
            activity.resources.displayMetrics
        )

        toolbar.updateLayoutParams < ViewGroup . MarginLayoutParams > {
            topMargin = 0
            height = originalToolbarHeight + statusBarHeight
        }

        toolbar.setPadding(0, statusBarHeight, 0, 0)

        action(insets)

        insets
    }

}

fun setMenuText(menu: Menu, color: Int) {
    for (i in 0 until menu.size) {
        val item = menu[i]

        // 如果是文字菜单（rare），设置 SpannableString
        val title = SpannableString(item.title)
        title.setSpan(ForegroundColorSpan(color), 0, title.length, 0)
        item.title = title

        // 如果是图标菜单，给图标着色
        item.icon?.mutate()?.setTint(color)
    }
}

fun colorWithAlpha(activity: AppCompatActivity, resId: Int, alpha: Int): Int {
    val typedValue = TypedValue()
    activity.theme.resolveAttribute(resId, typedValue, true)

    return ColorUtils.setAlphaComponent(typedValue.data, 0x99)
}