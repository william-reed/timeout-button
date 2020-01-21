package dev.williamreed.timeoutbutton

import android.content.Context
import android.util.AttributeSet
import android.view.View
import androidx.constraintlayout.widget.ConstraintLayout
import androidx.constraintlayout.widget.ConstraintSet
import androidx.core.content.res.getColorOrThrow
import com.google.android.material.button.MaterialButton


/**
 * Timeout Button
 *
 * A button with a line that goes around the perimeter of the button signifying a timeout until a
 * certain action occurs
 */
class TimeoutButtonOld @JvmOverloads constructor(context: Context, attrs: AttributeSet? = null, defStyle: Int = 0) :
    ConstraintLayout(context, attrs, defStyle) {
    /** The backing button being used */
    val button: MaterialButton
    /** the view responsible for drawing the perimeter */
    val progressView: ProgressRectangleView

    init {
        val inflated = View.inflate(context, R.layout.timeout_button, this)
        button = inflated.findViewById(R.id.button)
        progressView = inflated.findViewById(R.id.progress)

        // I'm not sure what a better way would be to support all of the button attributes as is.
        // i think i could have programmatically created the constraints and then passed along attrs
        // to the button being created but since i am just inflating it here i'm not sure if i can
        // get attributes. maybe by making a custom inflater but im not doing that now.
        // i tried to programatically create the constraints but didn't spend enough time to figure
        // that out completely so this way was easier
        @Suppress("UNUSED_VARIABLE")
        @SuppressWarnings("ResourceType")
        val typedArray = context.obtainStyledAttributes(
            attrs,
            intArrayOf(
                android.R.attr.text,                        // 0
                com.google.android.material.R.attr.icon,    // 1
                com.google.android.material.R.attr.iconTint // 2
            )
        ).apply {
            getText(0)?.let { button.text = it }
            getDrawable(1)?.let { button.icon = it }
            getColorStateList(2)?.let { button.iconTint = it }
            recycle()
        }

        // get custom attributes
        context.obtainStyledAttributes(attrs, R.styleable.TimeoutButton, 0, 0).apply {
            progressView.color = getColorOrThrow(R.styleable.TimeoutButton_progressColor)
            recycle()
        }

        // change some settings to get button shadows properly
        setPaddingDp(6, 6, 6, 6)
        clipToPadding = false

        val set = ConstraintSet().apply {
        }
    }

    /**
     * Start the button timeout animation
     */
    fun start() = progressView.start()
}