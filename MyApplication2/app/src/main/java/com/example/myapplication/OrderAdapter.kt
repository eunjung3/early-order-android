package com.example.myapplication

import android.content.res.Resources
import android.graphics.drawable.Drawable
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.bumptech.glide.Glide
import com.bumptech.glide.load.DataSource
import com.bumptech.glide.load.engine.DiskCacheStrategy
import com.bumptech.glide.load.engine.GlideException
import com.bumptech.glide.request.RequestListener
import com.bumptech.glide.request.target.Target


class OrderAdapter(
    private var menuItems: List<MenuItem>,
    private val onItemClick: (MenuItem) -> Unit
) : RecyclerView.Adapter<OrderAdapter.ViewHolder>() {

    inner class ViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        init {
            itemView.setOnClickListener {
                val position = adapterPosition
                if (position != RecyclerView.NO_POSITION) {
                    onItemClick(menuItems[position])
                }
            }
        }
        val nameTextView: TextView = itemView.findViewById(R.id.tv_menu_name)
        val priceTextView: TextView = itemView.findViewById(R.id.tv_menu_price)
        val soldOutTextView: TextView = itemView.findViewById(R.id.tv_sold_out) // 품절 여부 텍스트
        val itemImage: ImageView = itemView.findViewById(R.id.img_menu)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val view = LayoutInflater.from(parent.context).inflate(R.layout.item_menu, parent, false)
        return ViewHolder(view)
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        val menu = menuItems[position]
        
        // 품절 상태에 따른 전체 아이템 투명도 설정
        holder.itemView.alpha = if (menu.isSoldOut) 0.5f else 1.0f
        
        // 메뉴 이름과 가격 설정
        holder.nameTextView.text = menu.name
        holder.priceTextView.text = "${menu.price}원"

        // 품절 여부 표시
        holder.soldOutTextView.visibility = if (menu.isSoldOut) {
            View.VISIBLE
        } else {
            View.GONE
        }

        // 품절 상태일 때 클릭 비활성화
        holder.itemView.isEnabled = !menu.isSoldOut

        // 이미지 로딩
        if (!menu.imageUrl.isNullOrEmpty()) {
            Glide.with(holder.itemView.context)
                .load(menu.imageUrl)
                .centerCrop()
                .into(holder.itemImage)
        } else {
            holder.itemImage.setImageResource(android.R.drawable.ic_menu_gallery)
        }
    }

    override fun getItemCount(): Int {
        return menuItems.size
    }

    // 메뉴 데이터를 업데이트하는 메서드
    fun updateMenu(newMenuList: List<MenuItem>) {
        menuItems = newMenuList
        notifyDataSetChanged()
    }
}
