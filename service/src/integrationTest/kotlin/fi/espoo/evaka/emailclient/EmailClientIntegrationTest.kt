// SPDX-FileCopyrightText: 2017-2020 City of Espoo
//
// SPDX-License-Identifier: LGPL-2.1-or-later

package fi.espoo.evaka.emailclient

import fi.espoo.evaka.FullApplicationTest
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired

class EmailClientIntegrationTest : FullApplicationTest() {
    @Autowired
    lateinit var emailClient: IEmailClient

    @Test
    fun `email validation works`() {
    }
}
