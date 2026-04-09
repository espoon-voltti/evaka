// SPDX-FileCopyrightText: 2023 Tampere region
//
// SPDX-License-Identifier: LGPL-2.1-or-later

package evaka.instance.tampere.idm

import evaka.core.shared.AreaId
import evaka.core.shared.DaycareId
import evaka.core.shared.auth.UserRole
import evaka.core.shared.auth.insertDaycareAclRow
import evaka.core.shared.dev.DevDaycare
import evaka.core.shared.dev.DevEmployee
import evaka.core.shared.dev.insert
import evaka.core.shared.domain.HelsinkiDateTime
import evaka.instance.tampere.AbstractTampereIntegrationTest
import evaka.instance.tampere.export.ExportUnitsAclService
import java.nio.charset.StandardCharsets
import java.time.LocalDate
import java.time.LocalTime
import java.util.*
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired

class ExportUnitsAclServiceTest : AbstractTampereIntegrationTest() {

    @Autowired private lateinit var exportUnitsAclService: ExportUnitsAclService

    @Test
    fun exportUnitsAcl() {
        val timestamp = HelsinkiDateTime.of(LocalDate.of(2023, 12, 14), LocalTime.of(16, 33))
        db.transaction { tx ->
            val unitId =
                tx.insert(
                    DevDaycare(
                        id = DaycareId(UUID.fromString("4e042a2e-f2d3-11ec-b2d6-1bf5942d79f4")),
                        name = "Sammon koulun esiopetus",
                        areaId = AreaId(UUID.fromString("6529f5a2-9777-11eb-ba89-cfcda122ed3b")),
                    )
                )
            val employeeId =
                tx.insert(
                    DevEmployee(
                        firstName = "Leena",
                        lastName = "Testi",
                        email = "leena.testi@tampere.fi",
                        employeeNumber = "356751",
                    )
                )
            tx.insertDaycareAclRow(
                daycareId = unitId,
                employeeId = employeeId,
                role = UserRole.UNIT_SUPERVISOR,
            )
        }

        val (bucket, key) =
            db.transaction { tx -> exportUnitsAclService.exportUnitsAcl(tx, timestamp) }

        assertThat(key).isEqualTo("reporting/acl/tampere_evaka_acl_20231214.csv")
        val (data, contentType) =
            getS3Object(bucket, key).use {
                it.readAllBytes().toString(StandardCharsets.UTF_8) to it.response().contentType()
            }
        assertEquals(EXPECTED, data)
        assertEquals("text/csv", contentType)
    }
}

private const val EXPECTED =
    """unit_id;unit_name;first_name;last_name;email;employee_number
4e042a2e-f2d3-11ec-b2d6-1bf5942d79f4;Sammon koulun esiopetus;Leena;Testi;leena.testi@tampere.fi;356751
"""
