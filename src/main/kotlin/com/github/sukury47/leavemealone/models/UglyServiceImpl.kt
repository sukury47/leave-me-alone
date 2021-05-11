package com.github.sukury47.leavemealone.models

import kotlinx.coroutines.*
import kotlinx.coroutines.flow.asFlow
import kr.dogfoot.hwplib.`object`.HWPFile
import kr.dogfoot.hwplib.`object`.docinfo.BinData
import kr.dogfoot.hwplib.reader.HWPReader

class UglyServiceImpl : UglyService {
    override suspend fun loadSource(path: String): HWPFile = withContext(Dispatchers.IO) {
        HWPReader.fromFile(path)
    }

    @OptIn(ObsoleteCoroutinesApi::class)
    override suspend fun loadBinaries(source: HWPFile): List<UglyBinary> = withContext(Dispatchers.IO) {
        val binariesBySuffix = mutableMapOf<String, BinData>()
        source.docInfo.binDataList.forEach {
            binariesBySuffix[String.format("BIN%04X", it.binDataID)] = it
        }

        val embeddedBinaries = source.binData.embeddedBinaryDataList

        val items = mutableListOf<UglyBinary>()
        val accContext = newSingleThreadContext("acc")

        embeddedBinaries
            .asSequence()
            .map {
                val hex = it.name.substringBeforeLast(".")
                val binary = binariesBySuffix[hex]!!
                async {
                    UglyBinary(binary, it)
                }
            }.forEach {
                withContext(accContext) {
                    items.add(it.await())
                }
            }
        items
    }
}