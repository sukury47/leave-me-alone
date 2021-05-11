package com.github.sukury47.leavemealone.models

import kr.dogfoot.hwplib.`object`.HWPFile

interface UglyService {
    suspend fun loadSource(path: String) : HWPFile
    suspend fun loadBinaries(source: HWPFile) : List<UglyBinary>
}