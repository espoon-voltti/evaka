// SPDX-FileCopyrightText: 2017-2026 City of Espoo
//
// SPDX-License-Identifier: LGPL-2.1-or-later

package evaka.instance.turku.dw

import evaka.core.bi.CsvInputStream
import evaka.core.shared.domain.EvakaClock

interface DwExportClient {
    fun sendDwCsvFile(queryName: String, clock: EvakaClock, stream: CsvInputStream)
}
