package com.gas_price_finder.presentation

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.gas_price_finder.domain.model.ThemeMode
import com.gas_price_finder.domain.usecase.user.GetActiveUserUseCase
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import javax.inject.Inject

@HiltViewModel
class MainViewModel @Inject constructor(
    getActiveUserUseCase: GetActiveUserUseCase
) : ViewModel() {

    val themeMode: StateFlow<ThemeMode?> = getActiveUserUseCase()
        .map { user -> user?.tema }
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = null
        )
}
