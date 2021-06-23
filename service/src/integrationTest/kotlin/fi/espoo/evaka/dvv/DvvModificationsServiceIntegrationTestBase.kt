// SPDX-FileCopyrightText: 2017-2020 City of Espoo
//
// SPDX-License-Identifier: LGPL-2.1-or-later

package fi.espoo.evaka.dvv

import com.github.kittinunf.fuel.core.FuelManager
import com.nhaarman.mockito_kotlin.mock
import fi.espoo.evaka.FullApplicationTest
import fi.espoo.evaka.identity.ExternalIdentifier
import fi.espoo.evaka.pis.service.FridgeFamilyService
import fi.espoo.evaka.pis.service.ParentshipService
import fi.espoo.evaka.pis.service.PersonDTO
import fi.espoo.evaka.pis.service.PersonService
import fi.espoo.evaka.pis.service.PersonWithChildrenDTO
import fi.espoo.evaka.shared.async.AsyncJobRunner
import fi.espoo.evaka.shared.auth.AuthenticatedUser
import fi.espoo.evaka.shared.db.Database
import fi.espoo.evaka.vtjclient.service.persondetails.IPersonDetailsService
import fi.espoo.evaka.vtjclient.service.persondetails.PersonStorageService
import org.junit.jupiter.api.BeforeEach
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.stereotype.Service
import java.security.cert.X509Certificate
import java.util.UUID
import javax.net.ssl.HostnameVerifier
import javax.net.ssl.SSLContext
import javax.net.ssl.TrustManager
import javax.net.ssl.X509TrustManager

class DvvModificationsServiceIntegrationTestBase : FullApplicationTest() {

    @Autowired
    protected lateinit var personService: DvvIntegrationTestPersonService

    @Autowired
    protected lateinit var parentshipService: ParentshipService

    @Autowired
    protected lateinit var asyncJobRunner: AsyncJobRunner

    protected lateinit var fridgeFamilyService: FridgeFamilyService
    protected lateinit var dvvModificationsServiceClient: DvvModificationsServiceClient
    protected lateinit var dvvModificationsService: DvvModificationsService
    protected lateinit var requestCustomizerMock: DvvModificationRequestCustomizer

    @BeforeEach
    protected fun initDvvModificationService() {
        assert(httpPort > 0)

        fridgeFamilyService = FridgeFamilyService(personService, parentshipService, asyncJobRunner)
        val mockDvvBaseUrl = "http://localhost:$httpPort/mock-integration/dvv"
        requestCustomizerMock = mock()
        dvvModificationsServiceClient = DvvModificationsServiceClient(objectMapper, noCertCheckFuelManager(), listOf(requestCustomizerMock), env, mockDvvBaseUrl)
        dvvModificationsService = DvvModificationsService(dvvModificationsServiceClient, personService, fridgeFamilyService, asyncJobRunner)
    }

    fun noCertCheckFuelManager() = FuelManager().apply {
        val trustAllCerts = arrayOf<TrustManager>(object : X509TrustManager {
            override fun getAcceptedIssuers(): Array<X509Certificate>? = null
            override fun checkClientTrusted(chain: Array<X509Certificate>, authType: String) = Unit
            override fun checkServerTrusted(chain: Array<X509Certificate>, authType: String) = Unit
        })

        socketFactory = SSLContext.getInstance("SSL").apply {
            init(null, trustAllCerts, java.security.SecureRandom())
        }.socketFactory

        hostnameVerifier = HostnameVerifier { _, _ -> true }
    }
}

@Service
class DvvIntegrationTestPersonService(personDetailsService: IPersonDetailsService, personStorageService: PersonStorageService) : PersonService(personDetailsService, personStorageService) {
    companion object {
        private val ssnUpdateCounts = mutableMapOf<String, Int>()
        private val ssnCustodianUpdateCounts = mutableMapOf<String, Int>()

        fun resetSsnUpdateCounts() {
            ssnUpdateCounts.clear()
            ssnCustodianUpdateCounts.clear()
        }

        fun recordSsnUpdate(ssn: ExternalIdentifier.SSN) {
            ssnUpdateCounts.put(ssn.toString(), ssnUpdateCounts.getOrDefault(ssn.toString(), 0) + 1)
        }

        fun recordSsnCustodianUpdate(ssn: ExternalIdentifier.SSN) {
            ssnCustodianUpdateCounts.put(ssn.toString(), ssnCustodianUpdateCounts.getOrDefault(toString(), 0) + 1)
        }

        fun getSsnUpdateCount(ssn: ExternalIdentifier.SSN): Int {
            return ssnUpdateCounts.getOrDefault(ssn.toString(), 0)
        }

        fun getSsnUpdateCount(ssn: String): Int {
            return ssnUpdateCounts.getOrDefault(ssn, 0)
        }

        fun getSsnCustodianUpdateCount(ssn: ExternalIdentifier.SSN): Int {
            return ssnCustodianUpdateCounts.getOrDefault(ssn.toString(), 0)
        }

        fun getSsnCustodianUpdateCount(ssn: String): Int {
            return ssnCustodianUpdateCounts.getOrDefault(ssn, 0)
        }
    }

    override fun getOrCreatePerson(tx: Database.Transaction, user: AuthenticatedUser, ssn: ExternalIdentifier.SSN, updateStale: Boolean): PersonDTO? {
        recordSsnUpdate(ssn)
        return super.getOrCreatePerson(tx, user, ssn, updateStale)
    }

    override fun getUpToDatePersonWithChildren(
        tx: Database.Transaction,
        user: AuthenticatedUser,
        id: UUID
    ): PersonWithChildrenDTO? {
        return super.getUpToDatePersonWithChildren(tx, user, id)?.let {
            val ssn = it.socialSecurityNumber
            if (ssn != null)
                recordSsnCustodianUpdate(ExternalIdentifier.SSN.getInstance(ssn))
            it
        }
    }
}
