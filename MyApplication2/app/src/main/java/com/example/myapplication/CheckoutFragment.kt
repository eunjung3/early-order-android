package com.example.myapplication

import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.ImageButton
import android.widget.TextView
import android.widget.Toast
import androidx.fragment.app.Fragment
import androidx.fragment.app.activityViewModels
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import com.example.myapplication.databinding.FragmentCheckoutBinding
import android.app.Activity
import java.util.*
import retrofit2.Response

class CheckoutFragment : Fragment() {

    private lateinit var recyclerView: RecyclerView
    private lateinit var totalPriceTextView: TextView
    private lateinit var confirmOrderButton: Button
    private lateinit var backButton: ImageButton
    private lateinit var orderSummaryAdapter: OrderSummaryAdapter
    private var selectedItems: List<MenuItem> = listOf()
    private val orderViewModel: OrderViewModel by activityViewModels()
    private val cartViewModel: CartViewModel by activityViewModels()
    private var _binding: FragmentCheckoutBinding? = null
    private val binding get() = _binding!!
    private var paymentJob: Job? = null
    var isFromCart: Boolean = false
    private var orderResponse: Response<OrderResponse>? = null

    companion object {
        private const val PAYMENT_REQUEST_CODE = 1000
        private const val ARG_SELECTED_ITEMS = "selected_items"

        fun newInstance(selectedItems: List<MenuItem>): CheckoutFragment {
            val fragment = CheckoutFragment()
            val args = Bundle()
            args.putParcelableArrayList(ARG_SELECTED_ITEMS, ArrayList(selectedItems))
            fragment.arguments = args
            return fragment
        }
    }

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        _binding = FragmentCheckoutBinding.inflate(inflater, container, false)
        val view = binding.root

        activity?.let { activity ->
            view.post {
                (activity as? HomeActivity)?.hideBottomNavigation()
            }
        }

        // 선택된 메뉴 아이템들을 가져옴
        selectedItems = arguments?.getParcelableArrayList(ARG_SELECTED_ITEMS) ?: listOf()

        // RecyclerView 설정
        recyclerView = view.findViewById(R.id.recycler_order_summary)
        recyclerView.layoutManager = LinearLayoutManager(context)
        backButton = view.findViewById(R.id.btn_b2)

        // OrderSummaryAdapter 초기화 및 설정
        orderSummaryAdapter = OrderSummaryAdapter(selectedItems)
        recyclerView.adapter = orderSummaryAdapter

        // 총 가격 텍스트뷰 초기화 및 업데이트
        totalPriceTextView = view.findViewById(R.id.tv_total_price)
        updateTotalPrice()

        // 주문 확인 버튼 설정
        confirmOrderButton = view.findViewById(R.id.btn_confirm_order)
        confirmOrderButton.setOnClickListener {
            confirmOrder()
        }

        backButton.setOnClickListener {
            activity?.onBackPressedDispatcher?.onBackPressed()
        }

        return view
    }

    override fun onDestroyView() {
        super.onDestroyView()
        activity?.let { activity ->
            view?.post {
                (activity as? HomeActivity)?.showBottomNavigation()
            }
        }
        paymentJob?.cancel()
        _binding = null
    }

    private fun updateTotalPrice() {
        if (!isAdded) return
        val totalPrice = selectedItems.sumOf { it.calculateTotalPrice() }
        totalPriceTextView.text = "총 ${totalPrice}원"
    }

    private fun confirmOrder() {
        if (!isAdded) return

        binding.loadingProgressBar?.visibility = View.VISIBLE
        confirmOrderButton.isEnabled = false

        paymentJob = lifecycleScope.launch(Dispatchers.IO) {
            try {
                if (_binding == null) return@launch

                withContext(Dispatchers.Main) {
                    binding.loadingProgressBar?.visibility = View.VISIBLE
                    confirmOrderButton.isEnabled = false
                }

                withContext(Dispatchers.IO) {
                    val context = context ?: return@withContext
                    val token = context.getSharedPreferences("auth", Context.MODE_PRIVATE)
                        .getString("token", null) ?: return@withContext

                    val items = arguments?.getParcelableArrayList<MenuItem>(ARG_SELECTED_ITEMS) ?: return@withContext

                    // 1. 모든 아이템을 하나의 주문으로 묶어서 처리
                    val orderRequest = OrderRequest(
                        items = items.map { menuItem ->
                            OrderItemRequest(
                                menuName = menuItem.name,
                                quantity = menuItem.quantity,
                                price = menuItem.calculateTotalPrice(),
                                unitPrice = menuItem.calculateBasePrice(),
                                hotIceOption = menuItem.hotIceOption,
                                size = menuItem.size,
                                shotQuantity = menuItem.shotQuantity,
                                hazelnutQuantity = menuItem.hazelnutQuantity,
                                tapiocaQuantity = menuItem.tapiocaQuantity,
                                decafQuantity = menuItem.decafQuantity,
                                vanillaQuantity = menuItem.vanillaQuantity,
                                whippingQuantity = menuItem.whippingQuantity,
                                iceCreamQuantity = menuItem.iceCreamQuantity
                            )
                        }
                    )

                    // 디버그 로그 추가
                    Log.d("CheckoutOrder", "전체 주문 요청: ${items.size}개 메뉴")
                    items.forEach { menuItem ->
                        Log.d("CheckoutOrder", """
                            주문 아이템 상세 정보:
                            - 메뉴명: ${menuItem.name}
                            - 수량: ${menuItem.quantity}
                            - 기본 가격: ${menuItem.calculateBasePrice()}
                            - 총 가격: ${menuItem.calculateTotalPrice()}
                            - 개별 가격: ${menuItem.calculateTotalPrice() / menuItem.quantity}
                            - 옵션: ${menuItem.getOptionsText()}
                        """.trimIndent())
                    }

                    val orderResponse = RetrofitClient.apiService.createOrder("Bearer $token", orderRequest).execute()

                    if (!orderResponse.isSuccessful) {
                        throw Exception("주문 생성 실패")
                    }

                    // 응답 로그 추가
                    Log.d("CheckoutOrder", "주문 응답: ${orderResponse.body()}")

                    val orderId = orderResponse.body()?.data?.first()?.order_id ?: throw Exception("주문 ID 없음")
                    val totalAmount = items.sumOf { it.calculateTotalPrice() }

                    // 2. 결제 준비 요청
                    val paymentPrepareRequest = PaymentPrepareRequest(
                        order_id = orderId,
                        amount = totalAmount
                    )

                    // 결제 준비 요청 전
                    Log.d("CheckoutPayment", """
                        결제 준비 요청:
                        orderId: ${orderId}
                        totalAmount: ${totalAmount}
                    """.trimIndent())

                    val paymentResponse = RetrofitClient.apiService.preparePayment(
                        "Bearer $token",
                        paymentPrepareRequest
                    ).execute()

                    // 결제 준비 응답 확인
                    Log.d("CheckoutPayment", """
                        결제 준비 응답:
                        성공 여부: ${paymentResponse.isSuccessful}
                        응답 코드: ${paymentResponse.code()}
                        응답 메시지: ${paymentResponse.message()}
                        응답 바디: ${paymentResponse.body()}
                    """.trimIndent())

                    if (!paymentResponse.isSuccessful) {
                        throw Exception("결제 준비 실패")
                    }

                    val paymentData = paymentResponse.body() ?: throw Exception("결제 데이터 없음")

                    // 3. 토스 페이먼츠 결제 위젯 실행
                    withContext(Dispatchers.Main) {
                        val intent = Intent(requireContext(), TossPaymentActivity::class.java).apply {
                            putExtra("orderId", orderId.toString())
                            putExtra("amount", paymentData.amount)
                            putExtra("checkoutUrl", paymentData.checkoutUrl)
                        }
                        startActivityForResult(intent, PAYMENT_REQUEST_CODE)
                    }
                }
            } catch (e: Exception) {
                withContext(Dispatchers.Main) {
                    binding.loadingProgressBar?.visibility = View.GONE
                    confirmOrderButton.isEnabled = true
                    context?.let {
                        Toast.makeText(it, "결제 처리 중 오류가 발생했습니다: ${e.message}", Toast.LENGTH_SHORT).show()
                    }
                    e.printStackTrace()
                }
            }
        }
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)

        if (requestCode == PAYMENT_REQUEST_CODE) {
            binding.loadingProgressBar?.visibility = View.GONE
            confirmOrderButton.isEnabled = true

            when (resultCode) {
                Activity.RESULT_OK -> {
                    val orderId = data?.getStringExtra("orderId")
                    val paymentKey = data?.getStringExtra("paymentKey")
                    val amount = data?.getIntExtra("amount", 0)

                    if (orderId != null && paymentKey != null && amount != null) {
                        confirmPayment(paymentKey, orderId, amount)
                    }
                }
                Activity.RESULT_CANCELED -> {
                    val errorMessage = data?.getStringExtra("error") ?: "결제가 취소되었습니다."
                    Toast.makeText(context, errorMessage, Toast.LENGTH_SHORT).show()
                }
            }
        }
    }

    private fun confirmPayment(paymentKey: String, orderId: String, amount: Int) {
        lifecycleScope.launch(Dispatchers.IO) {
            try {
                val token = context?.getSharedPreferences("auth", Context.MODE_PRIVATE)
                    ?.getString("token", null) ?: return@launch

                // orderResponse에서 모든 주문 ID 가져오기
                val orderIds = orderResponse?.body()?.data?.map { it.order_id.toString() } ?: listOf(orderId)
                
                // 각 주문에 대해 결제 확인 API 호출
                orderIds.forEach { id ->
                    RetrofitClient.apiService.confirmPayment(
                        token = "Bearer $token",
                        paymentKey = paymentKey,
                        orderId = id,
                        amount = amount
                    ).execute()
                }

                withContext(Dispatchers.Main) {
                    // 결제 성공 처리
                    val selectedItems = arguments?.getParcelableArrayList<MenuItem>(ARG_SELECTED_ITEMS) ?: return@withContext

                    if (isFromCart) {
                        cartViewModel.removeSelectedItems(selectedItems)
                        cartViewModel.updateTotalPriceAndCount()
                    }

                        orderViewModel.setCurrentOrder(selectedItems, orderId.toInt())

                    activity?.supportFragmentManager
                        ?.beginTransaction()
                        ?.replace(R.id.fragment_container, OkFragment())
                        ?.addToBackStack(null)
                        ?.commit()
                }
            } catch (e: Exception) {
                Log.e("CheckoutFragment", "결제 확인 실패", e)
            }
        }
    }
}
