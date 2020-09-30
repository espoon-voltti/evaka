// SPDX-FileCopyrightText: 2017-2020 City of Espoo
//
// SPDX-License-Identifier: LGPL-2.1-or-later

package fi.espoo.evaka.daycare.controllers

import com.nhaarman.mockito_kotlin.whenever
import fi.espoo.evaka.daycare.CareArea
import fi.espoo.evaka.daycare.CareType
import fi.espoo.evaka.daycare.Location
import fi.espoo.evaka.daycare.MailingAddress
import fi.espoo.evaka.daycare.VisitingAddress
import fi.espoo.evaka.daycare.domain.ProviderType
import fi.espoo.evaka.daycare.service.LocationService
import org.jdbi.v3.core.Jdbi
import org.junit.jupiter.api.Test
import org.skyscreamer.jsonassert.JSONAssert
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest
import org.springframework.boot.test.mock.mockito.MockBean
import org.springframework.http.MediaType
import org.springframework.test.web.servlet.MockMvc
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders
import org.springframework.test.web.servlet.result.MockMvcResultMatchers
import java.util.UUID

@WebMvcTest(LocationController::class)
class LocationControllerTest {

    @MockBean
    lateinit var locationService: LocationService

    @MockBean
    lateinit var jdbi: Jdbi

    @Autowired
    lateinit var mockMvc: MockMvc

    private val areaId = "e6a22972-f82d-41ca-8198-147735298ead"
    private val daycare1Id = "d5879669-2254-4a2f-9cd6-1ffeff585c4b"
    private val daycare2Id = "1ffeff58-4a2f-2254-9cd6-58796695c4bd"

    @Test
    fun `fetches areas correctly`() {
        whenever(locationService.getAreas()).thenReturn(getAreas())

        val result = mockMvc.perform(MockMvcRequestBuilders.get("/enduser/areas"))
            .andExpect(MockMvcResultMatchers.status().isOk)
            .andExpect(MockMvcResultMatchers.content().contentType(MediaType.APPLICATION_JSON))
            .andReturn().response.contentAsString
        // NB: the poBox for club currently contains the entire mailing address in the database
        // NB: a PO Box requires both the box number and the postal code. Currently the API gives out
        // the visiting address postal code in 'postalCode' with the mail address PO box in 'pobox' which
        // are different and is wrong. Hopefully they are not used together for anything important.
        // This remains unchanged. To get the correct values, use the visiting and mailing address values.
        val expectedJSON =
            """
            [  
                {  
                    "id": "$areaId",
                    "name": "alue",
                    "daycares":[  
                        {  
                            "id": "$daycare2Id",
                            "name": "toka hoitola",
                            "address": "",
                            "location": null,
                            "phone": null,
                            "postalCode": "12354",
                            "type": ["CENTRE"],
                            "care_area_id": "$areaId",
                            "url": null,
                            "provider_type": "PRIVATE",
                            "language": "fi",
                            "pobox": null
                        }
                    ]
                }
            ]
            """.trimIndent()

        JSONAssert.assertEquals(expectedJSON, result, false)
    }

    private fun getAreas(): List<CareArea> {
        val areaId = UUID.fromString(areaId)
        return listOf(
            CareArea(
                id = areaId,
                name = "alue",
                shortName = "alue",
                locations = listOf(
                    Location(
                        id = UUID.fromString(daycare1Id),
                        name = "eka hoitola",
                        mailingAddress = MailingAddress(
                            streetAddress = "Postikatu 1",
                            postOffice = "Postila",
                            postalCode = "33333",
                            poBox = "PL 321"
                        ),
                        visitingAddress = VisitingAddress(
                            streetAddress = "Kayntikatu 1",
                            postOffice = "Käyntilä",
                            postalCode = "123123"
                        ),
                        type = emptySet(),
                        care_area_id = areaId,
                        canApplyDaycare = false,
                        canApplyPreschool = false,
                        canApplyClub = false
                    ),
                    Location(
                        id = UUID.fromString(daycare2Id),
                        name = "toka hoitola",
                        type = setOf(
                            CareType.CENTRE
                        ),
                        care_area_id = areaId,
                        provider_type = ProviderType.PRIVATE,
                        visitingAddress = VisitingAddress(
                            postalCode = "12354"
                        ),
                        mailingAddress = MailingAddress(),
                        canApplyDaycare = true,
                        canApplyPreschool = false,
                        canApplyClub = false
                    )
                )
            )
        )
    }
}
