package fi.espoo.evaka.s3

import fi.espoo.evaka.shared.domain.BadRequest
import java.io.InputStream
import javax.xml.bind.DatatypeConverter

fun checkFileContentType(file: InputStream, contentType: String) {
    val magicNumbers = contentTypesWithMagicNumbers[contentType]
        ?: throw BadRequest("Invalid content type")

    val contentMatchesMagicNumbers = file.use { stream ->
        val fileBytes = stream.readNBytes(magicNumbers.map { it.size }.maxOrNull() ?: 0)
        magicNumbers.any { numbers ->
            fileBytes.slice(numbers.indices).toByteArray().contentEquals(numbers)
        }
    }

    if (contentMatchesMagicNumbers) return
    throw BadRequest("Invalid content type")
}

private val contentTypesWithMagicNumbers = mapOf(
    "image/jpeg" to listOf("FF D8 FF DB", "FF D8 FF EE", "FF D8 FF E1", "FF D8 FF E0 00 10 4A 46 49 46 00 01"),
    "image/png" to listOf("89 50 4E 47 0D 0A 1A 0A"),
    "application/pdf" to listOf("25 50 44 46 2D"),
    "application/msword" to listOf("50 4B 03 04 14 00 06 00", "D0 CF 11 E0 A1 B1 1A E1"),
    "application/vnd.openxmlformats-officedocument.wordprocessingml.document" to listOf("50 4B 03 04"),
    "application/vnd.oasis.opendocument.text" to listOf("50 4B 03 04")
).mapValues { (_, magicNumbers) ->
    magicNumbers.map { hex -> DatatypeConverter.parseHexBinary(hex.replace(" ", "")) }
}
