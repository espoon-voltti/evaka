// SPDX-FileCopyrightText: 2017-2026 City of Espoo
//
// SPDX-License-Identifier: LGPL-2.1-or-later

package evaka.instance.oulu.dw

import evaka.core.shared.db.Database
import evaka.core.shared.db.QuerySql
import kotlin.reflect.KClass

object DwQueries {
    val getAbsences =
        csvQuery<DwAbsence> {
            sql(
                """
SELECT
    ab.child_id     AS lapsenID,
    ab.date         AS poissaolonpvm,
    ab.absence_type AS poissaolontyyppi,
    ab.category     AS poissaolonkategoria,
    pl.type         AS sijoitustyyppi
FROM absence ab, placement pl
WHERE current_date - INTERVAL '3 months' <= ab.date
    AND ab.child_id = pl.child_id
    AND pl.start_date <= ab.date
    AND pl.end_date >= ab.date
ORDER BY ab.date, ab.child_id
"""
            )
        }

    val getApplicationInfos =
        csvQuery<DwApplicationInfo> {
            sql(
                """
WITH application_infos AS (
    SELECT
        now() AT TIME ZONE 'Europe/Helsinki'                              AS tiedoston_ajopaiva,
        ap.id                                                             AS hakemuksen_id,
        ap.created_at                                                     AS hakemus_luotu,
        ap.modified_at                                                    AS hakemusta_paivitetty,
        ap.type                                                           AS tyyppi,
        ap.status                                                         AS tilanne,
        ap.origin                                                         AS alkupera,
        ap.transferapplication                                            AS siirtohakemus,
        ap.child_id                                                       AS lapsen_id,
        pe.date_of_birth                                                  AS syntymaaika,
        jsonb_array_elements_text(ap.document->'apply'->'preferredUnits') AS yksikot,
        (ap.document->>'preferredStartDate')::date                        AS haluttu_aloituspaiva
    FROM application ap, person pe
    WHERE current_date - INTERVAL '12 months' <= ap.created_at
    AND ap.child_id = pe.id
ORDER BY ap.created_at DESC)
SELECT
    hakemuksen_id,
    hakemus_luotu,
    hakemusta_paivitetty,
    tyyppi,
    tilanne,
    alkupera,
    siirtohakemus,
    lapsen_id,
    syntymaaika,
    yksikot,
    haluttu_aloituspaiva,
    dg.name               AS yksikko_nimi,
    dg.care_area_id       AS alue_id,
    ca.name               AS alue_nimi
FROM application_infos, daycare dg, care_area ca
WHERE dg.id IN (application_infos.yksikot::uuid)
    AND dg.care_area_id = ca.id
"""
            )
        }

    val getAssistanceActions =
        csvQuery<DwAssistanceAction> {
            sql(
                """
SELECT
    now() AT TIME ZONE 'Europe/Helsinki'    	AS pvm,
    ac.child_id                                	AS lapsen_id,
    aao.name_fi                                 AS tukitoimi,
    ac.other_action                             AS muu_tukitoimi,
    ac.start_date                               AS aloitus_pvm,
    ac.end_date                                 AS loppu_pvm,
    aao.category                                AS tuen_tyyppi
FROM assistance_action ac
    LEFT JOIN assistance_action_option_ref aaor ON aaor.action_id = ac.id
    LEFT JOIN assistance_action_option aao ON aao.id = aaor.option_id
WHERE current_date - INTERVAL '3 months' <= ac.end_date
"""
            )
        }

    val getAssistanceNeedDecisions =
        csvQuery<DwAssistanceNeedDecision> { sql("SELECT NULL WHERE FALSE") }

    val getChildReservations =
        csvQuery<DwChildReservations> {
            sql(
                """
                SELECT
                    ca.child_id AS lapsen_id,
                    ca.date AS paivamaara,
                    ar.start_time AS varaus_alkaa,
                    ar.end_time AS varaus_paattyy,
                    ca.start_time AS toteuma_alkaa,
                    ca.end_time AS toteuma_paattyy,
                    ca.unit_id AS yksikon_id
                FROM child_attendance ca
                         LEFT JOIN attendance_reservation ar ON ca.child_id = ar.child_id
                    AND ca.date = ar.date
                WHERE current_date::DATE - INTERVAL '3 months' <= ca.date
                ORDER BY ca.date DESC
                """
                    .trimIndent()
            )
        }

    val getDailyInfos =
        csvQuery<DwDailyInfo> {
            sql(
                """
SELECT
    now() AT TIME ZONE 'Europe/Helsinki'    AS pvm,
    p.id                                    AS lapsen_id,
    p.social_security_number                AS henkilöturvatunnus,
    p.date_of_birth                         AS syntymäaika,
    p.language                              AS kieli,
    p.street_address                        AS postiosoite,
    p.postal_code                           AS postinumero,
    p.post_office                           AS postitoimipaikka,
    p.nationalities                         AS kansalaisuudet,
    pl.type                                 AS sijoitustyyppi,
    pl.unit_id                              AS sijoitusyksikkö_id,
    d.name                                  AS yksikön_nimi,
    d.care_area_id                          AS palvelualue_id,
    ca.name                                 AS palvelualue,
    d.type                                  AS toimintamuoto,
    d.provider_type                         AS järjestämistapa,
    d.dw_cost_center                        AS kustannuspaikka,
    dg.id                                   AS sijoitusryhmä_id,
    dg.name                                 AS sijoitusryhmä,
    bc.unit_id                              AS varahoitoyksikkö_id,
    bu.name                                 AS varahoitoyksikkö,
    bc.group_id                             AS varahoitoryhmä_id,
    bg.name                                 AS varahoitoryhmä,
    sn.id IS NOT NULL                       AS palveluntarve_merkitty,
    sno.name_fi                             AS palveluntarve,
    sno.id                                  AS palveluntarve_id,
    sno.part_day                            AS osapäiväinen,
    sno.part_week                           AS osaviikkoinen,
    sn.shift_care                           AS vuorohoito,
    sno.daycare_hours_per_week              AS tunteja_viikossa,
    anvc.coefficient                        AS tuentarpeen_kerroin,
    af.capacity_factor                      AS lapsen_kapasiteetti,
    array(
        SELECT DISTINCT absence_type
        FROM absence
        WHERE child_id = p.id
            AND absence.date = current_date
    )                                       AS poissaolon_syy
FROM person p
    JOIN placement pl ON pl.child_id = p.id
        AND pl.start_date <= current_date
        AND pl.end_date >= current_date
    JOIN daycare d ON pl.unit_id = d.id
    JOIN care_area ca ON d.care_area_id = ca.id
    JOIN daycare_group_placement dgp ON pl.id = dgp.daycare_placement_id
        AND dgp.start_date <= current_date
        AND dgp.end_date >= current_date
    JOIN daycare_group dg ON d.id = dg.daycare_id
        AND dgp.daycare_group_id = dg.id
    LEFT JOIN backup_care bc ON p.id = bc.child_id
        AND bc.start_date <= current_date
        AND bc.end_date >= current_date
    LEFT JOIN daycare bu ON bu.id = bc.unit_id
    LEFT JOIN daycare_group bg ON bg.id = bc.group_id
    LEFT JOIN service_need sn ON pl.id = sn.placement_id
        AND sn.start_date <= current_date
        AND sn.end_date >= current_date
    LEFT JOIN service_need_option sno ON sno.id = sn.option_id
    LEFT JOIN assistance_factor af ON p.id = af.child_id
        AND lower(af.valid_during) <= current_date
        AND upper(af.valid_during) >= current_date
    LEFT JOIN assistance_need_voucher_coefficient anvc ON p.id = anvc.child_id
        AND lower(anvc.validity_period) <= current_date
        AND upper(anvc.validity_period) >= current_date
"""
            )
        }

    val getDailyUnitsAndGroupsAttendances =
        csvQuery<DwDailyUnitsAndGroupsAttendance> {
            sql(
                """
WITH staff_attendance_aggregate AS (
    SELECT group_id, arrived, departed, occupancy_coefficient
    FROM staff_attendance_realtime
    WHERE departed IS NOT NULL
        AND type = ANY('{PRESENT,OVERTIME,JUSTIFIED_CHANGE}'::staff_attendance_type[])
        AND (current_date - INTERVAL '1 week' <= DATE(arrived) OR current_date - interval '1 week' <= DATE(departed))
    UNION ALL
    SELECT group_id, arrived, departed, occupancy_coefficient
    FROM staff_attendance_external
    WHERE departed IS NOT NULL
        AND (current_date - INTERVAL '1 week' <= DATE(arrived) OR current_date - interval '1 week' <= DATE(departed))
),
caretaker_counts AS (
    SELECT
        u.id                           AS unit_id,
        g.id                           AS group_id,
        t::date                        AS date,
        COALESCE(
            SUM(
                CASE
                    WHEN (t::DATE = DATE(saa.arrived) OR t::DATE = DATE(saa.departed))
                        THEN ROUND(EXTRACT(EPOCH FROM (
                            LEAST(saa.departed, timezone('Europe/Helsinki', (t::DATE + 1)::DATE::TIMESTAMP)) -
                            GREATEST(saa.arrived, timezone('Europe/Helsinki', t::DATE::TIMESTAMP))
                        )) / 3600 / 7.65 * saa.occupancy_coefficient / 7, 4)
                    ELSE s.count
                END
            ), 0.0
        )                               AS caretaker_count
    FROM generate_series(current_date - interval '1 week', current_date, '1 day') t
        CROSS JOIN daycare_group g
        JOIN daycare u ON g.daycare_id = u.id
            AND daterange(g.start_date, g.end_date, '[]') @> t::DATE
        LEFT JOIN staff_attendance_aggregate saa ON g.id = saa.group_id
        LEFT JOIN staff_attendance s ON g.id = s.group_id
            AND t::DATE = s.date
    WHERE date_part('isodow', t::DATE) = ANY(u.operation_days)
        AND daterange(u.opening_date, u.closing_date, '[]') @> t::DATE
    GROUP BY u.id, g.id, t
)
SELECT
    now() AT TIME ZONE 'Europe/Helsinki'   AS aikaleima,
    cc.date                                AS pvm,
    d.name                                 AS toimintayksikkö,
    d.id                                   AS toimintayksikkö_id,
    d.operation_days                       AS toimintapäivät,
    d.provides_shift_care                  AS vuorohoitoyksikkö,
    d.shift_care_operation_days            AS vuorohoitopäivät,
    d.shift_care_open_on_holidays          AS vuorohoitopyhäpäivinä,
    dg.name                                AS ryhmä,
    dg.id                                  AS ryhmä_id,
    d.capacity                             AS toimintayksikön_laskennallinen_lapsimäärä,
    (
        SELECT count(*)
        FROM placement p
        WHERE p.unit_id = d.id
            AND p.start_date <= cc.date
            AND cc.date <= p.end_date
    )                                      AS toimintayksikön_lapsimäärä,
    dc.amount                              AS henkilökuntaa_ryhmässä,
    cc.caretaker_count                     AS henkilökuntaa_läsnä,
    (
        SELECT SUM(caretaker_count)
        FROM caretaker_counts
        WHERE unit_id = d.id
            AND date = cc.date
    )                                      AS kasvatusvastuullisten_lkm_yksikössä,
    (
        SELECT count(*)
        FROM daycare_group_placement dgp
        WHERE dgp.daycare_group_id = dg.id
            AND dgp.start_date <= cc.date
            AND cc.date <= dgp.end_date
    )                                      AS ryhmän_lapsimäärä
FROM daycare_group dg
    JOIN daycare d ON dg.daycare_id = d.id
    JOIN care_area ca ON ca.id = d.care_area_id
    LEFT JOIN caretaker_counts cc ON cc.group_id = dg.id
    LEFT JOIN daycare_caretaker dc ON dg.id = dc.group_id
        AND dc.start_date <= cc.date
        AND (dc.end_date >= cc.date OR dc.end_date IS NULL)
    LEFT JOIN staff_attendance_aggregate saa ON dg.id = saa.group_id
WHERE (current_date - interval '3 months' <= d.closing_date OR d.closing_date IS NULL)
    AND (current_date - interval '3 months' <= dg.end_date OR dg.end_date IS NULL)
GROUP BY
    cc.date,
    d.name,
    d.id,
    dg.name,
    dg.id,
    dc.amount,
    cc.caretaker_count
ORDER BY dg.name, cc.date
"""
            )
        }

    val getDailyUnitsOccupanciesConfirmed =
        csvQuery<DwDailyUnitsOccupanciesConfirmed> {
            sql(
                """
WITH
caretaker_counts AS (
    SELECT 
        a.id                                    AS area_id,
        a.name                                  AS area_name,
        u.id                                    AS unit_id,
        u.name                                  AS unit_name,
        current_date                            AS date,
        COALESCE(
            SUM(c.amount),
            0.0
        ) AS caretaker_count
    FROM daycare_group g
        JOIN daycare u ON g.daycare_id = u.id
            AND daterange(g.start_date, g.end_date, '[]') @> current_date
        JOIN care_area a ON a.id = u.care_area_id
        LEFT JOIN daycare_caretaker c ON g.id = c.group_id 
            AND daterange(c.start_date, c.end_date, '[]') @> current_date
    WHERE date_part('isodow', current_date) = ANY(u.operation_days)
        AND daterange(u.opening_date, u.closing_date, '[]') @> current_date
    GROUP BY a.id, u.id
),
placements AS (
    SELECT
        p.id AS placement_id,
        p.child_id,
        p.unit_id,
        p.type,
        u.type && ARRAY['FAMILY', 'GROUP_FAMILY']::care_types[] AS family_unit_placement,
        daterange(p.start_date, p.end_date, '[]') AS period
    FROM placement p
        JOIN daycare u ON p.unit_id = u.id
    WHERE daterange(p.start_date, p.end_date, '[]') @> current_date
),
default_sn_coefficients AS (
    SELECT
        occupancy_coefficient,
        occupancy_coefficient_under_3y,
        realized_occupancy_coefficient,
        realized_occupancy_coefficient_under_3y,
        valid_placement_type
    FROM service_need_option 
    WHERE default_option
),
placements_on_date AS (
    SELECT
        cc.date,
        cc.caretaker_count                          AS caretaker_count,
        pl.placement_id                             AS placement_id,
        pl.child_id                                 AS child_id,
        pl.unit_id                                  AS unit_id,
        coalesce(af.capacity_factor, 1)             AS assistance_coefficient,
        CASE
            WHEN pl.family_unit_placement = TRUE
                THEN 1.75
            WHEN cc.date < (p.date_of_birth + interval '3 year')::DATE
                THEN (
                    coalesce(sno.occupancy_coefficient_under_3y, dsc.occupancy_coefficient_under_3y)
                )
            ELSE (
                coalesce(sno.occupancy_coefficient, dsc.occupancy_coefficient)
            )
        END                                         AS service_need_coefficient
    from caretaker_counts cc
        JOIN (
            SELECT
                p.id AS placement_id,
                p.child_id,
                p.unit_id,
                p.type,
                u.type && array['FAMILY', 'GROUP_FAMILY']::care_types[] AS family_unit_placement,
                daterange(p.start_date, p.end_date, '[]') AS period
            FROM placement p
                JOIN daycare u ON p.unit_id = u.id
            WHERE daterange(p.start_date, p.end_date, '[]') @> current_date
        ) pl ON pl.unit_id = cc.unit_id
        JOIN person p ON p.id = pl.child_id
        LEFT JOIN assistance_factor af ON af.child_id = pl.child_id
            AND af.valid_during @> cc.date
        LEFT JOIN service_need sn ON sn.placement_id = pl.placement_id
            AND daterange(sn.start_date, sn.end_date, '[]') @> cc.date
        LEFT JOIN service_need_option sno ON sn.option_id = sno.id
        JOIN default_sn_coefficients dsc ON dsc.valid_placement_type = pl.type
)
SELECT
    pod.date                                                       AS pvm,
    pod.unit_id                                                    AS toimintayksikkö_id,
    d.name                                                         AS toimintayksikkö,
    pod.caretaker_count                                            AS kasvattajien_lkm,
    COUNT(pod.placement_id)                                        AS sijoituksien_lkm,
    SUM(pod.assistance_coefficient * pod.service_need_coefficient) AS täyttöaste_summa,
    CASE
        WHEN pod.caretaker_count IS NULL OR pod.caretaker_count = 0
            THEN NULL
        ELSE (SUM(pod.assistance_coefficient * pod.service_need_coefficient) / (pod.caretaker_count * 7)) * 100
    END                                                            AS täyttöaste_prosentteina
FROM placements_on_date pod
    JOIN daycare d ON d.id = pod.unit_id
GROUP BY
    pod.date, pod.unit_id, d.name, pod.caretaker_count
"""
            )
        }

    val getDailyUnitsOccupanciesRealized =
        csvQuery<DwDailyUnitsOccupanciesRealized> {
            sql(
                """
WITH
caretaker_counts AS (
    SELECT
        a.id                            AS area_id,
        a.name                          AS area_name,
        u.id                            AS unit_id,
        u.name                          AS unit_name,
        current_date                    AS date,
        COALESCE(
            SUM(
                CASE
                    WHEN sar.arrived IS NOT NULL AND (current_date = DATE(sar.arrived) OR current_date = DATE(sar.departed))
                        THEN ROUND(EXTRACT(EPOCH FROM (
                            LEAST(sar.departed, timezone('Europe/Helsinki', (current_date + 1)::DATE::TIMESTAMP)) - GREATEST(sar.arrived, timezone('Europe/Helsinki', current_date::TIMESTAMP))
                        )) / 3600 / 7.65 * sar.occupancy_coefficient / 7, 4)
                    ELSE s.count
                END
            ),
            0.0
        )                               AS caretaker_count
    FROM daycare_group g
        JOIN daycare u ON g.daycare_id = u.id
            AND daterange(g.start_date, g.end_date, '[]') @> current_date
        JOIN care_area a ON a.id = u.care_area_id
        LEFT JOIN (
            SELECT group_id, arrived, departed, occupancy_coefficient
            FROM staff_attendance_realtime
            WHERE departed IS NOT NULL
                AND type = ANY('{PRESENT,OVERTIME,JUSTIFIED_CHANGE}'::staff_attendance_type[])
            UNION ALL
            SELECT group_id, arrived, departed, occupancy_coefficient
            FROM staff_attendance_external
            WHERE departed IS NOT null
        ) sar ON g.id = sar.group_id
        LEFT JOIN staff_attendance s ON g.id = s.group_id
            AND current_date = s.date
    WHERE date_part('isodow', current_date) = ANY(u.operation_days)
        AND daterange(u.opening_date, u.closing_date, '[]') @> current_date
    GROUP BY a.id, u.id
),
default_sn_coefficients AS (
    SELECT
        occupancy_coefficient,
        occupancy_coefficient_under_3y,
        realized_occupancy_coefficient,
        realized_occupancy_coefficient_under_3y,
        valid_placement_type
    FROM service_need_option
    WHERE default_option
),
placements_realized AS (
    SELECT
        bc.id                                                   AS placement_id,
        bc.child_id,
        bc.unit_id,
        pl.type,
        u.type && array['FAMILY', 'GROUP_FAMILY']::care_types[] AS family_unit_placement,
        daterange(bc.start_date, bc.end_date, '[]')             AS period
    FROM backup_care bc
        JOIN daycare u ON bc.unit_id = u.id
        JOIN placement pl ON bc.child_id = pl.child_id
            AND daterange(bc.start_date, bc.end_date, '[]') && daterange(pl.start_date, pl.end_date, '[]')
    WHERE daterange(bc.start_date, bc.end_date, '[]') @> current_date
        AND NOT EXISTS (
            SELECT *
            FROM absence a
            WHERE a.child_id = pl.child_id
                AND current_date = a.date
                AND ARRAY[a.category] && absence_categories(pl.type)
        )
    UNION
    SELECT
        pl.id                                                   AS placement_id,
        pl.child_id,
        pl.unit_id,
        pl.type,
        u.type && ARRAY['FAMILY', 'GROUP_FAMILY']::care_types[] AS family_unit_placement,
        daterange(pl.start_date, pl.end_date, '[]')             AS period
    FROM placement pl
        JOIN daycare u ON pl.unit_id = u.id
    where daterange(pl.start_date, pl.end_date, '[]') @> current_date
        AND NOT EXISTS(
            SELECT *
            FROM backup_care bc
            WHERE daterange(bc.start_date, bc.end_date, '[]') @> current_date
                AND daterange(bc.start_date, bc.end_date, '[]') && daterange(pl.start_date, pl.end_date, '[]')
                AND bc.child_id = pl.child_id
        )
        AND NOT EXISTS (
            SELECT id, child_id, date, absence_type, modified_at, modified_by, category, questionnaire_id
            FROM absence a
            WHERE a.child_id = pl.child_id
                AND current_date = a.date
                AND ARRAY[a.category] && absence_categories(pl.type)
        )
),
placements_on_date AS (
    SELECT
        cc.date,
        cc.caretaker_count                                  AS caretaker_count,
        pr.placement_id                                     AS placement_id,
        pr.child_id                                         AS child_id,
        pr.unit_id                                          AS unit_id,
        coalesce(af.capacity_factor, 1)                     AS assistance_coefficient,
        CASE
            WHEN pr.family_unit_placement = TRUE
                THEN 1.75
            WHEN cc.date < (p.date_of_birth + INTERVAL '3 year')::DATE
                THEN (
                    coalesce(sno.realized_occupancy_coefficient_under_3y, dsc.realized_occupancy_coefficient_under_3y)
                )
            ELSE (
                coalesce(sno.realized_occupancy_coefficient, dsc.realized_occupancy_coefficient)
            )
        END AS service_need_coefficient
    FROM caretaker_counts cc
        JOIN placements_realized pr ON pr.unit_id = cc.unit_id
        JOIN person p ON p.id = pr.child_id
        LEFT JOIN assistance_factor af ON af.child_id = pr.child_id
            AND af.valid_during @> cc.date
        LEFT JOIN service_need sn ON sn.placement_id = pr.placement_id
            AND daterange(sn.start_date, sn.end_date, '[]') @> cc.date
        LEFT JOIN service_need_option sno ON sn.option_id = sno.id
        JOIN default_sn_coefficients dsc ON dsc.valid_placement_type = pr.type
)
SELECT
    pod.date                                                        AS pvm,
    pod.unit_id                                                     AS toimintayksikkö_id,
    d.name                                                          AS toimintayksikkö,
    pod.caretaker_count                                             AS kasvattajien_lkm,
    count(pod.placement_id)                                         AS sijoituksien_lkm,
    SUM(pod.assistance_coefficient * pod.service_need_coefficient)  AS käyttöaste_summa,
    CASE
        WHEN pod.caretaker_count IS NULL OR pod.caretaker_count = 0
            THEN null
        ELSE (SUM(pod.assistance_coefficient * pod.service_need_coefficient) / (pod.caretaker_count * 7)) * 100
    END                                                             AS käyttöaste_prosentteina
FROM placements_on_date pod
    JOIN daycare d ON d.id = pod.unit_id
GROUP BY
    pod.date, pod.unit_id, d.name, pod.caretaker_count
"""
            )
        }

    val getDaycareAssistances =
        csvQuery<DwDaycareAssistance> {
            sql(
                """
SELECT
    now() AT TIME ZONE 'Europe/Helsinki'    AS pvm,
    child_id                                AS lapsen_id,
    level                                   AS tuentarve_varhaiskasvatuksessa,
    lower(valid_during)                     AS aloitus_pvm,
    upper(valid_during)                     AS loppu_pvm
FROM daycare_assistance
WHERE current_date - INTERVAL '3 months' <= upper(valid_during)
"""
            )
        }

    val getFeeDecisions =
        csvQuery<DwFeeDecision> {
            sql(
                """
SELECT
    now() AT TIME ZONE 'Europe/Helsinki'    AS aikaleima,
    fd.decision_number                      AS maksupäätöksen_numero,
    fd.id                                   AS maksupäätös_id,
    lower(fd.valid_during)                  AS alkupvm,
    upper(fd.valid_during)                  AS loppupvm,
    fd.decision_type                        AS huojennustyyppi,
    fd.family_size                          AS perhekoko,
    fd.total_fee                            AS kokonaismaksu,
    fd.status                               AS tila,
    fdc.child_id                            AS lapsi_id,
    fdc.final_fee                           AS lapsikohtainen_maksu,
    fdc.placement_type                      AS toimintamuoto,
    ca.name                                 AS palvelualue,
    ca.id                                   AS palvelualue_id,
    d.name                                  AS toimipaikka,
    d.id                                    AS toimipaikka_id,
    d.dw_cost_center                        AS kustannuspaikka
FROM fee_decision fd
    JOIN fee_decision_child fdc on fd.id = fdc.fee_decision_id
    JOIN daycare d ON fdc.placement_unit_id = d.id
    JOIN care_area ca ON d.care_area_id = ca.id
WHERE fd.decision_number IS NOT NULL -- ei tuoda effican päätöksiä
    AND current_date - INTERVAL '3 months' <= upper(fd.valid_during)
"""
            )
        }

    val getOtherAssistanceMeasures =
        csvQuery<DwOtherAssistanceMeasure> {
            sql(
                """
SELECT
    now() AT TIME ZONE 'Europe/Helsinki'    AS pvm,
    child_id                                AS lapsen_id,
    type                                    AS muu_toimi,
    lower(valid_during)                     AS aloitus_pvm,
    upper(valid_during)                     AS loppu_pvm
FROM other_assistance_measure
WHERE current_date - INTERVAL '3 months' <= upper(valid_during)
"""
            )
        }

    val getPlacements =
        csvQuery<DwPlacement> {
            sql(
                """
SELECT
    now() AT TIME ZONE 'Europe/Helsinki' AS aikaleima,
    p.id                                 AS lapsen_id,
    d.name                               AS toimintayksikkö,
    d.id                                 AS toimintayksikkö_id,
    pl.start_date                        AS sijoituksen_alkupvm,
    pl.end_date                          AS sijoituksen_loppupvm,
    pl.type                              AS sijoitustyyppi,
    dg.name                              AS ryhmä,
    dg.id                                AS ryhmä_id,
    dg.start_date                        AS ryhmän_alkupvm,
    dg.end_date                          AS ryhmän_loppupvm,
    sno.name_fi                          AS palveluntarve,
    sno.id                               AS palveluntarve_id,
    sn.start_date                        AS palveluntarpeen_alkupvm,
    sn.end_date                          AS palveluntarpeen_loppupvm
FROM person p
    JOIN placement pl ON pl.child_id = p.id
        AND pl.start_date <= current_date
        AND pl.end_date >= current_date - INTERVAL '15 days'
    JOIN daycare d ON pl.unit_id = d.id
    JOIN daycare_group_placement dgp ON pl.id = dgp.daycare_placement_id
        AND dgp.start_date <= current_date
        AND dgp.end_date >= current_date - INTERVAL '15 days'
    JOIN daycare_group dg ON d.id = dg.daycare_id
        AND dgp.daycare_group_id = dg.id
    LEFT JOIN service_need sn ON pl.id = sn.placement_id
        AND sn.start_date <= current_date
        AND sn.end_date >= current_date - INTERVAL '15 days'
    LEFT JOIN service_need_option sno ON sn.option_id = sno.id
WHERE current_date - INTERVAL '3 months' <= pl.end_date
"""
            )
        }

    val getPreschoolAssistances =
        csvQuery<DwPreschoolAssistance> {
            sql(
                """
SELECT
    now() AT TIME ZONE 'Europe/Helsinki'    AS pvm,
    child_id                                AS lapsen_id,
    level                                   AS tuentarve_esiopetuksessa,
    lower(valid_during)                     AS aloitus_pvm,
    upper(valid_during)                     AS loppu_pvm
FROM preschool_assistance
WHERE current_date - INTERVAL '3 months' <= upper(valid_during)
"""
            )
        }

    val getUnitsAndGroups =
        csvQuery<DwUnitAndGroup> {
            sql(
                """
SELECT
    now() AT TIME ZONE 'Europe/Helsinki'   AS aikaleima,
    d.name                                 AS toimintayksikkö,
    d.id                                   AS toimintayksikkö_id,
    d.opening_date                         AS toimintayksikön_alkupvm,
    d.closing_date                         AS toimintayksikön_loppupvm,
    d.type                                 AS toimintamuoto,
    d.provider_type                        AS järjestämistapa,
    d.street_address                       AS katuosoite,
    d.postal_code                          AS postinumero,
    d.post_office                          AS postitoimipaikka,
    d.capacity                             AS toimintayksikön_laskennallinen_lapsimäärä,
    ca.name                                AS palvelualue,
    ca.id                                  AS palvelualue_id,
    d.dw_cost_center                       AS dw_kustannuspaikka,
    dg.name                                AS ryhmä,
    dg.id                                  AS ryhmä_id,
    dg.start_date                          AS ryhmän_alkupvm,
    dg.end_date                            AS ryhmän_loppupvm
FROM daycare_group dg
    JOIN daycare d ON dg.daycare_id = d.id
    JOIN care_area ca ON ca.id = d.care_area_id
    LEFT JOIN daycare_caretaker dc ON dg.id = dc.group_id
        AND dc.start_date <= current_date
        AND (dc.end_date >= current_date OR dc.end_date IS NULL)
WHERE (current_date - INTERVAL '3 months' <= d.closing_date OR d.closing_date IS NULL)
    AND (current_date - INTERVAL '3 months' <= dg.end_date OR dg.end_date IS NULL)
GROUP BY
    d.name,
    d.id,
    d.opening_date,
    d.closing_date,
    d.type,
    d.provider_type,
    ca.name,
    ca.id,
    d.dw_cost_center,
    dg.name,
    dg.id,
    dg.start_date,
    dg.end_date,
    dc.amount
ORDER BY d.name
"""
            )
        }

    val getVoucherValueDecisions =
        csvQuery<DwVoucherValueDecision> {
            sql(
                """
SELECT
    now() AT TIME ZONE 'Europe/Helsinki'    AS aikaleima,
    vvd.decision_number                     AS arvopäätöksen_numero,
    vvd.valid_from                          AS alkupvm,
    vvd.valid_to                            AS loppupvm,
    vvd.decision_type                       AS huojennustyyppi,
    vvd.family_size                         AS perhekoko,
    vvd.voucher_value                       AS palvelusetelin_arvo,
    vvd.final_co_payment                    AS omavastuuosuus,
    vvd.child_id                            AS lapsen_id,
    vvd.placement_type                      AS toimintamuoto,
    vvd.status                              AS tila,
    ca.name                                 AS palvelualue,
    ca.id                                   AS palvelualue_id,
    d.name                                  AS toimipaikka,
    d.id                                    AS toimipaikka_id
FROM voucher_value_decision vvd
    JOIN daycare d ON vvd.placement_unit_id = d.id
    JOIN care_area ca ON d.care_area_id = ca.id
WHERE vvd.decision_number IS NOT NULL -- ei tuoda effican päätöksiä
    AND current_date - INTERVAL '3 months' <= vvd.valid_to
"""
            )
        }
}

object FabricQueries {
    fun getAbsences(history: Boolean = false) =
        csvQuery<FabricAbsence> {
            sql(
                """
SELECT
    now() AT TIME ZONE 'Europe/Helsinki'    AS poiminta_aika,
    ab.id                                   As poissaolon_id,
    ab.modified_at                          AS muokkausaika,
    ab.child_id                             AS lapsen_id,
    ab.date                                 AS poissaolonpvm,
    ab.absence_type                         AS poissaolontyyppi,
    ab.category                             AS poissaolonkategoria,
    pl.type                                 AS sijoitustyyppi
FROM absence ab, placement pl
WHERE ab.child_id = pl.child_id
    AND pl.start_date <= ab.date
    AND pl.end_date >= ab.date
    AND CASE WHEN $history = FALSE THEN current_date - INTERVAL '3 months' <= ab.modified_at ELSE TRUE END
ORDER BY ab.date, ab.child_id
"""
            )
        }

    fun getApplicationInfos(history: Boolean = false) =
        csvQuery<FabricApplicationInfo> {
            sql(
                """
WITH application_infos AS (
    SELECT
        now() AT TIME ZONE 'Europe/Helsinki'                              AS poiminta_aika,
        ap.id                                                             AS hakemuksen_id,
        ap.created_at                                                     AS hakemus_luotu,
        ap.modified_at                                                    AS hakemusta_paivitetty,
        ap.type                                                           AS tyyppi,
        ap.status                                                         AS tilanne,
        ap.origin                                                         AS alkupera,
        ap.transferapplication                                            AS siirtohakemus,
        ap.child_id                                                       AS lapsen_id,
        pe.date_of_birth                                                  AS syntymaaika,
        jsonb_array_elements_text(ap.document->'apply'->'preferredUnits') AS yksikot,
        ap.document->'preferredStartDate'                                 AS haluttu_aloituspaiva
    FROM application ap, person pe
    WHERE CASE WHEN $history = FALSE THEN current_date - INTERVAL '12 months' <= ap.modified_at ELSE TRUE END
    AND ap.child_id = pe.id
ORDER BY ap.created_at DESC)
SELECT
    poiminta_aika,
    hakemuksen_id,
    hakemus_luotu,
    hakemusta_paivitetty,
    tyyppi,
    tilanne,
    alkupera,
    siirtohakemus,
    lapsen_id,
    syntymaaika,
    yksikot,
    haluttu_aloituspaiva,
    dg.name               AS yksikko_nimi,
    dg.care_area_id       AS alue_id,
    ca.name               AS alue_nimi
FROM application_infos, daycare dg, care_area ca
WHERE dg.id IN (application_infos.yksikot::uuid)
    AND dg.care_area_id = ca.id
"""
            )
        }

    fun getAssistanceActions(history: Boolean = false) =
        csvQuery<FabricAssistanceAction> {
            sql(
                """
SELECT
    now() AT TIME ZONE 'Europe/Helsinki'        AS poiminta_aika,
    ac.id                                       AS tukitoimen_id,
    ac.created_at                               AS luontiaika,
    ac.modified_at                              AS muokkausaika,
    ac.child_id                                 AS lapsen_id,
    aao.name_fi                                 AS tukitoimi,
    ac.other_action                             AS muu_tukitoimi,
    ac.start_date                               AS aloitus_pvm,
    ac.end_date                                 AS loppu_pvm,
    aao.category                                AS tuen_tyyppi
FROM assistance_action ac
    LEFT JOIN assistance_action_option_ref aaor ON aaor.action_id = ac.id
    LEFT JOIN assistance_action_option aao ON aao.id = aaor.option_id
WHERE CASE WHEN $history = FALSE THEN current_date - INTERVAL '3 months' <= ac.modified_at ELSE TRUE END
"""
            )
        }

    fun getChildReservations(history: Boolean = false) =
        csvQuery<FabricChildReservations> {
            sql(
                """
SELECT
    ca.id           AS varauksen_id,
    ca.created_at   AS luontiaika,
    ca.modified_at  AS muokkausaika,
    ca.child_id     AS lapsen_id,
    ca.date         AS paivamaara,
    ar.start_time   AS varaus_alkaa,
    ar.end_time     AS varaus_paattyy,
    ca.start_time   AS toteuma_alkaa,
    ca.end_time     AS toteuma_paattyy,
    ca.unit_id      AS yksikon_id
FROM child_attendance ca
    LEFT JOIN attendance_reservation ar ON ca.child_id = ar.child_id AND ca.date = ar.date
WHERE CASE WHEN $history = FALSE THEN current_date::DATE - INTERVAL '3 months' <= ca.modified_at ELSE TRUE END
ORDER BY ca.date DESC
                """
                    .trimIndent()
            )
        }

    val getDailyInfos =
        csvQuery<FabricDailyInfo> {
            sql(
                """
SELECT
    now() AT TIME ZONE 'Europe/Helsinki'    AS poiminta_aika,
    p.id                                    AS lapsen_id,
    p.social_security_number                AS henkilöturvatunnus,
    p.date_of_birth                         AS syntymäaika,
    p.language                              AS kieli,
    p.street_address                        AS postiosoite,
    p.postal_code                           AS postinumero,
    p.post_office                           AS postitoimipaikka,
    p.nationalities                         AS kansalaisuudet,
    pl.type                                 AS sijoitustyyppi,
    pl.unit_id                              AS sijoitusyksikkö_id,
    d.name                                  AS yksikön_nimi,
    d.care_area_id                          AS palvelualue_id,
    ca.name                                 AS palvelualue,
    d.type                                  AS toimintamuoto,
    d.provider_type                         AS järjestämistapa,
    d.dw_cost_center                        AS kustannuspaikka,
    dg.id                                   AS sijoitusryhmä_id,
    dg.name                                 AS sijoitusryhmä,
    bc.unit_id                              AS varahoitoyksikkö_id,
    bu.name                                 AS varahoitoyksikkö,
    bc.group_id                             AS varahoitoryhmä_id,
    bg.name                                 AS varahoitoryhmä,
    sn.id IS NOT NULL                       AS palveluntarve_merkitty,
    sno.name_fi                             AS palveluntarve,
    sno.id                                  AS palveluntarve_id,
    sno.part_day                            AS osapäiväinen,
    sno.part_week                           AS osaviikkoinen,
    sn.shift_care                           AS vuorohoito,
    sno.daycare_hours_per_week              AS tunteja_viikossa,
    anvc.coefficient                        AS tuentarpeen_kerroin,
    af.capacity_factor                      AS lapsen_kapasiteetti,
    array(
        SELECT DISTINCT absence_type
        FROM absence
        WHERE child_id = p.id
            AND absence.date = current_date
    )                                       AS poissaolon_syy
FROM person p
    JOIN placement pl ON pl.child_id = p.id
        AND pl.start_date <= current_date
        AND pl.end_date >= current_date
    JOIN daycare d ON pl.unit_id = d.id
    JOIN care_area ca ON d.care_area_id = ca.id
    JOIN daycare_group_placement dgp ON pl.id = dgp.daycare_placement_id
        AND dgp.start_date <= current_date
        AND dgp.end_date >= current_date
    JOIN daycare_group dg ON d.id = dg.daycare_id
        AND dgp.daycare_group_id = dg.id
    LEFT JOIN backup_care bc ON p.id = bc.child_id
        AND bc.start_date <= current_date
        AND bc.end_date >= current_date
    LEFT JOIN daycare bu ON bu.id = bc.unit_id
    LEFT JOIN daycare_group bg ON bg.id = bc.group_id
    LEFT JOIN service_need sn ON pl.id = sn.placement_id
        AND sn.start_date <= current_date
        AND sn.end_date >= current_date
    LEFT JOIN service_need_option sno ON sno.id = sn.option_id
    LEFT JOIN assistance_factor af ON p.id = af.child_id
        AND lower(af.valid_during) <= current_date
        AND upper(af.valid_during) >= current_date
    LEFT JOIN assistance_need_voucher_coefficient anvc ON p.id = anvc.child_id
        AND lower(anvc.validity_period) <= current_date
        AND upper(anvc.validity_period) >= current_date
"""
            )
        }

    val getDailyUnitsAndGroupsAttendances =
        csvQuery<FabricDailyUnitsAndGroupsAttendance> {
            sql(
                """
WITH staff_attendance_aggregate AS (
    SELECT group_id, arrived, departed, occupancy_coefficient
    FROM staff_attendance_realtime
    WHERE departed IS NOT NULL
        AND type = ANY('{PRESENT,OVERTIME,JUSTIFIED_CHANGE}'::staff_attendance_type[])
        AND (current_date - INTERVAL '1 week' <= DATE(arrived) OR current_date - interval '1 week' <= DATE(departed))
    UNION ALL
    SELECT group_id, arrived, departed, occupancy_coefficient
    FROM staff_attendance_external
    WHERE departed IS NOT NULL
        AND (current_date - INTERVAL '1 week' <= DATE(arrived) OR current_date - interval '1 week' <= DATE(departed))
),
caretaker_counts AS (
    SELECT
        u.id                           AS unit_id,
        g.id                           AS group_id,
        t::date                        AS date,
        COALESCE(
            SUM(
                CASE
                    WHEN (t::DATE = DATE(saa.arrived) OR t::DATE = DATE(saa.departed))
                        THEN ROUND(EXTRACT(EPOCH FROM (
                            LEAST(saa.departed, timezone('Europe/Helsinki', (t::DATE + 1)::DATE::TIMESTAMP)) -
                            GREATEST(saa.arrived, timezone('Europe/Helsinki', t::DATE::TIMESTAMP))
                        )) / 3600 / 7.65 * saa.occupancy_coefficient / 7, 4)
                    ELSE s.count
                END
            ), 0.0
        )                               AS caretaker_count
    FROM generate_series(current_date - interval '1 week', current_date, '1 day') t
        CROSS JOIN daycare_group g
        JOIN daycare u ON g.daycare_id = u.id
            AND daterange(g.start_date, g.end_date, '[]') @> t::DATE
        LEFT JOIN staff_attendance_aggregate saa ON g.id = saa.group_id
        LEFT JOIN staff_attendance s ON g.id = s.group_id
            AND t::DATE = s.date
    WHERE date_part('isodow', t::DATE) = ANY(u.operation_days)
        AND daterange(u.opening_date, u.closing_date, '[]') @> t::DATE
    GROUP BY u.id, g.id, t
)
SELECT
    now() AT TIME ZONE 'Europe/Helsinki'   AS poiminta_aika,
    cc.date                                AS päivämäärä,
    d.name                                 AS toimintayksikkö,
    d.id                                   AS toimintayksikkö_id,
    d.operation_days                       AS toimintapäivät,
    d.provides_shift_care                  AS vuorohoitoyksikkö,
    d.shift_care_operation_days            AS vuorohoitopäivät,
    d.shift_care_open_on_holidays          AS vuorohoitopyhäpäivinä,
    dg.name                                AS ryhmä,
    dg.id                                  AS ryhmä_id,
    d.capacity                             AS toimintayksikön_laskennallinen_lapsimäärä,
    (
        SELECT count(*)
        FROM placement p
        WHERE p.unit_id = d.id
            AND p.start_date <= cc.date
            AND cc.date <= p.end_date
    )                                      AS toimintayksikön_lapsimäärä,
    dc.amount                              AS henkilökuntaa_ryhmässä,
    cc.caretaker_count                     AS henkilökuntaa_läsnä,
    (
        SELECT SUM(caretaker_count)
        FROM caretaker_counts
        WHERE unit_id = d.id
            AND date = cc.date
    )                                      AS kasvatusvastuullisten_lkm_yksikössä,
    (
        SELECT count(*)
        FROM daycare_group_placement dgp
        WHERE dgp.daycare_group_id = dg.id
            AND dgp.start_date <= cc.date
            AND cc.date <= dgp.end_date
    )                                      AS ryhmän_lapsimäärä
FROM daycare_group dg
    JOIN daycare d ON dg.daycare_id = d.id
    JOIN care_area ca ON ca.id = d.care_area_id
    LEFT JOIN caretaker_counts cc ON cc.group_id = dg.id
    LEFT JOIN daycare_caretaker dc ON dg.id = dc.group_id
        AND dc.start_date <= cc.date
        AND (dc.end_date >= cc.date OR dc.end_date IS NULL)
    LEFT JOIN staff_attendance_aggregate saa ON dg.id = saa.group_id
WHERE (current_date - interval '3 months' <= d.closing_date OR d.closing_date IS NULL)
    AND (current_date - interval '3 months' <= dg.end_date OR dg.end_date IS NULL)
GROUP BY
    cc.date,
    d.name,
    d.id,
    dg.name,
    dg.id,
    dc.amount,
    cc.caretaker_count
ORDER BY dg.name, cc.date
"""
            )
        }

    val getDailyUnitsOccupanciesConfirmed =
        csvQuery<FabricDailyUnitsOccupanciesConfirmed> {
            sql(
                """
WITH
caretaker_counts AS (
    SELECT 
        a.id                                    AS area_id,
        a.name                                  AS area_name,
        u.id                                    AS unit_id,
        u.name                                  AS unit_name,
        current_date                            AS date,
        COALESCE(
            SUM(c.amount),
            0.0
        ) AS caretaker_count
    FROM daycare_group g
        JOIN daycare u ON g.daycare_id = u.id
            AND daterange(g.start_date, g.end_date, '[]') @> current_date
        JOIN care_area a ON a.id = u.care_area_id
        LEFT JOIN daycare_caretaker c ON g.id = c.group_id 
            AND daterange(c.start_date, c.end_date, '[]') @> current_date
    WHERE date_part('isodow', current_date) = ANY(u.operation_days)
        AND daterange(u.opening_date, u.closing_date, '[]') @> current_date
    GROUP BY a.id, u.id
),
placements AS (
    SELECT
        p.id AS placement_id,
        p.child_id,
        p.unit_id,
        p.type,
        u.type && ARRAY['FAMILY', 'GROUP_FAMILY']::care_types[] AS family_unit_placement,
        daterange(p.start_date, p.end_date, '[]') AS period
    FROM placement p
        JOIN daycare u ON p.unit_id = u.id
    WHERE daterange(p.start_date, p.end_date, '[]') @> current_date
),
default_sn_coefficients AS (
    SELECT
        occupancy_coefficient,
        occupancy_coefficient_under_3y,
        realized_occupancy_coefficient,
        realized_occupancy_coefficient_under_3y,
        valid_placement_type
    FROM service_need_option 
    WHERE default_option
),
placements_on_date AS (
    SELECT
        cc.date,
        cc.caretaker_count                          AS caretaker_count,
        pl.placement_id                             AS placement_id,
        pl.child_id                                 AS child_id,
        pl.unit_id                                  AS unit_id,
        coalesce(af.capacity_factor, 1)             AS assistance_coefficient,
        CASE
            WHEN pl.family_unit_placement = TRUE
                THEN 1.75
            WHEN cc.date < (p.date_of_birth + interval '3 year')::DATE
                THEN (
                    coalesce(sno.occupancy_coefficient_under_3y, dsc.occupancy_coefficient_under_3y)
                )
            ELSE (
                coalesce(sno.occupancy_coefficient, dsc.occupancy_coefficient)
            )
        END                                         AS service_need_coefficient
    from caretaker_counts cc
        JOIN (
            SELECT
                p.id AS placement_id,
                p.child_id,
                p.unit_id,
                p.type,
                u.type && array['FAMILY', 'GROUP_FAMILY']::care_types[] AS family_unit_placement,
                daterange(p.start_date, p.end_date, '[]') AS period
            FROM placement p
                JOIN daycare u ON p.unit_id = u.id
            WHERE daterange(p.start_date, p.end_date, '[]') @> current_date
        ) pl ON pl.unit_id = cc.unit_id
        JOIN person p ON p.id = pl.child_id
        LEFT JOIN assistance_factor af ON af.child_id = pl.child_id
            AND af.valid_during @> cc.date
        LEFT JOIN service_need sn ON sn.placement_id = pl.placement_id
            AND daterange(sn.start_date, sn.end_date, '[]') @> cc.date
        LEFT JOIN service_need_option sno ON sn.option_id = sno.id
        JOIN default_sn_coefficients dsc ON dsc.valid_placement_type = pl.type
)
SELECT
    now() AT TIME ZONE 'Europe/Helsinki'                            AS poiminta_aika,
    pod.date                                                        AS päivämäärä,
    pod.unit_id                                                     AS toimintayksikkö_id,
    d.name                                                          AS toimintayksikkö,
    d.created                                                       AS toimintayksikkö_luontiaika,
    d.updated                                                       AS toimintayksikkö_muokkausaika,
    pod.caretaker_count                                             AS kasvattajien_lkm,
    COUNT(pod.placement_id)                                         AS sijoituksien_lkm,
    SUM(pod.assistance_coefficient * pod.service_need_coefficient)  AS täyttöaste_summa,
    CASE
        WHEN pod.caretaker_count IS NULL OR pod.caretaker_count = 0
            THEN NULL
        ELSE (SUM(pod.assistance_coefficient * pod.service_need_coefficient) / (pod.caretaker_count * 7)) * 100
    END                                                             AS täyttöaste_prosentteina
FROM placements_on_date pod
    JOIN daycare d ON d.id = pod.unit_id
GROUP BY
    pod.date, pod.unit_id, d.name, d.created, d.updated, pod.caretaker_count
"""
            )
        }

    val getDailyUnitsOccupanciesRealized =
        csvQuery<FabricDailyUnitsOccupanciesRealized> {
            sql(
                """
WITH
caretaker_counts AS (
    SELECT
        a.id                            AS area_id,
        a.name                          AS area_name,
        u.id                            AS unit_id,
        u.name                          AS unit_name,
        current_date                    AS date,
        COALESCE(
            SUM(
                CASE
                    WHEN sar.arrived IS NOT NULL AND (current_date = DATE(sar.arrived) OR current_date = DATE(sar.departed))
                        THEN ROUND(EXTRACT(EPOCH FROM (
                            LEAST(sar.departed, timezone('Europe/Helsinki', (current_date + 1)::DATE::TIMESTAMP)) - GREATEST(sar.arrived, timezone('Europe/Helsinki', current_date::TIMESTAMP))
                        )) / 3600 / 7.65 * sar.occupancy_coefficient / 7, 4)
                    ELSE s.count
                END
            ),
            0.0
        )                               AS caretaker_count
    FROM daycare_group g
        JOIN daycare u ON g.daycare_id = u.id
            AND daterange(g.start_date, g.end_date, '[]') @> current_date
        JOIN care_area a ON a.id = u.care_area_id
        LEFT JOIN (
            SELECT group_id, arrived, departed, occupancy_coefficient
            FROM staff_attendance_realtime
            WHERE departed IS NOT NULL
                AND type = ANY('{PRESENT,OVERTIME,JUSTIFIED_CHANGE}'::staff_attendance_type[])
            UNION ALL
            SELECT group_id, arrived, departed, occupancy_coefficient
            FROM staff_attendance_external
            WHERE departed IS NOT null
        ) sar ON g.id = sar.group_id
        LEFT JOIN staff_attendance s ON g.id = s.group_id
            AND current_date = s.date
    WHERE date_part('isodow', current_date) = ANY(u.operation_days)
        AND daterange(u.opening_date, u.closing_date, '[]') @> current_date
    GROUP BY a.id, u.id
),
default_sn_coefficients AS (
    SELECT
        occupancy_coefficient,
        occupancy_coefficient_under_3y,
        realized_occupancy_coefficient,
        realized_occupancy_coefficient_under_3y,
        valid_placement_type
    FROM service_need_option
    WHERE default_option
),
placements_realized AS (
    SELECT
        bc.id                                                   AS placement_id,
        bc.child_id,
        bc.unit_id,
        pl.type,
        u.type && array['FAMILY', 'GROUP_FAMILY']::care_types[] AS family_unit_placement,
        daterange(bc.start_date, bc.end_date, '[]')             AS period
    FROM backup_care bc
        JOIN daycare u ON bc.unit_id = u.id
        JOIN placement pl ON bc.child_id = pl.child_id
            AND daterange(bc.start_date, bc.end_date, '[]') && daterange(pl.start_date, pl.end_date, '[]')
    WHERE daterange(bc.start_date, bc.end_date, '[]') @> current_date
        AND NOT EXISTS (
            SELECT *
            FROM absence a
            WHERE a.child_id = pl.child_id
                AND current_date = a.date
                AND ARRAY[a.category] && absence_categories(pl.type)
        )
    UNION
    SELECT
        pl.id                                                   AS placement_id,
        pl.child_id,
        pl.unit_id,
        pl.type,
        u.type && ARRAY['FAMILY', 'GROUP_FAMILY']::care_types[] AS family_unit_placement,
        daterange(pl.start_date, pl.end_date, '[]')             AS period
    FROM placement pl
        JOIN daycare u ON pl.unit_id = u.id
    where daterange(pl.start_date, pl.end_date, '[]') @> current_date
        AND NOT EXISTS(
            SELECT *
            FROM backup_care bc
            WHERE daterange(bc.start_date, bc.end_date, '[]') @> current_date
                AND daterange(bc.start_date, bc.end_date, '[]') && daterange(pl.start_date, pl.end_date, '[]')
                AND bc.child_id = pl.child_id
        )
        AND NOT EXISTS (
            SELECT id, child_id, date, absence_type, modified_at, modified_by, category, questionnaire_id
            FROM absence a
            WHERE a.child_id = pl.child_id
                AND current_date = a.date
                AND ARRAY[a.category] && absence_categories(pl.type)
        )
),
placements_on_date AS (
    SELECT
        cc.date,
        cc.caretaker_count                                  AS caretaker_count,
        pr.placement_id                                     AS placement_id,
        pr.child_id                                         AS child_id,
        pr.unit_id                                          AS unit_id,
        coalesce(af.capacity_factor, 1)                     AS assistance_coefficient,
        CASE
            WHEN pr.family_unit_placement = TRUE
                THEN 1.75
            WHEN cc.date < (p.date_of_birth + INTERVAL '3 year')::DATE
                THEN (
                    coalesce(sno.realized_occupancy_coefficient_under_3y, dsc.realized_occupancy_coefficient_under_3y)
                )
            ELSE (
                coalesce(sno.realized_occupancy_coefficient, dsc.realized_occupancy_coefficient)
            )
        END AS service_need_coefficient
    FROM caretaker_counts cc
        JOIN placements_realized pr ON pr.unit_id = cc.unit_id
        JOIN person p ON p.id = pr.child_id
        LEFT JOIN assistance_factor af ON af.child_id = pr.child_id
            AND af.valid_during @> cc.date
        LEFT JOIN service_need sn ON sn.placement_id = pr.placement_id
            AND daterange(sn.start_date, sn.end_date, '[]') @> cc.date
        LEFT JOIN service_need_option sno ON sn.option_id = sno.id
        JOIN default_sn_coefficients dsc ON dsc.valid_placement_type = pr.type
)
SELECT
    now() AT TIME ZONE 'Europe/Helsinki'                            AS poiminta_aika,
    pod.date                                                        AS päivämäärä,
    pod.unit_id                                                     AS toimintayksikkö_id,
    d.name                                                          AS toimintayksikkö,
    d.created                                                       AS toimintayksikkö_luontiaika,
    d.updated                                                       AS toimintayksikkö_muokkausaika,
    pod.caretaker_count                                             AS kasvattajien_lkm,
    count(pod.placement_id)                                         AS sijoituksien_lkm,
    SUM(pod.assistance_coefficient * pod.service_need_coefficient)  AS käyttöaste_summa,
    CASE
        WHEN pod.caretaker_count IS NULL OR pod.caretaker_count = 0
            THEN null
        ELSE (SUM(pod.assistance_coefficient * pod.service_need_coefficient) / (pod.caretaker_count * 7)) * 100
    END                                                             AS käyttöaste_prosentteina
FROM placements_on_date pod
    JOIN daycare d ON d.id = pod.unit_id
GROUP BY
    pod.date, pod.unit_id, d.name, d.created, d.updated, pod.caretaker_count
"""
            )
        }

    fun getDaycareAssistance(history: Boolean = false) =
        csvQuery<FabricDaycareAssistance> {
            sql(
                """
SELECT
    now() AT TIME ZONE 'Europe/Helsinki'    AS poiminta_aika,
    id                                      AS tuentarve_id,
    created                                 AS luontiaika,
    modified                                AS muokkausaika,
    child_id                                AS lapsen_id,
    level                                   AS tuentarve_varhaiskasvatuksessa,
    lower(valid_during)                     AS aloitus_pvm,
    upper(valid_during)                     AS loppu_pvm
FROM daycare_assistance
WHERE CASE WHEN $history = FALSE THEN current_date - INTERVAL '3 months' <= modified ELSE TRUE END
"""
            )
        }

    fun getFeeDecisions(history: Boolean = false) =
        csvQuery<FabricFeeDecision> {
            sql(
                """
SELECT
    now() AT TIME ZONE 'Europe/Helsinki'    AS poiminta_aika,
    fd.decision_number                      AS maksupäätöksen_numero,
    fd.id                                   AS maksupäätös_id,
    fd.created                              AS luontiaika,
    fd.updated                              AS muokkausaika,
    lower(fd.valid_during)                  AS alkupvm,
    upper(fd.valid_during)                  AS loppupvm,
    fd.decision_type                        AS huojennustyyppi,
    fd.family_size                          AS perhekoko,
    fd.total_fee                            AS kokonaismaksu,
    fd.status                               AS tila,
    fdc.child_id                            AS lapsi_id,
    fdc.final_fee                           AS lapsikohtainen_maksu,
    fdc.placement_type                      AS toimintamuoto,
    ca.name                                 AS palvelualue,
    ca.id                                   AS palvelualue_id,
    d.name                                  AS toimipaikka,
    d.id                                    AS toimipaikka_id,
    d.dw_cost_center                        AS kustannuspaikka
FROM fee_decision fd
    JOIN fee_decision_child fdc on fd.id = fdc.fee_decision_id
    JOIN daycare d ON fdc.placement_unit_id = d.id
    JOIN care_area ca ON d.care_area_id = ca.id
WHERE fd.decision_number IS NOT NULL -- ei tuoda effican päätöksiä
    AND CASE WHEN $history = FALSE THEN current_date - INTERVAL '3 months' <= fd.updated ELSE TRUE END
"""
            )
        }

    fun getOtherAssistanceMeasures(history: Boolean = false) =
        csvQuery<FabricOtherAssistanceMeasure> {
            sql(
                """
SELECT
    now() AT TIME ZONE 'Europe/Helsinki'    AS poiminta_aika,
    id                                      AS muu_toimi_id,
    created                                 AS luontiaika,
    updated                                 AS muokkausaika,
    child_id                                AS lapsen_id,
    type                                    AS muu_toimi,
    lower(valid_during)                     AS aloitus_pvm,
    upper(valid_during)                     AS loppu_pvm
FROM other_assistance_measure
WHERE CASE WHEN $history = FALSE THEN current_date - INTERVAL '3 months' <= updated ELSE TRUE END
"""
            )
        }

    fun getPlacements(history: Boolean = false) =
        csvQuery<FabricPlacement> {
            sql(
                """
SELECT
    now() AT TIME ZONE 'Europe/Helsinki'    AS poiminta_aika,
    p.id                                    AS lapsen_id,
    d.name                                  AS toimintayksikkö,
    d.id                                    AS toimintayksikkö_id,
    pl.id                                   AS sijoituksen_id,
    pl.created_at                           AS sijoituksen_luontiaika,
    pl.modified_at                          AS sijoituksen_muokkausaika,
    pl.start_date                           AS sijoituksen_alkupvm,
    pl.end_date                             AS sijoituksen_loppupvm,
    pl.type                                 AS sijoitustyyppi,
    dg.name                                 AS ryhmä,
    dg.id                                   AS ryhmä_id,
    dg.created_at                           AS ryhmän_luontiaika,
    dg.updated_at                           AS ryhmän_muokkausaika,
    dg.start_date                           AS ryhmän_alkupvm,
    dg.end_date                             AS ryhmän_loppupvm,
    sno.name_fi                             AS palveluntarve,
    sno.id                                  AS palveluntarve_id,
    sn.created                              AS palveluntarpeen_luontiaika,
    sn.updated                              AS palveluntarpeen_muokkausaika,
    sn.start_date                           AS palveluntarpeen_alkupvm,
    sn.end_date                             AS palveluntarpeen_loppupvm
FROM person p
    JOIN placement pl ON pl.child_id = p.id
    JOIN daycare d ON pl.unit_id = d.id
    JOIN daycare_group_placement dgp ON pl.id = dgp.daycare_placement_id
    JOIN daycare_group dg ON d.id = dg.daycare_id
        AND dgp.daycare_group_id = dg.id
    LEFT JOIN service_need sn ON pl.id = sn.placement_id
    LEFT JOIN service_need_option sno ON sn.option_id = sno.id
WHERE CASE WHEN $history = FALSE THEN current_date - INTERVAL '3 months' <= pl.modified_at ELSE TRUE END
"""
            )
        }

    fun getPreschoolAssistance(history: Boolean = false) =
        csvQuery<FabricPreschoolAssistance> {
            sql(
                """
SELECT
    now() AT TIME ZONE 'Europe/Helsinki'    AS poiminta_aika,
    id                                      AS tuentarve_id,
    created                                 AS luontiaika,
    modified                                AS muokkausaika,
    child_id                                AS lapsen_id,
    level                                   AS tuentarve_esiopetuksessa,
    lower(valid_during)                     AS aloitus_pvm,
    upper(valid_during)                     AS loppu_pvm
FROM preschool_assistance
WHERE CASE WHEN $history = FALSE THEN current_date - INTERVAL '3 months' <= modified ELSE TRUE END
"""
            )
        }

    fun getUnitsAndGroups(history: Boolean = false) =
        csvQuery<FabricUnitAndGroup> {
            sql(
                """
SELECT
    now() AT TIME ZONE 'Europe/Helsinki'    AS poiminta_aika,
    d.name                                  AS toimintayksikkö,
    d.id                                    AS toimintayksikkö_id,
    d.created                               AS toimintayksikkö_luontiaika,
    d.updated                               AS toimintayksikkö_muokkausaika,
    d.opening_date                          AS toimintayksikön_alkupvm,
    d.closing_date                          AS toimintayksikön_loppupvm,
    d.type                                  AS toimintamuoto,
    d.provider_type                         AS järjestämistapa,
    d.street_address                        AS katuosoite,
    d.postal_code                           AS postinumero,
    d.post_office                           AS postitoimipaikka,
    d.capacity                              AS toimintayksikön_laskennallinen_lapsimäärä,
    ca.name                                 AS palvelualue,
    ca.id                                   AS palvelualue_id,
    d.dw_cost_center                        AS dw_kustannuspaikka,
    dg.name                                 AS ryhmä,
    dg.id                                   AS ryhmä_id,
    dg.created_at                           AS ryhmä_luontiaika,
    dg.updated_at                           AS ryhmä_muokkausaika,
    dg.start_date                           AS ryhmän_alkupvm,
    dg.end_date                             AS ryhmän_loppupvm
FROM daycare_group dg
    JOIN daycare d ON dg.daycare_id = d.id
    JOIN care_area ca ON ca.id = d.care_area_id
WHERE CASE WHEN $history = FALSE THEN
    (current_date - INTERVAL '3 months' <= d.updated)
    OR (current_date - INTERVAL '3 months' <= dg.updated_at) -- ???
ELSE TRUE END
GROUP BY
    d.name,
    d.id,
    d.created,
    d.updated,
    d.opening_date,
    d.closing_date,
    d.type,
    d.provider_type,
    ca.name,
    ca.id,
    d.dw_cost_center,
    dg.name,
    dg.id,
    dg.created_at,
    dg.updated_at,
    dg.start_date,
    dg.end_date
ORDER BY d.name
"""
            )
        }

    fun getVoucherValueDecisions(history: Boolean = false) =
        csvQuery<FabricVoucherValueDecision> {
            sql(
                """
SELECT
    now() AT TIME ZONE 'Europe/Helsinki'    AS poiminta_aika,
    vvd.id                                  AS arvopäätöksen_id,
    vvd.created                             AS luontiaika,
    vvd.updated                             AS muokkausaika,
    vvd.decision_number                     AS arvopäätöksen_numero,
    vvd.valid_from                          AS alkupvm,
    vvd.valid_to                            AS loppupvm,
    vvd.decision_type                       AS huojennustyyppi,
    vvd.family_size                         AS perhekoko,
    vvd.voucher_value                       AS palvelusetelin_arvo,
    vvd.final_co_payment                    AS omavastuuosuus,
    vvd.child_id                            AS lapsen_id,
    vvd.placement_type                      AS toimintamuoto,
    vvd.status                              AS tila,
    ca.name                                 AS palvelualue,
    ca.id                                   AS palvelualue_id,
    d.name                                  AS toimipaikka,
    d.id                                    AS toimipaikka_id
FROM voucher_value_decision vvd
    JOIN daycare d ON vvd.placement_unit_id = d.id
    JOIN care_area ca ON d.care_area_id = ca.id
WHERE vvd.decision_number IS NOT NULL -- ei tuoda effican päätöksiä
    AND CASE WHEN $history = FALSE THEN current_date - INTERVAL '3 months' <= vvd.updated ELSE TRUE END
"""
            )
        }
}

interface CsvQuery {
    operator fun <R> invoke(tx: Database.Read, useResults: (records: Sequence<String>) -> R): R
}

class StreamingCsvQuery<T : Any>(
    private val clazz: KClass<T>,
    private val query: (Database.Read) -> Database.Result<T>,
) : CsvQuery {
    override operator fun <R> invoke(
        tx: Database.Read,
        useResults: (records: Sequence<String>) -> R,
    ): R = query(tx).useSequence { rows -> useResults(toCsvRecords(::convertToCsv, clazz, rows)) }
}

private const val QUERY_STREAM_CHUNK_SIZE = 10_000

private inline fun <reified T : Any> csvQuery(
    crossinline f: QuerySql.Builder.() -> QuerySql
): CsvQuery =
    StreamingCsvQuery(T::class) { tx ->
        tx.createQuery { f() }.setFetchSize(QUERY_STREAM_CHUNK_SIZE).mapTo<T>()
    }
