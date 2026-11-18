package com.learnwithitsuki.app.webview

import android.content.Context
import android.content.Intent
import android.graphics.Bitmap
import android.net.Uri
import android.webkit.WebResourceError
import android.webkit.WebResourceRequest
import android.webkit.WebView
import android.webkit.WebViewClient
import androidx.browser.customtabs.CustomTabsIntent

class WrapperWebViewClient(
    private val context: Context,
    private val allowedHost: String,
    private val onPageStarted: (String) -> Unit,
    private val onPageFinished: (String) -> Unit,
    private val onLoadError: (String) -> Unit
) : WebViewClient() {

    override fun onPageStarted(view: WebView?, url: String?, favicon: Bitmap?) {
        super.onPageStarted(view, url, favicon)
        if (url != null) onPageStarted(url)
    }

    override fun onPageFinished(view: WebView?, url: String?) {
        super.onPageFinished(view, url)
        if (url != null) onPageFinished(url)
    }

    override fun shouldOverrideUrlLoading(view: WebView?, request: WebResourceRequest?): Boolean {
        val uri = request?.url ?: return false
        val scheme = uri.scheme?.lowercase() ?: return false

        if (scheme == "tel" || scheme == "mailto" || scheme == "whatsapp") {
            runCatching {
                context.startActivity(Intent(Intent.ACTION_VIEW, uri))
            }
            return true
        }

        val host = uri.host?.lowercase()?.removePrefix("www.") ?: return false
        val normalizedAllowed = allowedHost.lowercase().removePrefix("www.")

        if (host == normalizedAllowed || host.endsWith(".$normalizedAllowed")) {
            return false
        }

        openExternalUri(uri)
        return true
    }

    override fun onReceivedError(
        view: WebView?,
        request: WebResourceRequest?,
        error: WebResourceError?
    ) {
        super.onReceivedError(view, request, error)
        if (request?.isForMainFrame == true) {
            onLoadError(error?.description?.toString() ?: "Gagal memuat halaman.")
        }
    }

    private fun openExternalUri(uri: Uri) {
        runCatching {
            CustomTabsIntent.Builder()
                .setShowTitle(true)
                .build()
                .launchUrl(context, uri)
        }.onFailure {
            runCatching { context.startActivity(Intent(Intent.ACTION_VIEW, uri)) }
        }
    }
}
