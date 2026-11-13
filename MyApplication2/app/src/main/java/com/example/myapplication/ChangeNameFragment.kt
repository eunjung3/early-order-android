package com.example.myapplication

import android.content.Context
import android.os.Bundle
import android.text.Editable
import android.text.TextWatcher
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.EditText
import android.widget.ImageButton
import android.widget.Toast
import androidx.fragment.app.Fragment
import com.google.android.material.bottomnavigation.BottomNavigationView
import com.google.android.material.button.MaterialButton
import retrofit2.Call
import retrofit2.Callback
import retrofit2.Response

class ChangeNameFragment : Fragment() {
    private lateinit var edName: EditText
    private lateinit var btnSave: MaterialButton
    private lateinit var btnBack: ImageButton
    private var currentName: String = ""

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        return inflater.inflate(R.layout.fragment_change_name, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        
        (activity as? HomeActivity)?.findViewById<BottomNavigationView>(R.id.bottom_navigation)?.visibility = View.GONE
        
        initViews(view)
        loadCurrentName()
        setupListeners()
    }

    private fun initViews(view: View) {
        edName = view.findViewById(R.id.ed_name)
        btnSave = view.findViewById(R.id.btn_save)
        btnBack = view.findViewById(R.id.btn_b2)
    }

    private fun loadCurrentName() {
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
                                currentName = userInfo.username
                                edName.setText(currentName)
                            }
                        }
                    }

                    override fun onFailure(call: Call<UserInfoResponse>, t: Throwable) {
                        Toast.makeText(
                            context,
                            "로그인 후 이용해주세요.",
                            Toast.LENGTH_SHORT
                        ).show()
                    }
                })
        }
    }

    private fun setupListeners() {
        edName.addTextChangedListener(object : TextWatcher {
            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}
            
            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {
                if (s?.length ?: 0 > 10) {
                    Toast.makeText(context, "이름은 최대 10자로 입력해주세요", Toast.LENGTH_SHORT).show()
                }
            }
            
            override fun afterTextChanged(s: Editable?) {}
        })

        btnSave.setOnClickListener {
            val newName = edName.text?.toString()?.trim() ?: ""
            
            when {
                newName.isEmpty() -> {
                    Toast.makeText(context, "이름을 입력해주세요", Toast.LENGTH_SHORT).show()
                }
                newName == currentName -> {
                    Toast.makeText(context, "현재 사용 중인 이름과 동일합니다", Toast.LENGTH_SHORT).show()
                }
                newName.length > 10 -> {
                    Toast.makeText(context, "이름은 최대 10자로 입력해주세요", Toast.LENGTH_SHORT).show()
                }
                else -> updateName(newName)
            }
        }

        btnBack.setOnClickListener {
            parentFragmentManager.popBackStack()
        }
    }

    private fun updateName(newName: String) {
        val authPrefs = requireContext().getSharedPreferences("auth", Context.MODE_PRIVATE)
        val token = authPrefs.getString("token", null)

        if (token != null) {
            RetrofitClient.apiService.updateUsername("Bearer $token", UpdateUsernameRequest(newName))
                .enqueue(object : Callback<UpdateUsernameResponse> {
                    override fun onResponse(
                        call: Call<UpdateUsernameResponse>,
                        response: Response<UpdateUsernameResponse>
                    ) {
                        if (response.isSuccessful && response.body()?.success == true) {
                            Toast.makeText(context, "이름이 변경되었습니다.", Toast.LENGTH_SHORT).show()
                            (parentFragment as? UserFragment)?.refreshUserInfo()
                            (activity?.supportFragmentManager?.fragments?.find { it is MyPageFragment } as? MyPageFragment)?.let {
                                it.checkLoginStatus()
                            }
                            parentFragmentManager.popBackStack()
                        } else {
                            Toast.makeText(context, response.body()?.message ?: "이름 변경에 실패했습니다.", Toast.LENGTH_SHORT).show()
                        }
                    }

                    override fun onFailure(call: Call<UpdateUsernameResponse>, t: Throwable) {
                        Toast.makeText(context, "이름 변경에 실패했습니다.", Toast.LENGTH_SHORT).show()
                    }
                })
        }
    }
}