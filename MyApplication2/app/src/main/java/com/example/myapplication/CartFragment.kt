package com.example.myapplication

import android.content.Context
import android.content.SharedPreferences
import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.CheckBox
import android.widget.ImageButton
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.fragment.app.Fragment
import androidx.fragment.app.activityViewModels
import androidx.fragment.app.viewModels
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.google.android.material.bottomnavigation.BottomNavigationView
import com.google.gson.Gson
import kotlinx.coroutines.launch
import kotlinx.coroutines.delay

class CartFragment : Fragment() {

    private lateinit var cartRecyclerView: RecyclerView
    private lateinit var totalPriceTextView: TextView
    private lateinit var selectedCountTextView: TextView
    private lateinit var cartAdapter: CartAdapter
    private val cartItems = mutableListOf<MenuItem>()
    private lateinit var backButton: ImageButton

    private val cartViewModel: CartViewModel by activityViewModels()
    private val orderViewModel: OrderViewModel by viewModels()
//    private lateinit var binding: FragmentCartBinding

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        val view = inflater.inflate(R.layout.fragment_cart, container, false)

        // RecyclerView와 텍스트 뷰, 버튼 초기화
        cartRecyclerView = view.findViewById(R.id.recyclerView_cart)
        totalPriceTextView = view.findViewById(R.id.tv_total_price)
        selectedCountTextView = view.findViewById(R.id.tv_selected_count)
        backButton = view.findViewById(R.id.btn_back)

        // RecyclerView 설정
        cartRecyclerView.layoutManager = LinearLayoutManager(context)
        cartAdapter = CartAdapter(cartItems, { item, quantity ->
            item.quantity = quantity
            cartViewModel.updateItemQuantity(item, quantity)
            cartViewModel.updateTotalPriceAndCount()
        }, { item ->
            removeItemFromCart(item)
        })
        cartRecyclerView.adapter = cartAdapter

        // 메뉴 데이터 주기적 업데이트 및 품절 상태 동기화
        viewLifecycleOwner.lifecycleScope.launch {
            viewLifecycleOwner.repeatOnLifecycle(Lifecycle.State.STARTED) {
                while (true) {
                    orderViewModel.loadMenuData() // 메뉴 데이터 새로고침
                    
                    // 메뉴 데이터와 장바구니 아이템의 품절 상태 동기화
                    orderViewModel.menuData.value.flatten().let { menuItems ->
                        cartItems.forEach { cartItem ->
                            menuItems.find { it.name == cartItem.name }?.let { menuItem ->
                                if (cartItem.isSoldOut != menuItem.isSoldOut) {
                                    cartAdapter.notifyDataSetChanged()
                                }
                            }
                        }
                    }
                    delay(5000) // 5초마다 새로고침
                }
            }
        }

        // CartViewModel에서 데이터 로드
        cartViewModel.cartItems.observe(viewLifecycleOwner) { items ->
            Log.d("CartFragment", "장바구니 데이터 업데이트 - 전체 아이템 수: ${items.size}")
            
            // 현재 메뉴 데이터에서 품절 상태 확인
            orderViewModel.menuData.value.flatten().let { menuItems ->
                items.forEach { cartItem ->
                    // 품절 상태 확인 및 업데이트
                    menuItems.find { it.name == cartItem.name }?.let { menuItem ->
                        if (cartItem.isSoldOut != menuItem.isSoldOut) {
                            cartItem.copy(isSoldOut = menuItem.isSoldOut).also { updatedItem ->
                                val index = cartItems.indexOf(cartItem)
                                if (index != -1) {
                                    cartItems[index] = updatedItem
                                    cartAdapter.notifyItemChanged(index)
                                }
                            }
                        }
                    }
                }
            }
            
            cartItems.clear()
            cartItems.addAll(items)
            cartAdapter.notifyDataSetChanged()
            cartViewModel.updateTotalPriceAndCount()

            // 장바구니 비어있는지 체크
            val emptyLayout = view.findViewById<View>(R.id.layout_empty_cart)
            val selectLayout = view.findViewById<View>(R.id.select)
            val recyclerView = view.findViewById<View>(R.id.recyclerView_cart)
            val bottomLayout = view.findViewById<View>(R.id.layout_bottom)

            if (items.isEmpty()) {
                emptyLayout.visibility = View.VISIBLE
                selectLayout.visibility = View.GONE
                recyclerView.visibility = View.GONE
                bottomLayout.visibility = View.GONE
            } else {
                emptyLayout.visibility = View.GONE
                selectLayout.visibility = View.VISIBLE
                recyclerView.visibility = View.VISIBLE
                bottomLayout.visibility = View.VISIBLE
            }
        }

        val selectAllCheckBox: CheckBox = view.findViewById(R.id.checkbox_select_all)
        val deleteSelectedButton: Button = view.findViewById(R.id.btn_delete_selected)

        // 전체선택 체크박스 상태 관찰
        cartViewModel.isAllSelected.observe(viewLifecycleOwner) { isAllSelected ->
            selectAllCheckBox.setOnCheckedChangeListener(null)
            selectAllCheckBox.isChecked = isAllSelected
            selectAllCheckBox.setOnCheckedChangeListener { _, isChecked ->
                cartViewModel.updateAllItemsSelection(isChecked)
            }
        }

        deleteSelectedButton.setOnClickListener {
            cartViewModel.deleteSelectedItems()
        }

        backButton.setOnClickListener {
            activity?.onBackPressedDispatcher?.onBackPressed()
        }

        val checkoutButton: Button = view.findViewById(R.id.btn_checkout)
        checkoutButton.setOnClickListener {
            val selectedItems = cartItems.filter { it.isSelected }
            Log.d("CartFragment", "결제 시도 - 전체 장바구니 아이템 수: ${cartItems.size}")
            Log.d("CartFragment", "선택된 아이템 수: ${selectedItems.size}")
            Log.d("CartFragment", "선택된 아이템 목록: ${selectedItems.map { "${it.name} (${it.category}, 선택됨: ${it.isSelected})" }}")
            Log.d("CartFragment", "선택되지 않은 아이템 목록: ${cartItems.filter { !it.isSelected }.map { "${it.name} (${it.category}, 선택됨: ${it.isSelected})" }}")
            
            if (selectedItems.isEmpty()) {
                Toast.makeText(context, "주문할 메뉴를 선택해주세요", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }

            // 품절된 메뉴가 있는지 확인
            val soldOutItems = selectedItems.filter { it.isSoldOut }
            if (soldOutItems.isNotEmpty()) {
                Toast.makeText(context, "품절된 메뉴가 포함되어 있습니다. 다른 메뉴를 선택해주세요.", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }

            val totalAmount = selectedItems.sumOf { it.calculateTotalPrice() }
            Log.d("CartFragment", "결제 총액: $totalAmount")
            
            val checkoutFragment = CheckoutFragment.newInstance(selectedItems).apply {
                isFromCart = true
                Log.d("CartFragment", "CheckoutFragment isFromCart 설정: $isFromCart")
            }
            parentFragmentManager.beginTransaction()
                .replace(R.id.fragment_container, checkoutFragment)
                .addToBackStack(null)
                .commit()
        }

        cartViewModel.totalPrice.observe(viewLifecycleOwner) { price ->
            totalPriceTextView.text = "총 ${price}원"
        }

        cartViewModel.selectedCount.observe(viewLifecycleOwner) { count ->
            selectedCountTextView.text = "총 ${count}개"
        }

        // StateFlow 관찰
//        viewLifecycleOwner.lifecycleScope.launch {
//            viewLifecycleOwner.repeatOnLifecycle(Lifecycle.State.STARTED) {
//                orderViewModel.cartItems.collect { items ->
//                    // 장바구니 아이템 업데이트
//                    updateCartItems(items)
//                }
//            }
//        }

        return view
    }

    override fun onDestroyView() {
        super.onDestroyView()
        // Fragment가 파괴될 때 무조건 하단 네비게이션 바 보이게 설정
        (activity as? HomeActivity)?.showBottomNavigation()
    }

    override fun onResume() {
        super.onResume()
        (activity as? HomeActivity)?.hideBottomNavigation()
        
        // 로그인 상태 확인
        val authPrefs = requireContext().getSharedPreferences("auth", Context.MODE_PRIVATE)
        val token = authPrefs.getString("token", null)
        
        Log.d("CartFragment", "onResume - 토큰 존재 여부: ${token != null}")
        if (token != null) {
            Log.d("CartFragment", "서버에서 장바구니 정보 로드 시도")
            cartViewModel.loadUserCart(token)
        } else {
            Log.d("CartFragment", "비로그인 상태 - 장바구니 초기화")
            cartViewModel.clearCart()
        }
    }

    private fun removeItemFromCart(item: MenuItem) {
        Log.d("CartFragment", "개별 아이템 삭제 시도: ${item.name} (${item.category})")
        Log.d("CartFragment", "삭제 전 장바구니 아이템 수: ${cartItems.size}")
        cartViewModel.removeItem(item)
    }

//    private fun updateCartItems(items: List<MenuItem>) {
//        // 장바구니 UI 업데이트 로직
//        // 예: RecyclerView 어댑터 업데이트
//        cartAdapter.submitList(items)
//
//        // 디버깅을 위한 로그
//        Log.d("CartFragment", "Cart items updated: $items")
//    }
}