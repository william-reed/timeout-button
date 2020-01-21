package dev.williamreed.timeoutbutton

import android.content.Context
import android.util.AttributeSet

/**
 * A [TimeoutWrapper] except it has default padding / settings for material buttons
 */
class MaterialTimeoutWrapper @JvmOverloads constructor(context: Context, attrs: AttributeSet? = null, defStyle: Int = 0) :
    TimeoutWrapper(context, attrs, defStyle) {

    init {
        // these settings seem to be best to 'cover' a material button. might change with updates.
        progressPadBottom = AndroidUtils.dpToPixels(5.8F)
        progressPadTop = AndroidUtils.dpToPixels(5.8F)

        progressRadius = AndroidUtils.dpToPixels(3F)
        progressStrokeWidth = AndroidUtils.dpToPixels(3F)

        // padding required to show the full shadow
        setPaddingDp(6, 6, 6, 6)
    }
}
