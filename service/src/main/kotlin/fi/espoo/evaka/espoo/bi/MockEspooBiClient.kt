// SPDX-FileCopyrightText: 2017-2023 City of Espoo
//
// SPDX-License-Identifier: LGPL-2.1-or-later

package fi.espoo.evaka.espoo.bi

import com.google.common.io.ByteStreams
import mu.KotlinLogging

class MockEspooBiClient : EspooBiClient {
    private val logger = KotlinLogging.logger {}

    override fun sendBiCsvFile(
        fileName: String,
        stream: EspooBiJob.CsvInputStream
    ) = stream.use {
        val byteCount = ByteStreams.exhaust(stream)
        logger.info { "Mock BI client ignored $fileName ($byteCount bytes)" }
    }
}
