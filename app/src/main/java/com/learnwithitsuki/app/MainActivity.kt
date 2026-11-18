package com.learnwithitsuki.app

import android.annotation.SuppressLint
import android.app.DownloadManager
import android.content.Context
import android.os.Environment
import android.webkit.URLUtil
import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.view.ViewGroup
import android.webkit.CookieManager
import android.webkit.ValueCallback
import android.webkit.WebSettings
import android.webkit.WebView
import androidx.activity.ComponentActivity
import androidx.activity.compose.BackHandler
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.material3.pulltorefresh.PullToRefreshBox
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.viewinterop.AndroidView
import androidx.core.splashscreen.SplashScreen.Companion.installSplashScreen
import androidx.webkit.WebSettingsCompat
import androidx.webkit.WebViewFeature
import com.learnwithitsuki.app.network.NetworkMonitor
import com.learnwithitsuki.app.ui.OfflineScreen
import com.learnwithitsuki.app.webview.WrapperWebChromeClient
import com.learnwithitsuki.app.webview.WrapperWebViewClient

class MainActivity : ComponentActivity() {

    private lateinit var networkMonitor: NetworkMonitor
    private var webViewRef: WebView? = null
    private var filePathCallback: ValueCallback<Array<Uri>>? = null
    private var pendingUrlState = mutableStateOf(BuildConfig.TARGET_URL)

    private val fileChooserLauncher = registerForActivityResult(
        ActivityResultContracts.OpenMultipleDocuments()
    ) { uris ->
        filePathCallback?.onReceiveValue(uris.toTypedArray())
        filePathCallback = null
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        installSplashScreen()
        enableEdgeToEdge()
        super.onCreate(savedInstanceState)

        networkMonitor = NetworkMonitor(applicationContext)
        networkMonitor.register()
        handleIntentUrl(intent)

        setContent {
            val useDarkTheme = isSystemInDarkTheme()
            val colorScheme = if (useDarkTheme) {
                darkColorScheme(primary = Color(0xFF2563EB))
            } else {
                lightColorScheme(primary = Color(0xFF2563EB))
            }

            MaterialTheme(colorScheme = colorScheme) {
                ItsukiWrapperScreen(
                    initialUrl = pendingUrlState.value,
                    useDarkTheme = useDarkTheme,
                    networkMonitor = networkMonitor,
                    onWebViewCreated = { webViewRef = it },
                    onOpenFileChooser = { callback ->
                        filePathCallback?.onReceiveValue(null)
                        filePathCallback = callback
                        fileChooserLauncher.launch(arrayOf("*/*"))
                        true
                    }
                )
            }
        }
    }

    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        handleIntentUrl(intent)
        webViewRef?.loadUrl(pendingUrlState.value)
    }

    override fun onDestroy() {
        networkMonitor.unregister()
        webViewRef?.destroy()
        webViewRef = null
        super.onDestroy()
    }

    private fun handleIntentUrl(intent: Intent?) {
        val dataUrl = intent?.dataString
        if (!dataUrl.isNullOrBlank() && dataUrl.startsWith("https://")) {
            pendingUrlState.value = dataUrl
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@SuppressLint("SetJavaScriptEnabled")
@Composable
private fun ItsukiWrapperScreen(
    initialUrl: String,
    useDarkTheme: Boolean,
    networkMonitor: NetworkMonitor,
    onWebViewCreated: (WebView) -> Unit,
    onOpenFileChooser: (ValueCallback<Array<Uri>>) -> Boolean
) {
    var webView by remember { mutableStateOf<WebView?>(null) }
    var progress by remember { mutableIntStateOf(0) }
    var isRefreshing by remember { mutableStateOf(false) }
    var hasLoadError by remember { mutableStateOf(false) }
    val isOnline by networkMonitor.isOnline.collectAsState()

    BackHandler(enabled = webView?.canGoBack() == true) {
        webView?.goBack()
    }

    Scaffold { innerPadding ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
        ) {
            val webViewContent: @Composable () -> Unit = {
                AndroidView(
                    modifier = Modifier.fillMaxSize(),
                    factory = { ctx ->
                        WebView(ctx).apply {
                            layoutParams = ViewGroup.LayoutParams(
                                ViewGroup.LayoutParams.MATCH_PARENT,
                                ViewGroup.LayoutParams.MATCH_PARENT
                            )
                            settings.apply {
                                javaScriptEnabled = true
                                domStorageEnabled = true
                                loadsImagesAutomatically = true
                                useWideViewPort = true
                                loadWithOverviewMode = true
                                setSupportZoom(false)
                                mixedContentMode = WebSettings.MIXED_CONTENT_NEVER_ALLOW
                                if (BuildConfig.USER_AGENT_SUFFIX.isNotBlank()) {
                                    userAgentString = "$userAgentString ${BuildConfig.USER_AGENT_SUFFIX}"
                                }
                            }
                            if (WebViewFeature.isFeatureSupported(WebViewFeature.ALGORITHMIC_DARKENING)) {
                                WebSettingsCompat.setAlgorithmicDarkeningAllowed(settings, useDarkTheme)
                            }
                            CookieManager.getInstance().setAcceptCookie(true)
                            CookieManager.getInstance().setAcceptThirdPartyCookies(this, true)

                            setDownloadListener { url, userAgent, contentDisposition, mimeType, _ ->
                                val request = DownloadManager.Request(Uri.parse(url)).apply {
                                    setMimeType(mimeType)
                                    addRequestHeader("User-Agent", userAgent)
                                    setTitle(URLUtil.guessFileName(url, contentDisposition, mimeType))
                                    setNotificationVisibility(
                                        DownloadManager.Request.VISIBILITY_VISIBLE_NOTIFY_COMPLETED
                                    )
                                    setDestinationInExternalPublicDir(
                                        Environment.DIRECTORY_DOWNLOADS,
                                        URLUtil.guessFileName(url, contentDisposition, mimeType)
                                    )
                                }
                                val dm = ctx.getSystemService(Context.DOWNLOAD_SERVICE) as DownloadManager
                                dm.enqueue(request)
                            }

                            webViewClient = WrapperWebViewClient(
                                context = ctx,
                                allowedHost = BuildConfig.ALLOWED_HOST,
                                onPageStarted = {
                                    hasLoadError = false
                                },
                                onPageFinished = {
                                    isRefreshing = false
                                    progress = 100
                                },
                                onLoadError = {
                                    isRefreshing = false
                                    hasLoadError = true
                                }
                            )

                            webChromeClient = WrapperWebChromeClient(
                                onProgressChanged = { value -> progress = value },
                                onOpenFileChooser = { callback, _ -> onOpenFileChooser(callback) }
                            )

                            loadUrl(initialUrl)
                            webView = this
                            onWebViewCreated(this)
                        }
                    }
                )
            }

            PullToRefreshBox(
                isRefreshing = isRefreshing,
                onRefresh = {
                    isRefreshing = true
                    hasLoadError = false
                    webView?.reload()
                },
                modifier = Modifier.fillMaxSize()
            ) {
                webViewContent()
            }

            if (progress in 1..99) {
                LinearProgressIndicator(
                    progress = { progress / 100f },
                    modifier = Modifier
                        .fillMaxWidth()
                        .align(Alignment.TopCenter),
                    color = Color(0xFF2563EB)
                )
            }

            if (!isOnline || hasLoadError) {
                OfflineScreen(
                    onRetry = {
                        if (networkMonitor.checkCurrentConnectivity()) {
                            hasLoadError = false
                            webView?.reload()
                        }
                    }
                )
            }
        }
    }
}
