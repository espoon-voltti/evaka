// SPDX-FileCopyrightText: 2017-2024 City of Espoo
//
// SPDX-License-Identifier: LGPL-2.1-or-later

package fi.espoo.evaka.jamix

import fi.espoo.evaka.specialdiet.JamixSpecialDiet
import fi.espoo.evaka.specialdiet.JamixSpecialDietFields
import fi.espoo.evaka.specialdiet.SpecialDiet
import kotlin.test.Test
import kotlin.test.assertEquals

class JamixServiceTest {
    @Test
    fun `cleanupJamixDietList retains normal entries`() {
        val testData = listOf(JamixSpecialDiet(1, JamixSpecialDietFields("Foobar", "Foo")))
        val result = cleanupJamixDietList(testData)
        assertEquals(setOf(SpecialDiet(1, "Foobar", "Foo")), result.toSet())
    }

    @Test
    fun `cleanupJamixDietList strips whitespace from entries`() {
        val testData =
            listOf(
                JamixSpecialDiet(1, JamixSpecialDietFields("tsekattava   \nFoobar  ", "ätarkastaFoo"))
            )
        val result = cleanupJamixDietList(testData)
        assertEquals(setOf(SpecialDiet(1, "tsekattava Foobar", "ätarkastaFoo")), result.toSet())
    }
}
