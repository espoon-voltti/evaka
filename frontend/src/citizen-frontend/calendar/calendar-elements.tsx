// SPDX-FileCopyrightText: 2017-2022 City of Espoo
//
// SPDX-License-Identifier: LGPL-2.1-or-later

import groupBy from 'lodash/groupBy'
import partition from 'lodash/partition'
import React, { useMemo } from 'react'
import styled, { css } from 'styled-components'

import { mapScheduleType } from 'lib-common/api-types/placement'
import type {
  AbsenceInfo,
  ReservableTimeRange,
  Reservation,
  ReservationResponseDay,
  ReservationResponseDayChild
} from 'lib-common/generated/api-types/reservations'
import type { ChildId } from 'lib-common/generated/api-types/shared'
import {
  reservationHasTimes,
  reservationsAndAttendancesDiffer
} from 'lib-common/reservations'
import StatusIcon from 'lib-components/atoms/StatusIcon'
import Tooltip from 'lib-components/atoms/Tooltip'
import {
  FixedSpaceColumn,
  FixedSpaceRow
} from 'lib-components/layout/flex-helpers'
import { Italic } from 'lib-components/typography'
import { featureFlags } from 'lib-customizations/citizen'

import type { Translations } from '../localization'
import { useTranslation } from '../localization'

import type { ChildImageData } from './RoundChildImages'
import RoundChildImages from './RoundChildImages'

export const Reservations = React.memo(function Reservations({
  data,
  childImages,
  backgroundHighlight
}: {
  data: ReservationResponseDay
  childImages: ChildImageData[]
  backgroundHighlight: BackgroundHighlightType
}) {
  const i18n = useTranslation()
  const showAttendanceWarning = data.children.some(
    ({ reservations, attendances, usedService }) =>
      (usedService != null &&
        usedService.usedServiceMinutes > usedService.reservedMinutes) ||
      reservationsAndAttendancesDiffer(reservations, attendances)
  )

  const groupedChildren = useMemo(
    () =>
      groupChildren({
        children: data.children,
        i18n
      }),
    [data.children, i18n]
  )

  return data.children.length === 0 && data.holiday ? (
    <Holiday />
  ) : (
    <div>
      <FixedSpaceColumn spacing="xs">
        {groupedChildren.map((group) => {
          const wordCount = group.text?.split(' ').length ?? 0
          return (
            <FixedSpaceRow
              key={group.key}
              spacing="xs"
              alignItems="center"
              data-qa="reservation-group"
            >
              <RoundChildImages
                images={childImages.filter((image) =>
                  group.childIds.includes(image.childId)
                )}
              />
              {group.childIds.length === 1 && wordCount > 2 ? (
                <Tooltip tooltip={group.text}>
                  <GroupedElementText
                    $type={group.type}
                    $backgroundHighlight={backgroundHighlight}
                    $clamp={true}
                    data-qa="reservation-text"
                  >
                    {group.text}
                  </GroupedElementText>
                </Tooltip>
              ) : (
                <GroupedElementText
                  $type={group.type}
                  $backgroundHighlight={backgroundHighlight}
                  data-qa="reservation-text"
                >
                  {group.text}
                </GroupedElementText>
              )}
            </FixedSpaceRow>
          )
        })}
      </FixedSpaceColumn>
      {showAttendanceWarning && <StatusIcon status="warning" />}
    </div>
  )
})

export const Holiday = React.memo(function Holiday() {
  const i18n = useTranslation()
  return <Italic data-qa="holiday">{i18n.calendar.holiday}</Italic>
})

const GroupedElementText = styled.div<{
  $type: DailyChildGroupElementType
  $backgroundHighlight: BackgroundHighlightType
  $clamp?: boolean
}>`
  word-break: break-word;
  white-space: pre-wrap;
  ${(p) =>
    p.$clamp &&
    css`
      display: -webkit-box;
      -webkit-line-clamp: 2;
      -webkit-box-orient: vertical;
      overflow: hidden;
    `}

  ${(p) =>
    p.$type === 'missingReservation' &&
    p.$backgroundHighlight !== 'holidayPeriod' &&
    p.$backgroundHighlight !== 'past'
      ? `color: ${p.theme.colors.accents.a2orangeDark};`
      : undefined}
`

type DailyChildGroupElementType =
  | 'attendance'
  | 'reservation'
  | 'present'
  | 'absent'
  | 'notYetReservable'
  | 'missingReservation'
  | 'absentFree'
  | 'absentPlanned'
  | 'absentUnknown'

export type BackgroundHighlightType =
  | 'nonEditableAbsence'
  | 'holidayPeriod'
  | 'holiday'
  | 'past'
  | undefined

interface DailyChildGroupElement {
  type: DailyChildGroupElementType
  text: string
  childId: ChildId
}

interface GroupedDailyChildren {
  type: DailyChildGroupElementType
  text: string
  childIds: ChildId[]
  key: string
}

const absenceElementType = (absence: AbsenceInfo) =>
  absence.type === 'FREE_ABSENCE'
    ? 'absentFree'
    : absence.type === 'UNKNOWN_ABSENCE'
      ? 'absentUnknown'
      : featureFlags.citizenAttendanceSummary &&
          absence.type === 'PLANNED_ABSENCE'
        ? 'absentPlanned'
        : 'absent'

const groupChildren = ({
  children,
  i18n
}: {
  children: ReservationResponseDayChild[]
  i18n: Translations
}): GroupedDailyChildren[] =>
  Object.entries(
    groupBy(
      children.map((child): DailyChildGroupElement => {
        if (child.holidayPeriodEffect?.type === 'NotYetReservable') {
          return {
            childId: child.childId,
            type: 'notYetReservable',
            text: i18n.calendar.reservationsOpenOn(
              child.holidayPeriodEffect.reservationsOpenOn
            )
          }
        }

        const [withTimes, withoutTimes] = partition(
          child.reservations,
          reservationHasTimes
        )

        const reservationsWithTimesText = withTimes
          .map((reservation) =>
            formatReservation(reservation, child.reservableTimeRange, i18n)
          )
          .join(', ')

        if (child.attendances.length > 0) {
          const attendanceText = child.attendances
            .map((a) => a.format())
            .join(', ')
          const attendancesWithReservationsText =
            attendanceText +
            (reservationsWithTimesText.length > 0
              ? `\n(${reservationsWithTimesText})`
              : '')
          return {
            childId: child.childId,
            type: 'attendance',
            text: attendancesWithReservationsText
          }
        }

        if (child.absence) {
          const elementType = absenceElementType(child.absence)
          return {
            childId: child.childId,
            type: elementType,
            text: i18n.calendar[elementType]
          }
        }

        if (child.reservations.length > 0) {
          if (withoutTimes.length > 0) {
            // In theory, we could have reservations with and without times, but in practice this shouldn't happen
            return {
              childId: child.childId,
              type: 'present',
              text: i18n.calendar.present
            }
          }

          return {
            childId: child.childId,
            type: 'reservation',
            text: reservationsWithTimesText
          }
        }

        const elementType = mapScheduleType(child.scheduleType, {
          RESERVATION_REQUIRED: () => 'missingReservation' as const,
          FIXED_SCHEDULE: () => 'present' as const,
          TERM_BREAK: () => 'absent' as const
        })
        return {
          childId: child.childId,
          type: elementType,
          text: i18n.calendar[elementType]
        }
      }),
      ({ type, text }) => `${type},${text ?? ''}`
    )
  ).map(
    ([key, children]): GroupedDailyChildren => ({
      type: children[0].type,
      text: children[0].text,
      childIds: children.map(({ childId }) => childId),
      key
    })
  )

export const formatReservation = (
  reservation: Reservation.Times,
  reservableTimeRange: ReservableTimeRange,
  i18n: Translations
) => {
  const timeOutput = reservation.range.format()

  if (!featureFlags.intermittentShiftCare) {
    return timeOutput
  } else {
    const showIntermittentShiftCareNotice =
      reservableTimeRange.type === 'INTERMITTENT_SHIFT_CARE' &&
      (reservableTimeRange.placementUnitOperationTime === null ||
        !reservableTimeRange.placementUnitOperationTime.contains(
          reservation.range
        ))

    return showIntermittentShiftCareNotice
      ? `${timeOutput} ${i18n.calendar.intermittentShiftCareNotification}`
      : timeOutput
  }
}
