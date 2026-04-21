// SPDX-FileCopyrightText: 2017-2023 City of Espoo
//
// SPDX-License-Identifier: LGPL-2.1-or-later

package evaka.core.bi

import java.io.InputStream
import java.nio.ByteBuffer
import java.nio.charset.Charset

class CsvInputStream(private val charset: Charset, records: Sequence<String>) : InputStream() {
    var totalBytes: Int = 0
        private set

    private val iterator = records.iterator()
    private var buffer: ByteBuffer? = null

    private fun acquireBuffer(): ByteBuffer? {
        if (buffer?.hasRemaining() != true) {
            buffer = null
            while (iterator.hasNext()) {
                val bytes = iterator.next().toByteArray(charset)
                if (bytes.isNotEmpty()) {
                    buffer = ByteBuffer.wrap(bytes)
                    break
                }
            }
        }
        return buffer
    }

    override fun read(): Int =
        when (val buffer = acquireBuffer()) {
            null -> -1

            // end of stream
            else -> buffer.get().toInt().also { totalBytes += 1 }
        }

    override fun available(): Int = acquireBuffer()?.remaining() ?: 0
}
