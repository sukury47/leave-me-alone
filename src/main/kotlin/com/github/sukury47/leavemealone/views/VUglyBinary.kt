package com.github.sukury47.leavemealone.views

import com.github.sukury47.leavemealone.models.UglyBinary
import javafx.beans.binding.Bindings
import javafx.event.EventHandler
import javafx.fxml.FXML
import javafx.scene.control.Label
import javafx.scene.image.ImageView
import javafx.scene.input.MouseButton
import javafx.scene.layout.StackPane

class VUglyBinary : IView {
    @FXML
    private lateinit var lbHex: Label
    @FXML
    private lateinit var lbFormat: Label
    @FXML
    private lateinit var lbSize: Label
    @FXML
    private lateinit var lbByteCount: Label
    @FXML
    private lateinit var iv: ImageView
    @FXML
    private lateinit var sp: StackPane

    fun draw(item: UglyBinary) {
        lbHex.textProperty().bind(
            Bindings.createStringBinding(
                { item.nameProperty.value.substringBeforeLast(".") }, item.nameProperty
            )
        )

        lbFormat.textProperty().bind(
            Bindings.createStringBinding(
                { item.nameProperty.value.substringAfterLast(".") },
                item.nameProperty
            )
        )

        lbSize.textProperty().bind(
            Bindings.createStringBinding(
                { "${item.widthProperty.value} x ${item.heightProperty.value}" },
                item.widthProperty,
                item.heightProperty
            )
        )

        lbByteCount.textProperty().bind(item.byteCountProperty)

        item.isSelectedProperty.addListener { _, _, newValue ->
            val borderColor = if (newValue) "#4B9BD2" else "#ffffff"
            println("$newValue : $borderColor")

            sp.style = "-fx-border-color:$borderColor;-fx-border-width:4"

            arrayOf(lbByteCount, lbSize, lbFormat, lbHex).forEach {
                it.style = "-fx-background-color:$borderColor"
            }
        }

        iv.image = item.thumbnail

        sp.onMouseClicked = EventHandler {
            println(it.button)
            if (it.button == MouseButton.PRIMARY) {
                item.isSelectedProperty.value = !item.isSelectedProperty.value
            }
        }
    }

    override fun onDestroy() {

    }
}