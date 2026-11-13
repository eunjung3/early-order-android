package com.example.myapplication

import android.content.Context
import android.graphics.Color
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.CheckBox
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import kotlin.properties.Delegates
import com.bumptech.glide.Glide

class CartAdapter(
    private val cartItems: MutableList<MenuItem>,
    private val quantityChangeListener: (MenuItem, Int) -> Unit,
    private val itemRemoveListener: (MenuItem) -> Unit
) : RecyclerView.Adapter<CartAdapter.CartViewHolder>() {

    inner class CartViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        val itemName: TextView = itemView.findViewById(R.id.tv_item_name)
        val itemPrice: TextView = itemView.findViewById(R.id.tv_item_price)
        val itemImage: ImageView = itemView.findViewById(R.id.iv_item_image)
        val quantityTextView: TextView = itemView.findViewById(R.id.tv_quantity)
        val decreaseButton: ImageView = itemView.findViewById(R.id.btn_decrease_quantity)
        val increaseButton: ImageView = itemView.findViewById(R.id.btn_increase_quantity)
        val removeButton: ImageView = itemView.findViewById(R.id.btn_remove_item)
        val itemCheckBox: CheckBox = itemView.findViewById(R.id.cb_item_select)
        val menuTotalPrice: TextView = itemView.findViewById(R.id.tv_menu_price)
        val tvSelectedOptions: TextView = itemView.findViewById(R.id.tv_selected_options)
        val tvOptionsPrice: TextView = itemView.findViewById(R.id.tv_options_price)
        val layoutAdditionalOptions: LinearLayout = itemView.findViewById(R.id.layout_additional_options)

        init {
            decreaseButton.setOnClickListener {
                val position = adapterPosition
                if (position != RecyclerView.NO_POSITION) {
                    val item = cartItems[position]
                    if (item.quantity > 1) {
                        val newQuantity = item.quantity - 1
                        quantityChangeListener(item, newQuantity)
                    }
                }
            }

            increaseButton.setOnClickListener {
                val position = adapterPosition
                if (position != RecyclerView.NO_POSITION) {
                    val item = cartItems[position]
                    val newQuantity = item.quantity + 1
                    quantityChangeListener(item, newQuantity)
                }
            }

            removeButton.setOnClickListener {
                val position = adapterPosition
                if (position != RecyclerView.NO_POSITION) {
                    itemRemoveListener(cartItems[position])
                }
            }
        }

        fun bind(item: MenuItem) {
            itemName.text = item.name
            val basePrice = item.calculateBasePrice()
            itemPrice.text = "${item.price}원"
            
            itemView.alpha = if (item.isSoldOut) 0.5f else 1.0f
            
            val soldOutTextView = itemView.findViewById<TextView>(R.id.tv_sold_out)
            soldOutTextView.visibility = if (item.isSoldOut) View.VISIBLE else View.GONE
            
            decreaseButton.isEnabled = !item.isSoldOut
            increaseButton.isEnabled = !item.isSoldOut
            
            if (!item.imageUrl.isNullOrEmpty()) {
                Glide.with(itemView.context)
                    .load(item.imageUrl)
                    .centerCrop()
                    .into(itemImage)
            } else {
                itemImage.setImageResource(android.R.drawable.ic_menu_gallery)
            }
            
            quantityTextView.text = item.quantity.toString()
            menuTotalPrice.text = "${basePrice}원"

            val basicOptions = item.getBasicOptionsText()
            tvSelectedOptions.text = basicOptions
            tvOptionsPrice.text = item.getBasicOptionsPriceText()

            layoutAdditionalOptions.removeAllViews()
            
            item.getAdditionalOptions().forEach { (optionText, priceText) ->
                addOptionView(layoutAdditionalOptions, optionText, priceText)
            }

            itemCheckBox.setOnCheckedChangeListener(null)
            itemCheckBox.isChecked = item.isSelected
            itemCheckBox.setOnCheckedChangeListener { _, isChecked ->
                item.isSelected = isChecked
                quantityChangeListener(item, item.quantity)
            }
        }

        private fun addOptionView(container: LinearLayout, optionText: String, priceText: String) {
            val optionLayout = LinearLayout(container.context).apply {
                orientation = LinearLayout.HORIZONTAL
                layoutParams = LinearLayout.LayoutParams(
                    LinearLayout.LayoutParams.MATCH_PARENT,
                    LinearLayout.LayoutParams.WRAP_CONTENT
                )
            }
            val optionTextView = TextView(container.context).apply {
                text = optionText
                textSize = 13f
                setTextColor(Color.parseColor("#666666"))
                layoutParams = LinearLayout.LayoutParams(
                    0,
                    LinearLayout.LayoutParams.WRAP_CONTENT,
                    1f
                )
            }

            val priceTextView = TextView(container.context).apply {
                text = priceText
                textSize = 13f
                setTextColor(Color.parseColor("#666666"))
                layoutParams = LinearLayout.LayoutParams(
                    LinearLayout.LayoutParams.WRAP_CONTENT,
                    LinearLayout.LayoutParams.WRAP_CONTENT
                ).apply {
                    marginStart = 8.dpToPx(context)
                }
            }
            optionLayout.addView(optionTextView)
            optionLayout.addView(priceTextView)
            container.addView(optionLayout)
        }

        private fun Int.dpToPx(context: Context): Int {
            return (this * context.resources.displayMetrics.density).toInt()
        }
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): CartViewHolder {
        val view =
            LayoutInflater.from(parent.context).inflate(R.layout.item_cart, parent, false)
        return CartViewHolder(view)
    }

    override fun onBindViewHolder(holder: CartViewHolder, position: Int) {
        holder.bind(cartItems[position])
    }

    override fun getItemCount(): Int {
        return cartItems.size
    }

    private fun calculateTotalPrice(item: MenuItem): Int {
        return item.calculateTotalPrice()
    }
}