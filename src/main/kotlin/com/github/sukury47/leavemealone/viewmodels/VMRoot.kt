package com.github.sukury47.leavemealone.viewmodels

import com.github.sukury47.leavemealone.models.UglyBinary
import javafx.beans.Observable
import javafx.collections.FXCollections

class VMRoot {
    val uglyBinariesProperty = FXCollections.observableArrayList<UglyBinary> { item -> arrayOf(item.nameProperty, item.widthProperty, item.heightProperty) }
}