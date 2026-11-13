package com.example.myapplication

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import androidx.fragment.app.FragmentManager
import com.google.android.material.button.MaterialButton
import com.google.android.material.bottomnavigation.BottomNavigationView

class OkFragment : Fragment() {
    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        return inflater.inflate(R.layout.fragment_ok, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        view.findViewById<MaterialButton>(R.id.btn_go_home).setOnClickListener {
            val fragmentManager = requireActivity().supportFragmentManager
            
            // 백스택 모두 제거
            fragmentManager.popBackStack(null, FragmentManager.POP_BACK_STACK_INCLUSIVE)
            
            // HomeFragment로 교체하고 BottomNavigation 업데이트
            fragmentManager.beginTransaction()
                .replace(R.id.fragment_container, HomeFragment())
                .commit()
                
            // BottomNavigation 업데이트
            (activity as? HomeActivity)?.let { homeActivity ->
                homeActivity.findViewById<BottomNavigationView>(R.id.bottom_navigation)?.selectedItemId = R.id.nav_home
            }
        }
    }
} 