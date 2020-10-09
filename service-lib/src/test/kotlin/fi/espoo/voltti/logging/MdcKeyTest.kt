// SPDX-FileCopyrightText: 2017-2020 City of Espoo
//
// SPDX-License-Identifier: LGPL-2.1-or-later

package fi.espoo.voltti.logging

import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test
import org.junit.runner.RunWith
import org.slf4j.MDC
import org.springframework.test.context.junit4.SpringRunner

@RunWith(SpringRunner::class)
class MdcKeyTest {
    @After
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
