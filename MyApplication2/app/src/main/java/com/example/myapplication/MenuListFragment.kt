package com.example.myapplication

import android.app.AlertDialog
import android.content.res.ColorStateList
import android.graphics.Color
import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.ImageButton
import android.widget.RadioButton
import android.widget.RadioGroup
import android.widget.TextView
import android.widget.Toast
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.flowWithLifecycle
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.google.android.material.bottomsheet.BottomSheetDialog
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.util.concurrent.atomic.AtomicInteger

class MenuListFragment : Fragment() {

    private lateinit var recyclerView: RecyclerView
    private lateinit var orderAdapter: OrderAdapter
    private lateinit var menuItems: List<MenuItem>
    private var favoriteMenuFragment: FavoriteMenuFragment? = null
    private val orderViewModel: OrderViewModel by viewModels()

    companion object {
        private const val ARG_MENU_ITEMS = "menu_items"

        fun newInstance(menuItems: List<MenuItem>): MenuListFragment {
            val fragment = MenuListFragment()
            val args = Bundle().apply {
                putParcelableArrayList(ARG_MENU_ITEMS, ArrayList(menuItems))
            }
            fragment.arguments = args
            return fragment
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        menuItems = arguments?.getParcelableArrayList(ARG_MENU_ITEMS) ?: emptyList()
        favoriteMenuFragment = parentFragmentManager.fragments
            .find { it is FavoriteMenuFragment } as? FavoriteMenuFragment
    }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        val view = inflater.inflate(R.layout.fragment_menu_list, container, false)
        
        recyclerView = view.findViewById(R.id.recyclerView_orders)
        recyclerView.layoutManager = LinearLayoutManager(requireContext())
        
        orderAdapter = OrderAdapter(menuItems, onMenuItemClick)
        recyclerView.adapter = orderAdapter
        
        return view
    }

    private val onMenuItemClick: (MenuItem) -> Unit = { menuItem ->
        showMenuOptionDialog(menuItem)
    }

    private fun showMenuOptionDialog(menuItem: MenuItem) {
        MenuOptionDialog(
            context = requireContext(),
            menuItem = menuItem,
            onAddToCart = { updatedMenuItem ->
                (parentFragment as? OrderFragment)?.addToCart(updatedMenuItem)
            },
            onPlaceOrder = { updatedMenuItem ->
                (parentFragment as? OrderFragment)?.moveToCheckout(listOf(updatedMenuItem))
            },
            onAddToFavorite = { favoriteMenuItem ->
                favoriteMenuFragment?.addToFavorite(favoriteMenuItem) ?: false
            },
            onRemoveFromFavorite = { menuItem ->
                favoriteMenuFragment?.removeFromFavorite(menuItem)
            },
            orderViewModel = orderViewModel
        ).show()
    }

    fun updateMenuItems(newItems: List<MenuItem>) {
        menuItems = newItems
        if (::orderAdapter.isInitialized) {
            orderAdapter.updateMenu(newItems)
        }
    }

    fun getRecyclerView(): RecyclerView = recyclerView
}
