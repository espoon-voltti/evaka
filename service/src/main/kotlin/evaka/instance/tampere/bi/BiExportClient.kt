// SPDX-FileCopyrightText: 2024 Tampere region
//
// SPDX-License-Identifier: LGPL-2.1-or-later

package evaka.instance.tampere.bi

import evaka.core.shared.domain.EvakaClock
import evaka.instance.espoo.bi.EspooBiJob

interface BiExportClient {
    fun sendBiCsvFile(
        tableName: String,
        clock: EvakaClock,
        stream: EspooBiJob.CsvInputStream,
    ): Pair<String, String>
}
