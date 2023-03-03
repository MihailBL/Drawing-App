package com.example.drawingapp

import android.annotation.SuppressLint
import android.content.Context
import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.graphics.Path
import android.os.Build
import android.util.AttributeSet
import android.util.TypedValue
import android.view.MotionEvent
import android.view.View
import androidx.annotation.RequiresApi
import androidx.core.graphics.toColor

/** NOTE: WHEN THE CLASS INHERITS FROM 'VIEW' SUPER CLASS, IT'S AUTOMATICALLY RECOGNIZED AS
 CUSTOM VIEW CLASS */
class DrawingView(context: Context, attributes: AttributeSet) : View(context, attributes){

    private var drawPath: CustomPath? = null
    private var brushSize: Float = 0.toFloat()
    private var color = Color.BLACK

    /** THE 'BITMAP': HOLDS THE PIXELS */
    private var canvasBitMap: Bitmap? = null

    /** THE 'PAINT': HOLDS THE STYLE AND COLOR INFORMATION ABOUT HOW TO DRAW GEOMETRIES, TEXT AND BITMAPS */
    private var drawPaint: Paint? = null
    private var canvasPaint: Paint? = null

    /** THE 'CANVAS CLASS': HOLDS THE "draw" CALLS. TO DRAW SOMETHING, YOU NEED 4 BASIC */
    private var canvas: Canvas? = null

    /** Keep the paths consistently on the screen */
    private val paths = ArrayList<CustomPath>()

    private val undoPaths = ArrayList<CustomPath>()

    init {
        setUpDrawing()
    }

    /** Set up and initialize the properties */
    private fun setUpDrawing(){
        this.drawPath = CustomPath(this.color, this.brushSize)
        this.drawPaint = Paint()
        this.drawPaint!!.color = color
        this.drawPaint!!.style = Paint.Style.STROKE
        this.drawPaint!!.strokeJoin = Paint.Join.ROUND
        this.canvasPaint = Paint(Paint.DITHER_FLAG)
    }

    /** This is called during layout when the size of this view has changed. */
    override fun onSizeChanged(width: Int, height: Int, oldw: Int, oldh: Int) {
        super.onSizeChanged(width, height, oldw, oldh)

        this.canvasBitMap = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888)
        this.canvas = Canvas(canvasBitMap!!)
    }

    /** Method that handles on user touch events/motions */
    @SuppressLint("ClickableViewAccessibility")
    override fun onTouchEvent(event: MotionEvent?): Boolean {
        val touchX = event?.x
        val touchY = event?.y

        when(event?.action){
            // When we press down on the screen
            MotionEvent.ACTION_DOWN -> {
                drawPath?.color = this.color
                drawPath?.brushThickness = this.brushSize
                drawPath?.reset()
                if (touchX != null && touchY != null) {
                    drawPath?.moveTo(touchX, touchY)
                }
            }
            // While we move on the screen
            MotionEvent.ACTION_MOVE -> {
                if (touchX != null && touchY != null)
                    drawPath?.lineTo(touchX, touchY)
            }
            // When releasing the screen
            MotionEvent.ACTION_UP -> {
                paths.add(drawPath!!)
                drawPath = CustomPath(this.color, this.brushSize)
            }
            else -> return false
        }
        // Invalidate the whole view so onDraw() would be called
        invalidate()
        return true
    }

    /** Called when the view should render its content */
    override fun onDraw(canvas: Canvas?) {
        super.onDraw(canvas)

        canvas?.drawBitmap(canvasBitMap!!, 0f, 0f, canvasPaint)

        // Render all the paths that were drawn(do not disappear)
        for (path in paths){
            drawPaint?.color = path.color
            drawPaint?.strokeWidth = path.brushThickness
            canvas?.drawPath(path, drawPaint!!)
        }

        // RENDER THE Path DURING ON TOUCH MOTION
        if (!drawPath!!.isEmpty) {
            drawPaint!!.strokeWidth = drawPath!!.brushThickness
            drawPaint!!.color = drawPath!!.color           // RENDER THE PATH IMMEDIATELY WITH THE CHOSEN COLOR!!!
            canvas?.drawPath(drawPath!!, drawPaint!!)
        }
    }

    /** Method for setting the brush size. Using TypedValue adjusting the screen depending on the screen*/
    fun setSizeForBrush(newSize: Float){
        this.brushSize = TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_DIP, newSize, resources.displayMetrics)
        this.drawPaint!!.strokeWidth = this.brushSize
    }

    /** Set the color*/
    fun setColor(newColor: String){
        this.color = Color.parseColor(newColor)
        this.drawPaint!!.color = this.color
    }

    /** Set the color using Int*/
    @RequiresApi(Build.VERSION_CODES.O)
    fun setColorFromInt(newColor: Int){
        // toColor() method creates and object of the Color class which represents the color from ->
        // -> the converted Int
        this.color = newColor.toColor().toArgb()
        this.drawPaint!!.color = this.color
    }

    /** Undo Button */
    fun onClickUndo(){
        if (this.paths.size > 0){
            this.undoPaths.add(this.paths.removeAt(this.paths.size-1))
            // invalidate() -> internally calls onDraw() method (IT WILL RE-RENDER THE ENTIRE SCREEN)!!!
            invalidate()
        }
    }

    /** Redo Button */
    fun onClickRedo(){
        if(this.undoPaths.size > 0){
            this.paths.add(this.undoPaths.removeAt(this.undoPaths.size-1))
            invalidate()
        }
    }

    /** Clear Button */
    fun onClickClear(){
        this.paths.clear()
        this.undoPaths.clear()
        invalidate()
    }

    /** NOTE: 'PATH' SUPER CLASS ENCAPSULATES COMPOUND(MULTIPLE CONTOUR) GEOMETRIC PATHS -> */
    internal inner class CustomPath(var color: Int, var brushThickness: Float) : Path() {

    }

}