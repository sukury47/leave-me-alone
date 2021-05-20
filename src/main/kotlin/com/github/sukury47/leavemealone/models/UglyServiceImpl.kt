package com.github.sukury47.leavemealone.models

import com.github.sukury47.leavemealone.LoggerDelegate
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.*
import kr.dogfoot.hwplib.`object`.bindata.EmbeddedBinaryData
import kr.dogfoot.hwplib.`object`.docinfo.BinData
import kr.dogfoot.hwplib.writer.HWPWriter
import net.coobird.thumbnailator.Thumbnails
import java.io.ByteArrayInputStream
import java.io.ByteArrayOutputStream
import java.io.File
import java.lang.IllegalArgumentException

class UglyServiceImpl : UglyService {
    private val logger by LoggerDelegate()
    override suspend fun loadSource(path: String): UglySource = withContext(Dispatchers.IO) {
        UglySource.instanceByLocalFile(File(path))
    }

    override suspend fun loadSourceByUrl(urlStr: String, progress: (count: Int, length: Int) -> Unit): UglySource  {
        return UglySource.instanceByUrl(urlStr, progress)
    }

    override suspend fun getBinariesCount(source: UglySource) = source.binary.binData.embeddedBinaryDataList.size

    @OptIn(FlowPreview::class)
    override suspend fun loadBinaries(source: UglySource): Flow<UglyBinary> {
        val binariesBySuffix = mutableMapOf<String, BinData>()
        source.binary.docInfo.binDataList.forEach {
            binariesBySuffix[String.format("BIN%04X", it.binDataID)] = it
        }

        val embeddedBinaries = source.binary.binData.embeddedBinaryDataList

        fun sibal(embeddedBinary: EmbeddedBinaryData) = flow {
            val hex = embeddedBinary.name.substringBeforeLast(".")
            val binary = binariesBySuffix[hex]!!
            logger.debug("UglyBinary()")
            emit(UglyBinary(binary, embeddedBinary))
        }

        logger.debug("size: ${embeddedBinaries.size}")

        return embeddedBinaries
            .asFlow()
            .flatMapMerge {
                sibal(it)
            }
    }

    override suspend fun compressByJpg(binary: UglyBinary): Long {
        if (binary.format == "jpg") {
            throw IllegalArgumentException()
        } else {
            val orgLength = binary.bytes.size.toLong()
            ByteArrayInputStream(binary.bytes).use {
                val baos = ByteArrayOutputStream()
                Thumbnails.of(it)
                    .scale(1.0)
                    .outputFormat("jpg")
                    .toOutputStream(baos)

                withContext(Dispatchers.Main) {
                    binary.bytes = baos.toByteArray()
                    binary.format = "jpg"
                }
            }
            val compressedLength = binary.bytes.size.toLong()
            return orgLength - compressedLength
        }
    }

    override suspend fun compressByScale(binary: UglyBinary, scale: Double): Long {
        val orgLength = binary.bytes.size.toLong()
        ByteArrayInputStream(binary.bytes).use {
            val baos = ByteArrayOutputStream()
            Thumbnails.of(it)
                .scale(scale)
                .toOutputStream(baos)

            withContext(Dispatchers.Main) {
                binary.bytes = baos.toByteArray()
                binary.widthProperty.value = (scale * binary.widthProperty.value).toInt()
                binary.heightProperty.value = (scale * binary.heightProperty.value).toInt()
            }
        }
        return orgLength - binary.bytes.size.toLong()
    }

    override suspend fun saveAs(source: UglySource, path: String) {
        HWPWriter.toFile(source.binary, path)
    }

    override suspend fun save(source: UglySource) {
        HWPWriter.toFile(source.binary, source.path)
    }
}