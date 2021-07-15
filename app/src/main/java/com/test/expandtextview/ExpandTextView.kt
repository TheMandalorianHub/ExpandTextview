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
import java.util.*
import java.util.regex.Matcher
import java.util.regex.Pattern
import kotlin.math.max

/**
 *@author wangxiaochen
 *@Date: 2019-07-19
 * 关于左右布局的顺序，原生tv会根据里面绘制的内容是什么文字进行对应的省略，但是如果类似阿语和汉语混着来就会显示奇怪了，他会从中间开始分叉
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
        if (isRtl()) {
            paint.textAlign = Paint.Align.RIGHT
        } else {
            paint.textAlign = Paint.Align.LEFT
        }
        //绘制原始高度
//        paint.isElegantTextHeight=true
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
//        paint.isElegantTextHeight = true
        if (isRtl()) {
            paint.textAlign = Paint.Align.RIGHT
        } else {
            paint.textAlign = Paint.Align.LEFT
        }
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

    /**
     * 是否展示了展开状态的监听回调
     */
    var expandListener: ExpandListener? = null
        set(value) {
            field = value
            field?.expand(expandListenerFlg)
        }

    //同上面那个变量有个绑定系关系，设置之后就会优先回调一次该变量
    @Volatile
    var expandListenerFlg = false

    //展开状态下收起内容区域的宽高
    var specialShrinkWidth = -1f
    var specialShrinkHeight = -1f

    //收缩状态下展开内容区域的宽高
    var specialExpandWidth = -1f
    var specialExpandHeight = -1f

    constructor(context: Context) : this(context, null)

    constructor(context: Context, attr: AttributeSet?) : super(context, attr) {
        log.setIgnoreWithClass("ExpandTextView")
        var typeArray = context.obtainStyledAttributes(attr, R.styleable.ExpandTextView)
        if (typeArray == null) {
            return
        }
        var num = typeArray.indexCount
        for (i in 0 until num) {
            var attr = typeArray.getIndex(i)
            when (attr) {
                //主内容属性
                R.styleable.ExpandTextView_content_size -> contentTextSize =
                    typeArray.getDimension(attr, 5f)
                R.styleable.ExpandTextView_special_shrink_width -> specialShrinkWidth =
                    typeArray.getDimension(attr, -1f)
                R.styleable.ExpandTextView_special_shrink_height -> specialShrinkHeight =
                    typeArray.getDimension(attr, -1f)
                R.styleable.ExpandTextView_special_expand_width -> specialExpandWidth =
                    typeArray.getDimension(attr, -1f)
                R.styleable.ExpandTextView_special_expand_height -> specialExpandHeight =
                    typeArray.getDimension(attr, -1f)
                R.styleable.ExpandTextView_content_color -> contentTextColor =
                    typeArray.getColor(attr, Color.BLACK)
                R.styleable.ExpandTextView_content_bold -> contentBold =
                    typeArray.getBoolean(attr, false)
                //特殊内容属性
                R.styleable.ExpandTextView_special_size -> specialTextSize =
                    typeArray.getDimension(attr, 5f)
                R.styleable.ExpandTextView_special_color -> specialTextColor =
                    typeArray.getColor(attr, Color.BLACK)
                R.styleable.ExpandTextView_special_bold -> specialBold =
                    typeArray.getBoolean(attr, false)
                R.styleable.ExpandTextView_special_right_margin -> specialRightMargin =
                    typeArray.getDimension(attr, 5f)
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

                R.styleable.ExpandTextView_max_line -> maxLine =
                    typeArray.getInteger(attr, Int.MAX_VALUE)
                R.styleable.ExpandTextView_bottom_margin -> bottomMargin =
                    typeArray.getDimension(attr, 0f)
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
//        log.d("w/h:$w/$h,ow/od:$oldw/$oldh")
    }

    /**
     * 获取自己测量后的高度
     */
    fun calHeight(width: Int): Float {
        log.d("计算自身高度")
        if (TextUtils.isEmpty(currentText)) {
            log.e("当前内容为空")
            notifyExpandListener(false)
            return 0f
        }
        if (width <= 0) {
            notifyExpandListener(false)
            log.e("当前宽度为0")
            return 0f
        }
        var staticLayout = getStaticLayout(currentText, contentPaint, width)
        var lineCount = staticLayout.lineCount
        log.d("当前宽度下，当前的内容可显示行数:$lineCount")
        var height = 0f
        if (lineCount <= maxLine) {
            notifyExpandListener(false)
            for (i in 0 until lineCount) {
                //获取当前行内容
                var currentLinStr = currentText.substring(
                    staticLayout.getLineStart(i),
                    staticLayout.getLineEnd(i)
                )
                var currentLinStaticLayout = getStaticLayout(currentLinStr, contentPaint, width)
                var currentLineHeight = getStaticLayoutHeight(currentLinStaticLayout)
                height = height + currentLineHeight + bottomMargin
            }
            //统一计算的高度并不准确
//            height = getStaticLayoutHeight(staticLayout) + lineCount * bottomMargin
            log.d("无折叠展示内容总高度:$height,去除margin高度:${height - lineCount * bottomMargin}")

        } else {
            notifyExpandListener(true)
            if (isExpand) {
                log.d("计算展开状态下的高度")
                //计算展开状态下的高度
                for (i in 0 until lineCount) {
                    //获取当前行内容
                    var currentLinStr = currentText.substring(
                        staticLayout.getLineStart(i),
                        staticLayout.getLineEnd(i)
                    )
                    var currentLinStaticLayout = getStaticLayout(currentLinStr, contentPaint, width)

                    if (i == lineCount - 1) {
                        //特殊展示的内容数据
                        var shrinkStaticLayout = getStaticLayout(shrinkText, specialPaint, width)
                        var shrinkWidth = getShrinkWidth(shrinkStaticLayout)
                        //主内容最后一行的宽度
                        var currentLinStrWidth = currentLinStaticLayout.getLineWidth(0)
                        //leftmargin对于收起是生效的，但是rightmargin是不生效的
                        if ((currentLinStrWidth + specialLeftMargin + shrinkWidth) > width) {
                            log.d("需要另起一行")
                            //需要另起一行
                            height =
                                height + getStaticLayoutHeight(currentLinStaticLayout) + bottomMargin
                            height = height + getShrinkHeight(shrinkStaticLayout) + bottomMargin
                        } else {
                            //取最高的那个
                            var max = max(
                                getStaticLayoutHeight(currentLinStaticLayout),
                                getShrinkHeight(shrinkStaticLayout)
                            )
                            log.d("取较高一个高度:$max")
                            height = height + max + bottomMargin
                        }
                    } else {
                        //普通行
                        var currentLineHeight = getStaticLayoutHeight(currentLinStaticLayout)
                        height = height + currentLineHeight + bottomMargin
                        log.d("普通行文本高度为:$currentLineHeight")
                    }
                }
                log.d("计算展开状态下总高度:$height")

            } else {
                log.d("计算收起状态下的高度")

                //计算收起状态下的高度,只需要取最大行数内容就可以了
                for (i in 0 until lineCount) {
                    //获取当前行内容
                    var currentLinStr = currentText.substring(
                        staticLayout.getLineStart(i),
                        staticLayout.getLineEnd(i)
                    )
                    var currentLinStaticLayout = getStaticLayout(currentLinStr, contentPaint, width)

                    if (i == maxLine - 1) {
                        var shrinkStaticLayout = getStaticLayout(shrinkText, specialPaint, width)
                        //计算最大行那一行的内容
                        var max =
                            max(
                                getStaticLayoutHeight(currentLinStaticLayout),
                                getExpandHeight(shrinkStaticLayout)
                            )
                        height = height + max + bottomMargin
                        log.d("取较大高度:$max，该行内容:$currentLinStr")
                        return height
                    } else {
                        //普通行
                        var normalHeight = getStaticLayoutHeight(currentLinStaticLayout)
                        height = height + normalHeight + bottomMargin
                        log.d("普通行高度:$normalHeight,该行内容:$currentLinStr")
                    }
                }
                log.d("计算收起状态总显示高度:$height")
            }
        }
        return height
    }

    fun notifyExpandListener(isExpand: Boolean) {
        expandListenerFlg = isExpand
        expandListener?.expand(expandListenerFlg)
    }


    override fun onDraw(canvas: Canvas) {
        super.onDraw(canvas)
        if (isPause) {
            return
        }
        drawText(canvas)
    }


    /**
     * 获取开始的索引
     */
    fun getStartPainIndex(): Float {
        if (isRtl()) {
            return width.toFloat()
        } else {
            return 0f
        }
    }

    //布局格式是否是从右往左
    fun isRtl(): Boolean {
        if (isInEditMode) {
            return false
        } else {
            return TextUtils.getLayoutDirectionFromLocale(Locale.getDefault())== View.LAYOUT_DIRECTION_RTL;
        }
    }

    /**
     * 最终绘制文本操作的入口
     */
    fun drawText(canvas: Canvas) {
        if (TextUtils.isEmpty(currentText)) {
            drawText(
                canvas, "", 0f, 0f, contentPaint, 0
            )
            log.e("当前需要绘制的文本内容为空")
            return
        }
        var staticLayout = getStaticLayout(currentText, contentPaint, width)
        var lineCount = staticLayout.lineCount
        if (lineCount <= maxLine) {
            //没有特殊内容的展示
            var height = 0f
            for (i in 0 until lineCount) {
                var currentLineStr =
                    currentText.substring(staticLayout.getLineStart(i), staticLayout.getLineEnd(i))
                var currentLineStaticLayout = getStaticLayout(currentLineStr, contentPaint, width)
                var currentLineHeight = getStaticLayoutHeight(currentLineStaticLayout)
                drawText(
                    canvas,
                    getTmpDealText(currentLineStr),
                    getStartPainIndex(),
                    height + getDrawTextY(contentPaint, currentLineHeight),
                    contentPaint,
                    currentLineHeight
                )
                height += currentLineHeight
                height += bottomMargin
            }
        } else {
            //包含特殊内容的展示
            var height = 0f
            if (isExpand) {
                //当前状态为展开状态，绘制展开状态内容
                var shrinkStaticLayout = getStaticLayout(shrinkText, specialPaint, width)
                var shrinkStaticLayoutWidth = getShrinkWidth(shrinkStaticLayout)
                var shrinkStaticLayoutHeight = getShrinkHeight(shrinkStaticLayout)
                for (i in 0 until lineCount) {
                    //获取当前行内容
                    var currentLineStr = currentText.substring(
                        staticLayout.getLineStart(i),
                        staticLayout.getLineEnd(i)
                    )
                    var currentLineStaticLayout =
                        getSingleLineTextStaticLayout(currentLineStr, contentPaint, width)
                    if (i == lineCount - 1) {
                        //最后一行特殊处理
                        var currentLineWidth = currentLineStaticLayout.getLineWidth(0)
                        if ((currentLineWidth + specialLeftMargin + shrinkStaticLayoutWidth) > width) {
                            //展开内容需要换行处理
                            //绘制主内容
                            var currentHeight = getStaticLayoutHeight(currentLineStaticLayout)

                            drawText(
                                canvas, getTmpDealText(currentLineStr),
                                getStartPainIndex(),
                                height + getDrawTextY(contentPaint, currentHeight),
                                contentPaint,
                                currentHeight
                            )
                            height = height + currentHeight
                            height += bottomMargin
                            //绘制特殊内容
                            specialRect.set(
                                getStartPainIndex(),
                                height - specialVerticalClickMore,
                                if (getStartPainIndex() == 0f) {
                                    //从左开始的可以往右算
                                    shrinkStaticLayoutWidth + specialHorizonClickMore
                                } else {
                                    //从右往左的，需要做减法
                                    getStartPainIndex() - shrinkStaticLayoutWidth - specialHorizonClickMore
                                },
                                height + shrinkStaticLayoutHeight + specialVerticalClickMore
                            )
                            drawText(
                                canvas,
                                shrinkText,
                                getStartPainIndex(),
                                height + getDrawTextY(specialPaint, shrinkStaticLayoutHeight),
                                specialPaint,
                                shrinkStaticLayoutHeight
                            )
                            height = height + shrinkStaticLayoutHeight
                            log.d("当前最后一行最底部距离:${height + bottomMargin}")
                        } else {
                            //都绘制在同一行
                            //绘制主内容
                            var currentWidth = getStartPainIndex()
                            var currentHeight = getStaticLayoutHeight(currentLineStaticLayout)
                            drawText(
                                canvas, getTmpDealText(currentLineStr),
                                currentWidth,
                                height + getDrawTextY(contentPaint, currentHeight),
                                contentPaint,
                                currentHeight
                            )
                            //左右对换，需要相反
                            if (getStartPainIndex() == 0f) {
                                currentWidth += currentLineWidth
                                //绘制特殊内容
                                currentWidth += specialLeftMargin
                            } else {
                                currentWidth -= currentLineWidth
                                //绘制特殊内容
                                currentWidth -= specialLeftMargin
                            }
                            drawText(
                                canvas, shrinkText,
                                currentWidth,
                                height + getDrawTextY(specialPaint, shrinkStaticLayoutHeight),
                                specialPaint,
                                shrinkStaticLayoutHeight
                            )

                            log.d("当前最后一行最底部距离:${height + shrinkStaticLayoutHeight + bottomMargin}")
                            if (getStartPainIndex() == 0f) {
                                specialRect.set(
                                    currentWidth - specialHorizonClickMore,
                                    height - specialVerticalClickMore,
                                    currentWidth + shrinkStaticLayoutWidth + specialVerticalClickMore,
                                    height + shrinkStaticLayoutHeight + specialVerticalClickMore
                                )
                            } else {
                                specialRect.set(
                                    currentWidth + specialHorizonClickMore,
                                    height - specialVerticalClickMore,
                                    currentWidth - shrinkStaticLayoutWidth - specialVerticalClickMore,
                                    height + shrinkStaticLayoutHeight + specialVerticalClickMore
                                )
                            }

                        }
                        return
                    } else {
                        var currentHeight = getStaticLayoutHeight(currentLineStaticLayout)
                        drawText(
                            canvas,
                            getTmpDealText(currentLineStr),
                            getStartPainIndex(),
                            height + getDrawTextY(contentPaint, currentHeight),
                            contentPaint,
                            currentHeight
                        )
                        height = height + currentHeight
                        height += bottomMargin
                    }
                }
            } else {
                //当前为收起状态，绘制收起状态内容
                //收起状态多了前缀，前缀需要特殊处理
                var expandStaticLayout = getStaticLayout(expandText, specialPaint, width)
                var expandStaticLayoutHeight = getExpandHeight(expandStaticLayout)
                var expandStaticLayoutWidth = getExpandWidth(expandStaticLayout)
                var height = 0f
                for (i in 0 until lineCount) {
                    //获取当前行内容
                    var currentLineStr = currentText.substring(
                        staticLayout.getLineStart(i),
                        staticLayout.getLineEnd(i)
                    )
                    var currentLineStaticLayout =
                        getSingleLineTextStaticLayout(currentLineStr, contentPaint, width)
                    if (i == maxLine - 1) {
                        //在最大一行特殊处理
                        //需要计算前缀的内容信息
                        var expandPrifixStaticLayout =
                            getStaticLayout(expandPrifix, contentPaint, width)
                        var expandPrifixStaticLayoutWidth = expandPrifixStaticLayout.getLineWidth(0)

                        //主内容可以进行绘制的宽度内容
                        var leftWidth =
                            width - expandStaticLayoutWidth - specialRightMargin - specialLeftMargin - expandPrifixStaticLayoutWidth
                        //当前正在绘制的宽内容
                        var currentWidth = 0f
                        //当前应该从什么位置开始绘制
                        var currentStartIndex = getStartPainIndex()
                        //临时的一个高度，表达当前主内容的y值
                        var currentHeight = getStaticLayoutHeight(currentLineStaticLayout)
                        var currentLineTmpHeight = height + currentHeight
                        if (true) {
                            //最后一行文本的宽度
                            var lastLineWidth = currentLineStaticLayout.getLineWidth(0)
                            var newContentStr = currentLineStr
                            while (lastLineWidth > leftWidth) {
//                                log.d("lastW:$lastLineWidth,left:$leftWidth,超标，将会进行缩减,上次内容：$newContentStr")
                                //对文字进行缩减操作,包头不包围，去除最后一个文字
                                newContentStr = newContentStr.substring(0, newContentStr.length - 1)
                                currentLineStaticLayout =
                                    getSingleLineTextStaticLayout(newContentStr, contentPaint, width)
                                lastLineWidth = currentLineStaticLayout.getLineWidth(0)
                            }
//                            log.d("缩减完毕:$lastLineWidth")
                            //缩减够了，只绘制缩减后的文字内容
                            drawText(
                                canvas,
                                getTmpDealText(newContentStr),
                                currentStartIndex,
                                height + getDrawTextY(contentPaint, currentHeight),
                                contentPaint,
                                currentHeight
                            )
                            //增加宽度
                            currentWidth += lastLineWidth
                            if (getStartPainIndex() == 0f) {
                                currentStartIndex += lastLineWidth
                            } else {
                                currentStartIndex -= lastLineWidth
                            }
                            //绘制冒号前缀
                            drawText(
                                canvas, expandPrifix,
                                currentStartIndex,
                                height + getDrawTextY(contentPaint, currentHeight),
                                contentPaint,
                                currentHeight
                            )
                            currentWidth += expandPrifixStaticLayoutWidth
                            //绘制特殊内容区域信息
                            currentWidth += specialLeftMargin
                            //俩极反转   //绘制特殊内容区域信息
                            if (getStartPainIndex() == 0f) {
                                currentStartIndex += expandPrifixStaticLayoutWidth
                                currentStartIndex += specialLeftMargin
                            } else {
                                currentStartIndex -= expandPrifixStaticLayoutWidth
                                currentStartIndex -= specialLeftMargin
                            }
                            //绘制尾缀提示
                            drawText(
                                canvas, expandText,
                                currentStartIndex,
                                height + getDrawTextY(specialPaint, expandStaticLayoutHeight),
                                specialPaint,
                                expandStaticLayoutHeight
                            )
                            //设置尾缀点击响应范围
                            if (getStartPainIndex() == 0f) {
                                //设置其响应的区域范围
                                specialRect.set(
                                    currentWidth - specialHorizonClickMore,
                                    height - specialVerticalClickMore,
                                    currentWidth + expandStaticLayoutWidth + specialHorizonClickMore,
                                    height + expandStaticLayoutHeight + specialVerticalClickMore
                                )
                            } else {
                                //设置其响应的区域范围
                                specialRect.set(
                                    currentWidth + specialHorizonClickMore,
                                    height - specialVerticalClickMore,
                                    currentWidth - expandStaticLayoutWidth - specialHorizonClickMore,
                                    height + expandStaticLayoutHeight + specialVerticalClickMore
                                )
                            }

                            return
                        } else {
                            //下面这种方式不再推荐，一个个独立的去绘制的话阿语绘制会产生问题，会变大，导致size不同
                            for (i in currentLineStr) {
                                var currentString = i.toString()
                                var currentStringStaticLayout =
                                    getStaticLayout(currentString, contentPaint, width)
                                var currentStringStaticLayoutWidth =
                                    currentStringStaticLayout.getLineWidth(0)
                                if ((currentWidth + currentStringStaticLayoutWidth) > leftWidth) {
                                    //这个文字不被继续绘制了，开始绘制特殊内容
                                    //绘制前缀信息
                                    drawText(
                                        canvas, expandPrifix,
                                        currentStartIndex,
                                        currentLineTmpHeight,
                                        contentPaint,
                                        currentHeight
                                    )
                                    currentWidth += expandPrifixStaticLayoutWidth
                                    //绘制特殊内容区域信息
                                    currentWidth += specialLeftMargin
                                    //俩极反转   //绘制特殊内容区域信息
                                    if (getStartPainIndex() == 0f) {
                                        currentStartIndex += expandPrifixStaticLayoutWidth
                                        currentStartIndex += specialLeftMargin
                                    } else {
                                        currentStartIndex -= expandPrifixStaticLayoutWidth
                                        currentStartIndex -= specialLeftMargin
                                    }
                                    drawText(
                                        canvas, expandText,
                                        currentStartIndex,
                                        height + expandStaticLayoutHeight,
                                        specialPaint,
                                        expandStaticLayoutHeight
                                    )

                                    if (getStartPainIndex() == 0f) {
                                        //设置其响应的区域范围
                                        specialRect.set(
                                            currentWidth - specialHorizonClickMore,
                                            height - specialVerticalClickMore,
                                            currentWidth + expandStaticLayoutWidth + specialHorizonClickMore,
                                            height + expandStaticLayoutHeight + specialVerticalClickMore
                                        )
                                    } else {
                                        //设置其响应的区域范围
                                        specialRect.set(
                                            currentWidth + specialHorizonClickMore,
                                            height - specialVerticalClickMore,
                                            currentWidth - expandStaticLayoutWidth - specialHorizonClickMore,
                                            height + expandStaticLayoutHeight + specialVerticalClickMore
                                        )
                                    }

                                    return
                                } else {
                                    //这个文字可以进行绘制操作
                                    drawText(
                                        canvas, currentString,
                                        currentStartIndex,
                                        currentLineTmpHeight,
                                        contentPaint,
                                        currentHeight
                                    )
                                    currentWidth += currentStringStaticLayoutWidth
                                    if (getStartPainIndex() == 0f) {
                                        currentStartIndex += currentStringStaticLayoutWidth
                                    } else {
                                        currentStartIndex -= currentStringStaticLayoutWidth
                                    }
                                }
                            }
                        }


                    } else {
                        var currentHeight = getStaticLayoutHeight(currentLineStaticLayout)
                        drawText(
                            canvas,
                            getTmpDealText(currentLineStr),
                            getStartPainIndex(),
                            height + getDrawTextY(contentPaint, currentHeight),
                            contentPaint,
                            currentHeight
                        )
                        height += currentHeight
                        height += bottomMargin
                    }

                }
            }
        }
    }

    /**
     * 只绘制单行文本
     */
    private fun drawText(
        canvas: Canvas,
        text: String,
        x: Float,
        y: Float,
        paint: Paint,
        textHeight: Int
    ) {
        canvas.drawText(text, x, y, paint)
//        canvas.drawTextRun(text,0,text.length,0,text.length,x,y,false,paint)
    }

    /**
     * 剔除文本中的一些特殊字符，特殊字符用空字符串来替换
     */
    fun getDealStr(text: String? = null): String {
        //不来屏蔽了，阿语里默认的会有一些奇怪的字符
//        if (true) {
//            return text ?: ""
//        }
        if (TextUtils.isEmpty(text)) {
            return ""
        }
        var dest = ""
//        val p = Pattern.compile("\\s*|\t|\r|\n")
//        val m = p.matcher(text)
//        dest = m.replaceAll("")

        //多个换行只取一个,实际上是如果超过俩个换行符，会指定为俩个
        val p = Pattern.compile("(\r?\n(\\s*\r?\n)+)") //正则表达式
        val m: Matcher = p.matcher(text)
        dest = m.replaceAll("\r\n\r\n")
        return dest
    }

    //获取当前绘制文字的一行的基线y的值
    //参考链接https://juejin.cn/post/6844903532835897351 绘制时需要计算绘制的基线位置
    fun getDrawTextY(paint: Paint, y: Int): Int {
        val fontMetricsInt = paint.getFontMetricsInt()
        var dy = (fontMetricsInt.bottom - fontMetricsInt.top) / 2 - fontMetricsInt.bottom
        dy = y / 2 + dy

        return dy
    }

    private fun getTmpDealText(text: String): String {
        return text.trimEnd()
//        return if (isRtl()){text.trimStart()}else{text.trimEnd()}
    }
    private fun getSingleLineTextStaticLayout(text: String, paint: TextPaint, width: Int): StaticLayout {
//        val p = Pattern.compile("\r\n") //正则表达式
//        val m: Matcher = p.matcher(text)
//        var dealText = m.replaceAll("")
        var dealText = getTmpDealText(text)
        var staticLayout =
            StaticLayout(
                dealText,
                0,
                dealText.length,
                paint,
                width,
                Layout.Alignment.ALIGN_NORMAL,
                1f,
                0f,
                true
            )
        return staticLayout
    }

    private fun getStaticLayout(text: String, paint: TextPaint, width: Int): StaticLayout {
//        var staticLayout = StaticLayout(text, paint, width, Layout.Alignment.ALIGN_NORMAL, 1f, 0f, false)
        //需要trim一下，不然如果后面有带换行符号的话绘制的时候不会换行，但是高度的计算会出现问题
        var dealText=text.trim()
        var staticLayout =
            StaticLayout(
                dealText,
                0,
                dealText.length,
                paint,
                width,
                Layout.Alignment.ALIGN_NORMAL,
                1f,
                0f,
                true
            )
//        var staticLayout = StaticLayout.Builder.obtain(text, 0, text.length, paint, width).build()
        return staticLayout
    }

    //统一获取staticlayout的包裹文字高度
    fun getStaticLayoutHeight(staticLayout: StaticLayout): Int {
        var height = staticLayout.height
        log.d("获取到的height:$height")
        return height
    }

    /**
     * 展开状态下 收缩内容的宽度
     */
    private fun getShrinkWidth(staticLayout: StaticLayout): Float {
        if (specialShrinkWidth != -1f) {
            return specialShrinkWidth
        }
        return staticLayout.getLineWidth(0)
    }


    /**
     * 展开状态下 收缩内容的高度
     */
    private fun getShrinkHeight(staticLayout: StaticLayout): Int {
        if (specialShrinkHeight != -1f) {
            return specialShrinkHeight.toInt()
        }
        return getStaticLayoutHeight(staticLayout)
    }

    /**
     * 收起状态下 展开内容的宽度
     */
    private fun getExpandWidth(staticLayout: StaticLayout): Float {
        if (specialExpandWidth != -1f) {
            return specialExpandWidth
        }
        return staticLayout.getLineWidth(0)
    }

    /**
     * 收起状态下 展开内容的高度
     */
    private fun getExpandHeight(staticLayout: StaticLayout): Int {
        if (specialExpandHeight != -1f) {
            return specialExpandHeight.toInt()
        }
        return getStaticLayoutHeight(staticLayout)
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

    /**
     * 如果是展现了收缩展开的状态，则会回调该监听进行通知，一般应用里只要在意监听一次就够了
     */
    interface ExpandListener {
        fun expand(isExpand: Boolean)
    }
}