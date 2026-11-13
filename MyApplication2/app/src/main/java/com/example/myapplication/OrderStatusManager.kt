package com.example.myapplication

import android.content.Context
import android.graphics.Typeface
import android.util.Log
import android.view.View
import android.widget.TextView
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import android.text.Spannable
import android.text.SpannableString
import android.text.style.StyleSpan

class OrderStatusManager(private val context: Context) {
    private var orderStatusAdapter: OrderStatusAdapter? = null

    init {
        orderStatusAdapter = OrderStatusAdapter().apply {
            setHasStableIds(true)
        }
    }

    fun getOrderStatusAdapter(): OrderStatusAdapter {
        return orderStatusAdapter ?: OrderStatusAdapter().apply {
            setHasStableIds(true)
        }.also {
            orderStatusAdapter = it
        }
    }

    fun updateOrderStatus(
        orderStatusOverlay: View,
        layoutOrderMenu: RecyclerView,
        tvOrderStatus: TextView,
        orderItems: List<MenuItem>,
        userName: String,
        orderStatus: String
    ) {
        try {
            if (layoutOrderMenu.adapter == null) {
                layoutOrderMenu.adapter = getOrderStatusAdapter()
                layoutOrderMenu.layoutManager = LinearLayoutManager(context)
            }

            val statusText = if (orderStatus == "완료") {
                SpannableString("$userName 님의 메뉴가 준비되었습니다 (o゜▽゜)o☆").apply {
                    setSpan(StyleSpan(Typeface.BOLD), 0, userName.length + 2, Spannable.SPAN_EXCLUSIVE_EXCLUSIVE)
                }
            } else {
                SpannableString("$userName 님의 메뉴가 준비중입니다").apply {
                    setSpan(StyleSpan(Typeface.BOLD), 0, userName.length + 2, Spannable.SPAN_EXCLUSIVE_EXCLUSIVE)
                }
            }

            orderStatusOverlay.visibility = View.VISIBLE
            tvOrderStatus.text = statusText
            orderStatusAdapter?.submitList(orderItems)

        } catch (e: Exception) {
            Log.e("OrderStatusManager", "주문 상태 업데이트 실패", e)
        }
    }
} 