package com.example.myapplication

import android.content.Context
import android.content.res.ColorStateList
import android.graphics.Color
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.widget.Button
import android.widget.ImageButton
import android.widget.RadioGroup
import android.widget.TextView
import android.widget.Toast
import com.google.android.material.bottomsheet.BottomSheetDialog
import java.util.concurrent.atomic.AtomicInteger
import android.app.AlertDialog
import android.content.Intent
import android.widget.RadioButton
import retrofit2.Call
import retrofit2.Callback
import retrofit2.Response
import android.os.Handler
import android.os.Looper


class MenuOptionDialog(
    private val context: Context,
    private val menuItem: MenuItem,
    private val onAddToCart: (MenuItem) -> Unit,
    private val onPlaceOrder: (MenuItem) -> Unit,
    private val onAddToFavorite: (MenuItem) -> Boolean,
    private val onRemoveFromFavorite: (MenuItem) -> Unit,
    private val orderViewModel: OrderViewModel
) {
    private val bottomSheetDialog = BottomSheetDialog(context)
    private val view: View = LayoutInflater.from(context).inflate(R.layout.menu_options, null)
    private lateinit var btnFavorite: ImageButton
    
    // 옵션 수량을 추적하기 위한 변수들
    private var shotQuantity = AtomicInteger(0)
    private var hazelnutQuantity = AtomicInteger(0)
    private var tapiocaQuantity = AtomicInteger(0)
    private var decafQuantity = AtomicInteger(0)
    private var vanillaQuantity = AtomicInteger(0)
    private var whippingQuantity = AtomicInteger(0)
    private var iceCreamQuantity = AtomicInteger(0)
    private var selectedHotIceOption = ""
    private var selectedSize = ""
    private val viewModel = MenuListViewModel()
    private var quantity = AtomicInteger(1)

    companion object {
        private const val MAX_TOTAL_OPTIONS = 5  // 모든 메뉴의 최대 추가 옵션 수량
    }

    init {
        bottomSheetDialog.setContentView(view)
        btnFavorite = view.findViewById(R.id.btn_favorite)

        // 초기 상태 설정
        quantity = AtomicInteger(1)
        shotQuantity = AtomicInteger(0)
        hazelnutQuantity = AtomicInteger(0)
        tapiocaQuantity = AtomicInteger(0)
        decafQuantity = AtomicInteger(0)
        vanillaQuantity = AtomicInteger(0)
        whippingQuantity = AtomicInteger(0)
        iceCreamQuantity = AtomicInteger(0)
        selectedHotIceOption = ""
        selectedSize = ""
        menuItem.isFavorite = false

        // 다이얼로그 초기화
        initializeDialogComponents(view, menuItem, bottomSheetDialog)

        // 다이얼로그가 닫힐 때 내부 상태 초기화
        bottomSheetDialog.setOnDismissListener {
            shotQuantity.set(0)
            hazelnutQuantity.set(0)
            tapiocaQuantity.set(0)
            decafQuantity.set(0)
            vanillaQuantity.set(0)
            whippingQuantity.set(0)
            iceCreamQuantity.set(0)
            selectedHotIceOption = ""
            selectedSize = ""
            menuItem.isFavorite = false
            
            view.findViewById<RadioGroup>(R.id.rg_hot_and_ice)?.clearCheck()
            view.findViewById<RadioGroup>(R.id.rg_size)?.clearCheck()
            
            view.findViewById<TextView>(R.id.tv_shot_quantity)?.text = "0"
            view.findViewById<TextView>(R.id.tv_hazelnut_quantity)?.text = "0"
            view.findViewById<TextView>(R.id.tv_tapioca_quantity)?.text = "0"
            view.findViewById<TextView>(R.id.tv_decaf_quantity)?.text = "0"
            view.findViewById<TextView>(R.id.tv_vanilla_quantity)?.text = "0"
            view.findViewById<TextView>(R.id.tv_cream_quantity)?.text = "0"
            view.findViewById<TextView>(R.id.tv_icec_quantity)?.text = "0"
            
            view.findViewById<Button>(R.id.btn_add_to_cart)?.isEnabled = false
            view.findViewById<Button>(R.id.btn_place_order)?.isEnabled = false
            
            viewModel.resetDialogPrice(menuItem.price)
        }
    }

    private fun initializeDialogComponents(view: View, menuItem: MenuItem, bottomSheetDialog: BottomSheetDialog) {
        view.findViewById<TextView>(R.id.tv_option_title).text = menuItem.name
        
        btnFavorite.setImageResource(if (menuItem.isFavorite) R.drawable.ic_favorite else R.drawable.ic_unfavorite)
        btnFavorite.imageTintList = ColorStateList.valueOf(Color.parseColor("#533434"))
        
        btnFavorite.setOnClickListener {
            if (!isLoggedIn()) {
                showLoginRequiredDialog()
                return@setOnClickListener
            }
            
            if (menuItem.category == "BAKERY/DESSERT" || 
                (menuItem.category == "COFFEE" && menuItem.name in listOf("사케라또", "아포카토")) ||
                (selectedHotIceOption.isNotEmpty() && selectedSize.isNotEmpty())) {
                toggleFavorite(btnFavorite, menuItem)
            } else {
                Toast.makeText(context, "필수 옵션을 선택해주세요", Toast.LENGTH_SHORT).show()
            }
        }

        // TEA 카테고리 체크 추가
        if (menuItem.category == "BAKERY/DESSERT" || menuItem.category == "TEA") {
            // HOT/ICE 섹션은 TEA 카테고리일 때는 보이게 함
            view.findViewById<View>(R.id.container_hot_ice).visibility = 
                if (menuItem.category == "TEA") View.VISIBLE else View.GONE
            
            // 사이즈 섹션은 TEA 카테고리일 때는 보이게 함
            view.findViewById<View>(R.id.container_size).visibility = 
                if (menuItem.category == "TEA") View.VISIBLE else View.GONE
            
            // divider2도 TEA 카테고리일 때만 보이게 함
            view.findViewById<View>(R.id.divider2).visibility = 
                if (menuItem.category == "TEA") View.VISIBLE else View.GONE
            
            // 추가 옵션들은 TEA와 디저트 모두 숨김
            view.findViewById<View>(R.id.container_shots_and_syrups).visibility = View.GONE
        }

        // 디저트 카테고리 체크
        if (menuItem.category == "BAKERY/DESSERT") {
            // HOT/ICE 섹션 전체 숨기기
            view.findViewById<View>(R.id.container_hot_ice).visibility = View.GONE
            
            // 사이즈 섹션 전체 숨기기
            view.findViewById<View>(R.id.container_size).visibility = View.GONE
            
            // 추가 옵션들 숨기기
            view.findViewById<View>(R.id.container_shots_and_syrups).visibility = View.GONE

            // 수량 조절 버튼과 가격 표시는 바로 활성화
            setupQuantityControls(
                view = view,
                quantityTextViewId = R.id.tv_quantity,
                increaseButtonId = R.id.btn_increase_quantity,
                decreaseButtonId = R.id.btn_decrease_quantity,
                quantity = this.quantity,
                menuItem = menuItem,
                selectedHotIceOption = "",
                selectedSize = "",
                shotQuantity = AtomicInteger(0),
                hazelnutQuantity = AtomicInteger(0),
                tapiocaQuantity = AtomicInteger(0),
                decafQuantity = AtomicInteger(0),
                vanillaQuantity = AtomicInteger(0),
                whippingQuantity = AtomicInteger(0),
                iceCreamQuantity = AtomicInteger(0)
            )

            // 버튼들 바로 활성화
            val addToCartButton = view.findViewById<Button>(R.id.btn_add_to_cart)
            val placeOrderButton = view.findViewById<Button>(R.id.btn_place_order)

            addToCartButton.apply {
                isEnabled = true
                alpha = 1.0f
                setTextColor(Color.WHITE)
            }

            placeOrderButton.apply {
                isEnabled = true
                alpha = 1.0f
                setTextColor(Color.WHITE)
            }

            // 버튼 클릭 리스너 설정
            addToCartButton.setOnClickListener {
                if (!isLoggedIn()) {
                    showLoginRequiredDialog()
                    return@setOnClickListener
                }
                
                val updatedMenuItem = menuItem.copy(
                    quantity = quantity.get(),
                    hotIceOption = selectedHotIceOption,
                    size = selectedSize,
                    shotQuantity = shotQuantity.get(),
                    hazelnutQuantity = hazelnutQuantity.get(),
                    tapiocaQuantity = tapiocaQuantity.get(),
                    decafQuantity = decafQuantity.get(),
                    vanillaQuantity = vanillaQuantity.get(),
                    whippingQuantity = whippingQuantity.get(),
                    iceCreamQuantity = iceCreamQuantity.get(),
                    totalPrice = menuItem.calculateTotalPrice()
                )
                
                // 다이얼로그를 먼저 닫고
                bottomSheetDialog.dismiss()
                
                // 약간의 딜레이 후 Toast 메시지 표시 및 장바구니 추가
                Handler(Looper.getMainLooper()).postDelayed({
                    context?.let {
                        Toast.makeText(it, "장바구니에 추가되었습니다.", Toast.LENGTH_SHORT).show()
                        onAddToCart(updatedMenuItem)
                    }
                }, 100)
            }

            placeOrderButton.setOnClickListener {
                if (!isLoggedIn()) {
                    showLoginRequiredDialog()
                    return@setOnClickListener
                }
                
                val updatedMenuItem = menuItem.copy(
                    quantity = quantity.get(),
                    hotIceOption = selectedHotIceOption,
                    size = selectedSize,
                    shotQuantity = shotQuantity.get(),
                    hazelnutQuantity = hazelnutQuantity.get(),
                    tapiocaQuantity = tapiocaQuantity.get(),
                    decafQuantity = decafQuantity.get(),
                    vanillaQuantity = vanillaQuantity.get(),
                    whippingQuantity = whippingQuantity.get(),
                    iceCreamQuantity = iceCreamQuantity.get(),
                    totalPrice = menuItem.calculateTotalPrice()
                )
                
                onPlaceOrder(updatedMenuItem)
                bottomSheetDialog.dismiss()
            }

            // 초기 가격 표시
            view.findViewById<TextView>(R.id.tv_menu_totalprice).text = "${menuItem.price}원"
            
            return  // 디저트일 경우 여기서 함수 종료
        }

        // 기존 음료 옵션 관련 코드
        val rgHotAndIce: RadioGroup = view.findViewById(R.id.rg_hot_and_ice)
        var selectedHotIceOption = menuItem.hotIceOption

        // 사이즈 선택 (필수)
        val rgSize: RadioGroup = view.findViewById(R.id.rg_size)
        var selectedSize = menuItem.size

        // 모든 수량 조절 버튼들을 리스트에 추가 (메뉴 수량 버튼 포함)
        val allQuantityButtons = listOf(
            view.findViewById<ImageButton>(R.id.btn_increase_shot),
            view.findViewById<ImageButton>(R.id.btn_decrease_shot),
            view.findViewById<ImageButton>(R.id.btn_increase_hazelnut),
            view.findViewById<ImageButton>(R.id.btn_decrease_hazelnut),
            view.findViewById<ImageButton>(R.id.btn_increase_tapioca),
            view.findViewById<ImageButton>(R.id.btn_decrease_tapioca),
            view.findViewById<ImageButton>(R.id.btn_increase_decaf),
            view.findViewById<ImageButton>(R.id.btn_decrease_decaf),
            view.findViewById<ImageButton>(R.id.btn_increase_vanilla),
            view.findViewById<ImageButton>(R.id.btn_decrease_vanilla),
            view.findViewById<ImageButton>(R.id.btn_increase_cream),
            view.findViewById<ImageButton>(R.id.btn_decrease_cream),
            view.findViewById<ImageButton>(R.id.btn_increase_icec),
            view.findViewById<ImageButton>(R.id.btn_decrease_icec),
            view.findViewById<ImageButton>(R.id.btn_increase_quantity),
            view.findViewById<ImageButton>(R.id.btn_decrease_quantity)
        )

        // 초기에 모든 버튼 비활성화 및 시각적 표시
        allQuantityButtons.forEach { button ->
            button.isEnabled = false
            button.alpha = 0.5f
        }

        // 담기 버튼 초기 비활성화
        val addToCartButton = view.findViewById<Button>(R.id.btn_add_to_cart)
        val placeOrderButton = view.findViewById<Button>(R.id.btn_place_order)

        // 버튼 초기 상태 설정
        addToCartButton.apply {
            isEnabled = false
            alpha = 0.5f
            setTextColor(Color.WHITE)
        }

        placeOrderButton.apply {
            isEnabled = false
            alpha = 0.5f
            setTextColor(Color.WHITE)
        }

        // 가격 표시 TextView 초기화
        val menuPriceTextView = view.findViewById<TextView>(R.id.tv_menu_totalprice)
        viewModel.calculateDialogPrice(
            price = menuItem.price,
            selectedSize = selectedSize,
            shotQuantity = shotQuantity.get(),
            hazelnutQuantity = hazelnutQuantity.get(),
            tapiocaQuantity = tapiocaQuantity.get(),
            decafQuantity = decafQuantity.get(),
            vanillaQuantity = vanillaQuantity.get(),
            whippingQuantity = whippingQuantity.get(),
            iceCreamQuantity = iceCreamQuantity.get(),
            quantity = 1
        )

        fun checkAndEnableButtons() {
            val isHotIceSelected = selectedHotIceOption.isNotEmpty()
            val isSizeSelected = selectedSize.isNotEmpty()
            val buttonsEnabled = isHotIceSelected && isSizeSelected

            allQuantityButtons.forEach { button ->
                button.isEnabled = buttonsEnabled
                button.alpha = if (buttonsEnabled) 1.0f else 0.5f
            }

            addToCartButton.isEnabled = buttonsEnabled
            placeOrderButton.isEnabled = buttonsEnabled
            addToCartButton.alpha = if (buttonsEnabled) 1.0f else 0.5f
            placeOrderButton.alpha = if (buttonsEnabled) 1.0f else 0.5f
        }

        // RadioGroup 리스너 수정
        rgHotAndIce.setOnCheckedChangeListener { _, checkedId ->
            selectedHotIceOption = when (checkedId) {
                R.id.rb_hot -> "HOT"
                R.id.rb_ice -> "ICE"
                else -> ""
            }
            checkAndEnableButtons()
            updatePrice(menuItem, selectedHotIceOption, selectedSize, shotQuantity, hazelnutQuantity, tapiocaQuantity, decafQuantity, vanillaQuantity, whippingQuantity, iceCreamQuantity, quantity, view)
        }

        rgSize.setOnCheckedChangeListener { _, checkedId ->
            selectedSize = when (checkedId) {
                R.id.rb_regular -> "Regular"
                R.id.rb_large -> "Large"
                else -> ""
            }
            checkAndEnableButtons()
            updatePrice(menuItem, selectedHotIceOption, selectedSize, shotQuantity, hazelnutQuantity, tapiocaQuantity, decafQuantity, vanillaQuantity, whippingQuantity, iceCreamQuantity, quantity, view)
        }

        setupOptionQuantityControls(view, R.id.tv_shot_quantity, R.id.btn_increase_shot, R.id.btn_decrease_shot, shotQuantity, menuItem, selectedHotIceOption, selectedSize, shotQuantity, hazelnutQuantity, tapiocaQuantity, decafQuantity, vanillaQuantity, whippingQuantity, iceCreamQuantity, quantity)
        setupOptionQuantityControls(view, R.id.tv_hazelnut_quantity, R.id.btn_increase_hazelnut, R.id.btn_decrease_hazelnut, hazelnutQuantity, menuItem, selectedHotIceOption, selectedSize, shotQuantity, hazelnutQuantity, tapiocaQuantity, decafQuantity, vanillaQuantity, whippingQuantity, iceCreamQuantity, quantity)
        setupOptionQuantityControls(view, R.id.tv_tapioca_quantity, R.id.btn_increase_tapioca, R.id.btn_decrease_tapioca, tapiocaQuantity, menuItem, selectedHotIceOption, selectedSize, shotQuantity, hazelnutQuantity, tapiocaQuantity, decafQuantity, vanillaQuantity, whippingQuantity, iceCreamQuantity, quantity)
        setupOptionQuantityControls(view, R.id.tv_decaf_quantity, R.id.btn_increase_decaf, R.id.btn_decrease_decaf, decafQuantity, menuItem, selectedHotIceOption, selectedSize, shotQuantity, hazelnutQuantity, tapiocaQuantity, decafQuantity, vanillaQuantity, whippingQuantity, iceCreamQuantity, quantity)
        setupOptionQuantityControls(view, R.id.tv_vanilla_quantity, R.id.btn_increase_vanilla, R.id.btn_decrease_vanilla, vanillaQuantity, menuItem, selectedHotIceOption, selectedSize, shotQuantity, hazelnutQuantity, tapiocaQuantity, decafQuantity, vanillaQuantity, whippingQuantity, iceCreamQuantity, quantity)
        setupOptionQuantityControls(view, R.id.tv_cream_quantity, R.id.btn_increase_cream, R.id.btn_decrease_cream, whippingQuantity, menuItem, selectedHotIceOption, selectedSize, shotQuantity, hazelnutQuantity, tapiocaQuantity, decafQuantity, vanillaQuantity, whippingQuantity, iceCreamQuantity, quantity)
        setupOptionQuantityControls(view, R.id.tv_icec_quantity, R.id.btn_increase_icec, R.id.btn_decrease_icec, iceCreamQuantity, menuItem, selectedHotIceOption, selectedSize, shotQuantity, hazelnutQuantity, tapiocaQuantity, decafQuantity, vanillaQuantity, whippingQuantity, iceCreamQuantity, quantity)
        setupQuantityControls(view, R.id.tv_quantity, R.id.btn_increase_quantity, R.id.btn_decrease_quantity, quantity, menuItem, selectedHotIceOption, selectedSize, shotQuantity, hazelnutQuantity, tapiocaQuantity, decafQuantity, vanillaQuantity, whippingQuantity, iceCreamQuantity)

        addToCartButton.setOnClickListener {
            if (!isLoggedIn()) {
                showLoginRequiredDialog()
                return@setOnClickListener
            }
            
            val updatedMenuItem = menuItem.copy(
                quantity = quantity.get(),
                hotIceOption = selectedHotIceOption,
                size = selectedSize,
                shotQuantity = shotQuantity.get(),
                hazelnutQuantity = hazelnutQuantity.get(),
                tapiocaQuantity = tapiocaQuantity.get(),
                decafQuantity = decafQuantity.get(),
                vanillaQuantity = vanillaQuantity.get(),
                whippingQuantity = whippingQuantity.get(),
                iceCreamQuantity = iceCreamQuantity.get(),
                totalPrice = menuItem.calculateTotalPrice()
            )
            // 다이얼로그를 먼저 닫고
            bottomSheetDialog.dismiss()

            // 약간의 딜레이 후 Toast 메시지 표시 및 장바구니 추가
            Handler(Looper.getMainLooper()).postDelayed({
                context?.let {
                    Toast.makeText(it, "장바구니에 추가되었습니다.", Toast.LENGTH_SHORT).show()
                    onAddToCart(updatedMenuItem)
                }
            }, 100)
        }

        placeOrderButton.setOnClickListener {
            if (!isLoggedIn()) {
                showLoginRequiredDialog()
                return@setOnClickListener
            }
            
            val updatedMenuItem = menuItem.copy(
                quantity = quantity.get(),
                hotIceOption = selectedHotIceOption,
                size = selectedSize,
                shotQuantity = shotQuantity.get(),
                hazelnutQuantity = hazelnutQuantity.get(),
                tapiocaQuantity = tapiocaQuantity.get(),
                decafQuantity = decafQuantity.get(),
                vanillaQuantity = vanillaQuantity.get(),
                whippingQuantity = whippingQuantity.get(),
                iceCreamQuantity = iceCreamQuantity.get(),
                totalPrice = menuItem.calculateTotalPrice()
            )
            
            onPlaceOrder(updatedMenuItem)
            bottomSheetDialog.dismiss()
        }

        when (menuItem.category) {
            "SMOOTHE", "SHAKE/FRAPPE" -> {
                // 기존 SMOOTHIE, SHAKE/FRAPPE 처리 코드
                view.findViewById<RadioButton>(R.id.rb_hot).visibility = View.GONE
                view.findViewById<RadioButton>(R.id.rb_ice).isChecked = true
                selectedHotIceOption = "ICE"
                
                // 특정 추가옵션만 표시
                hideAllOptions(view)
                view.findViewById<View>(R.id.layout_cream_option).visibility = View.VISIBLE
                view.findViewById<View>(R.id.layout_tapioca_option).visibility = View.VISIBLE
                view.findViewById<View>(R.id.layout_icec_option).visibility = View.VISIBLE
            }
            "BEVERAGE" -> {
                // BEVERAGE 공통: HOT 옵션 숨기기, ICE 자동 선택
                view.findViewById<RadioButton>(R.id.rb_hot).visibility = View.GONE
                view.findViewById<RadioButton>(R.id.rb_ice).isChecked = true
                selectedHotIceOption = "ICE"
                
                // 모든 추가옵션 먼저 숨기기
                hideAllOptions(view)
                
                when (menuItem.name) {
                    "복숭아 아이스티" -> {
                        // 복숭아 아이스티: 샷, 타피오카 옵션만 표시
                        view.findViewById<View>(R.id.layout_shot_option).visibility = View.VISIBLE
                        view.findViewById<View>(R.id.layout_tapioca_option).visibility = View.VISIBLE
                    }
                    else -> {
                        // 나머지 BEVERAGE 메뉴: 타피오카 옵션만 표시
                        view.findViewById<View>(R.id.layout_tapioca_option).visibility = View.VISIBLE
                    }
                }
            }
            "LATTE" -> {
                // LATTE 카테고리 공통 설정: 특정 옵션들 숨기기
                view.findViewById<View>(R.id.layout_shot_option).visibility = View.GONE
                view.findViewById<View>(R.id.layout_decaf_option).visibility = View.GONE
                view.findViewById<View>(R.id.layout_vanilla_option).visibility = View.GONE
                view.findViewById<View>(R.id.layout_hazelnut_option).visibility = View.GONE

                when (menuItem.name) {
                    "딸기라떼", "블루베리라떼" -> {
                        // HOT 옵션 숨기기
                        view.findViewById<RadioButton>(R.id.rb_hot).visibility = View.GONE
                        view.findViewById<RadioButton>(R.id.rb_ice).isChecked = true
                        selectedHotIceOption = "ICE"
                        
                        // 모든 추가 옵션 표시 (공통으로 숨긴 것 외)
                        view.findViewById<View>(R.id.layout_tapioca_option).visibility = View.VISIBLE
                        view.findViewById<View>(R.id.layout_cream_option).visibility = View.VISIBLE
                        view.findViewById<View>(R.id.layout_icec_option).visibility = View.VISIBLE
                    }
                    "달고나라떼" -> {
                        // HOT 옵션 숨기기
                        view.findViewById<RadioButton>(R.id.rb_hot).visibility = View.GONE
                        view.findViewById<RadioButton>(R.id.rb_ice).isChecked = true
                        selectedHotIceOption = "ICE"
                        
                        // 타피오카 옵션만 표시
                        hideAllOptions(view)  // 모든 옵션 먼저 숨기기
                        view.findViewById<View>(R.id.layout_tapioca_option).visibility = View.VISIBLE
                    }
                    else -> {
                        // 다른 LATTE 메뉴들은 기본 옵션 유지
                        view.findViewById<View>(R.id.layout_tapioca_option).visibility = View.VISIBLE
                        view.findViewById<View>(R.id.layout_cream_option).visibility = View.VISIBLE
                        view.findViewById<View>(R.id.layout_icec_option).visibility = View.VISIBLE
                    }
                }
            }
            "COFFEE" -> {
                when (menuItem.name) {
                    "디카페인 콜드브루 아메리카노", "디카페인 콜드브루 라떼" -> {
                        // 지정된 추가 옵션만 표시
                        hideAllOptions(view)
                        view.findViewById<View>(R.id.layout_decaf_option).visibility = View.VISIBLE
                        view.findViewById<View>(R.id.layout_vanilla_option).visibility = View.VISIBLE
                        view.findViewById<View>(R.id.layout_hazelnut_option).visibility = View.VISIBLE
                        view.findViewById<View>(R.id.layout_tapioca_option).visibility = View.VISIBLE
                    }
                    "아메리카노" -> {
                        hideAllOptions(view)
                        view.findViewById<View>(R.id.layout_shot_option).visibility = View.VISIBLE
                        view.findViewById<View>(R.id.layout_hazelnut_option).visibility = View.VISIBLE
                        view.findViewById<View>(R.id.layout_tapioca_option).visibility = View.VISIBLE
                    }
                    "카페라떼" -> {
                        hideAllOptions(view)
                        view.findViewById<View>(R.id.layout_shot_option).visibility = View.VISIBLE
                        view.findViewById<View>(R.id.layout_hazelnut_option).visibility = View.VISIBLE
                    }
                    "카푸치노" -> {
                        hideAllOptions(view)
                        view.findViewById<View>(R.id.layout_shot_option).visibility = View.VISIBLE
                        view.findViewById<View>(R.id.layout_vanilla_option).visibility = View.VISIBLE
                        view.findViewById<View>(R.id.layout_hazelnut_option).visibility = View.VISIBLE
                    }
                    "바닐라라떼", "카라멜마끼아또", "카페모카", "민트모카", "화이트모카" -> {
                        hideAllOptions(view)
                        view.findViewById<View>(R.id.layout_shot_option).visibility = View.VISIBLE
                    }
                    "사케라또", "아포카토" -> {
                        // HOT/ICE와 사이즈 옵션 숨기기
                        view.findViewById<View>(R.id.container_hot_ice).visibility = View.GONE
                        view.findViewById<View>(R.id.container_size).visibility = View.GONE
                        
                        // 추가 옵션 설정
                        hideAllOptions(view)
                        view.findViewById<View>(R.id.layout_shot_option).visibility = View.VISIBLE
                        view.findViewById<View>(R.id.layout_icec_option).visibility = View.VISIBLE
                        
                        // 모든 수량 조절 버튼 활성화
                        allQuantityButtons.forEach { button ->
                            button.isEnabled = true
                            button.alpha = 1.0f
                        }
                        
                        // 수량 조절 버튼 활성화
                        setupQuantityControls(
                            view = view,
                            quantityTextViewId = R.id.tv_quantity,
                            increaseButtonId = R.id.btn_increase_quantity,
                            decreaseButtonId = R.id.btn_decrease_quantity,
                            quantity = this.quantity,  // 클래스 레벨의 quantity 사용
                            menuItem = menuItem,
                            selectedHotIceOption = "",
                            selectedSize = "",
                            shotQuantity = this.shotQuantity,        // 기존 인스턴스 사용
                            hazelnutQuantity = this.hazelnutQuantity,
                            tapiocaQuantity = this.tapiocaQuantity,
                            decafQuantity = this.decafQuantity,
                            vanillaQuantity = this.vanillaQuantity,
                            whippingQuantity = this.whippingQuantity,
                            iceCreamQuantity = this.iceCreamQuantity
                        )

                        // 버튼들 바로 활성화
                        val addToCartButton = view.findViewById<Button>(R.id.btn_add_to_cart)
                        val placeOrderButton = view.findViewById<Button>(R.id.btn_place_order)

                        addToCartButton.apply {
                            isEnabled = true
                            alpha = 1.0f
                            setTextColor(Color.WHITE)
                        }

                        placeOrderButton.apply {
                            isEnabled = true
                            alpha = 1.0f
                            setTextColor(Color.WHITE)
                        }

                        // 초기 가격 표시
                        view.findViewById<TextView>(R.id.tv_menu_totalprice).text = "${menuItem.price}원"
                    }
                }
            }
        }
    }

    private fun setupOptionQuantityControls(
        view: View,
        quantityTextViewId: Int,
        increaseButtonId: Int,
        decreaseButtonId: Int,
        optionQuantity: AtomicInteger,
        menuItem: MenuItem,
        selectedHotIceOption: String,
        selectedSize: String,
        shotQuantity: AtomicInteger,
        hazelnutQuantity: AtomicInteger,
        tapiocaQuantity: AtomicInteger,
        decafQuantity: AtomicInteger,
        vanillaQuantity: AtomicInteger,
        whippingQuantity: AtomicInteger,
        iceCreamQuantity: AtomicInteger,
        quantity: AtomicInteger
    ) {
        val quantityTextView: TextView = view.findViewById(quantityTextViewId)
        val increaseButton: ImageButton = view.findViewById(increaseButtonId)
        val decreaseButton: ImageButton = view.findViewById(decreaseButtonId)

        // 초기 수량 표시
        quantityTextView.text = optionQuantity.get().toString()

        // 클릭 이벤트에 디바운싱 적용
        var lastClickTime = 0L
        val clickDelay = 300L  // 300ms

        increaseButton.setOnClickListener {
            val currentTime = System.currentTimeMillis()
            if (currentTime - lastClickTime > clickDelay) {
                lastClickTime = currentTime
                
                if (getTotalOptionsCount() >= MAX_TOTAL_OPTIONS) {
                    Toast.makeText(context, 
                        "추가 옵션은 최대 ${MAX_TOTAL_OPTIONS}개까지 선택 가능합니다", 
                        Toast.LENGTH_SHORT
                    ).show()
                    return@setOnClickListener
                }
                
                optionQuantity.incrementAndGet()
                quantityTextView.text = optionQuantity.get().toString()
                
                // 여기를 수정: 새로운 quantity 인스턴스를 생성하지 않고 기존 quantity 사용
                updatePrice(
                    menuItem = menuItem,
                    selectedHotIceOption = this.selectedHotIceOption,
                    selectedSize = this.selectedSize,
                    shotQuantity = shotQuantity,
                    hazelnutQuantity = hazelnutQuantity,
                    tapiocaQuantity = tapiocaQuantity,
                    decafQuantity = decafQuantity,
                    vanillaQuantity = vanillaQuantity,
                    whippingQuantity = whippingQuantity,
                    iceCreamQuantity = iceCreamQuantity,
                    quantity = this.quantity,  // 클래스 레벨의 quantity 사용
                    view = view
                )
            }
        }

        decreaseButton.setOnClickListener {
            val currentTime = System.currentTimeMillis()
            if (currentTime - lastClickTime > clickDelay && optionQuantity.get() > 0) {
                lastClickTime = currentTime
                optionQuantity.decrementAndGet()
                quantityTextView.text = optionQuantity.get().toString()
                
                // 여기를 수정: 새로운 quantity 인스턴스를 생성하지 않고 기존 quantity 사용
                updatePrice(
                    menuItem = menuItem,
                    selectedHotIceOption = this.selectedHotIceOption,
                    selectedSize = this.selectedSize,
                    shotQuantity = shotQuantity,
                    hazelnutQuantity = hazelnutQuantity,
                    tapiocaQuantity = tapiocaQuantity,
                    decafQuantity = decafQuantity,
                    vanillaQuantity = vanillaQuantity,
                    whippingQuantity = whippingQuantity,
                    iceCreamQuantity = iceCreamQuantity,
                    quantity = this.quantity,  // 클래스 레벨의 quantity 사용
                    view = view
                )
            }
        }
    }

    private fun setupQuantityControls(
        view: View,
        quantityTextViewId: Int,
        increaseButtonId: Int,
        decreaseButtonId: Int,
        quantity: AtomicInteger,
        menuItem: MenuItem,
        selectedHotIceOption: String,
        selectedSize: String,
        shotQuantity: AtomicInteger,
        hazelnutQuantity: AtomicInteger,
        tapiocaQuantity: AtomicInteger,
        decafQuantity: AtomicInteger,
        vanillaQuantity: AtomicInteger,
        whippingQuantity: AtomicInteger,
        iceCreamQuantity: AtomicInteger
    ) {
        val quantityTextView: TextView = view.findViewById(quantityTextViewId)
        val increaseButton: ImageButton = view.findViewById(increaseButtonId)
        
        increaseButton.setOnClickListener {
            quantity.incrementAndGet()
            quantityTextView.text = quantity.get().toString()
            updatePrice(
                menuItem = menuItem,
                selectedHotIceOption = this.selectedHotIceOption,
                selectedSize = this.selectedSize,
                shotQuantity = shotQuantity,
                hazelnutQuantity = hazelnutQuantity,
                tapiocaQuantity = tapiocaQuantity,
                decafQuantity = decafQuantity,
                vanillaQuantity = vanillaQuantity,
                whippingQuantity = whippingQuantity,
                iceCreamQuantity = iceCreamQuantity,
                quantity = quantity,
                view = view
            )
        }

        val decreaseButton: ImageButton = view.findViewById(decreaseButtonId)
        decreaseButton.setOnClickListener {
            if (quantity.get() > 1) {
                quantity.decrementAndGet()
                quantityTextView.text = quantity.get().toString()
                updatePrice(
                    menuItem = menuItem,
                    selectedHotIceOption = this.selectedHotIceOption,
                    selectedSize = this.selectedSize,
                    shotQuantity = shotQuantity,
                    hazelnutQuantity = hazelnutQuantity,
                    tapiocaQuantity = tapiocaQuantity,
                    decafQuantity = decafQuantity,
                    vanillaQuantity = vanillaQuantity,
                    whippingQuantity = whippingQuantity,
                    iceCreamQuantity = iceCreamQuantity,
                    quantity = quantity,
                    view = view
                )
            }
        }
    }

    private fun updatePrice(
        menuItem: MenuItem,
        selectedHotIceOption: String,
        selectedSize: String,
        shotQuantity: AtomicInteger,
        hazelnutQuantity: AtomicInteger,
        tapiocaQuantity: AtomicInteger,
        decafQuantity: AtomicInteger,
        vanillaQuantity: AtomicInteger,
        whippingQuantity: AtomicInteger,
        iceCreamQuantity: AtomicInteger,
        quantity: AtomicInteger,
        view: View
    ) {
        // 현재 선택된 모든 옵션 상태를 로그로 확인
        Log.d("PriceUpdate", """
            HotIce: $selectedHotIceOption
            Size: $selectedSize
            Shot: ${shotQuantity.get()}
            Hazelnut: ${hazelnutQuantity.get()}
            Tapioca: ${tapiocaQuantity.get()}
            Decaf: ${decafQuantity.get()}
            Vanilla: ${vanillaQuantity.get()}
            Whipping: ${whippingQuantity.get()}
            IceCream: ${iceCreamQuantity.get()}
            Quantity: ${quantity.get()}
        """.trimIndent())

        // 클래스 레벨 변수 업데이트
        this.selectedHotIceOption = selectedHotIceOption
        this.selectedSize = selectedSize
        
        if (menuItem.name in listOf("사케라또", "아포카토")) {
            // 사케라또, 아포카토의 경우 직접 계산
            var total = menuItem.price * quantity.get()
            total += shotQuantity.get() * MenuItem.SHOT_PRICE * quantity.get()
            total += hazelnutQuantity.get() * MenuItem.HAZELNUT_PRICE * quantity.get()
            total += tapiocaQuantity.get() * MenuItem.TAPIOCA_PRICE * quantity.get()
            total += decafQuantity.get() * MenuItem.DECAF_PRICE * quantity.get()
            total += vanillaQuantity.get() * MenuItem.VANILLA_PRICE * quantity.get()
            total += whippingQuantity.get() * MenuItem.WHIPPING_PRICE * quantity.get()
            total += iceCreamQuantity.get() * MenuItem.ICE_CREAM_PRICE * quantity.get()
            
            view.findViewById<TextView>(R.id.tv_menu_totalprice).text = "${total}원"
        } else {
            // 다른 메뉴들은 기존 방식대로 처리
            val updatedMenuItem = menuItem.copy(
                hotIceOption = this.selectedHotIceOption,
                size = this.selectedSize,
                shotQuantity = shotQuantity.get(),
                hazelnutQuantity = hazelnutQuantity.get(),
                tapiocaQuantity = tapiocaQuantity.get(),
                decafQuantity = decafQuantity.get(),
                vanillaQuantity = vanillaQuantity.get(),
                whippingQuantity = whippingQuantity.get(),
                iceCreamQuantity = iceCreamQuantity.get(),
                quantity = quantity.get()
            )
            
            val totalPrice = updatedMenuItem.calculateTotalPrice()
            view.findViewById<TextView>(R.id.tv_menu_totalprice).text = "${totalPrice}원"
        }
    }

    fun show() {
        bottomSheetDialog.show()
    }

    private fun toggleFavorite(btnFavorite: ImageButton, menuItem: MenuItem) {
        if (!isLoggedIn()) {
            showLoginRequiredDialog()
            return
        }

        // 현재 선택된 옵션들로 업데이트된 MenuItem 생성
        val updatedMenuItem = menuItem.copy(
            hotIceOption = selectedHotIceOption,
            size = selectedSize,
            shotQuantity = shotQuantity.get(),
            hazelnutQuantity = hazelnutQuantity.get(),
            tapiocaQuantity = tapiocaQuantity.get(),
            decafQuantity = decafQuantity.get(),
            vanillaQuantity = vanillaQuantity.get(),
            whippingQuantity = whippingQuantity.get(),
            iceCreamQuantity = iceCreamQuantity.get()
        )

        Log.d("Favorite", "Attempting to toggle favorite for menu: ${menuItem.name}")
        Log.d("Favorite", """
            Current Options:
            - Hot/Ice: $selectedHotIceOption
            - Size: $selectedSize
            - Shot: ${shotQuantity.get()}
            - Hazelnut: ${hazelnutQuantity.get()}
            - Tapioca: ${tapiocaQuantity.get()}
            - Decaf: ${decafQuantity.get()}
            - Vanilla: ${vanillaQuantity.get()}
            - Whipping: ${whippingQuantity.get()}
            - IceCream: ${iceCreamQuantity.get()}
        """.trimIndent())

        val sharedPreferences = context.getSharedPreferences("auth", Context.MODE_PRIVATE)
        val token = sharedPreferences.getString("token", null)

        if (token != null) {
            // 즐겨찾기 추가하려고 할 때
            if (!menuItem.isFavorite) {
                Log.d("Favorite", "Checking for duplicates in favorites...")
                RetrofitClient.apiService.getFavorites("Bearer $token")
                    .enqueue(object : Callback<FavoriteResponse> {
                        override fun onResponse(call: Call<FavoriteResponse>, response: Response<FavoriteResponse>) {
                            if (response.isSuccessful && response.body()?.success == true) {
                                val favorites = response.body()?.data
                                Log.d("Favorite", "Current favorites count: ${favorites?.size}")
                                
                                // 각 즐겨찾기 항목 로깅
                                favorites?.forEach { favorite ->
                                    Log.d("Favorite", """
                                        Existing Favorite:
                                        - Menu: ${favorite.menuName}
                                        - Hot/Ice: ${favorite.hotIceOption}
                                        - Size: ${favorite.size}
                                        - Shot: ${favorite.shotQuantity}
                                        - Hazelnut: ${favorite.hazelnutQuantity}
                                        - Tapioca: ${favorite.tapiocaQuantity}
                                        - Decaf: ${favorite.decafQuantity}
                                        - Vanilla: ${favorite.vanillaQuantity}
                                        - Whipping: ${favorite.whippingQuantity}
                                        - IceCream: ${favorite.iceCreamQuantity}
                                    """.trimIndent())
                                }

                                val isDuplicate = favorites?.any { favorite ->
                                    val duplicateCheck = favorite.menuName == menuItem.name &&
                                        favorite.hotIceOption == updatedMenuItem.hotIceOption &&
                                        favorite.size == updatedMenuItem.size &&
                                        favorite.shotQuantity == updatedMenuItem.shotQuantity &&
                                        favorite.hazelnutQuantity == updatedMenuItem.hazelnutQuantity &&
                                        favorite.tapiocaQuantity == updatedMenuItem.tapiocaQuantity &&
                                        favorite.decafQuantity == updatedMenuItem.decafQuantity &&
                                        favorite.vanillaQuantity == updatedMenuItem.vanillaQuantity &&
                                        favorite.whippingQuantity == updatedMenuItem.whippingQuantity &&
                                        favorite.iceCreamQuantity == updatedMenuItem.iceCreamQuantity
                                    
                                    Log.d("Favorite", "Duplicate check result: $duplicateCheck for ${favorite.menuName}")
                                    duplicateCheck
                                } ?: false

                                Log.d("Favorite", "Is duplicate: $isDuplicate")

                                if (isDuplicate) {
                                    Log.d("Favorite", "Duplicate found, not adding to favorites")
                                    menuItem.isFavorite = true
                                    btnFavorite.setImageResource(R.drawable.ic_favorite)
                                    Toast.makeText(context, "이미 즐겨찾기에 추가된 메뉴입니다.", Toast.LENGTH_SHORT).show()
                                } else {
                                    Log.d("Favorite", "No duplicate found, proceeding with favorite addition")
                                    updateFavoriteStatus(updatedMenuItem, token)
                                }
                            }
                        }

                        override fun onFailure(call: Call<FavoriteResponse>, t: Throwable) {
                            Log.e("Favorite", "Network error while checking favorites", t)
                            Toast.makeText(context, "네트워크 오류가 발생했습니다.", Toast.LENGTH_SHORT).show()
                        }
                    })
            } else {
                Log.d("Favorite", "Removing from favorites")
                updateFavoriteStatus(updatedMenuItem, token)
            }
        }
    }

    private fun updateFavoriteStatus(menuItem: MenuItem, token: String) {
        // 현재 선택된 옵션들을 포함하여 요청 생성
        val request = FavoriteUpdateRequest(
            menuName = menuItem.name,
            isFavorite = !menuItem.isFavorite,
            hotIceOption = menuItem.hotIceOption,  // updatedMenuItem의 값 사용
            size = menuItem.size,                  // updatedMenuItem의 값 사용
            shotQuantity = menuItem.shotQuantity,
            hazelnutQuantity = menuItem.hazelnutQuantity,
            tapiocaQuantity = menuItem.tapiocaQuantity,
            decafQuantity = menuItem.decafQuantity,
            vanillaQuantity = menuItem.vanillaQuantity,
            whippingQuantity = menuItem.whippingQuantity,
            iceCreamQuantity = menuItem.iceCreamQuantity,
            totalPrice = calculateTotalPrice(),   // 현재 옵션들이 적용된 총 가격 계산
            price = menuItem.price,
            imageUrl = menuItem.imageUrl
        )

        Log.d("Favorite", """
            Sending favorite update request:
            - Menu: ${request.menuName}
            - IsFavorite: ${request.isFavorite}
            - Hot/Ice: ${request.hotIceOption}
            - Size: ${request.size}
            - Shot: ${request.shotQuantity}
            - Hazelnut: ${request.hazelnutQuantity}
            - Tapioca: ${request.tapiocaQuantity}
            - Decaf: ${request.decafQuantity}
            - Vanilla: ${request.vanillaQuantity}
            - Whipping: ${request.whippingQuantity}
            - IceCream: ${request.iceCreamQuantity}
        """.trimIndent())

        RetrofitClient.apiService.updateFavoriteStatus("Bearer $token", request)
            .enqueue(object : Callback<ApiResponse<String>> {
                override fun onResponse(call: Call<ApiResponse<String>>, response: Response<ApiResponse<String>>) {
                    Log.d("Favorite", "Update response: ${response.isSuccessful}, Body: ${response.body()}")
                    if (response.isSuccessful && response.body()?.success == true) {
                        menuItem.apply {
                            isFavorite = !isFavorite
                            // 선택된 옵션들을 MenuItem에 저장
                            hotIceOption = menuItem.hotIceOption
                            size = menuItem.size
                            shotQuantity = this@MenuOptionDialog.shotQuantity.get()
                            hazelnutQuantity = this@MenuOptionDialog.hazelnutQuantity.get()
                            tapiocaQuantity = this@MenuOptionDialog.tapiocaQuantity.get()
                            decafQuantity = this@MenuOptionDialog.decafQuantity.get()
                            vanillaQuantity = this@MenuOptionDialog.vanillaQuantity.get()
                            whippingQuantity = this@MenuOptionDialog.whippingQuantity.get()
                            iceCreamQuantity = this@MenuOptionDialog.iceCreamQuantity.get()
                        }
                        
                        btnFavorite.setImageResource(
                            if (menuItem.isFavorite) R.drawable.ic_favorite 
                            else R.drawable.ic_unfavorite
                        )
                        
                        if (menuItem.isFavorite) {
                            onAddToFavorite(menuItem)
                            Toast.makeText(context, "즐겨찾기에 추가되었습니다.", Toast.LENGTH_SHORT).show()
                        } else {
                            onRemoveFromFavorite(menuItem)
                            Toast.makeText(context, "즐겨찾기가 해제되었습니다.", Toast.LENGTH_SHORT).show()
                        }
                    }
                }

                override fun onFailure(call: Call<ApiResponse<String>>, t: Throwable) {
                    Toast.makeText(context, "네트워크 오류가 발생했습니다.", Toast.LENGTH_SHORT).show()
                }
            })
    }

    private fun isLoggedIn(): Boolean {
        val sharedPreferences = context.getSharedPreferences("auth", Context.MODE_PRIVATE)
        val token = sharedPreferences.getString("token", null)
        return !token.isNullOrEmpty()
    }

    private fun showLoginRequiredDialog() {
        val dialog = AlertDialog.Builder(context)
            .setView(R.layout.dialog_login_required)
            .create()

        dialog.show()

        dialog.findViewById<Button>(R.id.btn_cancel)?.setOnClickListener {
            dialog.dismiss()
        }

        dialog.findViewById<Button>(R.id.btn_login)?.setOnClickListener {
            // LoginActivity로 이동
            val intent = Intent(context, LoginActivity::class.java)
            context.startActivity(intent)
            dialog.dismiss()
            bottomSheetDialog.dismiss()
        }
    }

    // 총 가격 계산 메서드 추가
    private fun calculateTotalPrice(): Int {
        var total = menuItem.price
        
        if (menuItem.name in listOf("사케라또", "아포카토")) {
            // 옵션 가격 계산 (다른 메뉴들과 동일한 방식)
            total += shotQuantity.get() * MenuItem.SHOT_PRICE
            total += hazelnutQuantity.get() * MenuItem.HAZELNUT_PRICE
            total += tapiocaQuantity.get() * MenuItem.TAPIOCA_PRICE
            total += decafQuantity.get() * MenuItem.DECAF_PRICE
            total += vanillaQuantity.get() * MenuItem.VANILLA_PRICE
            total += whippingQuantity.get() * MenuItem.WHIPPING_PRICE
            total += iceCreamQuantity.get() * MenuItem.ICE_CREAM_PRICE
            // 마지막에 전체 수량 곱하기
            total *= quantity.get()
        } else {
            // 다른 메뉴들은 기존 방식대로 계산
            if (selectedSize == "Large") {
                total += MenuItem.LARGE_PRICE_ADDITION
            }
            
            total += shotQuantity.get() * MenuItem.SHOT_PRICE
            total += hazelnutQuantity.get() * MenuItem.HAZELNUT_PRICE
            total += tapiocaQuantity.get() * MenuItem.TAPIOCA_PRICE
            total += decafQuantity.get() * MenuItem.DECAF_PRICE
            total += vanillaQuantity.get() * MenuItem.VANILLA_PRICE
            total += whippingQuantity.get() * MenuItem.WHIPPING_PRICE
            total += iceCreamQuantity.get() * MenuItem.ICE_CREAM_PRICE
            total *= quantity.get()
        }
        
        return total
    }

    // 모든 추가옵션을 숨기는 헬퍼 함수
    private fun hideAllOptions(view: View) {
        view.findViewById<View>(R.id.layout_shot_option).visibility = View.GONE
        view.findViewById<View>(R.id.layout_hazelnut_option).visibility = View.GONE
        view.findViewById<View>(R.id.layout_tapioca_option).visibility = View.GONE
        view.findViewById<View>(R.id.layout_decaf_option).visibility = View.GONE
        view.findViewById<View>(R.id.layout_vanilla_option).visibility = View.GONE
        view.findViewById<View>(R.id.layout_cream_option).visibility = View.GONE
        view.findViewById<View>(R.id.layout_icec_option).visibility = View.GONE
    }

    // 필수 옵션 체크 함수
    private fun checkRequiredOptions(view: View) {
        val hotIceSelected = view.findViewById<RadioGroup>(R.id.rg_hot_and_ice)?.checkedRadioButtonId != -1
        val sizeSelected = view.findViewById<RadioGroup>(R.id.rg_size)?.checkedRadioButtonId != -1
        
        val addToCartButton = view.findViewById<Button>(R.id.btn_add_to_cart)
        val placeOrderButton = view.findViewById<Button>(R.id.btn_place_order)
        
        if (hotIceSelected && sizeSelected) {
            addToCartButton?.isEnabled = true
            addToCartButton?.alpha = 1.0f
            placeOrderButton?.isEnabled = true
            placeOrderButton?.alpha = 1.0f
        } else {
            addToCartButton?.isEnabled = false
            addToCartButton?.alpha = 0.5f
            placeOrderButton?.isEnabled = false
            placeOrderButton?.alpha = 0.5f
        }
    }

    private fun getTotalOptionsCount(): Int {
        return shotQuantity.get() +
               hazelnutQuantity.get() +
               tapiocaQuantity.get() +
               decafQuantity.get() +
               vanillaQuantity.get() +
               whippingQuantity.get() +
               iceCreamQuantity.get()
    }

    private fun checkDuplicate(favorites: List<MenuItem>, newItem: MenuItem): Boolean {
        return favorites.any { favorite ->
            favorite.name == newItem.name &&
            favorite.hotIceOption == newItem.hotIceOption &&
            favorite.size == newItem.size &&
            favorite.shotQuantity == newItem.shotQuantity &&
            favorite.hazelnutQuantity == newItem.hazelnutQuantity &&
            favorite.tapiocaQuantity == newItem.tapiocaQuantity &&
            favorite.decafQuantity == newItem.decafQuantity &&
            favorite.vanillaQuantity == newItem.vanillaQuantity &&
            favorite.whippingQuantity == newItem.whippingQuantity &&
            favorite.iceCreamQuantity == newItem.iceCreamQuantity
        }
    }
}
