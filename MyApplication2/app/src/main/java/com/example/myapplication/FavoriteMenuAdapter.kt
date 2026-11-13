package com.example.myapplication

import android.graphics.Color
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.ImageButton
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.bumptech.glide.Glide


class FavoriteMenuAdapter(
    private var items: List<MenuItem>,
    private val onAddToCart: (MenuItem) -> Unit,
    private val onPlaceOrder: (MenuItem) -> Unit,
    private val onRemoveFromFavorite: (MenuItem) -> Unit,
    private val onAddToFavorite: (MenuItem) -> Unit
) : RecyclerView.Adapter<FavoriteMenuAdapter.ViewHolder>() {

    class ViewHolder(view: View) : RecyclerView.ViewHolder(view) {
        val menuImage: ImageView = view.findViewById(R.id.iv_menu_image)
        val menuName: TextView = view.findViewById(R.id.tv_menu_name)
        val menuPrice: TextView = view.findViewById(R.id.tv_menu_price)
        val selectedOptions: TextView = view.findViewById(R.id.tv_selected_options)
        val btnAddToCart: Button = view.findViewById(R.id.btn_add_to_cart)
        val btnPlaceOrder: Button = view.findViewById(R.id.btn_place_order)
        val btnFavorite: ImageButton = view.findViewById(R.id.btn_favorite)
        val additionalOptionsContainer: LinearLayout = view.findViewById(R.id.container_additional_options)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.item_favorite_menu, parent, false)
        return ViewHolder(view)
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        val item = items[position]
        
        if (!item.imageUrl.isNullOrEmpty()) {
            Glide.with(holder.itemView.context)
                .load(item.imageUrl)
                .centerCrop()
                .into(holder.menuImage)
        } else {
            holder.menuImage.setImageResource(android.R.drawable.ic_menu_gallery)
        }
        
        holder.menuName.text = item.name
        holder.menuPrice.text = "${item.calculateTotalPrice()}원"

        // 기본 옵션 텍스트 설정 (HOT/ICE, 사이즈)
        val basicOptions = StringBuilder()
        if (item.hotIceOption.isNotEmpty()) {
            basicOptions.append(item.hotIceOption)
        }
        if (item.size.isNotEmpty()) {
            if (basicOptions.isNotEmpty()) basicOptions.append(" / ")
            basicOptions.append(item.size)
        }
        
        // 옵션이 있을 때만 표시하고, 없으면 숨김
        if (basicOptions.isNotEmpty()) {
            holder.selectedOptions.visibility = View.VISIBLE
            holder.selectedOptions.text = basicOptions.toString()
        } else {
            holder.selectedOptions.visibility = View.GONE
        }
        
        // 추가 옵션 컨테이너 초기화
        holder.additionalOptionsContainer.removeAllViews()
        
        // 추가 옵션 표시
        val additionalOptions = mutableListOf<String>()
        
        if (item.shotQuantity > 0) additionalOptions.add("샷 ${item.shotQuantity}")
        if (item.hazelnutQuantity > 0) additionalOptions.add("헤이즐넛 시럽 ${item.hazelnutQuantity}")
        if (item.tapiocaQuantity > 0) additionalOptions.add("타피오카 펄 ${item.tapiocaQuantity}")
        if (item.decafQuantity > 0) additionalOptions.add("디카페인 샷 ${item.decafQuantity}")
        if (item.vanillaQuantity > 0) additionalOptions.add("바닐라 시럽 ${item.vanillaQuantity}")
        if (item.whippingQuantity > 0) additionalOptions.add("휘핑크림 ${item.whippingQuantity}")
        if (item.iceCreamQuantity > 0) additionalOptions.add("아이스크림 ${item.iceCreamQuantity}")

        additionalOptions.forEach { optionText ->
            val optionView = TextView(holder.itemView.context).apply {
                text = optionText
                textSize = 12f
                setTextColor(Color.parseColor("#666666"))
                layoutParams = LinearLayout.LayoutParams(
                    LinearLayout.LayoutParams.WRAP_CONTENT,
                    LinearLayout.LayoutParams.WRAP_CONTENT
                ).apply {
                    topMargin = 2
                }
            }
            holder.additionalOptionsContainer.addView(optionView)
        }

        holder.btnAddToCart.setOnClickListener { 
            val cartItem = item.copy(
                quantity = 1,
                isSelected = true
            )
            onAddToCart(cartItem)
        }

        holder.btnPlaceOrder.setOnClickListener { 
            val orderItem = item.copy(
                quantity = 1,
                isSelected = true
            )
            onPlaceOrder(orderItem)
        }

        holder.btnFavorite.setImageResource(
            if (item.isFavorite) R.drawable.ic_favorite else R.drawable.ic_unfavorite
        )

        holder.btnFavorite.setOnClickListener {
            val position = holder.adapterPosition
            if (position != RecyclerView.NO_POSITION) {
                val currentItem = items[position]
                if (currentItem.isFavorite) {
                    currentItem.isFavorite = false
                    onRemoveFromFavorite(currentItem)
                    notifyItemChanged(position)
                } else {
                    currentItem.isFavorite = true
                    onAddToFavorite(currentItem)
                    notifyItemChanged(position)
                }
            }
        }
    }

    override fun getItemCount() = items.size

    fun updateItems(newItems: List<MenuItem>) {
        items = newItems
        notifyDataSetChanged()
    }
} 