package dev.williamreed.timeoutbutton

import android.content.res.Resources
import android.util.TypedValue
import android.view.View

private var density: Float? = null
fun View.setPaddingDp(left: Int, top: Int, right: Int, bottom: Int) {
    var density = density
    if (density == null)
        density = context!!.resources.displayMetrics.density
    setPadding((left * density).toInt(), (top * density).toInt(), (right * density).toInt(), (bottom * density).toInt())
}

/**
 * Android Utils
 *
 * Static util methods that I don't want to pollute the namespace with.
 */
object AndroidUtils {
    private val metrics = Resources.getSystem().displayMetrics
    /**
     * Convert the arbitrary unit into a pixel based unit
     *
     * @param unit: likely to be one of TypedValue.COMPLEX_UNIT_*
     */
    fun complexToPixels(unit: Int, size: Float): Float {
        return TypedValue.applyDimension(unit, size, metrics)
    }

    /**
     * Convert from dp to pixels
     */
    fun dpToPixels(size: Float) = complexToPixels(TypedValue.COMPLEX_UNIT_DIP, size)
}
