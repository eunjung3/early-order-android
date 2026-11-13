package com.example.myapplication

import android.content.Context
import androidx.fragment.app.Fragment
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import retrofit2.Call
import retrofit2.Callback
import retrofit2.Response
import android.text.SpannableString
import android.text.style.UnderlineSpan
import android.text.Spanned
import android.graphics.Color
import android.text.style.ForegroundColorSpan

class UserViewModel : ViewModel() {
    private val _userInfo = MutableLiveData<UserInfo>()
    val userInfo: LiveData<UserInfo> = _userInfo

    private val _viewVisibility = MutableLiveData<Boolean>()
    val viewVisibility: LiveData<Boolean> = _viewVisibility

    private val _bottomNavVisibility = MutableLiveData<Boolean>()
    val bottomNavVisibility: LiveData<Boolean> = _bottomNavVisibility

    private val _navigationEvent = MutableLiveData<Fragment>()
    val navigationEvent: LiveData<Fragment> = _navigationEvent

    private val _error = MutableLiveData<String>()
    val error: LiveData<String> = _error

    private val _logoutEvent = MutableLiveData<Boolean>()
    val logoutEvent: LiveData<Boolean> = _logoutEvent

    private val _logoutButtonText = MutableLiveData<SpannableString>()
    val logoutButtonText: LiveData<SpannableString> = _logoutButtonText

    init {
        val spannableString = SpannableString("로그아웃")
        spannableString.setSpan(
            UnderlineSpan(),
            0,
            spannableString.length,
            Spanned.SPAN_EXCLUSIVE_EXCLUSIVE
        )
        spannableString.setSpan(
            ForegroundColorSpan(Color.parseColor("#808080")),
            0,
            spannableString.length,
            Spanned.SPAN_EXCLUSIVE_EXCLUSIVE
        )
        _logoutButtonText.value = spannableString
    }

    fun showBottomNav() {
        _bottomNavVisibility.value = true
    }

    fun hideBottomNav() {
        _bottomNavVisibility.value = false
    }

    fun loadUserInfo(context: Context) {
        val authPrefs = context.getSharedPreferences("auth", Context.MODE_PRIVATE)
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
                                _userInfo.value = userInfo
                            }
                        }
                    }
                    
                    override fun onFailure(call: Call<UserInfoResponse>, t: Throwable) {
                        _error.value = "로그인 후 이용해주세요."
                    }
                })
        }
    }

    fun logout(context: Context) {
        val authPrefs = context.getSharedPreferences("auth", Context.MODE_PRIVATE)
        authPrefs.edit().apply {
            clear()  // 모든 데이터 삭제
            putBoolean("auto_login", false)  // 자동 로그인 명시적으로 비활성화
            remove("saved_email")  // 저장된 이메일 삭제
            remove("saved_password")  // 저장된 비밀번호 삭제
            remove("token")  // 토큰 삭제
            apply()
        }
        
        _logoutEvent.value = true
    }
}
    