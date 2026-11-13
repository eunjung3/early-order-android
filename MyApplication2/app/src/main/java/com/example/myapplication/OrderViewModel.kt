package com.example.myapplication

import android.content.Context
import android.util.Log
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import android.app.Application
import androidx.lifecycle.AndroidViewModel
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.isActive
import kotlinx.coroutines.supervisorScope

class OrderViewModel(application: Application) : AndroidViewModel(application) {
    private val _menuData = MutableStateFlow<List<List<MenuItem>>>(emptyList())
    val menuData: StateFlow<List<List<MenuItem>>> = _menuData.asStateFlow()

    private val _isLoading = MutableStateFlow(false)
    val isLoading: StateFlow<Boolean> = _isLoading.asStateFlow()

    private val apiService = RetrofitClient.apiService
    private val categoryMap = mutableMapOf<String, List<MenuItem>>()

    private val _currentCategory = MutableStateFlow<List<MenuItem>>(emptyList())
    val currentCategory: StateFlow<List<MenuItem>> = _currentCategory.asStateFlow()

    val categoryTitles = listOf(
        "COFFEE", "LATTE", "TEA", "SMOOTHE",
        "SHAKE/FRAPPE", "BEVERAGE", "BAKERY/DESSERT"
    )

    private val _cartItems = MutableStateFlow<List<MenuItem>>(emptyList())
    val cartItems: StateFlow<List<MenuItem>> = _cartItems.asStateFlow()

    private val _currentOrder = MutableLiveData<List<MenuItem>>()
    val currentOrder: LiveData<List<MenuItem>> = _currentOrder

    private val _userInfo = MutableLiveData<UserInfo>()
    val userInfo: LiveData<UserInfo> = _userInfo

    private val _orderStatus = MutableLiveData<String?>()
    val orderStatus: LiveData<String?> = _orderStatus

    private var orderStatusCheckJob: Job? = null

    private val _currentOrderId = MutableLiveData<Int?>(null)
    val currentOrderId: LiveData<Int?> = _currentOrderId

    private val viewModelJob = SupervisorJob()
    private val externalScope = CoroutineScope(Dispatchers.Default + viewModelJob)

    private val _shouldHideOrder = MutableLiveData<Boolean>()
    val shouldHideOrder: LiveData<Boolean> = _shouldHideOrder

    init {
        externalScope.launch {
            supervisorScope {
                launch { loadMenuData() }
                launch {
                    // 앱 시작 시 저장된 주문 정보 복원
                    loadUserInfo()
                }
            }
        }
    }

    fun loadMenuData() {
        externalScope.launch {
            try {
                withContext(Dispatchers.IO) {
                    val response = apiService.getMenus().execute()
                    if (response.isSuccessful && response.body() != null) {
                        val menuList = response.body()!!.data
                        val processedMenus = processMenuData(menuList)

                        withContext(Dispatchers.Main) {
                            _menuData.emit(processedMenus)
                            _currentCategory.emit(processedMenus.firstOrNull() ?: emptyList())
                        }
                    }
                }
            } catch (e: Exception) {
                Log.e("OrderViewModel", "메뉴 로딩 실패", e)
            }
        }
    }

    private fun processMenuData(menuList: List<MenuItemResponse>): List<List<MenuItem>> {
        return menuList
            .asSequence()
            .map { MenuItem.fromServerResponse(it) }
            .groupBy { it.category }
            .let { menuMap ->
                categoryTitles.map { category ->
                    menuMap[category] ?: emptyList()
                }
            }
    }

    fun getCategoryTitle(position: Int): String {
        return categoryTitles.getOrNull(position) ?: ""
    }

    fun getMenuCategorySize(): Int {
        return _menuData.value.size
    }

    fun getMenuCategory(position: Int): List<MenuItem> {
        return _menuData.value.getOrNull(position) ?: emptyList()
    }

    fun saveToCart(menuItem: MenuItem, token: String) {
        externalScope.launch(Dispatchers.IO) {
            try {
                val cartOrder = CartOrder(
                    name = menuItem.name,
                    quantity = menuItem.quantity,
                    hotIceOption = menuItem.hotIceOption,
                    size = menuItem.size,
                    shotQuantity = menuItem.shotQuantity,
                    hazelnutQuantity = menuItem.hazelnutQuantity,
                    tapiocaQuantity = menuItem.tapiocaQuantity,
                    decafQuantity = menuItem.decafQuantity,
                    vanillaQuantity = menuItem.vanillaQuantity,
                    whippingQuantity = menuItem.whippingQuantity,
                    iceCreamQuantity = menuItem.iceCreamQuantity,
                    selectedOptions = menuItem.getOptionsText()
                )

                val request = CartOrderRequest(items = listOf(cartOrder))

                val response = RetrofitClient.apiService.createCartOrder(
                    token = "Bearer $token",
                    request = request
                ).execute()

                if (!response.isSuccessful) {
                    Log.e("OrderViewModel", "장바구니 저장 실패: ${response.errorBody()?.string()}")
                }
            } catch (e: Exception) {
                Log.e("OrderViewModel", "장바구니 저장 실패", e)
            }
        }
    }

    fun addToCart(menuItem: MenuItem) {
        val currentItems = _cartItems.value.toMutableList()
        currentItems.add(menuItem)
        _cartItems.value = currentItems

        // 디버깅을 위한 로그
        Log.d("OrderViewModel", "Cart items after adding: ${_cartItems.value}")
    }

    fun getCartItems(): List<MenuItem> {
        return _cartItems.value
    }

    fun setCurrentOrder(items: List<MenuItem>, orderId: Int) {
        Log.d("OrderViewModel", "주문 정보 저장 시작")

        // 수량이 2 이상인 메뉴 아이템을 개별 아이템으로 분리
        val expandedItems = items.flatMap { menuItem ->
            List(menuItem.quantity) {
                menuItem.copy(quantity = 1)
            }
        }

        Log.d("OrderViewModel", "주문 아이템: ${expandedItems.map { it.name }}")
        Log.d("OrderViewModel", "주문 ID: $orderId")

        _currentOrder.value = expandedItems
        _currentOrderId.value = orderId
        _orderStatus.value = "준비중"

        Log.d("OrderViewModel", "주문 정보 저장 완료")
        Log.d("OrderViewModel", "현재 주문 상태: ${_orderStatus.value}")

        // 주문 ID를 SharedPreferences에 저장
        getApplication<Application>()
            .getSharedPreferences("auth", Context.MODE_PRIVATE)
            .edit()
            .putInt("current_order_id", orderId)
            .apply()

        _currentOrderId.value = orderId
        _orderStatus.value = "준비중"

        startOrderStatusCheck()
        loadUserInfo()
    }

    private fun startOrderStatusCheck() {
        orderStatusCheckJob?.cancel()
        orderStatusCheckJob = externalScope.launch {
            try {
                while (isActive) {
                    delay(5000)

                    val token = getApplication<Application>()
                        .getSharedPreferences("auth", Context.MODE_PRIVATE)
                        .getString("token", null) ?: break

                    val currentOrderId = _currentOrderId.value ?: break

                    withContext(Dispatchers.IO) {
                        val response = RetrofitClient.apiService.getOrderStatus(
                            "Bearer $token",
                            currentOrderId
                        ).execute()

                        if (response.isSuccessful && response.body()?.success == true) {
                            response.body()?.data?.firstOrNull()?.let { order ->
                                withContext(Dispatchers.Main) {
                                    _orderStatus.value = order.status
                                    if (order.status == "완료") {
                                        // 15분 딜레이는 별도의 코루틴으로 분리
                                        launch {
                                            delay(15 * 60 * 1000)
                                            clearCurrentOrder()
                                        }
                                        return@withContext
                                    }
                                }
                            }
                        }
                    }
                }
            } catch (e: Exception) {
                Log.e("OrderViewModel", "주문 상태 확인 실패", e)
            }
        }
    }

    fun clearCurrentOrder() {
        _currentOrder.value = emptyList()
        _currentOrderId.value = null
        _orderStatus.value = null
        orderStatusCheckJob?.cancel()

        // SharedPreferences에서도 주문 정보 제거
        getApplication<Application>()
            .getSharedPreferences("auth", Context.MODE_PRIVATE)
            .edit()
            .remove("current_order_id")
            .apply()
    }

    private fun loadUserInfo() {
        viewModelScope.launch(Dispatchers.IO) {
            try {
                val token = getApplication<Application>()
                    .getSharedPreferences("auth", Context.MODE_PRIVATE)
                    .getString("token", null) ?: return@launch

                val savedOrderId = getApplication<Application>()
                    .getSharedPreferences("auth", Context.MODE_PRIVATE)
                    .getInt("current_order_id", -1)

                supervisorScope {
                    // 사용자 정보 로드
                    launch {
                        val userResponse = RetrofitClient.apiService.getUserInfo("Bearer $token").execute()
                        if (userResponse.isSuccessful && userResponse.body()?.success == true) {
                            withContext(Dispatchers.Main) {
                                _userInfo.value = userResponse.body()?.data
                            }
                        }
                    }

                    // 저장된 주문 정보가 있으면 복원
                    if (savedOrderId != -1) {
                        launch {
                            val orderResponse = RetrofitClient.apiService.getOrderStatus(
                                "Bearer $token",
                                savedOrderId
                            ).execute()

                            if (orderResponse.isSuccessful && orderResponse.body()?.success == true) {
                                orderResponse.body()?.data?.firstOrNull()?.let { order ->
                                    withContext(Dispatchers.Main) {
                                        if (order.status != "��료") {
                                            _currentOrderId.value = order.order_id
                                            _orderStatus.value = order.status
                                            // 주문 상태 체크 시작
                                            startOrderStatusCheck()
                                        } else {
                                            clearCurrentOrder()
                                        }
                                    }
                                }
                            }
                        }
                    }
                }
            } catch (e: Exception) {
                Log.e("OrderViewModel", "사용자/주문 정보 로드 실패", e)
            }
        }
    }

    override fun onCleared() {
        super.onCleared()
        viewModelJob.cancel()
    }
} 