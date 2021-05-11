package com.github.sukury47.leavemealone.viewmodels

import com.github.sukury47.leavemealone.LoggerDelegate
import com.github.sukury47.leavemealone.models.UglyBinary
import com.github.sukury47.leavemealone.models.UglyService
import javafx.beans.Observable
import javafx.beans.property.SimpleLongProperty
import javafx.collections.FXCollections
import javafx.collections.ObservableList
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.collect
import kotlinx.coroutines.flow.flowOn
import kr.dogfoot.hwplib.`object`.HWPFile
import org.koin.core.component.KoinComponent
import org.koin.core.component.inject
import java.io.File
import java.lang.Exception
import kotlin.math.log

class VMRoot : KoinComponent {
    private val uglyService by inject<UglyService>()
    val uglyBinariesProperty: ObservableList<UglyBinary> = FXCollections.observableArrayList<UglyBinary> { item ->
        arrayOf(
            item.nameProperty,
            item.widthProperty,
            item.heightProperty
        )
    }
    val uglySourceByteCount = SimpleLongProperty()
    private val myScope = MainScope()
    private val logger by LoggerDelegate()
    private var uglySource: HWPFile? = null

    enum class SortBy {
        FILE_SIZE_ASC,
        FILE_SIZE_DESC,
        IMG_WIDTH_ASC,
        IMG_WIDTH_DESC,
        IMG_HEIGHT_ASC,
        IMG_HEIGHT_DESC;

        val comparator: Comparator<UglyBinary>
            get() {
                return when (this) {
//                    FILE_SIZE_ASC -> Comparator { o1, o2 ->
//                        o1.bytes.size.minus(o2.bytes.size)
//                    }
//                    FILE_SIZE_DESC -> Comparator { o1, o2 ->
//                        o2.bytes.size.minus(o1.bytes.size)
//                    }
                    IMG_WIDTH_ASC -> Comparator { o1, o2 ->
                        o1.widthProperty.value.minus(o2.widthProperty.value)
                    }
                    IMG_WIDTH_DESC -> Comparator { o1, o2 ->
                        o2.widthProperty.value.minus(o1.widthProperty.value)
                    }
//                    IMG_HEIGHT_ASC -> Comparator { o1, o2 ->
//                        o1.height.minus(o2.height)
//                    }
//                    IMG_HEIGHT_DESC -> Comparator { o1, o2 ->
//                        o2.height.minus(o1.height)
//                    }
                    else -> throw Exception()
                }
            }
    }

    suspend fun loadUglySource() {
        uglySource = null
        uglyBinariesProperty.clear()
        val path = "C:\\Users\\constant\\Desktop\\block-me-if-you-can-download\\2020022700000589_1.hwp"
        uglySource = uglyService.loadSource(path)
        uglySource?.let { source ->
            uglySourceByteCount.value = File(path).length()
            uglyService
                .loadBinaries(source)
                .flowOn(Dispatchers.IO)
                .collect {
                    logger.debug("collect")
                    uglyBinariesProperty.add(it)
                }
        }
    }

    fun sortBy(sortBy: SortBy) {
        uglyBinariesProperty.sortWith(sortBy.comparator)
    }

    suspend fun compress() {
        myScope.launch {
            logger.debug("am i called?")
            uglyBinariesProperty
                .asSequence()
                .filter { it.isSelectedProperty.value }
                .onEach { logger.debug("??") }
                .filter { it.format != "jpg" }
                .onEach { logger.debug("i am not jpg") }
                .map {
                    async {
                        logger.debug("uglyService.compressByJpg")
                        uglyService.compressByJpg(it)
                    }
                }
                .forEach {
                    withContext(myScope.coroutineContext) {
                        uglySourceByteCount.value -= it.await()
                    }
                }
        }
    }
}