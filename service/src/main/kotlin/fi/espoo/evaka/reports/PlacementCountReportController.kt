// SPDX-FileCopyrightText: 2017-2023 City of Espoo
//
// SPDX-License-Identifier: LGPL-2.1-or-later

package fi.espoo.evaka.reports

import fi.espoo.evaka.Audit
import fi.espoo.evaka.daycare.domain.ProviderType
import fi.espoo.evaka.placement.PlacementType
import fi.espoo.evaka.shared.AreaId
import fi.espoo.evaka.shared.DaycareId
import fi.espoo.evaka.shared.auth.AuthenticatedUser
import fi.espoo.evaka.shared.db.Database
import fi.espoo.evaka.shared.domain.EvakaClock
import fi.espoo.evaka.shared.security.AccessControl
import fi.espoo.evaka.shared.security.Action
import java.math.BigDecimal
import java.time.LocalDate
import org.springframework.format.annotation.DateTimeFormat
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.RequestParam
import org.springframework.web.bind.annotation.RestController

@RestController
class PlacementCountReportController(private val accessControl: AccessControl) {
    @GetMapping("/reports/placement-count")
    fun getPlacementCountReport(
        db: Database,
        user: AuthenticatedUser,
        clock: EvakaClock,
        @RequestParam("examinationDate")
        @DateTimeFormat(iso = DateTimeFormat.ISO.DATE)
        examinationDate: LocalDate,
        @RequestParam(name = "providerTypes")
        requestedProviderTypes: List<ProviderType> = emptyList(),
        @RequestParam(name = "placementTypes")
        requestedPlacementTypes: List<PlacementType> = emptyList()
    ): PlacementCountReportResult {
        return db.connect { dbc ->
                dbc.read {
                    accessControl.requirePermissionFor(
                        it,
                        user,
                        clock,
                        Action.Global.READ_PLACEMENT_COUNT_REPORT
                    )
                    it.setStatementTimeout(REPORT_STATEMENT_TIMEOUT)
                    it.getPlacementCountReportRows(
                        examinationDate,
                        requestedProviderTypes.ifEmpty { ProviderType.values().toList() },
                        requestedPlacementTypes.ifEmpty { PlacementType.values().toList() }
                    )
                }
            }
            .also {
                Audit.PlacementCountReportRead.log(
                    meta =
                        mapOf(
                            "examinationDate" to examinationDate,
                            "providerTypes" to requestedProviderTypes,
                            "careTypes" to requestedPlacementTypes
                        )
                )
            }
    }

    private fun Database.Read.getPlacementCountReportRows(
        examinationDate: LocalDate,
        providerTypes: List<ProviderType>,
        placementTypes: List<PlacementType>
    ): PlacementCountReportResult {
        val resultRows =
            createQuery {
                    sql(
                        """
SELECT ca.id                                                             AS area_id,
       ca.name                                                           AS area_name,
       d.id                                                              AS daycare_id,
       d.name                                                            AS daycare_name,
       count(pl.id)                                                      AS placement_count,
       count(pl.id)
       FILTER (WHERE cwa.age_in_years > 2)                               AS placement_count_3v_and_over,
       count(pl.id)
       FILTER (WHERE cwa.age_in_years < 3)                               AS placement_count_under_3v,
       COALESCE(sum(CASE
                        WHEN cwa.age_in_years > 2
                            THEN COALESCE(sno.occupancy_coefficient, default_sno.occupancy_coefficient) * COALESCE(an.capacity_factor, 1.00)
                        ELSE COALESCE(sno.occupancy_coefficient_under_3y, default_sno.occupancy_coefficient_under_3y) *
                             COALESCE(an.capacity_factor, 1.00) END), 0) AS calculated_placements
FROM daycare d
         JOIN care_area ca
              ON d.care_area_id = ca.id
         JOIN placement pl
              ON pl.unit_id = d.id AND daterange(pl.start_date, pl.end_date, '[]') @> ${bind(examinationDate)}::date
         LEFT JOIN service_need sn
              ON sn.placement_id = pl.id AND daterange(sn.start_date, sn.end_date, '[]') @> ${bind(examinationDate)}::date
         LEFT JOIN service_need_option sno
              ON sn.option_id = sno.id
         LEFT JOIN service_need_option default_sno
              ON pl.type = default_sno.valid_placement_type AND default_sno.default_option
         LEFT JOIN assistance_factor an ON an.child_id = pl.child_id AND an.valid_during @> ${bind(examinationDate)}
         JOIN LATERAL (SELECT date_part('year', age(${bind(examinationDate)}, p.date_of_birth)) AS age_in_years
                       FROM person p
                       WHERE p.id = pl.child_id) cwa
              ON TRUE
WHERE d.opening_date <= ${bind(examinationDate)}
  AND (d.closing_date IS NULL OR d.closing_date >= ${bind(examinationDate)})
  AND d.provider_type = ANY
      (${bind(providerTypes)}::unit_provider_type[])
  AND pl.type = ANY(${bind(placementTypes)}::placement_type[])
GROUP BY ROLLUP ((ca.id, ca.name), (d.id, d.name))
ORDER BY ca.name, d.name ASC
            """
                    )
                }
                .toList<PlacementCountReportRow>()

        val daycaresByArea = mutableMapOf<String, MutableList<PlacementCountDaycareResult>>()
        val collectedAreaResults = mutableListOf<PlacementCountAreaResult>()
        var totalResult =
            PlacementCountReportResult(
                areaResults = emptyList(),
                placementCount = 0,
                placementCountUnder3v = 0,
                placementCount3vAndOver = 0,
                calculatedPlacements = BigDecimal(0.0)
            )

        resultRows.forEach { row ->
            if (row.daycareId != null && row.daycareName != null) {
                val daycareList = daycaresByArea.getOrPut(row.areaId.toString()) { mutableListOf() }
                daycareList.add(
                    PlacementCountDaycareResult(
                        daycareId = row.daycareId,
                        daycareName = row.daycareName,
                        placementCount = row.placementCount,
                        placementCountUnder3v = row.placementCountUnder3v,
                        placementCount3vAndOver = row.placementCount3vAndOver,
                        calculatedPlacements = row.calculatedPlacements
                    )
                )
            } else if (row.areaId != null && row.areaName != null) {
                collectedAreaResults.add(
                    PlacementCountAreaResult(
                        areaId = row.areaId,
                        areaName = row.areaName,
                        placementCount = row.placementCount,
                        placementCountUnder3v = row.placementCountUnder3v,
                        placementCount3vAndOver = row.placementCount3vAndOver,
                        calculatedPlacements = row.calculatedPlacements,
                        daycareResults = daycaresByArea[row.areaId.toString()].orEmpty()
                    )
                )
            } else {
                totalResult =
                    PlacementCountReportResult(
                        placementCount = row.placementCount,
                        placementCountUnder3v = row.placementCountUnder3v,
                        placementCount3vAndOver = row.placementCount3vAndOver,
                        calculatedPlacements = row.calculatedPlacements,
                        areaResults = collectedAreaResults
                    )
            }
        }

        return totalResult
    }

    data class PlacementCountReportRow(
        val areaId: AreaId?,
        val areaName: String?,
        val daycareId: DaycareId?,
        val daycareName: String?,
        val placementCount3vAndOver: Int,
        val placementCountUnder3v: Int,
        val placementCount: Int,
        val calculatedPlacements: BigDecimal
    )

    data class PlacementCountReportResult(
        val placementCount: Int,
        val placementCount3vAndOver: Int,
        val placementCountUnder3v: Int,
        val calculatedPlacements: BigDecimal,
        val areaResults: List<PlacementCountAreaResult>
    )

    data class PlacementCountAreaResult(
        val areaId: AreaId,
        val areaName: String,
        val placementCount: Int,
        val placementCount3vAndOver: Int,
        val placementCountUnder3v: Int,
        val calculatedPlacements: BigDecimal,
        val daycareResults: List<PlacementCountDaycareResult>
    )

    data class PlacementCountDaycareResult(
        val daycareId: DaycareId,
        val daycareName: String,
        val placementCount: Int,
        val placementCount3vAndOver: Int,
        val placementCountUnder3v: Int,
        val calculatedPlacements: BigDecimal
    )
}
