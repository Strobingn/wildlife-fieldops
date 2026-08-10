package com.strobingn.wildlifefieldops.ui.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.strobingn.wildlifefieldops.data.local.PhotoDao
import com.strobingn.wildlifefieldops.data.model.Photo
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class BeforeAfterViewModel @Inject constructor(
    private val photoDao: PhotoDao
) : ViewModel() {

    private val _photos = MutableStateFlow<List<Photo>>(emptyList())
    val photos: StateFlow<List<Photo>> = _photos.asStateFlow()

    fun loadForJob(jobId: String) {
        viewModelScope.launch {
            photoDao.getByJob(jobId).collect { list ->
                _photos.value = list
            }
        }
    }
}
