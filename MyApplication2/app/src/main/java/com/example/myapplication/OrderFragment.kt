package com.example.myapplication

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageButton
import android.widget.TextView
import android.Manifest
import android.content.pm.PackageManager
import android.location.Location
import android.os.Build
import android.widget.HorizontalScrollView
import android.widget.ImageView
import android.widget.Toast
import androidx.core.view.ViewCompat
import androidx.core.view.accessibility.AccessibilityNodeInfoCompat
import androidx.core.view.AccessibilityDelegateCompat
import androidx.core.app.ActivityCompat
import androidx.fragment.app.Fragment
import androidx.fragment.app.activityViewModels
import androidx.fragment.app.viewModels
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.RecyclerView
import androidx.viewpager2.adapter.FragmentStateAdapter
import androidx.viewpager2.widget.ViewPager2

import com.google.android.material.tabs.TabLayout
import com.google.android.material.tabs.TabLayoutMediator
import com.google.android.gms.location.FusedLocationProviderClient
import com.google.android.gms.location.LocationServices
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import android.graphics.Rect
import android.content.Context
import kotlinx.coroutines.delay
import android.util.Log

class OrderFragment : Fragment() {

    private val viewModel: OrderViewModel by viewModels()
    private lateinit var viewPager: ViewPager2
    private lateinit var tabLayout: TabLayout
    private lateinit var cartIcon: ImageButton
    private lateinit var cartItemCount: TextView
    private val cartViewModel: CartViewModel by viewModels({ requireActivity() })
    private lateinit var fusedLocationClient: FusedLocationProviderClient
    private val LOCATION_REQUEST_CODE = 1000
    private var fragmentView: View? = null

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        val view = inflater.inflate(R.layout.fragment_order, container, false)
        
        initializeViews(view)
        setupViewPager()
        setupClickListeners()
        observeCartCount()
        
        return view
    }

    private fun initializeViews(view: View) {
        viewPager = view.findViewById(R.id.viewPager)
        tabLayout = view.findViewById(R.id.tabLayout_categories)
        cartIcon = view.findViewById(R.id.btn_cart)
        cartItemCount = view.findViewById(R.id.tv_cart_count)
        
        setupClickListeners()
    }

    private fun setupViewPager() {
        val pagerAdapter = MenuPagerAdapter(this)
        viewPager.adapter = pagerAdapter
        
        TabLayoutMediator(tabLayout, viewPager) { tab, position ->
            tab.text = viewModel.categoryTitles[position]
        }.attach()

        viewPager.setCurrentItem(0, false)
        
        lifecycleScope.launch(Dispatchers.Main) {
            viewModel.menuData.collect { categories ->
                if (categories.isNotEmpty()) {
                    val currentPosition = viewPager.currentItem
                    val currentFragment = childFragmentManager.findFragmentByTag("f$currentPosition")
                    if (currentFragment is MenuListFragment) {
                        withContext(Dispatchers.Main) {
                            currentFragment.updateMenuItems(categories[currentPosition])
                        }
                    }
                }
            }
        }

        viewPager.registerOnPageChangeCallback(object : ViewPager2.OnPageChangeCallback() {
            override fun onPageSelected(position: Int) {
                super.onPageSelected(position)
                lifecycleScope.launch(Dispatchers.Main) {
                    val currentFragment = childFragmentManager.findFragmentByTag("f$position") as? MenuListFragment
                }
            }
        })
    }

    inner class MenuPagerAdapter(fragment: Fragment) : FragmentStateAdapter(fragment) {
        override fun getItemCount(): Int = viewModel.categoryTitles.size

        override fun createFragment(position: Int): Fragment {
            val menuItems = viewModel.getMenuCategory(position)
            return MenuListFragment.newInstance(menuItems)
        }
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        setupScrollIndicators()
        (activity as? HomeActivity)?.showBottomNavigation()
        
        // 주기적으로 메뉴 데이터 새로고침
        startMenuRefresh()
        
        // 장바구니 데이터 로드
        val authPrefs = requireContext().getSharedPreferences("auth", Context.MODE_PRIVATE)
        val token = authPrefs.getString("token", null)
        
        if (token != null) {
            cartViewModel.loadUserCart(token)
        }
    }

    override fun onResume() {
        super.onResume()
        (activity as? HomeActivity)?.showBottomNavigation()
        
        // 로그인 상태 확인
        val authPrefs = requireContext().getSharedPreferences("auth", Context.MODE_PRIVATE)
        val token = authPrefs.getString("token", null)
        
        if (token == null) {
            // 로그아웃 상태면 카트 초기화
            cartViewModel.clearCart()
        }
    }

    fun addToCart(menuItem: MenuItem) {
        cartViewModel.addToCart(menuItem)
    }

    fun placeOrder(menuItem: MenuItem) {
        // 주문 처리 로직 추가 (예: 서버에 주문 요청 전송)
    }

    fun moveToCheckout(items: List<MenuItem>) {
        // CheckoutFragment로 이동하면서 선택한 메뉴들 전달
        val checkoutFragment = CheckoutFragment.newInstance(items)
        parentFragmentManager.beginTransaction()
            .replace(R.id.fragment_container, checkoutFragment)
            .addToBackStack(null)
            .commit()
    }

    private fun observeCartCount() {
        cartViewModel.cartCount.observe(viewLifecycleOwner) { count ->
            cartItemCount.text = count.toString()
            cartItemCount.visibility = if (count > 0) View.VISIBLE else View.GONE
        }
    }

    private fun setupClickListeners() {
        cartIcon.setOnClickListener {
            val cartFragment = CartFragment()
            parentFragmentManager.beginTransaction()
                .replace(R.id.fragment_container, cartFragment)
                .addToBackStack("order")
                .commit()
        }
    }

    private fun setupScrollIndicators() {
        val horizontalScrollView = view?.findViewById<HorizontalScrollView>(R.id.horizontalScrollView)
        val leftArrow = view?.findViewById<ImageView>(R.id.left_arrow)
        val rightArrow = view?.findViewById<ImageView>(R.id.right_arrow)

        horizontalScrollView?.viewTreeObserver?.addOnScrollChangedListener {
            val scrollX = horizontalScrollView.scrollX
            val maxScrollX = horizontalScrollView.getChildAt(0).width - horizontalScrollView.width

            leftArrow?.visibility = if (scrollX > 0) View.VISIBLE else View.GONE
            rightArrow?.visibility = if (scrollX < maxScrollX) View.VISIBLE else View.GONE
        }
    }

    private fun startMenuRefresh() {
        lifecycleScope.launch {
            while (true) {
                try {
                    viewModel.loadMenuData() // 메뉴 데이터 새로고침
                } catch (e: Exception) {
                    Log.e("OrderFragment", "메뉴 데이터 새로고침 중 오류 발생", e)
                }
                delay(5000) // 5초마다 새로고침
            }
        }
    }

    // ViewPager의 현재 페이지 Fragment 업데이트
    private fun updateCurrentFragment() {
        val currentPosition = viewPager.currentItem
        val currentFragment = childFragmentManager.findFragmentByTag("f$currentPosition") as? MenuListFragment
        currentFragment?.let {
            val menuItems = viewModel.getMenuCategory(currentPosition)
            it.updateMenuItems(menuItems)
        }
    }
}
