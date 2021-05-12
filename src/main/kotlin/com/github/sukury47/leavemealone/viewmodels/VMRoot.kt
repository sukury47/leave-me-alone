package com.github.sukury47.leavemealone.viewmodels

import com.github.sukury47.leavemealone.LoggerDelegate
import com.github.sukury47.leavemealone.models.UglyBinary
import com.github.sukury47.leavemealone.models.UglyService
import javafx.beans.property.SimpleDoubleProperty
import javafx.beans.property.SimpleLongProperty
import javafx.beans.property.SimpleStringProperty
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
    private var uglySourcePath: String? = null

    val progressProperty = SimpleDoubleProperty(0.0)
    val progressMsgProperty = SimpleStringProperty()

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

    private fun clearUglySource() {
        uglySource = null
        uglySourcePath = null
        uglyBinariesProperty.clear()
    }

    suspend fun loadUglySource() {
        clearUglySource()
        val path = "C:\\Users\\constant\\Desktop\\block-me-if-you-can-download\\2015102600000001.hwp"
        uglySource = uglyService.loadSource(path)
        uglySource?.let { source ->
            uglySourcePath = path
            val max = uglyService.getBinariesCount(source)
            var count = 0
            uglySourceByteCount.value = File(path).length()
            uglyService
                .loadBinaries(source)
                .flowOn(Dispatchers.IO)
                .collect {
                    count++
                    logger.debug("collect")
                    uglyBinariesProperty.add(it)
                    progressMsgProperty.value = "loading binaries... ${++count} / $max"
                    progressProperty.value = count / max.toDouble()
                }
            clearProgress()
        }
    }

    private fun clearProgress() {
        progressMsgProperty.value = ""
        progressProperty.value = 0.0
    }

    suspend fun saveUglySourceAs(path: String) {
        uglySource?.let {
            //val path = "C:\\Users\\constant\\Desktop\\block-me-if-you-can-download\\2020022700000589_compressed.hwp"
            uglyService.saveAs(it, path)
        }
    }

    fun sortBy(sortBy: SortBy) {
        uglyBinariesProperty.sortWith(sortBy.comparator)
    }

    suspend fun compress() = coroutineScope {

        var tryCount = 0

        try {
            compressAnyNotJpg()
            tryCount++
        } catch (e: Exception) {

        }

        compressUntilEnough(tryCount)
    }

    private suspend fun compressUntilEnough(orgTryCount: Int) {
        var tryCount = orgTryCount
        while (!isCompressEnough()) {
            try {
                logger.debug("compress by scale!")
                compressByScale(++tryCount)
            } catch (e: Exception) {
                break
            }
        }

        //adjust file byte count
        uglySourcePath?.let {
            tryCount++
            setProgress("($tryCount)adjust byte count..", 1, 0)
            val dir = it.substringBeforeLast("\\")
            val tempPath = "$dir\\temp.hwp"
            uglyService.saveAs(uglySource!!, tempPath)

            setProgress("($tryCount)adjust byte count..", 1, 1)

            val tempFile = File(tempPath)
            uglySourceByteCount.value = tempFile.length()
            tempFile.delete()
            if (!isCompressEnough()) {
                compressUntilEnough(++tryCount)
            }
        }
    }

    private fun isCompressEnough() = uglySourceByteCount.value <= 5 * 1024 * 1024

    private suspend fun compressByScale(tryCount: Int) = coroutineScope {

        val items = uglyBinariesProperty
            .filter { it.isSelectedProperty.value }
            .filter { it.widthProperty.value > 500 && it.heightProperty.value > 500 }

        val max = items.count()
        var count = 0

        if (max == 0) {
            throw Exception()
        }

        val factor = (uglySourceByteCount.value - (5 * 1024 * 1024)) / max
        val scale = getScaleByFactor(factor)
        logger.debug("scale : $scale, factor :$factor , ${UglyBinary.toBinaryPrefixByteCount(factor)} count : $max")

        items
            .asSequence()
            .map {
                async(Dispatchers.IO) {
                    logger.debug("uglyService.compressByScale")
                    uglyService.compressByScale(it, scale)
                }
            }
            .forEach {
                uglySourceByteCount.value -= it.await()
                setProgress("($tryCount)compress images by scale : $scale", max, ++count)
            }
    }

    private fun getScaleByFactor(factor: Long): Double {
        return when {
            factor > 400 * 1024 -> 0.5
            factor > 300 * 1024 -> 0.6
            factor > 200 * 1024 -> 0.7
            factor > 100 * 1024 -> 0.8
            factor > 50 * 1024 -> 0.9
            else -> 0.95
        }
    }

    private fun setProgress(prefixMsg: String, max: Int, count: Int) {
        progressProperty.value = count / max.toDouble()
        progressMsgProperty.value = "$prefixMsg ($count / $max)"
    }

    private suspend fun compressAnyNotJpg() = coroutineScope {
        val items = uglyBinariesProperty.filter { it.isSelectedProperty.value && it.format != "jpg"}
        val max = items.size

        if (max == 0) {
            throw Exception()
        }

        var count = 0

        items
            .asSequence()
            .map {
                async(Dispatchers.IO) {
                    logger.debug("uglyService.compressByJpg")
                    uglyService.compressByJpg(it)
                }
            }
            .forEach {
                withContext(myScope.coroutineContext) {
                    uglySourceByteCount.value -= it.await()
                    progressProperty.value = ++count / max.toDouble()
                    progressMsgProperty.value = "(1)changing format of binaries $count / $max"
                }
            }
    }
}