package com.github.sukury47.leavemealone.models

import javafx.beans.property.IntegerProperty
import javafx.beans.property.SimpleBooleanProperty
import javafx.beans.property.SimpleIntegerProperty
import javafx.beans.property.SimpleStringProperty
import javafx.scene.image.Image
import kr.dogfoot.hwplib.`object`.bindata.EmbeddedBinaryData
import kr.dogfoot.hwplib.`object`.docinfo.BinData
import net.coobird.thumbnailator.Thumbnails
import java.io.ByteArrayInputStream
import java.io.ByteArrayOutputStream
import java.text.StringCharacterIterator
import java.util.concurrent.atomic.AtomicInteger
import kotlin.math.sign

class UglyBinary(private val binary: BinData, private val embeddedBinary: EmbeddedBinaryData) {
    val isSelectedProperty = SimpleBooleanProperty(true)
    val nameProperty = SimpleStringProperty()
    val widthProperty = SimpleIntegerProperty()
    val heightProperty = SimpleIntegerProperty()
    val id: String = embeddedBinary.name.substringBeforeLast(".")
    val byteCountProperty = SimpleStringProperty()

    var format: String
        get() = nameProperty.value.substringAfterLast(".")
        set(value) {
            val prefix = nameProperty.value.substringBeforeLast(".")
            val name = "$prefix.$value"
            nameProperty.value = name
            embeddedBinary.name = name
            binary.extensionForEmbedding = value
        }

    val thumbnail: Image
    var bytes: ByteArray = embeddedBinary.data
        set(value) {
            embeddedBinary.data = value
            byteCountProperty.value = toBinaryPrefixByteCount(value.size.toLong())
            field = value
        }

    init {
        nameProperty.value = embeddedBinary.name
        byteCountProperty.value = toBinaryPrefixByteCount(embeddedBinary.data.size.toLong())
        ByteArrayInputStream(embeddedBinary.data).use {
            val baos = ByteArrayOutputStream()
            Thumbnails.of(it)
                .size(200, 200)
                .keepAspectRatio(true)
                .outputFormat("jpg")
                .toOutputStream(baos)

            thumbnail = Image(ByteArrayInputStream(baos.toByteArray()))
        }

        ByteArrayInputStream(embeddedBinary.data).use {
            val img = Image(it)
            widthProperty.set(img.width.toInt())
            heightProperty.set(img.width.toInt())
        }
    }

    fun isScaleCompressible() = widthProperty.value > 500 || heightProperty.value > 500

    companion object {
        fun toBinaryPrefixByteCount(size: Long): String {
            val absB = if (size == Long.MIN_VALUE) Long.MAX_VALUE else Math.abs(size)
            if (absB < 1024) {
                return "$this B"
            } else {
                var value = absB
                val ci = StringCharacterIterator("KMGTPE")

                var i = 40
                while (i >= 0 && absB > 0xfffccccccccccccL shr i) {
                    value = value shr 10
                    ci.next()
                    i -= 10
                }

                value *= size.sign
                //println(value)
                return String.format("%.1f %ciB", value / 1024.0, ci.current())
            }
        }
    }
}