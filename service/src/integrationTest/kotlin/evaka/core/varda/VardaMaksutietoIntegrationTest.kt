// SPDX-FileCopyrightText: 2017-2026 City of Espoo
//
// SPDX-License-Identifier: LGPL-2.1-or-later

package evaka.core.varda

import evaka.core.PureJdbiTest
import evaka.core.daycare.domain.ProviderType
import evaka.core.invoicing.domain.FeeDecisionStatus
import evaka.core.invoicing.domain.VoucherValueDecisionStatus
import evaka.core.pis.service.insertGuardian
import evaka.core.placement.PlacementType
import evaka.core.serviceneed.ServiceNeedOption
import evaka.core.shared.ServiceNeedOptionId
import evaka.core.shared.dev.DevCareArea
import evaka.core.shared.dev.DevDaycare
import evaka.core.shared.dev.DevEmployee
import evaka.core.shared.dev.DevFeeDecision
import evaka.core.shared.dev.DevFeeDecisionChild
import evaka.core.shared.dev.DevParentship
import evaka.core.shared.dev.DevPerson
import evaka.core.shared.dev.DevPersonType
import evaka.core.shared.dev.DevPlacement
import evaka.core.shared.dev.DevServiceNeed
import evaka.core.shared.dev.DevVoucherValueDecision
import evaka.core.shared.dev.insert
import evaka.core.shared.dev.insertServiceNeedOption
import evaka.core.shared.domain.DateRange
import evaka.core.shared.domain.FiniteDateRange
import evaka.core.shared.domain.HelsinkiDateTime
import evaka.core.snDaycareFullDay35
import evaka.core.snDefaultFiveYearOldsPartDayDaycare
import java.math.BigDecimal
import java.time.LocalDate
import java.util.UUID
import kotlin.test.assertEquals
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test

class VardaMaksutietoIntegrationTest : PureJdbiTest(resetDbBeforeEach = true) {
    private val organizerOid = "organizerOid"
    private val area = DevCareArea()
    private val municipalUnit =
        DevDaycare(areaId = area.id, ophOrganizerOid = organizerOid, ophUnitOid = "unitOid")
    private val voucherUnit =
        DevDaycare(
            areaId = area.id,
            providerType = ProviderType.PRIVATE_SERVICE_VOUCHER,
            ophOrganizerOid = "voucherOrganizerOid",
            ophUnitOid = "voucherUnitOid",
        )
    private val employee = DevEmployee()
    private val guardian = DevPerson(ssn = "070644-937X")
    private val child = DevPerson(dateOfBirth = LocalDate.of(2020, 3, 3), ssn = "030320A904N")
    private val range = FiniteDateRange(LocalDate.of(2021, 1, 1), LocalDate.of(2021, 6, 30))
    private val today = LocalDate.of(2024, 1, 1)
    private val snFreeFiveYearOldsPartDay =
        snDefaultFiveYearOldsPartDayDaycare.copy(
            id = ServiceNeedOptionId(UUID.randomUUID()),
            defaultOption = false,
            feeCoefficient = BigDecimal("0.00"),
        )

    @BeforeEach
    fun beforeEach() {
        db.transaction { tx ->
            tx.insertServiceNeedOption(snDaycareFullDay35)
            tx.insertServiceNeedOption(snFreeFiveYearOldsPartDay)
            tx.insert(area)
            tx.insert(municipalUnit)
            tx.insert(voucherUnit)
            tx.insert(employee)
            tx.insert(guardian, DevPersonType.ADULT)
            tx.insert(child, DevPersonType.CHILD)
            tx.insertGuardian(guardian.id, child.id)
        }
    }

    @Test
    fun `free service need without a fee decision is sent as free of charge`() {
        insertPlacement(
            municipalUnit,
            PlacementType.DAYCARE_PART_TIME_FIVE_YEAR_OLDS,
            snFreeFiveYearOldsPartDay,
        )
        insertParentship(child)

        assertEquals(
            listOf(maksutieto(maksun_peruste_koodi = "MP01", asiakasmaksu = 0.0)),
            evakaMaksutiedot(),
        )
    }

    @Test
    fun `free of charge fee data is split when family size changes`() {
        insertPlacement(
            municipalUnit,
            PlacementType.DAYCARE_PART_TIME_FIVE_YEAR_OLDS,
            snFreeFiveYearOldsPartDay,
        )
        insertParentship(child)
        val siblingStart = LocalDate.of(2021, 4, 1)
        val sibling = DevPerson(dateOfBirth = siblingStart)
        db.transaction { tx -> tx.insert(sibling, DevPersonType.CHILD) }
        insertParentship(sibling, startDate = siblingStart)

        assertEquals(
            listOf(
                maksutieto(
                    maksun_peruste_koodi = "MP01",
                    asiakasmaksu = 0.0,
                    paattymis_pvm = siblingStart.minusDays(1),
                ),
                maksutieto(
                    maksun_peruste_koodi = "MP01",
                    asiakasmaksu = 0.0,
                    alkamis_pvm = siblingStart,
                    perheen_koko = 3,
                ),
            ),
            evakaMaksutiedot(),
        )
    }

    @Test
    fun `free service need without a head of family has no fee data`() {
        insertPlacement(
            municipalUnit,
            PlacementType.DAYCARE_PART_TIME_FIVE_YEAR_OLDS,
            snFreeFiveYearOldsPartDay,
        )

        assertEquals(emptyList(), evakaMaksutiedot())
    }

    @Test
    fun `sent fee decision takes precedence over a free service need`() {
        insertPlacement(
            municipalUnit,
            PlacementType.DAYCARE_PART_TIME_FIVE_YEAR_OLDS,
            snFreeFiveYearOldsPartDay,
        )
        insertParentship(child)
        // e.g. the service need was changed to a free one, but a new decision has not been sent
        val feeDecisionRange = FiniteDateRange(range.start, LocalDate.of(2021, 2, 28))
        insertFeeDecision(
            placementType = PlacementType.DAYCARE_FIVE_YEAR_OLDS,
            serviceNeedFeeCoefficient = BigDecimal("0.50"),
            finalFee = 15000,
            validDuring = feeDecisionRange,
        )

        assertEquals(
            listOf(
                maksutieto(
                    maksun_peruste_koodi = "MP02",
                    asiakasmaksu = 150.0,
                    paattymis_pvm = feeDecisionRange.end,
                ),
                maksutieto(
                    maksun_peruste_koodi = "MP01",
                    asiakasmaksu = 0.0,
                    alkamis_pvm = feeDecisionRange.end.plusDays(1),
                ),
            ),
            evakaMaksutiedot(),
        )
    }

    @Test
    fun `voucher value decision of a free service need is sent as free of charge without voucher value`() {
        insertPlacement(
            voucherUnit,
            PlacementType.DAYCARE_PART_TIME_FIVE_YEAR_OLDS,
            snFreeFiveYearOldsPartDay,
        )
        insertVoucherValueDecision(
            placementType = PlacementType.DAYCARE_PART_TIME_FIVE_YEAR_OLDS,
            serviceNeedFeeCoefficient = snFreeFiveYearOldsPartDay.feeCoefficient,
            finalCoPayment = 0,
        )

        assertEquals(
            listOf(
                maksutieto(
                    maksun_peruste_koodi = "MP01",
                    asiakasmaksu = 0.0,
                    palveluseteli_arvo = 0.0,
                )
            ),
            evakaMaksutiedot(),
        )
    }

    @Test
    fun `voucher value decision of a free service need is not free of charge when co-payment is not zero`() {
        insertPlacement(
            voucherUnit,
            PlacementType.DAYCARE_PART_TIME_FIVE_YEAR_OLDS,
            snFreeFiveYearOldsPartDay,
        )
        // e.g. an increasing fee alteration makes the co-payment non-zero
        insertVoucherValueDecision(
            placementType = PlacementType.DAYCARE_PART_TIME_FIVE_YEAR_OLDS,
            serviceNeedFeeCoefficient = snFreeFiveYearOldsPartDay.feeCoefficient,
            finalCoPayment = 5000,
        )

        assertEquals(
            listOf(
                maksutieto(
                    maksun_peruste_koodi = "MP02",
                    asiakasmaksu = 50.0,
                    palveluseteli_arvo = 1000.0,
                )
            ),
            evakaMaksutiedot(),
        )
    }

    @Test
    fun `zero co-payment due to income is not free of charge fee basis`() {
        insertPlacement(voucherUnit, PlacementType.DAYCARE)
        insertVoucherValueDecision(
            placementType = PlacementType.DAYCARE,
            serviceNeedFeeCoefficient = snDaycareFullDay35.feeCoefficient,
            finalCoPayment = 0,
        )

        assertEquals(
            listOf(
                maksutieto(
                    maksun_peruste_koodi = "MP03",
                    asiakasmaksu = 0.0,
                    palveluseteli_arvo = 1000.0,
                )
            ),
            evakaMaksutiedot(),
        )
    }

    @Test
    fun `five year old fee basis is kept when service need is not free of charge`() {
        insertPlacement(municipalUnit, PlacementType.DAYCARE_FIVE_YEAR_OLDS)
        insertFeeDecision(
            placementType = PlacementType.DAYCARE_FIVE_YEAR_OLDS,
            serviceNeedFeeCoefficient = BigDecimal("0.50"),
            finalFee = 15000,
        )

        assertEquals(
            listOf(maksutieto(maksun_peruste_koodi = "MP02", asiakasmaksu = 150.0)),
            evakaMaksutiedot(),
        )
    }

    @Test
    fun `zero final fee due to income is not free of charge fee basis`() {
        insertPlacement(municipalUnit, PlacementType.DAYCARE)
        insertFeeDecision(
            placementType = PlacementType.DAYCARE,
            serviceNeedFeeCoefficient = BigDecimal("1.00"),
            finalFee = 0,
        )

        assertEquals(
            listOf(maksutieto(maksun_peruste_koodi = "MP03", asiakasmaksu = 0.0)),
            evakaMaksutiedot(),
        )
    }

    private fun insertPlacement(
        unit: DevDaycare,
        placementType: PlacementType,
        serviceNeedOption: ServiceNeedOption = snDaycareFullDay35,
    ) {
        db.transaction { tx ->
            val placementId =
                tx.insert(
                    DevPlacement(
                        type = placementType,
                        childId = child.id,
                        unitId = unit.id,
                        startDate = range.start,
                        endDate = range.end,
                    )
                )
            tx.insert(
                DevServiceNeed(
                    placementId = placementId,
                    startDate = range.start,
                    endDate = range.end,
                    optionId = serviceNeedOption.id,
                    confirmedBy = employee.evakaUserId,
                    confirmedAt = HelsinkiDateTime.now(),
                )
            )
        }
    }

    private fun insertParentship(child: DevPerson, startDate: LocalDate = range.start) {
        db.transaction { tx ->
            tx.insert(
                DevParentship(
                    childId = child.id,
                    headOfChildId = guardian.id,
                    startDate = startDate,
                    endDate = range.end,
                )
            )
        }
    }

    private fun insertVoucherValueDecision(
        placementType: PlacementType,
        serviceNeedFeeCoefficient: BigDecimal,
        finalCoPayment: Int,
    ) {
        db.transaction { tx ->
            tx.insert(
                DevVoucherValueDecision(
                    childId = child.id,
                    headOfFamilyId = guardian.id,
                    placementUnitId = voucherUnit.id,
                    validFrom = range.start,
                    validTo = range.end,
                    status = VoucherValueDecisionStatus.SENT,
                    placementType = placementType,
                    serviceNeedFeeCoefficient = serviceNeedFeeCoefficient,
                    voucherValue = 100000,
                    finalCoPayment = finalCoPayment,
                )
            )
        }
    }

    private fun insertFeeDecision(
        placementType: PlacementType,
        serviceNeedFeeCoefficient: BigDecimal,
        finalFee: Int,
        validDuring: FiniteDateRange = range,
    ) {
        val feeDecision =
            DevFeeDecision(
                headOfFamilyId = guardian.id,
                validDuring = validDuring,
                status = FeeDecisionStatus.SENT,
            )
        db.transaction { tx ->
            tx.insert(feeDecision)
            tx.insert(
                DevFeeDecisionChild(
                    feeDecisionId = feeDecision.id,
                    childId = child.id,
                    placementType = placementType,
                    placementUnitId = municipalUnit.id,
                    serviceNeedFeeCoefficient = serviceNeedFeeCoefficient,
                    finalFee = finalFee,
                )
            )
        }
    }

    private fun evakaMaksutiedot(): List<Maksutieto> {
        val updater =
            VardaUpdater(DateRange(LocalDate.of(2019, 1, 1), null), organizerOid, "sourceSystem")
        val evakaState = db.read { updater.getEvakaState(it, today, child.id) }
        return evakaState!!.lapset.single().maksutiedot
    }

    private fun maksutieto(
        maksun_peruste_koodi: String,
        asiakasmaksu: Double,
        palveluseteli_arvo: Double = 0.0,
        alkamis_pvm: LocalDate = range.start,
        paattymis_pvm: LocalDate = range.end,
        perheen_koko: Int = 2,
    ) =
        Maksutieto(
            alkamis_pvm = alkamis_pvm,
            paattymis_pvm = paattymis_pvm,
            perheen_koko = perheen_koko,
            maksun_peruste_koodi = maksun_peruste_koodi,
            asiakasmaksu = asiakasmaksu,
            palveluseteli_arvo = palveluseteli_arvo,
            huoltajat =
                listOf(
                    Huoltaja(
                        henkilotunnus = guardian.ssn!!,
                        henkilo_oid = null,
                        etunimet = guardian.firstName,
                        sukunimi = guardian.lastName,
                    )
                ),
        )
}
