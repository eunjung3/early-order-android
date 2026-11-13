package com.example.myapplication

import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.os.Looper
import android.util.Log
import android.view.View
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import androidx.fragment.app.Fragment
import androidx.fragment.app.FragmentManager
import com.google.android.material.bottomnavigation.BottomNavigationView

class HomeActivity : AppCompatActivity() {
    private lateinit var bottomNavigationView: BottomNavigationView

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_home)

        val authPrefs = getSharedPreferences("auth", Context.MODE_PRIVATE)
        val isAutoLogin = authPrefs.getBoolean("auto_login", false)
        
        if (!isAutoLogin) {
            // 자동 로그인이 아닌 경우 임시 토큰을 정식 토큰으로 변환
            val tempToken = authPrefs.getString("temp_token", null)
            authPrefs.edit().apply {
                if (tempToken != null) {
                    putString("token", tempToken)
                    remove("temp_token")
                }
                apply()
            }
        }
        
        bottomNavigationView = findViewById(R.id.bottom_navigation)

        if (savedInstanceState == null) {
            loadFragment(HomeFragment())
        }

        bottomNavigationView.setOnItemSelectedListener { item ->
            when (item.itemId) {
                R.id.nav_home -> loadFragment(HomeFragment())
                R.id.nav_order -> loadFragment(OrderFragment())
                R.id.nav_mypage -> loadFragment(MyPageFragment())
                else -> false
            }.also { showBottomNavigation() }
        }

        // 마이페이지로 이동하는 인텐트 처리
        if (intent.getStringExtra("navigate_to") == "mypage") {
            supportFragmentManager.beginTransaction()
                .replace(R.id.fragment_container, MyPageFragment())
                .commit()
            bottomNavigationView.selectedItemId = R.id.nav_mypage
        }
    }

    private fun loadFragment(fragment: Fragment): Boolean {
        try {
            supportFragmentManager.beginTransaction()
                .setCustomAnimations(
                    R.anim.fade_in,
                    R.anim.fade_out,
                    R.anim.fade_in,
                    R.anim.fade_out
                )
                .replace(R.id.fragment_container, fragment)
                .commitAllowingStateLoss()
            return true
        } catch (e: Exception) {
            e.printStackTrace()
            return false
        }
    }

    fun showBottomNavigation() {
        // UI 업데이트는 메인 스레드에서 안전하게 처리
        if (Looper.myLooper() != Looper.getMainLooper()) {
            runOnUiThread { showBottomNavigation() }
            return
        }
        bottomNavigationView.visibility = View.VISIBLE
        bottomNavigationView.animate()
            .translationY(0f)
            .setDuration(300)
    }

    fun hideBottomNavigation() {
        // UI 업데이트는 메인 스레드에서 안전하게 처리
        if (Looper.myLooper() != Looper.getMainLooper()) {
            runOnUiThread { hideBottomNavigation() }
            return
        }
        bottomNavigationView.animate()
            .translationY(bottomNavigationView.height.toFloat())
            .setDuration(300)
            .withEndAction {
                bottomNavigationView.visibility = View.GONE
            }
    }

    fun refreshMyPageFragment() {
        Log.d("HomeActivity", "MyPageFragment 갱신 시도")
        supportFragmentManager.fragments.forEach { fragment ->
            if (fragment is MyPageFragment && fragment.isAdded) {
                Log.d("HomeActivity", "MyPageFragment 찾음, checkLoginStatus 호출")
                fragment.checkLoginStatus()
                return
            }
        }
        Log.d("HomeActivity", "활성화된 MyPageFragment를 찾을 수 없음")
    }
}
