package nl.viter.glider.feature.auth

import android.webkit.*
import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.viewinterop.AndroidView
import kotlinx.coroutines.launch
import nl.viter.glider.shared.data.AppStore
import org.jsoup.Jsoup

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AuthScreen(store: AppStore, onBack: () -> Unit) {
    var web by remember { mutableStateOf<WebView?>(null) }
    var message by remember { mutableStateOf<String?>(null) }
    val scope = rememberCoroutineScope()
    Scaffold(topBar = {
        TopAppBar(title = {
            Text("Hacker News account")
        }, navigationIcon = { TextButton(onClick = onBack) { Text("Close") } }, actions = {
            TextButton(onClick = {
                val cookie = CookieManager.getInstance().getCookie(
                    "https://news.ycombinator.com"
                )?.split(";")?.map {
                    it.trim()
                }?.firstOrNull { it.startsWith("user=") }?.substringAfter("user=")
                if (cookie == null) {
                    message = "Log in on Hacker News first"
                } else {
                    web?.evaluateJavascript(
                        "document.querySelector('#me')?.textContent || ''"
                    ) { encoded ->
                        val name = encoded.trim('"')
                        if (name.isNotBlank() &&
                            name != "null"
                        ) {
                            store.login(cookie, name)
                            onBack()
                        } else {
                            message =
                                "Open the HN frontpage after login, then tap Done"
                            web?.loadUrl("https://news.ycombinator.com/news")
                        }
                    }
                }
            }) { Text("Done") }
        })
    }) { padding ->
        Column(Modifier.padding(padding)) {
            message?.let { Text(it) }
            AndroidView(factory = { context ->
                WebView(context).apply {
                    settings.javaScriptEnabled = true
                    settings.allowFileAccess = false
                    settings.allowContentAccess = false
                    settings.mixedContentMode = WebSettings.MIXED_CONTENT_NEVER_ALLOW
                    webViewClient = object : WebViewClient() {
                        override fun shouldOverrideUrlLoading(
                            view: WebView,
                            request: WebResourceRequest
                        ): Boolean = request.url.scheme != "https" ||
                            request.url.host != "news.ycombinator.com"
                    }
                    web = this
                    loadUrl("https://news.ycombinator.com/login?goto=news")
                }
            }, modifier = Modifier.fillMaxSize())
        }
    }
    DisposableEffect(Unit) { onDispose { web?.destroy() } }
}
