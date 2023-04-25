// SPDX-FileCopyrightText: 2017-2022 City of Espoo
//
// SPDX-License-Identifier: LGPL-2.1-or-later

import groupBy from 'lodash/groupBy'
import partition from 'lodash/partition'
import React, { useMemo } from 'react'
import styled from 'styled-components'

import type {
  ReservationResponseDay,
  ReservationResponseDayChild
} from 'lib-common/generated/api-types/reservations'
import {
  reservationHasTimes,
  reservationsAndAttendancesDiffer
} from 'lib-common/reservations'
import type { UUID } from 'lib-common/types'
import StatusIcon from 'lib-components/atoms/StatusIcon'
import {
  FixedSpaceColumn,
  FixedSpaceRow
} from 'lib-components/layout/flex-helpers'
import { Light } from 'lib-components/typography'

import { useTranslation } from '../localization'

import type { ChildImageData } from './RoundChildImages'
import RoundChildImages from './RoundChildImages'

type DailyChildGroupElementType =
  | 'attendance'
  | 'reservation-no-times'
  | 'reservation'
  | 'absent'
  | 'missing-reservation'
  | 'absent-free'

interface DailyChildGroupElement {
  type: DailyChildGroupElementType
  text?: string
  childId: UUID
}

interface GroupedDailyChildren {
  type: DailyChildGroupElementType
  text?: string
  childIds: UUID[]
  key: string
}

export const Reservations = React.memo(function Reservations({
  data,
  childImages,
  isReservable
}: {
  data: ReservationResponseDay
  childImages: ChildImageData[]
  isReservable: boolean
}) {
  const i18n = useTranslation()
  const showAttendanceWarning = data.children.some(
    ({ reservations, attendances }) =>
      reservationsAndAttendancesDiffer(reservations, attendances)
  )

  const groupedChildren = useMemo(
    () => groupChildren(data.children),
    [data.children]
  )

  return data.children.length === 0 && data.holiday && !isReservable ? (
    <Holiday />
  ) : (
    <div>
      <FixedSpaceColumn spacing="xs">
        {groupedChildren.map((group) => (
          <FixedSpaceRow
            key={group.key}
            spacing="s"
            alignItems="center"
            data-qa="reservation-group"
          >
            <RoundChildImages
              images={childImages.filter((image) =>
                group.childIds.includes(image.childId)
              )}
            />
            <GroupedElementText
              className={group.type}
              data-qa="reservation-text"
            >
              {group.type === 'reservation-no-times'
                ? i18n.calendar.reservationNoTimes
                : group.type === 'missing-reservation'
                ? i18n.calendar.noReservation
                : group.type === 'absent'
                ? i18n.calendar.absent
                : group.type === 'absent-free'
                ? i18n.calendar.absentFree
                : group.text}
            </GroupedElementText>
          </FixedSpaceRow>
        ))}
      </FixedSpaceColumn>
      {showAttendanceWarning && <StatusIcon status="warning" />}
    </div>
  )
})

export const Holiday = React.memo(function Holiday() {
  const i18n = useTranslation()
  return <Light>{i18n.calendar.holiday}</Light>
})

const GroupedElementText = styled.div`
  word-break: break-word;

  &.missing-reservation,
  &.reservation-no-times {
    color: ${(p) => p.theme.colors.accents.a2orangeDark};
  }
`

const groupChildren = (relevantChildren: ReservationResponseDayChild[]) =>
  Object.entries(
    groupBy(
      relevantChildren.map((child): DailyChildGroupElement => {
        if (child.attendances.length > 0) {
          return {
            childId: child.childId,
            type: 'attendance',
            text: child.attendances
              .map(
                ({ startTime, endTime }) =>
                  `${startTime.format()}–${endTime?.format() ?? ''}`
              )
              .join(', ')
          }
        }

        if (child.absence) {
          return {
            childId: child.childId,
            type:
              child.absence.type === 'FREE_ABSENCE' ? 'absent-free' : 'absent'
          }
        }

        if (child.reservations.length > 0) {
          const [withTimes, withoutTimes] = partition(
            child.reservations,
            reservationHasTimes
          )

          if (withoutTimes.length > 0) {
            // In theory, we could have reservations with and without times, but in practice this shouldn't happen
            return {
              childId: child.childId,
              type: 'reservation-no-times'
            }
          }

          return {
            childId: child.childId,
            type: 'reservation',
            text: withTimes
              .map(
                ({ startTime, endTime }) =>
                  `${startTime.format()}–${endTime.format()}`
              )
              .join(', ')
          }
        }

        return {
          childId: child.childId,
          type: 'missing-reservation'
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
