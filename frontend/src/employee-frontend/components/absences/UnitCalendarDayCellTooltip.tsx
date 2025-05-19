// SPDX-FileCopyrightText: 2017-2023 City of Espoo
//
// SPDX-License-Identifier: LGPL-2.1-or-later

import React, { useMemo } from 'react'

import type {
  AbsenceWithModifierInfo,
  ChildReservation
} from 'lib-common/generated/api-types/absence'
import type { ServiceTimesPresenceStatus } from 'lib-common/generated/api-types/dailyservicetimes'
import type LocalDate from 'lib-common/local-date'

import { useTranslation } from '../../state/i18n'

interface UnitCalendarMonthlyDayCellTooltipProps {
  date: LocalDate
  absences: AbsenceWithModifierInfo[]
  dailyServiceTimes: ServiceTimesPresenceStatus
  reservations: ChildReservation[]
  backupCare: boolean
  isMissingHolidayReservation: boolean
  requiresBackupCare: boolean
}

export default React.memo(function UnitCalendarMonthlyDayCellTooltip({
  date,
  absences,
  dailyServiceTimes,
  reservations,
  backupCare,
  isMissingHolidayReservation,
  requiresBackupCare
}: UnitCalendarMonthlyDayCellTooltipProps) {
  const { i18n } = useTranslation()

  const dailyServiceTimeTooltip = useMemo(
    () =>
      dailyServiceTimes.type === 'PRESENT' ? (
        <div>
          {`${i18n.absences.dailyServiceTime} ${dailyServiceTimes.times.format()}`}
        </div>
      ) : undefined,
    [i18n, dailyServiceTimes]
  )

  const reservationTooltip = useMemo(
    () =>
      reservations.map((res, index) => {
        const reservationText =
          res.reservation.type === 'TIMES'
            ? `${i18n.absences.reservation} ${res.reservation.range.format()}`
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
        {dailyServiceTimeTooltip !== undefined ? (
          <div>
            <br />
            {dailyServiceTimeTooltip}
          </div>
        ) : undefined}
      </div>
    ),
    [i18n, dailyServiceTimeTooltip]
  )

  const backupCareTooltip = (
    <div>{i18n.absences.absenceTypes.TEMPORARY_RELOCATION}</div>
  )

  const absencesTooltip = useMemo(
    () =>
      absences.map(
        ({ category, absenceType, modifiedAt, modifiedByStaff }, index) => (
          <div key={index}>
            {index !== 0 && <br />}
            {`${i18n.absences.absenceCategories[category]}: ${i18n.absences.absenceTypes[absenceType]}`}
            <br />
            {`${modifiedAt.toLocalDate().format()} ${
              modifiedByStaff
                ? i18n.absences.modifiedByStaff
                : i18n.absences.modifiedByCitizen
            }`}
          </div>
        )
      ),
    [i18n, absences]
  )

  const requiresBackupCareTooltip = useMemo(
    () => (
      <div>
        {i18n.absences.shiftCare}
        <br />
        {i18n.absences.requiresBackupCare}
      </div>
    ),
    [i18n]
  )

  return (
    <div data-qa={`attendance-tooltip-${date.toString()}`}>
      {backupCare ? (
        backupCareTooltip
      ) : absences.length > 0 ? (
        absencesTooltip
      ) : isMissingHolidayReservation ? (
        missingHolidayReservationTooltip
      ) : requiresBackupCare ? (
        requiresBackupCareTooltip
      ) : reservations.length > 0 || dailyServiceTimes !== null ? (
        <div>
          {reservationTooltip}
          {reservationTooltip.length > 0 && <br />}
          {dailyServiceTimeTooltip}
        </div>
      ) : undefined}
    </div>
  )
})
