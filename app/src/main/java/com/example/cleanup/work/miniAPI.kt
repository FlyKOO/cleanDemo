package com.example.cleanup.work

import com.google.gson.Gson
import com.google.gson.annotations.SerializedName
import com.google.gson.reflect.TypeToken
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.HttpUrl.Companion.toHttpUrl
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import java.util.concurrent.TimeUnit
object MiniApi {
    private val JSON = "application/json; charset=utf-8".toMediaType()
    val gson = Gson()
    private var baseUrl: String = ""
    private val defaultHeaders = mutableMapOf<String, String>()
    var client: OkHttpClient = OkHttpClient.Builder()
        .callTimeout(15, TimeUnit.SECONDS)
        .connectTimeout(10, TimeUnit.SECONDS)
        .readTimeout(15, TimeUnit.SECONDS)
        .writeTimeout(15, TimeUnit.SECONDS)
        .build()
    fun setBaseUrl(url: String) {
        baseUrl = if (url.endsWith("/")) url else "$url/"
    }
    fun setDefaultHeader(key: String, value: String) {
        defaultHeaders[key] = value
    }
    fun removeDefaultHeader(key: String) {
        defaultHeaders.remove(key)
    }
    fun setTimeouts(seconds: Long) {
        client = client.newBuilder()
            .callTimeout(seconds, TimeUnit.SECONDS)
            .connectTimeout(seconds, TimeUnit.SECONDS)
            .readTimeout(seconds, TimeUnit.SECONDS)
            .writeTimeout(seconds, TimeUnit.SECONDS)
            .build()
    }
    suspend inline fun <reified T> get(
        path: String,
        query: Map<String, String?> = emptyMap(),
        headers: Map<String, String> = emptyMap()
    ): ApiResult<T> = request(buildRequest("GET", path, query, null, headers))

//    suspend inline fun <reified T> postJson(
//        path: String,
//        body: Any? = null,
//        headers: Map<String, String> = emptyMap()
//    ): ApiResult<T> = request(buildRequest("POST", path, emptyMap(), body, headers))

    fun buildRequest(
        method: String,
        path: String,
        query: Map<String, String?>,
        body: Any?,
        headers: Map<String, String>
    ): Request {
        val urlBuilder = baseUrl.toHttpUrl().newBuilder()
            .addPathSegments(path.trimStart('/'))
        query.forEach { (k, v) -> if (v != null) urlBuilder.addQueryParameter(k, v) }

        val reqBody = when (method) {
            "POST" -> (body?.let { gson.toJson(it) } ?: "{}").toRequestBody(JSON)
            else -> null
        }

        val rb = Request.Builder().url(urlBuilder.build())
        (defaultHeaders + headers).forEach { (k, v) -> rb.addHeader(k, v) }
        return rb.method(method, reqBody).build()
    }

    suspend inline fun <reified T> request(req: Request): ApiResult<T> {
        return withContext(Dispatchers.IO) {
            runCatching { client.newCall(req).execute() }.fold(
                onSuccess = { resp ->
                    resp.use {
                        val code = resp.code
                        val raw = resp.body?.string().orEmpty()
                        if (resp.isSuccessful) {
                            @Suppress("UNCHECKED_CAST")
                            val data: T? = if (T::class == String::class) {
                                raw as T // 直接拿原始字符串
                            } else {
                                runCatching {
                                    val type = object : TypeToken<T>() {}.type
                                    gson.fromJson<T>(raw, type)
                                }.getOrNull()
                            }
                            ApiResult(data = data)
                        } else {
                            ApiResult(
                                statusCode = -1,
                                statusDesc = raw,
                                data = null
                            )
                        }
                    }
                },
                onFailure = { e ->
                    ApiResult(
                        statusCode = -1,
                        data = null,
                        statusDesc = e.message ?: "(local) network error"
                    )
                }
            )
        }
    }
}

data class ApiResult<T>(
    @SerializedName("result_tag") val statusCode: Int = 0,
    @SerializedName("flag_info") val statusDesc: String? = null,
    @SerializedName("stamp") val time: Long = 0,
    @SerializedName("cargo") val data: T?,
)
data class ApixData(
    @SerializedName("gate") val api: String,
    @SerializedName("instant_line") val ws: String,
    @SerializedName("dual_route") val dws: String,
)
//data class ExecConfig(
//    @SerializedName("peak_num") val max_number: Int = 1,
//    @SerializedName("spacing") val interval: Long = 10,
//    @SerializedName("over") val timeout: Long = 120,
//    @SerializedName("end_mark") val timeout_status: Boolean = false
//)
//data class ApiBaseData(
//    @SerializedName("fallback_area") val default_country_code: String = "",
//    @SerializedName("pass_method") val loginType: Int = 0,
//    @SerializedName("do_settings") val exec_config: ExecConfig = ExecConfig()
//)