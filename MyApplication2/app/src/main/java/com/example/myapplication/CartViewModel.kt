package com.example.myapplication

import android.app.Application
import android.content.Context
import android.util.Log
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import retrofit2.Call
import retrofit2.Callback
import retrofit2.Response

class CartViewModel(application: Application) : AndroidViewModel(application) {
    private val _cartItems = MutableLiveData<List<MenuItem>>(emptyList())
    val cartItems: LiveData<List<MenuItem>> = _cartItems
    
    private val _cartCount = MutableLiveData(0)
    val cartCount: LiveData<Int> = _cartCount
    
    private val _totalPrice = MutableLiveData(0)
    val totalPrice: LiveData<Int> = _totalPrice
    
    private val _selectedCount = MutableLiveData(0)
    val selectedCount: LiveData<Int> = _selectedCount

    private val _isAllSelected = MutableLiveData(false)
    val isAllSelected: LiveData<Boolean> = _isAllSelected

    fun addToCart(menuItem: MenuItem) {
        viewModelScope.launch {
            val token = getApplication<Application>()
                .getSharedPreferences("auth", Context.MODE_PRIVATE)
                .getString("token", null) ?: return@launch

            val currentList = _cartItems.value?.toMutableList() ?: mutableListOf()
            val existingItem = currentList.find { 
                it.name == menuItem.name && it.hasSameOptionsAs(menuItem)
            }

            if (existingItem != null) {
                currentList.remove(existingItem)
                currentList.add(existingItem.copy(quantity = existingItem.quantity + menuItem.quantity))
            } else {
                currentList.add(menuItem)
            }

            _cartItems.value = currentList
            updateCartCount()
            saveToServer("Bearer $token")
        }
    }

    fun updateItemQuantity(item: MenuItem, newQuantity: Int) {
        viewModelScope.launch {
            val token = getApplication<Application>()
                .getSharedPreferences("auth", Context.MODE_PRIVATE)
                .getString("token", null) ?: return@launch

            val currentList = _cartItems.value?.toMutableList() ?: mutableListOf()
            val index = currentList.indexOfFirst {
                it.name == item.name && it.hasSameOptionsAs(item)
            }
            if (index != -1) {
                currentList[index] = item.copy(quantity = newQuantity)
                _cartItems.value = currentList
                updateCartCount()
                saveToServer("Bearer $token")
            }
        }
    }

    fun updateItemSelection(item: MenuItem, isSelected: Boolean) {
        viewModelScope.launch {
            val token = getApplication<Application>()
                .getSharedPreferences("auth", Context.MODE_PRIVATE)
                .getString("token", null) ?: return@launch

            val currentList = _cartItems.value?.toMutableList() ?: mutableListOf()
            val index = currentList.indexOfFirst {
                it.name == item.name && it.hasSameOptionsAs(item)
            }
            if (index != -1) {
                currentList[index] = item.copy(isSelected = isSelected)
                _cartItems.value = currentList
                saveToServer("Bearer $token")
            }
        }
    }

    fun removeItem(item: MenuItem) {
        viewModelScope.launch {
            val token = getApplication<Application>()
                .getSharedPreferences("auth", Context.MODE_PRIVATE)
                .getString("token", null) ?: return@launch

            Log.d("CartViewModel", "removeItem 시작")
            Log.d("CartViewModel", "제거할 아이템: ${item.name} (${item.category})")
            Log.d("CartViewModel", "제거할 아이템의 옵션: HOT/ICE=${item.hotIceOption}, 크기=${item.size}")
            
            val currentList = _cartItems.value?.toMutableList() ?: mutableListOf()
            currentList.removeAll { it.name == item.name && it.hasSameOptionsAs(item) }
            
            Log.d("CartViewModel", "제거 후 장바구니 아이템 수: ${currentList.size}")
            _cartItems.value = currentList
            updateCartCount()
            saveToServer("Bearer $token")
        }
    }

    fun loadUserCart(token: String) {
        viewModelScope.launch(Dispatchers.IO) {
            try {
                val savedCartCount = getApplication<Application>()
                    .getSharedPreferences("cart", Context.MODE_PRIVATE)
                    .getInt("cart_count", 0)
                
                withContext(Dispatchers.Main) {
                    _cartCount.value = savedCartCount
                }

                RetrofitClient.apiService.getCartItems("Bearer $token")
                    .enqueue(object : Callback<CartResponse> {
                        override fun onResponse(
                            call: Call<CartResponse>,
                            response: Response<CartResponse>
                        ) {
                            if (response.isSuccessful && response.body()?.success == true) {
                                val cartItems = response.body()?.data?.map { cartItem ->
                                    MenuItem(
                                        name = cartItem.menuItem.name,
                                        price = cartItem.menuItem.price,
                                        imageResource = 0,
                                        imageUrl = cartItem.menuItem.imageUrl,
                                        isSoldOut = false,
                                        quantity = cartItem.quantity,
                                        hotIceOption = cartItem.hotIceOption,
                                        size = cartItem.size,
                                        shotQuantity = cartItem.shotQuantity,
                                        hazelnutQuantity = cartItem.hazelnutQuantity,
                                        tapiocaQuantity = cartItem.tapiocaQuantity,
                                        decafQuantity = cartItem.decafQuantity,
                                        vanillaQuantity = cartItem.vanillaQuantity,
                                        whippingQuantity = cartItem.whippingQuantity,
                                        iceCreamQuantity = cartItem.iceCreamQuantity,
                                        isSelected = false,
                                        category = cartItem.menuItem.category ?: "COFFEE"
                                    )
                                } ?: emptyList()
                                
                                viewModelScope.launch(Dispatchers.Main) {
                                    _cartItems.value = cartItems
                                    updateCartCount()
                                    updateTotalPriceAndCount()
                                    
                                    saveCartCount(cartItems.sumOf { it.quantity })
                                }
                            }
                        }

                        override fun onFailure(call: Call<CartResponse>, t: Throwable) {
                            Log.e("CartViewModel", "장바구니 로드 실패", t)
                        }
                    })
            } catch (e: Exception) {
                Log.e("CartViewModel", "장바구니 로드 실패", e)
            }
        }
    }

    private fun saveCartCount(count: Int) {
        getApplication<Application>()
            .getSharedPreferences("cart", Context.MODE_PRIVATE)
            .edit()
            .putInt("cart_count", count)
            .apply()
    }

    private fun updateCartCount() {
        val count = _cartItems.value?.sumOf { it.quantity } ?: 0
        _cartCount.value = count
        saveCartCount(count)
    }

    fun clearCart() {
        viewModelScope.launch {
            val token = getApplication<Application>()
                .getSharedPreferences("auth", Context.MODE_PRIVATE)
                .getString("token", null)

            _cartItems.value = emptyList()
            _totalPrice.value = 0
            _selectedCount.value = 0
            _cartCount.value = 0

            token?.let { saveToServer("Bearer $it") }
        }
    }

    fun updateAllItemsSelection(isSelected: Boolean) {
        viewModelScope.launch(Dispatchers.Default) {
            val token = getApplication<Application>()
                .getSharedPreferences("auth", Context.MODE_PRIVATE)
                .getString("token", null)
            
            val currentList = _cartItems.value?.toMutableList() ?: mutableListOf()
            currentList.forEach { it.isSelected = isSelected }
            withContext(Dispatchers.Main) {
                _cartItems.value = currentList
                updateTotalPriceAndCount()
            }
            token?.let { saveToServer("Bearer $it") }
        }
    }

    fun deleteSelectedItems() {
        viewModelScope.launch(Dispatchers.Default) {
            val token = getApplication<Application>()
                .getSharedPreferences("auth", Context.MODE_PRIVATE)
                .getString("token", null)
            
            val currentList = _cartItems.value?.toMutableList() ?: mutableListOf()
            currentList.removeAll { it.isSelected }
            withContext(Dispatchers.Main) {
                _cartItems.value = currentList
                updateCartCount()
            }
            token?.let { saveToServer("Bearer $it") }
        }
    }

    fun updateTotalPriceAndCount() {
        viewModelScope.launch(Dispatchers.Default) {
            val items = _cartItems.value ?: emptyList()
            val total = items.sumOf { item ->
                if (item.isSelected) item.calculateTotalPrice() else 0
            }
            val count = items.sumOf { if (it.isSelected) it.quantity else 0 }
            
            val allSelected = items.isNotEmpty() && items.all { it.isSelected }
            
            withContext(Dispatchers.Main) {
                _totalPrice.value = total
                _selectedCount.value = count
                _isAllSelected.value = allSelected
            }
        }
    }

    private fun saveToServer(token: String) {
        viewModelScope.launch(Dispatchers.IO) {
            try {
                val items = _cartItems.value ?: return@launch
                val request = CartOrderRequest(
                    items = items.map { menuItem ->
                        CartOrder(
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
                    }
                )

                withContext(Dispatchers.IO) {
                    RetrofitClient.apiService.createCartOrder(
                        token = token,
                        request = request
                    ).enqueue(object : Callback<ApiResponse<String>> {
                        override fun onResponse(
                            call: Call<ApiResponse<String>>,
                            response: Response<ApiResponse<String>>
                        ) {
                            if (!response.isSuccessful) {
                                Log.e("CartViewModel", "장바구니 저장 실패: ${response.errorBody()?.string()}")
                            }
                        }

                        override fun onFailure(call: Call<ApiResponse<String>>, t: Throwable) {
                            Log.e("CartViewModel", "장바구니 저장 실패", t)
                        }
                    })
                }
            } catch (e: Exception) {
                Log.e("CartViewModel", "장바구니 저장 실패", e)
            }
        }
    }

    fun removeSelectedItems(selectedItems: List<MenuItem>) {
        viewModelScope.launch {
            val token = getApplication<Application>()
                .getSharedPreferences("auth", Context.MODE_PRIVATE)
                .getString("token", null) ?: return@launch

            Log.d("CartViewModel", "선택된 아이템들 제거 시작")
            Log.d("CartViewModel", "제거할 아이템 수: ${selectedItems.size}")
            selectedItems.forEach { item ->
                Log.d("CartViewModel", "제거 예정 아이템: ${item.name}, 옵션: HOT/ICE=${item.hotIceOption}, 크기=${item.size}")
            }

            val currentList = _cartItems.value?.toMutableList() ?: mutableListOf()
            
            // 선택된 아이템들과 정확히 일치하는 항목들을 찾아서 제거
            selectedItems.forEach { selectedItem ->
                currentList.removeAll { cartItem ->
                    cartItem.name == selectedItem.name &&
                    cartItem.hotIceOption == selectedItem.hotIceOption &&
                    cartItem.size == selectedItem.size &&
                    cartItem.shotQuantity == selectedItem.shotQuantity &&
                    cartItem.hazelnutQuantity == selectedItem.hazelnutQuantity &&
                    cartItem.tapiocaQuantity == selectedItem.tapiocaQuantity &&
                    cartItem.decafQuantity == selectedItem.decafQuantity &&
                    cartItem.vanillaQuantity == selectedItem.vanillaQuantity &&
                    cartItem.whippingQuantity == selectedItem.whippingQuantity &&
                    cartItem.iceCreamQuantity == selectedItem.iceCreamQuantity
                }
            }

            Log.d("CartViewModel", "제거 후 장바구니 아이템 수: ${currentList.size}")
            
            // 상태 업데이트와 서버 동기화를 한 번만 수행
            _cartItems.value = currentList
            updateCartCount()
            saveToServer("Bearer $token")
        }
    }
}
