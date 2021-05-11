package com.github.sukury47.leavemealone.models

import javafx.beans.property.IntegerProperty
import javafx.beans.property.SimpleIntegerProperty
import javafx.beans.property.SimpleStringProperty
import javafx.scene.image.Image
import kr.dogfoot.hwplib.`object`.bindata.EmbeddedBinaryData
import kr.dogfoot.hwplib.`object`.docinfo.BinData
import net.coobird.thumbnailator.Thumbnails
import java.io.ByteArrayInputStream
import java.io.ByteArrayOutputStream
import java.util.concurrent.atomic.AtomicInteger

class UglyBinary(private val binary: BinData, private val embeddedBinary: EmbeddedBinaryData) {
    val nameProperty = SimpleStringProperty()
    val widthProperty = SimpleIntegerProperty()
    val heightProperty = SimpleIntegerProperty()

    var format: String
        get() = nameProperty.value.substringAfterLast(".")
        set(value) {
            val prefix = nameProperty.value.substringBeforeLast(".")
            val name = "$prefix.$value"
            nameProperty.value = name
            binary.extensionForEmbedding = value
        }

    val thumbnail: Image

    init {
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
}