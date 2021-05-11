package com.github.sukury47.leavemealone.views

import com.github.sukury47.leavemealone.LoggerDelegate
import com.github.sukury47.leavemealone.models.UglyBinary
import com.github.sukury47.leavemealone.viewmodels.VMRoot
import javafx.beans.property.SimpleBooleanProperty
import javafx.collections.FXCollections
import javafx.collections.ListChangeListener
import javafx.event.EventHandler
import javafx.fxml.FXML
import javafx.fxml.FXMLLoader
import javafx.scene.Node
import javafx.scene.Parent
import javafx.scene.control.*
import javafx.scene.layout.TilePane
import javafx.scene.layout.VBox
import kotlinx.coroutines.MainScope
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
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
    private lateinit var miOpen: MenuItem

    @FXML
    private lateinit var miSortByWidthAsc: MenuItem

    @FXML
    private lateinit var miSortByWidthDesc: MenuItem

    @FXML
    private lateinit var miCompress: MenuItem

    @FXML
    private lateinit var scrp: ScrollPane

    @FXML
    private lateinit var lbStatus: Label

    private val logger by LoggerDelegate()
    private val myScope = MainScope()

    private val busyProperty = SimpleBooleanProperty(false)

    @FXML
    private fun initialize() {
        bindUglyBinaries()
        bindMenuBar()
        sorryMyBad()

        tpUglyBinaries.disableProperty().bind(busyProperty)
        viewModel.uglySourceByteCount.addListener { _, _, newValue ->
            val currentByteCount = UglyBinary.toBinaryPrefixByteCount(newValue.toLong())
            lbStatus.text = "$currentByteCount / 5MB"
        }
    }

    private fun sorryMyBad() {
        scrp.widthProperty().addListener { _, _, newValue ->
            tpUglyBinaries.prefWidth = newValue.toDouble() - 8
        }

        scrp.heightProperty().addListener { _, _, newValue ->
            tpUglyBinaries.prefHeight = newValue.toDouble() - 8
        }
    }

    private fun bindMenuBar() {
        miOpen.onAction = EventHandler {
            logger.debug("am i called?")
            miOpen.isDisable = true
            busyProperty.value = true
            myScope.launch {
                val job = myScope.launch {
                    viewModel.loadUglySource()
                }
                job.join()
                withContext(myScope.coroutineContext) {
                    miOpen.isDisable = false
                    busyProperty.value = false
                }
            }
        }

        miSortByWidthAsc.onAction = EventHandler {
            viewModel.sortBy(VMRoot.SortBy.IMG_WIDTH_ASC)
        }

        miSortByWidthDesc.onAction = EventHandler {
            viewModel.sortBy(VMRoot.SortBy.IMG_WIDTH_DESC)
        }

        miCompress.onAction = EventHandler {
            logger.debug("?????????????????????")
            miCompress.isDisable = true
            myScope.launch {
                viewModel.compress()
                miCompress.isDisable = false
            }
        }
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
            //viewModel.uglyBinariesProperty.add(UglyBinary("Ugly#${viewModel.uglyBinariesProperty.size}"))
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
            val fxmlLoader = FXMLLoader()
            val parent = fxmlLoader.load<Parent>(this.javaClass.getResourceAsStream("/views/cell_ugly_binary.fxml"))
            val view = fxmlLoader.getController<VUglyBinary>()
            view.draw(it)
            tpUglyBinaries.children.add(parent)
        }
    }

    private fun removeUglyBinaries(uglyBinaries: List<UglyBinary>) {
        uglyBinaries.forEach { item ->
            tpUglyBinaries.children.indexOfFirst { candidate ->
                val id = candidate.userData as String
                id == item.id
            }.let { tpUglyBinaries.children.removeAt(it) }
        }
    }

    override fun onDestroy() {

    }
}