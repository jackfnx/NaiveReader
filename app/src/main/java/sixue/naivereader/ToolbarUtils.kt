package sixue.naivereader

import android.util.TypedValue
import android.view.View
import android.view.ViewGroup
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.widget.Toolbar
import androidx.core.view.ViewCompat
import androidx.core.view.WindowCompat
import androidx.core.view.WindowInsetsCompat
import androidx.core.view.WindowInsetsControllerCompat
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

        toolbar.updateLayoutParams<ViewGroup.MarginLayoutParams> {
            topMargin = 0
            height = originalToolbarHeight + statusBarHeight
        }

        toolbar.setPadding(0, statusBarHeight, 0, 0)

        action(insets)

        insets
    }

}