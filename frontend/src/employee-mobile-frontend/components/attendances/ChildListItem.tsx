// SPDX-FileCopyrightText: 2017-2022 City of Espoo
//
// SPDX-License-Identifier: LGPL-2.1-or-later

import { farStickyNote, farUser, farUsers } from 'lib-icons'
import React, { useContext } from 'react'
import { Link, useParams } from 'react-router-dom'
import styled from 'styled-components'
import {
  AttendanceStatus,
  Child
} from 'lib-common/generated/api-types/attendance'
import { GroupNote } from 'lib-common/generated/api-types/note'
import { UUID } from 'lib-common/types'
import RoundIcon from 'lib-components/atoms/RoundIcon'
import { FixedSpaceRow } from 'lib-components/layout/flex-helpers'
import { Bold, InformationText } from 'lib-components/typography'
import { defaultMargins } from 'lib-components/white-space'
import colors, { attendanceColors } from 'lib-customizations/common'
import { Translations, useTranslation } from '../../state/i18n'
import { UnitContext } from '../../state/unit'
import { getTodaysServiceTimes } from '../../utils/dailyServiceTimes'

const ChildBox = styled.div`
  align-items: center;
  display: flex;
  padding: ${defaultMargins.xs} ${defaultMargins.s};
  border-radius: 2px;
  background-color: ${colors.greyscale.white};
`

const AttendanceLinkBox = styled(Link)`
  display: flex;
  align-items: center;
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

export const IconBox = styled.div<{ type: AttendanceStatus }>`
  background-color: ${(props) => attendanceColors[props.type]};
  border-radius: 50%;
  box-shadow: 0 0 0 2px ${(props) => attendanceColors[props.type]};
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

const NameRow = styled.div`
  display: flex;
  justify-content: space-between;
  width: 100%;
`

const GroupName = styled(InformationText)`
  white-space: nowrap;
  text-align: right;
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

  const maybeGroupName = type && groupId === 'all' ? groupName : undefined

  return (
    <ChildBox data-qa={`child-${child.id}`}>
      <AttendanceLinkBox to={childAttendanceUrl}>
        <IconBox type={child.status}>
          {child.imageUrl ? (
            <RoundImage src={child.imageUrl} />
          ) : (
            <RoundIcon
              content={farUser}
              color={type ? attendanceColors[type] : colors.main.dark}
              size="XL"
            />
          )}
        </IconBox>
        <ChildBoxInfo onClick={onClick}>
          <NameRow>
            <Bold data-qa="child-name">
              {child.firstName} {child.lastName}
            </Bold>
            <GroupName data-qa={`child-group-name-${child.id}`}>
              {maybeGroupName}
            </GroupName>
          </NameRow>
          <DetailsRow>
            <div>
              {infoText}
              {child.backup && (
                <RoundIcon content="V" size="m" color={colors.main.dark} />
              )}
            </div>
            <FixedSpaceRowWithLeftMargin>
              {child.dailyNote && (
                <Link
                  to={`/units/${unitId}/groups/${groupId}/child-attendance/${child.id}/note`}
                  data-qa="link-child-daycare-daily-note"
                >
                  <RoundIcon
                    content={farStickyNote}
                    color={colors.accents.pink}
                    size="m"
                  />
                </Link>
              )}
              {groupNote && (
                <Link
                  to={`/units/${unitId}/groups/${groupId}/child-attendance/${child.id}/note`}
                  data-qa="link-child-daycare-daily-note"
                >
                  <RoundIcon
                    content={farUsers}
                    color={colors.main.lighter}
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
