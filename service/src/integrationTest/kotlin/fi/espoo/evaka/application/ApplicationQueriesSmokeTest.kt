// SPDX-FileCopyrightText: 2017-2022 City of Espoo
//
// SPDX-License-Identifier: LGPL-2.1-or-later

package fi.espoo.evaka.application

import fi.espoo.evaka.PureJdbiTest
import fi.espoo.evaka.application.persistence.daycare.Adult
import fi.espoo.evaka.application.persistence.daycare.Apply
import fi.espoo.evaka.application.persistence.daycare.Child
import fi.espoo.evaka.application.persistence.daycare.DaycareFormV0
import fi.espoo.evaka.insertServiceNeedOptions
import fi.espoo.evaka.shared.ApplicationId
import fi.espoo.evaka.shared.DaycareId
import fi.espoo.evaka.shared.dev.DevCareArea
import fi.espoo.evaka.shared.dev.DevDaycare
import fi.espoo.evaka.shared.dev.DevPerson
import fi.espoo.evaka.shared.dev.DevPersonType
import fi.espoo.evaka.shared.dev.insert
import fi.espoo.evaka.shared.dev.insertTestApplication
import fi.espoo.evaka.shared.security.actionrule.AccessControlFilter
import fi.espoo.evaka.snDefaultDaycare
import java.time.LocalDate
import kotlin.test.assertEquals
import org.junit.jupiter.api.BeforeAll
import org.junit.jupiter.api.Test

class ApplicationQueriesSmokeTest : PureJdbiTest(resetDbBeforeEach = false) {
    private lateinit var daycareId: DaycareId
    private lateinit var applicationId: ApplicationId
    private lateinit var form: DaycareFormV0

    @BeforeAll
    override fun beforeAll() {
        super.beforeAll()
        db.transaction { tx ->
            val areaId = tx.insert(DevCareArea())
            val childId = tx.insert(DevPerson(), DevPersonType.RAW_ROW)
            val guardianId = tx.insert(DevPerson(), DevPersonType.RAW_ROW)
            daycareId = tx.insert(DevDaycare(areaId = areaId))
            form =
                DaycareFormV0(
                    type = ApplicationType.DAYCARE,
                    child = Child(dateOfBirth = LocalDate.of(2020, 1, 1)),
                    guardian = Adult(),
                    apply = Apply(preferredUnits = listOf(daycareId)),
                    preferredStartDate = LocalDate.of(2022, 1, 1),
                    serviceNeedOption =
                        ServiceNeedOption(
                            id = snDefaultDaycare.id,
                            nameFi = snDefaultDaycare.nameFi,
                            nameEn = snDefaultDaycare.nameEn,
                            nameSv = snDefaultDaycare.nameSv,
                            validPlacementType = null,
                        ),
                )
            applicationId =
                tx.insertTestApplication(
                    childId = childId,
                    guardianId = guardianId,
                    type = ApplicationType.DAYCARE,
                    document = form,
                )
            tx.insertServiceNeedOptions()
        }
    }

    @Test
    fun `fetchApplicationSummaries returns service need information from applications correctly`() {
        val applications =
            db.read {
                it.fetchApplicationSummaries(
                    today = LocalDate.of(2022, 1, 1),
                    params =
                        SearchApplicationRequest(
                            page = 1,
                            pageSize = 10,
                            sortBy = ApplicationSortColumn.STATUS,
                            sortDir = ApplicationSortDirection.ASC,
                            areas = emptyList(),
                            units = emptyList(),
                            basis = emptyList(),
                            type = ApplicationTypeToggle.ALL,
                            preschoolType = emptyList(),
                            statuses = ApplicationStatusOption.entries.toList(),
                            dateType = emptyList(),
                            distinctions = emptyList(),
                            periodStart = null,
                            periodEnd = null,
                            searchTerms = null,
                            transferApplications = TransferApplicationFilter.ALL,
                            voucherApplications = null,
                        ),
                    readWithAssistanceNeed = AccessControlFilter.PermitAll,
                    readWithoutAssistanceNeed = AccessControlFilter.PermitAll,
                    canReadServiceWorkerNotes = true,
                )
            }
        val application = applications.data.single()
        assertEquals(form.serviceNeedOption, application.serviceNeed)
    }

    @Test
    fun `getApplicationUnitSummaries returns service need information from applications correctly`() {
        val applications = db.read { it.getApplicationUnitSummaries(daycareId) }
        val application = applications.single()
        assertEquals(form.serviceNeedOption, application.serviceNeed)
    }
}
