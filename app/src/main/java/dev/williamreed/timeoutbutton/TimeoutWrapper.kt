package dev.williamreed.timeoutbutton

import android.animation.ObjectAnimator
import android.content.Context
import android.graphics.*
import android.util.AttributeSet
import android.view.animation.AccelerateDecelerateInterpolator
import android.view.animation.DecelerateInterpolator
import android.widget.Button
import android.widget.FrameLayout
import androidx.annotation.ColorInt
import androidx.annotation.Keep
import androidx.core.animation.doOnCancel
import androidx.core.animation.doOnEnd
import androidx.core.content.ContextCompat
import androidx.core.view.children
import io.reactivex.subjects.PublishSubject

/**
 * Timeout Wrapper
 *
 * Wraps a button with a progress indicator showing a particular timeout
 *
 * Expects a button as a child view. Note this view itself is _not_ a button. It expects a child
 * to be a button. This view simply facilitates drawing the progress animation over (or under) the
 * button.
 *
 * The progress animation draws over the button if the button is not pressed. If the animation is
 * complete the progress animation stays in whatever state it was in prior to completing. This
 * probably could be fixed but timeout buttons aren't meant to be shown after they complete so not
 * worried about it yet.
 */
open class TimeoutWrapper @JvmOverloads constructor(context: Context, attrs: AttributeSet? = null, defStyle: Int = 0) :
    FrameLayout(context, attrs, defStyle) {
    /** the current path being used */
    private var path: Path? = null
    private var paint: Paint
    private var length = 0f

    /** has the view received its size yet? can't start with size*/
    private var sized = false
    /** has the view been started yet? can't start with size */
    private var started = false
    /** has the animation been cancelled? don't notify the timeoutOccurred if it has */
    private var canceled = false
    private var animator: ObjectAnimator? = null

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
    /** stroke in pixels */
    var progressStrokeWidth: Float

    /**
     * Gets triggered when the progress / timeout animation is complete
     */
    val timeoutOccurred = PublishSubject.create<Unit>()

    init {
        // change some settings to get button shadows properly
        clipToPadding = false

        // get attributes
        val paintColor: Int
        context.obtainStyledAttributes(attrs, R.styleable.TimeoutWrapper, 0, 0).apply {
            paintColor = getColor(R.styleable.TimeoutWrapper_progressColor, DEFAULT_COLOR)
            val padding = getDimension(R.styleable.TimeoutWrapper_progressPadding, PADDING_UNDEFINED)
            if (padding != PADDING_UNDEFINED) {
                progressPadTop = padding
                progressPadLeft = padding
                progressPadRight = padding
                progressPadBottom = padding
            } else {
                // check for individual padding
                progressPadTop = getDimension(R.styleable.TimeoutWrapper_progressPaddingTop, DEFAULT_PADDING)
                progressPadLeft = getDimension(R.styleable.TimeoutWrapper_progressPaddingLeft, DEFAULT_PADDING)
                progressPadRight = getDimension(R.styleable.TimeoutWrapper_progressPaddingRight, DEFAULT_PADDING)
                progressPadBottom = getDimension(R.styleable.TimeoutWrapper_progressPaddingBottom, DEFAULT_PADDING)
            }
            progressRadius = getDimension(R.styleable.TimeoutWrapper_progressRadius, DEFAULT_RADIUS)
            animationDurationMs = getInteger(R.styleable.TimeoutWrapper_progressAnimationDurationMs, DEFAULT_ANIMATION_DURATION)
            progressStrokeWidth = getDimension(R.styleable.TimeoutWrapper_progressStrokeWidth, DEFAULT_STROKE_WIDTH)

            recycle()
        }

        paint = Paint().apply {
            color = paintColor
            strokeWidth = this@TimeoutWrapper.progressStrokeWidth
            style = Paint.Style.STROKE
            isAntiAlias = true
//            maskFilter = BlurMaskFilter(BLUR_RADIUS, BlurMaskFilter.Blur.NORMAL)
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
     * Start the animation. If it is already running, it is cleared
     */
    fun start() {
        // if we are already running, reset it
        if (started && sized) clear()

        // reset state
        started = true
        canceled = false

        // can't start until sized properly
        if (!sized) return

        // Measure the path
        val measure = PathMeasure(path, false)
        length = measure.length
        animator?.apply {
            start()
            doOnCancel { canceled = true }
            doOnEnd { if (!canceled) timeoutOccurred.onNext(Unit) }
        }

        buttonChild = children.firstOrNull { it is Button } as Button?

        // framelayouts don't normally draw
        setWillNotDraw(false)
    }

    /**
     * Reset the progress animation
     */
    fun clear() {
        started = false
        animator?.cancel()
        animator = ObjectAnimator.ofFloat(this, "phase", PHASE_START, PHASE_END).apply {
            duration = this@TimeoutWrapper.animationDurationMs.toLong()
            interpolator = AccelerateDecelerateInterpolator(context, null)
        }
        // manually set phase
        setPhase(PHASE_START)
    }

    /**
     * Called by [ObjectAnimator]
     */
    @Suppress("unused")
    @Keep
    fun setPhase(phase: Float) {
        paint.pathEffect = createPathEffect(length, phase)
        invalidate() // will call onDraw
    }

    private fun drawPath(c: Canvas) {
        path?.let { path -> c.drawPath(path, paint) }
    }

    /**
     * Draw the progress over or under the button depending on the state of the button
     */
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
        path = createPath(
            width.toFloat() - paddingLeft - paddingRight - progressPadLeft - progressPadRight,
            height.toFloat() - paddingTop - paddingBottom - progressPadTop - progressPadBottom
        )

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
        const val DEFAULT_COLOR = Color.RED
        val DEFAULT_RADIUS = AndroidUtils.dpToPixels(2.5F)
        val DEFAULT_STROKE_WIDTH = AndroidUtils.dpToPixels(3F)

        private const val PHASE_START = 1F
        private const val PHASE_END = 0F
    }
}