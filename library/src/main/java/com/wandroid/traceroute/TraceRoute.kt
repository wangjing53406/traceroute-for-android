package com.wandroid.traceroute

import android.os.Handler
import android.os.Looper

/**
 * traceroute on android with JNI.
 *
 * Created by wangjing on 2018/8/29.
 */
object TraceRoute {

    init {
        System.loadLibrary("traceroute")
    }

    /**
     * record traceroute messages
     */
    private var result: StringBuilder? = null

    /**
     * traceroute callback to user
     */
    private var callback: TraceRouteCallback? = null

    /**
     * to switch thread to android main thread
     */
    private var handler: Handler = Handler(Looper.getMainLooper())

    /**
     * keep this method to use with java
     *
     * @param callback The callback that will run
     */
    fun setCallback(callback: TraceRouteCallback?) {
        this.callback = callback
    }

    /**
     * this method is for kotlin simplify code
     *
     * @param traceRouteCallback The callback that will run
     */
    fun setCallback(traceRouteCallback: SimpleTraceRouteCallback.() -> Unit) {
        val simpleCallback = SimpleTraceRouteCallback()
        simpleCallback.traceRouteCallback()
        setCallback(simpleCallback)
    }

    /**
     * call clearResult from jni, don't confusion this method
     */
    fun clearResult() {
        result = null
    }

    /**
     * call appendResult from jni, don't confusion this method
     *
     * @param text current traceroute message
     */
    fun appendResult(text: String) {
        if (result == null) {
            result = StringBuilder()
        }
        result?.append(text)
        if (callback != null) {
            handler.post { callback?.onUpdate(text) }
        }
    }

    /**
     * traceroute with hostname
     *
     * @param hostname traceroute's hostname
     * @param async synchronous or asynchronous execution
     * @return TraceRouteResult
     */
    @Synchronized
    fun traceRoute(hostname: String, async: Boolean = false): TraceRouteResult? {
        val args = arrayOf("traceroute", hostname)
        if (async) {
            Thread({
                traceRoute(args)
            }, "trace_route_thread").start()
        } else {
            return traceRoute(args)
        }
        return null
    }

    /**
     * traceroute with commands args
     *
     * @param args traceroute commands args
     * @return TraceRouteResult
     */
    @Synchronized
    fun traceRoute(args: Array<String>): TraceRouteResult {
        val traceRouteResult = TraceRouteResult.instance()
        traceRouteResult.code = execute(args)
        if (traceRouteResult.code == 0) {
            traceRouteResult.message = result.toString()
            handler.post { callback?.onSuccess(traceRouteResult) }
        } else {
            traceRouteResult.message = "execute traceroute failed."
            handler.post {
                callback?.onFailed(traceRouteResult.code, traceRouteResult.message)
            }
        }
        return traceRouteResult
    }

    /**
     * JNI interface.
     *
     * @param args traceroute commands args
     * @return execute result code
     */
    external fun execute(args: Array<String>): Int

}

/**
 * TracerouteResult data class
 */
data class TraceRouteResult(var code: Int, var message: String) {

    companion object {

        fun instance(): TraceRouteResult = TraceRouteResult(-1, "")

    }

}

/**
 * traceroute callback
 */
interface TraceRouteCallback {

    /**
     * traceroute success
     *
     * @param traceRouteResult get traceroute result status. code is 0 for success.
     */
    fun onSuccess(traceRouteResult: TraceRouteResult)

    /**
     * callback when tracerouting
     *
     * @param text current traceroute message
     */
    fun onUpdate(text: String)

    /**
     * traceroute failed
     *
     * @param code execute code. Nonzero is failure
     * @param reason Failure explanation
     */
    fun onFailed(code: Int, reason: String)

}

/**
 * wrapper class for simple use with kotlin
 */
class SimpleTraceRouteCallback : TraceRouteCallback {

    private var _onSuccess: ((traceRouteResult: TraceRouteResult) -> Unit)? = null

    private var _onUpdate: ((text: String) -> Unit)? = null

    private var _onFailed: ((code: Int, reason: String) -> Unit)? = null

    /**
     * wrap for onSuccess
     *
     * @param traceRouteResult get traceroute result status. code is 0 for success.
     */
    fun success(success: (traceRouteResult: TraceRouteResult) -> Unit) {
        _onSuccess = success
    }

    /**
     * traceroute success
     *
     * @param traceRouteResult get traceroute result status. code is 0 for success.
     */
    override fun onSuccess(traceRouteResult: TraceRouteResult) {
        _onSuccess?.invoke(traceRouteResult)
    }

    /**
     * wrap for onUpdate
     *
     * param text current traceroute message
     */
    fun update(update: (text: String) -> Unit) {
        _onUpdate = update
    }

    /**
     * callback when tracerouting
     *
     * @param text current traceroute message
     */
    override fun onUpdate(text: String) {
        _onUpdate?.invoke(text)
    }

    /**
     * wrap for onFailed
     *
     * @param code execute code. Nonzero is failure
     * @param reason Failure explanation
     */
    fun failed(failed: (code: Int, reason: String) -> Unit) {
        _onFailed = failed
    }

    /**
     * traceroute failed
     *
     * @param code execute code. Nonzero is failure
     * @param reason Failure explanation
     */
    override fun onFailed(code: Int, reason: String) {
        _onFailed?.invoke(code, reason)
    }

}