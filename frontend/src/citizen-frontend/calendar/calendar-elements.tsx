// SPDX-FileCopyrightText: 2017-2022 City of Espoo
//
// SPDX-License-Identifier: LGPL-2.1-or-later

import groupBy from 'lodash/groupBy'
import partition from 'lodash/partition'
import React, { useMemo } from 'react'
import styled from 'styled-components'

import {
  ChildDailyData,
  DailyReservationData,
  ReservationChild
} from 'lib-common/generated/api-types/reservations'
import LocalDate from 'lib-common/local-date'
import {
  reservationHasTimes,
  reservationsAndAttendancesDiffer
} from 'lib-common/reservations'
import { UUID } from 'lib-common/types'
import StatusIcon from 'lib-components/atoms/StatusIcon'
import {
  FixedSpaceColumn,
  FixedSpaceRow
} from 'lib-components/layout/flex-helpers'
import { Light } from 'lib-components/typography'

import { useTranslation } from '../localization'

import RoundChildImages, { ChildImageData } from './RoundChildImages'

type DailyChildGroupElementType =
  | 'attendance'
  | 'reservation-no-times'
  | 'reservation'
  | 'absent'
  | 'missing-reservation'
  | 'absent-free'
  | 'no-service-need'

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
  allChildren,
  childImages,
  isReservable
}: {
  data: DailyReservationData
  allChildren: ReservationChild[]
  childImages: ChildImageData[]
  isReservable: boolean
}) {
  const i18n = useTranslation()
  const showAttendanceWarning = data.children.some(
    ({ reservations, attendances }) =>
      reservationsAndAttendancesDiffer(reservations, attendances)
  )

  const groupedChildren = useMemo(
    () => groupChildren(allChildren, data.children, data.date, data.isHoliday),
    [data.children, allChildren, data.date, data.isHoliday]
  )

  return data.children.length === 0 && data.isHoliday && !isReservable ? (
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
                : group.type === 'no-service-need'
                ? ''
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

const groupChildren = (
  allChildren: ReservationChild[],
  reservedChildren: ChildDailyData[],
  date: LocalDate,
  isHoliday: boolean
) =>
  Object.entries(
    groupBy(
      allChildren
        .filter((childInfo) =>
          childInfo.placements.some((placement) =>
            date.isBetween(placement.start, placement.end)
          )
        )
        .filter((childInfo) =>
          childInfo.maxOperationalDays.includes(date.getIsoDayOfWeek())
        )
        .filter(
          (childInfo) => !isHoliday || childInfo.maxOperationalDays.length == 7
        )
        .map<DailyChildGroupElement>((childInfo) => {
          const child = reservedChildren.find(
            ({ childId }) => childId === childInfo.id
          )

          if (!child) {
            if (!childInfo.serviceNeeds.some((sn) => sn.includes(date))) {
              return {
                childId: childInfo.id,
                type: 'no-service-need'
              }
            }

            return {
              childId: childInfo.id,
              type: 'missing-reservation'
            }
          }

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
              type: child.absence === 'FREE_ABSENCE' ? 'absent-free' : 'absent'
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
      ({ type, text }) => JSON.stringify([type, text])
    )
  ).map<GroupedDailyChildren>(([key, children]) => ({
    type: children[0].type,
    text: children[0].text,
    childIds: children.map(({ childId }) => childId),
    key
  }))
