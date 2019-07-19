package com.test.expandtextview

import android.content.Context
import android.graphics.*
import android.text.Layout
import android.text.StaticLayout
import android.text.TextPaint
import android.text.TextUtils
import android.util.AttributeSet
import android.view.MotionEvent
import android.view.View
import java.util.regex.Pattern
import kotlin.math.max

/**
 *@author wangxiaochen
 *@Date: 2019-07-19
 */
class ExpandTextView : View {
    companion object {
        @Volatile
        var isPause = false

        //暂停计算
        fun onPause() {
            isPause = true
        }

        //恢复计算
        fun onResume() {
            isPause = false
        }
    }

    //当前是否是展开状态
    @Volatile
    var isExpand = false
        set(value) {
            field = value
            bottom = calHeight(width).toInt()
            log.d("计算的bottom:$bottom")
            requestLayout()
            invalidate()
        }

    //展开状态下 特殊位置的展示,特殊展示不考虑多行文本的情况
    var shrinkText = "收起"
    //收缩状态下 特殊位置的展示
    var expandText = "展开"
    //如果是收起状态 展开的前缀内容，这个前缀的内容的绘制不在特殊内容绘制当中
    var expandPrifix = "..."
    /**
     * 主要内容展示的颜色
     */
    var contentTextColor = Color.BLACK
    /**
     * 主要内容的展示颜色
     */
    var contentTextSize = 10f
    /**
     * 主要内容是否进行粗体展示
     */
    var contentBold = false
    /**
     * 每行文本之间底部的间距
     */
    var bottomMargin = 0f
    /**
     * 如果包含特殊展示，在收起的状态下的特殊展示那一行，右边空出来的宽度
     */
    var specialRightMargin = 0f
    /**
     * 特殊按钮距离左边的距离，如果太近会靠在一起，不是很美观
     */
    var specialLeftMargin = 0f
    /**
     * 特殊展示的文本颜色
     */
    var specialTextColor = Color.BLACK
    /**
     * 特殊展示按钮的文本size
     */
    var specialTextSize = 10f
    /**
     * 特殊展示是否进行粗体展示
     */
    var specialBold = true

    /**
     * 特殊按钮点击的横向热区扩大范围
     */
    var specialHorizonClickMore = 0f
    /**
     * 特殊按钮纵向热区扩大范围
     */
    var specialVerticalClickMore = 0f

    /**
     * 当前控件总共的文本内容数据
     */
    var currentText: String = ""
        set(value) {
            field = getDealStr(value)
            bottom = calHeight(width).toInt()
            log.d("计算的bottom:$bottom")
            requestLayout()
            invalidate()
        }
    /**
     * 主内容的paint
     */
    val contentPaint: TextPaint by lazy {
        var paint = TextPaint()
        paint.textSize = contentTextSize
        paint.color = contentTextColor
        paint.style = Paint.Style.FILL
        paint.strokeWidth = 5f
        paint.isFakeBoldText = contentBold
        paint.isAntiAlias = true
        paint
    }
    /**
     * 特殊内容的paint
     */
    val specialPaint: TextPaint by lazy {
        var paint = TextPaint()
        paint.textSize = specialTextSize
        paint.color = specialTextColor
        paint.style = Paint.Style.FILL
        paint.strokeWidth = 5f
        paint.isFakeBoldText = specialBold
        paint.isAntiAlias = true
        paint
    }

    /**
     * 最多展示的行数
     */
    var maxLine = 2

    /**
     * 用来测量文本宽高的临时变量
     */
    var rect = Rect()

    /**
     * 特殊按钮展示的区域范围
     */
    var specialRect = RectF()

    /**
     * 点击事件，区分主内容和展开内容的点击监听
     */
    var clickListener: ClickListener? = null


    constructor(context: Context) : this(context, null)

    constructor(context: Context, attr: AttributeSet?) : super(context, attr) {
        var typeArray = context.obtainStyledAttributes(attr, R.styleable.ExpandTextView)
        if (typeArray == null) {
            return
        }
        var num = typeArray.indexCount
        for (i in 0 until num) {
            var attr = typeArray.getIndex(i)
            when (attr) {
                //主内容属性
                R.styleable.ExpandTextView_content_size -> contentTextSize = typeArray.getDimension(attr, 5f)
                R.styleable.ExpandTextView_content_color -> contentTextColor = typeArray.getColor(attr, Color.BLACK)
                R.styleable.ExpandTextView_content_bold -> contentBold = typeArray.getBoolean(attr, false)
                //特殊内容属性
                R.styleable.ExpandTextView_special_size -> specialTextSize = typeArray.getDimension(attr, 5f)
                R.styleable.ExpandTextView_special_color -> specialTextColor = typeArray.getColor(attr, Color.BLACK)
                R.styleable.ExpandTextView_special_bold -> specialBold = typeArray.getBoolean(attr, false)
                R.styleable.ExpandTextView_special_right_margin -> specialRightMargin = typeArray.getDimension(attr, 5f)
                R.styleable.ExpandTextView_special_horizon_click_more -> specialHorizonClickMore =
                    typeArray.getDimension(attr, 5f)
                R.styleable.ExpandTextView_special_vertical_click_more -> specialVerticalClickMore =
                    typeArray.getDimension(attr, 5f)
                //一些文本的设置
                R.styleable.ExpandTextView_current_text -> {
                    currentText = getDealStr(typeArray.getString(attr)) ?: ""
                }
                R.styleable.ExpandTextView_shrink_text -> {
                    shrinkText = getDealStr(typeArray.getString(attr)) ?: ""
                }
                R.styleable.ExpandTextView_expand_text -> {
                    expandText = getDealStr(typeArray.getString(attr)) ?: ""
                }
                R.styleable.ExpandTextView_expand_prifix_text -> {
                    expandPrifix = getDealStr(typeArray.getString(attr)) ?: ""
                }

                R.styleable.ExpandTextView_max_line -> maxLine = typeArray.getInteger(attr, Int.MAX_VALUE)
                R.styleable.ExpandTextView_bottom_margin -> bottomMargin = typeArray.getDimension(attr, 0f)
                R.styleable.ExpandTextView_special_left_margin -> {
                    specialLeftMargin = typeArray.getDimension(attr, 0f)
                }
            }
        }
        typeArray.recycle()
    }

    override fun onMeasure(widthMeasureSpec: Int, heightMeasureSpec: Int) {
        super.onMeasure(widthMeasureSpec, heightMeasureSpec)
        var width = MeasureSpec.getSize(widthMeasureSpec)
        var widthMode = MeasureSpec.getMode(widthMeasureSpec)
        var heightMode = MeasureSpec.getMode(heightMeasureSpec)
        var height = MeasureSpec.getSize(heightMeasureSpec)
        when (heightMode) {
            MeasureSpec.EXACTLY -> height = height
            else -> height = calHeight(width).toInt()
        }
        setMeasuredDimension(width, height)
    }

    override fun onSizeChanged(w: Int, h: Int, oldw: Int, oldh: Int) {
        super.onSizeChanged(w, h, oldw, oldh)
        log.d("w/h:$w/$h,ow/od:$oldw/$oldh")
    }

    /**
     * 获取自己测量后的高度
     */
    fun calHeight(width: Int): Float {
        if (TextUtils.isEmpty(currentText)) {
            log.e("当前内容为空")
            return 0f
        }
        if (width <= 0) {
            log.e("当前宽度为0")
            return 0f
        }
        var staticLayout = getStaticLayout(currentText, contentPaint, width)
        var lineCount = staticLayout.lineCount
        var height = 0f
        if (lineCount <= maxLine) {
            height = staticLayout.height + lineCount * bottomMargin
        } else {
            if (isExpand) {
                //计算展开状态下的高度
                for (i in 0 until lineCount) {
                    //获取当前行内容
                    var currentLinStr = currentText.substring(staticLayout.getLineStart(i), staticLayout.getLineEnd(i))
                    var currentLinStaticLayout = getStaticLayout(currentLinStr, contentPaint, width)

                    if (i == lineCount - 1) {
                        //特殊展示的内容数据
                        var shrinkStaticLayout = getStaticLayout(shrinkText, specialPaint, width)
                        var shrinkWidth = shrinkStaticLayout.getLineWidth(0)
                        //主内容最后一行的宽度
                        var currentLinStrWidth = currentLinStaticLayout.getLineWidth(0)
                        //leftmargin对于收起是生效的，但是rightmargin是不生效的
                        if ((currentLinStrWidth + specialLeftMargin + shrinkWidth) > width) {
                            //需要另起一行
                            height = height + currentLinStaticLayout.height + bottomMargin
                            height = height + shrinkStaticLayout.height + bottomMargin
                        } else {
                            //取最高的那个
                            var max = max(currentLinStaticLayout.height, shrinkStaticLayout.height)
                            height = height + max + bottomMargin
                        }
                    } else {
                        //普通行
                        height = height + currentLinStaticLayout.height + bottomMargin
                    }
                }
            } else {
                //计算收起状态下的高度,只需要取最大行数内容就可以了
                for (i in 0 until lineCount) {
                    //获取当前行内容
                    var currentLinStr = currentText.substring(staticLayout.getLineStart(i), staticLayout.getLineEnd(i))
                    var currentLinStaticLayout = getStaticLayout(currentLinStr, contentPaint, width)

                    if (i == maxLine - 1) {
                        var shrinkStaticLayout = getStaticLayout(shrinkText, specialPaint, width)
                        //计算最大行那一行的内容
                        var max = max(currentLinStaticLayout.height, shrinkStaticLayout.height)
                        height = height + max + bottomMargin
                        return height
                    } else {
                        //普通行
                        height = height + currentLinStaticLayout.height + bottomMargin
                    }
                }
            }
        }
        return height
    }

    override fun onDraw(canvas: Canvas) {
        super.onDraw(canvas)
        if (isPause) {
            return
        }
        drawText(canvas)
    }

    /**
     * 最终绘制文本操作的入口
     */
    fun drawText(canvas: Canvas) {
        if (TextUtils.isEmpty(currentText)) {
            canvas.drawText("", 0f, 0f, contentPaint)
            log.e("当前需要绘制的文本内容为空")
            return
        }
        var staticLayout = getStaticLayout(currentText, contentPaint, width)
        var lineCount = staticLayout.lineCount
        if (lineCount <= maxLine) {
            //没有特殊内容的展示
            var height = 0f

            for (i in 0 until lineCount) {
                var currentLineStr = currentText.substring(staticLayout.getLineStart(i), staticLayout.getLineEnd(i))
                var currentLineStaticLayout = getStaticLayout(currentLineStr, contentPaint, width)
                height = height + currentLineStaticLayout.height
                canvas.drawText(currentLineStr, 0f, height, contentPaint)
                height+=bottomMargin
            }
        } else {
            //包含特殊内容的展示
            var height = 0f
            if (isExpand) {
                //当前状态为展开状态，绘制展开状态内容
                var shrinkStaticLayout = getStaticLayout(shrinkText, specialPaint, width)
                var shrinkStaticLayoutWidth = shrinkStaticLayout.getLineWidth(0)
                var shrinkStaticLayoutHeight = shrinkStaticLayout.height
                for (i in 0 until lineCount) {
                    //获取当前行内容
                    var currentLineStr = currentText.substring(staticLayout.getLineStart(i), staticLayout.getLineEnd(i))
                    var currentLineStaticLayout = getStaticLayout(currentLineStr, contentPaint, width)
                    if (i == lineCount - 1) {
                        //最后一行特殊处理
                        var currentLineWidth = currentLineStaticLayout.getLineWidth(0)
                        if ((currentLineWidth + specialLeftMargin + shrinkStaticLayoutWidth) > width) {
                            //展开内容需要换行处理
                            //绘制主内容
                            height = height + currentLineStaticLayout.height
                            canvas.drawText(currentLineStr, 0f, height, contentPaint)
                            height += bottomMargin
                            //绘制特殊内容
                            specialRect.set(
                                0f,
                                height - specialVerticalClickMore,
                                shrinkStaticLayoutWidth + specialHorizonClickMore,
                                height + shrinkStaticLayoutHeight + specialVerticalClickMore
                            )
                            height = height + shrinkStaticLayoutHeight
                            canvas.drawText(shrinkText, 0f, height, specialPaint)
                            log.d("当前最后一行最底部距离:${height +bottomMargin}")
                        } else {
                            //都绘制在同一行
                            //绘制主内容
                            var currentWidth = 0f
                            canvas.drawText(
                                currentLineStr,
                                currentWidth,
                                height + currentLineStaticLayout.height,
                                contentPaint
                            )
                            currentWidth += currentLineWidth
                            //绘制特殊内容
                            currentWidth += specialLeftMargin
                            canvas.drawText(shrinkText, currentWidth, height + shrinkStaticLayoutHeight, specialPaint)
                            log.d("当前最后一行最底部距离:${height + shrinkStaticLayoutHeight+bottomMargin}")
                            specialRect.set(
                                currentWidth - specialHorizonClickMore,
                                height - specialVerticalClickMore,
                                currentWidth + shrinkStaticLayoutWidth + specialVerticalClickMore,
                                height + shrinkStaticLayoutHeight + specialVerticalClickMore
                            )
                        }
                        return
                    } else {
                        height = height + currentLineStaticLayout.height
                        canvas.drawText(currentLineStr, 0f, height, contentPaint)
                        height += bottomMargin
                    }
                }
            } else {
                //当前为收起状态，绘制收起状态内容
                //收起状态多了前缀，前缀需要特殊处理
                var expandStaticLayout = getStaticLayout(expandText, specialPaint, width)
                var expandStaticLayoutHeight = expandStaticLayout.height
                var expandStaticLayoutWidth = expandStaticLayout.getLineWidth(0)
                var height = 0f
                for (i in 0 until lineCount) {
                    //获取当前行内容
                    var currentLineStr = currentText.substring(staticLayout.getLineStart(i), staticLayout.getLineEnd(i))
                    var currentLineStaticLayout = getStaticLayout(currentLineStr, contentPaint, width)
                    if (i == maxLine - 1) {
                        //在最大一行特殊处理
                        //需要计算前缀的内容信息
                        var expandPrifixStaticLayout = getStaticLayout(expandPrifix, contentPaint, width)
                        var expandPrifixStaticLayoutWidth = expandPrifixStaticLayout.getLineWidth(0)

                        //主内容可以进行绘制的宽度内容
                        var leftWidth =
                            width - expandStaticLayoutWidth - specialRightMargin - specialLeftMargin - expandPrifixStaticLayoutWidth
                        //当前正在绘制的宽内容
                        var currentWidth = 0f
                        //临时的一个高度，表达当前主内容的y值
                        var currentLineTmpHeight = height + currentLineStaticLayout.height
                        for (i in currentLineStr) {
                            var currentString = i.toString()
                            var currentStringStaticLayout = getStaticLayout(currentString, contentPaint, width)
                            var currentStringStaticLayoutWidth = currentStringStaticLayout.getLineWidth(0)
                            if ((currentWidth + currentStringStaticLayoutWidth) > leftWidth) {
                                //这个文字不被继续绘制了，开始绘制特殊内容
                                //绘制前缀信息
                                canvas.drawText(expandPrifix, currentWidth, currentLineTmpHeight, contentPaint)
                                currentWidth += expandPrifixStaticLayoutWidth
                                //绘制特殊内容区域信息
                                currentWidth += specialLeftMargin
                                canvas.drawText(
                                    expandText,
                                    currentWidth,
                                    height + expandStaticLayoutHeight,
                                    specialPaint
                                )
                                //设置其响应的区域范围
                                specialRect.set(
                                    currentWidth - specialHorizonClickMore,
                                    height - specialVerticalClickMore,
                                    currentWidth + expandStaticLayoutWidth + specialHorizonClickMore,
                                    height + expandStaticLayoutHeight + specialVerticalClickMore
                                )
                                return
                            } else {
                                //这个文字可以进行绘制操作
                                canvas.drawText(currentString, currentWidth, currentLineTmpHeight, contentPaint)
                                currentWidth += currentStringStaticLayoutWidth
                            }
                        }

                    } else {
                        height += currentLineStaticLayout.height
                        canvas.drawText(currentLineStr, 0f, height, contentPaint)
                        height += bottomMargin
                    }

                }
            }
        }
    }

    /**
     * 剔除文本中的一些特殊字符，特殊字符用空字符串来替换
     */
    fun getDealStr(text: String): String {
        if (TextUtils.isEmpty(text)) {
            return ""
        }
        var dest = ""
        val p = Pattern.compile("\\s*|\t|\r|\n")
        val m = p.matcher(text)
        dest = m.replaceAll("")
        return dest
    }

    private fun getStaticLayout(text: String, paint: TextPaint, width: Int): StaticLayout {
//        var staticLayout = StaticLayout(text, paint, width, Layout.Alignment.ALIGN_NORMAL, 1f, 0f, false)
        var staticLayout =
            StaticLayout(text, 0, text.length, paint, width, Layout.Alignment.ALIGN_NORMAL, 1f, 0f, false)
        return staticLayout
    }

    //down 时间是否在special中
    @Volatile
    var downInSpecial = false

    override fun onTouchEvent(event: MotionEvent): Boolean {
        var x = event.x
        var y = event.y
        when (event.action) {
            MotionEvent.ACTION_DOWN -> {
                if (clickListener != null) {
                    downInSpecial = specialRect.contains(x, y)
                    return true
                } else {
                    downInSpecial = false
                }
            }
            MotionEvent.ACTION_UP -> {
                if (downInSpecial) {
                    if (specialRect.contains(x, y)) {
                        //点击的特殊按钮
                        clickListener?.onSpecialTextClick(isExpand)
                        return true
                    } else {
                        //滑走了，不操作咯
                    }
                } else {
                    clickListener?.onContentTextClick()
                    return true
                }
            }
        }
        return super.onTouchEvent(event)
    }

    interface ClickListener {
        fun onContentTextClick()
        /**
         * 特殊按钮被点击是调用，
         * @param currentExpand 在点击的时候，当前的状态是否是展开状态 true->是展开的状态，false->是收起的状态
         */
        fun onSpecialTextClick(currentExpand: Boolean)
    }
}