package com.example.myapplication

import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.fragment.app.Fragment
import androidx.fragment.app.activityViewModels
import androidx.lifecycle.ViewModel
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.ViewModelProvider
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.example.myapplication.databinding.FragmentHomeBinding
import com.google.android.material.tabs.TabLayoutMediator
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import android.view.animation.Animation
import android.view.animation.AnimationUtils
import android.view.MotionEvent

class HomeFragment : Fragment() {
    private var _binding: FragmentHomeBinding? = null
    private val binding get() = _binding!!
    private lateinit var recommendedMenuAdapter: RecommendedMenuAdapter
    private lateinit var orderStatusManager: OrderStatusManager
    private lateinit var orderStatusOverlay: View
    private lateinit var layoutOrderMenu: RecyclerView
    private lateinit var tvOrderStatus: TextView
    private val orderViewModel: OrderViewModel by activityViewModels {
        object : ViewModelProvider.Factory {
            override fun <T : ViewModel> create(modelClass: Class<T>): T {
                return OrderViewModel(requireActivity().application) as T
            }
        }
    }
    private val cartViewModel: CartViewModel by activityViewModels()
    private lateinit var posterAdapter: PosterViewPagerAdapter

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentHomeBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        // View 초기화
        orderStatusOverlay = binding.includeOrderStatus.cardOrderStatus
        layoutOrderMenu = binding.includeOrderStatus.layoutOrderMenu
        tvOrderStatus = binding.includeOrderStatus.tvOrderStatus

        // OrderStatusManager 초기화
        orderStatusManager = OrderStatusManager(requireContext())

        // RecyclerView 초기화
        initRecyclerView()

        // 주문 상태 관찰
        observeOrderStatus()

        // 추천 메뉴 설정
        setupRecommendedMenus(view)

        // 메뉴 데이터 관찰
        observeMenuData()

        // 포스터 관련 코드 추가
        setupPosterViewPager()
        loadPosters()

        // 인디케이터 설정
        TabLayoutMediator(binding.indicator, binding.viewPagerNewMenu) { _, _ -> }.attach()

        // 주문 상태 카드에 스와이프 제스처 추가
        var startY = 0f
        orderStatusOverlay.setOnTouchListener(object : View.OnTouchListener {
            override fun onTouch(v: View, event: MotionEvent): Boolean {
                when (event.action) {
                    MotionEvent.ACTION_DOWN -> {
                        startY = event.y
                        return true
                    }
                    MotionEvent.ACTION_MOVE -> {
                        val deltaY = event.y - startY
                        if (deltaY > 0) {  // 아래로 스와이프는 무시
                            return false
                        } else {
                            v.translationY = deltaY
                            return true
                        }
                    }
                    MotionEvent.ACTION_UP -> {
                        val deltaY = event.y - startY
                        if (deltaY < -200) {  // 위로 일정 거리 이상 스와이프
                            orderViewModel.orderStatus.value?.let { status ->
                                if (status == "완료") {
                                    orderViewModel.clearCurrentOrder()
                                    orderStatusOverlay.hideWithAnimation()
                                } else {
                                    // 진행 중인 주문은 원위치
                                    v.animate().translationY(0f).setDuration(200).start()
                                }
                            }
                        } else {
                            // 충분히 스와이프하지 않은 경우 원위치
                            v.animate().translationY(0f).setDuration(200).start()
                        }
                        return true
                    }
                    else -> return false
                }
            }
        })

        // 포스터 자동 새로고침 시작
        startPosterRefresh()
    }

    override fun onResume() {
        super.onResume()
        (activity as? HomeActivity)?.showBottomNavigation()
    }

    private fun observeOrderStatus() {
        orderViewModel.currentOrder.observe(viewLifecycleOwner) { menuItems ->
            viewLifecycleOwner.lifecycleScope.launch(Dispatchers.Main.immediate) {
                try {
                    orderViewModel.orderStatus.value?.let { status ->
                        if (menuItems.isNotEmpty()) {
                            orderViewModel.userInfo.value?.let { userInfo ->
                                orderStatusManager.updateOrderStatus(
                                    orderStatusOverlay,
                                    layoutOrderMenu,
                                    tvOrderStatus,
                                    menuItems,
                                    userInfo.username,
                                    status
                                )

                                // 처음 표시될 때만 애니메이션 적용
                                if (orderStatusOverlay.visibility != View.VISIBLE) {
                                    orderStatusOverlay.showWithAnimation()
                                }
                            }
                        } else {
                            // 주문이 없을 때만 숨김
                            orderStatusOverlay.hideWithAnimation()
                        }
                    }
                } catch (e: Exception) {
                    Log.e("HomeFragment", "주문 상태 업데이트 실패", e)
                }
            }
        }

        orderViewModel.orderStatus.observe(viewLifecycleOwner) { status ->
            orderViewModel.currentOrder.value?.let { menuItems ->
                if (menuItems.isNotEmpty()) {
                    orderViewModel.userInfo.value?.let { userInfo ->
                        orderStatusManager.updateOrderStatus(
                            orderStatusOverlay,
                            layoutOrderMenu,
                            tvOrderStatus,
                            menuItems,
                            userInfo.username,
                            status ?: "준비중"
                        )
                    }
                }
            }
        }

        // 15분 타이머 관찰
        orderViewModel.shouldHideOrder.observe(viewLifecycleOwner) { shouldHide ->
            if (shouldHide) {
                orderStatusOverlay.hideWithAnimation()
            }
        }
    }

    private fun setupRecommendedMenus(view: View) {
        recommendedMenuAdapter = RecommendedMenuAdapter { menuItem ->
            MenuOptionDialog(
                context = requireContext(),
                menuItem = menuItem,
                onAddToCart = { updatedMenuItem ->
                    cartViewModel.addToCart(updatedMenuItem)
                },
                onPlaceOrder = { updatedMenuItem ->
                    parentFragmentManager.beginTransaction()
                        .replace(R.id.fragment_container, CheckoutFragment.newInstance(listOf(updatedMenuItem)))
                        .addToBackStack(null)
                        .commit()
                },
                onAddToFavorite = { favoriteMenuItem ->
                    false // 기본값 반환
                },
                onRemoveFromFavorite = { /* 빈 구현 */ },
                orderViewModel = orderViewModel
            ).show()
        }

        view.findViewById<RecyclerView>(R.id.rv_recommended_menu).adapter = recommendedMenuAdapter
    }

    private fun observeMenuData() {
        lifecycleScope.launch {
            orderViewModel.menuData.collect { menuList ->
                val nonDessertMenus = menuList.flatten()
                    .filter { it.category != "BAKERY/DESSERT" }
                    .shuffled()
                    .take(3)
                recommendedMenuAdapter.updateItems(nonDessertMenus)
            }
        }
    }

    private fun initRecyclerView() {
        layoutOrderMenu.apply {
            layoutManager = LinearLayoutManager(context)
            adapter = orderStatusManager.getOrderStatusAdapter()
            Log.d("HomeFragment", "RecyclerView adapter 설정 완료")
        }
    }

    private fun setupPosterViewPager() {
        posterAdapter = PosterViewPagerAdapter()
        binding.viewPagerNewMenu.adapter = posterAdapter
        binding.viewPagerNewMenu.apply {
            postponeEnterTransition()
            viewTreeObserver.addOnPreDrawListener {
                startPostponedEnterTransition()
                true
            }
        }
    }

    private fun startPosterRefresh() {
        lifecycleScope.launch {
            while (true) {
                try {
                    loadPosters() // 포스터 데이터 새로고침
                } catch (e: Exception) {
                    Log.e("HomeFragment", "포스터 데이터 새로고침 중 오류 발생", e)
                }
                delay(5000) // 5초마다 새로고침
            }
        }
    }

    private fun loadPosters() {
        if (!isAdded || _binding == null) return

        lifecycleScope.launch {
            try {
                // IO 스레드에서 포스터 데이터 가져오기
                val response = withContext(Dispatchers.IO) {
                    RetrofitClient.apiService.getPosters().execute()
                }

                // 메인 스레드에서 UI 업데이트
                withContext(Dispatchers.Main) {
                    if (!isAdded || _binding == null) return@withContext

                    if (response.isSuccessful && response.body()?.success == true) {
                        val posters = response.body()?.posters ?: emptyList()
                        if (posters.isNotEmpty()) {
                            posterAdapter.submitList(posters)
                            updatePosterVisibility(true)
                        } else {
                            updatePosterVisibility(false)
                        }
                    } else {
                        updatePosterVisibility(false)
                    }
                }
            } catch (e: Exception) {
                Log.e("HomeFragment", "포스터 로딩 실패", e)
                withContext(Dispatchers.Main) {
                    if (!isAdded || _binding == null) return@withContext
                    updatePosterVisibility(false)
                }
            }
        }
    }

    private fun updatePosterVisibility(show: Boolean) {
        binding.apply {
            viewPagerNewMenu.visibility = if (show) View.VISIBLE else View.GONE
            indicator.visibility = if (show) View.VISIBLE else View.GONE
        }
    }

    private fun View.showWithAnimation() {
        if (visibility == View.VISIBLE) return

        lifecycleScope.launch(Dispatchers.Main) {
            visibility = View.VISIBLE
            withContext(Dispatchers.Default) {
                val animation = AnimationUtils.loadAnimation(context, R.anim.slide_down_in)
                withContext(Dispatchers.Main) {
                    startAnimation(animation)
                }
            }
        }
    }

    private fun View.hideWithAnimation(onAnimationEnd: () -> Unit = {}) {
        if (visibility == View.GONE) return

        lifecycleScope.launch(Dispatchers.Main) {
            val animation = AnimationUtils.loadAnimation(context, R.anim.slide_up_out)
            animation.setAnimationListener(object : Animation.AnimationListener {
                override fun onAnimationStart(animation: Animation?) {}
                override fun onAnimationRepeat(animation: Animation?) {}
                override fun onAnimationEnd(animation: Animation?) {
                    visibility = View.GONE
                    onAnimationEnd()
                }
            })

            withContext(Dispatchers.Default) {
                withContext(Dispatchers.Main) {
                    startAnimation(animation)
                }
            }
        }
    }
}
