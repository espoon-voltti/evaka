// SPDX-FileCopyrightText: 2017-2026 City of Espoo
//
// SPDX-License-Identifier: LGPL-2.1-or-later
package evaka.core.reports

import evaka.core.koski.OpiskeluoikeudenTyyppiKoodi
import kotlin.test.Test
import kotlin.test.assertEquals

class KoskiErrorReportTest {
    @Test
    fun `KoskiStudyRightType mirrors OpiskeluoikeudenTyyppiKoodi so the report can map the db enum by name`() {
        assertEquals(
            OpiskeluoikeudenTyyppiKoodi.entries.map { it.name },
            KoskiStudyRightType.entries.map { it.name },
        )
    }
}
