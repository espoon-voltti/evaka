// SPDX-FileCopyrightText: 2017-2025 City of Espoo
//
// SPDX-License-Identifier: LGPL-2.1-or-later

package fi.espoo.evaka.aromi

import fi.espoo.evaka.AromiEnv
import fi.espoo.evaka.FullApplicationTest
import fi.espoo.evaka.shared.domain.MockEvakaClock
import fi.espoo.evaka.shared.sftp.SftpClient
import java.nio.charset.StandardCharsets
import kotlin.test.assertEquals
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired

class AromiServiceTest : FullApplicationTest(resetDbBeforeEach = true) {
    @Autowired private lateinit var aromiService: AromiService
    @Autowired private lateinit var aromiEnv: AromiEnv

    @Test
    fun `sendOrders uploads csv to sftp server`() {
        val clock = MockEvakaClock(2025, 3, 3, 10, 53, 22)
        aromiService.sendOrders(db, clock)

        val sftpClient = SftpClient(aromiEnv.sftp)
        val data = sftpClient.getAsString("upload/EVAKA03032025.csv", StandardCharsets.ISO_8859_1)
        assertEquals("", data)
    }
}
