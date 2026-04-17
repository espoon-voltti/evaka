// SPDX-FileCopyrightText: 2021 City of Tampere
//
// SPDX-License-Identifier: LGPL-2.1-or-later

package evaka.instance.tampere.person.service

import com.github.tomakehurst.wiremock.client.WireMock.aResponse
import com.github.tomakehurst.wiremock.client.WireMock.equalTo
import com.github.tomakehurst.wiremock.client.WireMock.get
import com.github.tomakehurst.wiremock.client.WireMock.getRequestedFor
import com.github.tomakehurst.wiremock.client.WireMock.post
import com.github.tomakehurst.wiremock.client.WireMock.postRequestedFor
import com.github.tomakehurst.wiremock.client.WireMock.stubFor
import com.github.tomakehurst.wiremock.client.WireMock.urlEqualTo
import com.github.tomakehurst.wiremock.client.WireMock.verify
import evaka.core.dvv.DvvModificationsServiceClient
import evaka.instance.tampere.AbstractTampereIntegrationTest
import java.time.LocalDate
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired

internal class DvvModificationsServiceClientIT : AbstractTampereIntegrationTest() {

    @Autowired lateinit var dvvModificationsServiceClient: DvvModificationsServiceClient

    @Test
    fun getFirstModificationToken() {
        stubFor(
            get(urlEqualTo("/mock/modifications/kirjausavain/2021-04-01"))
                .willReturn(
                    aResponse()
                        .withStatus(200)
                        .withHeader("Content-Type", "application/json")
                        .withBodyFile(
                            "modifications-client/first-modification-token-response-ok.json"
                        )
                )
        )

        val response =
            dvvModificationsServiceClient.getFirstModificationToken(LocalDate.of(2021, 4, 1))
        assertThat(response).returns(5446623423) { it?.latestModificationToken }

        verify(
            getRequestedFor(urlEqualTo("/mock/modifications/kirjausavain/2021-04-01"))
                .withHeader("Authorization", equalTo("Basic dXNlcjpwYXNz"))
                .withHeader("X-API-KEY", equalTo("vtj-mutpa-api-key-123"))
        )
    }

    @Test
    fun getModifications() {
        stubFor(
            post(urlEqualTo("/mock/modifications/muutokset"))
                .willReturn(
                    aResponse()
                        .withStatus(200)
                        .withHeader("Content-Type", "application/json")
                        .withBodyFile("modifications-client/modifications-response-ok.json")
                )
        )

        val response = dvvModificationsServiceClient.getModifications("5446623423", listOf())
        assertThat(response).returns("3494393") { it.viimeisinKirjausavain }

        verify(
            postRequestedFor(urlEqualTo("/mock/modifications/muutokset"))
                .withHeader("Authorization", equalTo("Basic dXNlcjpwYXNz"))
                .withHeader("X-API-KEY", equalTo("vtj-mutpa-api-key-123"))
        )
    }
}
