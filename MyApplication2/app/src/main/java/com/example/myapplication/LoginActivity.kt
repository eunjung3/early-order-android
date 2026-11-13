package com.example.myapplication

import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.text.Editable
import android.text.InputType
import android.text.TextWatcher
import android.util.Log
import android.view.View
import android.widget.Button
import android.widget.CheckBox
import android.widget.EditText
import android.widget.ImageView
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import retrofit2.Call
import retrofit2.Callback
import retrofit2.Response

class LoginActivity : AppCompatActivity() {

    private var isPasswordVisible = false
    private lateinit var errorEmailTextView: TextView
    private lateinit var errorPasswordTextView: TextView
    private lateinit var autoLoginCheckBox: CheckBox

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_login)

        val emailEditText = findViewById<EditText>(R.id.ed_id)
        val passwordEditText = findViewById<EditText>(R.id.ed_pw)
        val loginButton = findViewById<Button>(R.id.btn_login)
        val passwordButton = findViewById<Button>(R.id.btn_pwr)
        val signUpButton = findViewById<Button>(R.id.btn_sig)
        val backButton = findViewById<ImageView>(R.id.btn_b2)
        val passwordToggle = findViewById<ImageView>(R.id.password_toggle)
        autoLoginCheckBox = findViewById(R.id.checkbox_auto_login)

        errorEmailTextView = findViewById(R.id.tv_email_error)
        errorPasswordTextView = findViewById(R.id.tv_password_error)

        val authPrefs = getSharedPreferences("auth", Context.MODE_PRIVATE)
        val isAutoLogin = authPrefs.getBoolean("auto_login", false)

        if (isAutoLogin) {
            val savedEmail = authPrefs.getString("saved_email", "")
            val savedPassword = authPrefs.getString("saved_password", "")

            // 이메일과 비밀번호가 저장되어 있으면 자동으로 입력
            if (!savedEmail.isNullOrEmpty() && !savedPassword.isNullOrEmpty()) {
                performLogin(savedEmail, savedPassword)
            }
        } else {
            authPrefs.edit().clear().apply()
        }

        fun isEmailValid(email: String): Boolean {
            return android.util.Patterns.EMAIL_ADDRESS.matcher(email).matches()
        }

        // 비밀번호 입력 필드의 초기 inputType 설정
        passwordEditText.inputType = InputType.TYPE_CLASS_TEXT or InputType.TYPE_TEXT_VARIATION_PASSWORD

        // 비밀번호 표시/숨기기 기능
        passwordToggle.setOnClickListener {
            togglePasswordVisibility(passwordEditText, passwordToggle)
        }

        // 입력 필드에 TextWatcher 추가
        emailEditText.addTextChangedListener(object : TextWatcher {
            override fun afterTextChanged(s: Editable?) {
                errorEmailTextView.visibility = View.GONE
            }
            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}
            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {}
        })

        passwordEditText.addTextChangedListener(object : TextWatcher {
            override fun afterTextChanged(s: Editable?) {
                errorPasswordTextView.visibility = View.GONE
            }
            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}
            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {}
        })

        // 로그인 버튼 클릭 리스너 설정
        loginButton.setOnClickListener {
            val email = emailEditText.text.toString()
            val password = passwordEditText.text.toString()

            errorEmailTextView.visibility = View.GONE
            errorPasswordTextView.visibility = View.GONE

            if (email.isEmpty()) {
                errorEmailTextView.text = "이메일을 입력해주세요."
                errorEmailTextView.visibility = View.VISIBLE
            } else if (!isEmailValid(email)) {
                errorEmailTextView.text = "올바른 이메일 주소를 입력해주세요."
                errorEmailTextView.visibility = View.VISIBLE
            } else if (password.isEmpty()) {
                errorPasswordTextView.text = "비밀번호를 입력해주세요."
                errorPasswordTextView.visibility = View.VISIBLE
            } else {
                val loginRequest = LoginRequest(email, password)
                RetrofitClient.apiService.login(loginRequest).enqueue(object : Callback<LoginResponse> {
                    override fun onResponse(
                        call: Call<LoginResponse>,
                        response: Response<LoginResponse>
                    ) {
                        if (response.isSuccessful) {
                            val loginResponse = response.body()
                            if (loginResponse != null && loginResponse.success) {
                                Toast.makeText(this@LoginActivity, "로그인 성공", Toast.LENGTH_SHORT).show()

                                // SharedPreferences를 하나로 통합
                                val authPrefs = getSharedPreferences("auth", Context.MODE_PRIVATE)
                                authPrefs.edit().apply {
                                    if (autoLoginCheckBox.isChecked) {
                                        // 자동 로그인이 체크된 경우에만 모든 정보 저장
                                        putString("token", loginResponse.access_token)
                                        putBoolean("is_admin", loginResponse.is_admin)
                                        putBoolean("auto_login", true)
                                        putString("saved_email", email)
                                        putString("saved_password", password)
                                    } else {
                                        // 자동 로그인이 체크되지 않은 경우 저장된 정보 모두 삭제
                                        clear()
                                        // 현재 세션에서만 사용할 토큰 저장
                                        putString("token", loginResponse.access_token)
                                    }
                                    apply()
                                }

                                // HomeActivity로 이동
                                val intent = Intent(this@LoginActivity, HomeActivity::class.java)
                                intent.flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP
                                intent.putExtra("refresh_mypage", true)
                                startActivity(intent)
                                finish()
                            } else {
                                Toast.makeText(this@LoginActivity, "로그인 실패", Toast.LENGTH_SHORT).show()
                            }
                        } else if (response.code() == 401) {
                            val errorBody = response.errorBody()?.string()
                            if (errorBody?.contains("Email not verified") == true) {
                                Toast.makeText(this@LoginActivity, "이메일 인증이 필요합니다.", Toast.LENGTH_SHORT).show()
                                val intent = Intent(this@LoginActivity, EmailActivity::class.java)
                                intent.putExtra("email", email)
                                startActivity(intent)
                            } else {
                                Toast.makeText(this@LoginActivity, "아이디 또는 비밀번호가 잘못되었습니다.", Toast.LENGTH_SHORT).show()
                            }
                        } else {
                            Toast.makeText(this@LoginActivity, "서버 오류가 발생했습니다.", Toast.LENGTH_SHORT).show()
                        }
                    }

                    override fun onFailure(call: Call<LoginResponse>, t: Throwable) {
                        // 네트워크 오류 처리
                        Toast.makeText(this@LoginActivity, "네트워크 오류가 발생했습니다.", Toast.LENGTH_SHORT)
                            .show()
                    }
                })
            }
        }

        passwordButton.setOnClickListener {
            val intent = Intent(this, PwActivity::class.java)
            startActivity(intent)
        }

        signUpButton.setOnClickListener {
            val intent = Intent(this, SignUpActivity::class.java)
            startActivity(intent)
        }

        backButton.setOnClickListener {
            val intent = Intent(this, HomeActivity::class.java).apply {
                putExtra("navigate_to", "mypage")
                flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP
            }
            startActivity(intent)
            finish()
        }

        // 자동 로그인 체크박스 상태 변경 시
        autoLoginCheckBox.setOnCheckedChangeListener { _, isChecked ->
            if (!isChecked) {
                val authPrefs = getSharedPreferences("auth", Context.MODE_PRIVATE)
                authPrefs.edit().clear().apply()
            }
        }
    }

    private fun performLogin(email: String, password: String) {
        val loginRequest = LoginRequest(email, password)
        RetrofitClient.apiService.login(loginRequest).enqueue(object : Callback<LoginResponse> {
            override fun onResponse(call: Call<LoginResponse>, response: Response<LoginResponse>) {
                if (response.isSuccessful && response.body()?.success == true) {
                    val loginResponse = response.body()!!
                    handleLoginSuccess(loginResponse, email, password)
                } else {
                    Toast.makeText(this@LoginActivity, "로그인 실패", Toast.LENGTH_SHORT).show()
                }
            }

            override fun onFailure(call: Call<LoginResponse>, t: Throwable) {
                Toast.makeText(this@LoginActivity, "네트워크 오류", Toast.LENGTH_SHORT).show()
            }
        })
    }

    private fun handleLoginSuccess(loginResponse: LoginResponse, email: String, password: String) {
        val authPrefs = getSharedPreferences("auth", Context.MODE_PRIVATE)
        authPrefs.edit().apply {
            if (autoLoginCheckBox.isChecked) {
                // 자동 로그인이 체크된 경우 - 모든 정보 영구 저장
                putString("token", loginResponse.access_token)
                putBoolean("is_admin", loginResponse.is_admin)
                putBoolean("auto_login", true)
                putString("saved_email", email)
                putString("saved_password", password)
            } else {
                // 자동 로그인이 체크되지 않은 경우 - 현재 세션용 임시 저장
                putString("temp_token", loginResponse.access_token)
                putBoolean("is_admin", loginResponse.is_admin)
                putBoolean("auto_login", false)
            }
            apply()
        }

        val intent = Intent(this@LoginActivity, HomeActivity::class.java)
        startActivity(intent)
        finish()
    }

    private fun togglePasswordVisibility(editText: EditText, toggleButton: ImageView) {
        if (isPasswordVisible) {
            // 비밀번호 숨김 상태로 변경
            editText.inputType = InputType.TYPE_CLASS_TEXT or InputType.TYPE_TEXT_VARIATION_PASSWORD
            toggleButton.setImageResource(R.drawable.password_hide)
        } else {
            // 비밀번호 표시 상태로 변경
            editText.inputType = InputType.TYPE_CLASS_TEXT or InputType.TYPE_TEXT_VARIATION_VISIBLE_PASSWORD
            toggleButton.setImageResource(R.drawable.password_show)
        }
        editText.setSelection(editText.text.length)
        isPasswordVisible = !isPasswordVisible
    }
}
