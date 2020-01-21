package dev.williamreed.timeoutbutton

import android.content.Context
import android.util.AttributeSet

/**
 * A [TimeoutButton] except it has default padding / settings for material buttons
 */
class MaterialTimeoutButton @JvmOverloads constructor(context: Context, attrs: AttributeSet? = null, defStyle: Int = 0) :
    TimeoutButton(context, attrs, defStyle) {

    init {
        progressPadBottom = AndroidUtils.dpToPixels(5.8F)
        progressPadTop = AndroidUtils.dpToPixels(5.8F)

        progressRadius = AndroidUtils.dpToPixels(3F)
        progressStrokeWidth = AndroidUtils.dpToPixels(3F)
    }
}