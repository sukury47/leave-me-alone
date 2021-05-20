package com.github.sukury47.leavemealone.models

import javafx.beans.property.SimpleLongProperty
import kr.dogfoot.hwplib.`object`.HWPFile
import kr.dogfoot.hwplib.reader.HWPReader
import java.io.BufferedInputStream
import java.io.File
import java.io.FileOutputStream
import java.net.URL

class UglySource(val path: String, byteCount: Long, val binary: HWPFile) {
    val byteCountProperty = SimpleLongProperty(byteCount)

    val isVolatile: Boolean
        get() = path.substringAfterLast(File.separator).startsWith("fromUrl_")

    companion object {
        fun instanceByLocalFile(file: File) : UglySource {
            val binary = HWPReader.fromFile(file.path)
            return UglySource(file.path, file.length(), binary)
        }

        fun instanceByLocalPath(path: String) : UglySource {
            return instanceByLocalFile(File(path))
        }

        suspend fun instanceByUrl(urlStr: String, progress: (count: Int, length: Int) -> Unit) : UglySource {
            val url = URL(urlStr)
            val connection = url.openConnection()
            connection.connect()
            val length = connection.contentLength
            BufferedInputStream(url.openStream()).use {
                val path = "${System.getProperty("user.home")}\\Downloads\\fromUrl_${System.currentTimeMillis()}.hwp"
                val fos = FileOutputStream(path)
                val buffer = ByteArray(1024)
                var totalBytesRead = 0
                var bytesRead = 0
                while (true) {
                    bytesRead = it.read(buffer, 0, 1024)
                    if (bytesRead == -1) {
                        break
                    } else {
                        fos.write(buffer, 0, bytesRead)
                        totalBytesRead += bytesRead
                        progress(totalBytesRead, length)
                        //delay(100)
                    }
                }
                fos.close()
                val instance = instanceByLocalPath(path)
                File(path).delete()
                return instance
            }
        }
    }
}