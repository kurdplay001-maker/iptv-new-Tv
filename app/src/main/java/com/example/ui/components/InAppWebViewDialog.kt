package com.example.ui.components

import android.graphics.Bitmap
import android.webkit.WebResourceError
import android.webkit.WebResourceRequest
import android.webkit.WebView
import android.webkit.WebViewClient
import androidx.activity.compose.BackHandler
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import com.example.ui.theme.KurdPlayBg
import com.example.ui.theme.KurdPlayBluePrimary
import com.example.ui.theme.KurdPlaySurface

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun InAppWebViewDialog(
    url: String,
    title: String,
    onDismiss: () -> Unit
) {
    var webView: WebView? by remember { mutableStateOf(null) }
    var isLoading by remember { mutableStateOf(true) }
    var currentWebTitle by remember { mutableStateOf(title) }
    var currentUrl by remember { mutableStateOf(url) }

    // Intercept back key: step-by-step back in web history if possible!
    BackHandler {
        if (webView?.canGoBack() == true) {
            webView?.goBack()
        } else {
            onDismiss()
        }
    }

    Dialog(
        onDismissRequest = {
            if (webView?.canGoBack() == true) {
                webView?.goBack()
            } else {
                onDismiss()
            }
        },
        properties = DialogProperties(
            usePlatformDefaultWidth = false,
            dismissOnBackPress = false
        )
    ) {
        Surface(
            modifier = Modifier
                .fillMaxSize()
                .padding(top = 24.dp),
            color = KurdPlayBg
        ) {
            Column(modifier = Modifier.fillMaxSize()) {
                // Top Header Bar
                TopAppBar(
                    title = {
                        Column {
                            Text(
                                text = currentWebTitle,
                                color = Color.White,
                                fontSize = 14.sp,
                                fontWeight = FontWeight.Bold,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                            Text(
                                text = currentUrl,
                                color = Color.Gray,
                                fontSize = 11.sp,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                        }
                    },
                    navigationIcon = {
                        // Step-by-step Back arrow button
                        IconButton(
                            onClick = {
                                if (webView?.canGoBack() == true) {
                                    webView?.goBack()
                                } else {
                                    onDismiss()
                                }
                            }
                        ) {
                            Icon(
                                Icons.AutoMirrored.Filled.ArrowBack,
                                contentDescription = "Step Back",
                                tint = Color.White
                            )
                        }
                    },
                    actions = {
                        IconButton(onClick = { webView?.reload() }) {
                            Icon(Icons.Default.Refresh, contentDescription = "Reload", tint = KurdPlayBluePrimary)
                        }
                        // Prominent Close 'X' button to dismiss full web view immediately
                        IconButton(
                            onClick = onDismiss,
                            modifier = Modifier.padding(end = 4.dp)
                        ) {
                            Surface(
                                shape = CircleShape,
                                color = Color.Red.copy(alpha = 0.2f),
                                modifier = Modifier.size(32.dp)
                            ) {
                                Box(contentAlignment = Alignment.Center) {
                                    Icon(
                                        Icons.Default.Close,
                                        contentDescription = "Close Window",
                                        tint = Color.Red,
                                        modifier = Modifier.size(18.dp)
                                    )
                                }
                            }
                        }
                    },
                    colors = TopAppBarDefaults.topAppBarColors(containerColor = KurdPlaySurface)
                )

                if (isLoading) {
                    LinearProgressIndicator(
                        modifier = Modifier.fillMaxWidth(),
                        color = KurdPlayBluePrimary,
                        trackColor = Color.Transparent
                    )
                }

                // WebView container
                var customView by remember { mutableStateOf<android.view.View?>(null) }
                var customViewCallback by remember { mutableStateOf<android.webkit.WebChromeClient.CustomViewCallback?>(null) }

                Box(modifier = Modifier.weight(1f)) {
                    if (customView != null) {
                        // Display full screen HTML5 video container
                        AndroidView(
                            factory = { _ -> customView!! },
                            modifier = Modifier.fillMaxSize()
                        )
                    } else {
                        AndroidView(
                            factory = { context ->
                                WebView(context).apply {
                                    settings.javaScriptEnabled = true
                                    settings.domStorageEnabled = true
                                    settings.loadWithOverviewMode = true
                                    settings.useWideViewPort = true
                                    settings.mediaPlaybackRequiresUserGesture = false

                                    webChromeClient = object : android.webkit.WebChromeClient() {
                                        override fun onShowCustomView(
                                            view: android.view.View?,
                                            callback: CustomViewCallback?
                                        ) {
                                            customView = view
                                            customViewCallback = callback
                                        }

                                        override fun onHideCustomView() {
                                            customView = null
                                            customViewCallback?.onCustomViewHidden()
                                        }
                                    }

                                    webViewClient = object : WebViewClient() {
                                        override fun onPageStarted(view: WebView?, url: String?, favicon: Bitmap?) {
                                            super.onPageStarted(view, url, favicon)
                                            isLoading = true
                                            if (url != null) currentUrl = url
                                        }

                                        override fun onPageFinished(view: WebView?, url: String?) {
                                            super.onPageFinished(view, url)
                                            isLoading = false
                                            if (url != null) currentUrl = url
                                            if (view?.title != null && view.title!!.isNotBlank()) {
                                                currentWebTitle = view.title!!
                                            }
                                        }

                                        override fun onReceivedError(
                                            view: WebView?,
                                            request: WebResourceRequest?,
                                            error: WebResourceError?
                                        ) {
                                            super.onReceivedError(view, request, error)
                                            isLoading = false
                                        }
                                    }
                                    loadUrl(url)
                                    webView = this
                                }
                            },
                            update = { view ->
                                webView = view
                            },
                            modifier = Modifier.fillMaxSize()
                        )
                    }
                }
            }
        }
    }
}
