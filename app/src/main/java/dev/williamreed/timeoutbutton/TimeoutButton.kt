package dev.williamreed.timeoutbutton

import android.animation.ObjectAnimator
import android.content.Context
import android.graphics.*
import android.util.AttributeSet
import android.view.animation.DecelerateInterpolator
import android.widget.Button
import android.widget.FrameLayout
import androidx.annotation.ColorInt
import androidx.core.content.ContextCompat
import androidx.core.view.children

/**
 * Timeout Button
 *
 * Expects a button as a child view
 */
open class TimeoutButton @JvmOverloads constructor(context: Context, attrs: AttributeSet? = null, defStyle: Int = 0) :
    FrameLayout(context, attrs, defStyle) {
    /** the current path being used */
    private var path: Path? = null
    private var paint: Paint
    private var length = 0f

    /** has the view received its size yet? can't start with size*/
    private var sized = false
    /** has the view been started yet? can't start with size */
    private var started = false
    private var animator: ObjectAnimator

    /** if we have a button as a child keep it here */
    private var buttonChild: Button? = null

    /** padding in pixels */
    var progressPadTop: Float
    /** padding in pixels */
    var progressPadLeft: Float
    /** padding in pixels */
    var progressPadRight: Float
    /** padding in pixels */
    var progressPadBottom: Float

    /** radius in pixels */
    var progressRadius: Float
    /** duration in ms */
    var animationDurationMs: Int
    var decelerateFactor: Float
    /** stroke in pixels */
    var progressStrokeWidth: Float

    init {
        // change some settings to get button shadows properly
        clipToPadding = false

        // get attributes
        val paintColor: Int
        context.obtainStyledAttributes(attrs, R.styleable.TimeoutButton, 0, 0).apply {
            paintColor = getColor(R.styleable.TimeoutButton_progressColor, DEFAULT_COLOR)
            val padding = getDimension(R.styleable.TimeoutButton_progressPadding, PADDING_UNDEFINED)
            if (padding != PADDING_UNDEFINED) {
                progressPadTop = padding
                progressPadLeft = padding
                progressPadRight = padding
                progressPadBottom = padding
            } else {
                // check for individual padding
                progressPadTop = getDimension(R.styleable.TimeoutButton_progressPaddingTop, DEFAULT_PADDING)
                progressPadLeft = getDimension(R.styleable.TimeoutButton_progressPaddingLeft, DEFAULT_PADDING)
                progressPadRight = getDimension(R.styleable.TimeoutButton_progressPaddingRight, DEFAULT_PADDING)
                progressPadBottom = getDimension(R.styleable.TimeoutButton_progressPaddingBottom, DEFAULT_PADDING)
            }
            progressRadius = getDimension(R.styleable.TimeoutButton_progressRadius, DEFAULT_RADIUS)
            animationDurationMs = getInteger(R.styleable.TimeoutButton_progressAnimationDurationMs, DEFAULT_ANIMATION_DURATION)
            decelerateFactor = getFloat(R.styleable.TimeoutButton_progressDecelerateFactor, DEFAULT_DECEL_FACTOR)
            progressStrokeWidth = getDimension(R.styleable.TimeoutButton_progressStrokeWidth, DEFAULT_STROKE_WIDTH)

            recycle()
        }

        paint = Paint().apply {
            color = paintColor
            strokeWidth = this@TimeoutButton.progressStrokeWidth
            style = Paint.Style.STROKE
            isAntiAlias = true
//            maskFilter = BlurMaskFilter(BLUR_RADIUS, BlurMaskFilter.Blur.NORMAL)
        }

        animator = ObjectAnimator.ofFloat(this@TimeoutButton, "phase", 1.0f, 0.0f).apply {
            duration = this@TimeoutButton.animationDurationMs.toLong()
            interpolator = DecelerateInterpolator(this@TimeoutButton.decelerateFactor)
        }
    }

    @ColorInt
    var color: Int = ContextCompat.getColor(context, R.color.primaryDarkColor)
        set(value) {
            field = value
            paint.color = value
            invalidate()
        }

    /**
     * Start the animation
     */
    fun start() {
        started = true
        if (!sized) return

        // Measure the path
        val measure = PathMeasure(path, false)
        length = measure.length
        animator.start()

        buttonChild = children.firstOrNull { it is Button } as Button?

        // framelayouts don't normally draw
        setWillNotDraw(false)
    }

    /**
     * Called by [ObjectAnimator]
     */
    @Suppress("unused")
    fun setPhase(phase: Float) {
        paint.pathEffect = createPathEffect(length, phase)
        invalidate() // will call onDraw
    }

    private fun drawPath(c: Canvas) {
        path?.let { path -> c.drawPath(path, paint) }
    }

    override fun dispatchDraw(canvas: Canvas?) {
        // draw under?
        buttonChild?.let { button ->
            if (button.isPressed) {
                canvas?.let { drawPath(it) }
            }
        }
        // draw children
        super.dispatchDraw(canvas)
        // draw over
        buttonChild?.let { button ->
            if (!button.isPressed) {
                canvas?.let { drawPath(it) }
            }
        }
    }

    override fun onSizeChanged(xNew: Int, yNew: Int, xOld: Int, yOld: Int) {
        super.onSizeChanged(xNew, yNew, xOld, yOld)
        path = createPath(width.toFloat() - paddingLeft - paddingRight - progressPadLeft - progressPadRight,
            height.toFloat() - paddingTop - paddingBottom - progressPadTop - progressPadBottom)

        sized = true
        if (started) {
            start()
        }
    }

    /**
     * Create the path the line / rectangle should follow
     */
    private fun createPath(width: Float, height: Float): Path {
        // subtract stroke size so it can fit
        val w = width - progressStrokeWidth
        val h = height - progressStrokeWidth
        return Path().apply {
            // use the full width for x pos here for easier calculation
            moveTo(width / 2, progressStrokeWidth / 2)
            rLineTo(w / 2, 0F)
            rLineTo(0F, h)
            rLineTo(-w, 0F)
            rLineTo(0F, -h)
            rLineTo(w / 2, 0F)

            // add appropriate padding
            transform(Matrix().apply {
                postTranslate(
                    paddingLeft.toFloat() + progressPadLeft,
                    paddingTop.toFloat() + progressPadTop
                )
            })
        }
    }

    /**
     * Create the appropriate path effect combining the [CornerPathEffect] and [DashPathEffect]
     */
    private fun createPathEffect(
        pathLength: Float,
        phase: Float
    ): PathEffect {
        // combine both of these effects
        return ComposePathEffect(
            CornerPathEffect(progressRadius),
            // we are not actually using this for a dash effect, rather to adjust the length
            DashPathEffect(floatArrayOf(pathLength, pathLength), phase * pathLength)
        )
    }

    companion object {
        //        const val BLUR_RADIUS = 1.3F

        const val PADDING_UNDEFINED = -1F
        const val DEFAULT_PADDING = 0F
        const val DEFAULT_ANIMATION_DURATION = 5_000
        val DEFAULT_RADIUS = AndroidUtils.dpToPixels(2.5F)
        const val DEFAULT_DECEL_FACTOR = 1.1F
        val DEFAULT_STROKE_WIDTH = AndroidUtils.dpToPixels(3F)
        val DEFAULT_COLOR = Color.RED
    }
}