package com.github.sukury47.leavemealone.models

import javafx.beans.property.IntegerProperty
import javafx.beans.property.SimpleIntegerProperty
import javafx.beans.property.SimpleStringProperty
import java.util.concurrent.atomic.AtomicInteger

class UglyBinary(name: String) {
    val id: Number = sequence()
    val nameProperty = SimpleStringProperty()
    val widthProperty = SimpleIntegerProperty()
    val heightProperty = SimpleIntegerProperty()

    init {
        nameProperty.value = name
    }

    companion object {
        private var id = AtomicInteger(1)
        fun sequence() : Number = id.getAndIncrement()
    }


}