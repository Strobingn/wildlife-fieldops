package com.strobingn.wildlifefieldops.ui.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.strobingn.wildlifefieldops.data.local.InventoryItemDao
import com.strobingn.wildlifefieldops.data.model.InventoryItem
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class InventoryViewModel @Inject constructor(
    private val inventoryItemDao: InventoryItemDao
) : ViewModel() {

    private val _searchQuery = MutableStateFlow("")
    val searchQuery = _searchQuery.asStateFlow()

    @OptIn(kotlinx.coroutines.ExperimentalCoroutinesApi::class)
    val items = _searchQuery.flatMapLatest { query ->
        if (query.isBlank()) {
            inventoryItemDao.getAll()
        } else {
            inventoryItemDao.getAll().map { list ->
                list.filter {
                    it.name.contains(query, ignoreCase = true) ||
                    it.sku.contains(query, ignoreCase = true) ||
                    it.category.contains(query, ignoreCase = true)
                }
            }
        }
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val lowStockItems = inventoryItemDao.getLowStock()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    fun setSearchQuery(query: String) {
        _searchQuery.value = query
    }

    fun addItem(item: InventoryItem) = viewModelScope.launch {
        inventoryItemDao.insert(item)
    }

    fun updateItem(item: InventoryItem) = viewModelScope.launch {
        inventoryItemDao.update(item)
    }

    fun adjustStock(itemId: String, newQuantity: Double) = viewModelScope.launch {
        val item = inventoryItemDao.getById(itemId)
        item?.let {
            inventoryItemDao.update(it.copy(quantityOnHand = newQuantity, updatedAt = System.currentTimeMillis()))
        }
    }

    fun deleteItem(item: InventoryItem) = viewModelScope.launch {
        inventoryItemDao.delete(item)
    }
}
