// SPDX-FileCopyrightText: 2017-2023 City of Espoo
//
// SPDX-License-Identifier: LGPL-2.1-or-later

package fi.espoo.evaka.espoo.bi

interface EspooBiClient {
    fun sendBiCsvFile(
        fileName: String,
        stream: EspooBiJob.CsvInputStream
    )
}
