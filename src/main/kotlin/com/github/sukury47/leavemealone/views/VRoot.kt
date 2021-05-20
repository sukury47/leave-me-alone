package com.github.sukury47.leavemealone.views

import com.github.sukury47.leavemealone.LoggerDelegate
import com.github.sukury47.leavemealone.ScreenController
import com.github.sukury47.leavemealone.models.UglyBinary
import com.github.sukury47.leavemealone.viewmodels.VMRoot
import javafx.application.Platform
import javafx.beans.binding.Bindings
import javafx.beans.property.SimpleBooleanProperty
import javafx.collections.ListChangeListener
import javafx.event.EventHandler
import javafx.fxml.FXML
import javafx.fxml.FXMLLoader
import javafx.scene.Parent
import javafx.scene.control.*
import javafx.scene.input.TransferMode
import javafx.scene.layout.TilePane
import javafx.scene.layout.VBox
import javafx.stage.FileChooser
import kotlinx.coroutines.*
import org.koin.core.component.KoinComponent
import org.koin.core.component.inject
import java.io.File

class VRoot : IView, KoinComponent {


    @FXML
    private lateinit var tpUglyBinaries: TilePane

    @FXML
    private lateinit var miOpen: MenuItem

    @FXML
    private lateinit var miSortByWidthAsc: MenuItem

    @FXML
    private lateinit var miSortByWidthDesc: MenuItem

    @FXML
    private lateinit var miCompressAll: MenuItem

    @FXML
    private lateinit var miSaveAs: MenuItem

    @FXML
    private lateinit var miSave: MenuItem

    @FXML
    private lateinit var miQuit: MenuItem

    @FXML
    private lateinit var miOpenFromUrl: MenuItem

    @FXML
    private lateinit var scrp: ScrollPane

    @FXML
    private lateinit var lbStatus: Label

    @FXML
    private lateinit var vbProgress: VBox

    @FXML
    private lateinit var pb: ProgressBar

    @FXML
    private lateinit var lbPbMsg: Label

    @FXML
    private lateinit var btnCancel: Button

    private val logger by LoggerDelegate()
    private val myScope = MainScope() + CoroutineExceptionHandler { _, throwable ->
        logger.error(throwable.message)
        val alert = Alert(Alert.AlertType.ERROR)
        alert.title = "Unexpected Error"
        alert.headerText = null
        alert.contentText = throwable.message
        alert.initOwner(screenController.primaryStage)
        alert.showAndWait()
    }

    private val busyProperty = SimpleBooleanProperty(false)

    private val viewModel: VMRoot by inject()
    private val screenController by inject<ScreenController>()

    private val extensionFilterForHwp = FileChooser.ExtensionFilter("한글", "*.hwp")

    private var job: Job? = null

    @FXML
    private fun initialize() {
        bindUglyBinaries()
        bindMenuBar()
        sorryMyBad()
        bindProgress()
        bindCancel()

        viewModel.uglySourceByteCount.addListener { _, _, newValue ->
            val currentByteCount = UglyBinary.toBinaryPrefixByteCount(newValue.toLong())
            lbStatus.text = "$currentByteCount / 5MB"
        }

        val isUglySourceNullProperty = viewModel.uglySourceByteCount.lessThan(1)

        arrayOf(miOpen, miOpenFromUrl).forEach { it.disableProperty().bind(busyProperty) }

        val sibal = isUglySourceNullProperty.or(busyProperty)

        arrayOf(miSave, miSaveAs, miCompressAll, miSortByWidthDesc, miSortByWidthAsc).forEach { it.disableProperty().bind(sibal) }

        scrp.run {
            setOnDragOver {
                if (it.gestureSource != it && it.dragboard.hasFiles() && it.dragboard.files.size == 1 && it.dragboard.files[0].extension == "hwp") {
                    it.acceptTransferModes(*TransferMode.COPY_OR_MOVE)
                }
                it.consume()
            }

            setOnDragDropped {
                val board = it.dragboard
                var success = false
                if (board.hasFiles() && board.files.size == 1) {
                    openFile(board.files[0])
                    success = true
                }
                it.isDropCompleted = success
                it.consume()
            }
        }
    }

    private fun bindCancel() {
        btnCancel.onAction = EventHandler {
            job?.cancel()
        }
    }

    private fun bindProgress() {
        busyProperty.addListener { _, _, newValue ->
            logger.debug("busyProperty.value : $newValue")
            vbProgress.isVisible = newValue
        }

        pb.progressProperty().bindBidirectional(viewModel.progressProperty)
        lbPbMsg.textProperty().bindBidirectional(viewModel.progressMsgProperty)
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
            openFileChooser()
        }

        miSortByWidthAsc.onAction = EventHandler {
            viewModel.sortBy(VMRoot.SortBy.IMG_WIDTH_ASC)
        }

        miSortByWidthDesc.onAction = EventHandler {
            viewModel.sortBy(VMRoot.SortBy.IMG_WIDTH_DESC)
        }

        miCompressAll.onAction = EventHandler {
            busyProperty.value = true
            myScope.launch {
                viewModel.compress()
                busyProperty.value = false
            }
        }

        miSaveAs.onAction = EventHandler {
            openFileChooserForSaveAs()
        }

        miSave.onAction = EventHandler {
            if (viewModel.isUglySourceVolatile()) {
               openFileChooserForSaveAs()
            } else {
                myScope.launch {
                    viewModel.saveUglySource()
                }
            }
        }

        miOpenFromUrl.onAction = EventHandler {
            val dialog = TextInputDialog()
            dialog.title = "Open File From URL"
            dialog.graphic = null
            dialog.headerText = null
            dialog.contentText = "Enter URL"
            val optional = dialog.showAndWait()
            if (optional.isPresent) {
                block()
                job = myScope.launch {
                    try {
                        viewModel.loadUglySourceByUrl(optional.get())
                    } finally {
                        unblock()
                    }
                }
            }
        }

        miQuit.onAction = EventHandler {
            Platform.exit()
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
                                val oldIndex =
                                    (it.from until it.to).first { oldIndex -> it.getPermutation(oldIndex) == newIndex }
                                children[newIndex] = source[oldIndex]
                            }
                        }
                    }
                    it.wasAdded() -> addUglyBinaries(it.addedSubList)
                    it.wasRemoved() -> removeUglyBinaries(it.removed)
                }
            }
        })
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
        if (uglyBinaries.size == tpUglyBinaries.children.size) {
            tpUglyBinaries.children.clear()
        } else {
            uglyBinaries.forEach { item ->
                tpUglyBinaries.children.indexOfFirst { candidate ->
                    val id = candidate.userData as String
                    id == item.id
                }.let { tpUglyBinaries.children.removeAt(it) }
            }
        }
    }

    private fun openFileChooser() {
        val chooser = getFileChooserForHwp()
        val file = chooser.showOpenDialog(null)
        if (file != null) {
            openFile(file)
        }
    }

    private fun getFileChooserForHwp(): FileChooser {
        val chooser = FileChooser()
        chooser.extensionFilters.add(extensionFilterForHwp)
        chooser.initialDirectory = File(viewModel.lastUglySourcePath)
        return chooser
    }

    private fun openFileChooserForSaveAs() {
        val chooser = getFileChooserForHwp()
        val file = chooser.showSaveDialog(null)
        if (file != null) {
            logger.debug(file.path)
            block()
            myScope.launch {
                viewModel.saveUglySourceAs(file)
                unblock()
            }
        }
    }

    private fun openFile(file: File) {
        block()
        job = myScope.launch {
            try {
                viewModel.loadUglySourceByLocalFile(file)
            } finally {
                unblock()
            }
        }
    }

    private fun block() {
        busyProperty.value = true
    }

    private fun unblock() {
        busyProperty.value = false
    }

    override fun onDestroy() {
        myScope.cancel()
    }
}