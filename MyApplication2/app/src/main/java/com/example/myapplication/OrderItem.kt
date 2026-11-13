package com.example.myapplication

data class OrderItem(
    val name: String,
    val price: Int,
    val quantity: Int,
    val options: String = "",
    val imageResource: Int = 0
)
