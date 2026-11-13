package com.example.myapplication

import android.app.DatePickerDialog
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
import androidx.fragment.app.viewModels
import androidx.lifecycle.Observer
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import java.util.Calendar
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

class OrderHistoryFragment : Fragment() {
    private val viewModel: OrderHistoryViewModel by viewModels()
    private lateinit var recyclerView: RecyclerView
    private lateinit var orderHistoryAdapter: OrderHistoryAdapter
    private lateinit var backButton: ImageButton
    private lateinit var startDateButton: Button
    private lateinit var endDateButton: Button
    private lateinit var searchButton: Button
    private lateinit var oneWeekButton: Button
    private lateinit var oneMonthButton: Button
    private lateinit var threeMonthsButton: Button
    private lateinit var noOrdersTextView: TextView

    private var startDate: Calendar = Calendar.getInstance()
    private var endDate: Calendar = Calendar.getInstance()
    private val dateFormat = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault())

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        val view = inflater.inflate(R.layout.fragment_order_history, container, false)
        initializeViews(view)
        setupRecyclerView()
        setupClickListeners()
        
        // 초기 날짜 설정 (1주일)
        startDate.add(Calendar.WEEK_OF_YEAR, -1)
        updateDateButtonsText()
        loadOrderHistory()
        
        return view
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        setupObservers()
        
        // 초기 데이터 로드
        startDate.add(Calendar.WEEK_OF_YEAR, -1)
        updateDateButtonsText()
        loadOrderHistory()
    }

    override fun onResume() {
        super.onResume()
        (activity as? HomeActivity)?.hideBottomNavigation()
    }

    override fun onDestroyView() {
        super.onDestroyView()
        // Fragment가 파괴될 때 하단 네비게이션 바 다시 보이게 설정
        (activity as? HomeActivity)?.showBottomNavigation()
    }

    private fun initializeViews(view: View) {
        recyclerView = view.findViewById(R.id.recycler_order_history)
        backButton = view.findViewById(R.id.btn_back)
        startDateButton = view.findViewById(R.id.btn_start_date)
        endDateButton = view.findViewById(R.id.btn_end_date)
        searchButton = view.findViewById(R.id.btn_search)
        oneWeekButton = view.findViewById(R.id.btn_1week)
        oneMonthButton = view.findViewById(R.id.btn_1month)
        threeMonthsButton = view.findViewById(R.id.btn_3months)
        noOrdersTextView = view.findViewById(R.id.tv_no_orders)
    }

    private fun setupRecyclerView() {
        orderHistoryAdapter = OrderHistoryAdapter()
        recyclerView.apply {
            adapter = orderHistoryAdapter
            layoutManager = LinearLayoutManager(context)
        }
    }

    private fun setupClickListeners() {
        backButton.setOnClickListener {
            parentFragmentManager.popBackStack()
        }

        oneWeekButton.setOnClickListener {
            startDate = Calendar.getInstance()
            startDate.add(Calendar.WEEK_OF_YEAR, -1)
            endDate = Calendar.getInstance()
            updateDateButtonsText()
        }

        oneMonthButton.setOnClickListener {
            startDate = Calendar.getInstance()
            startDate.add(Calendar.MONTH, -1)
            endDate = Calendar.getInstance()
            updateDateButtonsText()
        }

        threeMonthsButton.setOnClickListener {
            startDate = Calendar.getInstance()
            startDate.add(Calendar.MONTH, -3)
            endDate = Calendar.getInstance()
            updateDateButtonsText()
        }

        startDateButton.setOnClickListener {
            showDatePicker(startDate) { selectedCalendar ->
                if (selectedCalendar.after(endDate)) {
                    Toast.makeText(context, "시작일은 종료일 이후일 수 없습니다.", Toast.LENGTH_SHORT).show()
                    return@showDatePicker
                }
                startDate = selectedCalendar
                updateDateButtonsText()
            }
        }

        endDateButton.setOnClickListener {
            showDatePicker(endDate) { selectedCalendar ->
                if (selectedCalendar.before(startDate)) {
                    Toast.makeText(context, "종료일은 시작일 이전일 수 없습니다.", Toast.LENGTH_SHORT).show()
                    return@showDatePicker
                }
                endDate = selectedCalendar
                updateDateButtonsText()
            }
        }

        searchButton.setOnClickListener {
            loadOrderHistory()
        }
    }

    private fun showDatePicker(calendar: Calendar, onDateSelected: (Calendar) -> Unit) {
        DatePickerDialog(
            requireContext(),
            { _, year, month, dayOfMonth ->
                val selectedCalendar = Calendar.getInstance().apply {
                    set(year, month, dayOfMonth)
                }
                onDateSelected(selectedCalendar)
            },
            calendar.get(Calendar.YEAR),
            calendar.get(Calendar.MONTH),
            calendar.get(Calendar.DAY_OF_MONTH)
        ).show()
    }

    private fun updateDateButtonsText() {
        startDateButton.text = dateFormat.format(startDate.time)
        endDateButton.text = dateFormat.format(endDate.time)
    }

    private fun setupObservers() {
        viewModel.isLoading.observe(viewLifecycleOwner) { isLoading ->
            // TODO: 로딩 인디케이터 추가
        }

        viewModel.error.observe(viewLifecycleOwner) { error ->
            Toast.makeText(requireContext(), error, Toast.LENGTH_SHORT).show()
            noOrdersTextView.visibility = View.VISIBLE
            recyclerView.visibility = View.GONE
        }

        viewModel.orderGroups.observe(viewLifecycleOwner) { orders ->
            orderHistoryAdapter.submitList(orders)
            if (orders.isEmpty()) {
                noOrdersTextView.visibility = View.VISIBLE
                recyclerView.visibility = View.GONE
            } else {
                noOrdersTextView.visibility = View.GONE
                recyclerView.visibility = View.VISIBLE
            }
        }
    }

    private fun loadOrderHistory() {
        // 시작일은 해당 날짜의 시작(00:00:00)으로 설정
        val formattedStartDate = startDate.apply {
            set(Calendar.HOUR_OF_DAY, 0)
            set(Calendar.MINUTE, 0)
            set(Calendar.SECOND, 0)
        }.time.let { dateFormat.format(it) }

        // 종료일은 해당 날짜의 끝(23:59:59)으로 설정
        val formattedEndDate = endDate.apply {
            set(Calendar.HOUR_OF_DAY, 23)
            set(Calendar.MINUTE, 59)
            set(Calendar.SECOND, 59)
        }.time.let { dateFormat.format(it) }

        Log.d("OrderHistory", "조회 시작일: $formattedStartDate, 종료일: $formattedEndDate")
        
        viewModel.fetchOrderHistory(
            startDate = formattedStartDate,
            endDate = formattedEndDate
        )
    }
}