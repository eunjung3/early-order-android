package com.example.myapplication

import android.annotation.SuppressLint
import android.content.Context
import android.content.Intent
import android.graphics.Color
import android.os.Bundle
import android.os.CountDownTimer
import android.os.Handler
import android.os.Looper
import android.text.Editable
import android.text.InputFilter
import android.text.SpannableString
import android.text.Spanned
import android.text.TextWatcher
import android.text.style.UnderlineSpan
import android.util.Log
import android.view.View
import android.widget.Button
import android.widget.EditText
import android.widget.ImageButton
import android.widget.LinearLayout
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.lifecycleScope
import retrofit2.Call
import retrofit2.Callback
import retrofit2.Response
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

class EmailActivity : AppCompatActivity() {

    private lateinit var tvTimer: TextView
    private lateinit var tvErrorMessage: TextView
    private lateinit var verifyButton: Button
    private lateinit var resendButton: Button
    private lateinit var backButton: ImageButton
    private lateinit var email: String

    private var countDownTimer: CountDownTimer? = null
    private val timerDuration: Long = 180000
    private val tickInterval: Long = 1000

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_email)

        Log.d("EmailActivity", "EmailActivity 시작")

        // View 초기화
        tvTimer = findViewById(R.id.tv_timer)
        tvErrorMessage = findViewById(R.id.tv_error_message)
        verifyButton = findViewById(R.id.btn_verify)
        resendButton = findViewById(R.id.btn_resend_code)
        backButton = findViewById(R.id.btn_b2)

        val spannableText = SpannableString("인증 코드 재전송")
        spannableText.setSpan(UnderlineSpan(), 0, spannableText.length, Spanned.SPAN_EXCLUSIVE_EXCLUSIVE)
        resendButton.text = spannableText

        email = intent.getStringExtra("email") ?: ""

        startTimer()

        val verificationMessage = findViewById<TextView>(R.id.tv_email)
        val codeInputs = findViewById<LinearLayout>(R.id.linear_code_input)

        verificationMessage.text = "인증 코드가 $email 로 발송되었습니다."

        verifyButton.setOnClickListener {
            val userInputCode = collectCodeInput(codeInputs)
            verifyCode(userInputCode)
        }

        resendButton.setOnClickListener {
            startTimer()
            tvErrorMessage.visibility = View.GONE
        }

        backButton.setOnClickListener {
            onBackPressed()
        }

        // 인증 코드 입력 필드 설정
        setupCodeInputs(codeInputs)
    }

    private fun setupCodeInputs(codeInputs: LinearLayout) {
        for (i in 0 until codeInputs.childCount) {
            val editText = codeInputs.getChildAt(i) as EditText

            editText.filters = arrayOf(InputFilter { source, _, _, _, _, _ ->
                // 영문과 숫자만 허용하는 정규식 패턴
                val pattern = "[A-Za-z0-9]".toRegex()
                val filteredString = source.toString().filter { it.toString().matches(pattern) }
                if (filteredString.isEmpty()) {
                    return@InputFilter ""
                }
                // 대문자로 변환
                return@InputFilter filteredString.uppercase()
            })

            editText.setOnKeyListener { _, keyCode, event ->
                if (keyCode == android.view.KeyEvent.KEYCODE_DEL && 
                    event.action == android.view.KeyEvent.ACTION_DOWN && 
                    editText.text.isEmpty() && 
                    i > 0) {
                    (codeInputs.getChildAt(i - 1) as EditText).apply {
                        requestFocus()
                        setSelection(length())
                    }
                    return@setOnKeyListener true
                }
                false
            }

            editText.addTextChangedListener(object : TextWatcher {
                override fun afterTextChanged(s: Editable?) {
                    if (s?.length == 1 && i < codeInputs.childCount - 1) {
                        (codeInputs.getChildAt(i + 1) as EditText).requestFocus()
                    }
                    updateVerifyButtonState(codeInputs)
                }
                override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}
                override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {}
            })
        }
    }

    private fun collectCodeInput(layout: LinearLayout): String {
        val codeBuilder = StringBuilder()
        for (i in 0 until layout.childCount) {
            val view = layout.getChildAt(i)
            if (view is EditText) {
                codeBuilder.append(view.text.toString().trim())
            }
        }
        return codeBuilder.toString()
    }

    private fun verifyCode(inputCode: String) {
        val request = VerificationRequest(email, inputCode)
        RetrofitClient.apiService.verifyEmail(request)
            .enqueue(object : Callback<VerificationResponse> {
                override fun onResponse(call: Call<VerificationResponse>, response: Response<VerificationResponse>) {
                    if (response.isSuccessful && response.body()?.success == true) {
                        // 성공 메시지 표시
                        Toast.makeText(this@EmailActivity, "회원가입이 완료되었습니다.", Toast.LENGTH_SHORT).show()
                        
                        // 1.5초 후에 로그인 화면으로 이동
                        Handler(Looper.getMainLooper()).postDelayed({
                            val intent = Intent(this@EmailActivity, LoginActivity::class.java)
                            intent.flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
                            startActivity(intent)
                            finish()
                        }, 1500) // 1.5초 대기
                    } else {
                        Toast.makeText(this@EmailActivity, "인증번호가 일치하지 않습니다.", Toast.LENGTH_SHORT).show()
                    }
                }

                override fun onFailure(call: Call<VerificationResponse>, t: Throwable) {
                    Toast.makeText(this@EmailActivity, "네트워크 오류가 발생했습니다.", Toast.LENGTH_SHORT).show()
                }
            })
    }

    // 타이머 관련 함수들
    private fun startTimer() {
        countDownTimer?.cancel()
        
        countDownTimer = object : CountDownTimer(timerDuration, tickInterval) {
            override fun onTick(millisUntilFinished: Long) {
                val secondsRemaining = (millisUntilFinished / 1000).toInt()
                val minutes = secondsRemaining / 60
                val seconds = secondsRemaining % 60
                tvTimer.text = String.format("%02d:%02d", minutes, seconds)
            }

            override fun onFinish() {
                tvTimer.text = "시간 초과"
                verifyButton.apply {
                    isEnabled = false
                    alpha = 0.5f
                }
                tvErrorMessage.text = "인증 시간이 초과되었습니다. 새로 요청하세요."
                tvErrorMessage.visibility = View.VISIBLE
            }
        }.start()
    }

    private fun updateVerifyButtonState(layout: LinearLayout) {
        var isAllFieldsFilled = true
        for (i in 0 until layout.childCount) {
            val view = layout.getChildAt(i)
            if (view is EditText && view.text.toString().trim().isEmpty()) {
                isAllFieldsFilled = false
                break
            }
        }

        verifyButton.apply {
            isEnabled = isAllFieldsFilled
            alpha = if (isAllFieldsFilled) 1.0f else 0.5f
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        countDownTimer?.cancel()
    }
}
