package com.example.myapplication

import android.annotation.SuppressLint
import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.LinearLayout
import android.widget.TextView
import android.widget.Toast
import androidx.fragment.app.Fragment
import com.google.android.material.bottomnavigation.BottomNavigationView
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import retrofit2.Call
import retrofit2.Callback
import retrofit2.Response

class MyPageFragment : Fragment() {
    private lateinit var layoutBeforeLogin: LinearLayout
    private lateinit var layoutAfterLogin: LinearLayout
    private lateinit var tvUserName: TextView
    private lateinit var tvUserEmail: TextView
    private lateinit var btnMyInfo: Button
    private lateinit var btnCart: Button
    private lateinit var btnOrderHistory: Button
    private lateinit var btnFavorites: Button
    
    private var isInitialCheck = true
    private var isDataLoaded = false
    private var cachedUserInfo: UserInfo? = null

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        val view = inflater.inflate(R.layout.fragment_mypage, container, false)
        
        initializeViews(view)
        setupListeners(view)
        
        // 초기 상태 설정
        if (isInitialCheck) {
            layoutBeforeLogin.visibility = View.INVISIBLE
            layoutAfterLogin.visibility = View.INVISIBLE
            checkLoginStatus()
            isInitialCheck = false
        } else {
            val authPrefs = requireContext().getSharedPreferences("auth", Context.MODE_PRIVATE)
            if (authPrefs.getString("token", null) != null) {
                if (cachedUserInfo != null) {
                    // 캐시된 데이터가 있으면 바로 표시
                    showUserInfo(cachedUserInfo!!)
                } else {
                    // 캐시된 데이터가 없으면 로딩
                    layoutAfterLogin.visibility = View.INVISIBLE
                    checkLoginStatus()
                }
            } else {
                showLoggedOutView()
            }
        }
        
        return view
    }

    private fun initializeViews(view: View) {
        layoutBeforeLogin = view.findViewById(R.id.layout_before_login)
        layoutAfterLogin = view.findViewById(R.id.layout_after_login)
        tvUserName = view.findViewById(R.id.tv_user_name)
        tvUserEmail = view.findViewById(R.id.tv_user_email)
        btnMyInfo = view.findViewById(R.id.btn_my_info)
        btnCart = view.findViewById(R.id.btn_cart)
        btnOrderHistory = view.findViewById(R.id.btn_order_history)
        btnFavorites = view.findViewById(R.id.btn_favorites)
    }

    private fun showLoggedInView(userInfo: UserInfo) {
        layoutBeforeLogin?.visibility = View.GONE
        layoutAfterLogin?.visibility = View.VISIBLE
        tvUserName?.text = userInfo.username
        tvUserEmail?.text = userInfo.email
        
        // 버튼 활성화
        btnMyInfo?.isEnabled = true
        btnCart?.isEnabled = true
        btnOrderHistory?.isEnabled = true
        btnFavorites?.isEnabled = true
    }

    private fun showLoggedOutView() {
        layoutBeforeLogin?.visibility = View.VISIBLE
        layoutAfterLogin?.visibility = View.GONE
        
        // 버튼 비활성화
        btnMyInfo?.isEnabled = false
        btnCart?.isEnabled = false
        btnOrderHistory?.isEnabled = false
        btnFavorites?.isEnabled = false
    }

    private fun setupListeners(view: View) {
        // 로그인 버튼
        view.findViewById<Button>(R.id.login_button)?.setOnClickListener {
            val intent = Intent(requireContext(), LoginActivity::class.java)
            startActivity(intent)
        }

        // 회원가입 버튼
        view.findViewById<Button>(R.id.register_button)?.setOnClickListener {
            val intent = Intent(requireContext(), SignUpActivity::class.java)
            startActivity(intent)
        }

        // 내정보 버튼
        btnMyInfo.setOnClickListener {
            parentFragmentManager.beginTransaction()
                .replace(R.id.fragment_container, UserFragment())
                .addToBackStack(null)
                .commit()
        }

        // 장바구니 버튼
        btnCart.setOnClickListener {
            parentFragmentManager.beginTransaction()
                .replace(R.id.fragment_container, CartFragment())
                .addToBackStack(null)
                .commit()
        }

        // 주문내역 버튼
        btnOrderHistory.setOnClickListener {
            parentFragmentManager.beginTransaction()
                .replace(R.id.fragment_container, OrderHistoryFragment())
                .addToBackStack(null)
                .commit()
        }

        // 즐겨찾기 버튼
        btnFavorites.setOnClickListener {
            parentFragmentManager.beginTransaction()
                .replace(R.id.fragment_container, FavoriteMenuFragment())
                .addToBackStack(null)
                .commit()
        }
    }

    override fun onResume() {
        super.onResume()
        // 하단 네비게이션 바 보이게 하기
        (activity as? HomeActivity)?.showBottomNavigation()
        
        // 캐시된 데이터가 없을 때만 새로 로드
        if (cachedUserInfo == null || !isDataLoaded) {
            checkLoginStatus()
        }
    }

    fun checkLoginStatus() {
        val authPrefs = requireContext().getSharedPreferences("auth", Context.MODE_PRIVATE)
        val token = authPrefs.getString("token", null)

        if (token != null) {
            RetrofitClient.apiService.getUserInfo("Bearer $token")
                .enqueue(object : Callback<UserInfoResponse> {
                    override fun onResponse(call: Call<UserInfoResponse>, response: Response<UserInfoResponse>) {
                        if (!isAdded) return

                        if (response.isSuccessful && response.body()?.success == true) {
                            response.body()?.data?.let { userInfo ->
                                // 데이터 캐시
                                cachedUserInfo = userInfo
                                
                                view?.post {
                                    if (!isAdded) return@post
                                    showUserInfo(userInfo)
                                    isDataLoaded = true
                                }
                            }
                        } else {
                            handleLoginFailure()
                        }
                    }
                    override fun onFailure(call: Call<UserInfoResponse>, t: Throwable) {
                        if (!isAdded) return
                        handleLoginFailure()
                    }
                })
        } else {
            showLoggedOutView()
        }
    }

    private fun handleLoginFailure() {
        if (isAdded && view != null) {
            view?.post {
                showLoggedOutView()
                clearLoginData()
                isDataLoaded = false
            }
        }
    }

    private fun clearLoginData() {
        val authPrefs = requireContext().getSharedPreferences("auth", Context.MODE_PRIVATE)
        authPrefs.edit().remove("token").apply()
    }

    private fun logout() {
        val authPrefs = requireContext().getSharedPreferences("auth", Context.MODE_PRIVATE)
        authPrefs.edit().clear().apply()
        showLoggedOutView()
    }

    private fun navigateToUserFragment() {
        parentFragmentManager.beginTransaction()
            .replace(R.id.fragment_container, UserFragment())
            .addToBackStack(null)
            .commit()
    }

    override fun onPause() {
        super.onPause()
        // 필요한 경우 여기서 API 호출을 취소할 수 있습니다
    }

    private fun enableButtons() {
        btnMyInfo.isEnabled = true
        btnCart.isEnabled = true
        btnOrderHistory.isEnabled = true
        btnFavorites.isEnabled = true
    }

    private fun disableButtons() {
        btnMyInfo.isEnabled = false
        btnCart.isEnabled = false
        btnOrderHistory.isEnabled = false
        btnFavorites.isEnabled = false
    }

    override fun onDestroyView() {
        super.onDestroyView()
        // Fragment가 완전히 제거될 때만 캐시와 상태 초기화
        if (requireActivity().isFinishing) {
            isDataLoaded = false
            cachedUserInfo = null
        }
    }

    private fun showUserInfo(userInfo: UserInfo) {
        // 데이터를 먼저 설정하고 한번에 표시
        layoutAfterLogin.visibility = View.INVISIBLE
        tvUserEmail.text = userInfo.email
        tvUserName.text = userInfo.username
        
        layoutBeforeLogin.visibility = View.GONE
        layoutAfterLogin.visibility = View.VISIBLE
        enableButtons()
    }
}
