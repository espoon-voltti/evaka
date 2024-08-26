// SPDX-FileCopyrightText: 2017-2021 City of Espoo
//
// SPDX-License-Identifier: LGPL-2.1-or-later

package fi.espoo.evaka.vasu

import fi.espoo.evaka.PureJdbiTest
import fi.espoo.evaka.shared.domain.FiniteDateRange
import fi.espoo.evaka.shared.domain.OfficialLanguage
import java.time.LocalDate
import kotlin.test.assertEquals
import org.junit.jupiter.api.Test

class VasuTemplateQueriesTest : PureJdbiTest(resetDbBeforeEach = true) {
    @Test
    fun `vasu template can be created and updated`() {
        db.transaction { tx ->
            // when a template is created
            val dateRangeNow = FiniteDateRange(LocalDate.now(), LocalDate.now())
            val templateId =
                tx.insertVasuTemplate(
                    "foo",
                    dateRangeNow,
                    CurriculumType.DAYCARE,
                    OfficialLanguage.FI,
                    VasuContent(sections = listOf()),
                )
            assertEquals(
                VasuTemplate(
                    templateId,
                    "foo",
                    CurriculumType.DAYCARE,
                    dateRangeNow,
                    OfficialLanguage.FI,
                    VasuContent(sections = listOf()),
                    0,
                ),
                tx.getVasuTemplate(templateId),
            )

            // when a template is updated
            val modifiedDateRange = dateRangeNow.copy(start = LocalDate.now().minusYears(1))
            tx.updateVasuTemplate(templateId, VasuTemplateUpdate("bar", modifiedDateRange))
            assertEquals(
                VasuTemplate(
                    templateId,
                    "bar",
                    CurriculumType.DAYCARE,
                    modifiedDateRange,
                    OfficialLanguage.FI,
                    VasuContent(sections = listOf()),
                    0,
                ),
                tx.getVasuTemplate(templateId),
            )
        }
    }
}
