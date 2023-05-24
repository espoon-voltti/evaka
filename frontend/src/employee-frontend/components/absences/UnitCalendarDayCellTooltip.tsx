// SPDX-FileCopyrightText: 2017-2023 City of Espoo
//
// SPDX-License-Identifier: LGPL-2.1-or-later

import React, { useMemo } from 'react'

import {
  AbsenceWithModifierInfo,
  ChildReservation
} from 'lib-common/generated/api-types/daycare'
import { HelsinkiDateTimeRange } from 'lib-common/generated/api-types/shared'
import LocalDate from 'lib-common/local-date'

import { useTranslation } from '../../state/i18n'

interface UnitCalendarMonthlyDayCellTooltipProps {
  date: LocalDate
  absences: AbsenceWithModifierInfo[]
  dailyServiceTimes: HelsinkiDateTimeRange[]
  reservations: ChildReservation[]
  backupCare: boolean
  isMissingHolidayReservation: boolean
}

export default React.memo(function UnitCalendarMonthlyDayCellTooltip({
  date,
  absences,
  dailyServiceTimes,
  reservations,
  backupCare,
  isMissingHolidayReservation
}: UnitCalendarMonthlyDayCellTooltipProps): JSX.Element {
  const { i18n } = useTranslation()

  const dailyServiceTimeTooltip = useMemo(
    () =>
      dailyServiceTimes.map((dst, index) => (
        <div key={index}>
          {`${i18n.absences.dailyServiceTime} ${dst.start
            .toLocalTime()
            .format()} - ${dst.end.toLocalTime().format()}`}
        </div>
      )),
    [i18n, dailyServiceTimes]
  )

  const reservationTooltip = useMemo(
    () =>
      reservations.map((res, index) => {
        const reservationText =
          res.reservation.type === 'TIMES'
            ? `${
                i18n.absences.reservation
              } ${res.reservation.startTime.format()} - ${res.reservation.endTime.format()}`
            : i18n.absences.present
        const userTypeText =
          res.createdByEvakaUserType === 'CITIZEN'
            ? i18n.absences.guardian
            : i18n.absences.staff
        return (
          <div key={index}>
            {reservationText}
            <br />
            {res.created.toLocalDate().format()} {userTypeText}
          </div>
        )
      }),
    [i18n, reservations]
  )

  const missingHolidayReservationTooltip = useMemo(
    () => (
      <div>
        {i18n.absences.missingHolidayReservation}
        {dailyServiceTimeTooltip.length > 0 && (
          <div>
            <br />
            {dailyServiceTimeTooltip}
          </div>
        )}
      </div>
    ),
    [i18n, dailyServiceTimeTooltip]
  )

  const backupCareTooltip = (
    <div>{i18n.absences.absenceTypes['TEMPORARY_RELOCATION']}</div>
  )

  const absencesTooltip = useMemo(
    () =>
      absences.map(
        ({ category, absenceType, modifiedAt, modifiedByType }, index) => (
          <div key={index}>
            {index !== 0 && <br />}
            {`${i18n.absences.absenceCategories[category]}: ${i18n.absences.absenceTypes[absenceType]}`}
            <br />
            {`${modifiedAt.toLocalDate().format()} ${
              i18n.absences.modifiedByType[modifiedByType]
            }`}
          </div>
        )
      ),
    [i18n, absences]
  )

  return (
    <div data-qa={`attendance-tooltip-${date.toString()}`}>
      {backupCare ? (
        backupCareTooltip
      ) : absences.length > 0 ? (
        absencesTooltip
      ) : isMissingHolidayReservation ? (
        missingHolidayReservationTooltip
      ) : reservations.length > 0 || dailyServiceTimes.length > 0 ? (
        <div>
          {reservationTooltip}
          {reservationTooltip.length > 0 && <br />}
          {dailyServiceTimeTooltip}
        </div>
      ) : undefined}
    </div>
  )
})
