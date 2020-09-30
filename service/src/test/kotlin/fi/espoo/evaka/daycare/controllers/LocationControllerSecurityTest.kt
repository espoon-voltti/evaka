// SPDX-FileCopyrightText: 2017-2020 City of Espoo
//
// SPDX-License-Identifier: LGPL-2.1-or-later

package fi.espoo.evaka.daycare.controllers

import com.nhaarman.mockito_kotlin.whenever
import fi.espoo.evaka.daycare.service.LocationService
import fi.espoo.evaka.shared.auth.AuthenticatedUserMockMvc
import org.assertj.core.api.Assertions.assertThat
import org.jdbi.v3.core.Jdbi
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest
import org.springframework.boot.test.mock.mockito.MockBean
import org.springframework.context.annotation.Import
import org.springframework.http.MediaType
import org.springframework.test.web.servlet.MockMvc
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders
import org.springframework.test.web.servlet.result.MockMvcResultMatchers

@WebMvcTest(LocationController::class)
@Import(AuthenticatedUserMockMvc::class)
class LocationControllerSecurityTest {

    @MockBean
    lateinit var locationService: LocationService

    @MockBean
    lateinit var jdbi: Jdbi

    @Autowired
    lateinit var mockMvc: MockMvc

    @Test
    fun `get areas works without login`() {
        whenever(locationService.getAreas()).thenReturn(listOf())
        val result = mockMvc.perform(MockMvcRequestBuilders.get("/enduser/areas"))
            .andExpect(MockMvcResultMatchers.status().isOk)
            .andExpect(MockMvcResultMatchers.content().contentType(MediaType.APPLICATION_JSON))
            .andReturn().response.contentAsString
        assertThat(result).isEqualTo("[]")
    }
}
