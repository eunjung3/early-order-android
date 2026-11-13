package com.example.myapplication

import android.os.Bundle
import android.text.Editable
import android.text.TextWatcher
import android.util.Patterns
import android.view.View
import android.widget.Button
import android.widget.EditText
import android.widget.ImageButton
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import retrofit2.Call
import retrofit2.Callback
import retrofit2.Response

class PwActivity : AppCompatActivity() {

    private lateinit var emailEditText: EditText
    private lateinit var btnSendPassword: Button
    private lateinit var errorEmailTextView: TextView

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_pw)

        // 뷰 초기화
        emailEditText = findViewById(R.id.ed_email)
        btnSendPassword = findViewById(R.id.button2)
        errorEmailTextView = findViewById(R.id.tv_email_error)
        val backButton = findViewById<ImageButton>(R.id.btn_b2)

        // 뒤로가기 버튼 리스너 설정
        backButton.setOnClickListener {
            onBackPressed()
        }

        // 비밀번호 받기 버튼 리스너 설정
        btnSendPassword.setOnClickListener {
            val email = emailEditText.text.toString().trim()
            
            when {
                email.isEmpty() -> {
                    showError("이메일을 입력해주세요.")
                }
                !Patterns.EMAIL_ADDRESS.matcher(email).matches() -> {
                    showError("올바른 이메일 주소를 입력해주세요.")
                }
                else -> {
                    sendPasswordResetRequest(email)
                }
            }
        }
    }

    private fun showError(message: String) {
        errorEmailTextView.text = message
        errorEmailTextView.visibility = View.VISIBLE
    }

    private fun sendPasswordResetRequest(email: String) {
        val request = FindPasswordRequest(email)
        
        RetrofitClient.apiService.findPassword(request)
            .enqueue(object : Callback<FindPasswordResponse> {
                override fun onResponse(
                    call: Call<FindPasswordResponse>,
                    response: Response<FindPasswordResponse>
                ) {
                    if (response.isSuccessful && response.body()?.success == true) {
                        Toast.makeText(
                            this@PwActivity,
                            "임시 비밀번호가 이메일로 전송되었습니다.",
                            Toast.LENGTH_LONG
                        ).show()
                        finish()
                    } else {
                        showError("가입되어 있지 않은 이메일입니다.")
                    }
                }

                override fun onFailure(call: Call<FindPasswordResponse>, t: Throwable) {
                    Toast.makeText(
                        this@PwActivity,
                        "네트워크 오류가 발생했습니다.",
                        Toast.LENGTH_SHORT
                    ).show()
                }
            })
    }
}
