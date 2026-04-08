// SPDX-FileCopyrightText: 2017-2022 City of Espoo
//
// SPDX-License-Identifier: LGPL-2.1-or-later

package evaka.core.application

import evaka.core.PureJdbiTest
import evaka.core.application.persistence.daycare.Adult
import evaka.core.application.persistence.daycare.Apply
import evaka.core.application.persistence.daycare.Child
import evaka.core.application.persistence.daycare.DaycareFormV0
import evaka.core.insertServiceNeedOptions
import evaka.core.shared.dev.DevCareArea
import evaka.core.shared.dev.DevDaycare
import evaka.core.shared.dev.DevPerson
import evaka.core.shared.dev.DevPersonType
import evaka.core.shared.dev.insert
import evaka.core.shared.dev.insertTestApplication
import evaka.core.shared.security.actionrule.AccessControlFilter
import evaka.core.snDefaultDaycare
import java.time.LocalDate
import kotlin.test.assertEquals
import org.junit.jupiter.api.BeforeAll
import org.junit.jupiter.api.Test

class ApplicationQueriesSmokeTest : PureJdbiTest(resetDbBeforeEach = false) {
    private val area = DevCareArea()
    private val daycare = DevDaycare(areaId = area.id)
    private val child = DevPerson()
    private val guardian = DevPerson()
    private val form =
        DaycareFormV0(
            type = ApplicationType.DAYCARE,
            child = Child(dateOfBirth = LocalDate.of(2020, 1, 1)),
            guardian = Adult(),
            apply = Apply(preferredUnits = listOf(daycare.id)),
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

    @BeforeAll
    override fun beforeAll() {
        super.beforeAll()
        db.transaction { tx ->
            tx.insert(area)
            tx.insert(daycare)
            tx.insert(child, DevPersonType.RAW_ROW)
            tx.insert(guardian, DevPersonType.RAW_ROW)
            tx.insertTestApplication(
                childId = child.id,
                guardianId = guardian.id,
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
        val applications = db.read { it.getApplicationUnitSummaries(daycare.id) }
        val application = applications.single()
        assertEquals(form.serviceNeedOption, application.serviceNeed)
    }
}
