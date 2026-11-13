package com.example.myapplication

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.bumptech.glide.Glide
import com.google.android.material.button.MaterialButton

class RecommendedMenuAdapter(
    private var menuItems: List<MenuItem> = emptyList(),
    private val onItemClick: (MenuItem) -> Unit
) : RecyclerView.Adapter<RecommendedMenuAdapter.ViewHolder>() {

    class ViewHolder(view: View) : RecyclerView.ViewHolder(view) {
        val menuImage: ImageView = view.findViewById(R.id.iv_menu_image)
        val menuName: TextView = view.findViewById(R.id.tv_menu_name)
        val menuPrice: TextView = view.findViewById(R.id.tv_menu_price)
        val orderButton: MaterialButton = view.findViewById(R.id.btn_order)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.recommended_menu_item, parent, false)
        return ViewHolder(view)
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        val menuItem = menuItems[position]
        
        holder.menuName.text = menuItem.name
        holder.menuPrice.text = "${menuItem.price}원"
        
        menuItem.imageUrl?.let { url ->
            Glide.with(holder.menuImage.context)
                .load(url)
                .centerCrop()
                .into(holder.menuImage)
        }

        holder.orderButton.setOnClickListener {
            onItemClick(menuItem)
        }
    }

    override fun getItemCount() = menuItems.size

    fun updateItems(newItems: List<MenuItem>) {
        menuItems = newItems
        notifyDataSetChanged()
    }
} 