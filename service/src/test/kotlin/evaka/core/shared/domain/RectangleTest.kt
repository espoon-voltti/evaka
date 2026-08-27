// SPDX-FileCopyrightText: 2017-2024 City of Espoo
//
// SPDX-License-Identifier: LGPL-2.1-or-later

package evaka.core.shared.domain

import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import org.junit.jupiter.api.Test

class RectangleTest {
    @Test
    fun fromString() {
        assertEquals(Rectangle(1, 2, 345, 6), Rectangle.fromString("1,2,345,6"))
        assertFailsWith<IllegalArgumentException> { Rectangle.fromString("foobar") }
        assertFailsWith<IllegalArgumentException> { Rectangle.fromString("1,2,3") }
        assertFailsWith<IllegalArgumentException> { Rectangle.fromString("foo,bar,baz,quux") }
    }
}
