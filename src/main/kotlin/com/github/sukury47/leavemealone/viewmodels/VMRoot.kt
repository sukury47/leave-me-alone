package com.github.sukury47.leavemealone.viewmodels

import com.github.sukury47.leavemealone.LoggerDelegate
import com.github.sukury47.leavemealone.models.*
import javafx.beans.property.SimpleDoubleProperty
import javafx.beans.property.SimpleLongProperty
import javafx.beans.property.SimpleStringProperty
import javafx.collections.FXCollections
import javafx.collections.ObservableList
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.collect
import kotlinx.coroutines.flow.flowOn
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

    private var uglySource: UglySource? = null
        set(value) {
            value?.let {
                lastUglySourcePath = it.path.substringBeforeLast("\\")
                uglySourceByteCount.bind(it.byteCountProperty)
            }
            field = value
        }
    val uglySourceByteCount = SimpleLongProperty(0)

    private val logger by LoggerDelegate()

    var lastUglySourcePath: String = System.getProperty("user.home") + "\\Downloads"

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
        uglyBinariesProperty.clear()
        uglySourceByteCount.unbind()
    }

    suspend fun loadUglySourceByUrl(urlStr: String) {
        try {
            uglySource = withContext(Dispatchers.IO) {
                uglyService.loadSourceByUrl(urlStr) { count, length ->
                    launch(Dispatchers.Main) {
                        setProgress("download file...", length, count)
                    }
                }
            }
            loadUglyBinaries(uglySource!!)
        } catch (e: Exception) {
            clearUglySource()
            throw e
        } finally {
            clearProgress()
        }
    }

    suspend fun loadUglySourceByLocalFile(file: File) {
        try {
            uglySource = withContext(Dispatchers.IO) {
                uglyService.loadSource(file.path)
            }
            loadUglyBinaries(uglySource!!)
        } catch (e: Exception) {
            clearUglySource()
        } finally {
            clearProgress()
        }
    }

    private suspend fun loadUglyBinaries(uglySource: UglySource) {
        val length = uglyService.getBinariesCount(uglySource)
        var count = 0
        uglyService
            .loadBinaries(uglySource)
            .flowOn(Dispatchers.IO)
            .collect {
                logger.debug("collect")
                uglyBinariesProperty.add(it)
                setProgress("loading binaries...", length, ++count)
            }
    }

    private fun clearProgress() {
        progressMsgProperty.value = ""
        progressProperty.value = 0.0
    }

    suspend fun saveUglySourceAs(path: String) {
        uglySource?.let {
            withContext(Dispatchers.IO) {
                uglyService.saveAs(it, path)
            }
        }
    }

    suspend fun saveUglySource() {
        uglySource?.let {
            withContext(Dispatchers.IO) {
                uglyService.save(it)
            }
        }
    }

    suspend fun saveUglySourceAs(file: File) {
        saveUglySourceAs(file.path)
    }

    fun sortBy(sortBy: VMRoot.SortBy) {
        uglyBinariesProperty.sortWith(sortBy.comparator)
    }

    suspend fun compress()  {

        var tryCount = 0

        val affectedCount = compressAnyNotJpg()
        if (affectedCount > 0) {
            tryCount++
        }

        compressUntilEnough(tryCount)
    }

    private suspend fun compressUntilEnough(orgTryCount: Int) {
        var tryCount = orgTryCount
        while (!isCompressEnough()) {
            logger.debug("compress by scale!")
            if(compressByScale(++tryCount) == 0) {
                break
            }
        }

        //adjust file byte count
        uglySource?.let {
            tryCount++
            setProgress("($tryCount)adjust byte count..", 1, 0)
            val dir = it.path.substringBeforeLast("\\")
            val tempPath = "$dir\\temp.hwp"
            uglyService.saveAs(uglySource!!, tempPath)

            setProgress("($tryCount)adjust byte count..", 1, 1)

            val tempFile = File(tempPath)
            it.byteCountProperty.value = tempFile.length()
            tempFile.delete()
            if (!isCompressEnough()) {
                compressUntilEnough(++tryCount)
            }
        }
    }

    private fun isCompressEnough() = uglySource!!.byteCountProperty.value <= 5 * 1024 * 1024

    private suspend fun compressByScale(tryCount: Int) : Int {

        val items = uglyBinariesProperty
            .filter { it.isSelectedProperty.value }
            .filter { it.widthProperty.value > 500 && it.heightProperty.value > 500 }

        val length = items.count()
        if (length > 0) {
            val factor = (uglySource!!.byteCountProperty.value - (5 * 1024 * 1024)) / length
            val scale = getScaleFactor(factor)
            logger.debug("scale : $scale, factor :$factor , ${UglyBinary.toBinaryPrefixByteCount(factor)} count : $length")

            coroutineScope {
                var count = 0
                items
                    .asSequence()
                    .map {
                        async(Dispatchers.IO) {
                            logger.debug("uglyService.compressByScale")
                            uglyService.compressByScale(it, scale)
                        }
                    }
                    .forEach {
                        uglySource!!.byteCountProperty.value -= it.await()
                        setProgress("($tryCount)compress images by scale : $scale", length, ++count)
                    }
            }

        }
        return length
    }

    private fun getScaleFactor(factor: Long): Double {
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

    private suspend fun compressAnyNotJpg() : Int {
        val items = uglyBinariesProperty.filter { it.isSelectedProperty.value && it.format != "jpg" }
        val length = items.size

        if (length > 0) {
            var count = 0
            coroutineScope {
                items
                    .asSequence()
                    .map {
                        async(Dispatchers.IO) {
                            logger.debug("uglyService.compressByJpg")
                            uglyService.compressByJpg(it)
                        }
                    }
                    .forEach {
                        withContext(Dispatchers.Main) {
                            uglySource!!.byteCountProperty.value -= it.await()
                            setProgress("(1)changing format of binaries", length, ++count)
                        }
                    }
            }
        }
        return length
    }

    fun isUglySourceVolatile() = uglySource?.isVolatile ?: false
}