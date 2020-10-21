// SPDX-FileCopyrightText: 2017-2020 City of Espoo
//
// SPDX-License-Identifier: LGPL-2.1-or-later

package fi.espoo.evaka.pis.controllers

import com.fasterxml.jackson.databind.ObjectMapper
import com.nhaarman.mockito_kotlin.any
import com.nhaarman.mockito_kotlin.argumentCaptor
import com.nhaarman.mockito_kotlin.verify
import com.nhaarman.mockito_kotlin.whenever
import fi.espoo.evaka.application.enduser.objectMapper
import fi.espoo.evaka.dvv.DvvModificationsBatchRefreshService
import fi.espoo.evaka.pis.service.MergeService
import fi.espoo.evaka.pis.service.PersonIdentityRequest
import fi.espoo.evaka.pis.service.PersonJSON
import fi.espoo.evaka.pis.service.PersonService
import fi.espoo.evaka.pis.service.VTJBatchRefreshService
import fi.espoo.evaka.pis.testdata.ssn
import fi.espoo.evaka.pis.testdata.validPerson
import fi.espoo.evaka.pis.testdata.validPersonIdentity
import fi.espoo.evaka.shared.auth.AuthenticatedUser
import fi.espoo.evaka.shared.auth.AuthenticatedUserMockMvc
import fi.espoo.evaka.shared.auth.mockMvcAuthentication
import fi.espoo.evaka.shared.config.Roles
import org.jdbi.v3.core.Jdbi
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest
import org.springframework.boot.test.mock.mockito.MockBean
import org.springframework.context.annotation.Import
import org.springframework.http.MediaType
import org.springframework.test.web.servlet.MockMvc
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post
import org.springframework.test.web.servlet.result.MockMvcResultMatchers.content
import org.springframework.test.web.servlet.result.MockMvcResultMatchers.status
import java.util.UUID

private val objectMapper: ObjectMapper = objectMapper()

@Import(AuthenticatedUserMockMvc::class)
@WebMvcTest(PersonController::class)
class PersonControllerTest {
    @MockBean
    lateinit var personService: PersonService

    @MockBean
    lateinit var mergeService: MergeService

    @MockBean
    lateinit var vtjBatchRefreshService: VTJBatchRefreshService

    @MockBean
    lateinit var dvvModificationsBatchRefreshService: DvvModificationsBatchRefreshService

    @MockBean
    lateinit var jdbi: Jdbi

    @Autowired
    lateinit var mockMvc: MockMvc

    @Test
    fun `getOrCreatePersonIdentity returns ok and person is created`() {
        val user = AuthenticatedUser(UUID.randomUUID(), setOf())
        whenever(personService.getOrCreatePersonIdentity(any())).thenReturn(validPersonIdentity)

        mockMvc.perform(
            post("/person/identity")
                .content(
                    """
                    {
                        "socialSecurityNumber": "${ssn.ssn}",
                        "firstName": "${validPersonIdentity.firstName}",
                        "lastName": "${validPersonIdentity.lastName}",
                        "email": "${validPersonIdentity.email}"
                    }
                    """.trimIndent()
                )
                .contentType(MediaType.APPLICATION_JSON)
                .with(user.mockMvcAuthentication())
        )
            .andExpect(status().isOk)
            .andExpect(
                content().json(
                    """
                {
                    "id": "${validPersonIdentity.id}",
                    "roles": ["END_USER"]
                }
                """.trimMargin()
                )
            )

        argumentCaptor<PersonIdentityRequest>().run {
            verify(personService).getOrCreatePersonIdentity(capture())
            assertEquals(ssn.ssn, firstValue.identity.ssn)
        }
    }

    @Test
    fun `getPersonIdentity returns ok if person is found`() {
        val user = AuthenticatedUser(UUID.randomUUID(), setOf())
        whenever(
            personService.getUpToDatePerson(
                user,
                validPerson.id
            )
        ).thenReturn(validPerson)

        mockMvc.perform(get("/person/identity/${validPerson.id}").with(user.mockMvcAuthentication()))
            .andExpect(status().isOk)
            .andExpect(content().json(objectToJson(PersonJSON.from(validPerson))))
    }

    @Test
    fun `getPerson returns ok if person is found`() {
        val user = AuthenticatedUser(UUID.randomUUID(), setOf())
        whenever(
            personService.getUpToDatePerson(
                user,
                validPerson.id
            )
        ).thenReturn(validPerson)

        mockMvc.perform(get("/person/details/${validPerson.id}").with(user.mockMvcAuthentication()))
            .andExpect(status().isOk)
            .andExpect(content().json(objectToJson(PersonJSON.from(validPerson))))
    }

    @Test
    fun `getOrCreatePersonBySsn returns ok and person is created`() {
        val user = AuthenticatedUser(UUID.randomUUID(), setOf(Roles.SERVICE_WORKER))

        whenever(personService.getOrCreatePerson(user, ssn)).thenReturn(validPerson)

        mockMvc.perform(
            get("/person/details/ssn/${ssn.ssn}").with(user.mockMvcAuthentication())
        )
            .andExpect(status().isOk)
            .andExpect(
                content().json(objectToJson(PersonJSON.from(validPerson)))
            )
    }

    private fun objectToJson(obj: Any) = objectMapper.writeValueAsString(obj)
}
