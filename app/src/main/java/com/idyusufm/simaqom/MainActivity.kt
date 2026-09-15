package com.idyusufm.simaqom

import android.annotation.SuppressLint
import android.app.Dialog
import android.content.ActivityNotFoundException
import android.content.Intent
import android.graphics.Bitmap
import android.net.ConnectivityManager
import android.net.NetworkCapabilities
import android.net.Uri
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.os.Message
import android.view.View
import android.view.ViewGroup
import android.webkit.CookieManager
import android.webkit.ValueCallback
import android.webkit.WebChromeClient
import android.webkit.WebResourceError
import android.webkit.WebResourceRequest
import android.webkit.WebSettings
import android.webkit.WebView
import android.webkit.WebViewClient
import android.widget.Button
import android.widget.ProgressBar
import android.widget.TextView
import android.widget.Toast
import androidx.activity.OnBackPressedCallback
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity
import androidx.core.net.toUri
import androidx.core.splashscreen.SplashScreen.Companion.installSplashScreen
import androidx.swiperefreshlayout.widget.SwipeRefreshLayout
import com.google.android.material.dialog.MaterialAlertDialogBuilder

class MainActivity : AppCompatActivity() {

    private lateinit var webView: WebView
    private lateinit var swipeRefreshLayout: SwipeRefreshLayout
    private lateinit var progressBar: ProgressBar
    private lateinit var layoutOfflineError: View
    private lateinit var layoutSplash: View
    private lateinit var tvAppVersion: TextView
    private lateinit var btnRetry: Button
    private lateinit var tvOfflineMessage: TextView

    private var popupDialog: Dialog? = null
    private var popupWebView: WebView? = null
    private var isSplashHidden = false
    private val splashHandler = Handler(Looper.getMainLooper())

    // File upload callback handler
    private var filePathCallback: ValueCallback<Array<Uri>>? = null
    private val fileChooserLauncher = registerForActivityResult(
        ActivityResultContracts.StartActivityForResult(),
    ) { result ->
        if (filePathCallback != null) {
            val results: Array<Uri>? = when {
                ((result.resultCode == RESULT_OK) && (result.data != null)) -> {
                    val data = result.data
                    val dataString = data?.dataString
                    val clipData = data?.clipData
                    when {
                        clipData != null -> {
                            Array(clipData.itemCount) { i -> clipData.getItemAt(i).uri }
                        }
                        dataString != null -> arrayOf(dataString.toUri())
                        else -> null
                    }
                }
                else -> null
            }
            filePathCallback?.onReceiveValue(results)
            filePathCallback = null
        }
    }

    @SuppressLint("SetJavaScriptEnabled")
    override fun onCreate(savedInstanceState: Bundle?) {
        installSplashScreen()
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        // Initialize UI Elements
        webView = findViewById(R.id.webView)
        swipeRefreshLayout = findViewById(R.id.swipeRefreshLayout)
        progressBar = findViewById(R.id.progressBar)
        layoutOfflineError = findViewById(R.id.layoutOfflineError)
        layoutSplash = findViewById(R.id.layoutSplash)
        tvAppVersion = findViewById(R.id.tvAppVersion)
        btnRetry = findViewById(R.id.btnRetry)
        tvOfflineMessage = findViewById(R.id.tvOfflineMessage)

        tvOfflineMessage.setText(R.string.offline_message)

        // Display dynamic app version
        val versionName = try {
            val packageInfo = packageManager.getPackageInfo(packageName, 0)
            packageInfo.versionName ?: "1.0.0"
        } catch (_: Exception) {
            "1.0.0"
        }
        tvAppVersion.text = getString(R.string.app_version_format, versionName)

        setupWebViewSettings()
        setupWebViewClients()
        setupSwipeRefresh()
        setupBackNavigation()

        btnRetry.setOnClickListener {
            if (isNetworkAvailable()) {
                layoutOfflineError.visibility = View.GONE
                webView.visibility = View.VISIBLE
                webView.reload()
            } else {
                Toast.makeText(this, "Still no internet connection", Toast.LENGTH_SHORT).show()
            }
        }

        // Load entry URL
        if (isNetworkAvailable()) {
            webView.loadUrl("https://idyusufm.github.io/simaqom")
            // Ensure splash stays visible for at least 1.5 seconds for branding display
            splashHandler.postDelayed({
                hideSplashView()
            }, 1500)
        } else {
            hideSplashView()
            showOfflineView()
        }
    }

    private fun hideSplashView() {
        if (!isSplashHidden) {
            isSplashHidden = true
            layoutSplash.animate()
                .alpha(0f)
                .setDuration(400)
                .withEndAction {
                    layoutSplash.visibility = View.GONE
                }
        }
    }

    @SuppressLint("SetJavaScriptEnabled")
    private fun setupWebViewSettings() {
        val settings = webView.settings
        settings.javaScriptEnabled = true
        settings.domStorageEnabled = true
        settings.databaseEnabled = true
        settings.allowFileAccess = true
        settings.allowContentAccess = true
        settings.useWideViewPort = false
        settings.loadWithOverviewMode = false
        settings.setSupportZoom(false)
        settings.builtInZoomControls = false
        settings.displayZoomControls = false
        settings.javaScriptCanOpenWindowsAutomatically = true
        settings.setSupportMultipleWindows(true)
        settings.mixedContentMode = WebSettings.MIXED_CONTENT_ALWAYS_ALLOW

        // Cache mode
        settings.cacheMode = WebSettings.LOAD_DEFAULT

        // Standard Chrome User-Agent without "wv" or custom WebView tokens for Google Sign-In compatibility
        val defaultUA = settings.userAgentString
        settings.userAgentString = defaultUA.replace("; wv", "").replace("Version/4.0 ", "")

        // Enable cookies & third-party cookies
        val cookieManager = CookieManager.getInstance()
        cookieManager.setAcceptCookie(true)
        cookieManager.setAcceptThirdPartyCookies(webView, true)

        // Register JavaScript Interface Bridge
        webView.addJavascriptInterface(WebAppInterface(this), "AndroidBridge")
    }

    private fun setupWebViewClients() {
        webView.webViewClient = object : WebViewClient() {
            override fun onPageStarted(view: WebView?, url: String?, favicon: Bitmap?) {
                super.onPageStarted(view, url, favicon)
                progressBar.visibility = View.VISIBLE
                layoutOfflineError.visibility = View.GONE
                webView.visibility = View.VISIBLE
            }

            override fun onPageFinished(view: WebView?, url: String?) {
                super.onPageFinished(view, url)
                progressBar.visibility = View.GONE
                swipeRefreshLayout.isRefreshing = false
                CookieManager.getInstance().flush()

                // Inject JS to disable pinch zoom, gesture zoom, and user scaling
                val disableZoomJs = """
                    (function() {
                        var meta = document.querySelector('meta[name="viewport"]');
                        if (!meta) {
                            meta = document.createElement('meta');
                            meta.name = 'viewport';
                            document.getElementsByTagName('head')[0].appendChild(meta);
                        }
                        meta.content = 'width=device-width, initial-scale=1.0, maximum-scale=1.0, minimum-scale=1.0, user-scalable=no';
                        document.addEventListener('gesturestart', function(e) {
                            e.preventDefault();
                        });
                    })();
                """.trimIndent()
                view?.evaluateJavascript(disableZoomJs, null)
            }

            override fun onReceivedError(
                view: WebView?,
                request: WebResourceRequest?,
                error: WebResourceError?,
            ) {
                super.onReceivedError(view, request, error)
                if (request?.isForMainFrame == true && !isNetworkAvailable()) {
                    hideSplashView()
                    showOfflineView()
                }
            }

            override fun shouldOverrideUrlLoading(view: WebView?, request: WebResourceRequest?): Boolean {
                val url = request?.url?.toString() ?: return false
                
                            // Allow tel, mailto, whatsapp, sms to open in external handler
                if (url.startsWith("tel:") || url.startsWith("mailto:") || 
                    url.startsWith("whatsapp:") || url.startsWith("sms:")) {
                    try {
                        val intent = Intent(Intent.ACTION_VIEW, url.toUri())
                        startActivity(intent)
                    } catch (_: ActivityNotFoundException) {
                        Toast.makeText(this@MainActivity, "No application found to handle this action", Toast.LENGTH_SHORT).show()
                    }
                    return true
                }

                // Any other non-http(s) scheme (e.g. Telegram's tg:, or
                // market:, intent:, etc.) can't be loaded inside the
                // WebView itself — hand it to the system instead of
                // letting the WebView show an error page for it.
                if (request.url.scheme != "http" && request.url.scheme != "https") {
                    try {
                        startActivity(Intent(Intent.ACTION_VIEW, request.url))
                    } catch (_: ActivityNotFoundException) {
                        Toast.makeText(this@MainActivity, "No app found to open this link", Toast.LENGTH_SHORT).show()
                    }
                    return true
                }

                return false
            }
        }

        webView.webChromeClient = object : WebChromeClient() {
            override fun onProgressChanged(view: WebView?, newProgress: Int) {
                super.onProgressChanged(view, newProgress)
                if (newProgress == 100) {
                    progressBar.visibility = View.GONE
                } else {
                    progressBar.visibility = View.VISIBLE
                    progressBar.progress = newProgress
                }
            }

            // Handle popup windows created by Google OAuth/Sign-In (Firebase Auth signInWithPopup)
            @SuppressLint("SetJavaScriptEnabled")
            override fun onCreateWindow(
                view: WebView?,
                isDialog: Boolean,
                isUserGesture: Boolean,
                resultMsg: Message?,
            ): Boolean {
                dismissPopupDialog()

                val newPopupWebView = WebView(this@MainActivity).apply {
                    settings.javaScriptEnabled = true
                    settings.domStorageEnabled = true
                    settings.databaseEnabled = true
                    settings.allowFileAccess = true
                    settings.allowContentAccess = true
                    settings.setSupportZoom(false)
                    settings.builtInZoomControls = false
                    settings.displayZoomControls = false
                    settings.javaScriptCanOpenWindowsAutomatically = true
                    settings.setSupportMultipleWindows(true)
                    settings.userAgentString = webView.settings.userAgentString

                    CookieManager.getInstance().setAcceptCookie(true)
                    CookieManager.getInstance().setAcceptThirdPartyCookies(this, true)

                    webViewClient = object : WebViewClient() {
                        override fun shouldOverrideUrlLoading(view: WebView?, request: WebResourceRequest?): Boolean {
                            val url = request?.url?.toString() ?: return false
                            if (url.startsWith("tel:") || url.startsWith("mailto:") || 
                                url.startsWith("whatsapp:") || url.startsWith("sms:")) {
                                try {
                                    val intent = Intent(Intent.ACTION_VIEW, url.toUri())
                                    startActivity(intent)
                                } catch (_: ActivityNotFoundException) {
                                }
                                return true
                            }
                            return false
                        }

                        override fun onPageFinished(view: WebView?, url: String?) {
                            super.onPageFinished(view, url)
                            CookieManager.getInstance().flush()
                        }
                    }

                    webChromeClient = object : WebChromeClient() {
                        override fun onCloseWindow(window: WebView?) {
                            super.onCloseWindow(window)
                            dismissPopupDialog()
                        }
                    }
                }

                popupWebView = newPopupWebView
                val dialog = Dialog(this@MainActivity, android.R.style.Theme_DeviceDefault_Light_NoActionBar_Fullscreen).apply {
                    setContentView(newPopupWebView, ViewGroup.LayoutParams(
                        ViewGroup.LayoutParams.MATCH_PARENT,
                        ViewGroup.LayoutParams.MATCH_PARENT,
                    ))
                    setOnDismissListener {
                        dismissPopupDialog()
                    }
                }
                popupDialog = dialog
                dialog.show()

                val transport = resultMsg?.obj as? WebView.WebViewTransport
                transport?.webView = newPopupWebView
                resultMsg?.sendToTarget()
                return true
            }

            // Handle file input <input type="file">
            override fun onShowFileChooser(
                webView: WebView?,
                filePathCallback: ValueCallback<Array<Uri>>?,
                fileChooserParams: FileChooserParams?,
            ): Boolean {
                this@MainActivity.filePathCallback?.onReceiveValue(null)
                this@MainActivity.filePathCallback = filePathCallback

                val intent = fileChooserParams?.createIntent() ?: Intent(Intent.ACTION_GET_CONTENT).apply {
                    type = "*/*"
                    addCategory(Intent.CATEGORY_OPENABLE)
                }

                try {
                    fileChooserLauncher.launch(intent)
                    return true
                } catch (_: ActivityNotFoundException) {
                    this@MainActivity.filePathCallback = null
                    Toast.makeText(this@MainActivity, "Cannot open file picker", Toast.LENGTH_SHORT).show()
                    return false
                }
            }
        }
    }

    private fun dismissPopupDialog() {
        popupWebView?.let {
            it.stopLoading()
            it.destroy()
        }
        popupWebView = null
        popupDialog?.let {
            if (it.isShowing) {
                it.dismiss()
            }
        }
        popupDialog = null
    }

    private fun setupSwipeRefresh() {
        swipeRefreshLayout.isEnabled = true
        swipeRefreshLayout.setColorSchemeColors(getColor(R.color.primary))
        swipeRefreshLayout.setOnRefreshListener {
            if (isNetworkAvailable()) {
                webView.reload()
            } else {
                swipeRefreshLayout.isRefreshing = false
                showOfflineView()
            }
        }
    }

    private fun setupBackNavigation() {
        onBackPressedDispatcher.addCallback(
            this,
            object : OnBackPressedCallback(enabled = true) {
                override fun handleOnBackPressed() {
                    if (popupDialog?.isShowing == true) {
                        if (popupWebView?.canGoBack() == true) {
                            popupWebView?.goBack()
                        } else {
                            dismissPopupDialog()
                        }
                    } else {
                        showExitConfirmationDialog()
                    }
                }
            },
        )
    }

    private fun showExitConfirmationDialog() {
        MaterialAlertDialogBuilder(this)
            .setTitle(R.string.exit_dialog_title)
            .setMessage(R.string.exit_dialog_message)
            .setPositiveButton(R.string.exit_dialog_positive) { _, _ ->
                finish()
            }
            .setNegativeButton(R.string.exit_dialog_negative) { dialog, _ ->
                dialog.dismiss()
            }
            .show()
    }

    private fun showOfflineView() {
        webView.visibility = View.GONE
        layoutOfflineError.visibility = View.VISIBLE
        swipeRefreshLayout.isRefreshing = false
        progressBar.visibility = View.GONE
    }

    private fun isNetworkAvailable(): Boolean {
        val connectivityManager = getSystemService(CONNECTIVITY_SERVICE) as ConnectivityManager
        val network = connectivityManager.activeNetwork ?: return false
        val capabilities = connectivityManager.getNetworkCapabilities(network) ?: return false
        return capabilities.hasCapability(NetworkCapabilities.NET_CAPABILITY_INTERNET)
    }

    override fun onResume() {
        super.onResume()
        webView.onResume()
    }

    override fun onPause() {
        super.onPause()
        webView.onPause()
    }

    override fun onDestroy() {
        splashHandler.removeCallbacksAndMessages(null)
        dismissPopupDialog()
        webView.destroy()
        super.onDestroy()
    }
}
