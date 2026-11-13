package com.example.myapplication

import android.app.Application
import android.content.Context
import android.util.Log
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.launch

class OrderHistoryViewModel(application: Application) : AndroidViewModel(application) {
    private val _orderGroups = MutableLiveData<List<OrderGroup>>()
    val orderGroups: LiveData<List<OrderGroup>> = _orderGroups

    private val _isLoading = MutableLiveData<Boolean>()
    val isLoading: LiveData<Boolean> = _isLoading

    private val _error = MutableLiveData<String>()
    val error: LiveData<String> = _error

    private var startDate: String = ""
    private var endDate: String = ""

    fun fetchOrderHistory(startDate: String, endDate: String) {
        this.startDate = startDate
        this.endDate = endDate

        _isLoading.value = true
        viewModelScope.launch {
            try {
                Log.d("OrderHistory", "주문 내역 조회 시작 - 시작일: $startDate, 종료일: $endDate")

                val token = getApplication<Application>()
                    .getSharedPreferences("auth", Context.MODE_PRIVATE)
                    .getString("token", null)

                if (token == null) {
                    Log.e("OrderHistory", "토큰이 없음")
                    _error.value = "로그인이 필요합니다."
                    _isLoading.value = false
                    return@launch
                }

                Log.d("OrderHistory", "토큰 확인 완료: ${token.take(20)}...")

                RetrofitClient.apiService.getOrderHistory(
                    token = "Bearer $token",
                    startDate = startDate,
                    endDate = endDate
                ).enqueue(object : retrofit2.Callback<OrderHistoryResponse> {
                    override fun onResponse(
                        call: retrofit2.Call<OrderHistoryResponse>,
                        response: retrofit2.Response<OrderHistoryResponse>
                    ) {
                        _isLoading.value = false
                        Log.d("OrderHistory", "응답 코드: ${response.code()}")
                        Log.d("OrderHistory", "응답 바디: ${response.body()}")

                        if (response.isSuccessful && response.body()?.data != null) {
                            val orders = response.body()?.data ?: emptyList()
                            Log.d("OrderHistory", "주문 내역 개수: ${orders.size}")
                            orders.forEachIndexed { index, order ->
                                Log.d("OrderHistory", "주문 #$index - ID: ${order.orderId}")
                                Log.d("OrderHistory", "메뉴 이름: ${order.menuName}")
                                Log.d("OrderHistory", "주문 상태: ${order.status}")
                                Log.d("OrderHistory", "주문 옵션: ${order.options}")
                                Log.d("OrderHistory", "총 금액: ${order.totalAmount}")
                                Log.d("OrderHistory", "주문 시간: ${order.orderDate}")
                            }

                            if (orders.isEmpty()) {
                                Log.d("OrderHistory", "주문 내역 없음")
                                _error.value = "주문 내역이 없습니다."
                            } else {
                                processOrders(orders)
                            }
                        } else {
                            Log.e("OrderHistory", "API 오류 - 코드: ${response.code()}")
                            Log.e("OrderHistory", "에러 바디: ${response.errorBody()?.string()}")
                            _error.value = when (response.code()) {
                                401 -> "로그인이 필요합니다."
                                404 -> "주문 내역이 없습니다."
                                else -> "주문 내역을 불러오는데 실패했습니다."
                            }
                        }
                    }

                    override fun onFailure(call: retrofit2.Call<OrderHistoryResponse>, t: Throwable) {
                        _isLoading.value = false
                        Log.e("OrderHistory", "네트워크 오류", t)
                        _error.value = "네트워크 오류가 발생했습니다."
                    }
                })
            } catch (e: Exception) {
                _isLoading.value = false
                Log.e("OrderHistory", "예외 발생", e)
                _error.value = "오류가 발생했습니다."
            }
        }
    }

    private fun processOrders(orders: List<OrderHistory>) {
        val filteredOrders = orders
            .filter { order -> 
                val orderDate = order.orderDate.substring(0, 10)  // "2024-11-30 20:35:14" -> "2024-11-30"
                orderDate in startDate..endDate && order.status != "대기중"
            }

        if (filteredOrders.isEmpty()) {
            _error.value = "선택한 기간에 주문 내역이 없습니다."
            _orderGroups.value = emptyList()
            return
        }

        val groupedOrders = filteredOrders
            .groupBy { it.orderDate }
            .map { (orderTime, ordersInGroup) ->
                OrderGroup(
                    orderTime = orderTime,
                    orders = ordersInGroup,
                    totalAmount = ordersInGroup.sumOf { it.totalAmount },
                    status = if (ordersInGroup.all { it.status == "완료" }) "준비 완료" else "준비중"
                )
            }
            .sortedByDescending { it.orderTime }

        _orderGroups.value = groupedOrders
    }
} 