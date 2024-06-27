// SPDX-FileCopyrightText: 2017-2020 City of Espoo
//
// SPDX-License-Identifier: LGPL-2.1-or-later

package fi.espoo.evaka.varda

import fi.espoo.evaka.daycare.CareType
import fi.espoo.evaka.daycare.domain.ProviderType
import fi.espoo.evaka.defaultMunicipalOrganizerOid
import fi.espoo.evaka.defaultPurchasedOrganizerOid
import fi.espoo.evaka.insertServiceNeedOptions
import fi.espoo.evaka.invoicing.createFeeDecisionChildFixture
import fi.espoo.evaka.invoicing.createFeeDecisionFixture
import fi.espoo.evaka.invoicing.createVoucherValueDecisionFixture
import fi.espoo.evaka.invoicing.data.upsertFeeDecisions
import fi.espoo.evaka.invoicing.data.upsertValueDecisions
import fi.espoo.evaka.invoicing.domain.FeeDecisionServiceNeed
import fi.espoo.evaka.invoicing.domain.FeeDecisionStatus
import fi.espoo.evaka.invoicing.domain.FeeDecisionType
import fi.espoo.evaka.invoicing.domain.VoucherValueDecision
import fi.espoo.evaka.invoicing.domain.VoucherValueDecisionStatus
import fi.espoo.evaka.placement.PlacementType
import fi.espoo.evaka.serviceneed.ServiceNeedOption
import fi.espoo.evaka.serviceneed.deleteServiceNeed
import fi.espoo.evaka.shared.ChildId
import fi.espoo.evaka.shared.DaycareId
import fi.espoo.evaka.shared.EvakaUserId
import fi.espoo.evaka.shared.FeeDecisionId
import fi.espoo.evaka.shared.PersonId
import fi.espoo.evaka.shared.ServiceNeedId
import fi.espoo.evaka.shared.VoucherValueDecisionId
import fi.espoo.evaka.shared.db.Database
import fi.espoo.evaka.shared.dev.DevDaycare
import fi.espoo.evaka.shared.dev.DevPerson
import fi.espoo.evaka.shared.dev.DevPersonType
import fi.espoo.evaka.shared.dev.DevPlacement
import fi.espoo.evaka.shared.dev.DevServiceNeed
import fi.espoo.evaka.shared.dev.insert
import fi.espoo.evaka.shared.dev.insertVardaServiceNeed
import fi.espoo.evaka.shared.dev.upsertServiceNeedOption
import fi.espoo.evaka.shared.domain.DateRange
import fi.espoo.evaka.shared.domain.HelsinkiDateTime
import fi.espoo.evaka.shared.domain.RealEvakaClock
import fi.espoo.evaka.snDefaultClub
import fi.espoo.evaka.snDefaultDaycare
import fi.espoo.evaka.snDefaultPreschool
import fi.espoo.evaka.testAdult_1
import fi.espoo.evaka.testAdult_2
import fi.espoo.evaka.testArea
import fi.espoo.evaka.testChild_1
import fi.espoo.evaka.testDaycare
import fi.espoo.evaka.testDaycareNotInvoiced
import fi.espoo.evaka.testDecisionMaker_1
import fi.espoo.evaka.toValueDecisionServiceNeed
import fi.espoo.evaka.varda.integration.MockVardaIntegrationEndpoint
import fi.espoo.evaka.varda.integration.VardaClient
import java.time.Instant
import java.time.LocalDate
import java.util.UUID
import kotlin.test.assertEquals
import kotlin.test.assertNotNull
import kotlin.test.assertNull
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired

class VardaServiceIntegrationTest : VardaIntegrationTest(resetDbBeforeEach = true) {
    @Autowired lateinit var mockEndpoint: MockVardaIntegrationEndpoint

    private val purchasedDaycare =
        DevDaycare(
            name = "Test Purchased Daycare",
            areaId = testArea.id,
            providerType = ProviderType.PURCHASED,
            ophOrganizerOid = defaultPurchasedOrganizerOid,
            invoicedByMunicipality = false
        )

    private val externalPurchasedDaycare =
        DevDaycare(
            name = "Test External Purchased Daycare",
            areaId = testArea.id,
            providerType = ProviderType.EXTERNAL_PURCHASED,
            ophOrganizerOid = defaultPurchasedOrganizerOid,
            invoicedByMunicipality = false
        )

    private val ghostUnitDaycare =
        DevDaycare(
            name = "Test Ghost Unit Daycare",
            areaId = testArea.id,
            type = setOf(CareType.CENTRE),
            uploadToVarda = false,
            uploadChildrenToVarda = false,
            uploadToKoski = false,
            ghostUnit = true,
            invoicedByMunicipality = false
        )

    @BeforeEach
    fun beforeEach() {
        db.transaction { tx ->
            tx.insert(testDecisionMaker_1)
            tx.insert(testArea)
            tx.insert(testDaycare)
            tx.insert(testDaycareNotInvoiced)
            tx.insert(purchasedDaycare)
            tx.insert(externalPurchasedDaycare)
            tx.insert(ghostUnitDaycare)
            listOf(testAdult_1, testAdult_2).forEach { tx.insert(it, DevPersonType.ADULT) }
            tx.insert(testChild_1, DevPersonType.CHILD)
            tx.insertServiceNeedOptions()
        }
        insertVardaUnit(db)
        db.transaction {
            @Suppress("DEPRECATION")
            it.createUpdate(
                    "INSERT INTO guardian(guardian_id, child_id) VALUES (:guardianId, :childId)"
                )
                .bind("guardianId", testAdult_1.id)
                .bind("childId", testChild_1.id)
                .execute()

            @Suppress("DEPRECATION")
            it.createUpdate(
                    "INSERT INTO varda_reset_child(evaka_child_id, reset_timestamp) VALUES (:evakaChildId, now())"
                )
                .bind("evakaChildId", testChild_1.id)
                .execute()
        }
        mockEndpoint.cleanUp()
    }

    val VOUCHER_VALUE = 10000
    val VOUCHER_CO_PAYMENT = 5000

    @Test
    fun `setToBeReset works`() {
        fun countChildrenToBeReset(): Int =
            db.read {
                @Suppress("DEPRECATION")
                it.createQuery(
                        "SELECT count(*) FROM varda_reset_child WHERE reset_timestamp IS NULL"
                    )
                    .exactlyOne<Int>()
            }

        assertEquals(0, countChildrenToBeReset())

        db.transaction { it.setToBeReset(listOf(testChild_1.id)) }
        assertEquals(1, countChildrenToBeReset())
    }

    @Test
    fun `hasVardaServiceNeeds works`() {
        val since = HelsinkiDateTime.now()
        val snId = createServiceNeed(db, option = snDefaultDaycare)
        val childId = testChild_1.id

        assertEquals(false, db.read { it.hasVardaServiceNeeds(childId) })

        db.transaction {
            @Suppress("DEPRECATION")
            it.createUpdate(
                    """
                INSERT INTO varda_service_need (evaka_child_id, evaka_service_need_id, evaka_service_need_updated) 
                VALUES (:evakaChildId, :evakaServiceNeedId, :since)
                """
                        .trimIndent()
                )
                .bind("evakaChildId", childId)
                .bind("evakaServiceNeedId", snId)
                .bind("since", since)
                .execute()
        }

        assertEquals(true, db.read { it.hasVardaServiceNeeds(childId) })
    }

    @Test
    fun `calculateEvakaVsVardaServiceNeedChangesByChild finds a new evaka service need with voucher value decision when varda has none`() {
        val since = HelsinkiDateTime.now()
        val option = snDefaultDaycare.copy(updated = since)
        val snStartDate = since.minusDays(10).toLocalDate()
        val snEndDate = since.toLocalDate()
        val snId =
            createServiceNeed(
                db,
                option,
                child = testChild_1,
                fromDays = snStartDate,
                toDays = snEndDate
            )
        createVoucherDecision(
            db,
            snStartDate,
            snEndDate,
            testDaycare.id,
            100,
            100,
            testAdult_1.id,
            testChild_1,
            since.toInstant(),
            VoucherValueDecisionStatus.SENT
        )

        val childId = db.read { it.getChildIdByServiceNeedId(snId) }

        val diffs =
            calculateEvakaVsVardaServiceNeedChangesByChild(
                db,
                RealEvakaClock(),
                evakaEnv.feeDecisionMinDate
            )
        assertEquals(1, diffs.keys.size)
        assertServiceNeedDiffSizes(diffs.get(childId), 1, 0, 0)
        assertEquals(snId, diffs.get(childId)?.additions?.get(0))
    }

    @Test
    fun `calculateEvakaVsVardaServiceNeedChangesByChild does not find a new evaka service need with voucher value decision when varda has none, but the service need ended before 01-01-2019`() {
        val since = HelsinkiDateTime.now()
        val option = snDefaultDaycare.copy(updated = since)
        val snStartDate = LocalDate.of(2018, 1, 1)
        val snEndDate = LocalDate.of(2018, 11, 30)
        createServiceNeed(
            db,
            option,
            child = testChild_1,
            fromDays = snStartDate,
            toDays = snEndDate
        )
        createVoucherDecision(
            db,
            snStartDate,
            snEndDate,
            testDaycare.id,
            100,
            100,
            testAdult_1.id,
            testChild_1,
            since.toInstant(),
            VoucherValueDecisionStatus.SENT
        )
        val diffs =
            calculateEvakaVsVardaServiceNeedChangesByChild(
                db,
                RealEvakaClock(),
                evakaEnv.feeDecisionMinDate
            )
        assertEquals(0, diffs.keys.size)
    }

    @Test
    fun `calculateEvakaVsVardaServiceNeedChangesByChild excludes if hours per week is 0`() {
        val since = HelsinkiDateTime.now()
        val option = snDefaultPreschool.copy(updated = since)
        createServiceNeed(db, option)

        val diffs =
            calculateEvakaVsVardaServiceNeedChangesByChild(
                db,
                RealEvakaClock(),
                evakaEnv.feeDecisionMinDate
            )
        assertEquals(0, diffs.keys.size)
    }

    @Test
    fun `calculateEvakaVsVardaServiceNeedChangesByChild finds updated evaka service need from purchased daycare`() {
        val since = HelsinkiDateTime.now()
        val option = snDefaultDaycare.copy(updated = since)
        val snId = createServiceNeed(db, option, unitId = purchasedDaycare.id)
        val childId =
            db.read { it.getChildIdByServiceNeedId(snId) }
                ?: throw Exception("Created service need not found?!?")
        db.transaction {
            it.insertVardaServiceNeed(
                VardaServiceNeed(
                    evakaChildId = childId,
                    evakaServiceNeedId = snId,
                    evakaServiceNeedUpdated =
                        since.minusHours(
                            100
                        ) // Evaka and varda timestamps differ, thus sn has changed
                )
            )
        }

        val diffs =
            calculateEvakaVsVardaServiceNeedChangesByChild(
                db,
                RealEvakaClock(),
                evakaEnv.feeDecisionMinDate
            )
        assertEquals(1, diffs.keys.size)
        assertServiceNeedDiffSizes(diffs.get(childId), 0, 1, 0)
        assertEquals(snId, diffs.get(childId)?.updates?.get(0))
    }

    @Test
    fun `calculateEvakaVsVardaServiceNeedChangesByChild finds deleted evaka service need when there are also other changes`() {
        val since = HelsinkiDateTime.now()
        val option = snDefaultDaycare.copy(updated = since)
        val snStartDate = since.minusDays(10).toLocalDate()
        val snEndDate = since.toLocalDate()
        val changedServiceNeedId =
            createServiceNeed(db, option, fromDays = snStartDate, toDays = snEndDate)
        createFeeDecision(
            db,
            testChild_1,
            testAdult_1.id,
            DateRange(snStartDate, snEndDate),
            since.toInstant()
        )

        val childId =
            db.read { it.getChildIdByServiceNeedId(changedServiceNeedId) }
                ?: throw Exception("Created service need not found?!?")
        db.transaction {
            it.insertVardaServiceNeed(
                VardaServiceNeed(
                    evakaChildId = childId,
                    evakaServiceNeedId = changedServiceNeedId,
                    evakaServiceNeedUpdated =
                        since.minusHours(
                            100
                        ) // Evaka and varda timestamps differ, thus sn has changed
                )
            )
        }

        val deletedSnId = ServiceNeedId(UUID.randomUUID())

        db.transaction {
            it.insertVardaServiceNeed(
                VardaServiceNeed(
                    evakaChildId = childId,
                    evakaServiceNeedId =
                        deletedSnId, // Does not exist in evaka, thus should be deleted
                    evakaServiceNeedUpdated = since.minusHours(100)
                )
            )
        }

        val diffs =
            calculateEvakaVsVardaServiceNeedChangesByChild(
                db,
                RealEvakaClock(),
                evakaEnv.feeDecisionMinDate
            )
        assertEquals(1, diffs.keys.size)
        assertServiceNeedDiffSizes(diffs.get(childId), 0, 1, 1)
        assertEquals(changedServiceNeedId, diffs.get(childId)?.updates?.get(0))
        assertEquals(deletedSnId, diffs.get(childId)?.deletes?.get(0))
    }

    @Test
    fun `calculateEvakaVsVardaServiceNeedChangesByChild finds removed evaka service needs that exists in varda and there are no other changes`() {
        val since = HelsinkiDateTime.now()
        val childId: ChildId = ChildId(UUID.randomUUID())
        val deletedSnId = ServiceNeedId(UUID.randomUUID())
        db.transaction {
            it.insert(
                DevPerson(
                    id = childId,
                    dateOfBirth = since.plusYears(-5).toLocalDate(),
                    ssn = "260718A384E"
                ),
                DevPersonType.RAW_ROW
            )

            it.insertVardaServiceNeed(
                VardaServiceNeed(
                    evakaChildId = childId,
                    evakaServiceNeedId =
                        deletedSnId, // Does not exist in evaka, thus should be deleted
                    evakaServiceNeedUpdated = since.minusHours(100)
                )
            )
        }

        val diffs =
            calculateEvakaVsVardaServiceNeedChangesByChild(
                db,
                RealEvakaClock(),
                evakaEnv.feeDecisionMinDate
            )
        assertEquals(1, diffs.keys.size)
        assertServiceNeedDiffSizes(diffs[childId], 0, 0, 1)
        assertEquals(deletedSnId, diffs[childId]?.deletes?.get(0))
    }

    @Test
    fun `getServiceNeedFeeData matches fee decisions correctly to service needs`() {
        val since = HelsinkiDateTime.now()
        val startDate = since.minusDays(100).toLocalDate()
        val endDate = since.minusDays(90).toLocalDate()
        val startDate2 = endDate.plusDays(1)
        val snId1 = createServiceNeed(db, snDefaultDaycare, testChild_1, startDate, endDate)
        val fdId1 =
            createFeeDecision(
                db,
                testChild_1,
                testAdult_1.id,
                DateRange(startDate, endDate),
                since.toInstant()
            )

        val snId2 = createServiceNeed(db, snDefaultDaycare, testChild_1, startDate2)
        val fdId2 =
            createFeeDecision(
                db,
                testChild_1,
                testAdult_1.id,
                DateRange(startDate2, null),
                since.toInstant()
            )
        createFeeDecision(
            db,
            testChild_1,
            testAdult_1.id,
            DateRange(startDate.minusDays(10), startDate.minusDays(1)),
            since.toInstant()
        )

        val sn1FeeDiff = db.read { it.getServiceNeedFeeData(snId1) }
        assertEquals(1, sn1FeeDiff.size)
        assertEquals(fdId1, sn1FeeDiff.find { it.serviceNeedId == snId1 }?.feeDecisionIds?.get(0))

        val sn2FeeDiff = db.read { it.getServiceNeedFeeData(snId2) }
        assertEquals(1, sn2FeeDiff.size)
        assertEquals(fdId2, sn2FeeDiff.find { it.serviceNeedId == snId2 }?.feeDecisionIds?.get(0))
    }

    @Test
    fun `getServiceNeedFeeData does not pick fee decision before cutoff date 2019-09-01`() {
        val since = HelsinkiDateTime.now()

        val lastFeedecisionCutoffDate = LocalDate.of(2019, 8, 31)
        val snId = createServiceNeed(db, snDefaultDaycare, testChild_1, lastFeedecisionCutoffDate)
        createFeeDecision(
            db,
            testChild_1,
            testAdult_1.id,
            DateRange(lastFeedecisionCutoffDate, lastFeedecisionCutoffDate),
            since.toInstant()
        )

        val snFeeDiff = db.read { it.getServiceNeedFeeData(snId) }
        assertEquals(0, snFeeDiff.size)
    }

    @Test
    fun `getServiceNeedFeeData does not pick fee decision before cutoff date 2019-09-01 if service need ends before that`() {
        val since = HelsinkiDateTime.now()

        val lastFeedecisionCutoffDate = LocalDate.of(2019, 8, 31)
        val snId =
            createServiceNeed(
                db,
                snDefaultDaycare,
                testChild_1,
                lastFeedecisionCutoffDate,
                lastFeedecisionCutoffDate
            )
        createFeeDecision(
            db,
            testChild_1,
            testAdult_1.id,
            DateRange(lastFeedecisionCutoffDate, lastFeedecisionCutoffDate.plusDays(1)),
            since.toInstant()
        )

        val snFeeDiff = db.read { it.getServiceNeedFeeData(snId) }
        assertEquals(0, snFeeDiff.size)
    }

    @Test
    fun `getServiceNeedFeeData matches voucher value decisions correctly to service needs`() {
        val since = HelsinkiDateTime.now()
        val startDate = since.minusDays(100).toLocalDate()
        val endDate = since.minusDays(90).toLocalDate()
        val startDate2 = endDate.plusDays(1)
        val snId1 = createServiceNeed(db, snDefaultDaycare, testChild_1, startDate, endDate)
        val vd1 =
            createVoucherDecision(
                db,
                startDate,
                startDate,
                testDaycare.id,
                100,
                100,
                testAdult_1.id,
                testChild_1,
                since.toInstant(),
                VoucherValueDecisionStatus.SENT
            )

        val snId2 = createServiceNeed(db, snDefaultDaycare, testChild_1, startDate2)
        val vd2 =
            createVoucherDecision(
                db,
                startDate2,
                null,
                testDaycare.id,
                100,
                100,
                testAdult_1.id,
                testChild_1,
                since.toInstant(),
                VoucherValueDecisionStatus.SENT
            )
        createVoucherDecision(
            db,
            startDate.minusDays(10),
            startDate.minusDays(1),
            testDaycare.id,
            100,
            100,
            testAdult_1.id,
            testChild_1,
            since.toInstant(),
            VoucherValueDecisionStatus.SENT
        )

        val sn1FeeDiff = db.read { it.getServiceNeedFeeData(snId1) }
        assertEquals(1, sn1FeeDiff.size)
        assertEquals(
            vd1.id,
            sn1FeeDiff.find { it.serviceNeedId == snId1 }?.voucherValueDecisionIds?.get(0)
        )

        val sn2FeeDiff = db.read { it.getServiceNeedFeeData(snId2) }
        assertEquals(1, sn2FeeDiff.size)
        assertEquals(
            vd2.id,
            sn2FeeDiff.find { it.serviceNeedId == snId2 }?.voucherValueDecisionIds?.get(0)
        )
    }

    @Test
    fun `getServiceNeedFeeData does not pick service need voucher before cutoff date 2019-09-01 if service need ends before that`() {
        val since = HelsinkiDateTime.now()
        val lastVoucherCutoffDate = LocalDate.of(2019, 8, 31)

        val snId1 =
            createServiceNeed(
                db,
                snDefaultDaycare,
                testChild_1,
                lastVoucherCutoffDate,
                lastVoucherCutoffDate
            )
        createVoucherDecision(
            db,
            lastVoucherCutoffDate,
            lastVoucherCutoffDate.plusDays(1),
            testDaycare.id,
            100,
            100,
            testAdult_1.id,
            testChild_1,
            since.toInstant(),
            VoucherValueDecisionStatus.SENT
        )

        val sn1FeeDiff = db.read { it.getServiceNeedFeeData(snId1) }
        assertEquals(0, sn1FeeDiff.size)
    }

    @Test
    fun `getServiceNeedFeeData does not pick service need voucher before cutoff date 2019-09-01`() {
        val since = HelsinkiDateTime.now()
        val lastVoucherCutoffDate = LocalDate.of(2019, 8, 31)

        val snId1 =
            createServiceNeed(
                db,
                snDefaultDaycare,
                testChild_1,
                lastVoucherCutoffDate,
                lastVoucherCutoffDate
            )
        createVoucherDecision(
            db,
            lastVoucherCutoffDate,
            lastVoucherCutoffDate,
            testDaycare.id,
            100,
            100,
            testAdult_1.id,
            testChild_1,
            since.toInstant(),
            VoucherValueDecisionStatus.SENT
        )

        val sn1FeeDiff = db.read { it.getServiceNeedFeeData(snId1) }
        assertEquals(0, sn1FeeDiff.size)
    }

    internal fun Database.Transaction.insertVardaChild(
        id: ChildId,
        organizerOid: String = defaultMunicipalOrganizerOid
    ) = insertVardaOrganizerChild(this, id, 123L, 234, "1.2.3.4.5", organizerOid)

    @Test
    fun `updateChildData created related varda decision data when evaka service need is added`() {
        val childId = testChild_1.id
        db.transaction { it.insertVardaChild(childId) }
        val since = HelsinkiDateTime.now()
        val serviceNeedPeriod = DateRange(since.minusDays(100).toLocalDate(), since.toLocalDate())
        val feeDecisionPeriod =
            DateRange(serviceNeedPeriod.start, serviceNeedPeriod.start.plusDays(10))
        val voucherDecisionPeriod = DateRange(feeDecisionPeriod.end!!.plusDays(1), null)
        createServiceNeedAndFeeData(
            testChild_1,
            testAdult_1,
            since,
            serviceNeedPeriod,
            feeDecisionPeriod,
            voucherDecisionPeriod
        )

        updateChildData(db, vardaClient, evakaEnv.feeDecisionMinDate)

        assertVardaElementCounts(1, 1, 2)
        val vardaDecision = mockEndpoint.decisions.values.elementAt(0)
        assertVardaDecision(
            vardaDecision,
            serviceNeedPeriod.start,
            serviceNeedPeriod.end!!,
            serviceNeedPeriod.start.minusDays(15),
            1,
            snDefaultDaycare.daycareHoursPerWeek.toDouble()
        )
        assertVardaFeeData(
            VardaFeeData(
                huoltajat = listOf(asVardaGuardian(testAdult_1)),
                maksun_peruste_koodi = "MP03",
                asiakasmaksu = 289.0,
                palveluseteli_arvo = 0.0,
                alkamis_pvm = feeDecisionPeriod.start,
                paattymis_pvm = feeDecisionPeriod.end,
                perheen_koko = 2,
                lahdejarjestelma = "SourceSystemVarda",
                lapsi = "not asserted"
            ),
            mockEndpoint.feeData.values.elementAt(0)
        )
        assertVardaFeeData(
            VardaFeeData(
                huoltajat = listOf(asVardaGuardian(testAdult_1)),
                maksun_peruste_koodi = "MP03",
                asiakasmaksu = 50.0,
                palveluseteli_arvo = 100.0,
                alkamis_pvm = voucherDecisionPeriod.start,
                paattymis_pvm = serviceNeedPeriod.end,
                perheen_koko = 2,
                lahdejarjestelma = "SourceSystemVarda",
                lapsi = "not asserted"
            ),
            mockEndpoint.feeData.values.elementAt(1)
        )
    }

    @Test
    fun `updateChildData does not react to new evaka service need for non varda placement`() {
        db.transaction { it.insertVardaChild(testChild_1.id) }
        val since = HelsinkiDateTime.now()
        val serviceNeedPeriod = DateRange(since.minusDays(100).toLocalDate(), since.toLocalDate())
        createServiceNeed(
            db,
            snDefaultDaycare,
            testChild_1,
            serviceNeedPeriod.start,
            serviceNeedPeriod.end!!,
            PlacementType.PRESCHOOL
        )

        updateChildData(db, vardaClient, evakaEnv.feeDecisionMinDate)
        assertVardaElementCounts(0, 0, 0)
    }

    @Test
    fun `updateChildData does not react to new evaka service need for unit which does not send child info to varda`() {
        db.transaction { it.insertVardaChild(testChild_1.id) }
        val since = HelsinkiDateTime.now()
        val serviceNeedPeriod = DateRange(since.minusDays(100).toLocalDate(), since.toLocalDate())
        createServiceNeed(
            db,
            snDefaultDaycare,
            testChild_1,
            serviceNeedPeriod.start,
            serviceNeedPeriod.end!!,
            PlacementType.PRESCHOOL,
            ghostUnitDaycare.id
        )

        updateChildData(db, vardaClient, evakaEnv.feeDecisionMinDate)
        assertVardaElementCounts(0, 0, 0)
    }

    @Test
    fun `updateChildData sends service need without fee data if unit is not invoiced by evaka`() {
        db.transaction { it.insertVardaChild(testChild_1.id) }
        val since = HelsinkiDateTime.now()
        val serviceNeedPeriod = DateRange(since.minusDays(100).toLocalDate(), since.toLocalDate())
        createServiceNeed(
            db,
            snDefaultDaycare,
            testChild_1,
            serviceNeedPeriod.start,
            serviceNeedPeriod.end!!,
            PlacementType.DAYCARE,
            testDaycareNotInvoiced.id
        )

        updateChildData(db, vardaClient, evakaEnv.feeDecisionMinDate)
        assertVardaElementCounts(1, 1, 0)
    }

    @Test
    fun `getServiceNeedsForVardaByChild for non varda unit handles placement types`() {
        db.transaction { it.insertVardaChild(testChild_1.id) }
        val since = HelsinkiDateTime.now()
        val id =
            createServiceNeed(
                db,
                snDefaultDaycare,
                testChild_1,
                since.minusDays(100).toLocalDate(),
                since.minusDays(81).toLocalDate(),
                PlacementType.DAYCARE,
                testDaycareNotInvoiced.id
            )
        createServiceNeed(
            db,
            snDefaultPreschool,
            testChild_1,
            since.minusDays(80).toLocalDate(),
            since.minusDays(61).toLocalDate(),
            PlacementType.PRESCHOOL,
            testDaycareNotInvoiced.id
        )
        createServiceNeed(
            db,
            snDefaultClub,
            testChild_1,
            since.minusDays(60).toLocalDate(),
            since.minusDays(40).toLocalDate(),
            PlacementType.CLUB,
            testDaycareNotInvoiced.id
        )
        val serviceNeeds =
            db.transaction { it.getServiceNeedsForVardaByChild(RealEvakaClock(), testChild_1.id) }
        assertEquals(1, serviceNeeds.size)
        assertEquals(id, serviceNeeds.first())
    }

    @Test
    fun `getServiceNeedsForVardaByChild finds current service needs`() {
        db.transaction { it.insertVardaChild(testChild_1.id) }
        val since = HelsinkiDateTime.now()
        val id =
            createServiceNeed(
                db,
                snDefaultDaycare,
                testChild_1,
                since.minusDays(100).toLocalDate(),
                since.plusDays(200).toLocalDate(),
                PlacementType.DAYCARE,
                testDaycare.id
            )
        val serviceNeeds =
            db.transaction { it.getServiceNeedsForVardaByChild(RealEvakaClock(), testChild_1.id) }
        assertEquals(1, serviceNeeds.size)
        assertEquals(id, serviceNeeds.first())
    }

    @Test
    fun `getServiceNeedsForVardaByChild ignores future service needs`() {
        db.transaction { it.insertVardaChild(testChild_1.id) }
        val since = HelsinkiDateTime.now()
        createServiceNeed(
            db,
            snDefaultDaycare,
            testChild_1,
            since.plusDays(100).toLocalDate(),
            since.plusDays(200).toLocalDate(),
            PlacementType.DAYCARE,
            testDaycare.id
        )
        val serviceNeeds =
            db.transaction { it.getServiceNeedsForVardaByChild(RealEvakaClock(), testChild_1.id) }
        assertEquals(0, serviceNeeds.size)
    }

    @Test
    fun `getServiceNeedsForVardaByChild ignores 0 hour service needs`() {
        db.transaction { it.insertVardaChild(testChild_1.id) }
        val since = HelsinkiDateTime.now()
        createServiceNeed(
            db,
            snDefaultDaycare.copy(daycareHoursPerWeek = 0),
            testChild_1,
            since.minusDays(100).toLocalDate(),
            since.plusDays(200).toLocalDate(),
            PlacementType.DAYCARE,
            testDaycare.id
        )
        val serviceNeeds =
            db.transaction { it.getServiceNeedsForVardaByChild(RealEvakaClock(), testChild_1.id) }
        assertEquals(0, serviceNeeds.size)
    }

    @Test
    fun `updateChildData removes all related varda data when last service need is removed from evaka`() {
        db.transaction { it.insertVardaChild(testChild_1.id) }
        val since = HelsinkiDateTime.now()
        val serviceNeedPeriod = DateRange(since.minusDays(100).toLocalDate(), since.toLocalDate())
        val feeDecisionPeriod =
            DateRange(serviceNeedPeriod.start, serviceNeedPeriod.start.plusDays(10))
        val voucherDecisionPeriod = DateRange(feeDecisionPeriod.end!!.plusDays(1), null)
        val id =
            createServiceNeedAndFeeData(
                testChild_1,
                testAdult_1,
                since,
                serviceNeedPeriod,
                feeDecisionPeriod,
                voucherDecisionPeriod
            )

        updateChildData(db, vardaClient, evakaEnv.feeDecisionMinDate)

        assertVardaElementCounts(1, 1, 2)

        db.transaction { it.deleteServiceNeed(id) }
        updateChildData(db, vardaClient, evakaEnv.feeDecisionMinDate)

        assertVardaElementCounts(0, 0, 0)
    }

    @Test
    fun `updateChildData sends child service need to varda only when SENT fee decision is created in evaka`() {
        db.transaction { it.insertVardaChild(testChild_1.id) }
        val since = HelsinkiDateTime.now()
        val serviceNeedPeriod = DateRange(since.minusDays(100).toLocalDate(), since.toLocalDate())
        createServiceNeed(
            db,
            snDefaultDaycare,
            testChild_1,
            serviceNeedPeriod.start,
            serviceNeedPeriod.end!!
        )

        updateChildData(db, vardaClient, evakaEnv.feeDecisionMinDate)
        assertVardaElementCounts(0, 0, 0)

        val feeDecisionPeriod =
            DateRange(serviceNeedPeriod.start, serviceNeedPeriod.start.plusDays(10))
        val fdId =
            createFeeDecision(
                db,
                testChild_1,
                testAdult_1.id,
                DateRange(feeDecisionPeriod.start, feeDecisionPeriod.end),
                since.toInstant(),
                FeeDecisionStatus.WAITING_FOR_SENDING
            )

        updateChildData(db, vardaClient, evakaEnv.feeDecisionMinDate)
        assertVardaElementCounts(0, 0, 0)

        db.transaction {
            @Suppress("DEPRECATION")
            it.createUpdate("UPDATE fee_decision SET status = 'SENT' WHERE id = :id")
                .bind("id", fdId)
                .execute()
        }

        updateChildData(db, vardaClient, evakaEnv.feeDecisionMinDate)
        assertVardaElementCounts(1, 1, 1)

        val vardaDecision = mockEndpoint.decisions.values.elementAt(0)
        assertVardaDecision(
            vardaDecision,
            serviceNeedPeriod.start,
            serviceNeedPeriod.end!!,
            serviceNeedPeriod.start.minusDays(15),
            1,
            snDefaultDaycare.daycareHoursPerWeek.toDouble()
        )
        assertVardaFeeData(
            VardaFeeData(
                huoltajat = listOf(asVardaGuardian(testAdult_1)),
                maksun_peruste_koodi = "MP03",
                asiakasmaksu = 289.0,
                palveluseteli_arvo = 0.0,
                alkamis_pvm = feeDecisionPeriod.start,
                paattymis_pvm = feeDecisionPeriod.end,
                perheen_koko = 2,
                lahdejarjestelma = "SourceSystemVarda",
                lapsi = "not asserted"
            ),
            mockEndpoint.feeData.values.elementAt(0)
        )
    }

    @Test
    fun `updateChildData sends child service need to varda only when voucher value decision is created in evaka`() {
        db.transaction { it.insertVardaChild(testChild_1.id) }
        val since = HelsinkiDateTime.now()
        val serviceNeedPeriod = DateRange(since.minusDays(100).toLocalDate(), since.toLocalDate())
        createServiceNeed(
            db,
            snDefaultDaycare,
            testChild_1,
            serviceNeedPeriod.start,
            serviceNeedPeriod.end!!
        )

        updateChildData(db, vardaClient, evakaEnv.feeDecisionMinDate)
        assertVardaElementCounts(0, 0, 0)

        val voucherDecisionPeriod = DateRange(serviceNeedPeriod.start, null)
        createVoucherDecision(
            db,
            voucherDecisionPeriod.start,
            voucherDecisionPeriod.end,
            testDaycare.id,
            VOUCHER_VALUE,
            VOUCHER_CO_PAYMENT,
            testAdult_1.id,
            testChild_1,
            since.toInstant(),
            VoucherValueDecisionStatus.SENT
        )

        updateChildData(db, vardaClient, evakaEnv.feeDecisionMinDate)

        assertVardaElementCounts(1, 1, 1)

        val vardaDecision = mockEndpoint.decisions.values.elementAt(0)
        assertVardaDecision(
            vardaDecision,
            serviceNeedPeriod.start,
            serviceNeedPeriod.end!!,
            serviceNeedPeriod.start.minusDays(15),
            1,
            snDefaultDaycare.daycareHoursPerWeek.toDouble()
        )
        assertVardaFeeData(
            VardaFeeData(
                huoltajat = listOf(asVardaGuardian(testAdult_1)),
                maksun_peruste_koodi = "MP03",
                asiakasmaksu = 50.0,
                palveluseteli_arvo = 100.0,
                alkamis_pvm = voucherDecisionPeriod.start,
                paattymis_pvm = serviceNeedPeriod.end,
                perheen_koko = 2,
                lahdejarjestelma = "SourceSystemVarda",
                lapsi = "not asserted"
            ),
            mockEndpoint.feeData.values.elementAt(0)
        )
    }

    @Test
    fun `updateChildData sends child service need fee data to varda with both guardians if they live in the same address`() {
        db.transaction {
            @Suppress("DEPRECATION")
            it.createUpdate(
                    "INSERT INTO guardian(guardian_id, child_id) VALUES (:guardianId, :childId)"
                )
                .bind("guardianId", testAdult_2.id)
                .bind("childId", testChild_1.id)
                .execute()

            @Suppress("DEPRECATION")
            it.createUpdate("UPDATE person SET residence_code = 'aptunnus_1' WHERE id = ANY(:ids)")
                .bind("ids", listOf(testAdult_1.id, testAdult_2.id).toTypedArray())
                .execute()

            it.insertVardaChild(testChild_1.id)
        }

        val since = HelsinkiDateTime.now()
        val serviceNeedPeriod = DateRange(since.minusDays(100).toLocalDate(), since.toLocalDate())
        createServiceNeed(
            db,
            snDefaultDaycare,
            testChild_1,
            serviceNeedPeriod.start,
            serviceNeedPeriod.end!!
        )

        val feeDecisionPeriod =
            DateRange(serviceNeedPeriod.start, serviceNeedPeriod.start.plusDays(10))
        DateRange(feeDecisionPeriod.end!!.plusDays(1), null)
        createFeeDecision(
            db,
            testChild_1,
            testAdult_1.id,
            DateRange(feeDecisionPeriod.start, feeDecisionPeriod.end),
            since.toInstant()
        )

        updateChildData(db, vardaClient, evakaEnv.feeDecisionMinDate)

        assertVardaElementCounts(1, 1, 1)

        val vardaDecision = mockEndpoint.decisions.values.elementAt(0)
        assertEquals(2, mockEndpoint.feeData.values.elementAt(0).huoltajat.size)
        assertVardaDecision(
            vardaDecision,
            serviceNeedPeriod.start,
            serviceNeedPeriod.end!!,
            serviceNeedPeriod.start.minusDays(15),
            1,
            snDefaultDaycare.daycareHoursPerWeek.toDouble()
        )
    }

    @Test
    fun `updateChildData doesn't send blank guardian oid as empty string`() {
        db.transaction { it.insertVardaChild(testChild_1.id) }
        val since = HelsinkiDateTime.now()
        val serviceNeedPeriod = DateRange(since.minusDays(100).toLocalDate(), since.toLocalDate())
        createServiceNeed(
            db,
            snDefaultDaycare,
            testChild_1,
            serviceNeedPeriod.start,
            serviceNeedPeriod.end!!
        )

        val feeDecisionPeriod =
            DateRange(serviceNeedPeriod.start, serviceNeedPeriod.start.plusDays(10))
        createFeeDecision(
            db,
            testChild_1,
            testAdult_1.id,
            DateRange(feeDecisionPeriod.start, feeDecisionPeriod.end),
            since.toInstant()
        )
        db.transaction {
            @Suppress("DEPRECATION")
            it.createUpdate("update person set oph_person_oid = ' ' where id = :id")
                .bind("id", testAdult_1.id)
                .execute()
        }

        updateChildData(db, vardaClient, evakaEnv.feeDecisionMinDate)

        assertVardaElementCounts(1, 1, 1)

        val vardaFeeData = mockEndpoint.feeData.values.elementAt(0)
        assertEquals(1, vardaFeeData.huoltajat.size)
        assertNull(vardaFeeData.huoltajat.first().henkilo_oid)
    }

    @Test
    fun `updateChildData sends child service need fee data to varda with one guardian only if guardians live in different address`() {
        db.transaction {
            @Suppress("DEPRECATION")
            it.createUpdate(
                    "INSERT INTO guardian(guardian_id, child_id) VALUES (:guardianId, :childId)"
                )
                .bind("guardianId", testAdult_2.id)
                .bind("childId", testChild_1.id)
                .execute()

            @Suppress("DEPRECATION")
            it.createUpdate("UPDATE person SET residence_code = 'aptunnus_1' WHERE id = ANY(:ids)")
                .bind("ids", listOf(testAdult_1.id).toTypedArray())
                .execute()

            it.insertVardaChild(testChild_1.id)
        }

        val since = HelsinkiDateTime.now()
        val serviceNeedPeriod = DateRange(since.minusDays(100).toLocalDate(), since.toLocalDate())
        createServiceNeed(
            db,
            snDefaultDaycare,
            testChild_1,
            serviceNeedPeriod.start,
            serviceNeedPeriod.end!!
        )

        val feeDecisionPeriod =
            DateRange(serviceNeedPeriod.start, serviceNeedPeriod.start.plusDays(10))
        DateRange(feeDecisionPeriod.end!!.plusDays(1), null)
        createFeeDecision(
            db,
            testChild_1,
            testAdult_1.id,
            DateRange(feeDecisionPeriod.start, feeDecisionPeriod.end),
            since.toInstant()
        )

        updateChildData(db, vardaClient, evakaEnv.feeDecisionMinDate)

        assertVardaElementCounts(1, 1, 1)

        val vardaDecision = mockEndpoint.decisions.values.elementAt(0)
        assertEquals(1, mockEndpoint.feeData.values.elementAt(0).huoltajat.size)
        assertVardaDecision(
            vardaDecision,
            serviceNeedPeriod.start,
            serviceNeedPeriod.end!!,
            serviceNeedPeriod.start.minusDays(15),
            1,
            snDefaultDaycare.daycareHoursPerWeek.toDouble()
        )
    }

    @Test
    fun `updateChildData sends child service need to varda without the fee data if head of family is not a guardian`() {
        db.transaction { it.insertVardaChild(testChild_1.id) }
        val since = HelsinkiDateTime.now()
        val serviceNeedPeriod = DateRange(since.minusDays(100).toLocalDate(), since.toLocalDate())
        createServiceNeed(
            db,
            snDefaultDaycare,
            testChild_1,
            serviceNeedPeriod.start,
            serviceNeedPeriod.end!!
        )

        val feeDecisionPeriod =
            DateRange(serviceNeedPeriod.start, serviceNeedPeriod.start.plusDays(10))
        val voucherDecisionPeriod =
            DateRange(feeDecisionPeriod.end!!.plusDays(1), feeDecisionPeriod.end!!.plusDays(10))

        createFeeDecision(
            db,
            testChild_1,
            testAdult_2.id,
            DateRange(feeDecisionPeriod.start, feeDecisionPeriod.end),
            since.toInstant()
        )
        createVoucherDecision(
            db,
            voucherDecisionPeriod.start,
            voucherDecisionPeriod.end,
            testDaycare.id,
            VOUCHER_VALUE,
            VOUCHER_CO_PAYMENT,
            testAdult_2.id,
            testChild_1,
            since.toInstant(),
            VoucherValueDecisionStatus.SENT
        )

        updateChildData(db, vardaClient, evakaEnv.feeDecisionMinDate)

        assertVardaElementCounts(1, 1, 0)

        val vardaDecision = mockEndpoint.decisions.values.elementAt(0)
        assertVardaDecision(
            vardaDecision,
            serviceNeedPeriod.start,
            serviceNeedPeriod.end!!,
            serviceNeedPeriod.start.minusDays(15),
            1,
            snDefaultDaycare.daycareHoursPerWeek.toDouble()
        )
    }

    @Test
    fun `updateChildData sends child service need to varda without the fee data if child has no guardians`() {
        db.transaction {
            @Suppress("DEPRECATION") it.createUpdate("DELETE FROM guardian").execute()
            it.insertVardaChild(testChild_1.id)
        }
        val since = HelsinkiDateTime.now()
        val serviceNeedPeriod = DateRange(since.minusDays(100).toLocalDate(), since.toLocalDate())
        createServiceNeed(
            db,
            snDefaultDaycare,
            testChild_1,
            serviceNeedPeriod.start,
            serviceNeedPeriod.end!!
        )

        val feeDecisionPeriod =
            DateRange(serviceNeedPeriod.start, serviceNeedPeriod.start.plusDays(10))
        DateRange(feeDecisionPeriod.end!!.plusDays(1), null)
        createFeeDecision(
            db,
            testChild_1,
            testAdult_2.id,
            DateRange(feeDecisionPeriod.start, feeDecisionPeriod.end),
            since.toInstant()
        )

        updateChildData(db, vardaClient, evakaEnv.feeDecisionMinDate)

        assertVardaElementCounts(1, 1, 0)

        val vardaDecision = mockEndpoint.decisions.values.elementAt(0)
        assertVardaDecision(
            vardaDecision,
            serviceNeedPeriod.start,
            serviceNeedPeriod.end!!,
            serviceNeedPeriod.start.minusDays(15),
            1,
            snDefaultDaycare.daycareHoursPerWeek.toDouble()
        )
    }

    @Test
    fun `updateChildData sends child service need to varda without the fee data if service need ends before the fee data handling started in evaka`() {
        db.transaction { it.insertVardaChild(testChild_1.id) }
        val since = HelsinkiDateTime.now()
        val tenDaysAgoFeeDecisionMinDate = since.minusDays(10)
        val serviceNeedPeriod =
            DateRange(
                tenDaysAgoFeeDecisionMinDate.minusDays(10).toLocalDate(),
                tenDaysAgoFeeDecisionMinDate.minusDays(1).toLocalDate()
            )
        createServiceNeed(
            db,
            snDefaultDaycare,
            testChild_1,
            serviceNeedPeriod.start,
            serviceNeedPeriod.end!!
        )

        updateChildData(db, vardaClient, tenDaysAgoFeeDecisionMinDate.toLocalDate())

        assertVardaElementCounts(1, 1, 0)

        val vardaDecision = mockEndpoint.decisions.values.elementAt(0)
        assertVardaDecision(
            vardaDecision,
            serviceNeedPeriod.start,
            serviceNeedPeriod.end!!,
            serviceNeedPeriod.start.minusDays(15),
            1,
            snDefaultDaycare.daycareHoursPerWeek.toDouble()
        )
    }

    @Test
    fun `updateChildData sends new voucher fee data to varda`() {
        db.transaction { it.insertVardaChild(testChild_1.id) }
        val since = HelsinkiDateTime.now()
        val serviceNeedPeriod = DateRange(since.minusDays(100).toLocalDate(), since.toLocalDate())
        val feeDecisionPeriod =
            DateRange(serviceNeedPeriod.start, serviceNeedPeriod.start.plusDays(10))
        val voucherDecisionPeriod = DateRange(feeDecisionPeriod.end!!.plusDays(1), null)
        val child = testChild_1
        val adult = testAdult_1
        createServiceNeed(
            db,
            snDefaultDaycare,
            child,
            serviceNeedPeriod.start,
            serviceNeedPeriod.end!!
        )
        createFeeDecision(
            db,
            child,
            adult.id,
            DateRange(feeDecisionPeriod.start, feeDecisionPeriod.end),
            since.toInstant()
        )
        updateChildData(db, vardaClient, evakaEnv.feeDecisionMinDate)
        assertVardaCallCounts(1)
        assertVardaElementCounts(1, 1, 1)
        createVoucherDecision(
            db,
            voucherDecisionPeriod.start,
            voucherDecisionPeriod.end,
            testDaycare.id,
            VOUCHER_VALUE,
            VOUCHER_CO_PAYMENT,
            adult.id,
            child,
            since.toInstant(),
            VoucherValueDecisionStatus.SENT
        )
        updateChildData(db, vardaClient, evakaEnv.feeDecisionMinDate)
        assertVardaElementCounts(1, 1, 2)
        assertVardaCallCounts(3)
        // test that fee data is not sent if not modified
        updateChildData(db, vardaClient, evakaEnv.feeDecisionMinDate)
        assertVardaElementCounts(1, 1, 2)
        assertVardaCallCounts(3)
    }

    @Test
    fun `updateChildData sends new fee decision data to varda`() {
        db.transaction { it.insertVardaChild(testChild_1.id) }
        val since = HelsinkiDateTime.now()
        val serviceNeedPeriod = DateRange(since.minusDays(100).toLocalDate(), since.toLocalDate())
        val feeDecisionPeriod =
            DateRange(serviceNeedPeriod.start, serviceNeedPeriod.start.plusDays(10))
        val voucherDecisionPeriod = DateRange(feeDecisionPeriod.end!!.plusDays(1), null)
        val child = testChild_1
        val adult = testAdult_1
        createServiceNeed(
            db,
            snDefaultDaycare,
            child,
            serviceNeedPeriod.start,
            serviceNeedPeriod.end!!
        )
        createVoucherDecision(
            db,
            voucherDecisionPeriod.start,
            voucherDecisionPeriod.end,
            testDaycare.id,
            VOUCHER_VALUE,
            VOUCHER_CO_PAYMENT,
            adult.id,
            child,
            since.toInstant(),
            VoucherValueDecisionStatus.SENT
        )
        updateChildData(db, vardaClient, evakaEnv.feeDecisionMinDate)
        assertVardaCallCounts(1)
        assertVardaElementCounts(1, 1, 1)
        createFeeDecision(db, child, adult.id, feeDecisionPeriod, since.toInstant())
        updateChildData(db, vardaClient, evakaEnv.feeDecisionMinDate)
        assertVardaElementCounts(1, 1, 2)
        assertVardaCallCounts(3)
        // test that fee data is not sent if not modified
        updateChildData(db, vardaClient, evakaEnv.feeDecisionMinDate)
        assertVardaElementCounts(1, 1, 2)
        assertVardaCallCounts(3)
    }

    @Test
    fun `updateChildData sends all fee decisions related to service need`() {
        db.transaction { it.insertVardaChild(testChild_1.id) }
        val since = HelsinkiDateTime.now()
        val serviceNeedPeriod = DateRange(since.minusDays(100).toLocalDate(), since.toLocalDate())

        val feeDecisionPeriod =
            DateRange(serviceNeedPeriod.start, serviceNeedPeriod.start.plusDays(30))
        val feeDecisionPeriod2 =
            DateRange(serviceNeedPeriod.start.plusDays(31), serviceNeedPeriod.start.plusDays(70))
        val feeDecisionPeriod3 =
            DateRange(serviceNeedPeriod.start.plusDays(71), serviceNeedPeriod.start.plusDays(100))

        val child = testChild_1
        val adult = testAdult_1
        createServiceNeed(
            db,
            snDefaultDaycare,
            child,
            serviceNeedPeriod.start,
            serviceNeedPeriod.end!!
        )
        createFeeDecision(db, child, adult.id, feeDecisionPeriod, since.toInstant())
        createFeeDecision(db, child, adult.id, feeDecisionPeriod2, since.toInstant())
        createFeeDecision(db, child, adult.id, feeDecisionPeriod3, since.toInstant())
        updateChildData(db, vardaClient, evakaEnv.feeDecisionMinDate)
        assertVardaElementCounts(1, 1, 3)
    }

    @Test
    fun `updateChildData does not send fee decisions related to service need if daycare is not a varda daycare`() {
        db.transaction { it.insertVardaChild(testChild_1.id) }
        val since = HelsinkiDateTime.now()
        val serviceNeedPeriod = DateRange(since.minusDays(100).toLocalDate(), since.toLocalDate())
        val feeDecisionPeriod =
            DateRange(serviceNeedPeriod.start, serviceNeedPeriod.start.plusDays(30))

        val child = testChild_1
        val adult = testAdult_1
        createServiceNeed(
            db,
            snDefaultDaycare,
            child,
            serviceNeedPeriod.start,
            serviceNeedPeriod.end!!,
            unitId = ghostUnitDaycare.id
        )
        createFeeDecision(
            db,
            child,
            adult.id,
            feeDecisionPeriod,
            since.toInstant(),
            daycareId = ghostUnitDaycare.id
        )

        updateChildData(db, vardaClient, evakaEnv.feeDecisionMinDate)
        assertVardaElementCounts(0, 0, 0)
    }

    @Test
    fun `updateChildData retries failed service need addition`() {
        db.transaction { it.insertVardaChild(testChild_1.id) }
        val since = HelsinkiDateTime.now()
        val serviceNeedPeriod = DateRange(since.minusDays(100).toLocalDate(), since.toLocalDate())
        val feeDecisionPeriod =
            DateRange(serviceNeedPeriod.start, serviceNeedPeriod.start.plusDays(10))
        val voucherDecisionPeriod = DateRange(feeDecisionPeriod.end!!.plusDays(1), null)
        val snId =
            createServiceNeedAndFeeData(
                testChild_1,
                testAdult_1,
                since,
                serviceNeedPeriod,
                feeDecisionPeriod,
                voucherDecisionPeriod
            )
        mockEndpoint.failNextVardaCall(
            400,
            MockVardaIntegrationEndpoint.VardaCallType.FEE_DATA,
            mockEndpoint.getMockErrorResponseForFeeData()
        )
        updateChildData(db, vardaClient, evakaEnv.feeDecisionMinDate)
        assertFailedVardaUpdates(1)
        assertEquals(1, getVardaServiceNeedError(snId).size)
        assertVardaElementCounts(1, 1, 0)
        assertVardaServiceNeedIds(snId, 1, 1)

        updateChildData(db, vardaClient, evakaEnv.feeDecisionMinDate)
        assertFailedVardaUpdates(0)
        assertEquals(0, getVardaServiceNeedError(snId).size)
        assertVardaElementCounts(1, 1, 2)
        assertVardaServiceNeedIds(snId, 2, 2)

        val vardaDecision = mockEndpoint.decisions.values.elementAt(0)
        assertVardaDecision(
            vardaDecision,
            serviceNeedPeriod.start,
            serviceNeedPeriod.end!!,
            serviceNeedPeriod.start.minusDays(15),
            2,
            snDefaultDaycare.daycareHoursPerWeek.toDouble()
        )
        assertVardaFeeData(
            VardaFeeData(
                huoltajat = listOf(asVardaGuardian(testAdult_1)),
                maksun_peruste_koodi = "MP03",
                asiakasmaksu = 289.0,
                palveluseteli_arvo = 0.0,
                alkamis_pvm = feeDecisionPeriod.start,
                paattymis_pvm = feeDecisionPeriod.end,
                perheen_koko = 2,
                lahdejarjestelma = "SourceSystemVarda",
                lapsi = "not asserted"
            ),
            mockEndpoint.feeData.values.elementAt(0)
        )
        assertVardaFeeData(
            VardaFeeData(
                huoltajat = listOf(asVardaGuardian(testAdult_1)),
                maksun_peruste_koodi = "MP03",
                asiakasmaksu = 50.0,
                palveluseteli_arvo = 100.0,
                alkamis_pvm = voucherDecisionPeriod.start,
                paattymis_pvm = serviceNeedPeriod.end,
                perheen_koko = 2,
                lahdejarjestelma = "SourceSystemVarda",
                lapsi = "not asserted"
            ),
            mockEndpoint.feeData.values.elementAt(1)
        )
    }

    @Test
    fun `updateChildData deletes if service need changes to bad placement type`() {
        val snId = insertServiceNeedWithFeeDecision()

        updateChildData(db, vardaClient, evakaEnv.feeDecisionMinDate)
        assertVardaElementCounts(1, 1, 1)
        assertVardaServiceNeedIds(snId, 1, 1)

        db.transaction {
            @Suppress("DEPRECATION")
            it.createUpdate("update placement set type = 'PRESCHOOL'::placement_type").execute()
        }

        updateChildData(db, vardaClient, evakaEnv.feeDecisionMinDate)

        val vardaServiceNeed = db.read { it.getVardaServiceNeedByEvakaServiceNeedId(snId) }
        assertNull(vardaServiceNeed)
    }

    @Test
    fun `updateChildData deletes if service need hours is set to 0`() {
        val snId = insertServiceNeedWithFeeDecision()

        updateChildData(db, vardaClient, evakaEnv.feeDecisionMinDate)
        assertVardaElementCounts(1, 1, 1)
        assertVardaServiceNeedIds(snId, 1, 1)

        db.transaction {
            @Suppress("DEPRECATION")
            it.createUpdate("update service_need set option_id = :id")
                .bind("id", snDefaultPreschool.id)
                .execute()
        }

        updateChildData(db, vardaClient, evakaEnv.feeDecisionMinDate)

        val vardaServiceNeed = db.read { it.getVardaServiceNeedByEvakaServiceNeedId(snId) }
        assertNull(vardaServiceNeed)
    }

    @Test
    fun `updateChildData deletes if service need start_date is updated to future`() {
        val snId = insertServiceNeedWithFeeDecision()

        db.transaction {
            @Suppress("DEPRECATION")
            it.createUpdate("update service_need set start_date = :startDate, end_date = :endDate")
                .bind("startDate", LocalDate.now().plusYears(1))
                .bind("endDate", LocalDate.now().plusYears(2))
                .execute()
        }

        updateChildData(db, vardaClient, evakaEnv.feeDecisionMinDate)

        val vardaServiceNeed = db.read { it.getVardaServiceNeedByEvakaServiceNeedId(snId) }
        assertNull(vardaServiceNeed)
    }

    @Test
    fun `updateChildData does not delete if service need is not changed`() {
        val snId = insertServiceNeedWithFeeDecision()

        updateChildData(db, vardaClient, evakaEnv.feeDecisionMinDate)
        assertVardaElementCounts(1, 1, 1)
        assertVardaServiceNeedIds(snId, 1, 1)

        updateChildData(db, vardaClient, evakaEnv.feeDecisionMinDate)
        assertVardaServiceNeedIds(snId, 1, 1)
    }

    @Test
    fun `getEvakaServiceNeedInfoForVarda handles extrnal purchased`() {
        db.transaction { it.insertVardaChild(testChild_1.id) }
        val since = HelsinkiDateTime.now()
        val serviceNeedPeriod = DateRange(since.minusDays(100).toLocalDate(), since.toLocalDate())
        val snId =
            createServiceNeed(
                db,
                snDefaultDaycare,
                testChild_1,
                serviceNeedPeriod.start,
                serviceNeedPeriod.end!!,
                PlacementType.DAYCARE,
                externalPurchasedDaycare.id
            )

        val result = db.read { it.getEvakaServiceNeedInfoForVarda(snId) }

        assertEquals("jm02", result.toVardaDecisionForChild("", "").jarjestamismuoto_koodi)
    }

    @Test
    fun `parseVardaErrorBody works`() {
        val errors = mockEndpoint.getMockErrorResponses()

        vardaClient.parseVardaErrorBody(errors["DY004"]!!).let { (codes, descriptions) ->
            assertEquals("DY004", codes.first())
            assertEquals("Ensure this value is greater than or equal to 1.", descriptions.first())
        }
        vardaClient.parseVardaErrorBody(errors["VS009"]!!).let { (codes, descriptions) ->
            assertEquals("VS009", codes.first())
            assertEquals(
                "Varhaiskasvatussuhde alkamis_pvm cannot be after Toimipaikka paattymis_pvm.",
                descriptions.first()
            )
        }
        vardaClient.parseVardaErrorBody(errors["MA003"]!!).let { (codes, descriptions) ->
            assertEquals("MA003", codes.first())
            assertEquals("No matching huoltaja found.", descriptions.first())
        }
        vardaClient.parseVardaErrorBody(errors["HE012"]!!).let { (codes, descriptions) ->
            assertEquals("HE012", codes.first())
            assertEquals("Name has disallowed characters.", descriptions.first())
        }
    }

    // TODO: find a way to run update process through async job mechanism in tests (ie. use correct
    // varda client)
    private fun updateChildData(
        db: Database.Connection,
        vardaClient: VardaClient,
        feeDecisionMinDate: LocalDate
    ) {
        getChildrenToUpdate(db, RealEvakaClock(), feeDecisionMinDate).entries.forEach {
            resetVardaChild(
                db,
                RealEvakaClock(),
                vardaClient,
                it.key,
                feeDecisionMinDate,
                ophEnv.organizerOid
            )
        }
    }

    private fun insertServiceNeedWithFeeDecision(): ServiceNeedId {
        db.transaction { it.insertVardaChild(testChild_1.id) }
        val since = HelsinkiDateTime.now()
        val serviceNeedPeriod = DateRange(since.minusDays(100).toLocalDate(), since.toLocalDate())
        val feeDecisionPeriod =
            DateRange(serviceNeedPeriod.start, serviceNeedPeriod.start.plusDays(30))

        val child = testChild_1
        val adult = testAdult_1
        val snId =
            createServiceNeed(
                db,
                snDefaultDaycare,
                child,
                serviceNeedPeriod.start,
                serviceNeedPeriod.end!!,
                unitId = testDaycare.id
            )
        createFeeDecision(
            db,
            child,
            adult.id,
            feeDecisionPeriod,
            since.toInstant(),
            daycareId = testDaycare.id
        )
        return snId
    }

    private fun assertFailedVardaUpdates(n: Int) {
        val failures =
            db.read {
                @Suppress("DEPRECATION")
                it.createQuery(
                        "SELECT update_failed FROM varda_service_need WHERE update_failed = true"
                    )
                    .toList<Boolean>()
            }
        assertEquals(n, failures.size)
    }

    private fun getVardaServiceNeedError(snId: ServiceNeedId): List<String> {
        return db.read {
            @Suppress("DEPRECATION")
            it.createQuery(
                    "SELECT errors[0] FROM varda_service_need WHERE evaka_service_need_id = :snId AND array_length(errors, 1) > 0"
                )
                .bind("snId", snId)
                .toList<String>()
        }
    }

    private fun assertVardaServiceNeedIds(
        evakaServiceNeedId: ServiceNeedId,
        expectedVardaDecisionId: Long,
        expectedVardaPlacementId: Long
    ) {
        val vardaServiceNeed =
            db.read { it.getVardaServiceNeedByEvakaServiceNeedId(evakaServiceNeedId) }
        assertNotNull(vardaServiceNeed)
        assertEquals(expectedVardaDecisionId, vardaServiceNeed.vardaDecisionId)
        assertEquals(expectedVardaPlacementId, vardaServiceNeed.vardaPlacementId)
    }

    private fun createServiceNeedAndFeeData(
        child: DevPerson,
        adult: DevPerson,
        since: HelsinkiDateTime,
        serviceNeedPeriod: DateRange,
        feeDecisionPeriod: DateRange,
        voucherDecisionPeriod: DateRange
    ): ServiceNeedId {
        val id =
            createServiceNeed(
                db,
                snDefaultDaycare,
                child,
                serviceNeedPeriod.start,
                serviceNeedPeriod.end!!
            )
        createFeeDecision(
            db,
            child,
            adult.id,
            DateRange(feeDecisionPeriod.start, feeDecisionPeriod.end),
            since.toInstant()
        )
        createVoucherDecision(
            db,
            voucherDecisionPeriod.start,
            voucherDecisionPeriod.end,
            testDaycare.id,
            VOUCHER_VALUE,
            VOUCHER_CO_PAYMENT,
            adult.id,
            child,
            since.toInstant(),
            VoucherValueDecisionStatus.SENT
        )
        return id
    }

    private fun asVardaGuardian(g: DevPerson): VardaGuardian =
        VardaGuardian(henkilotunnus = g.ssn ?: "", etunimet = g.firstName, sukunimi = g.lastName)

    private fun assertVardaDecision(
        vardaDecision: VardaDecision,
        expectedStartDate: LocalDate,
        expectedEndDate: LocalDate,
        expectedApplicationDate: LocalDate,
        expectedChildId: Long,
        expectedHoursPerWeek: Double
    ) {
        assertEquals(
            true,
            vardaDecision.lapsi.endsWith("$expectedChildId/"),
            "Expected ${vardaDecision.lapsi} to end with $expectedChildId"
        )
        assertEquals(expectedStartDate, vardaDecision.alkamis_pvm)
        assertEquals(expectedEndDate, vardaDecision.paattymis_pvm)
        assertEquals(expectedApplicationDate, vardaDecision.hakemus_pvm)
        assertEquals(expectedHoursPerWeek, vardaDecision.tuntimaara_viikossa)
    }

    private fun assertVardaFeeData(expectedVardaFeeData: VardaFeeData, vardaFeeData: VardaFeeData) {
        expectedVardaFeeData.huoltajat.forEach { expectedHuoltaja ->
            assertEquals(
                true,
                vardaFeeData.huoltajat.any { g ->
                    g.henkilotunnus == expectedHuoltaja.henkilotunnus
                }
            )
        }
        assertEquals(expectedVardaFeeData.maksun_peruste_koodi, vardaFeeData.maksun_peruste_koodi)
        assertEquals(expectedVardaFeeData.asiakasmaksu, vardaFeeData.asiakasmaksu)
        assertEquals(expectedVardaFeeData.palveluseteli_arvo, vardaFeeData.palveluseteli_arvo)
        assertEquals(expectedVardaFeeData.alkamis_pvm, vardaFeeData.alkamis_pvm)
        assertEquals(expectedVardaFeeData.paattymis_pvm, vardaFeeData.paattymis_pvm)
        assertEquals(expectedVardaFeeData.perheen_koko, vardaFeeData.perheen_koko)
        assertEquals(expectedVardaFeeData.lahdejarjestelma, vardaFeeData.lahdejarjestelma)
    }

    private fun assertVardaElementCounts(
        expectedDecisionCount: Int,
        expectedPlacementCount: Int,
        expectedFeeDataCount: Int
    ) {
        assertEquals(
            expectedDecisionCount,
            mockEndpoint.decisions.values.size,
            "Expected varda decision count $expectedDecisionCount does not match ${mockEndpoint.decisions.values.size}"
        )
        assertEquals(
            expectedPlacementCount,
            mockEndpoint.placements.values.size,
            "Expected varda placement count $expectedPlacementCount does not match ${mockEndpoint.placements.values.size}"
        )
        assertEquals(
            expectedFeeDataCount,
            mockEndpoint.feeData.values.size,
            "Expected varda fee data count $expectedFeeDataCount does not match ${mockEndpoint.feeData.values.size}"
        )
    }

    private fun assertVardaCallCounts(expectedFeeDataCallCount: Int) {
        assertEquals(
            expectedFeeDataCallCount,
            mockEndpoint.feeDataCalls,
            "Expected varda fee data call count $expectedFeeDataCallCount does not match ${mockEndpoint.feeDataCalls}"
        )
    }

    private fun assertServiceNeedDiffSizes(
        diff: VardaChildCalculatedServiceNeedChanges?,
        expectedAdditions: Int,
        expectedUpdates: Int,
        expectedDeletes: Int
    ) {
        assertNotNull(diff)
        assertEquals(expectedAdditions, diff.additions.size)
        assertEquals(expectedUpdates, diff.updates.size)
        assertEquals(expectedDeletes, diff.deletes.size)
    }

    private fun createServiceNeed(
        db: Database.Connection,
        option: ServiceNeedOption,
        child: DevPerson = testChild_1,
        fromDays: LocalDate = HelsinkiDateTime.now().minusDays(100).toLocalDate(),
        toDays: LocalDate = HelsinkiDateTime.now().toLocalDate(),
        placementType: PlacementType = PlacementType.DAYCARE,
        unitId: DaycareId = testDaycare.id
    ): ServiceNeedId {
        val placement =
            DevPlacement(
                childId = child.id,
                unitId = unitId,
                type = placementType,
                startDate = fromDays,
                endDate = toDays
            )
        val serviceNeed =
            DevServiceNeed(
                placementId = placement.id,
                optionId = option.id,
                startDate = fromDays,
                endDate = toDays,
                confirmedBy = EvakaUserId(testDecisionMaker_1.id.raw)
            )

        return db.transaction { tx ->
            tx.insert(placement)
            tx.upsertServiceNeedOption(option)
            tx.insert(serviceNeed)
        }
    }

    private fun createFeeDecision(
        db: Database.Connection,
        child: DevPerson,
        headOfFamilyId: PersonId,
        period: DateRange,
        sentAt: Instant,
        status: FeeDecisionStatus = FeeDecisionStatus.SENT,
        daycareId: DaycareId = testDaycare.id
    ): FeeDecisionId {
        val fd =
            createFeeDecisionFixture(
                status,
                FeeDecisionType.NORMAL,
                period,
                headOfFamilyId,
                listOf(
                    createFeeDecisionChildFixture(
                        child.id,
                        child.dateOfBirth,
                        daycareId,
                        PlacementType.DAYCARE,
                        snDefaultDaycare.toFeeDecisionServiceNeed()
                    )
                )
            )
        db.transaction { tx ->
            tx.upsertFeeDecisions(listOf(fd))
            tx.setFeeDecisionSentAt(fd.id, sentAt)
        }

        return fd.id
    }

    private fun createVoucherDecision(
        db: Database.Connection,
        validFrom: LocalDate,
        validTo: LocalDate?,
        unitId: DaycareId,
        value: Int,
        coPayment: Int,
        adultId: PersonId,
        child: DevPerson,
        sentAt: Instant,
        status: VoucherValueDecisionStatus = VoucherValueDecisionStatus.DRAFT
    ): VoucherValueDecision {
        return db.transaction {
            val decision =
                createVoucherValueDecisionFixture(
                    status,
                    validFrom = validFrom,
                    validTo = validTo,
                    headOfFamilyId = adultId,
                    childId = child.id,
                    dateOfBirth = child.dateOfBirth,
                    unitId = unitId,
                    value = value,
                    coPayment = coPayment,
                    placementType = PlacementType.DAYCARE,
                    serviceNeed = snDefaultDaycare.toValueDecisionServiceNeed()
                )
            it.upsertValueDecisions(listOf(decision))
            it.setVoucherValueDecisionSentAt(decision.id, sentAt)
            decision
        }
    }
}

private fun Database.Read.getChildIdByServiceNeedId(serviceNeedId: ServiceNeedId): ChildId? =
    @Suppress("DEPRECATION")
    createQuery(
            """
SELECT p.child_id FROM placement p LEFT JOIN service_need sn ON p.id = sn.placement_id
WHERE sn.id = :serviceNeedId
        """
        )
        .bind("serviceNeedId", serviceNeedId)
        .exactlyOneOrNull<ChildId>()

private fun ServiceNeedOption.toFeeDecisionServiceNeed() =
    FeeDecisionServiceNeed(
        optionId = this.id,
        feeCoefficient = this.feeCoefficient,
        contractDaysPerMonth = this.contractDaysPerMonth,
        descriptionFi = this.feeDescriptionFi,
        descriptionSv = this.feeDescriptionSv,
        missing = false
    )

private fun Database.Transaction.setFeeDecisionSentAt(id: FeeDecisionId, sentAt: Instant) =
    @Suppress("DEPRECATION")
    createUpdate("""
UPDATE fee_decision SET sent_at = :sentAt
WHERE id = :id 
        """)
        .bind("id", id)
        .bind("sentAt", sentAt)
        .execute()

private fun Database.Transaction.setVoucherValueDecisionSentAt(
    id: VoucherValueDecisionId,
    sentAt: Instant
) =
    @Suppress("DEPRECATION")
    createUpdate(
            """
UPDATE voucher_value_decision SET sent_at = :sentAt
WHERE id = :id 
        """
        )
        .bind("id", id)
        .bind("sentAt", sentAt)
        .execute()
