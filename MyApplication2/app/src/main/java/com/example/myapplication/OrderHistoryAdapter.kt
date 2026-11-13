package com.example.myapplication

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageButton
import android.widget.LinearLayout
import android.widget.TextView
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView

class OrderHistoryAdapter : ListAdapter<OrderGroup, OrderHistoryAdapter.ViewHolder>(DiffCallback()) {

    // 펼침 상태를 저장할 HashSet
    private val expandedItems = HashSet<String>()

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.item_order_history, parent, false)
        return ViewHolder(view, expandedItems)
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        holder.bind(getItem(position))
    }

    class ViewHolder(
        view: View,
        private val expandedItems: HashSet<String>
    ) : RecyclerView.ViewHolder(view) {
        private val orderDate: TextView = view.findViewById(R.id.tv_order_date)
        private val orderItems: TextView = view.findViewById(R.id.tv_order_items)
        private val orderOptions: TextView = view.findViewById(R.id.tv_order_options)
        private val totalAmount: TextView = view.findViewById(R.id.tv_total_amount)
        private val orderStatus: TextView = view.findViewById(R.id.tv_order_status)
        private val expandButton: ImageButton = view.findViewById(R.id.btn_expand)
        private val expandedItemsLayout: LinearLayout = view.findViewById(R.id.layout_expanded_items)

        fun bind(orderGroup: OrderGroup) {
            orderDate.text = orderGroup.orderTime

            // 첫 번째 메뉴만 표시하고 총 금액 표시
            val firstOrder = orderGroup.orders.first()

            // 총 수량 계산
            val totalQuantity = orderGroup.orders.sumOf { it.quantity }

            // 메뉴 이름과 총 수량 표시
            orderItems.text = if (totalQuantity > 1) {
                "${firstOrder.menuName}  외 ${totalQuantity - 1}"
            } else {
                firstOrder.menuName
            }

            // 옵션은 펼쳤을 때만 보이도록 숨김
            orderOptions.visibility = View.GONE

            totalAmount.text = "${orderGroup.totalAmount}원"
            orderStatus.text = orderGroup.status

            // 총문 시간을 키로 사용
            val key = orderGroup.orderTime

            // 총 수량이 1개 초과이거나 여러 개의 주문이 있는 경우 펼치기 버튼 표시
            if (totalQuantity > 1 || orderGroup.orders.size > 1) {
                expandButton.visibility = View.VISIBLE

                // 저장된 상태에 따라 펼침/접힘 상태 설정
                val isExpanded = expandedItems.contains(key)
                expandedItemsLayout.visibility = if (isExpanded) View.VISIBLE else View.GONE
                expandButton.rotation = if (isExpanded) 180f else 0f

                // 필요한 경우에만 setupExpandableContent 호출
                if (isExpanded) {
                    setupExpandableContent(orderGroup)
                }

                expandButton.setOnClickListener {
                    if (expandedItems.contains(key)) {
                        expandedItems.remove(key)
                        expandedItemsLayout.visibility = View.GONE
                        expandButton.rotation = 0f
                    } else {
                        expandedItems.add(key)
                        setupExpandableContent(orderGroup)
                        expandedItemsLayout.visibility = View.VISIBLE
                        expandButton.rotation = 180f
                    }
                }
            } else {
                expandButton.visibility = View.GONE
                expandedItemsLayout.visibility = View.GONE

                // 수량이 1개일 때는 옵션 바로 표시
                val optionsText = buildOptionText(firstOrder)
                if (optionsText.isNotEmpty()) {
                    orderOptions.text = optionsText
                    orderOptions.visibility = View.VISIBLE
                } else {
                    orderOptions.visibility = View.GONE
                }
            }
        }

        private fun buildOptionText(order: OrderHistory): String {
            val options = buildString {
                if (order.menuName !in listOf("사케라또", "아포카토")) {
                    val basicOptions = mutableListOf<String>()
                    if (!order.options.hotIceOption.isNullOrEmpty()) {
                        basicOptions.add(order.options.hotIceOption)
                    }
                    if (order.options.size.isNotEmpty()) {
                        basicOptions.add(order.options.size)
                    }
                    if (basicOptions.isNotEmpty()) {
                        append(basicOptions.joinToString(" / "))
                    }
                }

                val extraOptions = mutableListOf<String>()
                with(order.options) {
                    if (shot > 0) extraOptions.add("샷 ${shot}")
                    if (hazelnut > 0) extraOptions.add("헤이즐럿 시럽 ${hazelnut}")
                    if (tapioca > 0) extraOptions.add("타피오카 펄 ${tapioca}")
                    if (decaf > 0) extraOptions.add("디카페인 샷 ${decaf}")
                    if (vanilla > 0) extraOptions.add("바닐라 시럽 ${vanilla}")
                    if (whipping > 0) extraOptions.add("휘핑크림 ${whipping}")
                    if (iceCreamQuantity > 0) extraOptions.add("아이스크림 ${iceCreamQuantity}")
                }
                if (extraOptions.isNotEmpty()) {
                    if (isNotEmpty()) append(" / ")
                    append(extraOptions.joinToString(" / "))
                }
            }
            return options.toString()
        }

        private fun setupExpandableContent(orderGroup: OrderGroup) {
            expandedItemsLayout.removeAllViews()

            // 수량만큼 개별 아이템으로 표시
            orderGroup.orders.forEach { order ->
                // 각 주문의 수량만큼 반복
                repeat(order.quantity) {
                    val itemView = LayoutInflater.from(itemView.context)
                        .inflate(R.layout.item_expanded_order_history, expandedItemsLayout, false)

                    // 메뉴 이름 표시
                    itemView.findViewById<TextView>(R.id.tv_expanded_menu_name).text = "• ${order.menuName}"

                    // 옵션 표시
                    val optionsText = buildOptionText(order)
                    val optionsView = itemView.findViewById<TextView>(R.id.tv_expanded_menu_options)
                    if (optionsText.isNotEmpty()) {
                        optionsView.text = optionsText
                        optionsView.visibility = View.VISIBLE
                    } else {
                        optionsView.visibility = View.GONE
                    }

                    // 개별 메뉴 가격 표시 (총 가격을 수량으로 나눔)
                    val unitPrice = order.totalAmount / order.quantity
                    itemView.findViewById<TextView>(R.id.tv_expanded_menu_price).text = "${unitPrice}원"

                    expandedItemsLayout.addView(itemView)
                }
            }
        }
    }

    private class DiffCallback : DiffUtil.ItemCallback<OrderGroup>() {
        override fun areItemsTheSame(oldItem: OrderGroup, newItem: OrderGroup) =
            oldItem.orderTime == newItem.orderTime
        override fun areContentsTheSame(oldItem: OrderGroup, newItem: OrderGroup) =
            oldItem == newItem
    }
}