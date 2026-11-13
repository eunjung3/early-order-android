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
import android.widget.Toast
import androidx.fragment.app.Fragment
import retrofit2.Call
import retrofit2.Callback
import retrofit2.Response
import android.text.method.HideReturnsTransformationMethod
import android.text.method.PasswordTransformationMethod

class ChangepwFragment : Fragment() {

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        // fragment_changepw 레이아웃 연결
        val view = inflater.inflate(R.layout.fragment_changepw, container, false)

        // EditText와 Button 초기화
        val currentPasswordEditText: EditText = view.findViewById(R.id.ed_pw)
        val nextButton: Button = view.findViewById(R.id.button2)
        val backButton: ImageButton = view.findViewById(R.id.btn_b2)
        val passwordToggle: ImageView = view.findViewById(R.id.password_toggle)

        var isPasswordVisible = false
        
        passwordToggle.setOnClickListener {
            isPasswordVisible = !isPasswordVisible
            
            // 비밀번호 표시/숨김 전환
            if (isPasswordVisible) {
                currentPasswordEditText.transformationMethod = HideReturnsTransformationMethod.getInstance()
                passwordToggle.setImageResource(R.drawable.password_show)
            } else {
                currentPasswordEditText.transformationMethod = PasswordTransformationMethod.getInstance()
                passwordToggle.setImageResource(R.drawable.password_hide)
            }
            // 커서를 텍스트 끝으로 이동
            currentPasswordEditText.setSelection(currentPasswordEditText.text.length)
        }

        // 다음 버튼 클릭 시 이벤트 처리
        nextButton.setOnClickListener {
            val currentPassword = currentPasswordEditText.text.toString()

            if (currentPassword.isEmpty()) {
                Toast.makeText(context, "비밀번호를 입력해 주세요.", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }

            val authPrefs = requireContext().getSharedPreferences("auth", Context.MODE_PRIVATE)
            val token = authPrefs.getString("token", null)

            // 현재 비밀번호 확인을 위해 같은 비밀번호를 전송하여 확인
            RetrofitClient.apiService.changePassword(
                "Bearer $token",
                ChangePasswordRequest(currentPassword, currentPassword)
            ).enqueue(object : Callback<PasswordVerificationResponse> {
                override fun onResponse(
                    call: Call<PasswordVerificationResponse>,
                    response: Response<PasswordVerificationResponse>
                ) {
                    if (response.isSuccessful && response.body()?.success == true) {
                        val fragment = ChangepasswordFragment().apply {
                            arguments = Bundle().apply {
                                putString("current_password", currentPassword)
                            }
                        }
                        parentFragmentManager.beginTransaction()
                            .setCustomAnimations(
                                R.anim.enter_from_right,
                                R.anim.exit_to_left
                            )
                            .replace(R.id.fragment_container, fragment)
                            .addToBackStack(null)
                            .commit()
                    } else {
                        Toast.makeText(context, "현재 비밀번호가 일치하지 않습니다.", Toast.LENGTH_SHORT).show()
                    }
                }

                override fun onFailure(call: Call<PasswordVerificationResponse>, t: Throwable) {
                    Toast.makeText(context, "비밀번호 확인에 실패했습니다.", Toast.LENGTH_SHORT).show()
                }
            })
        }

        backButton.setOnClickListener {
            // 뒤로가기 기능
            activity?.onBackPressedDispatcher?.onBackPressed()
        }

        return view
    }
}
