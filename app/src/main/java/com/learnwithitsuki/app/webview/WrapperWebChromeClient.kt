package com.learnwithitsuki.app.webview

import android.net.Uri
import android.webkit.ValueCallback
import android.webkit.WebChromeClient
import android.webkit.WebView

class WrapperWebChromeClient(
    private val onProgressChanged: (Int) -> Unit,
    private val onOpenFileChooser: (ValueCallback<Array<Uri>>, FileChooserParams?) -> Boolean
) : WebChromeClient() {

    override fun onProgressChanged(view: WebView?, newProgress: Int) {
        super.onProgressChanged(view, newProgress)
        onProgressChanged(newProgress)
    }

    override fun onShowFileChooser(
        webView: WebView?,
        filePathCallback: ValueCallback<Array<Uri>>?,
        fileChooserParams: FileChooserParams?
    ): Boolean {
        if (filePathCallback == null) return false
        return onOpenFileChooser(filePathCallback, fileChooserParams)
    }
}
