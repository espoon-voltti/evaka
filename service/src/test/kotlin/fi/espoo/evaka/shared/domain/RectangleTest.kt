// SPDX-FileCopyrightText: 2017-2024 City of Espoo
//
// SPDX-License-Identifier: LGPL-2.1-or-later

package fi.espoo.evaka.shared.domain

import kotlin.test.assertEquals
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertThrows

class RectangleTest {
    @Test
    fun fromString() {
        assertEquals(Rectangle(1, 2, 345, 6), Rectangle.fromString("1,2,345,6"))
        assertThrows<IllegalArgumentException> { Rectangle.fromString("foobar") }
        assertThrows<IllegalArgumentException> { Rectangle.fromString("1,2,3") }
        assertThrows<IllegalArgumentException> { Rectangle.fromString("foo,bar,baz,quux") }
    }
}
