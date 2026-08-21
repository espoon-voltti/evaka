// SPDX-FileCopyrightText: 2017-2026 City of Espoo
//
// SPDX-License-Identifier: LGPL-2.1-or-later

package evaka.core.user

import kotlin.test.Test
import kotlin.test.assertEquals

class UserAgentParserTest {
    private val parser = UserAgentParser()

    @Test
    fun `Safari on macOS`() {
        assertEquals(
            ParsedUserAgent(DeviceClass.DESKTOP, "macOS", "Safari"),
            parser.parse(
                "Mozilla/5.0 (Macintosh; Intel Mac OS X 10_15_7) AppleWebKit/605.1.15 (KHTML, like Gecko) Version/18.3 Safari/605.1.15"
            ),
        )
    }

    @Test
    fun `Chrome on ChromeOS`() {
        assertEquals(
            ParsedUserAgent(DeviceClass.DESKTOP, "ChromeOS", "Chrome"),
            parser.parse(
                "Mozilla/5.0 (X11; CrOS x86_64 14541.0.0) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/132.0.0.0 Safari/537.36"
            ),
        )
    }

    @Test
    fun `Edge on Windows`() {
        assertEquals(
            ParsedUserAgent(DeviceClass.DESKTOP, "Windows", "Edge"),
            parser.parse(
                "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/132.0.0.0 Safari/537.36 Edg/132.0.0.0"
            ),
        )
    }

    @Test
    fun `Safari on an iPhone`() {
        assertEquals(
            ParsedUserAgent(DeviceClass.PHONE, "iOS", "Safari"),
            parser.parse(
                "Mozilla/5.0 (iPhone; CPU iPhone OS 18_3 like Mac OS X) AppleWebKit/605.1.15 (KHTML, like Gecko) Version/18.3 Mobile/15E148 Safari/604.1"
            ),
        )
    }

    @Test
    fun `Chrome on an iPad`() {
        assertEquals(
            ParsedUserAgent(DeviceClass.TABLET, "iOS", "Chrome"),
            parser.parse(
                "Mozilla/5.0 (iPad; CPU OS 18_3 like Mac OS X) AppleWebKit/605.1.15 (KHTML, like Gecko) CriOS/132.0.0.0 Mobile/15E148 Safari/604.1"
            ),
        )
    }

    @Test
    fun `Chrome on an Android phone`() {
        assertEquals(
            ParsedUserAgent(DeviceClass.PHONE, "Android", "Chrome"),
            parser.parse(
                "Mozilla/5.0 (Linux; Android 15; Pixel 9) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/132.0.0.0 Mobile Safari/537.36"
            ),
        )
    }

    @Test
    fun `Chrome on an Android tablet, which omits Mobile`() {
        assertEquals(
            ParsedUserAgent(DeviceClass.TABLET, "Android", "Chrome"),
            parser.parse(
                "Mozilla/5.0 (Linux; Android 14; SM-X710) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/132.0.0.0 Safari/537.36"
            ),
        )
    }

    @Test
    fun `a user agent that identifies nothing`() {
        assertEquals(
            ParsedUserAgent(DeviceClass.UNKNOWN, "", ""),
            parser.parse("something that is not a user agent"),
        )
    }

    @Test
    fun `a missing user agent header`() {
        assertEquals(ParsedUserAgent(DeviceClass.UNKNOWN, "", ""), parser.parse(null))
    }
}
