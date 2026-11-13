package com.example.myapplication

import android.app.Activity
import android.content.Intent
import android.graphics.Bitmap
import android.net.Uri
import android.net.http.SslError
import android.os.Bundle
import android.util.Log
import android.webkit.ConsoleMessage
import android.webkit.JsResult
import android.webkit.SslErrorHandler
import android.webkit.WebView
import android.webkit.WebViewClient
import android.webkit.WebChromeClient
import android.webkit.WebSettings
import androidx.appcompat.app.AppCompatActivity
import androidx.activity.OnBackPressedCallback
import android.widget.Toast

class TossPaymentActivity : AppCompatActivity() {
    private lateinit var webView: WebView

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_toss_payment)

        Log.d("TossPayment", "Activity 시작")
        checkWebViewPackage() // WebView 패키지 확인
        checkTossApp() // 토스 앱 설치 여부 확인

        webView = findViewById(R.id.webView)
        setupWebView()

        val orderId = intent.getStringExtra("orderId")
        val checkoutUrl = intent.getStringExtra("checkoutUrl")
        val amount = intent.getIntExtra("amount", 0)

        Log.d("TossPayment", """
            결제 정보:
            orderId: $orderId
            checkoutUrl: $checkoutUrl
            amount: $amount
        """.trimIndent())

        if (orderId == null || checkoutUrl == null) {
            Log.e("TossPayment", "필수 파라미터 누락")
            finish()
            return
        }

        webView.loadUrl(checkoutUrl)

        // 뒤로가기 처리 추가
        onBackPressedDispatcher.addCallback(this, object : OnBackPressedCallback(true) {
            override fun handleOnBackPressed() {
                try {
                    deletePendingOrder()
                    setResult(Activity.RESULT_CANCELED, Intent().apply {
                        putExtra("error", "결제가 취소되었습니다.")
                    })
                    finish()
                } catch (e: Exception) {
                    Log.e("TossPayment", "뒤로가기 처리 실패", e)
                }
            }
        })
    }

    private fun setupWebView() {
        try {
            // WebView 패키지 확인 및 초기화
            androidx.webkit.WebViewCompat.getCurrentWebViewPackage(this)?.let { webViewPackage ->
                Log.d("TossPayment", "WebView 패키지 정보: ${webViewPackage.packageName} (${webViewPackage.versionName})")
            }
            Log.d("TossPayment", "WebView 설정 시작")

            // WebChromeClient 설정
            webView.webChromeClient = object : WebChromeClient() {
                override fun onJsAlert(view: WebView?, url: String?, message: String?, result: JsResult?): Boolean {
                    // JavaScript alert을 완전히 무시하고 기본 동작만 완료
                    result?.confirm()
                    return true // alert 표시하지 않음
                }
                
                // 기존 코드 유지
                override fun onConsoleMessage(consoleMessage: ConsoleMessage?): Boolean {
                    consoleMessage?.apply {
                        Log.d("TossPayment-Console", "${message()} -- From line ${lineNumber()} of ${sourceId()}")
                    }
                    return true
                }
            }

            webView.settings.apply {
                javaScriptEnabled = true
                domStorageEnabled = true
                javaScriptCanOpenWindowsAutomatically = true
                setSupportMultipleWindows(true)
                loadWithOverviewMode = true
                useWideViewPort = true
                allowContentAccess = true
                allowFileAccess = true
                databaseEnabled = true
                setSupportMultipleWindows(true)
                javaScriptCanOpenWindowsAutomatically = true
                mixedContentMode = WebSettings.MIXED_CONTENT_ALWAYS_ALLOW
                cacheMode = WebSettings.LOAD_NO_CACHE  // 캐시 비활성화

                // User Agent 수정 - 모바일 웹뷰임을 나타내는 식별자 제거
                val defaultUserAgent = userAgentString
                val customUserAgent = defaultUserAgent
                    .replace("; wv", "")
                    .replace("Mobile Safari", "Safari")
                userAgentString = customUserAgent

                Log.d("TossPayment", """
                WebView 설정 상태:
                JavaScript 활성화: $javaScriptEnabled
                DOM Storage 활성화: $domStorageEnabled
                User Agent: $userAgentString
                Mixed Content Mode: $mixedContentMode
            """.trimIndent())
            }

            webView.webViewClient = object : WebViewClient() {
                override fun onReceivedSslError(view: WebView?, handler: SslErrorHandler?, error: SslError?) {
                    if (BuildConfig.DEBUG) {
                        handler?.proceed()
                    } else {
                        super.onReceivedSslError(view, handler, error)
                    }
                }

                override fun onPageStarted(view: WebView?, url: String?, favicon: Bitmap?) {
                    super.onPageStarted(view, url, favicon)
                    Log.d("TossPayment", "페이지 로딩 시작: $url")
                }

                override fun onReceivedError(view: WebView?, errorCode: Int, description: String?, failingUrl: String?) {
                    Log.e("TossPayment", "페이지 로딩 에러: code=$errorCode, description=$description, url=$failingUrl")
                    super.onReceivedError(view, errorCode, description, failingUrl)
                    setResult(Activity.RESULT_CANCELED, Intent().apply {
                        putExtra("error", "페이지 로딩 중 오류가 발생했습니다: $description")
                    })
                    finish()
                }

                override fun shouldOverrideUrlLoading(view: WebView?, url: String?): Boolean {
                    Log.d("TossPayment", "URL 로딩 시도: $url")
                    url?.let {
                        if (isBackendEndpoint(it, "/api/payments/success")) {
                            Log.d("TossPayment", "결제 성공 URL 감지")
                            try {
                                val uri = Uri.parse(it)
                                val paymentKey = uri.getQueryParameter("paymentKey")
                                val orderId = uri.getQueryParameter("orderId")
                                val amount = uri.getQueryParameter("amount")?.toIntOrNull()

                                Log.d("TossPayment", "결제 성공 파라미터: paymentKey=$paymentKey, orderId=$orderId, amount=$amount")

                                if (paymentKey != null && orderId != null && amount != null) {
                                    setResult(Activity.RESULT_OK, Intent().apply {
                                        putExtra("paymentKey", paymentKey)
                                        putExtra("orderId", orderId)
                                        putExtra("amount", amount)
                                    })
                                    finish()
                                    return true
                                }
                            } catch (e: Exception) {
                                e.printStackTrace()
                                setResult(Activity.RESULT_CANCELED, Intent().apply {
                                    putExtra("error", "결제 처리 중 오류가 발생했습니다")
                                })
                                finish()
                                return true
                            }
                        } else if (isBackendEndpoint(it, "/api/payments/fail") ||
                                   it.contains("close") || 
                                   it.contains("cancel") || 
                                   it.contains("error")) {
                            deletePendingOrder()
                            val errorMessage = Uri.parse(it).getQueryParameter("message") ?: "결제가 취소되었습니다."
                            setResult(Activity.RESULT_CANCELED, Intent().apply {
                                putExtra("error", errorMessage)
                            })
                            finish()
                            return true
                        }

                        if (!it.startsWith("http://") && !it.startsWith("https://")) {
                            try {
                                val intent = Intent.parseUri(it, Intent.URI_INTENT_SCHEME)
                                startActivity(intent)
                                return true
                            } catch (e: Exception) {
                                e.printStackTrace()
                            }
                        }
                    }
                    return false
                }

                override fun onPageFinished(view: WebView?, url: String?) {
                    super.onPageFinished(view, url)
                    Log.d("TossPayment", "페이지 로딩 완료: $url")
                }

                override fun onLoadResource(view: WebView?, url: String?) {
                    super.onLoadResource(view, url)
                    Log.d("TossPayment", "리소스 로딩: $url")
                }
            }

            Log.d("TossPayment", "WebView 설정 완료")
        } catch (e: Exception) {
            Log.e("TossPayment", "WebView 설정 실패", e)
            android.app.AlertDialog.Builder(this)
                .setTitle("WebView 초기화 실패")
                .setMessage("WebView 초기화에 실패했습니다. 앱을 다시 실행해주세요.")
                .setPositiveButton("확인") { _, _ -> finish() }
                .setCancelable(false)
                .show()
        }
    }

    private fun isBackendEndpoint(url: String, path: String): Boolean {
        val uri = Uri.parse(url)
        val baseUri = Uri.parse(RetrofitClient.BASE_URL)
        return uri.scheme == baseUri.scheme &&
            uri.host.equals(baseUri.host, ignoreCase = true) &&
            uri.port == baseUri.port &&
            uri.path == path
    }

    private fun isAppInstalled(packageName: String): Boolean {
        return try {
            packageManager.getPackageInfo(packageName, 0)
            true
        } catch (e: Exception) {
            false
        }
    }

    private fun checkTossApp() {
        val isTossInstalled = isAppInstalled("viva.republica.toss")
        Log.d("TossPayment", "토스 앱 설치 여부: $isTossInstalled")
    }

    private fun checkWebViewPackage() {
        try {
            val webViewPackage = packageManager.getPackageInfo("com.google.android.webview", 0)
            val chromePackage = packageManager.getPackageInfo("com.android.chrome", 0)

            Log.d("TossPayment", """
                WebView 패키지 정보:
                System WebView: ${webViewPackage.versionName}
                Chrome: ${chromePackage.versionName}
            """.trimIndent())

        } catch (e: Exception) {
            Log.e("TossPayment", "WebView 패키지 확인 실패", e)
            // 사용자에게 안내 메시지 표시
            android.app.AlertDialog.Builder(this)
                .setTitle("WebView 업데이트 필요")
                .setMessage("정상적인 결제를 위해 Android System WebView와 Chrome 브라우저의 업데이트가 필요합니다.")
                .setPositiveButton("확인") { _, _ ->
                    // Play 스토어의 WebView 페이지로 이동
                    startActivity(Intent(Intent.ACTION_VIEW).apply {
                        data = Uri.parse("market://details?id=com.google.android.webview")
                    })
                    finish()
                }
                .setCancelable(false)
                .show()
        }
    }

    private fun deletePendingOrder() {
        try {
            val orderId = intent.getStringExtra("orderId")?.toIntOrNull()
            if (orderId != null) {
                val token = getSharedPreferences("auth", MODE_PRIVATE).getString("token", null)
                if (token != null) {
                    Thread {
                        try {
                            RetrofitClient.apiService.deletePendingOrder("Bearer $token", orderId).execute()
                        } catch (e: Exception) {
                            Log.e("TossPayment", "주문 삭제 실패", e)
                        }
                    }.start()
                }
            }
        } catch (e: Exception) {
            Log.e("TossPayment", "주문 삭제 처리 실패", e)
        }
    }
} 