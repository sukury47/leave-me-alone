package com.github.sukury47.leavemealone.views

import com.github.sukury47.leavemealone.viewmodels.VMRoot
import org.koin.core.component.KoinComponent
import org.koin.core.component.inject

class VRoot : IView, KoinComponent {
    private val viewModel: VMRoot by inject()

    override fun onDestroy() {

    }
}