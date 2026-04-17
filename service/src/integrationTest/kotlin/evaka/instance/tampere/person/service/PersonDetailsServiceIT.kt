// SPDX-FileCopyrightText: 2021 City of Tampere
//
// SPDX-License-Identifier: LGPL-2.1-or-later

package evaka.instance.tampere.person.service

import com.github.tomakehurst.wiremock.client.WireMock.aResponse
import com.github.tomakehurst.wiremock.client.WireMock.equalTo
import com.github.tomakehurst.wiremock.client.WireMock.post
import com.github.tomakehurst.wiremock.client.WireMock.postRequestedFor
import com.github.tomakehurst.wiremock.client.WireMock.stubFor
import com.github.tomakehurst.wiremock.client.WireMock.urlEqualTo
import com.github.tomakehurst.wiremock.client.WireMock.verify
import evaka.core.identity.ExternalIdentifier
import evaka.core.shared.auth.AuthenticatedUser
import evaka.core.vtjclient.service.persondetails.IPersonDetailsService
import evaka.instance.tampere.AbstractTampereIntegrationTest
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired

internal class PersonDetailsServiceIT : AbstractTampereIntegrationTest() {

    @Autowired lateinit var personDetailsService: IPersonDetailsService

    @Test
    fun getBasicDetailsFor() {
        stubFor(
            post(urlEqualTo("/mock/vtj"))
                .willReturn(
                    aResponse()
                        .withStatus(200)
                        .withHeader("Content-Type", "text/xml")
                        .withBodyFile("person-client/henkilotunnuskysely-response-ok.xml")
                        .withTransformers("response-template")
                        .withTransformerParameter("ssn", "070644-937X")
                )
        )

        val person =
            personDetailsService.getBasicDetailsFor(
                IPersonDetailsService.DetailsQuery(
                    AuthenticatedUser.SystemInternalUser.evakaUserId,
                    ExternalIdentifier.SSN.getInstance("070644-937X"),
                )
            )

        assertThat(person).returns("070644-937X", { it.socialSecurityNumber })
        verify(
            postRequestedFor(urlEqualTo("/mock/vtj"))
                .withoutHeader("Authorization")
                .withHeader("X-API-KEY", equalTo("vtj-kysely-api-key-123"))
                .withHeader("Content-Type", equalTo("text/xml; charset=utf-8"))
                .withHeader("SOAPAction", equalTo("\"\""))
        )
    }
}
