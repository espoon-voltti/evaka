// SPDX-FileCopyrightText: 2021-2023 City of Tampere
//
// SPDX-License-Identifier: LGPL-2.1-or-later

package fi.espoo.evaka.titania

import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.CsvSource

class TitaniaPersonTest {
    @ParameterizedTest
    @CsvSource(
        value =
            [
                "ANKKA IINES,IINES,ANKKA",
                "ANKKA,'',ANKKA",
                "ANKKA PROFESSORI TAAVI,PROFESSORI TAAVI,ANKKA",
                "'ANKKA ','',ANKKA",
                "' ANKKA',ANKKA,''",
                "'','',''"
            ]
    )
    fun firstNameAndLastName(
        name: String,
        expectedFirstName: String,
        expectedLastName: String
    ) {
        val person =
            TitaniaPerson(
                employeeId = "123456",
                name = name,
                actualWorkingTimeEvents = TitaniaWorkingTimeEvents(event = emptyList()),
                payrollItems = null
            )

        assertThat(person)
            .extracting({ it.firstName() }, { it.lastName() })
            .containsExactly(expectedFirstName, expectedLastName)
    }
}
