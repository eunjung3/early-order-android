package com.example.myapplication

import android.os.Parcelable
import kotlinx.parcelize.Parcelize
import android.util.Log

@Parcelize
data class MenuItem(
    val name: String,
    val price: Int,
    val imageResource: Int = 0,
    val imageUrl: String? = null,
    val isSoldOut: Boolean,
    var isSelected: Boolean = false,
    var quantity: Int = 1,
    var totalPrice: Int = price,
    var hotIceOption: String = "",
    var size: String = "",
    var shotQuantity: Int = 0,
    var hazelnutQuantity: Int = 0,
    var tapiocaQuantity: Int = 0,
    var selectedOptions: String = "",
    val category: String = "COFFEE",  // 기본값은 COFFEE로 설정
    var isFavorite: Boolean = false,   // 즐겨찾기 상태 추가
    var decafQuantity: Int = 0,
    var vanillaQuantity: Int = 0,
    var whippingQuantity: Int = 0,
    var iceCreamQuantity: Int = 0
) : Parcelable {
    companion object {
        const val LARGE_PRICE_ADDITION = 1000
        const val SHOT_PRICE = 500
        const val DECAF_PRICE = 800
        const val VANILLA_PRICE = 500
        const val HAZELNUT_PRICE = 500
        const val WHIPPING_PRICE = 800
        const val TAPIOCA_PRICE = 1000
        const val ICE_CREAM_PRICE = 1500

        fun fromServerResponse(response: MenuItemResponse): MenuItem {
            return MenuItem(
                name = decodeUnicode(response.name),
                price = response.price,
                imageUrl = response.image_url,
                isSoldOut = !response.is_available,
                category = response.category,
                quantity = 1,
                isSelected = false,
                totalPrice = response.price
            )
        }
    }

    fun calculateBasePrice(): Int {
        var basePrice = price
        if (size == "Large") basePrice += LARGE_PRICE_ADDITION
        basePrice += shotQuantity * SHOT_PRICE
        basePrice += hazelnutQuantity * HAZELNUT_PRICE
        basePrice += tapiocaQuantity * TAPIOCA_PRICE
        basePrice += decafQuantity * DECAF_PRICE
        basePrice += vanillaQuantity * VANILLA_PRICE
        basePrice += whippingQuantity * WHIPPING_PRICE
        basePrice += iceCreamQuantity * ICE_CREAM_PRICE
        return basePrice
    }

    fun hasSameOptionsAs(other: MenuItem): Boolean {
        return name == other.name &&
                hotIceOption == other.hotIceOption &&
                size == other.size &&
                shotQuantity == other.shotQuantity &&
                hazelnutQuantity == other.hazelnutQuantity &&
                tapiocaQuantity == other.tapiocaQuantity &&
                decafQuantity == other.decafQuantity &&
                vanillaQuantity == other.vanillaQuantity &&
                whippingQuantity == other.whippingQuantity &&
                iceCreamQuantity == other.iceCreamQuantity
    }

    fun getOptionsText(): String {
        val basicOptions = StringBuilder()
        val additionalOptions = StringBuilder()

        // 기본 옵션 (HOT/ICE, 사이즈)
        if (hotIceOption.isNotEmpty()) {
            basicOptions.append(hotIceOption)
        }

        if (size.isNotEmpty()) {
            if (basicOptions.isNotEmpty()) basicOptions.append(" / ")
            basicOptions.append(size)
        }

        // 추가 옵션
        if (shotQuantity > 0) {
            if (additionalOptions.isNotEmpty()) additionalOptions.append(" / ")
            additionalOptions.append("샷 ${shotQuantity}개")
        }

        if (hazelnutQuantity > 0) {
            if (additionalOptions.isNotEmpty()) additionalOptions.append(" / ")
            additionalOptions.append("헤이즐럿 시럽 ${hazelnutQuantity}개")
        }

        if (tapiocaQuantity > 0) {
            if (additionalOptions.isNotEmpty()) additionalOptions.append(" / ")
            additionalOptions.append("타피오카 펄 ${tapiocaQuantity}개")
        }

        if (decafQuantity > 0) {
            if (additionalOptions.isNotEmpty()) additionalOptions.append(" / ")
            additionalOptions.append("디카페인 샷${decafQuantity}개")
        }

        if (vanillaQuantity > 0) {
            if (additionalOptions.isNotEmpty()) additionalOptions.append(" / ")
            additionalOptions.append("바닐라 시럽 ${vanillaQuantity}개")
        }

        if (whippingQuantity > 0) {
            if (additionalOptions.isNotEmpty()) additionalOptions.append(" / ")
            additionalOptions.append("휘핑크림 ${whippingQuantity}개")
        }

        if (iceCreamQuantity > 0) {
            if (additionalOptions.isNotEmpty()) additionalOptions.append(" / ")
            additionalOptions.append("아이스크림 ${iceCreamQuantity}개")
        }

        return buildString {
            append(basicOptions)
            if (basicOptions.isNotEmpty() && additionalOptions.isNotEmpty()) {
                append(" / ")
            }
            append(additionalOptions)
        }
    }

    fun calculateTotalPrice(): Int {
        var totalPrice = price
        
        if (name in listOf("사케라또", "아포카토")) {
            // 옵션 가격 계산 (다른 메뉴들과 동일한 방식)
            totalPrice += shotQuantity * SHOT_PRICE
            totalPrice += hazelnutQuantity * HAZELNUT_PRICE
            totalPrice += tapiocaQuantity * TAPIOCA_PRICE
            totalPrice += decafQuantity * DECAF_PRICE
            totalPrice += vanillaQuantity * VANILLA_PRICE
            totalPrice += whippingQuantity * WHIPPING_PRICE
            totalPrice += iceCreamQuantity * ICE_CREAM_PRICE
            // 마지막에 전체 수량 곱하기
            totalPrice *= quantity
        } else {
            // 다른 메뉴들은 기존 방식대로 계산
            if (size == "Large") totalPrice += LARGE_PRICE_ADDITION
            totalPrice += shotQuantity * SHOT_PRICE
            totalPrice += hazelnutQuantity * HAZELNUT_PRICE
            totalPrice += tapiocaQuantity * TAPIOCA_PRICE
            totalPrice += decafQuantity * DECAF_PRICE
            totalPrice += vanillaQuantity * VANILLA_PRICE
            totalPrice += whippingQuantity * WHIPPING_PRICE
            totalPrice += iceCreamQuantity * ICE_CREAM_PRICE
            totalPrice *= quantity
        }
        
        return totalPrice
    }

    fun getBasicOptionsText(): String {
        return buildString {
            if (hotIceOption.isNotEmpty()) append(hotIceOption)
            if (size.isNotEmpty()) {
                if (isNotEmpty()) append(" / ")
                append(size)
            }
        }
    }

    fun getBasicOptionsPriceText(): String {
        val sizePrice = if (size == "Large") LARGE_PRICE_ADDITION else 0
        return if (sizePrice > 0) "+${sizePrice}원" else ""
    }

    fun getAdditionalOptions(): List<Pair<String, String>> {
        val options = mutableListOf<Pair<String, String>>()
        
        if (shotQuantity > 0) {
            options.add("샷 ${shotQuantity}" to "+${shotQuantity * SHOT_PRICE}원")
        }
        if (hazelnutQuantity > 0) {
            options.add("헤이즐럿 시럽 ${hazelnutQuantity}" to "+${hazelnutQuantity * HAZELNUT_PRICE}원")
        }
        if (tapiocaQuantity > 0) {
            options.add("타피오카 펄 ${tapiocaQuantity}" to "+${tapiocaQuantity * TAPIOCA_PRICE}원")
        }
        if (decafQuantity > 0) {
            options.add("디카페인 샷 ${decafQuantity}" to "+${decafQuantity * DECAF_PRICE}원")
        }
        if (vanillaQuantity > 0) {
            options.add("바닐라 시럽 ${vanillaQuantity}" to "+${vanillaQuantity * VANILLA_PRICE}원")
        }
        if (whippingQuantity > 0) {
            options.add("휘핑크림 ${whippingQuantity}" to "+${whippingQuantity * WHIPPING_PRICE}원")
        }
        if (iceCreamQuantity > 0) {
            options.add("아이스크림 ${iceCreamQuantity}" to "+${iceCreamQuantity * ICE_CREAM_PRICE}원")
        }
        
        return options
    }
}

private fun decodeUnicode(str: String): String {
    return try {
        String(str.toByteArray(Charsets.UTF_8), Charsets.UTF_8)
    } catch (e: Exception) {
        Log.e("MenuItem", "디코딩 실패: $str", e)
        str
    }
}
