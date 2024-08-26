// SPDX-FileCopyrightText: 2017-2020 City of Espoo
//
// SPDX-License-Identifier: LGPL-2.1-or-later

package fi.espoo.voltti.logging

import kotlin.test.assertEquals
import kotlin.test.assertNull
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.Test
import org.slf4j.MDC

class MdcKeyTest {
    @AfterEach
    fun afterEach() {
        MDC.clear()
    }

    @Test
    fun `setting key to MDC`() {
        val testValue = "TestValue"
        MdcKey.REQ_IP.set(testValue)
        assertEquals(testValue, MDC.get(MdcKey.REQ_IP.key))
    }

    @Test
    fun `unsetting non set key`() {
        MdcKey.REQ_IP.unset()
        assertNull(MDC.get(MdcKey.REQ_IP.key))
    }

    @Test
    fun `unsetting previously set key`() {
        MdcKey.REQ_IP.set("no empty value")
        MdcKey.REQ_IP.unset()
        assertNull(MDC.get(MdcKey.REQ_IP.key))
    }
}
