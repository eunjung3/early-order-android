package com.example.myapplication

import android.content.Context
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.EditText
import android.widget.ImageButton
import android.widget.ImageView
import android.widget.TextView
import android.widget.Toast
import androidx.fragment.app.Fragment
import com.example.myapplication.R
import retrofit2.Call
import retrofit2.Callback
import retrofit2.Response
import android.text.method.HideReturnsTransformationMethod
import android.text.method.PasswordTransformationMethod
import androidx.fragment.app.FragmentManager

class ChangepasswordFragment : Fragment() {

    private var currentPassword: String = ""

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        currentPassword = arguments?.getString("current_password") ?: ""
    }

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        // fragment_changepassword 레이아웃 연결
        val view = inflater.inflate(R.layout.fragment_changepassword, container, false)

        // EditText, TextView 및 Button 초기화
        val passwordEditText: EditText = view.findViewById(R.id.ed_pw)
        val confirmPasswordEditText: EditText = view.findViewById(R.id.ed_pw2)
        val passwordErrorTextView: TextView = view.findViewById(R.id.tv_pw_error)
        val confirmPasswordErrorTextView: TextView = view.findViewById(R.id.tv_pw_error2)
        val completeButton: Button = view.findViewById(R.id.button2)
        val backButton: ImageButton = view.findViewById(R.id.btn_b2)
        val passwordToggle: ImageView = view.findViewById(R.id.password_toggle)
        val passwordToggle2: ImageView = view.findViewById(R.id.password_toggle2)

        var isPasswordVisible = false
        var isConfirmPasswordVisible = false

        passwordToggle.setOnClickListener {
            isPasswordVisible = !isPasswordVisible
            
            if (isPasswordVisible) {
                passwordEditText.transformationMethod = HideReturnsTransformationMethod.getInstance()
                passwordToggle.setImageResource(R.drawable.password_show)
            } else {
                passwordEditText.transformationMethod = PasswordTransformationMethod.getInstance()
                passwordToggle.setImageResource(R.drawable.password_hide)
            }
            passwordEditText.setSelection(passwordEditText.text.length)
        }

        passwordToggle2.setOnClickListener {
            isConfirmPasswordVisible = !isConfirmPasswordVisible
            
            if (isConfirmPasswordVisible) {
                confirmPasswordEditText.transformationMethod = HideReturnsTransformationMethod.getInstance()
                passwordToggle2.setImageResource(R.drawable.password_show)
            } else {
                confirmPasswordEditText.transformationMethod = PasswordTransformationMethod.getInstance()
                passwordToggle2.setImageResource(R.drawable.password_hide)
            }
            confirmPasswordEditText.setSelection(confirmPasswordEditText.text.length)
        }

        // 완료 버튼 클릭 이벤트 처리
        completeButton.setOnClickListener {
            val newPassword = passwordEditText.text.toString()
            val confirmPassword = confirmPasswordEditText.text.toString()

            when {
                newPassword.isEmpty() || confirmPassword.isEmpty() -> {
                    Toast.makeText(context, "모든 필드를 입력해주세요.", Toast.LENGTH_SHORT).show()
                }
                !isPasswordValid(newPassword) -> {
                    Toast.makeText(context, "비밀번호는 8자리 이상, 영문, 숫자, 특수문자를 포함해야 합니다.", Toast.LENGTH_SHORT).show()
                }
                newPassword != confirmPassword -> {
                    Toast.makeText(context, "비밀번호가 일치하지 않습니다.", Toast.LENGTH_SHORT).show()
                }
                else -> {
                    val authPrefs = requireContext().getSharedPreferences("auth", Context.MODE_PRIVATE)
                    val token = authPrefs.getString("token", null)

                    if (token != null) {
                        RetrofitClient.apiService.changePassword(
                            "Bearer $token",
                            ChangePasswordRequest(currentPassword, newPassword)
                        ).enqueue(object : Callback<PasswordVerificationResponse> {
                            override fun onResponse(
                                call: Call<PasswordVerificationResponse>,
                                response: Response<PasswordVerificationResponse>
                            ) {
                                if (response.isSuccessful && response.body()?.success == true) {
                                    Toast.makeText(context, "비밀번호가 변경되었습니다.", Toast.LENGTH_SHORT).show()
                                    requireActivity().supportFragmentManager.popBackStack(null, FragmentManager.POP_BACK_STACK_INCLUSIVE)
                                } else {
                                    Toast.makeText(context, "비밀번호 변경에 실패했습니다.", Toast.LENGTH_SHORT).show()
                                }
                            }

                            override fun onFailure(call: Call<PasswordVerificationResponse>, t: Throwable) {
                                Toast.makeText(context, "네트워크 오류가 발생했습니다.", Toast.LENGTH_SHORT).show()
                            }
                        })
                    }
                }
            }
        }

        backButton.setOnClickListener {
            // 뒤로가기 기능
            requireActivity().supportFragmentManager.beginTransaction()
                .setCustomAnimations(
                    R.anim.enter_from_left,  // 들어오는 애니메이션
                    R.anim.exit_to_right     // 나가는 애니메이션
                )
                .commit()

            requireActivity().supportFragmentManager.popBackStack()

        }

        return view
    }

    private fun isPasswordValid(password: String): Boolean {
        val passwordPattern = "^(?=.*[A-Za-z])(?=.*\\d)(?=.*[@\$!%*?&])[A-Za-z\\d@\$!%*?&]{8,}$"
        return Regex(passwordPattern).matches(password)
    }
}
