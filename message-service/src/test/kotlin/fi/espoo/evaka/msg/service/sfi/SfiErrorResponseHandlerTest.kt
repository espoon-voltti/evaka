// SPDX-FileCopyrightText: 2017-2020 City of Espoo
//
// SPDX-License-Identifier: LGPL-2.1-or-later

package fi.espoo.evaka.msg.service.sfi

import fi.espoo.evaka.msg.service.sfi.SfiErrorResponseHandler.SFiMessageDeliveryException
import org.assertj.core.api.Assertions.assertThat
import org.assertj.core.api.Assertions.fail
import org.junit.Test
import org.junit.runner.RunWith
import org.mockito.InjectMocks
import org.mockito.junit.MockitoJUnitRunner

@RunWith(MockitoJUnitRunner::class)
class SfiErrorResponseHandlerTest {

    @InjectMocks
    lateinit var defaultHandler: DefaultSfiErrorResponseHandler

    @InjectMocks
    lateinit var ignoringHandler: IgnoreSpecificErrorsHandler

    @Test
    fun `sending an error to default error handler should throw an exception`() {

        val error = SfiResponse(code = 512, text = "Some error 4442")
        try {
            defaultHandler.handleError(error)
            @Suppress("UNREACHABLE_CODE")
            fail<Nothing>("Expecting an exception")
        } catch (e: Exception) {
            assertThat(e).isExactlyInstanceOf(SFiMessageDeliveryException::class.java)
            assertThat(e).hasMessage("SFI message delivery failed with code ${error.code}: ${error.text}")
        }
    }

    @Test
    fun `sending a non-account exists error to ignoring handler should throw an exception`() {

        val error = SfiResponse(code = 512, text = "Some error 4442")
        try {
            ignoringHandler.handleError(error)
            fail<Nothing>("Expecting an exception")
        } catch (e: Exception) {
            assertThat(e).isExactlyInstanceOf(SFiMessageDeliveryException::class.java)
            assertThat(e).hasMessage("SFI message delivery failed with code ${error.code}: ${error.text}")
        }
    }

    @Test
    fun `sending an account does not exist error to ignoring handler should NOT throw an exception`() {

        val error = SfiResponse(code = ERROR_SFI_ACCOUNT_NOT_FOUND, text = "Tiliä ei löydy")
        try {
            ignoringHandler.handleError(error)
        } catch (e: Exception) {
            fail<Nothing>("Error should have been handled without throwing an exception")
        }
    }
}
