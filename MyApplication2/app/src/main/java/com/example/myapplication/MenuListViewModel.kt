package com.example.myapplication

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

class MenuListViewModel : ViewModel() {
    private val _dialogTotalPrice = MutableStateFlow(0)
    val dialogTotalPrice: StateFlow<Int> = _dialogTotalPrice.asStateFlow()

    fun calculateDialogPrice(
        price: Int,
        selectedSize: String,
        shotQuantity: Int,
        hazelnutQuantity: Int,
        tapiocaQuantity: Int,
        decafQuantity: Int,
        vanillaQuantity: Int,
        whippingQuantity: Int,
        iceCreamQuantity: Int,
        quantity: Int
    ) {
        viewModelScope.launch(Dispatchers.Default) {
            val calculatedPrice = withContext(Dispatchers.Default) {
                var total = price
                
                if (selectedSize == "Large") {
                    total += MenuItem.LARGE_PRICE_ADDITION
                }
                
                total += (shotQuantity * MenuItem.SHOT_PRICE)
                total += (hazelnutQuantity * MenuItem.HAZELNUT_PRICE)
                total += (tapiocaQuantity * MenuItem.TAPIOCA_PRICE)
                total += (decafQuantity * MenuItem.DECAF_PRICE)
                total += (vanillaQuantity * MenuItem.VANILLA_PRICE)
                total += (whippingQuantity * MenuItem.WHIPPING_PRICE)
                total += (iceCreamQuantity * MenuItem.ICE_CREAM_PRICE)
                total * quantity
            }

            withContext(Dispatchers.Main) {
                _dialogTotalPrice.value = calculatedPrice
            }
        }
    }

    fun resetDialogPrice(basePrice: Int) {
        viewModelScope.launch {
            calculateDialogPrice(
                price = basePrice,
                selectedSize = "",
                shotQuantity = 0,
                hazelnutQuantity = 0,
                tapiocaQuantity = 0,
                decafQuantity = 0,
                vanillaQuantity = 0,
                whippingQuantity = 0,
                iceCreamQuantity = 0,
                quantity = 1
            )
        }
    }

    fun setDialogPrice(price: Int) {
        _dialogTotalPrice.value = price
    }
}
