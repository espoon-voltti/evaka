// SPDX-FileCopyrightText: 2021 City of Oulu
//
// SPDX-License-Identifier: LGPL-2.1-or-later

package fi.espoo.evaka.oulu.database

import fi.espoo.evaka.oulu.AbstractIntegrationTest
import org.jdbi.v3.core.Jdbi
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired

class DevDataInitializerTest : AbstractIntegrationTest() {
    @Autowired private lateinit var jdbi: Jdbi

    @Test
    fun init() {
        DevDataInitializer(jdbi)
    }
}
