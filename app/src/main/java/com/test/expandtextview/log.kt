package com.test.expandtextview

import android.util.Log
import java.util.*

/**
 *@author wangxiaochen
 *@Date: 2019/3/30
 * 日志调试输出类,支持类名+methodName+lineNum输出
 * 还要考虑下tag名称过长的问题
 */
object log {

    /**
     * 全局控制是否输出日志
     */
    var isDebug = true
    /**
     * 针对某个类进行管控的统一配置集合
     * 支持仅针对某个类的输出的日志不进行输出,方便与对某个类进行调试
     */
    @JvmStatic
    private var configMap = HashMap<String, Boolean>()

    /**
     * 针对单独的某个类进行设置是否需要输出该日志的类信息
     * 如果调用设置了一般都是想屏蔽的
     * @param isOut ->false 不可以输出 true->可以进行输出
     */
    @JvmStatic
    fun setIgnoreWithClass(clazzName: String, isOut: Boolean = false) {
        Log.d("${javaClass.`package`.name}:", "log->setIgnoreWithClass,class:$clazzName 的日志输出状态被设置为:$isOut")
        configMap[clazzName] = isOut
    }

    @JvmStatic
    fun d(msg: String) {
        if (isDebug) {
            //之前设置过特殊内容
            if (configMap.size > 0) {
                if (getClassNameConfigValue(getClassName())){
                    Log.d(getMsgOfLog(), msg)
                }
            } else {
                //没有特殊进行拦截的内容
                Log.d(getMsgOfLog(), msg)
            }
        }
    }

    /**
     * 在子类中的调用,类名可能是 `ChoosePicView$startAnimator$1` 这样的形式,一般我们屏蔽一个类的日志输出后,内部类的类名同样需要进行屏蔽
     * 判断调用类是否被屏蔽了日志输出
     * @return true ->可以进行日志输出
     *         false ->不可以进行日志输出
     *         默认是可以进行输出的
     *         ChoosePicView$startAnimator$1
     */
    fun getClassNameConfigValue(clazzName: String):Boolean {
        for (key in configMap.keys) {
            if (key.equals(clazzName.split("$").first())) {
                //包含这个key值,直接返回对应的结果
                return configMap[key]!!
            }
        }
        return true
    }

    @JvmStatic
    fun w(msg: String) {
        if (isDebug) {
            //之前设置过特殊内容
            if (configMap.size > 0) {
                if (getClassNameConfigValue(getClassName())){
                    Log.w(getMsgOfLog(), msg)
                }
            }else {
                //没有特殊进行拦截的内容
                Log.d(getMsgOfLog(), msg)
            }
        }
    }

    @JvmStatic
    fun i(msg: String) {
        if (isDebug) {
            //之前设置过特殊内容
            if (configMap.size > 0) {
                if (getClassNameConfigValue(getClassName())){
                    Log.i(getMsgOfLog(), msg)
                }
            }else {
                //没有特殊进行拦截的内容
                Log.d(getMsgOfLog(), msg)
            }
        }
    }

    @JvmStatic
    fun e(msg: String) {
        if (isDebug) {
            //之前设置过特殊内容
            if (configMap.size > 0) {
                if (getClassNameConfigValue(getClassName())){
                    Log.e(getMsgOfLog(), msg)
                }
            } else {
                //没有特殊进行拦截的内容
                Log.e(getMsgOfLog(), msg)
            }
        }
    }

    /**
     * 获取调用当前线度的类名
     */
    private fun getClassName(): String {
        val trace = Thread.currentThread().stackTrace
        var className = "unknowClassName"

        //根据stack 层级+调用log的层级,需要向上偏移俩个单位
        for (i in 0 until trace.size) {
            if ("getClassName".equals(trace[i].methodName)) {
                //获取到当前方法的执行栈具体位置,上一次是D,在上一层是具体的调用位置
                var truelyTrace = trace[i + 2]
                className = truelyTrace.className.split(".").last()
                break
            }
        }
        if ("unknowClassName".equals(className)) {
            loge("getClassName->未找到调用方法的具体栈位信息!")
        }
        return className
    }

    private fun getMsgOfLog(): String {
        //获取当前方法执行堆栈
        val trace = Thread.currentThread().stackTrace
        var className = "unknowClassName"
        var methodName = "unknowClassName"
        var lineNumber = "unknowLineNumber"

        //根据stack 层级+调用log的层级,需要向上偏移俩个单位
        for (i in 0 until trace.size) {
            if ("getMsgOfLog".equals(trace[i].methodName)) {
                //获取到当前方法的执行栈具体位置,上一次是D,在上一层是具体的调用位置
                var truelyTrace = trace[i + 2]
                className = truelyTrace.className.split(".").last()
                methodName = truelyTrace.methodName
                lineNumber = truelyTrace.lineNumber.toString()
                break
            }
        }
        if ("unknowClassName".equals(className)) {
            loge("getMsgOfLog->未找到调用方法的栈位信息")
        }

        return "$className->$methodName.$lineNumber"
    }

    /**
     * 本类的错误信息调试
     */
    private fun loge(msg: String) {
        Log.e("log->loge", msg)
    }

}