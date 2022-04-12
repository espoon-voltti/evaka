// SPDX-FileCopyrightText: 2017-2021 City of Espoo
//
// SPDX-License-Identifier: LGPL-2.1-or-later

package fi.espoo.evaka.serviceneed

import fi.espoo.evaka.PureJdbiTest
import fi.espoo.evaka.placement.PlacementType
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test

class ServiceNeedQueriesTest : PureJdbiTest(resetDbBeforeEach = true) {
    @Test
    fun getServiceNeedOptionPublicInfos() {
        assertThat(db.read { tx -> tx.getServiceNeedOptionPublicInfos(PlacementType.values().toList()) }).isEmpty()
    }
}
