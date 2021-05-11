package com.github.sukury47.leavemealone.views

import com.github.sukury47.leavemealone.models.UglyBinary
import com.github.sukury47.leavemealone.viewmodels.VMRoot
import javafx.collections.FXCollections
import javafx.collections.ListChangeListener
import javafx.event.EventHandler
import javafx.fxml.FXML
import javafx.scene.Node
import javafx.scene.control.Button
import javafx.scene.control.Label
import javafx.scene.layout.TilePane
import javafx.scene.layout.VBox
import org.koin.core.component.KoinComponent
import org.koin.core.component.inject
import kotlin.random.Random

class VRoot : IView, KoinComponent {
    private val viewModel: VMRoot by inject()

    @FXML
    private lateinit var tpUglyBinaries: TilePane

    @FXML
    private lateinit var btnAdd: Button

    @FXML
    private lateinit var btnRemove: Button

    @FXML
    private lateinit var btnUpdate: Button

    @FXML
    private lateinit var btnMix: Button

    @FXML
    private fun initialize() {
        bindUglyBinaries()
    }

    private fun bindUglyBinaries() {

        viewModel.uglyBinariesProperty.addListener(ListChangeListener {
            while (it.next()) {
                when {
                    it.wasPermutated() -> {
                        val children = tpUglyBinaries.children
                        val source = tpUglyBinaries.children.toList()

                        (it.from until it.to).forEach { oldIndex ->
                            val newIndex = it.getPermutation(oldIndex)
                            children[oldIndex] = Label("$oldIndex -> $newIndex")
                            children[newIndex] = source[oldIndex]
                        }

                        children.forEachIndexed { newIndex, node ->
                            if (node is Label) {
                                val oldIndex = (it.from until it.to).first { oldIndex -> it.getPermutation(oldIndex) == newIndex }
                                children[newIndex] = source[oldIndex]
                            }
                        }

                    }
                    it.wasAdded() -> addUglyBinaries(it.addedSubList)
                    it.wasRemoved() -> removeUglyBinaries(it.removed)
                }
            }
        })

        //for dev
        btnAdd.onAction = EventHandler {
            viewModel.uglyBinariesProperty.add(UglyBinary("Ugly#${viewModel.uglyBinariesProperty.size}"))
        }

        btnUpdate.onAction = EventHandler {
            val i = Random.Default.nextInt(0, viewModel.uglyBinariesProperty.size)
            val item = viewModel.uglyBinariesProperty[i]
            println("index : $i")
            item.nameProperty.value = item.nameProperty.value + "_U"
            println("value : ${item.nameProperty.value}")
            //viewModel.uglyBinariesProperty[i].
        }

        btnMix.onAction = EventHandler {
            viewModel.uglyBinariesProperty.sortBy { Random.Default.nextInt(-1, 2) }
        }

        btnRemove.onAction = EventHandler {
            val i = Random.Default.nextInt(0, viewModel.uglyBinariesProperty.size)
            //val item = viewModel.uglyBinariesProperty[i]
            println("remove index :$i")
            viewModel.uglyBinariesProperty.removeAt(i)
        }
    }

    private fun addUglyBinaries(uglyBinaries: List<UglyBinary>) {
        uglyBinaries.forEach {
            val vBox = VBox()
            vBox.userData = it.id
            val lbName = Label()
            lbName.textProperty().bindBidirectional(it.nameProperty)
            vBox.children.add(lbName)
            tpUglyBinaries.children.add(vBox)
        }
    }

    private fun removeUglyBinaries(uglyBinaries: List<UglyBinary>) {
        uglyBinaries.forEach { item ->
            tpUglyBinaries.children.indexOfFirst { candidate ->
                val id = candidate.userData as Number
                id == item.id
            }.let { tpUglyBinaries.children.removeAt(it) }
        }
    }

    override fun onDestroy() {

    }
}