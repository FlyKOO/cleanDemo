package com.example.cleanup.work

import android.content.Context
import android.net.ConnectivityManager
import android.net.Network
import android.net.NetworkRequest
import com.example.cleanup.utils.RsaUtil
import kotlinx.coroutines.*
import okhttp3.*
import okio.ByteString
import com.google.gson.Gson
import java.net.URLEncoder
import java.util.Locale
import java.util.TimeZone
import java.util.concurrent.ConcurrentLinkedQueue
import java.util.concurrent.atomic.AtomicBoolean
import kotlin.math.min

data class EventBody(
    val affair_tag: Int,
    val meta: Int? = null,
    val tagalong: String? = null  // 额外参数
)

object Ev : WebSocketListener() {
    private var buildedUrl: String = ""
    private val gson = Gson()
    private lateinit var androidId: String

    private val okHttp by lazy {
        OkHttpClient.Builder()
//            .pingInterval(Duration) // 心跳 Duration.ofSeconds(20)
            .build()
    }

    private var webSocket: WebSocket? = null
    private val connecting = AtomicBoolean(false)
    private val isOpen = AtomicBoolean(false)
    private val sendQueue = ConcurrentLinkedQueue<String>()

    private var scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)
    private var reconnectJob: Job? = null
    private var reconnectAttempt = 0
    private var netType = ""
    private val event = "incident"
    private val pem = "-----BEGIN PUBLIC KEY-----\n" +
            "MIIBIjANBgkqhkiG9w0BAQEFAAOCAQ8AMIIBCgKCAQEA6aLkHRsjzSeN9GNVYujI\n" +
            "W+DMFIGAXofhH8uhrfGUJ6dsCOSSQ3CJb2bSO0g6oeDFVpBK09uoflsJA1GUa3qb\n" +
            "j2If/DFF22RlP0PDD9E26GMuyqV7s4l4RGpBR9Bl98DHnaDOR7yl4OIOpQPOZZgX\n" +
            "NDpNo3g4wDIRbY9Giacs4B7p4iaFflkNqvW9uc3FaMq+7ldNuFSBTKW3UAm2N4bo\n" +
            "xshqgEyA7NkyORgpimC4VoSRtmCdDQJsymTf2F8p4aTwelPDXdy/pGZmK6BgCfMm\n" +
            "g9RhC1h5SbKfXmQu8rjhcN2sVJbr7l3c31MTgKXsgbF+BQWq3iVD5HT9p/koWFcd\n" +
            "6QIDAQAB\n" +
            "-----END PUBLIC KEY-----\n".trimIndent()

    fun init(context: Context, serverBaseWsUrl: String, androidID: String, netT: String) {
        netType = netT
        androidId = androidID
//        appCtx = context.applicationContext
        buildedUrl =
            buildUrl(context, serverBaseWsUrl.trimEnd('/')) // e.g. ws://10.0.3.53:9029/socket.io
        registerNetworkCallback(context.applicationContext)
        connect()
    }

    fun sendEvent(eventId: Int, kv: Int? = 1, extra: String? = null) {
        var extracted = ""
        if (extra != null && extra != "") {
            if (pem.isEmpty()) {
                return
            }
            extracted = RsaUtil.encryptToBase64(pem, extra.toByteArray())
        }
        val payload0 = gson.toJson(EventBody(eventId, kv, extracted))
        val payload = "42[\"${event}\",$payload0]"
        println("sendEvent:$payload")
        enqueueOrSend(payload)
    }

    private fun buildUrl(ctx: Context, wsbaseUrl: String): String {
        val qs = listOfNotNull(
            "sys_uid" to androidId,
            "client_seq" to ctx.packageManager.getPackageInfo(
                ctx.packageName,
                0
            ).versionCode.toString(),
            "wire_flag" to netType,
            "parole" to Locale.getDefault().language,
            "station" to "google",
            "sender_url" to "",
            "parcel_name" to ctx.packageName,
            "local_zone" to TimeZone.getDefault().id
        ).joinToString("&") { (k, v) -> "${k}=${URLEncoder.encode(v, "UTF-8")}" }
        return "$wsbaseUrl/socket.io/?$qs"
    }

    private fun connect() {
        println("connect")
        if (connecting.getAndSet(true)) return
        val req = Request.Builder().url(buildedUrl).build() //ws://testapi4.ta2wan.com/socket.io/
        webSocket = okHttp.newWebSocket(req, this)
    }

    private fun enqueueOrSend(text: String) {
        if (isOpen.get()) {
            webSocket?.send(text)
        } else {
            sendQueue.add(text)
        }
    }

    private fun flushQueue() {
        while (true) {
            val txt = sendQueue.poll() ?: break
            webSocket?.send(txt)
        }
    }

    private fun scheduleReconnect() {
        println("scheduleReconnect")
        if (reconnectJob?.isActive == true) return
        reconnectJob = scope.launch {
            val delayMs = (1000L * listOf(1, 2, 5, 10, 20, 30).getOrElse(reconnectAttempt) { 30 })
            delay(delayMs)
            reconnectAttempt = min(reconnectAttempt + 1, 5)
            connect()
        }
    }

    private fun resetBackoff() {
        reconnectAttempt = 0
        reconnectJob?.cancel()
        reconnectJob = null
    }

    // ---- WebSocketListener ----
    override fun onOpen(webSocket: WebSocket, response: Response) {
        println("onOpen")
        isOpen.set(true)
        connecting.set(false)
        resetBackoff()
        flushQueue()
    }

    override fun onMessage(webSocket: WebSocket, text: String) {

    }

    override fun onMessage(webSocket: WebSocket, bytes: ByteString) {

    }

    override fun onClosing(webSocket: WebSocket, code: Int, reason: String) {
        println("onClosing")
        isOpen.set(false)
        webSocket.close(1000, null)
    }

    override fun onClosed(webSocket: WebSocket, code: Int, reason: String) {
        println("onClosed")
        isOpen.set(false)
        connecting.set(false)
        scheduleReconnect()
    }

    override fun onFailure(webSocket: WebSocket, t: Throwable, response: Response?) {
        println("onFailure")
        isOpen.set(false)
        connecting.set(false)
        scheduleReconnect()
    }

    // 网络变化自动重连
    private fun registerNetworkCallback(appCtx: Context) {
        val cm = appCtx.getSystemService(Context.CONNECTIVITY_SERVICE) as ConnectivityManager
        val req = NetworkRequest.Builder().build()
        cm.registerNetworkCallback(req, object : ConnectivityManager.NetworkCallback() {
            override fun onAvailable(network: Network) {
                // 网络恢复时尝试重连
                scheduleReconnect()
            }
        })
    }
}

fun EVENT_50(kv: Int? = 1, extra: String? = null) //应用通知
{
    Ev.sendEvent(50, kv, extra)
}

fun EVENT_2(kv: Int? = 1, extra: String? = null) //打开app
{
    Ev.sendEvent(2, kv, extra)
}

fun EVENT_3(kv: Int? = 1, extra: String? = null) //点击一键智能清理
{
    Ev.sendEvent(3, kv, extra)
}

fun EVENT_4(kv: Int? = 1, extra: String? = null) //点击应用管理
{
    Ev.sendEvent(4, kv, extra)
}

fun EVENT_5(kv: Int? = 1, extra: String? = null) //点击大文件扫描
{
    Ev.sendEvent(5, kv, extra)
}

fun EVENT_6(kv: Int? = 1, extra: String? = null) //点击清理历史记录
{
    Ev.sendEvent(6, kv, extra)
}

fun EVENT_7(kv: Int? = 1, extra: String? = null) //点击扫描
{
    Ev.sendEvent(7, kv, extra)
}

fun EVENT_8(kv: Int? = 1, extra: String? = null) //点击设置
{
    Ev.sendEvent(8, kv, extra)
}

fun EVENT_9(kv: Int? = 1, extra: String? = null) //应用管理-切换分类
{
    Ev.sendEvent(9, kv, extra)
}

fun EVENT_10(kv: Int? = 1, extra: String? = null) //应用管理-开关显示系统应用
{
    Ev.sendEvent(10, kv, extra)
}

fun EVENT_11(kv: Int? = 1, extra: String? = null) //应用管理-点击清除缓存
{
    Ev.sendEvent(11, kv, extra)
}

fun EVENT_12(kv: Int? = 1, extra: String? = null) //应用管理-点击卸载
{
    Ev.sendEvent(12, kv, extra)
}

fun EVENT_13(kv: Int? = 1, extra: String? = null) //大文件扫描-切换分类
{
    Ev.sendEvent(13, kv, extra)
}

fun EVENT_14(kv: Int? = 1, extra: String? = null) //清理历史记录-切换分类
{
    Ev.sendEvent(14, kv, extra)
}

fun EVENT_15(kv: Int? = 1, extra: String? = null) //清理历史记录-删除记录
{
    Ev.sendEvent(15, kv, extra)
}

fun EVENT_16(kv: Int? = 1, extra: String? = null) //设置-切换语言
{
    Ev.sendEvent(16, kv, extra)
}

fun EVENT_17(kv: Int? = 1, extra: String? = null) //设置-开关自动清理
{
    Ev.sendEvent(17, kv, extra)
}

fun EVENT_18(kv: Int? = 1, extra: String? = null) //设置-点击自动清理设置
{
    Ev.sendEvent(18, kv, extra)
}

fun EVENT_19(kv: Int? = 1, extra: String? = null) //设置-点击定时清理
{
    Ev.sendEvent(19, kv, extra)
}

fun EVENT_20(kv: Int? = 1, extra: String? = null) //定时清理-点击添加新的定时清理任务
{
    Ev.sendEvent(20, kv, extra)
}

fun EVENT_21(kv: Int? = 1, extra: String? = null) //设置-点击应用管理权限
{
    Ev.sendEvent(21, kv, extra)
}

fun EVENT_22(kv: Int? = 1, extra: String? = null) //设置-开关通知
{
    Ev.sendEvent(22, kv, extra)
}

fun EVENT_23(kv: Int? = 1, extra: String? = null) //设置-开关安全模式
{
    Ev.sendEvent(23, kv, extra)
}

fun EVENT_24(kv: Int? = 1, extra: String? = null) //设置-点击关于
{
    Ev.sendEvent(24, kv, extra)
}

fun EVENT_25(kv: Int? = 1, extra: String? = null) //app在前台运行30秒
{
    Ev.sendEvent(25, kv, extra)
}


// 50               应用通知
// 2                打开app
// 3                点击一键智能清理
// 4                点击应用管理
// 5                点击大文件扫描
// 6                点击清理历史记录
// 7                点击扫描
// 8                点击设置
// 9                应用管理-切换分类
// 10               应用管理-开关显示系统应用
// 11               应用管理-点击清除缓存
// 12               应用管理-点击卸载
// 13               大文件扫描-切换分类
// 14               清理历史记录-切换分类
// 15               清理历史记录-删除记录
// 16               设置-切换语言
// 17               设置-开关自动清理
// 18               设置-点击自动清理设置
// 19               设置-点击定时清理
// 20               定时清理-点击添加新的定时清理任务
// 21               设置-点击应用管理权限
// 22               设置-开关通知
// 23               设置-开关安全模式
// 24               设置-点击关于
// 25               app在前台运行30秒