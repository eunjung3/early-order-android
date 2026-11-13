package com.example.myapplication

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.bumptech.glide.Glide

class OrderSummaryAdapter(private val items: List<MenuItem>) : 
    RecyclerView.Adapter<OrderSummaryAdapter.ViewHolder>() {

    class ViewHolder(view: View) : RecyclerView.ViewHolder(view) {
        val menuImage: ImageView = view.findViewById(R.id.iv_menu_image)
        val menuNameTextView: TextView = view.findViewById(R.id.tv_menu_name)
//        val menuPriceTextView: TextView = view.findViewById(R.id.tv_menu_price)
        val menuQuantityTextView: TextView = view.findViewById(R.id.tv_menu_quantity)
        val selectedOptionsTextView: TextView = view.findViewById(R.id.tv_selected_options)
        val totalPriceTextView: TextView = view.findViewById(R.id.tv_total_price)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.item_order_summary, parent, false)
        return ViewHolder(view)
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        val item = items[position]
        
        // 이미지 설정 (Glide 사용)
        if (!item.imageUrl.isNullOrEmpty()) {
            Glide.with(holder.itemView.context)
                .load(item.imageUrl)
                .centerCrop()
                .into(holder.menuImage)
        } else {
            holder.menuImage.setImageResource(android.R.drawable.ic_menu_gallery)
        }
        
        // 메뉴 이름과 기본 가격
        holder.menuNameTextView.text = item.name
//        holder.menuPriceTextView.text = "${item.price}원"
        
        // 수량
        holder.menuQuantityTextView.text = "수량: ${item.quantity}개"
        
        // 디저트가 아닌 경우에만 옵션을 표시
        if (item.category != "BAKERY/DESSERT") {
            val options = mutableListOf<String>()
            
            // 사케라또, 아포카토가 아닌 경우에만 필수 옵션 추가
            if (item.name !in listOf("사케라또", "아포카토")) {
                // HOT/ICE 옵션
                item.hotIceOption?.let { options.add(it) }
                // 사이즈 옵션
                item.size?.let { options.add(it) }
            }
            
            // 추가 옵션들
            if (item.shotQuantity > 0) options.add("샷 ${item.shotQuantity}")
            if (item.hazelnutQuantity > 0) options.add("헤이즐넛 시럽 ${item.hazelnutQuantity}")
            if (item.tapiocaQuantity > 0) options.add("타피오카 펄 ${item.tapiocaQuantity}")
            if (item.decafQuantity > 0) options.add("디카페인 샷 ${item.decafQuantity}")
            if (item.vanillaQuantity > 0) options.add("바닐라 시럽 ${item.vanillaQuantity}")
            if (item.whippingQuantity > 0) options.add("휘핑크림 ${item.whippingQuantity}")
            if (item.iceCreamQuantity > 0) options.add("아이스크림 ${item.iceCreamQuantity}")
            
            holder.selectedOptionsTextView.text = options.joinToString(" / ")
        } else {
            holder.selectedOptionsTextView.text = ""  // 디저트인 경우 빈 문자열 표시
        }
        
        // 총 가격 (기본 가격 + 옵션 가격) * 수량
        val totalPrice = item.calculateTotalPrice()
        holder.totalPriceTextView.text = "${totalPrice}원"
    }

    override fun getItemCount() = items.size
}