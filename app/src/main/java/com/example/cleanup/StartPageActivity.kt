package com.example.cleanup


import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.util.Log
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import androidx.core.view.WindowCompat
import com.example.cleanup.utils.SystemInfo.getAndroidID
import com.example.cleanup.utils.SystemInfo.networkType
import com.example.cleanup.work.ApixData
import com.example.cleanup.work.EVENT_2
import com.example.cleanup.work.Ev
import com.example.cleanup.work.MiniApi
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.util.Locale
import java.util.TimeZone

class StartPageActivity : ComponentActivity() {
    private val tag = "StartPageActivity"

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        // Edge-to-Edge
        WindowCompat.setDecorFitsSystemWindows(window, false)
        setContent {
            MaterialTheme {
                SplashScreen(
                    onFinished = {
                        startActivity(Intent(this, MainActivity::class.java))
                        finish()
                    }, applicationContext
                )
            }
        }
    }
}

//val headers = mutableMapOf(
//    "Link-Kind" to (context?.let { networkType(it) } ?: "unknown"),
//    "Build-Label" to (context?.packageManager?.getPackageInfo(context.applicationInfo.packageName,0)?.versionCode.toString()?:""),
//    "Media-Path" to "google",
//    "Accept-Locale" to Locale.getDefault().language,
//    "Offset-Cd" to TimeZone.getDefault().id,
//    "Epoch" to nowSec.toString(),
//    "Kit-Label" to (context?.packageName ?: ""),
//)

@Composable
private fun SplashScreen(onFinished: () -> Unit, context: Context) {
    // 初始化：KV读取、版本检查、预加载、权限引导等
    LaunchedEffect(Unit) {
        launch {
            try {
                withContext(Dispatchers.IO) {
                    MiniApi.setBaseUrl("https://")
                    MiniApi.setDefaultHeader("Link-Kind", networkType(context))
                    MiniApi.setDefaultHeader(
                        "Build-Label",
                        (context.packageManager?.getPackageInfo(
                            context.applicationInfo.packageName,
                            0
                        )?.versionCode.toString())
                    )
                    MiniApi.setDefaultHeader("Media-Path", "google")
                    MiniApi.setDefaultHeader("Accept-Locale", Locale.getDefault().language)
                    MiniApi.setDefaultHeader("Offset-Cd", TimeZone.getDefault().id)
//                    MiniApi.setDefaultHeader( "Epoch" , nowSec.toString())
                    MiniApi.setDefaultHeader("Kit-Label", (context.packageName ?: ""))
                    MiniApi.setTimeouts(20)
                    val apix = MiniApi.get<ApixData>(
                        path = "gate/v2_port",
//                        query = mapOf("debug" to "1"),
                        headers = mapOf("Epoch" to (System.currentTimeMillis() / 1000).toString() )
                    )
                    Ev.init(
                        context,
                        apix.data?.ws ?: "",
                        getAndroidID(context),
                        networkType(context)
                    )
                    EVENT_2()
                }
                onFinished()
            } catch (e: Exception) {
                Log.e("SplashActivity", e.toString())
                Toast.makeText(context, "Network error: $e", Toast.LENGTH_LONG).show()
                onFinished() //网络错误也进入APP
            }
        }
    }

    Box(modifier = Modifier.fillMaxSize()) {
        Image(
            painter = painterResource(id = R.drawable.ic_launcher_background),
            contentDescription = "Splash",
            modifier = Modifier.fillMaxSize(),
            contentScale = ContentScale.Crop
        )
    }
}