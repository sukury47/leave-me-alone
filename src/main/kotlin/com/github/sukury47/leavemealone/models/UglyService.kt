package com.github.sukury47.leavemealone.models

import kotlinx.coroutines.flow.Flow
import kr.dogfoot.hwplib.`object`.HWPFile

interface UglyService {
    suspend fun loadSource(path: String): HWPFile
    suspend fun loadBinaries(source: HWPFile): Flow<UglyBinary>
    suspend fun compressByJpg(binary: UglyBinary): Long
    suspend fun compressByScale(binary: UglyBinary, scale: Float): UglyBinary
}