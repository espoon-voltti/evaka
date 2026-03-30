// SPDX-FileCopyrightText: 2017-2026 City of Espoo
//
// SPDX-License-Identifier: LGPL-2.1-or-later

package fi.espoo.evaka.oulu.dw

import fi.espoo.evaka.espoo.bi.EspooBiJob
import fi.espoo.evaka.shared.domain.EvakaClock

interface DwExportClient {
    fun sendDwCsvFile(
        queryName: String,
        clock: EvakaClock,
        stream: EspooBiJob.CsvInputStream,
        fileNamePrefix: String,
    ): Pair<String, String>
}
