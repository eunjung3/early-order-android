package com.example.myapplication

import android.annotation.SuppressLint
import android.content.Intent
import android.os.Bundle
import android.text.InputType
import android.util.Log
import android.util.Patterns
import android.view.View
import android.widget.Button
import android.widget.EditText
import android.widget.ImageView
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import retrofit2.Call
import retrofit2.Callback
import retrofit2.Response
import android.widget.ImageButton
import android.content.Context

class SignUpActivity : AppCompatActivity() {

    private var isPasswordVisible = false
    private var isConfirm_PasswordVisible = false
    private var isEmailAvailable = false

    private lateinit var errorNameTextView: TextView
    private lateinit var errorEmailTextView: TextView
    private lateinit var errorPasswordTextView: TextView
    private lateinit var errorConfirm_PasswordTextView: TextView

    // 이메일 유효성 검사 함수
    fun isEmailValid(email: String): Boolean {
        return Patterns.EMAIL_ADDRESS.matcher(email).matches()
    }

    fun isPasswordValid(password: String): Boolean {
        // 비밀번호는 8자리 이상이며 영문, 숫자, 특수문자가 최소 하나씩 포함되어야 함
        val passwordPattern = "^(?=.*[A-Za-z])(?=.*\\d)(?=.*[@\$!%*?&])[A-Za-z\\d@\$!%*?&]{8,}$"
        return Regex(passwordPattern).matches(password)
    }

    @SuppressLint("MissingInflatedId")
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_sign_up)

        val signUpEmailEditText = findViewById<EditText>(R.id.ed_email)
        val passwordEditText = findViewById<EditText>(R.id.ed_pwd)
        val passwordEditText2 = findViewById<EditText>(R.id.ed_pwd2)
        val signUpNameEditText = findViewById<EditText>(R.id.ed_name)
        val sendVerificationButton = findViewById<Button>(R.id.btn_ok)
        val backButton = findViewById<ImageButton>(R.id.btn_b2)
        val passwordToggle = findViewById<ImageView>(R.id.passwordToggle)
        val passwordToggle2 = findViewById<ImageView>(R.id.passwordToggle2)
        val checkEmailButton = findViewById<Button>(R.id.btn_emailcheck)

        errorNameTextView = findViewById(R.id.tv_name_error)
        errorEmailTextView = findViewById(R.id.tv_email_error)
        errorPasswordTextView = findViewById(R.id.tv_pwd_error)
        errorConfirm_PasswordTextView = findViewById(R.id.tv_pwd2_error)

        signUpEmailEditText.inputType = InputType.TYPE_CLASS_TEXT or InputType.TYPE_TEXT_VARIATION_EMAIL_ADDRESS
        passwordEditText.inputType = InputType.TYPE_CLASS_TEXT or InputType.TYPE_TEXT_VARIATION_PASSWORD
        passwordEditText2.inputType = InputType.TYPE_CLASS_TEXT or InputType.TYPE_TEXT_VARIATION_PASSWORD

        sendVerificationButton.setOnClickListener {
            val username = signUpNameEditText.text.toString().trim()
            val email = signUpEmailEditText.text.toString().trim()
            val password = passwordEditText.text.toString()
            val confirm_Password = passwordEditText2.text.toString()

            clearErrorMessages()

            when {
                username.isEmpty() -> {
                    errorNameTextView.text = "이름을 입력해주세요."
                    errorNameTextView.visibility = View.VISIBLE
                }
                email.isEmpty() -> {
                    errorEmailTextView.text = "이메일을 입력해주세요."
                    errorEmailTextView.visibility = View.VISIBLE
                }
                !isEmailValid(email) -> {
                    errorEmailTextView.text = "올바른 이메일 형식을 입력해주세요."
                    errorEmailTextView.visibility = View.VISIBLE
                }
                !isPasswordValid(password) -> {
                    errorPasswordTextView.text = "비밀번호는 8자리 이상, 영문, 숫자, 특수문자를 포함해야 합니다."
                    errorPasswordTextView.visibility = View.VISIBLE
                }
                password != confirm_Password -> {
                    errorConfirm_PasswordTextView.text = "비밀번호가 일치하지 않습니다."
                    errorConfirm_PasswordTextView.visibility = View.VISIBLE
                }
                !isEmailAvailable -> {
                    errorEmailTextView.text = "이메일 중복 확인이 필요합니다."
                    errorEmailTextView.visibility = View.VISIBLE
                }
                else -> {
                    // 서버로 회원가입 요청
                    callSignUpApi(email, password, username, confirm_Password)
                }
            }
        }

        checkEmailButton.setOnClickListener {
            val email = signUpEmailEditText.text.toString()
            if (email.isEmpty()) {
                errorEmailTextView.text = "이메일을 입력해주세요."
                errorEmailTextView.visibility = View.VISIBLE
            } else if (!isEmailValid(email)) {
                errorEmailTextView.text = "올바른 이메일 형식을 입력해주세요."
                errorEmailTextView.visibility = View.VISIBLE
            } else {
                checkEmailDuplicate(email)
            }
        }

        backButton.setOnClickListener {
            onBackPressed()
        }

        // 비밀번호 토글 설정
        passwordToggle.setOnClickListener {
            togglePasswordVisibility(passwordEditText, passwordToggle)
        }

        passwordToggle2.setOnClickListener {
            togglePasswordVisibility(passwordEditText2, passwordToggle2)
        }
    }

    // 비밀번호 가시성 토글 함수
    private fun togglePasswordVisibility(editText: EditText, toggleButton: ImageView) {
        if (isPasswordVisible) {
            editText.inputType = InputType.TYPE_CLASS_TEXT or InputType.TYPE_TEXT_VARIATION_PASSWORD
            toggleButton.setImageResource(R.drawable.password_hide)
        } else {
            editText.inputType = InputType.TYPE_CLASS_TEXT or InputType.TYPE_TEXT_VARIATION_VISIBLE_PASSWORD
            toggleButton.setImageResource(R.drawable.password_show)
        }
        editText.setSelection(editText.text.length)
        isPasswordVisible = !isPasswordVisible
    }

    private fun clearErrorMessages() {
        errorNameTextView.visibility = View.GONE
        errorEmailTextView.visibility = View.GONE
        errorPasswordTextView.visibility = View.GONE
        errorConfirm_PasswordTextView.visibility = View.GONE
    }

    // 인증 코드 전송 후 EmailActivity로 이동하는 부분
    private fun sendVerificationCode(email: String, password: String, username: String, confirm_Password: String) {
        Log.d("SignUpActivity", "인증 코드 전송 요청 시작")
        val emailRequest = EmailRequest(email)
        RetrofitClient.apiService.sendVerificationCode(emailRequest).enqueue(object : Callback<VerificationResponse> {
            override fun onResponse(call: Call<VerificationResponse>, response: Response<VerificationResponse>) {
                if (response.isSuccessful && response.body() != null) {
                    Log.d("SignUpActivity", "인증 코드 전송 성공, EmailActivity로 이동")
                    val intent = Intent(this@SignUpActivity, EmailActivity::class.java)
                    intent.putExtra("email", email)
                    intent.putExtra("password", password)
                    intent.putExtra("username", username)
                    intent.putExtra("confirm_password", confirm_Password)
                    startActivity(intent)
                } else {
                    Log.d("SignUpActivity", "인증 코드 전송 실패")
                    Toast.makeText(this@SignUpActivity, "인증 코드 전송에 실패했습니다.", Toast.LENGTH_SHORT).show()
                }
            }

            override fun onFailure(call: Call<VerificationResponse>, t: Throwable) {
                Log.e("SignUpActivity", "인증 코드 전송 네트워크 오류", t)
                Toast.makeText(this@SignUpActivity, "네트워크 오류가 발생했습니다.", Toast.LENGTH_SHORT).show()
            }
        })
    }

    // Retrofit을 사용한 회원가입 API 호출 함수
    private fun callSignUpApi(email: String, password: String, username: String, confirm_Password: String) {
        Log.d("SignUpActivity", "회원가입 API 호출 시작")
        
        val signUpRequest = SignUpRequest(
            email = email,
            password = password,
            username = username,
            confirm_password = confirm_Password,
            is_admin = false
        )
        
        RetrofitClient.apiService.signUp(signUpRequest).enqueue(object : Callback<SignUpResponse> {
            override fun onResponse(call: Call<SignUpResponse>, response: Response<SignUpResponse>) {
                if (response.isSuccessful && response.body()?.success == true) {
                    Log.d("SignUpActivity", "회원가입 성공, 인증 코드 전송 시작")
                    sendVerificationCode(email, password, username, confirm_Password)
                } else {
                    Log.d("SignUpActivity", "회원가입 실패: ${response.errorBody()?.string()}")
                    Toast.makeText(this@SignUpActivity, "회원가입에 실패했습니다.", Toast.LENGTH_SHORT).show()
                }
            }

            override fun onFailure(call: Call<SignUpResponse>, t: Throwable) {
                Log.e("SignUpActivity", "회원가입 네트워크 오류", t)
                Toast.makeText(this@SignUpActivity, "네트워크 오류가 발생했습니다.", Toast.LENGTH_SHORT).show()
            }
        })
    }

    private fun checkEmailDuplicate(email: String) {
        val emailCheckRequest = EmailCheckRequest(email)
        RetrofitClient.apiService.checkEmailDuplicate(emailCheckRequest).enqueue(object : Callback<EmailCheckResponse> {
            override fun onResponse(
                call: Call<EmailCheckResponse>,
                response: Response<EmailCheckResponse>
            ) {
                if (response.isSuccessful) {
                    val emailCheckResponse = response.body()
                    Log.d("EmailCheckResponse", "Available: ${emailCheckResponse?.available}")
                    if (emailCheckResponse != null && emailCheckResponse.available) {
                        isEmailAvailable = true
                        // 이메일 사용 가능
                        errorEmailTextView.text = "사용 가능한 이메일입니다."
                        errorEmailTextView.setTextColor(resources.getColor(R.color.colorSuccess))  // 성공 메시지 색상 설정
                    } else {
                        isEmailAvailable = false
                        // 이메일 중복됨
                        errorEmailTextView.text = "이미 사용 중인 이메일입니다."
                        errorEmailTextView.setTextColor(resources.getColor(R.color.colorSuccess))  // 오류 메시지 색상 설정
                    }
                    errorEmailTextView.visibility = View.VISIBLE
                } else {
                    // 서버 오류 처리
                    Toast.makeText(this@SignUpActivity, "서버와의 통신에 실패했습니다.", Toast.LENGTH_SHORT)
                        .show()
                }
            }

            override fun onFailure(call: Call<EmailCheckResponse>, t: Throwable) {
                // 네트워크 오류 처리
                Toast.makeText(this@SignUpActivity, "네트워크 오류가 발생했습니다.", Toast.LENGTH_SHORT).show()
            }
        })
    }
}
