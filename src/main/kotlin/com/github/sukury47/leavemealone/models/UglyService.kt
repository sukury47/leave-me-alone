package com.github.sukury47.leavemealone.models

import kotlinx.coroutines.flow.Flow
import kr.dogfoot.hwplib.`object`.HWPFile

interface UglyService {
    suspend fun loadSource(path: String): UglySource
    suspend fun loadSourceByUrl(urlStr: String, progress: (count: Int, length: Int) -> Unit) : UglySource
    suspend fun loadBinaries(source: UglySource): Flow<UglyBinary>
    suspend fun getBinariesCount(source: UglySource): Int
    suspend fun compressByJpg(binary: UglyBinary): Long
    suspend fun compressByScale(binary: UglyBinary, scale: Double): Long
    suspend fun saveAs(source: UglySource, path: String)
    suspend fun save(source: UglySource)
}