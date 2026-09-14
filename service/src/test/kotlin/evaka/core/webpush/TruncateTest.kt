// SPDX-FileCopyrightText: 2017-2026 City of Espoo
//
// SPDX-License-Identifier: LGPL-2.1-or-later

package evaka.core.webpush

import kotlin.test.assertEquals
import org.junit.jupiter.api.Test

class TruncateTest {
    @Test
    fun `text at the limit is not cut`() {
        assertEquals("abcde", truncate("abcde", 5))
    }

    @Test
    fun `long text is cut to the limit with an ellipsis`() {
        assertEquals("abcd…", truncate("abcdef", 5))
    }

    @Test
    fun `an emoji counts as one character and is never cut in half`() {
        // Each emoji is two UTF-16 chars, so counting or cutting by chars would give wrong results
        assertEquals("😀😀😀😀", truncate("😀😀😀😀", 4))
        assertEquals("😀😀😀…", truncate("😀😀😀😀😀", 4))
    }
}
