package com.example.myapplication

import android.graphics.Typeface
import android.os.Handler
import android.os.Looper
import android.text.Spannable
import android.text.SpannableString
import android.text.style.StyleSpan
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageButton
import android.widget.LinearLayout
import android.widget.TextView
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.RecyclerView
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

class OrderStatusAdapter : RecyclerView.Adapter<OrderStatusAdapter.OrderViewHolder>() {
    private var items: List<MenuItem> = listOf()

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): OrderViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.item_order_status_menu, parent, false)
        return OrderViewHolder(view)
    }

    override fun onBindViewHolder(holder: OrderViewHolder, position: Int) {
        if (items.isEmpty()) return
        holder.bind(items)
    }

    override fun getItemCount(): Int {
        return if (items.isNotEmpty()) 1 else 0
    }

    fun submitList(newItems: List<MenuItem>) {
        items = newItems
        notifyDataSetChanged()
    }

    class OrderViewHolder(view: View) : RecyclerView.ViewHolder(view) {
        private val tvMenuName: TextView = view.findViewById(R.id.tv_menu_name)
        private val tvMenuOptions: TextView = view.findViewById(R.id.tv_menu_options)
        private val btnExpand: ImageButton = view.findViewById(R.id.btn_expand)
        private val layoutExpandedItems: LinearLayout = view.findViewById(R.id.layout_expanded_items)
        private var isExpanded = false
        private var allItems: List<MenuItem> = listOf()

        fun bind(items: List<MenuItem>) {
            allItems = items
            val firstItem = items[0]
            tvMenuName.text = "• ${firstItem.name}"
            
            val optionText = buildOptionText(firstItem, isExpanded)
            
            if (optionText.isNotEmpty()) {
                tvMenuOptions.visibility = View.VISIBLE
                tvMenuOptions.text = optionText
            } else {
                tvMenuOptions.visibility = View.GONE
            }

            if (items.size > 1) {
                btnExpand.visibility = View.VISIBLE
                setupExpandableContent()
            } else {
                btnExpand.visibility = View.GONE
                layoutExpandedItems.visibility = View.GONE
            }

            btnExpand.setOnClickListener {
                isExpanded = !isExpanded
                tvMenuOptions.text = buildOptionText(firstItem, isExpanded)
                layoutExpandedItems.visibility = if (isExpanded) View.VISIBLE else View.GONE
                btnExpand.rotation = if (isExpanded) 180f else 0f
            }
        }

        private fun setupExpandableContent() {
            layoutExpandedItems.removeAllViews()
            for (i in 1 until allItems.size) {
                val itemView = LayoutInflater.from(itemView.context)
                    .inflate(R.layout.item_expanded_menu_option, layoutExpandedItems, false)
                
                val tvMenuName = itemView.findViewById<TextView>(R.id.tv_expanded_menu_name)
                val tvMenuOptions = itemView.findViewById<TextView>(R.id.tv_expanded_menu_options)
                
                val item = allItems[i]
                tvMenuName.text = "• ${item.name}"
                
                val optionText = buildOptionText(item, true)
                if (optionText.isNotEmpty()) {
                    tvMenuOptions.visibility = View.VISIBLE
                    tvMenuOptions.text = optionText
                } else {
                    tvMenuOptions.visibility = View.GONE
                }
                
                layoutExpandedItems.addView(itemView)
            }
        }

        private fun buildOptionText(menuItem: MenuItem, isExpanded: Boolean = false): CharSequence {
            val baseText = buildString {
                when (menuItem.category) {
                    "BAKERY/DESSERT" -> {
                        return@buildString
                    }
                    else -> {
                        val hasOptions = menuItem.shotQuantity > 0 || 
                                       menuItem.hazelnutQuantity > 0 || 
                                       menuItem.vanillaQuantity > 0 || 
                                       menuItem.whippingQuantity > 0 || 
                                       menuItem.iceCreamQuantity > 0 || 
                                       menuItem.tapiocaQuantity > 0 || 
                                       menuItem.decafQuantity > 0

                        if (menuItem.name !in listOf("사케라또", "아포카토")) {
                            if (menuItem.hotIceOption != null && menuItem.size != null) {
                                append(menuItem.hotIceOption)
                                append(" / ")
                                append(menuItem.size)
                                if (hasOptions) append(" / ")
                            }
                        }

                        val options = mutableListOf<String>()
                        if (menuItem.shotQuantity > 0) options.add("샷 ${menuItem.shotQuantity}")
                        if (menuItem.hazelnutQuantity > 0) options.add("헤이즐넛 시럽 ${menuItem.hazelnutQuantity}")
                        if (menuItem.vanillaQuantity > 0) options.add("바닐라 시럽 ${menuItem.vanillaQuantity}")
                        if (menuItem.whippingQuantity > 0) options.add("휘핑크림 ${menuItem.whippingQuantity}")
                        if (menuItem.iceCreamQuantity > 0) options.add("아이스크림 ${menuItem.iceCreamQuantity}")
                        if (menuItem.tapiocaQuantity > 0) options.add("타피오카 펄 ${menuItem.tapiocaQuantity}")
                        if (menuItem.decafQuantity > 0) options.add("디카페인 샷 ${menuItem.decafQuantity}")
                        
                        if (options.isNotEmpty()) {
                            append(options.joinToString(" / "))
                        }
                    }
                }
            }

            return if (!isExpanded && allItems.size > 1 && baseText.isNotEmpty()) {
                val fullText = "$baseText  외 ${allItems.size - 1}"
                SpannableString(fullText).apply {
                    setSpan(
                        StyleSpan(Typeface.BOLD),
                        baseText.length + 1,
                        fullText.length,
                        Spannable.SPAN_EXCLUSIVE_EXCLUSIVE
                    )
                }
            } else {
                baseText
            }
        }
    }
} 