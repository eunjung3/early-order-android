package com.example.myapplication

import android.content.Context
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageButton
import android.widget.Toast
import androidx.fragment.app.Fragment
import androidx.fragment.app.activityViewModels
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import retrofit2.Call
import retrofit2.Callback
import retrofit2.Response

class FavoriteMenuFragment : Fragment() {

    private lateinit var adapter: FavoriteMenuAdapter
    private val favoriteItems = mutableListOf<MenuItem>()
    private val pendingRemovals = mutableSetOf<MenuItem>()
    private val job = Job()
    private val coroutineScope = CoroutineScope(Dispatchers.Main + job)

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        return inflater.inflate(R.layout.fragment_menu_favorite, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        
        // 하단바 숨기기
        (activity as? HomeActivity)?.hideBottomNavigation()
        setupRecyclerView()
        loadFavorites()
        
        view.findViewById<ImageButton>(R.id.btn_back).setOnClickListener {
            // 뒤로가기 전에 하단바 다시 표시
            (activity as? HomeActivity)?.showBottomNavigation()
            parentFragmentManager.popBackStack()
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        // Fragment가 제거될 때 하단바 다시 표시
        (activity as? HomeActivity)?.showBottomNavigation()
    }

    override fun onResume() {
        super.onResume()
        coroutineScope.launch(Dispatchers.Main) {
            // pendingRemovals에 있는 항목들과 일치하는 항목들을 favoriteItems에서 제거
            favoriteItems.removeAll { menuItem ->
                pendingRemovals.any { pendingItem -> 
                    pendingItem.hasSameOptionsAs(menuItem)
                }
            }
            updateEmptyState()
            adapter.updateItems(favoriteItems)
            
            // 서버에서 최신 데이터 로드
            loadFavorites()
        }
    }

    override fun onPause() {
        super.onPause()
        coroutineScope.launch(Dispatchers.IO) {
            processPendingRemovals()
            // 서버 동기화가 완료될 때까지 약간의 지연 추가
            withContext(Dispatchers.IO) {
                kotlinx.coroutines.delay(500) // 500ms 대기
            }
        }
    }

    private suspend fun processPendingRemovals() {
        val token = requireContext().getSharedPreferences("auth", Context.MODE_PRIVATE)
            .getString("token", null) ?: return

        val failedRemovals = mutableSetOf<MenuItem>()
        
        pendingRemovals.forEach { menuItem ->
            try {
                val request = FavoriteUpdateRequest(
                    menuName = menuItem.name,
                    isFavorite = false,
                    hotIceOption = menuItem.hotIceOption,
                    size = menuItem.size,
                    shotQuantity = menuItem.shotQuantity,
                    hazelnutQuantity = menuItem.hazelnutQuantity,
                    tapiocaQuantity = menuItem.tapiocaQuantity,
                    decafQuantity = menuItem.decafQuantity,
                    vanillaQuantity = menuItem.vanillaQuantity,
                    whippingQuantity = menuItem.whippingQuantity,
                    iceCreamQuantity = menuItem.iceCreamQuantity,
                    totalPrice = menuItem.calculateTotalPrice(),
                    price = menuItem.price,
                    imageUrl = menuItem.imageUrl
                )
                val response = withContext(Dispatchers.IO) {
                    RetrofitClient.apiService.updateFavoriteStatus("Bearer $token", request)
                        .execute()
                }
                
                if (!response.isSuccessful) {
                    failedRemovals.add(menuItem)
                }
            } catch (e: Exception) {
                failedRemovals.add(menuItem)
                withContext(Dispatchers.Main) {
                    Toast.makeText(context, "즐겨찾기 삭제 중 오류가 발생했습니다.", Toast.LENGTH_SHORT).show()
                }
            }
        }
        
        pendingRemovals.clear()
        pendingRemovals.addAll(failedRemovals)
        
        // 모든 삭제가 성공적으로 완료된 경우에만 서버에서 새로운 데이터를 가져옴
        if (failedRemovals.isEmpty()) {
            withContext(Dispatchers.Main) {
                loadFavorites()
            }
        }
    }

    private fun setupRecyclerView() {
        adapter = FavoriteMenuAdapter(
            items = favoriteItems,
            onAddToCart = { menuItem -> 
                coroutineScope.launch(Dispatchers.Main) {
                    val cartViewModel: CartViewModel by activityViewModels()
                    cartViewModel.addToCart(menuItem.copy(
                        quantity = 1,
                        isSelected = true
                    ))
                    Toast.makeText(context, "장바구니에 추가되었습니다.", Toast.LENGTH_SHORT).show()
                }
            },
            onPlaceOrder = { menuItem ->
                coroutineScope.launch(Dispatchers.Main) {
                    val checkoutItem = menuItem.copy(
                        quantity = 1,
                        isSelected = true
                    )
                    val checkoutItems = listOf(checkoutItem)
                    
                    parentFragmentManager.beginTransaction()
                        .replace(R.id.fragment_container, CheckoutFragment.newInstance(checkoutItems))
                        .addToBackStack(null)
                        .commit()
                }
            },
            onRemoveFromFavorite = { menuItem ->
                coroutineScope.launch(Dispatchers.Main) {
                    menuItem.isFavorite = false
                    pendingRemovals.add(menuItem)
                    adapter.updateItems(favoriteItems)
                    Toast.makeText(context, "즐겨찾기가 해제되었습니다.", Toast.LENGTH_SHORT).show()
                }
            },
            onAddToFavorite = { menuItem ->
                coroutineScope.launch(Dispatchers.Main) {
                    pendingRemovals.remove(menuItem)
                    menuItem.isFavorite = true
                    adapter.updateItems(favoriteItems)
                    Toast.makeText(context, "즐겨찾기가 추가되었습니다.", Toast.LENGTH_SHORT).show()
                }
            }
        )
        
        view?.findViewById<RecyclerView>(R.id.rv_favorite_menu)?.apply {
            layoutManager = LinearLayoutManager(context)
            adapter = this@FavoriteMenuFragment.adapter
        }
    }

    private fun updateEmptyState() {
        view?.let { view ->
            val emptyLayout = view.findViewById<View>(R.id.layout_empty_favorite)
            val recyclerView = view.findViewById<RecyclerView>(R.id.rv_favorite_menu)
            
            if (favoriteItems.isEmpty()) {
                emptyLayout.visibility = View.VISIBLE
                recyclerView.visibility = View.GONE
            } else {
                emptyLayout.visibility = View.GONE
                recyclerView.visibility = View.VISIBLE
            }
        }
    }

    fun addToFavorite(menuItem: MenuItem): Boolean {
        // 완전히 동일한 메뉴와 옵션이 있는지 확인
        val isDuplicate = favoriteItems.any { item ->
            item.hasSameOptionsAs(menuItem)
        }
        
        val result = if (!isDuplicate) {
            // 중복되지 않은 경우에만 새로운 항목 추가
            favoriteItems.add(menuItem.copy(isFavorite = true))
            true
        } else {
            false
        }

        if (this::adapter.isInitialized) {
            updateEmptyState()
            adapter.updateItems(favoriteItems)
        }
        return result
    }

    fun removeFromFavorite(menuItem: MenuItem) {
        // 이름과 옵션이 모두 일치하는 모든 항목 제거
        favoriteItems.removeAll { item ->
            if (item.hasSameOptionsAs(menuItem)) {
                item.isFavorite = false
                pendingRemovals.add(item)  // 서버 동기화를 위해 추가
                true
            } else {
                false
            }
        }
        
        if (this::adapter.isInitialized) {
            updateEmptyState()
            adapter.updateItems(favoriteItems)
        }
    }

    private fun loadFavorites() {
        val token = requireContext().getSharedPreferences("auth", Context.MODE_PRIVATE)
            .getString("token", null) ?: return

        coroutineScope.launch(Dispatchers.IO) {
            try {
                val response = RetrofitClient.apiService.getFavorites("Bearer $token").execute()
                withContext(Dispatchers.Main) {
                    if (response.isSuccessful && response.body()?.success == true) {
                        val favorites = response.body()?.data?.map { favoriteItem ->
                            MenuItem(
                                name = favoriteItem.menuName,
                                price = favoriteItem.price,
                                imageUrl = favoriteItem.imageUrl,
                                hotIceOption = favoriteItem.hotIceOption ?: "",
                                size = favoriteItem.size ?: "",
                                shotQuantity = favoriteItem.shotQuantity ?: 0,
                                hazelnutQuantity = favoriteItem.hazelnutQuantity ?: 0,
                                tapiocaQuantity = favoriteItem.tapiocaQuantity ?: 0,
                                decafQuantity = favoriteItem.decafQuantity ?: 0,
                                vanillaQuantity = favoriteItem.vanillaQuantity ?: 0,
                                whippingQuantity = favoriteItem.whippingQuantity ?: 0,
                                iceCreamQuantity = favoriteItem.iceCreamQuantity ?: 0,
                                totalPrice = favoriteItem.totalPrice ?: favoriteItem.price,
                                isFavorite = true,
                                isSoldOut = false
                            )
                        }?.filter { menuItem ->  // 서버에서 받은 데이터 중 pendingRemovals에 없는 항목만 필터링
                            !pendingRemovals.any { it.hasSameOptionsAs(menuItem) }
                        } ?: emptyList()
                        
                        favoriteItems.clear()
                        favoriteItems.addAll(favorites)
                        adapter.updateItems(favoriteItems)
                        updateEmptyState()
                    }
                }
            } catch (e: Exception) {
                withContext(Dispatchers.Main) {
                    Toast.makeText(context, "즐겨찾기 목록을 불러오는데 실패했습니다.", Toast.LENGTH_SHORT).show()
                }
            }
        }
    }

    override fun onStart() {
        super.onStart()
        val sharedPreferences = requireContext().getSharedPreferences("auth", Context.MODE_PRIVATE)
        val token = sharedPreferences.getString("token", null)
        
        if (token == null) {
            favoriteItems.clear()
            updateEmptyState()
            adapter.updateItems(favoriteItems)
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        job.cancel()
    }
}