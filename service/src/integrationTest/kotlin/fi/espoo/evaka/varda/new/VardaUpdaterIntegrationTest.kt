// SPDX-FileCopyrightText: 2017-2024 City of Espoo
//
// SPDX-License-Identifier: LGPL-2.1-or-later

package fi.espoo.evaka.varda.new

import fi.espoo.evaka.PureJdbiTest
import fi.espoo.evaka.application.ApplicationStatus
import fi.espoo.evaka.daycare.domain.ProviderType
import fi.espoo.evaka.decision.DecisionStatus
import fi.espoo.evaka.decision.DecisionType
import fi.espoo.evaka.insertApplication
import fi.espoo.evaka.invoicing.domain.FeeDecisionStatus
import fi.espoo.evaka.invoicing.domain.VoucherValueDecisionStatus
import fi.espoo.evaka.pis.getPersonById
import fi.espoo.evaka.pis.service.insertGuardian
import fi.espoo.evaka.placement.PlacementType
import fi.espoo.evaka.serviceneed.ShiftCareType
import fi.espoo.evaka.shared.data.DateSet
import fi.espoo.evaka.shared.dev.DevCareArea
import fi.espoo.evaka.shared.dev.DevDaycare
import fi.espoo.evaka.shared.dev.DevEmployee
import fi.espoo.evaka.shared.dev.DevFeeDecision
import fi.espoo.evaka.shared.dev.DevFeeDecisionChild
import fi.espoo.evaka.shared.dev.DevPerson
import fi.espoo.evaka.shared.dev.DevPersonType
import fi.espoo.evaka.shared.dev.DevPlacement
import fi.espoo.evaka.shared.dev.DevServiceNeed
import fi.espoo.evaka.shared.dev.DevVoucherValueDecision
import fi.espoo.evaka.shared.dev.TestDecision
import fi.espoo.evaka.shared.dev.insert
import fi.espoo.evaka.shared.dev.insertServiceNeedOption
import fi.espoo.evaka.shared.dev.insertTestDecision
import fi.espoo.evaka.shared.domain.DateRange
import fi.espoo.evaka.shared.domain.FiniteDateRange
import fi.espoo.evaka.shared.domain.HelsinkiDateTime
import fi.espoo.evaka.snDaycareFullDay25to35
import fi.espoo.evaka.snDaycareFullDay35
import fi.espoo.evaka.snDaycareFullDayPartWeek25
import fi.espoo.evaka.snDaycarePartDay25
import fi.espoo.evaka.snDefaultTemporaryPartDayDaycare
import fi.espoo.evaka.snPreschoolDaycarePartDay35to45
import java.net.URI
import java.time.LocalDate
import java.time.LocalTime
import kotlin.test.assertEquals
import org.junit.jupiter.api.Test

class VardaUpdaterIntegrationTest : PureJdbiTest(resetDbBeforeEach = true) {
    @Test
    fun `evaka state - multiple placements, service needs, fee decisions and value decisions`() {
        val municipalOrganizerOid = "municipalOrganizerOid"
        val municipalUnit1Oid = "municipalUnit1Oid"
        val municipalUnit2Oid = "municipalUnit2Oid"

        val voucherOrganizerOid = "voucherOrganizerOid"
        val voucherUnitOid = "voucherUnitOid"

        val area = DevCareArea()
        val municipalDaycare1 =
            DevDaycare(
                areaId = area.id,
                providerType = ProviderType.MUNICIPAL,
                ophOrganizerOid = municipalOrganizerOid,
                ophUnitOid = municipalUnit1Oid
            )
        val municipalDaycare2 =
            DevDaycare(
                areaId = area.id,
                providerType = ProviderType.MUNICIPAL,
                ophOrganizerOid = municipalOrganizerOid,
                ophUnitOid = municipalUnit2Oid
            )
        val voucherDaycare =
            DevDaycare(
                areaId = area.id,
                providerType = ProviderType.PRIVATE_SERVICE_VOUCHER,
                ophOrganizerOid = voucherOrganizerOid,
                ophUnitOid = voucherUnitOid
            )
        val employee = DevEmployee()

        val guardian = DevPerson(ssn = "070644-937X")
        val child = DevPerson(ssn = "030320A904N")

        val mUnit1Range1 = FiniteDateRange(LocalDate.of(2021, 1, 1), LocalDate.of(2021, 6, 30))
        val mUnit1Range2 = FiniteDateRange(LocalDate.of(2021, 8, 1), LocalDate.of(2021, 12, 31))
        val vUnitRange = FiniteDateRange(LocalDate.of(2022, 1, 1), LocalDate.of(2022, 6, 30))
        val mUnit2Range = FiniteDateRange(LocalDate.of(2023, 1, 1), LocalDate.of(2023, 1, 31))
        val today = LocalDate.of(2024, 1, 1)

        val feeDecisionUnit1Range1 =
            DevFeeDecision(
                headOfFamilyId = guardian.id,
                validDuring = mUnit1Range1,
                status = FeeDecisionStatus.SENT,
                familySize = 2
            )
        val feeDecisionChildUnit1Range1 =
            DevFeeDecisionChild(
                feeDecisionId = feeDecisionUnit1Range1.id,
                childId = child.id,
                placementUnitId = municipalDaycare1.id,
                finalFee = 0
            )

        val feeDecisionUnit1Range2 =
            DevFeeDecision(
                headOfFamilyId = guardian.id,
                validDuring = mUnit1Range2,
                status = FeeDecisionStatus.SENT,
                familySize = 3
            )
        val feeDecisionChildUnit1Range2 =
            DevFeeDecisionChild(
                feeDecisionId = feeDecisionUnit1Range2.id,
                childId = child.id,
                placementUnitId = municipalDaycare1.id,
                finalFee = 1000
            )

        val voucherValueDecision =
            DevVoucherValueDecision(
                childId = child.id,
                headOfFamilyId = guardian.id,
                placementUnitId = voucherDaycare.id,
                validFrom = vUnitRange.start,
                validTo = vUnitRange.end,
                status = VoucherValueDecisionStatus.SENT,
                familySize = 4,
                voucherValue = 100000,
                finalCoPayment = 2000,
            )

        val feeDecisionUnit2 =
            DevFeeDecision(
                headOfFamilyId = guardian.id,
                validDuring = mUnit2Range,
                status = FeeDecisionStatus.SENT,
                familySize = 5
            )
        val feeDecisionChildUnit2 =
            DevFeeDecisionChild(
                feeDecisionId = feeDecisionUnit2.id,
                childId = child.id,
                placementUnitId = municipalDaycare2.id,
                finalFee = 3000
            )

        db.transaction { tx ->
            tx.insertServiceNeedOption(snDaycareFullDay35)
            tx.insertServiceNeedOption(snDaycareFullDayPartWeek25)

            tx.insert(area)
            tx.insert(municipalDaycare1)
            tx.insert(municipalDaycare2)
            tx.insert(voucherDaycare)
            tx.insert(employee)

            tx.insert(guardian, DevPersonType.ADULT)
            tx.insert(child, DevPersonType.CHILD)
            tx.insertGuardian(guardian.id, child.id)

            tx.insert(
                    DevPlacement(
                        childId = child.id,
                        unitId = municipalDaycare1.id,
                        startDate = mUnit1Range1.start,
                        endDate = mUnit1Range2.end
                    )
                )
                .also { placementId ->
                    tx.insert(
                        DevServiceNeed(
                            placementId = placementId,
                            startDate = mUnit1Range1.start,
                            endDate = mUnit1Range1.end,
                            optionId = snDaycareFullDay35.id,
                            confirmedBy = employee.evakaUserId,
                            confirmedAt = HelsinkiDateTime.now()
                        )
                    )
                    tx.insert(
                        DevServiceNeed(
                            placementId = placementId,
                            startDate = mUnit1Range2.start,
                            endDate = mUnit1Range2.end,
                            optionId = snDaycareFullDayPartWeek25.id,
                            confirmedBy = employee.evakaUserId,
                            confirmedAt = HelsinkiDateTime.now()
                        )
                    )
                }
            tx.insert(feeDecisionUnit1Range1)
            tx.insert(feeDecisionChildUnit1Range1)
            tx.insert(feeDecisionUnit1Range2)
            tx.insert(feeDecisionChildUnit1Range2)

            tx.insert(
                    DevPlacement(
                        childId = child.id,
                        unitId = voucherDaycare.id,
                        startDate = vUnitRange.start,
                        endDate = vUnitRange.end
                    )
                )
                .also { placementId ->
                    tx.insert(
                        DevServiceNeed(
                            placementId = placementId,
                            startDate = vUnitRange.start,
                            endDate = vUnitRange.end,
                            optionId = snDaycareFullDay35.id,
                            confirmedBy = employee.evakaUserId,
                            confirmedAt = HelsinkiDateTime.now()
                        )
                    )
                }
            tx.insert(voucherValueDecision)

            tx.insert(
                    DevPlacement(
                        childId = child.id,
                        unitId = municipalDaycare2.id,
                        startDate = mUnit2Range.start,
                        endDate = mUnit2Range.end
                    )
                )
                .also { placementId ->
                    tx.insert(
                        DevServiceNeed(
                            placementId = placementId,
                            startDate = mUnit2Range.start,
                            endDate = mUnit2Range.end,
                            optionId = snDaycareFullDay35.id,
                            confirmedBy = employee.evakaUserId,
                            confirmedAt = HelsinkiDateTime.now()
                        )
                    )
                }
            tx.insert(feeDecisionUnit2)
            tx.insert(feeDecisionChildUnit2)
        }

        val updater =
            VardaUpdater(
                DateRange(LocalDate.of(2019, 1, 1), null),
                municipalOrganizerOid,
                "sourceSystem"
            )

        val evakaState = db.read { updater.getEvakaState(it, today, child.id) }

        assertEquals(
            VardaUpdater.EvakaHenkiloNode(
                henkilo =
                    Henkilo(
                        etunimet = child.firstName,
                        sukunimi = child.lastName,
                        henkilo_oid = null,
                        henkilotunnus = child.ssn,
                    ),
                lapset =
                    listOf(
                        VardaUpdater.EvakaLapsiNode(
                            lapsi =
                                Lapsi(
                                    vakatoimija_oid = municipalOrganizerOid,
                                    oma_organisaatio_oid = null,
                                    paos_organisaatio_oid = null
                                ),
                            varhaiskasvatuspaatokset =
                                listOf(
                                    varhaiskasvatuspaatos(municipalUnit1Oid, mUnit1Range1),
                                    varhaiskasvatuspaatos(
                                        municipalUnit1Oid,
                                        mUnit1Range2,
                                        tuntimaara_viikossa = 25.0
                                    ),
                                    varhaiskasvatuspaatos(municipalUnit2Oid, mUnit2Range),
                                ),
                            maksutiedot =
                                listOf(
                                    maksutieto(
                                        range = mUnit1Range1,
                                        perheen_koko = 2,
                                        asiakasmaksu = 0.0,
                                    ),
                                    maksutieto(
                                        range = mUnit1Range2,
                                        perheen_koko = 3,
                                        asiakasmaksu = 10.0,
                                    ),
                                    maksutieto(
                                        range = mUnit2Range,
                                        perheen_koko = 5,
                                        asiakasmaksu = 30.0,
                                    ),
                                )
                        ),
                        VardaUpdater.EvakaLapsiNode(
                            lapsi =
                                Lapsi(
                                    vakatoimija_oid = null,
                                    oma_organisaatio_oid = municipalOrganizerOid,
                                    paos_organisaatio_oid = voucherOrganizerOid
                                ),
                            varhaiskasvatuspaatokset =
                                listOf(
                                    varhaiskasvatuspaatos(
                                        voucherUnitOid,
                                        vUnitRange,
                                        jarjestamismuoto_koodi = "jm03" // voucher unit
                                    )
                                ),
                            maksutiedot =
                                listOf(
                                    maksutieto(
                                        range = vUnitRange,
                                        perheen_koko = 4,
                                        palveluseteli_arvo = 1000.0,
                                        asiakasmaksu = 20.0,
                                    )
                                )
                        )
                    )
            ),
            evakaState
        )
    }

    @Test
    fun `evaka state - child in a unit without oph unit id gets an empty state`() {
        val area = DevCareArea()
        val unit = DevDaycare(areaId = area.id, ophUnitOid = null)
        val employee = DevEmployee()

        val child = DevPerson(ssn = "030320A904N")
        val placementRange = FiniteDateRange(LocalDate.of(2021, 1, 1), LocalDate.of(2021, 6, 30))
        val today = LocalDate.of(2024, 1, 1)

        db.transaction { tx ->
            tx.insertServiceNeedOption(snDaycareFullDay35)
            tx.insert(area)
            tx.insert(unit)
            tx.insert(employee)

            tx.insert(child, DevPersonType.CHILD)
            tx.insert(
                    DevPlacement(
                        childId = child.id,
                        unitId = unit.id,
                        startDate = placementRange.start,
                        endDate = placementRange.end
                    )
                )
                .also { placementId ->
                    tx.insert(
                        DevServiceNeed(
                            placementId = placementId,
                            startDate = placementRange.start,
                            endDate = placementRange.end,
                            optionId = snDaycareFullDay35.id,
                            confirmedBy = employee.evakaUserId,
                            confirmedAt = HelsinkiDateTime.now()
                        )
                    )
                }
        }

        val updater =
            VardaUpdater(
                DateRange(LocalDate.of(2019, 1, 1), null),
                "municipalOrganizerOid",
                "sourceSystem"
            )

        val evakaState = db.read { updater.getEvakaState(it, today, child.id) }
        assertEquals(
            VardaUpdater.EvakaHenkiloNode(
                henkilo =
                    Henkilo(
                        etunimet = child.firstName,
                        sukunimi = child.lastName,
                        henkilo_oid = null,
                        henkilotunnus = child.ssn,
                    ),
                lapset = emptyList()
            ),
            evakaState
        )
    }

    @Test
    fun `evaka state - service need option mapping`() {
        val municipalOrganizerOid = "municipalOrganizerOid"
        val unitOid = "unitOid"

        val area = DevCareArea()
        val unit =
            DevDaycare(
                areaId = area.id,
                ophOrganizerOid = municipalOrganizerOid,
                ophUnitOid = unitOid
            )
        val employee = DevEmployee()

        val child = DevPerson(ssn = "030320A904N")

        val startDate = LocalDate.of(2021, 1, 1)
        val start = { i: Int -> startDate.plusMonths(i.toLong()) }
        val end = { i: Int -> startDate.plusMonths(i.toLong() + 1).minusDays(1) }
        val range = { i: Int -> FiniteDateRange(start(i), end(i)) }

        val serviceNeeds =
            listOf(
                snDaycareFullDay35 to ShiftCareType.NONE,
                snDaycareFullDay25to35 to ShiftCareType.NONE,
                snDaycareFullDayPartWeek25 to ShiftCareType.NONE,
                snDaycarePartDay25 to ShiftCareType.NONE,
                snPreschoolDaycarePartDay35to45 to ShiftCareType.NONE,
                snDefaultTemporaryPartDayDaycare to ShiftCareType.NONE,
                snDaycareFullDay35 to ShiftCareType.FULL
            )

        val today = LocalDate.of(2024, 1, 1)

        db.transaction { tx ->
            serviceNeeds.map { it.first }.toSet().forEach { tx.insertServiceNeedOption(it) }
            tx.insert(area)
            tx.insert(unit)
            tx.insert(employee)

            tx.insert(child, DevPersonType.CHILD)
            tx.insert(
                    DevPlacement(
                        childId = child.id,
                        unitId = unit.id,
                        startDate = start(0),
                        endDate = end(serviceNeeds.size)
                    )
                )
                .also { placementId ->
                    serviceNeeds.forEachIndexed { i, (sno, shiftCare) ->
                        val period = range(i)
                        tx.insert(
                            DevServiceNeed(
                                placementId = placementId,
                                startDate = period.start,
                                endDate = period.end,
                                optionId = sno.id,
                                shiftCare = shiftCare,
                                partWeek = sno.partWeek!!,
                                confirmedBy = employee.evakaUserId,
                                confirmedAt = HelsinkiDateTime.now()
                            )
                        )
                    }
                }
        }

        val updater =
            VardaUpdater(
                DateRange(LocalDate.of(2019, 1, 1), null),
                municipalOrganizerOid,
                "sourceSystem"
            )

        val evakaState = db.read { updater.getEvakaState(it, today, child.id) }

        assertEquals(
            VardaUpdater.EvakaHenkiloNode(
                henkilo =
                    Henkilo(
                        etunimet = child.firstName,
                        sukunimi = child.lastName,
                        henkilo_oid = null,
                        henkilotunnus = child.ssn,
                    ),
                lapset =
                    listOf(
                        VardaUpdater.EvakaLapsiNode(
                            lapsi =
                                Lapsi(
                                    vakatoimija_oid = municipalOrganizerOid,
                                    oma_organisaatio_oid = null,
                                    paos_organisaatio_oid = null
                                ),
                            varhaiskasvatuspaatokset =
                                listOf(
                                    // snDaycareFullDay35,
                                    varhaiskasvatuspaatos(
                                        unitOid,
                                        range(0),
                                        tuntimaara_viikossa = 35.0,
                                    ),
                                    // snDaycareFullDay25to35,
                                    varhaiskasvatuspaatos(
                                        unitOid,
                                        range(1),
                                        tuntimaara_viikossa = 30.0,
                                    ),
                                    // snDaycareFullDayPartWeek25,
                                    varhaiskasvatuspaatos(
                                        unitOid,
                                        range(2),
                                        tuntimaara_viikossa = 25.0,
                                        paivittainen_vaka_kytkin = false
                                    ),
                                    // snDaycarePartDay25,
                                    varhaiskasvatuspaatos(
                                        unitOid,
                                        range(3),
                                        tuntimaara_viikossa = 25.0
                                    ),
                                    // snPreschoolDaycarePartDay35to45
                                    varhaiskasvatuspaatos(
                                        unitOid,
                                        range(4),
                                        tuntimaara_viikossa = 20.0,
                                        kokopaivainen_vaka_kytkin = false
                                    ),
                                    // snDefaultTemporaryPartDayDaycare
                                    varhaiskasvatuspaatos(
                                        unitOid,
                                        range(5),
                                        tuntimaara_viikossa = 25.0,
                                        paivittainen_vaka_kytkin = false,
                                        tilapainen_vaka_kytkin = true
                                    ),
                                    // snDaycareFullDay35 with shift care
                                    varhaiskasvatuspaatos(
                                        unitOid,
                                        range(6),
                                        tuntimaara_viikossa = 35.0,
                                        vuorohoito_kytkin = true,
                                        kokopaivainen_vaka_kytkin = false,
                                        paivittainen_vaka_kytkin = false
                                    ),
                                ),
                            maksutiedot = emptyList()
                        )
                    )
            ),
            evakaState
        )
    }

    @Test
    fun `evaka state - jarjestamismuoto mapping`() {
        val today = LocalDate.of(2024, 1, 1)

        val municipalOrganizerOid = "municipalOrganizerOid"
        val ophOrganizerOid = "organizerOid"
        val startDate = LocalDate.of(2021, 1, 1)

        val employee = DevEmployee()
        val area = DevCareArea()

        class TestItem(index: Long, providerType: ProviderType, val jarjestamismuoto: String) {
            val unitOid = "unitOid$index"
            val range =
                FiniteDateRange(
                    startDate.plusMonths(index),
                    startDate.plusMonths(index + 1).minusDays(1)
                )
            val unit =
                DevDaycare(
                    areaId = area.id,
                    providerType = providerType,
                    ophOrganizerOid =
                        if (providerType == ProviderType.PRIVATE_SERVICE_VOUCHER) ophOrganizerOid
                        else municipalOrganizerOid,
                    ophUnitOid = unitOid
                )
        }

        val testItems =
            listOf(
                TestItem(0, ProviderType.MUNICIPAL, "jm01"),
                TestItem(1, ProviderType.PURCHASED, "jm02"),
                TestItem(2, ProviderType.PRIVATE, "jm04"),
                TestItem(3, ProviderType.MUNICIPAL_SCHOOL, "jm01"),
                TestItem(4, ProviderType.PRIVATE_SERVICE_VOUCHER, "jm03"),
                TestItem(5, ProviderType.EXTERNAL_PURCHASED, "jm02"),
            )

        val child = DevPerson(ssn = "030320A904N")

        db.transaction { tx ->
            tx.insertServiceNeedOption(snDaycareFullDay35)
            tx.insert(employee)
            tx.insert(area)
            testItems.forEach { tx.insert(it.unit) }
            tx.insert(child, DevPersonType.CHILD)

            testItems.forEach {
                tx.insert(
                        DevPlacement(
                            childId = child.id,
                            unitId = it.unit.id,
                            startDate = it.range.start,
                            endDate = it.range.end
                        )
                    )
                    .also { placementId ->
                        tx.insert(
                            DevServiceNeed(
                                placementId = placementId,
                                startDate = it.range.start,
                                endDate = it.range.end,
                                optionId = snDaycareFullDay35.id,
                                confirmedBy = employee.evakaUserId,
                                confirmedAt = HelsinkiDateTime.now()
                            )
                        )
                    }
            }
        }

        val updater =
            VardaUpdater(
                DateRange(LocalDate.of(2019, 1, 1), null),
                municipalOrganizerOid,
                "sourceSystem"
            )

        val evakaState = db.read { updater.getEvakaState(it, today, child.id) }

        assertEquals(
            VardaUpdater.EvakaHenkiloNode(
                henkilo =
                    Henkilo(
                        etunimet = child.firstName,
                        sukunimi = child.lastName,
                        henkilo_oid = null,
                        henkilotunnus = child.ssn,
                    ),
                lapset =
                    testItems
                        .groupBy { it.unit.providerType == ProviderType.PRIVATE_SERVICE_VOUCHER }
                        .map { (isVoucher, items) ->
                            VardaUpdater.EvakaLapsiNode(
                                lapsi =
                                    if (isVoucher)
                                        Lapsi(
                                            vakatoimija_oid = null,
                                            oma_organisaatio_oid = municipalOrganizerOid,
                                            paos_organisaatio_oid = ophOrganizerOid,
                                        )
                                    else
                                        Lapsi(
                                            vakatoimija_oid = municipalOrganizerOid,
                                            oma_organisaatio_oid = null,
                                            paos_organisaatio_oid = null
                                        ),
                                varhaiskasvatuspaatokset =
                                    items.map {
                                        varhaiskasvatuspaatos(
                                            it.unitOid,
                                            it.range,
                                            jarjestamismuoto_koodi = it.jarjestamismuoto
                                        )
                                    },
                                maksutiedot = emptyList()
                            )
                        }
            ),
            evakaState
        )
    }

    @Test
    fun `evaka state - 5 year old fee basis`() {
        val organizerOid = "organizerOid"
        val unitOid = "unitOid"

        val area = DevCareArea()
        val unit =
            DevDaycare(areaId = area.id, ophOrganizerOid = organizerOid, ophUnitOid = unitOid)
        val employee = DevEmployee()

        val guardian = DevPerson(ssn = "070644-937X")
        val child = DevPerson(ssn = "030320A904N")

        val placementRange = FiniteDateRange(LocalDate.of(2021, 1, 1), LocalDate.of(2021, 6, 30))
        val today = LocalDate.of(2024, 1, 1)

        db.transaction { tx ->
            tx.insertServiceNeedOption(snDaycareFullDay35)
            tx.insert(area)
            tx.insert(unit)
            tx.insert(employee)

            tx.insert(guardian, DevPersonType.ADULT)
            tx.insert(child, DevPersonType.CHILD)
            tx.insertGuardian(guardian.id, child.id)

            tx.insert(
                    DevPlacement(
                        type = PlacementType.DAYCARE_FIVE_YEAR_OLDS,
                        childId = child.id,
                        unitId = unit.id,
                        startDate = placementRange.start,
                        endDate = placementRange.end
                    )
                )
                .also { placementId ->
                    tx.insert(
                        DevServiceNeed(
                            placementId = placementId,
                            startDate = placementRange.start,
                            endDate = placementRange.end,
                            optionId = snDaycareFullDay35.id,
                            confirmedBy = employee.evakaUserId,
                            confirmedAt = HelsinkiDateTime.now()
                        )
                    )
                }

            tx.insert(
                    DevFeeDecision(
                        headOfFamilyId = guardian.id,
                        validDuring = placementRange,
                        status = FeeDecisionStatus.SENT
                    )
                )
                .also { feeDecisionId ->
                    tx.insert(
                        DevFeeDecisionChild(
                            feeDecisionId = feeDecisionId,
                            childId = child.id,
                            placementType = PlacementType.DAYCARE_FIVE_YEAR_OLDS,
                            placementUnitId = unit.id
                        )
                    )
                }
        }

        val updater =
            VardaUpdater(DateRange(LocalDate.of(2019, 1, 1), null), organizerOid, "sourceSystem")

        val evakaState = db.read { updater.getEvakaState(it, today, child.id) }
        assertEquals(
            VardaUpdater.EvakaHenkiloNode(
                henkilo =
                    Henkilo(
                        etunimet = child.firstName,
                        sukunimi = child.lastName,
                        henkilo_oid = null,
                        henkilotunnus = child.ssn,
                    ),
                lapset =
                    listOf(
                        VardaUpdater.EvakaLapsiNode(
                            lapsi =
                                Lapsi(
                                    vakatoimija_oid = organizerOid,
                                    oma_organisaatio_oid = null,
                                    paos_organisaatio_oid = null
                                ),
                            varhaiskasvatuspaatokset =
                                listOf(
                                    varhaiskasvatuspaatos(
                                        unitOid,
                                        placementRange,
                                        tuntimaara_viikossa = 35.0,
                                    )
                                ),
                            maksutiedot =
                                listOf(
                                    maksutieto(
                                        range = placementRange,
                                        maksun_peruste_koodi = "MP02",
                                    )
                                )
                        )
                    )
            ),
            evakaState
        )
    }

    @Test
    fun `evaka state - time ranges are cut to the varda enabled range`() {
        val organizerOid = "organizerOid"
        val unitOid = "unitOid"

        val area = DevCareArea()
        val daycare =
            DevDaycare(areaId = area.id, ophOrganizerOid = organizerOid, ophUnitOid = unitOid)
        val employee = DevEmployee()

        val guardian = DevPerson(ssn = "070644-937X")
        val child = DevPerson(ssn = "030320A904N")

        val placementRange1 = FiniteDateRange(LocalDate.of(2020, 1, 1), LocalDate.of(2021, 3, 31))
        val placementRange2 = FiniteDateRange(LocalDate.of(2021, 9, 1), LocalDate.of(2022, 12, 31))
        val vardaEnabledRange = DateRange(LocalDate.of(2021, 1, 1), LocalDate.of(2021, 12, 31))
        val today = LocalDate.of(2024, 1, 1)

        db.transaction { tx ->
            tx.insertServiceNeedOption(snDaycareFullDay35)

            tx.insert(area)
            tx.insert(daycare)
            tx.insert(employee)

            tx.insert(guardian, DevPersonType.ADULT)
            tx.insert(child, DevPersonType.CHILD)
            tx.insertGuardian(guardian.id, child.id)

            tx.insert(
                    DevPlacement(
                        childId = child.id,
                        unitId = daycare.id,
                        startDate = placementRange1.start,
                        endDate = placementRange1.end
                    )
                )
                .also { placementId ->
                    tx.insert(
                        DevServiceNeed(
                            placementId = placementId,
                            startDate = placementRange1.start,
                            endDate = placementRange1.end,
                            optionId = snDaycareFullDay35.id,
                            confirmedBy = employee.evakaUserId,
                            confirmedAt = HelsinkiDateTime.now()
                        )
                    )
                }
            tx.insert(
                    DevFeeDecision(
                        headOfFamilyId = guardian.id,
                        validDuring = placementRange1,
                        status = FeeDecisionStatus.SENT
                    )
                )
                .let { feeDecisionId ->
                    tx.insert(
                        DevFeeDecisionChild(
                            feeDecisionId = feeDecisionId,
                            childId = child.id,
                            placementUnitId = daycare.id,
                            finalFee = 1000
                        )
                    )
                }

            tx.insert(
                    DevPlacement(
                        childId = child.id,
                        unitId = daycare.id,
                        startDate = placementRange2.start,
                        endDate = placementRange2.end
                    )
                )
                .also { placementId ->
                    tx.insert(
                        DevServiceNeed(
                            placementId = placementId,
                            startDate = placementRange2.start,
                            endDate = placementRange2.end,
                            optionId = snDaycareFullDay35.id,
                            confirmedBy = employee.evakaUserId,
                            confirmedAt = HelsinkiDateTime.now()
                        )
                    )
                }
            tx.insert(
                    DevFeeDecision(
                        headOfFamilyId = guardian.id,
                        validDuring = placementRange2,
                        status = FeeDecisionStatus.SENT
                    )
                )
                .let { feeDecisionId ->
                    tx.insert(
                        DevFeeDecisionChild(
                            feeDecisionId = feeDecisionId,
                            childId = child.id,
                            placementUnitId = daycare.id,
                            finalFee = 2000
                        )
                    )
                }
        }

        val updater = VardaUpdater(vardaEnabledRange, organizerOid, "sourceSystem")

        val evakaState = db.read { updater.getEvakaState(it, today, child.id) }

        assertEquals(
            VardaUpdater.EvakaHenkiloNode(
                henkilo =
                    Henkilo(
                        etunimet = child.firstName,
                        sukunimi = child.lastName,
                        henkilo_oid = null,
                        henkilotunnus = child.ssn,
                    ),
                lapset =
                    listOf(
                        VardaUpdater.EvakaLapsiNode(
                            lapsi =
                                Lapsi(
                                    vakatoimija_oid = organizerOid,
                                    oma_organisaatio_oid = null,
                                    paos_organisaatio_oid = null
                                ),
                            varhaiskasvatuspaatokset =
                                listOf(
                                    varhaiskasvatuspaatos(
                                        unitOid,
                                        FiniteDateRange(
                                            LocalDate.of(2021, 1, 1),
                                            LocalDate.of(2021, 3, 31)
                                        ),
                                    ),
                                    varhaiskasvatuspaatos(
                                        unitOid,
                                        FiniteDateRange(
                                            LocalDate.of(2021, 9, 1),
                                            LocalDate.of(2021, 12, 31)
                                        ),
                                    ),
                                ),
                            maksutiedot =
                                listOf(
                                    Maksutieto(
                                        alkamis_pvm = LocalDate.of(2021, 1, 1),
                                        paattymis_pvm = LocalDate.of(2021, 3, 31),
                                        perheen_koko = 2,
                                        maksun_peruste_koodi = "MP03",
                                        asiakasmaksu = 10.0,
                                        palveluseteli_arvo = 0.0,
                                        huoltajat =
                                            listOf(
                                                Huoltaja(
                                                    henkilotunnus = guardian.ssn,
                                                    henkilo_oid = null,
                                                    etunimet = guardian.firstName,
                                                    sukunimi = guardian.lastName
                                                )
                                            )
                                    ),
                                    Maksutieto(
                                        alkamis_pvm = LocalDate.of(2021, 9, 1),
                                        paattymis_pvm = LocalDate.of(2021, 12, 31),
                                        perheen_koko = 2,
                                        maksun_peruste_koodi = "MP03",
                                        asiakasmaksu = 20.0,
                                        palveluseteli_arvo = 0.0,
                                        huoltajat =
                                            listOf(
                                                Huoltaja(
                                                    henkilotunnus = guardian.ssn,
                                                    henkilo_oid = null,
                                                    etunimet = guardian.firstName,
                                                    sukunimi = guardian.lastName
                                                )
                                            )
                                    )
                                )
                        )
                    )
            ),
            evakaState
        )
    }

    @Test
    fun `evaka state - does not include service needs that start in the future`() {
        val organizerOid = "organizerOid"

        val area = DevCareArea()
        val daycare = DevDaycare(areaId = area.id, ophOrganizerOid = organizerOid)
        val employee = DevEmployee()

        val child = DevPerson(ssn = "030320A904N")

        val placementRange = FiniteDateRange(LocalDate.of(2024, 1, 2), LocalDate.of(2024, 12, 31))
        val today = LocalDate.of(2024, 1, 1)

        db.transaction { tx ->
            tx.insertServiceNeedOption(snDaycareFullDay35)

            tx.insert(area)
            tx.insert(daycare)
            tx.insert(employee)

            tx.insert(child, DevPersonType.CHILD)

            tx.insert(
                    DevPlacement(
                        childId = child.id,
                        unitId = daycare.id,
                        startDate = placementRange.start,
                        endDate = placementRange.end
                    )
                )
                .also { placementId ->
                    tx.insert(
                        DevServiceNeed(
                            placementId = placementId,
                            startDate = placementRange.start,
                            endDate = placementRange.end,
                            optionId = snDaycareFullDay35.id,
                            confirmedBy = employee.evakaUserId,
                            confirmedAt = HelsinkiDateTime.now()
                        )
                    )
                }
        }

        val updater =
            VardaUpdater(DateRange(LocalDate.of(2019, 1, 1), null), organizerOid, "sourceSystem")

        val evakaState = db.read { updater.getEvakaState(it, today, child.id) }

        assertEquals(
            VardaUpdater.EvakaHenkiloNode(
                henkilo =
                    Henkilo(
                        etunimet = child.firstName,
                        sukunimi = child.lastName,
                        henkilo_oid = null,
                        henkilotunnus = child.ssn,
                    ),
                lapset = emptyList()
            ),
            evakaState
        )
    }

    @Test
    fun `evaka state - fee data is included starting at 2019-09-01`() {
        // Varda explicitly instructs to only send fee data after 2019-09-01

        val organizerOid = "organizerOid"
        val unitOid = "unitOid"

        val area = DevCareArea()
        val unit =
            DevDaycare(areaId = area.id, ophOrganizerOid = organizerOid, ophUnitOid = unitOid)
        val employee = DevEmployee()

        val guardian = DevPerson(ssn = "070644-937X")
        val child = DevPerson(ssn = "030320A904N")

        val placementRange = FiniteDateRange(LocalDate.of(2019, 1, 1), LocalDate.of(2019, 12, 31))
        val today = LocalDate.of(2024, 1, 1)

        db.transaction { tx ->
            tx.insertServiceNeedOption(snDaycareFullDay35)
            tx.insert(area)
            tx.insert(unit)
            tx.insert(employee)

            tx.insert(guardian, DevPersonType.ADULT)
            tx.insert(child, DevPersonType.CHILD)
            tx.insertGuardian(guardian.id, child.id)

            tx.insert(
                    DevPlacement(
                        childId = child.id,
                        unitId = unit.id,
                        startDate = placementRange.start,
                        endDate = placementRange.end
                    )
                )
                .also { placementId ->
                    tx.insert(
                        DevServiceNeed(
                            placementId = placementId,
                            startDate = placementRange.start,
                            endDate = placementRange.end,
                            optionId = snDaycareFullDay35.id,
                            confirmedBy = employee.evakaUserId,
                            confirmedAt = HelsinkiDateTime.now()
                        )
                    )
                }

            tx.insert(
                    DevFeeDecision(
                        headOfFamilyId = guardian.id,
                        validDuring = placementRange,
                        status = FeeDecisionStatus.SENT
                    )
                )
                .also { feeDecisionId ->
                    tx.insert(
                        DevFeeDecisionChild(
                            feeDecisionId = feeDecisionId,
                            childId = child.id,
                            placementUnitId = unit.id
                        )
                    )
                }
        }

        val updater =
            VardaUpdater(DateRange(LocalDate.of(2019, 1, 1), null), organizerOid, "sourceSystem")

        val evakaState = db.read { updater.getEvakaState(it, today, child.id) }
        assertEquals(
            VardaUpdater.EvakaHenkiloNode(
                henkilo =
                    Henkilo(
                        etunimet = child.firstName,
                        sukunimi = child.lastName,
                        henkilo_oid = null,
                        henkilotunnus = child.ssn,
                    ),
                lapset =
                    listOf(
                        VardaUpdater.EvakaLapsiNode(
                            lapsi =
                                Lapsi(
                                    vakatoimija_oid = organizerOid,
                                    oma_organisaatio_oid = null,
                                    paos_organisaatio_oid = null
                                ),
                            varhaiskasvatuspaatokset =
                                listOf(varhaiskasvatuspaatos(unitOid, placementRange)),
                            maksutiedot =
                                listOf(
                                    maksutieto(
                                        range =
                                            placementRange.copy(start = LocalDate.of(2019, 9, 1))
                                    )
                                )
                        )
                    )
            ),
            evakaState
        )
    }

    @Test
    fun `evaka state - fee data that doesn't overlap with service needs is not included`() {
        val organizerOid = "organizerOid"
        val unitOid = "unitOid"

        val area = DevCareArea()
        val unit =
            DevDaycare(areaId = area.id, ophOrganizerOid = organizerOid, ophUnitOid = unitOid)
        val employee = DevEmployee()

        val guardian = DevPerson(ssn = "070644-937X")
        val child = DevPerson(ssn = "030320A904N")

        val placementRange = FiniteDateRange(LocalDate.of(2021, 1, 1), LocalDate.of(2021, 12, 31))
        val feeRanges =
            listOf(
                FiniteDateRange(LocalDate.of(2020, 12, 1), LocalDate.of(2020, 12, 31)),
                FiniteDateRange(LocalDate.of(2022, 1, 1), LocalDate.of(2022, 1, 31))
            )
        val today = LocalDate.of(2024, 1, 1)

        db.transaction { tx ->
            tx.insertServiceNeedOption(snDaycareFullDay35)
            tx.insert(area)
            tx.insert(unit)
            tx.insert(employee)

            tx.insert(guardian, DevPersonType.ADULT)
            tx.insert(child, DevPersonType.CHILD)
            tx.insertGuardian(guardian.id, child.id)

            tx.insert(
                    DevPlacement(
                        childId = child.id,
                        unitId = unit.id,
                        startDate = placementRange.start,
                        endDate = placementRange.end
                    )
                )
                .also { placementId ->
                    tx.insert(
                        DevServiceNeed(
                            placementId = placementId,
                            startDate = placementRange.start,
                            endDate = placementRange.end,
                            optionId = snDaycareFullDay35.id,
                            confirmedBy = employee.evakaUserId,
                            confirmedAt = HelsinkiDateTime.now()
                        )
                    )
                }

            feeRanges.forEach { feeRange ->
                tx.insert(
                        DevFeeDecision(
                            headOfFamilyId = guardian.id,
                            validDuring = feeRange,
                            status = FeeDecisionStatus.SENT
                        )
                    )
                    .also { feeDecisionId ->
                        tx.insert(
                            DevFeeDecisionChild(
                                feeDecisionId = feeDecisionId,
                                childId = child.id,
                                placementUnitId = unit.id
                            )
                        )
                    }
            }
        }

        val updater =
            VardaUpdater(DateRange(LocalDate.of(2019, 1, 1), null), organizerOid, "sourceSystem")

        val evakaState = db.read { updater.getEvakaState(it, today, child.id) }
        assertEquals(
            VardaUpdater.EvakaHenkiloNode(
                henkilo =
                    Henkilo(
                        etunimet = child.firstName,
                        sukunimi = child.lastName,
                        henkilo_oid = null,
                        henkilotunnus = child.ssn,
                    ),
                lapset =
                    listOf(
                        VardaUpdater.EvakaLapsiNode(
                            lapsi =
                                Lapsi(
                                    vakatoimija_oid = organizerOid,
                                    oma_organisaatio_oid = null,
                                    paos_organisaatio_oid = null
                                ),
                            varhaiskasvatuspaatokset =
                                listOf(varhaiskasvatuspaatos(unitOid, placementRange)),
                            maksutiedot = emptyList()
                        )
                    )
            ),
            evakaState
        )
    }

    @Test
    fun `evaka state - fee data is cut to service need ranges per lapsi`() {
        val municipalOrganizerOid = "municipalOrganizerOid"
        val paosOrganizerOid = "organizerOid1"
        val unitOid1 = "unitOid1"
        val unitOid2 = "unitOid2"

        val area = DevCareArea()
        val unit1 =
            DevDaycare(areaId = area.id, ophOrganizerOid = paosOrganizerOid, ophUnitOid = unitOid1)
        val unit2 =
            DevDaycare(
                areaId = area.id,
                ophOrganizerOid = municipalOrganizerOid,
                ophUnitOid = unitOid2
            )
        val employee = DevEmployee()

        val guardian = DevPerson(ssn = "070644-937X")
        val child = DevPerson(ssn = "030320A904N")

        val today = LocalDate.of(2023, 1, 1)
        val lapsi1placement1range =
            FiniteDateRange(LocalDate.of(2021, 1, 1), LocalDate.of(2021, 12, 31))
        val lapsi1placement2range =
            FiniteDateRange(LocalDate.of(2022, 1, 1), LocalDate.of(2022, 12, 31))
        val lapsi1range = DateSet.of(lapsi1placement1range, lapsi1placement2range).spanningRange()!!

        // This overlaps with a placement => it's cut to the placement's range
        val lapsi1fee1 = FiniteDateRange(LocalDate.of(2020, 12, 1), LocalDate.of(2021, 1, 31))

        // This overlaps with a placement of lapsi1 and a placement of lapsi2 => it's cut to the
        // range of lapsi1
        val lapsi1fee2 = FiniteDateRange(LocalDate.of(2021, 12, 1), LocalDate.of(2023, 1, 31))

        val lapsi2placement1range =
            FiniteDateRange(LocalDate.of(2023, 1, 1), LocalDate.of(2023, 12, 31))
        val lapsi2placement2range =
            FiniteDateRange(LocalDate.of(2024, 1, 1), LocalDate.of(2024, 12, 31))

        // This only overlaps with a placement that starts in the future => it's not included
        val lapsi2fee1 = FiniteDateRange(LocalDate.of(2024, 1, 1), LocalDate.of(2024, 12, 31))

        db.transaction { tx ->
            tx.insertServiceNeedOption(snDaycareFullDay35)
            tx.insert(area)
            tx.insert(unit1)
            tx.insert(unit2)
            tx.insert(employee)

            tx.insert(guardian, DevPersonType.ADULT)
            tx.insert(child, DevPersonType.CHILD)
            tx.insertGuardian(guardian.id, child.id)

            listOf(
                    unit1.id to lapsi1placement1range,
                    unit1.id to lapsi1placement2range,
                    unit2.id to lapsi2placement1range,
                    unit2.id to lapsi2placement2range
                )
                .forEach { (unitId, range) ->
                    tx.insert(
                            DevPlacement(
                                childId = child.id,
                                unitId = unitId,
                                startDate = range.start,
                                endDate = range.end
                            )
                        )
                        .also { placementId ->
                            tx.insert(
                                DevServiceNeed(
                                    placementId = placementId,
                                    startDate = range.start,
                                    endDate = range.end,
                                    optionId = snDaycareFullDay35.id,
                                    confirmedBy = employee.evakaUserId,
                                )
                            )
                        }
                }
            listOf(unit1.id to lapsi1fee1, unit1.id to lapsi1fee2, unit2.id to lapsi2fee1)
                .forEach { (unitId, range) ->
                    tx.insert(
                            DevFeeDecision(
                                headOfFamilyId = guardian.id,
                                validDuring = range,
                                status = FeeDecisionStatus.SENT
                            )
                        )
                        .also { feeDecisionId ->
                            tx.insert(
                                DevFeeDecisionChild(
                                    feeDecisionId = feeDecisionId,
                                    childId = child.id,
                                    placementUnitId = unitId
                                )
                            )
                        }
                }
        }

        val updater =
            VardaUpdater(
                DateRange(LocalDate.of(2019, 1, 1), null),
                municipalOrganizerOid,
                "sourceSystem"
            )

        val evakaState = db.read { updater.getEvakaState(it, today, child.id) }
        assertEquals(
            VardaUpdater.EvakaHenkiloNode(
                henkilo =
                    Henkilo(
                        etunimet = child.firstName,
                        sukunimi = child.lastName,
                        henkilo_oid = null,
                        henkilotunnus = child.ssn,
                    ),
                lapset =
                    listOf(
                        VardaUpdater.EvakaLapsiNode(
                            lapsi =
                                Lapsi(
                                    vakatoimija_oid = null,
                                    oma_organisaatio_oid = municipalOrganizerOid,
                                    paos_organisaatio_oid = paosOrganizerOid
                                ),
                            varhaiskasvatuspaatokset =
                                // The future placement is not included
                                listOf(
                                    varhaiskasvatuspaatos(unitOid1, lapsi1placement1range),
                                    varhaiskasvatuspaatos(unitOid1, lapsi1placement2range)
                                ),
                            maksutiedot =
                                listOf(
                                    maksutieto(lapsi1fee1.intersection(lapsi1range)!!),
                                    maksutieto(lapsi1fee2.intersection(lapsi1range)!!)
                                )
                        ),
                        VardaUpdater.EvakaLapsiNode(
                            lapsi =
                                Lapsi(
                                    vakatoimija_oid = municipalOrganizerOid,
                                    oma_organisaatio_oid = null,
                                    paos_organisaatio_oid = null
                                ),
                            varhaiskasvatuspaatokset =
                                // The future placement is not included
                                listOf(varhaiskasvatuspaatos(unitOid2, lapsi2placement1range)),
                            maksutiedot = emptyList()
                        )
                    )
            ),
            evakaState
        )
    }

    @Test
    fun `evaka state - correct application date is picked up`() {
        val organizerOid = "organizerId"
        val unitOid = "unitId"

        val area = DevCareArea()
        val unit1 =
            DevDaycare(areaId = area.id, ophOrganizerOid = organizerOid, ophUnitOid = unitOid)
        val unit2 =
            DevDaycare(areaId = area.id, ophOrganizerOid = organizerOid, ophUnitOid = unitOid)
        val employee = DevEmployee()

        val guardian = DevPerson(ssn = "070644-937X")
        val child = DevPerson(ssn = "030320A904N")

        val placementRange = FiniteDateRange(LocalDate.of(2021, 1, 1), LocalDate.of(2021, 12, 31))
        val applicationDate1 = placementRange.start.minusDays(3)
        val applicationDate2 = placementRange.start.minusDays(2)
        val applicationDate3 = placementRange.start.minusDays(1)
        val today = LocalDate.of(2024, 1, 1)

        db.transaction { tx ->
            tx.insertServiceNeedOption(snDaycareFullDay35)
            tx.insert(area)
            tx.insert(unit1)
            tx.insert(unit2)
            tx.insert(employee)

            tx.insert(guardian, DevPersonType.ADULT)
            tx.insert(child, DevPersonType.CHILD)
            tx.insertGuardian(guardian.id, child.id)

            tx.insert(
                    DevPlacement(
                        childId = child.id,
                        unitId = unit1.id,
                        startDate = placementRange.start,
                        endDate = placementRange.end
                    )
                )
                .also { placementId ->
                    tx.insert(
                        DevServiceNeed(
                            placementId = placementId,
                            startDate = placementRange.start,
                            endDate = placementRange.end,
                            optionId = snDaycareFullDay35.id,
                            confirmedBy = employee.evakaUserId,
                            confirmedAt = HelsinkiDateTime.now()
                        )
                    )
                }

            // This application is picked up!
            tx.insertApplication(
                    guardian = guardian,
                    child = child,
                    status = ApplicationStatus.ACTIVE,
                    sentDate = applicationDate1,
                )
                .let { applicationDetails ->
                    tx.insertTestDecision(
                        TestDecision(
                            applicationId = applicationDetails.id,
                            unitId = unit1.id,
                            startDate = placementRange.start,
                            endDate = placementRange.end,
                            status = DecisionStatus.ACCEPTED,
                            type = DecisionType.DAYCARE,
                            createdBy = employee.evakaUserId,
                        )
                    )
                }

            // This application is not picked up because its sent date is before the previous one:
            // The latest application is always picked up
            tx.insertApplication(
                    guardian = guardian,
                    child = child,
                    status = ApplicationStatus.ACTIVE,
                    sentDate = applicationDate1.minusDays(1),
                )
                .let { applicationDetails ->
                    tx.insertTestDecision(
                        TestDecision(
                            applicationId = applicationDetails.id,
                            unitId = unit1.id,
                            startDate = placementRange.start,
                            endDate = placementRange.end,
                            status = DecisionStatus.ACCEPTED,
                            type = DecisionType.DAYCARE,
                            createdBy = employee.evakaUserId,
                        )
                    )
                }

            // This application is not picked up because the decision's date range doesn't overlap
            // with the placement
            tx.insertApplication(
                    guardian = guardian,
                    child = child,
                    status = ApplicationStatus.ACTIVE,
                    sentDate = applicationDate2,
                )
                .let { applicationDetails ->
                    tx.insertTestDecision(
                        TestDecision(
                            applicationId = applicationDetails.id,
                            unitId = unit1.id,
                            startDate = placementRange.start.minusDays(1),
                            endDate = placementRange.start.minusDays(1),
                            status = DecisionStatus.ACCEPTED,
                            type = DecisionType.DAYCARE,
                            createdBy = employee.evakaUserId,
                        )
                    )
                }

            // This application is not picked up because the decision targets a wrong unit
            tx.insertApplication(
                    guardian = guardian,
                    child = child,
                    status = ApplicationStatus.ACTIVE,
                    sentDate = applicationDate3,
                )
                .let { applicationDetails ->
                    tx.insertTestDecision(
                        TestDecision(
                            applicationId = applicationDetails.id,
                            unitId = unit2.id,
                            startDate = placementRange.start,
                            endDate = placementRange.end,
                            status = DecisionStatus.ACCEPTED,
                            type = DecisionType.DAYCARE,
                            createdBy = employee.evakaUserId,
                        )
                    )
                }
        }

        val updater =
            VardaUpdater(DateRange(LocalDate.of(2019, 1, 1), null), organizerOid, "sourceSystem")

        val evakaState = db.read { updater.getEvakaState(it, today, child.id) }
        assertEquals(
            VardaUpdater.EvakaHenkiloNode(
                henkilo =
                    Henkilo(
                        etunimet = child.firstName,
                        sukunimi = child.lastName,
                        henkilo_oid = null,
                        henkilotunnus = child.ssn,
                    ),
                lapset =
                    listOf(
                        VardaUpdater.EvakaLapsiNode(
                            lapsi =
                                Lapsi(
                                    vakatoimija_oid = organizerOid,
                                    oma_organisaatio_oid = null,
                                    paos_organisaatio_oid = null
                                ),
                            varhaiskasvatuspaatokset =
                                listOf(
                                    varhaiskasvatuspaatos(
                                        unitOid,
                                        placementRange,
                                        hakemus_pvm = applicationDate1
                                    )
                                ),
                            maksutiedot = emptyList()
                        )
                    )
            ),
            evakaState
        )
    }

    @Test
    fun `write to varda - new child`() {
        val sourceSystem = "sourceSystem"
        val organizerOid = "organizerOid"
        val unitOid = "unitOid"
        val placementRange = FiniteDateRange(LocalDate.of(2021, 1, 1), LocalDate.of(2021, 12, 31))
        val applicationDate = placementRange.start.minusMonths(4)

        val updater =
            VardaUpdater(DateRange(LocalDate.of(2019, 1, 1), null), organizerOid, sourceSystem)
        val client = DryRunClient()

        updater.diffAndUpdate(
            client,
            vardaHenkilo =
                VardaUpdater.VardaHenkiloNode(
                    henkilo =
                        VardaReadClient.HenkiloResponse(
                            url = URI.create("henkilo_0"),
                            henkilo_oid = null,
                            lapsi = emptyList()
                        ),
                    lapset = emptyList()
                ),
            evakaHenkilo =
                VardaUpdater.EvakaHenkiloNode(
                    henkilo =
                        Henkilo(
                            etunimet = "Test",
                            sukunimi = "Person",
                            henkilo_oid = null,
                            henkilotunnus = "030320A904N",
                        ),
                    lapset =
                        listOf(
                            VardaUpdater.EvakaLapsiNode(
                                lapsi =
                                    Lapsi(
                                        vakatoimija_oid = organizerOid,
                                        oma_organisaatio_oid = null,
                                        paos_organisaatio_oid = null
                                    ),
                                varhaiskasvatuspaatokset =
                                    listOf(
                                        varhaiskasvatuspaatos(
                                            unitOid,
                                            placementRange,
                                            hakemus_pvm = applicationDate
                                        )
                                    ),
                                maksutiedot = listOf(maksutieto(range = placementRange))
                            )
                        )
                )
        )

        assertEquals(
            listOf(
                "Create" to
                    VardaWriteClient.CreateLapsiRequest(
                        lahdejarjestelma = sourceSystem,
                        henkilo = URI.create("henkilo_0"),
                        vakatoimija_oid = organizerOid,
                        oma_organisaatio_oid = null,
                        paos_organisaatio_oid = null
                    ),
                "Create" to
                    VardaWriteClient.CreateVarhaiskasvatuspaatosRequest(
                        lapsi = URI.create("lapsi_0"),
                        hakemus_pvm = applicationDate,
                        alkamis_pvm = placementRange.start,
                        paattymis_pvm = placementRange.end,
                        tuntimaara_viikossa = 35.0,
                        kokopaivainen_vaka_kytkin = true,
                        tilapainen_vaka_kytkin = false,
                        paivittainen_vaka_kytkin = true,
                        vuorohoito_kytkin = false,
                        jarjestamismuoto_koodi = "jm01",
                        lahdejarjestelma = sourceSystem
                    ),
                "Create" to
                    VardaWriteClient.CreateVarhaiskasvatussuhdeRequest(
                        lahdejarjestelma = sourceSystem,
                        varhaiskasvatuspaatos = URI.create("varhaiskasvatuspaatos_0"),
                        toimipaikka_oid = unitOid,
                        alkamis_pvm = placementRange.start,
                        paattymis_pvm = placementRange.end
                    ),
                "Create" to
                    VardaWriteClient.CreateMaksutietoRequest(
                        lahdejarjestelma = sourceSystem,
                        huoltajat =
                            listOf(
                                Huoltaja(
                                    henkilotunnus = "070644-937X",
                                    henkilo_oid = null,
                                    etunimet = "Test",
                                    sukunimi = "Person"
                                )
                            ),
                        lapsi = URI.create("lapsi_0"),
                        alkamis_pvm = placementRange.start,
                        paattymis_pvm = placementRange.end,
                        maksun_peruste_koodi = "MP03",
                        palveluseteli_arvo = 0.0,
                        asiakasmaksu = 0.0,
                        perheen_koko = 2
                    )
            ),
            client.operations
        )
    }

    @Test
    fun `write to varda - changes to existing child`() {
        val sourceSystem = "sourceSystem"

        val organizerOid = "organizerOid"
        val unitOid = "unitOid"

        val voucherOrganizerOid = "voucherOrganizerOid"
        val voucherUnitOid = "voucherUnitOid"

        val henkiloUrl = URI.create("henkilo")
        val lapsiKeep = URI.create("lapsi_keep")
        val lapsiDelete = URI.create("lapsi_delete")

        val keep =
            object {
                val range = FiniteDateRange(LocalDate.of(2021, 1, 1), LocalDate.of(2021, 12, 31))
                val applicationDate = range.start.minusMonths(4)
                val fee = 0.0

                val varhaiskasvatusPaatosUrl = URI.create("varhaiskasvatuspaatos_keep")
                val varhaiskasvatusSuhdeUrl = URI.create("varhaiskasvatussuhde_keep")
                val maksutietoUrl = URI.create("maksutieto_keep")
            }
        val change =
            object {
                val range = FiniteDateRange(LocalDate.of(2022, 1, 1), LocalDate.of(2022, 12, 31))
                val applicationDateOld = range.start.minusMonths(6)
                val feeOld = 25.0

                val applicationDateNew = range.start.minusMonths(5)
                val feeNew = 62.0

                val varhaiskasvatusPaatosUrl = URI.create("varhaiskasvatuspaatos_change")
                val varhaiskasvatusSuhdeUrl = URI.create("varhaiskasvatussuhde_change")
                val maksutietoUrl = URI.create("maksutieto_change")
            }
        val delete =
            object {
                val range = FiniteDateRange(LocalDate.of(2023, 1, 1), LocalDate.of(2023, 12, 31))
                val applicationDate = range.start.minusMonths(1)

                val varhaiskasvatusPaatosUrl = URI.create("varhaiskasvatuspaatos_delete")
                val varhaiskasvatusSuhdeUrl = URI.create("varhaiskasvatussuhde_delete")
                val maksutietoUrl = URI.create("maksutieto_delete")
            }

        val vardaHenkilo =
            VardaUpdater.VardaHenkiloNode(
                henkilo =
                    VardaReadClient.HenkiloResponse(
                        url = henkiloUrl,
                        henkilo_oid = "henkilo_oid_1",
                        lapsi = listOf(lapsiKeep, lapsiDelete)
                    ),
                lapset =
                    listOf(
                        VardaUpdater.VardaLapsiNode(
                            lapsi =
                                VardaReadClient.LapsiResponse(
                                    url = lapsiKeep,
                                    lahdejarjestelma = sourceSystem,
                                    vakatoimija_oid = organizerOid,
                                    oma_organisaatio_oid = null,
                                    paos_organisaatio_oid = null,
                                    paos_kytkin = false
                                ),
                            varhaiskasvatuspaatokset =
                                listOf(
                                    // This will be kept intact
                                    VardaUpdater.VardaVarhaiskasvatuspaatosNode(
                                        varhaiskasvatuspaatos =
                                            VardaReadClient.VarhaiskasvatuspaatosResponse(
                                                url = keep.varhaiskasvatusPaatosUrl,
                                                lahdejarjestelma = sourceSystem,
                                                alkamis_pvm = keep.range.start,
                                                paattymis_pvm = keep.range.end,
                                                hakemus_pvm = keep.applicationDate,
                                                tuntimaara_viikossa = 35.0,
                                                kokopaivainen_vaka_kytkin = true,
                                                tilapainen_vaka_kytkin = false,
                                                paivittainen_vaka_kytkin = true,
                                                vuorohoito_kytkin = false,
                                                jarjestamismuoto_koodi = "jm01"
                                            ),
                                        varhaiskasvatussuhteet =
                                            listOf(
                                                VardaReadClient.VarhaiskasvatussuhdeResponse(
                                                    url = keep.varhaiskasvatusSuhdeUrl,
                                                    varhaiskasvatuspaatos =
                                                        keep.varhaiskasvatusPaatosUrl,
                                                    lahdejarjestelma = sourceSystem,
                                                    alkamis_pvm = keep.range.start,
                                                    paattymis_pvm = keep.range.end,
                                                    toimipaikka_oid = unitOid
                                                )
                                            )
                                    ),
                                    // This will be replaced
                                    VardaUpdater.VardaVarhaiskasvatuspaatosNode(
                                        varhaiskasvatuspaatos =
                                            VardaReadClient.VarhaiskasvatuspaatosResponse(
                                                url = change.varhaiskasvatusPaatosUrl,
                                                lahdejarjestelma = sourceSystem,
                                                alkamis_pvm = change.range.start,
                                                paattymis_pvm = change.range.end,
                                                hakemus_pvm = change.applicationDateOld,
                                                tuntimaara_viikossa = 35.0,
                                                kokopaivainen_vaka_kytkin = true,
                                                tilapainen_vaka_kytkin = false,
                                                paivittainen_vaka_kytkin = true,
                                                vuorohoito_kytkin = false,
                                                jarjestamismuoto_koodi = "jm01"
                                            ),
                                        varhaiskasvatussuhteet =
                                            listOf(
                                                VardaReadClient.VarhaiskasvatussuhdeResponse(
                                                    url = change.varhaiskasvatusSuhdeUrl,
                                                    varhaiskasvatuspaatos =
                                                        change.varhaiskasvatusSuhdeUrl,
                                                    lahdejarjestelma = sourceSystem,
                                                    alkamis_pvm = change.range.start,
                                                    paattymis_pvm = change.range.end,
                                                    toimipaikka_oid = unitOid
                                                )
                                            )
                                    )
                                ),
                            maksutiedot =
                                listOf(
                                    // This will be kept intact
                                    VardaReadClient.MaksutietoResponse(
                                        url = keep.maksutietoUrl,
                                        lapsi = lapsiKeep,
                                        lahdejarjestelma = sourceSystem,
                                        alkamis_pvm = keep.range.start,
                                        paattymis_pvm = keep.range.end,
                                        perheen_koko = 2,
                                        maksun_peruste_koodi = "MP03",
                                        asiakasmaksu = keep.fee,
                                        palveluseteli_arvo = 0.0,
                                        huoltajat =
                                            listOf(
                                                Huoltaja(
                                                    henkilotunnus = null,
                                                    henkilo_oid = "huoltaja_oid_1",
                                                    etunimet = "Test",
                                                    sukunimi = "Person"
                                                )
                                            )
                                    ),
                                    // This will be replaced
                                    VardaReadClient.MaksutietoResponse(
                                        url = change.maksutietoUrl,
                                        lapsi = lapsiKeep,
                                        lahdejarjestelma = sourceSystem,
                                        alkamis_pvm = change.range.start,
                                        paattymis_pvm = change.range.end,
                                        perheen_koko = 2,
                                        maksun_peruste_koodi = "MP03",
                                        asiakasmaksu = change.feeOld,
                                        palveluseteli_arvo = 0.0,
                                        huoltajat =
                                            listOf(
                                                Huoltaja(
                                                    henkilotunnus = null,
                                                    henkilo_oid = "huoltaja_oid_1",
                                                    etunimet = "Test",
                                                    sukunimi = "Person"
                                                )
                                            )
                                    )
                                )
                        ),

                        // This will be removed altogether
                        VardaUpdater.VardaLapsiNode(
                            lapsi =
                                VardaReadClient.LapsiResponse(
                                    url = lapsiDelete,
                                    lahdejarjestelma = sourceSystem,
                                    vakatoimija_oid = null,
                                    oma_organisaatio_oid = voucherOrganizerOid,
                                    paos_organisaatio_oid = null,
                                    paos_kytkin = true
                                ),
                            varhaiskasvatuspaatokset =
                                listOf(
                                    VardaUpdater.VardaVarhaiskasvatuspaatosNode(
                                        varhaiskasvatuspaatos =
                                            VardaReadClient.VarhaiskasvatuspaatosResponse(
                                                url = delete.varhaiskasvatusPaatosUrl,
                                                lahdejarjestelma = sourceSystem,
                                                alkamis_pvm = delete.range.start,
                                                paattymis_pvm = delete.range.end,
                                                hakemus_pvm = delete.applicationDate,
                                                tuntimaara_viikossa = 35.0,
                                                kokopaivainen_vaka_kytkin = true,
                                                tilapainen_vaka_kytkin = false,
                                                paivittainen_vaka_kytkin = true,
                                                vuorohoito_kytkin = false,
                                                jarjestamismuoto_koodi = "jm01"
                                            ),
                                        varhaiskasvatussuhteet =
                                            listOf(
                                                VardaReadClient.VarhaiskasvatussuhdeResponse(
                                                    url = delete.varhaiskasvatusSuhdeUrl,
                                                    varhaiskasvatuspaatos =
                                                        delete.varhaiskasvatusPaatosUrl,
                                                    lahdejarjestelma = sourceSystem,
                                                    alkamis_pvm = delete.range.start,
                                                    paattymis_pvm = delete.range.end,
                                                    toimipaikka_oid = voucherUnitOid
                                                )
                                            )
                                    )
                                ),
                            maksutiedot =
                                listOf(
                                    VardaReadClient.MaksutietoResponse(
                                        url = delete.maksutietoUrl,
                                        lapsi = lapsiDelete,
                                        lahdejarjestelma = sourceSystem,
                                        alkamis_pvm = delete.range.start,
                                        paattymis_pvm = delete.range.end,
                                        perheen_koko = 2,
                                        maksun_peruste_koodi = "MP03",
                                        asiakasmaksu = 0.0,
                                        palveluseteli_arvo = 0.0,
                                        huoltajat =
                                            listOf(
                                                Huoltaja(
                                                    henkilotunnus = null,
                                                    henkilo_oid = "huoltaja_oid_2",
                                                    etunimet = "Test",
                                                    sukunimi = "Person"
                                                )
                                            )
                                    )
                                )
                        ),
                    )
            )

        val evakaHenkilo =
            VardaUpdater.EvakaHenkiloNode(
                henkilo =
                    Henkilo(
                        etunimet = "Test",
                        sukunimi = "Person",
                        henkilo_oid = null,
                        henkilotunnus = "030320A904N",
                    ),
                lapset =
                    listOf(
                        VardaUpdater.EvakaLapsiNode(
                            lapsi =
                                Lapsi(
                                    vakatoimija_oid = organizerOid,
                                    oma_organisaatio_oid = null,
                                    paos_organisaatio_oid = null
                                ),
                            varhaiskasvatuspaatokset =
                                listOf(
                                    varhaiskasvatuspaatos(
                                        unitOid,
                                        keep.range,
                                        hakemus_pvm = keep.applicationDate
                                    ),
                                    varhaiskasvatuspaatos(
                                        unitOid,
                                        change.range,
                                        hakemus_pvm = change.applicationDateNew
                                    )
                                ),
                            maksutiedot =
                                listOf(
                                    maksutieto(range = keep.range, asiakasmaksu = keep.fee),
                                    maksutieto(range = change.range, asiakasmaksu = change.feeNew)
                                )
                        )
                    )
            )

        val client = DryRunClient()
        val updater =
            VardaUpdater(DateRange(LocalDate.of(2019, 1, 1), null), organizerOid, sourceSystem)

        updater.diffAndUpdate(client, vardaHenkilo = vardaHenkilo, evakaHenkilo = evakaHenkilo)

        assertEquals(
            listOf(
                "Delete" to delete.maksutietoUrl,
                "Delete" to delete.varhaiskasvatusSuhdeUrl,
                "Delete" to delete.varhaiskasvatusPaatosUrl,
                "Delete" to lapsiDelete,
                "Delete" to change.maksutietoUrl,
                "Delete" to change.varhaiskasvatusSuhdeUrl,
                "Delete" to change.varhaiskasvatusPaatosUrl,
                "Create" to
                    VardaWriteClient.CreateVarhaiskasvatuspaatosRequest(
                        lapsi = lapsiKeep,
                        hakemus_pvm = change.applicationDateNew,
                        alkamis_pvm = change.range.start,
                        paattymis_pvm = change.range.end,
                        tuntimaara_viikossa = 35.0,
                        kokopaivainen_vaka_kytkin = true,
                        tilapainen_vaka_kytkin = false,
                        paivittainen_vaka_kytkin = true,
                        vuorohoito_kytkin = false,
                        jarjestamismuoto_koodi = "jm01",
                        lahdejarjestelma = sourceSystem
                    ),
                "Create" to
                    VardaWriteClient.CreateVarhaiskasvatussuhdeRequest(
                        lahdejarjestelma = sourceSystem,
                        varhaiskasvatuspaatos = URI.create("varhaiskasvatuspaatos_0"),
                        toimipaikka_oid = unitOid,
                        alkamis_pvm = change.range.start,
                        paattymis_pvm = change.range.end
                    ),
                "Create" to
                    VardaWriteClient.CreateMaksutietoRequest(
                        lahdejarjestelma = sourceSystem,
                        huoltajat =
                            listOf(
                                Huoltaja(
                                    henkilotunnus = "070644-937X",
                                    henkilo_oid = null,
                                    etunimet = "Test",
                                    sukunimi = "Person"
                                )
                            ),
                        lapsi = lapsiKeep,
                        alkamis_pvm = change.range.start,
                        paattymis_pvm = change.range.end,
                        maksun_peruste_koodi = "MP03",
                        palveluseteli_arvo = 0.0,
                        asiakasmaksu = change.feeNew,
                        perheen_koko = 2
                    )
            ),
            client.operations
        )
    }

    @Test
    fun `write to varda - data outside the varda enabled range is not deleted`() {
        val sourceSystem = "sourceSystem"

        val organizerOid = "organizerOid"
        val unitOid = "unitOid"

        val henkiloUrl = URI.create("henkilo")
        val lapsiUrl = URI.create("lapsi")

        val vardaStart = LocalDate.of(2021, 1, 1)

        val keep =
            object {
                // Starts before vardaStart
                val range = FiniteDateRange(LocalDate.of(2020, 1, 1), LocalDate.of(2021, 12, 31))
                val applicationDate = range.start.minusMonths(4)

                val varhaiskasvatusPaatosUrl = URI.create("varhaiskasvatuspaatos_keep")
                val varhaiskasvatusSuhdeUrl = URI.create("varhaiskasvatussuhde_keep")
                val maksutietoUrl = URI.create("maksutieto_keep")
            }

        val vardaHenkilo =
            VardaUpdater.VardaHenkiloNode(
                henkilo =
                    VardaReadClient.HenkiloResponse(
                        url = henkiloUrl,
                        henkilo_oid = "henkilo_oid_1",
                        lapsi = listOf(lapsiUrl)
                    ),
                lapset =
                    listOf(
                        VardaUpdater.VardaLapsiNode(
                            lapsi =
                                VardaReadClient.LapsiResponse(
                                    url = lapsiUrl,
                                    lahdejarjestelma = sourceSystem,
                                    vakatoimija_oid = organizerOid,
                                    oma_organisaatio_oid = null,
                                    paos_organisaatio_oid = null,
                                    paos_kytkin = false
                                ),
                            varhaiskasvatuspaatokset =
                                listOf(
                                    VardaUpdater.VardaVarhaiskasvatuspaatosNode(
                                        varhaiskasvatuspaatos =
                                            VardaReadClient.VarhaiskasvatuspaatosResponse(
                                                url = keep.varhaiskasvatusPaatosUrl,
                                                lahdejarjestelma = sourceSystem,
                                                alkamis_pvm = keep.range.start,
                                                paattymis_pvm = keep.range.end,
                                                hakemus_pvm = keep.applicationDate,
                                                tuntimaara_viikossa = 35.0,
                                                kokopaivainen_vaka_kytkin = true,
                                                tilapainen_vaka_kytkin = false,
                                                paivittainen_vaka_kytkin = true,
                                                vuorohoito_kytkin = false,
                                                jarjestamismuoto_koodi = "jm01"
                                            ),
                                        varhaiskasvatussuhteet =
                                            listOf(
                                                VardaReadClient.VarhaiskasvatussuhdeResponse(
                                                    url = keep.varhaiskasvatusSuhdeUrl,
                                                    varhaiskasvatuspaatos =
                                                        keep.varhaiskasvatusPaatosUrl,
                                                    lahdejarjestelma = sourceSystem,
                                                    alkamis_pvm = keep.range.start,
                                                    paattymis_pvm = keep.range.end,
                                                    toimipaikka_oid = unitOid
                                                )
                                            )
                                    )
                                ),
                            maksutiedot =
                                listOf(
                                    // This will be kept intact
                                    VardaReadClient.MaksutietoResponse(
                                        url = keep.maksutietoUrl,
                                        lapsi = lapsiUrl,
                                        lahdejarjestelma = sourceSystem,
                                        alkamis_pvm = keep.range.start,
                                        paattymis_pvm = keep.range.end,
                                        perheen_koko = 2,
                                        maksun_peruste_koodi = "MP03",
                                        asiakasmaksu = 10.0,
                                        palveluseteli_arvo = 0.0,
                                        huoltajat =
                                            listOf(
                                                Huoltaja(
                                                    henkilotunnus = null,
                                                    henkilo_oid = "huoltaja_oid_1",
                                                    etunimet = "Test",
                                                    sukunimi = "Person"
                                                )
                                            )
                                    )
                                )
                        )
                    )
            )

        val evakaHenkilo =
            VardaUpdater.EvakaHenkiloNode(
                henkilo =
                    Henkilo(
                        etunimet = "Test",
                        sukunimi = "Person",
                        henkilo_oid = null,
                        henkilotunnus = "030320A904N",
                    ),
                // No data in eVaka
                lapset = listOf()
            )

        val client = DryRunClient()
        val updater = VardaUpdater(DateRange(vardaStart, null), organizerOid, sourceSystem)

        updater.diffAndUpdate(client, vardaHenkilo = vardaHenkilo, evakaHenkilo = evakaHenkilo)

        // Nothing is deleted because data in Varda is outside Varda's enabled range
        assertEquals(emptyList(), client.operations)
    }

    @Test
    fun `write to varda - data from other source system is set to end the day before first evaka data`() {
        val henkiloUrl = URI.create("henkilo")
        val lapsiUrl = URI.create("lapsi")

        val old =
            object {
                val sourceSystem = "oldSourceSystem"
                val organizerOid = "oldOrganizerOid"
                val unitOid = "oldUnitOid"

                val beforeEvakaRange =
                    FiniteDateRange(LocalDate.of(2020, 1, 1), LocalDate.of(2021, 12, 31))
                val applicationDate = beforeEvakaRange.start.minusMonths(4)

                val varhaiskasvatusPaatosToEndUrl = URI.create("varhaiskasvatuspaatos_to_end")
                val varhaiskasvatusSuhdeToEndUrl = URI.create("varhaiskasvatussuhde_to_end")
                val maksutietoToEndUrl = URI.create("maksutieto_to_end")

                val afterEvakaRange =
                    FiniteDateRange(LocalDate.of(2021, 7, 1), LocalDate.of(2021, 12, 31))
                val varhaiskasvatusPaatosToDeleteUrl = URI.create("varhaiskasvatuspaatos_to_delete")
                val varhaiskasvatusSuhdeToDeleteUrl = URI.create("varhaiskasvatussuhde_to_delete")
            }
        val new =
            object {
                val sourceSystem = "sourceSystem"
                val organizerOid = "organizerOid"
                val unitOid = "unitOid"

                val range = FiniteDateRange(LocalDate.of(2021, 6, 1), LocalDate.of(2022, 12, 31))
                val applicationDate = range.start.minusDays(22)
            }

        val vardaHenkilo =
            VardaUpdater.VardaHenkiloNode(
                henkilo =
                    VardaReadClient.HenkiloResponse(
                        url = henkiloUrl,
                        henkilo_oid = "henkilo_oid_1",
                        lapsi = listOf(lapsiUrl)
                    ),
                lapset =
                    listOf(
                        VardaUpdater.VardaLapsiNode(
                            lapsi =
                                VardaReadClient.LapsiResponse(
                                    url = lapsiUrl,
                                    lahdejarjestelma = old.sourceSystem,
                                    vakatoimija_oid = old.organizerOid,
                                    oma_organisaatio_oid = null,
                                    paos_organisaatio_oid = null,
                                    paos_kytkin = false
                                ),
                            varhaiskasvatuspaatokset =
                                listOf(
                                    VardaUpdater.VardaVarhaiskasvatuspaatosNode(
                                        varhaiskasvatuspaatos =
                                            VardaReadClient.VarhaiskasvatuspaatosResponse(
                                                url = old.varhaiskasvatusPaatosToEndUrl,
                                                lahdejarjestelma = old.sourceSystem,
                                                alkamis_pvm = old.beforeEvakaRange.start,
                                                paattymis_pvm = old.beforeEvakaRange.end,
                                                hakemus_pvm = old.applicationDate,
                                                tuntimaara_viikossa = 35.0,
                                                kokopaivainen_vaka_kytkin = true,
                                                tilapainen_vaka_kytkin = false,
                                                paivittainen_vaka_kytkin = true,
                                                vuorohoito_kytkin = false,
                                                jarjestamismuoto_koodi = "jm01"
                                            ),
                                        varhaiskasvatussuhteet =
                                            listOf(
                                                VardaReadClient.VarhaiskasvatussuhdeResponse(
                                                    url = old.varhaiskasvatusSuhdeToEndUrl,
                                                    varhaiskasvatuspaatos =
                                                        old.varhaiskasvatusPaatosToEndUrl,
                                                    lahdejarjestelma = old.sourceSystem,
                                                    alkamis_pvm = old.beforeEvakaRange.start,
                                                    paattymis_pvm = old.beforeEvakaRange.end,
                                                    toimipaikka_oid = old.unitOid
                                                )
                                            )
                                    ),
                                    VardaUpdater.VardaVarhaiskasvatuspaatosNode(
                                        varhaiskasvatuspaatos =
                                            VardaReadClient.VarhaiskasvatuspaatosResponse(
                                                url = old.varhaiskasvatusPaatosToDeleteUrl,
                                                lahdejarjestelma = old.sourceSystem,
                                                alkamis_pvm = old.afterEvakaRange.start,
                                                paattymis_pvm = old.afterEvakaRange.end,
                                                hakemus_pvm = old.applicationDate,
                                                tuntimaara_viikossa = 35.0,
                                                kokopaivainen_vaka_kytkin = true,
                                                tilapainen_vaka_kytkin = false,
                                                paivittainen_vaka_kytkin = true,
                                                vuorohoito_kytkin = false,
                                                jarjestamismuoto_koodi = "jm01"
                                            ),
                                        varhaiskasvatussuhteet =
                                            listOf(
                                                VardaReadClient.VarhaiskasvatussuhdeResponse(
                                                    url = old.varhaiskasvatusSuhdeToDeleteUrl,
                                                    varhaiskasvatuspaatos =
                                                        old.varhaiskasvatusPaatosToDeleteUrl,
                                                    lahdejarjestelma = old.sourceSystem,
                                                    alkamis_pvm = old.afterEvakaRange.start,
                                                    paattymis_pvm = old.afterEvakaRange.end,
                                                    toimipaikka_oid = old.unitOid
                                                )
                                            )
                                    )
                                ),
                            maksutiedot =
                                listOf(
                                    VardaReadClient.MaksutietoResponse(
                                        url = old.maksutietoToEndUrl,
                                        lapsi = lapsiUrl,
                                        lahdejarjestelma = old.sourceSystem,
                                        alkamis_pvm = old.beforeEvakaRange.start,
                                        paattymis_pvm = old.beforeEvakaRange.end,
                                        perheen_koko = 2,
                                        maksun_peruste_koodi = "MP03",
                                        asiakasmaksu = 10.0,
                                        palveluseteli_arvo = 0.0,
                                        huoltajat =
                                            listOf(
                                                Huoltaja(
                                                    henkilotunnus = null,
                                                    henkilo_oid = "huoltaja_oid_1",
                                                    etunimet = "Test",
                                                    sukunimi = "Person"
                                                )
                                            )
                                    )
                                )
                        )
                    )
            )

        val evakaHenkilo =
            VardaUpdater.EvakaHenkiloNode(
                henkilo =
                    Henkilo(
                        etunimet = "Test",
                        sukunimi = "Person",
                        henkilo_oid = null,
                        henkilotunnus = "030320A904N",
                    ),
                lapset =
                    listOf(
                        VardaUpdater.EvakaLapsiNode(
                            lapsi =
                                Lapsi(
                                    vakatoimija_oid = new.organizerOid,
                                    oma_organisaatio_oid = null,
                                    paos_organisaatio_oid = null
                                ),
                            varhaiskasvatuspaatokset =
                                listOf(
                                    varhaiskasvatuspaatos(
                                        new.unitOid,
                                        new.range,
                                        hakemus_pvm = new.applicationDate
                                    )
                                ),
                            maksutiedot = emptyList()
                        )
                    )
            )

        val client = DryRunClient()
        val updater =
            VardaUpdater(
                DateRange(LocalDate.of(2021, 1, 1), null),
                new.organizerOid,
                new.sourceSystem
            )

        updater.diffAndUpdate(client, vardaHenkilo, evakaHenkilo)

        val dayBeforeEvaka = new.range.start.minusDays(1)
        assertEquals(
            listOf(
                "SetPaattymisPvm" to
                    (old.maksutietoToEndUrl to
                        VardaWriteClient.SetPaattymisPvmRequest(dayBeforeEvaka)),
                "SetPaattymisPvm" to
                    (old.varhaiskasvatusSuhdeToEndUrl to
                        VardaWriteClient.SetPaattymisPvmRequest(dayBeforeEvaka)),
                "SetPaattymisPvm" to
                    (old.varhaiskasvatusPaatosToEndUrl to
                        VardaWriteClient.SetPaattymisPvmRequest(dayBeforeEvaka)),
                "Delete" to old.varhaiskasvatusSuhdeToDeleteUrl,
                "Delete" to old.varhaiskasvatusPaatosToDeleteUrl,
                "Create" to
                    VardaWriteClient.CreateLapsiRequest(
                        lahdejarjestelma = new.sourceSystem,
                        henkilo = URI.create("henkilo"),
                        vakatoimija_oid = new.organizerOid,
                        oma_organisaatio_oid = null,
                        paos_organisaatio_oid = null
                    ),
                "Create" to
                    VardaWriteClient.CreateVarhaiskasvatuspaatosRequest(
                        lapsi = URI.create("lapsi_0"),
                        hakemus_pvm = new.applicationDate,
                        alkamis_pvm = new.range.start,
                        paattymis_pvm = new.range.end,
                        tuntimaara_viikossa = 35.0,
                        kokopaivainen_vaka_kytkin = true,
                        tilapainen_vaka_kytkin = false,
                        paivittainen_vaka_kytkin = true,
                        vuorohoito_kytkin = false,
                        jarjestamismuoto_koodi = "jm01",
                        lahdejarjestelma = new.sourceSystem
                    ),
                "Create" to
                    VardaWriteClient.CreateVarhaiskasvatussuhdeRequest(
                        lahdejarjestelma = new.sourceSystem,
                        varhaiskasvatuspaatos = URI.create("varhaiskasvatuspaatos_0"),
                        toimipaikka_oid = new.unitOid,
                        alkamis_pvm = new.range.start,
                        paattymis_pvm = new.range.end
                    )
            ),
            client.operations
        )
    }

    @Test
    fun `write to varda - data created with different source system is not deleted`() {
        val otherSourceSystem = "otherSourceSystem"
        val evakaSourceSystem = "sourceSystem"

        val organizerOid = "organizerOid"
        val unitOid = "unitOid"

        val henkiloUrl = URI.create("henkilo")
        val lapsiUrl = URI.create("lapsi")

        val keep =
            object {
                val range = FiniteDateRange(LocalDate.of(2020, 1, 1), LocalDate.of(2021, 12, 31))
                val applicationDate = range.start.minusMonths(4)

                val varhaiskasvatusPaatosUrl = URI.create("varhaiskasvatuspaatos_keep")
                val varhaiskasvatusSuhdeUrl = URI.create("varhaiskasvatussuhde_keep")
                val maksutietoUrl = URI.create("maksutieto_keep")
            }

        val vardaHenkilo =
            VardaUpdater.VardaHenkiloNode(
                henkilo =
                    VardaReadClient.HenkiloResponse(
                        url = henkiloUrl,
                        henkilo_oid = "henkilo_oid_1",
                        lapsi = listOf(lapsiUrl)
                    ),
                lapset =
                    listOf(
                        VardaUpdater.VardaLapsiNode(
                            lapsi =
                                VardaReadClient.LapsiResponse(
                                    url = lapsiUrl,
                                    lahdejarjestelma = otherSourceSystem,
                                    vakatoimija_oid = organizerOid,
                                    oma_organisaatio_oid = null,
                                    paos_organisaatio_oid = null,
                                    paos_kytkin = false
                                ),
                            varhaiskasvatuspaatokset =
                                listOf(
                                    VardaUpdater.VardaVarhaiskasvatuspaatosNode(
                                        varhaiskasvatuspaatos =
                                            VardaReadClient.VarhaiskasvatuspaatosResponse(
                                                url = keep.varhaiskasvatusPaatosUrl,
                                                lahdejarjestelma = otherSourceSystem,
                                                alkamis_pvm = keep.range.start,
                                                paattymis_pvm = keep.range.end,
                                                hakemus_pvm = keep.applicationDate,
                                                tuntimaara_viikossa = 35.0,
                                                kokopaivainen_vaka_kytkin = true,
                                                tilapainen_vaka_kytkin = false,
                                                paivittainen_vaka_kytkin = true,
                                                vuorohoito_kytkin = false,
                                                jarjestamismuoto_koodi = "jm01"
                                            ),
                                        varhaiskasvatussuhteet =
                                            listOf(
                                                VardaReadClient.VarhaiskasvatussuhdeResponse(
                                                    url = keep.varhaiskasvatusSuhdeUrl,
                                                    varhaiskasvatuspaatos =
                                                        keep.varhaiskasvatusPaatosUrl,
                                                    lahdejarjestelma = otherSourceSystem,
                                                    alkamis_pvm = keep.range.start,
                                                    paattymis_pvm = keep.range.end,
                                                    toimipaikka_oid = unitOid
                                                )
                                            )
                                    )
                                ),
                            maksutiedot =
                                listOf(
                                    // This will be kept intact
                                    VardaReadClient.MaksutietoResponse(
                                        url = keep.maksutietoUrl,
                                        lapsi = lapsiUrl,
                                        lahdejarjestelma = otherSourceSystem,
                                        alkamis_pvm = keep.range.start,
                                        paattymis_pvm = keep.range.end,
                                        perheen_koko = 2,
                                        maksun_peruste_koodi = "MP03",
                                        asiakasmaksu = 10.0,
                                        palveluseteli_arvo = 0.0,
                                        huoltajat =
                                            listOf(
                                                Huoltaja(
                                                    henkilotunnus = null,
                                                    henkilo_oid = "huoltaja_oid_1",
                                                    etunimet = "Test",
                                                    sukunimi = "Person"
                                                )
                                            )
                                    )
                                )
                        )
                    )
            )

        val evakaHenkilo =
            VardaUpdater.EvakaHenkiloNode(
                henkilo =
                    Henkilo(
                        etunimet = "Test",
                        sukunimi = "Person",
                        henkilo_oid = null,
                        henkilotunnus = "030320A904N",
                    ),
                // No data in eVaka
                lapset = listOf()
            )

        val client = DryRunClient()
        val updater =
            VardaUpdater(DateRange(LocalDate.of(2019, 1, 1), null), organizerOid, evakaSourceSystem)

        updater.diffAndUpdate(client, vardaHenkilo = vardaHenkilo, evakaHenkilo = evakaHenkilo)

        // Nothing is deleted because all data was created by another source system
        assertEquals(emptyList(), client.operations)
    }

    @Test
    fun `evaka state - child without ssn or oph id is skipped`() {
        val area = DevCareArea()
        val unit = DevDaycare(areaId = area.id)
        val employee = DevEmployee()

        val child1 = DevPerson(ssn = null, ophPersonOid = null)
        val child2 = DevPerson(ssn = null, ophPersonOid = "")
        val placementRange = FiniteDateRange(LocalDate.of(2021, 1, 1), LocalDate.of(2021, 6, 30))
        val now = HelsinkiDateTime.of(LocalDate.of(2024, 1, 1), LocalTime.of(12, 0))

        db.transaction { tx ->
            tx.insertServiceNeedOption(snDaycareFullDay35)
            tx.insert(area)
            tx.insert(unit)
            tx.insert(employee)

            tx.insert(child1, DevPersonType.CHILD)
            tx.insert(
                    DevPlacement(
                        childId = child1.id,
                        unitId = unit.id,
                        startDate = placementRange.start,
                        endDate = placementRange.end
                    )
                )
                .also { placementId ->
                    tx.insert(
                        DevServiceNeed(
                            placementId = placementId,
                            startDate = placementRange.start,
                            endDate = placementRange.end,
                            optionId = snDaycareFullDay35.id,
                            confirmedBy = employee.evakaUserId,
                            confirmedAt = HelsinkiDateTime.now()
                        )
                    )
                }

            tx.insert(child2, DevPersonType.CHILD)
            tx.insert(
                    DevPlacement(
                        childId = child2.id,
                        unitId = unit.id,
                        startDate = placementRange.start,
                        endDate = placementRange.end
                    )
                )
                .also { placementId ->
                    tx.insert(
                        DevServiceNeed(
                            placementId = placementId,
                            startDate = placementRange.start,
                            endDate = placementRange.end,
                            optionId = snDaycareFullDay35.id,
                            confirmedBy = employee.evakaUserId,
                            confirmedAt = HelsinkiDateTime.now()
                        )
                    )
                }
        }

        val updater =
            VardaUpdater(
                DateRange(LocalDate.of(2019, 1, 1), null),
                "municipalOrganizerOid",
                "sourceSystem"
            )

        val readClient = FailEveryOperation()
        val writeClient = DryRunClient()
        updater.updateChild(db, readClient, writeClient, now, child1.id, true)
        updater.updateChild(db, readClient, writeClient, now, child2.id, true)

        assertEquals(emptyList(), writeClient.operations)
    }

    @Test
    fun `haeHenkilo is used for children without ssn`() {
        val child = DevPerson(ssn = null, ophPersonOid = "ophPersonOid")

        db.transaction { tx ->
            tx.insert(child, DevPersonType.CHILD)
            tx.execute {
                sql("INSERT INTO varda_state (child_id, state) VALUES (${bind(child.id)}, NULL)")
            }
        }

        val updater =
            VardaUpdater(DateRange(LocalDate.of(2019, 1, 1), null), "organizerOid", "sourceSystem")

        class TestReadClient : FailEveryOperation() {
            override fun haeHenkilo(
                body: VardaReadClient.HaeHenkiloRequest
            ): VardaReadClient.HenkiloResponse =
                VardaReadClient.HenkiloResponse(
                    url = URI.create("henkilo"),
                    henkilo_oid = "henkilo_oid",
                    lapsi = emptyList()
                )
        }

        updater.updateChild(
            dbc = db,
            readClient = TestReadClient(),
            writeClient = DryRunClient(),
            now = HelsinkiDateTime.of(LocalDate.of(2021, 1, 1), LocalTime.of(12, 0)),
            childId = child.id,
            saveState = true
        )

        val state =
            db.read { it.getVardaUpdateState<VardaUpdater.EvakaHenkiloNode>(listOf(child.id)) }
                .values
                .first()

        assertEquals(
            VardaUpdater.EvakaHenkiloNode(
                henkilo =
                    Henkilo(
                        etunimet = child.firstName,
                        sukunimi = child.lastName,
                        henkilo_oid = "ophPersonOid",
                        henkilotunnus = null,
                    ),
                lapset = emptyList()
            ),
            state
        )
    }

    @Test
    fun `state is saved after update`() {
        val child = DevPerson(ssn = "030320A904N")

        db.transaction { tx ->
            tx.insert(child, DevPersonType.CHILD)
            tx.execute {
                sql("INSERT INTO varda_state (child_id, state) VALUES (${bind(child.id)}, NULL)")
            }
        }

        val updater =
            VardaUpdater(DateRange(LocalDate.of(2019, 1, 1), null), "organizerOid", "sourceSystem")

        class TestReadClient : FailEveryOperation() {
            override fun getOrCreateHenkilo(body: VardaReadClient.GetOrCreateHenkiloRequest) =
                VardaReadClient.HenkiloResponse(
                    url = URI.create("henkilo"),
                    henkilo_oid = "henkilo_oid",
                    lapsi = emptyList()
                )
        }

        updater.updateChild(
            dbc = db,
            readClient = TestReadClient(),
            writeClient = DryRunClient(),
            now = HelsinkiDateTime.of(LocalDate.of(2021, 1, 1), LocalTime.of(12, 0)),
            childId = child.id,
            saveState = true
        )

        val state =
            db.read { it.getVardaUpdateState<VardaUpdater.EvakaHenkiloNode>(listOf(child.id)) }
                .values
                .first()

        assertEquals(
            VardaUpdater.EvakaHenkiloNode(
                henkilo =
                    Henkilo(
                        etunimet = child.firstName,
                        sukunimi = child.lastName,
                        henkilo_oid = null,
                        henkilotunnus = child.ssn,
                    ),
                lapset = emptyList()
            ),
            state
        )
    }

    @Test
    fun `oph oid is saved after update`() {
        val child = DevPerson(ssn = "030320A904N", ophPersonOid = null)

        db.transaction { tx ->
            tx.insert(child, DevPersonType.CHILD)
            tx.execute {
                sql("INSERT INTO varda_state (child_id, state) VALUES (${bind(child.id)}, null)")
            }
        }

        val updater =
            VardaUpdater(DateRange(LocalDate.of(2019, 1, 1), null), "organizerOid", "sourceSystem")

        class TestReadClient : FailEveryOperation() {
            override fun getOrCreateHenkilo(
                body: VardaReadClient.GetOrCreateHenkiloRequest
            ): VardaReadClient.HenkiloResponse =
                VardaReadClient.HenkiloResponse(
                    url = URI.create("henkilo"),
                    henkilo_oid = "henkilo_oid",
                    lapsi = emptyList()
                )
        }

        updater.updateChild(
            dbc = db,
            readClient = TestReadClient(),
            writeClient = DryRunClient(),
            now = HelsinkiDateTime.of(LocalDate.of(2021, 1, 1), LocalTime.of(12, 0)),
            childId = child.id,
            saveState = true
        )

        val updatedChild = db.read { it.getPersonById(child.id)!! }
        assertEquals("henkilo_oid", updatedChild.ophPersonOid)
    }

    @Test
    fun `error is saved`() {
        val child = DevPerson(ssn = "030320A904N", ophPersonOid = null)
        val now = HelsinkiDateTime.of(LocalDate.of(2021, 1, 1), LocalTime.of(12, 0))

        db.transaction { tx ->
            tx.insert(child, DevPersonType.CHILD)
            tx.execute {
                sql("INSERT INTO varda_state (child_id, state) VALUES (${bind(child.id)}, null)")
            }
        }

        val updater =
            VardaUpdater(DateRange(LocalDate.of(2019, 1, 1), null), "organizerOid", "sourceSystem")

        class TestReadClient : FailEveryOperation() {
            override fun getOrCreateHenkilo(
                body: VardaReadClient.GetOrCreateHenkiloRequest
            ): VardaReadClient.HenkiloResponse = error("this is an error message")
        }

        updater.updateChild(
            dbc = db,
            readClient = TestReadClient(),
            writeClient = DryRunClient(),
            now = now,
            childId = child.id,
            saveState = true
        )

        val (lastSuccessAt, erroredAt, error) =
            db.read { tx ->
                tx.createQuery { sql("SELECT last_success_at, errored_at, error FROM varda_state") }
                    .map {
                        Triple<HelsinkiDateTime?, HelsinkiDateTime?, String?>(
                            column("last_success_at"),
                            column("errored_at"),
                            column("error")
                        )
                    }
                    .exactlyOne()
            }

        assertEquals(null, lastSuccessAt)
        assertEquals(now, erroredAt)
        assertEquals("this is an error message", error)
    }

    @Test
    fun `non-compatible state decodes as null`() {
        val child = DevPerson(ssn = "030320A904N")
        db.transaction { tx ->
            tx.insert(child, DevPersonType.CHILD)
            tx.execute {
                sql(
                    "INSERT INTO varda_state (child_id, state) VALUES (${bind(child.id)}, '{\"foo\": \"bar\"}'::jsonb)"
                )
            }
        }

        val states =
            db.read { tx ->
                tx.getVardaUpdateState<VardaUpdater.EvakaHenkiloNode>(listOf(child.id))
            }
        assertEquals(mapOf(child.id to null), states)
    }

    private fun varhaiskasvatuspaatos(
        toimipaikka_oid: String,
        range: FiniteDateRange,
        hakemus_pvm: LocalDate = range.start.minusDays(15),
        tuntimaara_viikossa: Double = 35.0,
        kokopaivainen_vaka_kytkin: Boolean = true,
        tilapainen_vaka_kytkin: Boolean = false,
        paivittainen_vaka_kytkin: Boolean = true,
        vuorohoito_kytkin: Boolean = false,
        jarjestamismuoto_koodi: String = "jm01"
    ) =
        VardaUpdater.EvakaVarhaiskasvatuspaatosNode(
            varhaiskasvatuspaatos =
                Varhaiskasvatuspaatos(
                    hakemus_pvm = hakemus_pvm,
                    alkamis_pvm = range.start,
                    paattymis_pvm = range.end,
                    tuntimaara_viikossa = tuntimaara_viikossa,
                    kokopaivainen_vaka_kytkin = kokopaivainen_vaka_kytkin,
                    tilapainen_vaka_kytkin = tilapainen_vaka_kytkin,
                    paivittainen_vaka_kytkin = paivittainen_vaka_kytkin,
                    vuorohoito_kytkin = vuorohoito_kytkin,
                    jarjestamismuoto_koodi = jarjestamismuoto_koodi,
                ),
            varhaiskasvatussuhteet =
                listOf(Varhaiskasvatussuhde(toimipaikka_oid, range.start, range.end))
        )

    private fun maksutieto(
        range: FiniteDateRange,
        perheen_koko: Int = 2,
        maksun_peruste_koodi: String = "MP03",
        asiakasmaksu: Double = 0.0,
        palveluseteli_arvo: Double = 0.0,
        huoltajat: List<Huoltaja> =
            listOf(
                Huoltaja(
                    henkilotunnus = "070644-937X",
                    henkilo_oid = null,
                    etunimet = "Test",
                    sukunimi = "Person"
                )
            )
    ) =
        Maksutieto(
            alkamis_pvm = range.start,
            paattymis_pvm = range.end,
            perheen_koko = perheen_koko,
            maksun_peruste_koodi = maksun_peruste_koodi,
            asiakasmaksu = asiakasmaksu,
            palveluseteli_arvo = palveluseteli_arvo,
            huoltajat = huoltajat
        )
}

open class FailEveryOperation : VardaReadClient {
    override fun haeHenkilo(
        body: VardaReadClient.HaeHenkiloRequest
    ): VardaReadClient.HenkiloResponse {
        throw NotImplementedError()
    }

    override fun getOrCreateHenkilo(
        body: VardaReadClient.GetOrCreateHenkiloRequest
    ): VardaReadClient.HenkiloResponse {
        throw NotImplementedError()
    }

    override fun getLapsi(url: URI): VardaReadClient.LapsiResponse {
        throw NotImplementedError()
    }

    override fun getMaksutiedotByLapsi(lapsiUrl: URI): List<VardaReadClient.MaksutietoResponse> {
        throw NotImplementedError()
    }

    override fun getVarhaiskasvatuspaatoksetByLapsi(
        lapsiUrl: URI
    ): List<VardaReadClient.VarhaiskasvatuspaatosResponse> {
        throw NotImplementedError()
    }

    override fun getVarhaiskasvatussuhteetByLapsi(
        lapsiUrl: URI
    ): List<VardaReadClient.VarhaiskasvatussuhdeResponse> {
        throw NotImplementedError()
    }
}
