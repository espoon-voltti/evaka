// SPDX-FileCopyrightText: 2017-2021 City of Espoo
//
// SPDX-License-Identifier: LGPL-2.1-or-later

import React, { useContext } from 'react'
import styled from 'styled-components'
import { fontWeights } from 'lib-components/typography'
import {
  AttendanceStatus,
  Child
} from 'lib-common/generated/api-types/attendance'
import RoundIcon from 'lib-components/atoms/RoundIcon'
import colors from 'lib-customizations/common'
import { defaultMargins } from 'lib-components/white-space'
import { farStickyNote, farUser, farUsers } from 'lib-icons'
import { Translations, useTranslation } from '../../state/i18n'
import { Link, useParams } from 'react-router-dom'
import { getTodaysServiceTimes } from '../../utils/dailyServiceTimes'
import { FixedSpaceRow } from 'lib-components/layout/flex-helpers'
import { UUID } from 'lib-common/types'
import { UnitContext } from '../../state/unit'
import { GroupNote } from 'lib-common/generated/api-types/note'

const ChildBox = styled.div`
  align-items: center;
  color: ${colors.greyscale.darkest};
  display: flex;
  padding: ${defaultMargins.xs} ${defaultMargins.s};
  border-radius: 2px;
  background-color: ${colors.greyscale.white};
`

const AttendanceLinkBox = styled(Link)`
  align-items: center;
  display: flex;
  width: 100%;
`

const imageHeight = '56px'

const ChildBoxInfo = styled.div`
  margin-left: 24px;
  flex-grow: 1;
  display: flex;
  flex-direction: column;
  align-items: flex-start;
  justify-content: space-between;
  min-height: ${imageHeight};
`

const Bold = styled.div`
  font-weight: ${fontWeights.semibold};

  h2,
  h3 {
    font-weight: ${fontWeights.medium};
  }
`

export const IconBox = styled.div<{ type: AttendanceStatus }>`
  background-color: ${(props) => {
    switch (props.type) {
      case 'ABSENT':
        return colors.greyscale.dark
      case 'DEPARTED':
        return colors.main.primary
      case 'PRESENT':
        return colors.accents.green
      case 'COMING':
        return colors.accents.water
    }
  }};
  border-radius: 50%;
  box-shadow: ${(props) => {
    switch (props.type) {
      case 'ABSENT':
        return `0 0 0 2px ${colors.greyscale.dark}`
      case 'DEPARTED':
        return `0 0 0 2px ${colors.main.primary}`
      case 'PRESENT':
        return `0 0 0 2px ${colors.accents.green}`
      case 'COMING':
        return `0 0 0 2px ${colors.accents.water}`
    }
  }};
  border: 2px solid ${colors.greyscale.white};
`

const DetailsRow = styled.div`
  display: flex;
  flex-direction: row;
  justify-content: space-between;
  align-items: center;
  color: ${colors.greyscale.dark};
  font-size: 0.875em;
  width: 100%;
`

const RoundImage = styled.img`
  border-radius: 9000px;
  width: ${imageHeight};
  height: ${imageHeight};
  display: block;
`

const FixedSpaceRowWithLeftMargin = styled(FixedSpaceRow)`
  margin-left: ${defaultMargins.m};
`

interface ChildListItemProps {
  child: Child
  onClick?: () => void
  type?: AttendanceStatus
  childAttendanceUrl: string
  groupNote?: GroupNote | null
}

export default React.memo(function ChildListItem({
  child,
  onClick,
  type,
  childAttendanceUrl,
  groupNote
}: ChildListItemProps) {
  const { i18n } = useTranslation()
  const { unitInfoResponse } = useContext(UnitContext)

  const { unitId, groupId } = useParams<{
    unitId: UUID
    groupId: UUID | 'all'
  }>()

  const groupName = unitInfoResponse
    .map(
      ({ groups }) =>
        groups.find((elem) => elem.id === child.groupId)?.name.toUpperCase() ??
        ''
    )
    .getOrElse('')

  const infoText: React.ReactNode = !type
    ? groupName
    : childReservationInfo(i18n, child)

  return (
    <ChildBox data-qa={`child-${child.id}`}>
      <AttendanceLinkBox to={childAttendanceUrl}>
        <IconBox type={child.status}>
          {child.imageUrl ? (
            <RoundImage src={child.imageUrl} />
          ) : (
            <RoundIcon
              content={farUser}
              color={
                type === 'ABSENT'
                  ? colors.greyscale.dark
                  : type === 'DEPARTED'
                  ? colors.main.primary
                  : type === 'PRESENT'
                  ? colors.accents.green
                  : type === 'COMING'
                  ? colors.accents.water
                  : colors.main.dark
              }
              size="XL"
            />
          )}
        </IconBox>
        <ChildBoxInfo onClick={onClick}>
          <Bold data-qa={'child-name'}>
            {child.firstName} {child.lastName}
          </Bold>
          <DetailsRow>
            <div>
              {infoText}
              {child.backup && (
                <RoundIcon content="V" size="m" color={colors.accents.green} />
              )}
            </div>
            <FixedSpaceRowWithLeftMargin>
              {child.dailyNote && (
                <Link
                  to={`/units/${unitId}/groups/${groupId}/child-attendance/${child.id}/note`}
                  data-qa={'link-child-daycare-daily-note'}
                >
                  <RoundIcon
                    content={farStickyNote}
                    color={colors.accents.petrol}
                    size="m"
                  />
                </Link>
              )}
              {groupNote && (
                <Link
                  to={`/units/${unitId}/groups/${groupId}/child-attendance/${child.id}/note`}
                  data-qa={'link-child-daycare-daily-note'}
                >
                  <RoundIcon
                    content={farUsers}
                    color={colors.main.light}
                    size="m"
                  />
                </Link>
              )}
            </FixedSpaceRowWithLeftMargin>
          </DetailsRow>
        </ChildBoxInfo>
      </AttendanceLinkBox>
    </ChildBox>
  )
})

const childReservationInfo = (
  i18n: Translations,
  { reservations, dailyServiceTimes }: Child
): React.ReactNode => {
  if (reservations.length > 0) {
    return `${
      reservations.length > 1
        ? i18n.attendances.serviceTime.reservationsShort
        : i18n.attendances.serviceTime.reservationShort
    } ${reservations
      .map(({ startTime, endTime }) => `${startTime}-${endTime}`)
      .join(', ')}`
  }

  const todaysServiceTime = getTodaysServiceTimes(dailyServiceTimes)

  return (
    <em>
      {todaysServiceTime === 'not_set'
        ? i18n.attendances.serviceTime.notSetShort
        : todaysServiceTime === 'not_today'
        ? i18n.attendances.serviceTime.noServiceTodayShort
        : todaysServiceTime === 'variable_times'
        ? i18n.attendances.serviceTime.variableTimesShort
        : i18n.attendances.serviceTime.serviceTodayShort(
            todaysServiceTime.start,
            todaysServiceTime.end
          )}
    </em>
  )
}
