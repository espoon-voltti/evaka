// SPDX-FileCopyrightText: 2017-2020 City of Espoo
//
// SPDX-License-Identifier: LGPL-2.1-or-later

package fi.espoo.evaka.daycare.dao

import fi.espoo.evaka.daycare.AbstractIntegrationTest
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import java.util.UUID

class DaycareDAOIntegrationTest : AbstractIntegrationTest() {
    @Autowired
    lateinit var daycareDAO: DaycareDAO

    @Test
    fun `get daycare manager returns results`() {
        val expectedDaycareId = UUID.fromString("68851e10-eb86-443e-b28d-0f6ee9642a3c")
        val expectedName = "Minna Manageri"

        val manager = daycareDAO.getDaycareManager(expectedDaycareId)

        assertThat(manager!!.name).isEqualTo(expectedName)
    }

    @Test
    fun `get daycare manager returns results for a daycare with some null fields in the manager`() {
        val expectedDaycareId = UUID.fromString("ba6fa7cb-7dfa-4629-b0c1-ae3a721c8a91")
        val expectedName = ""

        val manager = daycareDAO.getDaycareManager(expectedDaycareId)

        assertThat(manager!!.name).isEqualTo(expectedName)
    }
}
