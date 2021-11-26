package wiki.scene.split

import android.content.Context
import android.graphics.*
import android.text.InputFilter
import android.text.InputFilter.LengthFilter
import android.text.InputType
import android.text.TextUtils
import android.util.AttributeSet
import android.util.TypedValue
import androidx.appcompat.widget.AppCompatEditText

class SplitEditText : AppCompatEditText {

    constructor(context: Context) : super(context) {
        init(context, null)
    }

    constructor(context: Context, attrs: AttributeSet?) : super(context, attrs) {
        init(context, attrs)
    }

    companion object {
        private const val DEFAULT_CIPHER_MASK = "*"
    }

    //画笔
    private lateinit var mPaint: Paint

    //路径
    private val mPath: Path by lazy { Path() }

    //边框风格
    @BorderStyle
    private var mBorderStyle = BorderStyle.BOX

    //文本风格
    @TextStyle
    private var mTextStyle = TextStyle.PLAIN_TEXT

    //画笔宽度
    private var mStrokeWidth = 0F

    //框与框之间的间距大小
    private var mBorderSpacing = 0F

    //边框颜色
    private var mBorderColor: Int = 0x666666

    //输入的边框颜色
    private var mInputBorderColor = 0

    //焦点的边框颜色
    private var mFocusBorderColor = 0

    //框的背景颜色
    private var mBoxBackgroundColor = 0x00000000

    //框的圆角大小
    private var mBorderCornerRadius = 0F

    //输入框宽度
    private var mBoxWidth = 0F

    //输入框高度
    private var mBoxHeight = 0F

    //允许输入的最大长度
    private var mMaxLength = 6

    //文本长度
    private var mTextLength = 0

    //密码掩码
    private var mCipherMask: String = ""

    //是否是粗体
    private var isFakeBoldText = false

    private val mRectF: RectF by lazy { RectF(0F, 0F, 0F, 0F) }
    private val mRadiusFirstArray: FloatArray by lazy { FloatArray(8) }
    private val mRadiusLastArray: FloatArray by lazy { FloatArray(8) }

    private var isDraw = false

    private val mOnTextInputListener: OnTextInputListener? = null

    private fun init(context: Context, attrs: AttributeSet?) {
        val displayMetrics = resources.displayMetrics

        mStrokeWidth = TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_DIP, 1f, displayMetrics)
        mBorderSpacing = TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_DIP, 8f, displayMetrics)
        setPadding(0, 0, 0, 0)

        val a = context.obtainStyledAttributes(attrs, R.styleable.SplitEditText)
        val attrsCount = a.indexCount
        for (i in 0 until attrsCount) {
            when (val attr = a.getIndex(i)) {
                R.styleable.SplitEditText_setStrokeWidth -> {
                    mStrokeWidth = a.getDimension(attr, mStrokeWidth)
                }
                R.styleable.SplitEditText_setBorderColor -> {
                    mBorderColor = a.getColor(attr, mBorderColor)
                }
                R.styleable.SplitEditText_setInputBorderColor -> {
                    mInputBorderColor = a.getColor(attr, mInputBorderColor)
                }
                R.styleable.SplitEditText_setFocusBorderColor -> {
                    mFocusBorderColor = a.getColor(attr, mFocusBorderColor)
                }
                R.styleable.SplitEditText_setBoxBackgroundColor -> {
                    mBoxBackgroundColor = a.getColor(attr, mBoxBackgroundColor)
                }
                R.styleable.SplitEditText_setBorderCornerRadius -> {
                    mBorderCornerRadius = a.getDimension(attr, mBorderCornerRadius)
                }
                R.styleable.SplitEditText_setBorderSpacing -> {
                    mBorderSpacing = a.getDimension(attr, mBorderSpacing)
                }
                R.styleable.SplitEditText_setMaxLength -> {
                    mMaxLength = a.getInt(attr, mMaxLength)
                }
                R.styleable.SplitEditText_setBorderStyle -> {
                    mBorderStyle = a.getInt(attr, mBorderStyle)
                }
                R.styleable.SplitEditText_setTextStyle -> {
                    mTextStyle = a.getInt(attr, mTextStyle)
                }
                R.styleable.SplitEditText_setCipherMask -> {
                    mCipherMask =
                        if (a.getString(attr) == null) DEFAULT_CIPHER_MASK else a.getString(attr)!!
                }
                R.styleable.SplitEditText_setFakeBoldText -> {
                    isFakeBoldText = a.getBoolean(attr, false)
                }
            }
        }

        a.recycle()

        inputType = InputType.TYPE_TEXT_VARIATION_PASSWORD

        if (mInputBorderColor == 0) {
            mInputBorderColor = mBorderColor
        }
        if (mFocusBorderColor == 0) {
            mFocusBorderColor = mBorderColor
        }

        initPaint()


        if (TextUtils.isEmpty(mCipherMask)) {
            mCipherMask = DEFAULT_CIPHER_MASK
        } else if (mCipherMask.length > 1) {
            mCipherMask = mCipherMask.substring(0, 1)
        }

        background = null
        isCursorVisible = false
        filters = arrayOf<InputFilter>(LengthFilter(mMaxLength))

    }

    /**
     * 初始化画笔
     */
    private fun initPaint() {
        mPaint = Paint(Paint.ANTI_ALIAS_FLAG)
        mPaint.isAntiAlias = true
        mPaint.textAlign = Paint.Align.CENTER
    }


    override fun onSizeChanged(w: Int, h: Int, oldw: Int, oldh: Int) {
        super.onSizeChanged(w, h, oldw, oldh)
        val width = w - paddingLeft - paddingRight
        val height = h - paddingTop - paddingBottom
        updateSizeChanged(width, height)
    }

    private fun updateSizeChanged(width: Int, height: Int) {
        //如果框与框之间的间距小于0或者总间距大于控件可用宽度则将间距重置为0
        if (mBorderSpacing < 0 || (mMaxLength - 1) * mBorderSpacing > width) {
            mBorderSpacing = 0f
        }
        //计算出每个框的宽度
        mBoxWidth = (width - (mMaxLength - 1) * mBorderSpacing) / mMaxLength - mStrokeWidth
        mBoxHeight = height - mStrokeWidth
    }

    override fun onDraw(canvas: Canvas?) {
        //移除super.onDraw(canvas);不绘制EditText相关的
        //绘制边框
        drawBorders(canvas)
    }

    /**
     * 绘制边框
     */
    private fun drawBorders(canvas: Canvas?) {
        isDraw = true
        //遍历绘制未输入文本的框边界
        for (i in mTextLength until mMaxLength) {
            drawBorder(canvas, i, mBorderColor)
        }
        val color = if (mInputBorderColor != 0) mInputBorderColor else mBorderColor
        //遍历绘制已输入文本的框边界
        for (i in 0 until mTextLength) {
            drawBorder(canvas, i, color)
        }

        //绘制焦点框边界
        if (mTextLength < mMaxLength && mFocusBorderColor != 0 && isFocused) {
            drawBorder(canvas, mTextLength, mFocusBorderColor)
        }
    }

    /**
     * 绘制边框
     */
    private fun drawBorder(canvas: Canvas?, position: Int, borderColor: Int) {
        mPaint.strokeWidth = mStrokeWidth
        mPaint.style = Paint.Style.STROKE
        mPaint.isFakeBoldText = false
        mPaint.color = borderColor

        //计算出对应的矩形
        val left = paddingLeft + mStrokeWidth / 2 + (mBoxWidth + mBorderSpacing) * position
        val top = paddingTop + mStrokeWidth / 2
        mRectF[left, top, left + mBoxWidth] = top + mBoxHeight
        when (mBorderStyle) {
            BorderStyle.BOX -> drawBorderBox(canvas, position, borderColor)
            BorderStyle.LINE -> drawBorderLine(canvas)
        }
        if (mTextLength > position && !TextUtils.isEmpty(text)) {
            drawText(canvas, position)
        }
    }

    private fun drawText(canvas: Canvas?, position: Int) {
        canvas?.let {
            mPaint.strokeWidth = 0f
            mPaint.color = currentTextColor
            mPaint.style = Paint.Style.FILL_AND_STROKE
            mPaint.textSize = textSize
            mPaint.isFakeBoldText = isFakeBoldText
            val x = mRectF.centerX()
            //y轴坐标 = 中心线 + 文字高度的一半 - 基线到文字底部的距离（也就是bottom）
            val y =
                mRectF.centerY() + (mPaint.fontMetrics.bottom - mPaint.fontMetrics.top) / 2 - mPaint.fontMetrics.bottom
            when (mTextStyle) {
                TextStyle.PLAIN_TEXT -> canvas.drawText(text!![position].toString(), x, y, mPaint)
                TextStyle.CIPHER_TEXT -> canvas.drawText(mCipherMask, x, y, mPaint)
            }
        }

    }

    /**
     * 绘制框风格
     *
     * @param canvas
     * @param position
     */
    private fun drawBorderBox(canvas: Canvas?, position: Int, borderColor: Int) {
        canvas?.let {
            if (mBorderCornerRadius > 0) { //当边框带有圆角时
                if (mBorderSpacing == 0f) { //当边框之间的间距为0时，只需要开始一个和最后一个框有圆角
                    if (position == 0 || position == mMaxLength - 1) {
                        if (mBoxBackgroundColor != 0) {
                            mPaint.style = Paint.Style.FILL
                            mPaint.color = mBoxBackgroundColor
                            canvas.drawPath(getRoundRectPath(mRectF, position == 0), mPaint)
                        }
                        mPaint.style = Paint.Style.STROKE
                        mPaint.color = borderColor
                        canvas.drawPath(getRoundRectPath(mRectF, position == 0), mPaint)
                    } else {
                        if (mBoxBackgroundColor != 0) {
                            mPaint.style = Paint.Style.FILL
                            mPaint.color = mBoxBackgroundColor
                            canvas.drawRect(mRectF, mPaint)
                        }
                        mPaint.style = Paint.Style.STROKE
                        mPaint.color = borderColor
                        canvas.drawRect(mRectF, mPaint)
                    }
                } else {
                    if (mBoxBackgroundColor != 0) {
                        mPaint.style = Paint.Style.FILL
                        mPaint.color = mBoxBackgroundColor
                        canvas.drawRoundRect(
                            mRectF,
                            mBorderCornerRadius,
                            mBorderCornerRadius,
                            mPaint
                        )
                    }
                    mPaint.style = Paint.Style.STROKE
                    mPaint.color = borderColor
                    canvas.drawRoundRect(mRectF, mBorderCornerRadius, mBorderCornerRadius, mPaint)
                }
            } else {
                if (mBoxBackgroundColor != 0) {
                    mPaint.style = Paint.Style.FILL
                    mPaint.color = mBoxBackgroundColor
                    canvas.drawRect(mRectF, mPaint)
                }
                mPaint.style = Paint.Style.STROKE
                mPaint.color = borderColor
                canvas.drawRect(mRectF, mPaint)
            }
        }

    }

    /**
     * 绘制线风格
     *
     * @param canvas
     */
    private fun drawBorderLine(canvas: Canvas?) {
        val y = paddingTop + mBoxHeight
        canvas?.drawLine(mRectF.left, y, mRectF.right, y, mPaint)
    }

    private fun getRoundRectPath(rectF: RectF, isFirst: Boolean): Path {
        mPath.reset()
        if (isFirst) {
            //左上角
            mRadiusFirstArray[0] = mBorderCornerRadius
            mRadiusFirstArray[1] = mBorderCornerRadius
            //左下角
            mRadiusFirstArray[6] = mBorderCornerRadius
            mRadiusFirstArray[7] = mBorderCornerRadius
            mPath.addRoundRect(rectF, mRadiusFirstArray, Path.Direction.CW)
        } else {
            //右上角
            mRadiusLastArray[2] = mBorderCornerRadius
            mRadiusLastArray[3] = mBorderCornerRadius
            //右下角
            mRadiusLastArray[4] = mBorderCornerRadius
            mRadiusLastArray[5] = mBorderCornerRadius
            mPath.addRoundRect(rectF, mRadiusLastArray, Path.Direction.CW)
        }
        return mPath
    }

    override fun onTextChanged(
        text: CharSequence?,
        start: Int,
        lengthBefore: Int,
        lengthAfter: Int
    ) {
        super.onTextChanged(text, start, lengthBefore, lengthAfter)
        mTextLength = text!!.length
        refreshView()
        //改变监听
        if (mOnTextInputListener != null) {
            mOnTextInputListener.onTextInputChanged(text.toString(), mTextLength)
            if (mTextLength == mMaxLength) {
                mOnTextInputListener.onTextInputCompleted(text.toString())
            }
        }
    }

    override fun onSelectionChanged(selStart: Int, selEnd: Int) {
        super.onSelectionChanged(selStart, selEnd)
        if (selStart == selEnd) {
            setSelection(if (text == null) 0 else text!!.length)
        }
    }

    override fun onFocusChanged(focused: Boolean, direction: Int, previouslyFocusedRect: Rect?) {
        super.onFocusChanged(focused, direction, previouslyFocusedRect)
        //焦点改变时刷新状态
        refreshView()
    }

    override fun onDetachedFromWindow() {
        super.onDetachedFromWindow()
        isDraw = false
    }

    fun getBorderColor(): Int {
        return mBorderColor
    }

    fun getInputBorderColor(): Int {
        return mInputBorderColor
    }

    fun getFocusBorderColor(): Int {
        return mFocusBorderColor
    }

    fun getBoxBackgroundColor(): Int {
        return mBoxBackgroundColor
    }

    fun getBorderCornerRadius(): Float {
        return mBorderCornerRadius
    }

    fun getBorderSpacing(): Float {
        return mBorderSpacing
    }

    @BorderStyle
    fun getBorderStyle(): Int {
        return mBorderStyle
    }


    fun setBorderColor(borderColor: Int) {
        mBorderColor = borderColor
        refreshView()
    }

    fun setInputBorderColor(inputBorderColor: Int) {
        mInputBorderColor = inputBorderColor
        refreshView()
    }

    fun setFocusBorderColor(focusBorderColor: Int) {
        mFocusBorderColor = focusBorderColor
        refreshView()
    }

    fun setBoxBackgroundColor(boxBackgroundColor: Int) {
        mBoxBackgroundColor = boxBackgroundColor
        refreshView()
    }

    fun setBorderCornerRadius(borderCornerRadius: Float) {
        mBorderCornerRadius = borderCornerRadius
        refreshView()
    }

    fun setBorderSpacing(borderSpacing: Float) {
        mBorderSpacing = borderSpacing
        refreshView()
    }

    fun setBorderStyle(@TextStyle borderStyle: Int) {
        mBorderStyle = borderStyle
        refreshView()
    }

    @TextStyle
    fun getTextStyle(): Int {
        return mTextStyle
    }

    fun setTextStyle(@TextStyle textStyle: Int) {
        mTextStyle = textStyle
        refreshView()
    }

    fun getCipherMask(): String {
        return mCipherMask
    }

    /**
     * 是否粗体
     *
     * @param fakeBoldText
     */
    fun setFakeBoldText(fakeBoldText: Boolean) {
        isFakeBoldText = fakeBoldText
        refreshView()
    }

    /**
     * 设置密文掩码 不设置时，默认为[.DEFAULT_CIPHER_MASK]
     *
     * @param cipherMask
     */
    fun setCipherMask(cipherMask: String?) {
        mCipherMask = cipherMask!!
        refreshView()
    }

    /**
     * 刷新视图
     */
    private fun refreshView() {
        if (isDraw) {
            invalidate()
        }
    }

}