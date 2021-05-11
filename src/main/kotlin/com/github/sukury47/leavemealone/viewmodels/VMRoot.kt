package com.github.sukury47.leavemealone.viewmodels

import com.github.sukury47.leavemealone.models.UglyBinary
import com.github.sukury47.leavemealone.models.UglyService
import javafx.beans.Observable
import javafx.collections.FXCollections
import kotlinx.coroutines.MainScope
import kotlinx.coroutines.launch
import org.koin.core.component.KoinComponent
import org.koin.core.component.inject

class VMRoot : KoinComponent {
    val uglyService by inject<UglyService>()
    val uglyBinariesProperty = FXCollections.observableArrayList<UglyBinary> { item -> arrayOf(item.nameProperty, item.widthProperty, item.heightProperty) }
    val myScope = MainScope()

    fun loadUglySource() {
        myScope.launch {
            val path = ""
            uglyService.loadSource(path)
        }
    }
}