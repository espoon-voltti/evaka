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
import evaka.core.shared.dev.DevCareArea
import evaka.core.shared.dev.DevDaycare
import evaka.core.shared.dev.DevEmployee
import evaka.core.shared.dev.DevFeeDecision
import evaka.core.shared.dev.DevFeeDecisionChild
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
import java.math.BigDecimal
import java.time.LocalDate
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
    private val child = DevPerson(ssn = "030320A904N")
    private val range = FiniteDateRange(LocalDate.of(2021, 1, 1), LocalDate.of(2021, 6, 30))
    private val today = LocalDate.of(2024, 1, 1)

    @BeforeEach
    fun beforeEach() {
        db.transaction { tx ->
            tx.insertServiceNeedOption(snDaycareFullDay35)
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
    fun `free of charge fee basis when fee coefficient and final fee are zero`() {
        insertPlacement(municipalUnit, PlacementType.DAYCARE_FIVE_YEAR_OLDS)
        insertFeeDecision(
            placementType = PlacementType.DAYCARE_FIVE_YEAR_OLDS,
            serviceNeedFeeCoefficient = BigDecimal("0.00"),
            finalFee = 0,
        )

        assertEquals(
            listOf(maksutieto(maksun_peruste_koodi = "MP01", asiakasmaksu = 0.0)),
            evakaMaksutiedot(),
        )
    }

    @Test
    fun `free of charge voucher value decision is sent without voucher value`() {
        insertPlacement(voucherUnit, PlacementType.DAYCARE)
        db.transaction { tx ->
            tx.insert(
                DevVoucherValueDecision(
                    childId = child.id,
                    headOfFamilyId = guardian.id,
                    placementUnitId = voucherUnit.id,
                    validFrom = range.start,
                    validTo = range.end,
                    status = VoucherValueDecisionStatus.SENT,
                    serviceNeedFeeCoefficient = BigDecimal("0.00"),
                    voucherValue = 100000,
                    finalCoPayment = 0,
                )
            )
        }

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
    fun `fee basis is not free of charge when fee coefficient is zero but final fee is not`() {
        insertPlacement(municipalUnit, PlacementType.DAYCARE_FIVE_YEAR_OLDS)
        // e.g. an increasing fee alteration makes the final fee non-zero
        insertFeeDecision(
            placementType = PlacementType.DAYCARE_FIVE_YEAR_OLDS,
            serviceNeedFeeCoefficient = BigDecimal("0.00"),
            finalFee = 5000,
        )

        assertEquals(
            listOf(maksutieto(maksun_peruste_koodi = "MP02", asiakasmaksu = 50.0)),
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

    private fun insertPlacement(unit: DevDaycare, placementType: PlacementType) {
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
                    optionId = snDaycareFullDay35.id,
                    confirmedBy = employee.evakaUserId,
                    confirmedAt = HelsinkiDateTime.now(),
                )
            )
        }
    }

    private fun insertFeeDecision(
        placementType: PlacementType,
        serviceNeedFeeCoefficient: BigDecimal,
        finalFee: Int,
    ) {
        val feeDecision =
            DevFeeDecision(
                headOfFamilyId = guardian.id,
                validDuring = range,
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
    ) =
        Maksutieto(
            alkamis_pvm = range.start,
            paattymis_pvm = range.end,
            perheen_koko = 2,
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
