package com.example.myapplication

import android.content.Context
import androidx.appcompat.app.AppCompatActivity
import com.google.gson.Gson

class CartRepository(private val context: Context) {
    private val sharedPreferences = context.getSharedPreferences("Cart", AppCompatActivity.MODE_PRIVATE)

    fun saveItems(items: List<MenuItem>) {
        val json = Gson().toJson(items)
        sharedPreferences.edit().putString("cart_items", json).apply()
    }

    fun loadItems(): List<MenuItem> {
        val json = sharedPreferences.getString("cart_items", null)
        return if (json != null) {
            try {
                Gson().fromJson(json, Array<MenuItem>::class.java).toList()
            } catch (e: Exception) {
                e.printStackTrace()
                emptyList()
            }
        } else {
            emptyList()
        }
    }
}