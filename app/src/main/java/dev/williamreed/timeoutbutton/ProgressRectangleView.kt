package dev.williamreed.timeoutbutton

import android.animation.ObjectAnimator
import android.content.Context
import android.graphics.*
import android.util.AttributeSet
import android.view.View
import android.view.animation.DecelerateInterpolator
import androidx.annotation.ColorInt
import androidx.core.content.ContextCompat


/**
 * Progress Rectangle View
 *
 * Displays a progress like animated line that moves in a clockwise rectangle on the perimeter of
 * this view. Respects the given padding for the rectangle as well.
 */
class ProgressRectangleView @JvmOverloads constructor(context: Context, attrs: AttributeSet? = null, defStyle: Int = 0) :
    View(context, attrs, defStyle) {
    /** the current path being used */
    private var path: Path? = null
    private var paint: Paint
    private var length = 0f

    /** has the view received its size yet? can't start with size*/
    private var sized = false
    /** has the view been started yet? can't start with size */
    private var started = false
    private var animator: ObjectAnimator

    init {
        paint = Paint().apply {
            color = ContextCompat.getColor(context, R.color.dark_gray)
            strokeWidth = STROKE_WIDTH
            style = Paint.Style.STROKE
            isAntiAlias = true
//            maskFilter = BlurMaskFilter(BLUR_RADIUS, BlurMaskFilter.Blur.NORMAL)
        }

        animator = ObjectAnimator.ofFloat(this@ProgressRectangleView, "phase", 1.0f, 0.0f).apply {
            duration = ANIMATION_DURATION_MS
            interpolator = DecelerateInterpolator(DECELERATE_FACTOR)
        }
    }

    @ColorInt var color: Int = ContextCompat.getColor(context, R.color.primaryDarkColor)
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
    }

    /**
     * Called by [ObjectAnimator]
     */
    @Suppress("unused")
    fun setPhase(phase: Float) {
        paint.pathEffect = createPathEffect(length, phase)
        invalidate() // will call onDraw
    }

    override fun onDraw(c: Canvas) {
        super.onDraw(c)

        if (started && sized) {
            path?.let { path -> c.drawPath(path, paint) }
        }
    }

    override fun onSizeChanged(xNew: Int, yNew: Int, xOld: Int, yOld: Int) {
        super.onSizeChanged(xNew, yNew, xOld, yOld)
        path = createPath(width.toFloat() - paddingLeft - paddingRight, height.toFloat() - paddingTop - paddingBottom)

        sized = true
        if (started) {
            start()
        }
    }

    override fun onMeasure(widthMeasureSpec: Int, heightMeasureSpec: Int) {
        super.onMeasure(widthMeasureSpec, heightMeasureSpec)

        val widthMode = MeasureSpec.getMode(widthMeasureSpec)
        val widthSize = MeasureSpec.getSize(widthMeasureSpec)
        val heightMode = MeasureSpec.getMode(heightMeasureSpec)
        val heightSize = MeasureSpec.getSize(heightMeasureSpec)

        val a = 3
    }

    /**
     * Create the path the line / rectangle should follow
     */
    private fun createPath(width: Float, height: Float): Path {
        // subtract stroke size so it can fit
        val w = width - STROKE_WIDTH
        val h = height - STROKE_WIDTH
        return Path().apply {
            // use the full width for x pos here for easier calculation
            moveTo(width / 2, STROKE_WIDTH / 2)
            rLineTo(w / 2, 0F)
            rLineTo(0F, h)
            rLineTo(-w, 0F)
            rLineTo(0F, -h)
            rLineTo(w / 2, 0F)

            // add appropriate padding
            transform(Matrix().apply {
                postTranslate(
                    paddingLeft.toFloat(),
                    paddingTop.toFloat()
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
            CornerPathEffect(CORNER_RADIUS),
            // we are not actually using this for a dash effect, rather to adjust the length
            DashPathEffect(floatArrayOf(pathLength, pathLength), phase * pathLength)
        )
    }

    companion object {
        val STROKE_WIDTH = AndroidUtils.dpToPixels(3F)//7.0F
        val CORNER_RADIUS = AndroidUtils.dpToPixels(2.5F) //6.0F
//        const val BLUR_RADIUS = 1.3F
        const val ANIMATION_DURATION_MS = 10_000L
        const val DECELERATE_FACTOR = 1.1F
    }
}