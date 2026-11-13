package com.example.myapplication

import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.text.Editable
import android.text.TextWatcher
import android.util.Patterns
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.EditText
import android.widget.ImageButton
import android.widget.Toast
import androidx.fragment.app.Fragment
import com.google.android.material.bottomnavigation.BottomNavigationView
import com.google.android.material.button.MaterialButton
import retrofit2.Call
import retrofit2.Callback
import retrofit2.Response

class ChangeEmailFragment : Fragment() {
    private lateinit var edEmail: EditText
    private lateinit var btnEmailCheck: Button
    private lateinit var btnNext: MaterialButton
    private lateinit var btnBack: ImageButton
    private var currentEmail: String = ""
    private var isEmailAvailable = false
    private var isEmailChecked = false

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        return inflater.inflate(R.layout.fragment_change_email, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        
        // 하단 네비게이션 바 숨기기
        (activity as? HomeActivity)?.findViewById<BottomNavigationView>(R.id.bottom_navigation)?.visibility = View.GONE
        
        initViews(view)
        loadCurrentEmail()
        setupListeners()
    }

    private fun initViews(view: View) {
        edEmail = view.findViewById(R.id.ed_email)
        btnEmailCheck = view.findViewById(R.id.btn_emailcheck)
        btnNext = view.findViewById(R.id.btn_next)
        btnBack = view.findViewById(R.id.btn_b2)
        
        // 초기 상태에서 다음 버튼 비활성화
        btnNext.isEnabled = false
        btnNext.alpha = 0.5f
        btnNext.setTextColor(android.graphics.Color.WHITE)
    }

    private fun loadCurrentEmail() {
        val authPrefs = requireContext().getSharedPreferences("auth", Context.MODE_PRIVATE)
        val token = authPrefs.getString("token", null)

        if (token != null) {
            RetrofitClient.apiService.getUserInfo("Bearer $token")
                .enqueue(object : Callback<UserInfoResponse> {
                    override fun onResponse(
                        call: Call<UserInfoResponse>,
                        response: Response<UserInfoResponse>
                    ) {
                        if (response.isSuccessful && response.body()?.success == true) {
                            response.body()?.data?.let { userInfo ->
                                currentEmail = userInfo.email
                                edEmail.setText(currentEmail)
                                updateCheckButtonState()
                            }
                        }
                    }
                    
                    override fun onFailure(call: Call<UserInfoResponse>, t: Throwable) {
                        Toast.makeText(context, "로그인 후 이용해주세요.", Toast.LENGTH_SHORT).show()
                    }
                })
        }
    }

    private fun setupListeners() {
        // 뒤로가기 버튼 리스너
        btnBack.setOnClickListener {
            parentFragmentManager.popBackStack()
        }

        edEmail.addTextChangedListener(object : TextWatcher {
            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}
            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {
                updateCheckButtonState()
                isEmailAvailable = false
                isEmailChecked = false
                btnNext.isEnabled = false
                btnNext.setTextColor(android.graphics.Color.WHITE)
            }
            override fun afterTextChanged(s: Editable?) {}
        })

        btnEmailCheck.setOnClickListener {
            val newEmail = edEmail.text.toString().trim()
            when {
                !isEmailValid(newEmail) -> {
                    Toast.makeText(context, "올바른 이메일 형식을 입력해주세요", Toast.LENGTH_SHORT).show()
                }
                else -> checkEmailDuplicate(newEmail)
            }
        }

        btnNext.setOnClickListener {
            val newEmail = edEmail.text.toString().trim()
            if (!isEmailChecked) {
                Toast.makeText(context, "이메일 중복 확인이 필요합니다", Toast.LENGTH_SHORT).show()
            } else if (isEmailAvailable) {
                // 이메일 인증 코드 전송
                sendVerificationCode(newEmail)
            }
        }
    }

    private fun updateCheckButtonState() {
        val newEmail = edEmail.text.toString().trim()
        
        // 확인 버튼 상태 업데이트 (빈 칸이거나 현재 이메일과 같을 때만 비활성화)
        btnEmailCheck.isEnabled = newEmail.isNotEmpty() && newEmail != currentEmail
        btnEmailCheck.alpha = if (btnEmailCheck.isEnabled) 1.0f else 0.5f
        
        // 다음 버튼 상태 업데이트 (이메일이 사용 가능하고 중복 확인이 완료되었을 때만 활성화)
        btnNext.isEnabled = isEmailAvailable && isEmailChecked
        btnNext.alpha = if (btnNext.isEnabled) 1.0f else 0.5f
        btnNext.setTextColor(android.graphics.Color.WHITE)
    }

    private fun isEmailValid(email: String): Boolean {
        return Patterns.EMAIL_ADDRESS.matcher(email).matches()
    }

    private fun checkEmailDuplicate(email: String) {
        isEmailChecked = true  // 이메일 중복 확인 시도 표시
        RetrofitClient.apiService.checkEmailDuplicate(EmailCheckRequest(email))
            .enqueue(object : Callback<EmailCheckResponse> {
                override fun onResponse(
                    call: Call<EmailCheckResponse>,
                    response: Response<EmailCheckResponse>
                ) {
                    if (response.isSuccessful) {
                        response.body()?.let { emailCheckResponse ->
                            isEmailAvailable = emailCheckResponse.available
                            btnNext.isEnabled = emailCheckResponse.available
                            btnNext.alpha = if (emailCheckResponse.available) 1.0f else 0.5f
                            btnNext.setTextColor(android.graphics.Color.WHITE)
                            
                            if (emailCheckResponse.available) {
                                Toast.makeText(context, "사용 가능한 이메일입니다", Toast.LENGTH_SHORT).show()
                            } else {
                                Toast.makeText(context, "이미 사용 중인 이메일입니다", Toast.LENGTH_SHORT).show()
                            }
                        }
                    }
                }

                override fun onFailure(call: Call<EmailCheckResponse>, t: Throwable) {
                    isEmailChecked = false  // 확인 실패 시 초기화
                    Toast.makeText(context, "네트워크 오류가 발생했습니다", Toast.LENGTH_SHORT).show()
                }
            })
    }

    private fun sendVerificationCode(email: String) {
        // EmailActivity로 바로 이동
        val intent = Intent(requireContext(), EmailActivity::class.java).apply {
            putExtra("email", email)
            putExtra("isChangeEmail", true)
        }
        startActivity(intent)
    }

    private fun updateEmail(newEmail: String) {
        val authPrefs = requireContext().getSharedPreferences("auth", Context.MODE_PRIVATE)
        val token = authPrefs.getString("token", null)

        if (token != null) {
            RetrofitClient.apiService.updateEmail("Bearer $token", UpdateEmailRequest(newEmail))
                .enqueue(object : Callback<UpdateEmailResponse> {
                    override fun onResponse(
                        call: Call<UpdateEmailResponse>,
                        response: Response<UpdateEmailResponse>
                    ) {
                        if (response.isSuccessful && response.body()?.success == true) {
                            Toast.makeText(context, "이메일이 성공적으로 변경되었습니다.", Toast.LENGTH_SHORT).show()
                            parentFragmentManager.popBackStack()
                        } else {
                            Toast.makeText(context, response.body()?.message ?: "이메일 변경에 실패했습니다.", Toast.LENGTH_SHORT).show()
                        }
                    }

                    override fun onFailure(call: Call<UpdateEmailResponse>, t: Throwable) {
                        Toast.makeText(context, "네트워크 오류가 발생했습니다.", Toast.LENGTH_SHORT).show()
                    }
                })
        }
    }
}