package com.example.myapplication

import android.content.Context
import android.content.Intent
import android.graphics.Color
import android.os.Bundle
import android.text.SpannableString
import android.text.Spanned
import android.text.style.ForegroundColorSpan
import android.text.style.UnderlineSpan
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.ImageButton
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AlertDialog
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import com.google.android.material.bottomnavigation.BottomNavigationView
import retrofit2.Call
import retrofit2.Callback
import retrofit2.Response

class UserFragment : Fragment() {

    private val viewModel: UserViewModel by viewModels()
    
    // 사용자 정보 표시할 변수들
    private lateinit var tvName: TextView
    private lateinit var tvEmail: TextView
    private lateinit var btnUpdateInfo: Button
    private lateinit var btnChangePassword: ImageButton
    private lateinit var backButton: ImageButton
    private lateinit var btnChangeName: ImageButton

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        return inflater.inflate(R.layout.fragment_user, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        try {
            initViews(view)
            setupObservers()
            setupListeners()
            
            viewModel.hideBottomNav()
            viewModel.loadUserInfo(requireContext())
        } catch (e: Exception) {
            Log.e("UserFragment", "Error in onViewCreated", e)
            Toast.makeText(context, "화면을 불러오는데 실패했습니다.", Toast.LENGTH_SHORT).show()
            parentFragmentManager.popBackStack()
        }
    }

    private fun initViews(view: View) {
        tvName = view.findViewById(R.id.tv_name) 
            ?: throw IllegalStateException("tv_name not found in layout")
        tvEmail = view.findViewById(R.id.tv_email) 
            ?: throw IllegalStateException("tv_email not found in layout")
        btnUpdateInfo = view.findViewById(R.id.btn_update_info) 
            ?: throw IllegalStateException("btn_update_info not found in layout")
        btnChangePassword = view.findViewById(R.id.pw_btn) 
            ?: throw IllegalStateException("pw_btn not found in layout")
        backButton = view.findViewById(R.id.btn_b2) 
            ?: throw IllegalStateException("btn_b2 not found in layout")
        btnChangeName = view.findViewById(R.id.name_btn)
            ?: throw IllegalStateException("name_btn not found in layout")

        viewModel.logoutButtonText.observe(viewLifecycleOwner) { spannableText ->
            btnUpdateInfo.text = spannableText
        }
    }

    private fun setupObservers() {
        viewModel.userInfo.observe(viewLifecycleOwner) { userInfo ->
            tvName.visibility = View.INVISIBLE
            tvEmail.visibility = View.INVISIBLE
            
            tvName.text = userInfo.username
            tvEmail.text = userInfo.email
            
            tvName.visibility = View.VISIBLE
            tvEmail.visibility = View.VISIBLE
        }

        viewModel.viewVisibility.observe(viewLifecycleOwner) { isVisible ->
            tvName.visibility = if (isVisible) View.VISIBLE else View.INVISIBLE
            tvEmail.visibility = if (isVisible) View.VISIBLE else View.INVISIBLE
        }

        viewModel.bottomNavVisibility.observe(viewLifecycleOwner) { isVisible ->
            (activity as? HomeActivity)?.findViewById<BottomNavigationView>(R.id.bottom_navigation)
                ?.visibility = if (isVisible) View.VISIBLE else View.GONE
        }

        viewModel.navigationEvent.observe(viewLifecycleOwner) { fragment ->
            fragment?.let {
                parentFragmentManager.beginTransaction()
                    .replace(R.id.fragment_container, it)
                    .addToBackStack(null)
                    .commit()
            }
        }

        viewModel.error.observe(viewLifecycleOwner) { message ->
            Toast.makeText(context, message, Toast.LENGTH_SHORT).show()
        }

        viewModel.logoutEvent.observe(viewLifecycleOwner) { shouldLogout ->
            if (shouldLogout) {
                viewModel.showBottomNav()
                parentFragmentManager.popBackStack()
                (parentFragment as? MyPageFragment)?.checkLoginStatus()
            }
        }
    }

    private fun setupListeners() {
        btnChangeName.setOnClickListener {
            hideBottomNav()
            navigateToFragment(ChangeNameFragment())
        }

        btnChangePassword.setOnClickListener {
            hideBottomNav()
            navigateToFragment(ChangepwFragment())
        }

        backButton.setOnClickListener {
            showBottomNav()
            navigateBack()
        }

        btnUpdateInfo.setOnClickListener {
            showLogoutDialog()
        }
    }

    private fun hideBottomNav() {
        (activity as? HomeActivity)?.findViewById<BottomNavigationView>(R.id.bottom_navigation)
            ?.visibility = View.GONE
    }

    private fun showBottomNav() {
        (activity as? HomeActivity)?.findViewById<BottomNavigationView>(R.id.bottom_navigation)
            ?.visibility = View.VISIBLE
    }

    private fun navigateToFragment(fragment: Fragment) {
        parentFragmentManager.beginTransaction()
            .replace(R.id.fragment_container, fragment)
            .addToBackStack(null)
            .commit()
    }

    private fun navigateBack() {
        try {
            Log.d("UserFragment", "시작: navigateBack()")
            
            // Activity 참조를 미리 저장
            val mainActivity = activity as? HomeActivity
            
            Log.d("UserFragment", "애니메이션 및 popBackStack 실행")
            requireActivity().supportFragmentManager.beginTransaction()
                .setCustomAnimations(
                    R.anim.enter_from_left,
                    R.anim.exit_to_right
                )
                .commit()
            requireActivity().supportFragmentManager.popBackStack()
            
            // HomeActivity의 메서드를 통해 MyPageFragment 갱신
            android.os.Handler(android.os.Looper.getMainLooper()).postDelayed({
                try {
                    Log.d("UserFragment", "Handler 실행됨")
                    mainActivity?.let { activity ->
                        Log.d("UserFragment", "HomeActivity 찾음, MyPageFragment 갱신 시도")
                        // HomeActivity에 새로운 메서드 추가 필요
                        activity.refreshMyPageFragment()
                    } ?: Log.d("UserFragment", "HomeActivity를 찾을 수 없음")
                } catch (e: Exception) {
                    Log.e("UserFragment", "MyPageFragment 갱신 중 오류 발생", e)
                }
            }, 200)
            
            Log.d("UserFragment", "완료: navigateBack()")
        } catch (e: Exception) {
            Log.e("UserFragment", "navigateBack 중 오류 발생", e)
            requireActivity().supportFragmentManager.popBackStack()
        }
    }

    private fun showLogoutDialog() {
        val dialogView = LayoutInflater.from(requireContext()).inflate(R.layout.dialog_logout, null)
        val dialog = AlertDialog.Builder(requireContext(), R.style.CustomAlertDialog)
            .setView(dialogView)
            .create()

        dialogView.findViewById<Button>(R.id.btn_confirm).setOnClickListener {
            viewModel.logout(requireContext())
            dialog.dismiss()
        }

        dialogView.findViewById<Button>(R.id.btn_cancel).setOnClickListener {
            dialog.dismiss()
        }

        dialog.show()
    }

    fun refreshUserInfo() {
        viewModel.loadUserInfo(requireContext())
    }
}
