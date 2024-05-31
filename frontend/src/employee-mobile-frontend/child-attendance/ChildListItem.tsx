// SPDX-FileCopyrightText: 2017-2022 City of Espoo
//
// SPDX-License-Identifier: LGPL-2.1-or-later

import React, { useMemo } from 'react'
import { Link } from 'react-router-dom'
import styled from 'styled-components'

import {
  AttendanceChild,
  AttendanceStatus
} from 'lib-common/generated/api-types/attendance'
import LocalDate from 'lib-common/local-date'
import { queryOrDefault, useQuery, useQueryResult } from 'lib-common/query'
import RoundIcon from 'lib-components/atoms/RoundIcon'
import { FixedSpaceRow } from 'lib-components/layout/flex-helpers'
import { Bold, InformationText } from 'lib-components/typography'
import { defaultMargins } from 'lib-components/white-space'
import colors, { attendanceColors } from 'lib-customizations/common'
import { farStickyNote, farUser, farUsers } from 'lib-icons'

import { routes } from '../App'
import { groupNotesQuery } from '../child-notes/queries'
import { getTodaysServiceTimes } from '../common/dailyServiceTimes'
import { useTranslation } from '../common/i18n'
import { UnitOrGroup, toUnitOrGroup } from '../common/unit-or-group'
import { unitInfoQuery } from '../units/queries'

import { ListItem } from './ChildList'
import { Reservations } from './Reservations'

const ChildBox = styled.div`
  align-items: center;
  display: flex;
  padding: ${defaultMargins.xs} ${defaultMargins.s};
  border-radius: 2px;
  background-color: ${colors.grayscale.g0};
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
  border: 2px solid ${colors.grayscale.g0};
`

const DetailsRow = styled.div`
  display: flex;
  flex-direction: row;
  justify-content: space-between;
  align-items: center;
  color: ${colors.grayscale.g70};
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
  word-break: break-word;
`

const GroupName = styled(InformationText)`
  text-align: right;
`

interface ChildListItemProps {
  unitOrGroup: UnitOrGroup
  child: ListItem
  onClick?: () => void
  type?: AttendanceStatus
  childAttendanceUrl: string
}

export default React.memo(function ChildListItem({
  unitOrGroup,
  child,
  onClick,
  type,
  childAttendanceUrl
}: ChildListItemProps) {
  const unitId = unitOrGroup.unitId
  const unitInfoResponse = useQueryResult(unitInfoQuery({ unitId }))

  const { data: groupNotes = [] } = useQuery(
    queryOrDefault(
      groupNotesQuery,
      []
    )(child.groupId ? { groupId: child.groupId } : undefined)
  )
  const groupName = unitInfoResponse
    .map(
      ({ groups }) =>
        groups.find((elem) => elem.id === child.groupId)?.name.toUpperCase() ??
        ''
    )
    .getOrElse('')

  const infoText: React.ReactNode = !type ? (
    groupName
  ) : (
    <ChildReservationInfo child={child} />
  )

  const maybeGroupName =
    type && unitOrGroup.type === 'unit' ? groupName : undefined
  const today = LocalDate.todayInSystemTz()
  const childAge = today.differenceInYears(child.dateOfBirth)

  return (
    <ChildBox data-qa={`child-${child.id}`}>
      <AttendanceLinkBox to={childAttendanceUrl}>
        <IconBox type={child.status}>
          {child.imageUrl ? (
            <RoundImage src={child.imageUrl} />
          ) : (
            <RoundIcon
              content={farUser}
              color={type ? attendanceColors[type] : colors.main.m1}
              size="XL"
            />
          )}
        </IconBox>
        <ChildBoxInfo onClick={onClick}>
          <NameRow>
            <Bold data-qa="child-name">
              {child.firstName} {child.lastName}
              {child.preferredName ? ` (${child.preferredName})` : null}
            </Bold>
            <GroupName data-qa={`child-group-name-${child.id}`}>
              {maybeGroupName}
            </GroupName>
          </NameRow>
          <DetailsRow>
            <LeftDetailsDiv>
              {infoText}
              {child.backup && (
                <RoundIcon content="V" size="m" color={colors.main.m1} />
              )}
            </LeftDetailsDiv>
            <FixedSpaceRowWithLeftMargin alignItems="center">
              {child.dailyNote && (
                <Link
                  to={
                    routes.childNotes(
                      toUnitOrGroup({ unitId, groupId: child.groupId }),
                      child.id
                    ).value
                  }
                  data-qa="link-child-daycare-daily-note"
                >
                  <RoundIcon
                    content={farStickyNote}
                    color={colors.accents.a9pink}
                    size="m"
                  />
                </Link>
              )}
              {child.groupId && groupNotes.length > 0 ? (
                <Link
                  to={
                    routes.childNotes(
                      toUnitOrGroup({ unitId, groupId: child.groupId }),
                      child.id
                    ).value
                  }
                  data-qa="link-child-daycare-daily-note"
                >
                  <RoundIcon
                    content={farUsers}
                    color={colors.main.m4}
                    size="m"
                  />
                </Link>
              ) : null}
              <AgeRoundIcon
                content={`${childAge}v`}
                color={
                  childAge < 3 ? colors.accents.a6turquoise : colors.main.m1
                }
                size="m"
              />
            </FixedSpaceRowWithLeftMargin>
          </DetailsRow>
        </ChildBoxInfo>
      </AttendanceLinkBox>
    </ChildBox>
  )
})

function ChildReservationInfo(props: { child: AttendanceChild }) {
  const { reservations, dailyServiceTimes, scheduleType } = props.child
  const { i18n } = useTranslation()

  const reservationsWithTimes = useMemo(
    () =>
      reservations.flatMap((reservation) =>
        reservation.type === 'TIMES' ? [reservation] : []
      ),
    [reservations]
  )
  if (reservationsWithTimes.length > 0) {
    return <Reservations reservations={reservationsWithTimes} />
  }
  if (scheduleType === 'FIXED_SCHEDULE') {
    return <em>{i18n.attendances.serviceTime.present}</em>
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
                todaysServiceTime.formatStart(),
                todaysServiceTime.formatEnd()
              )}
    </em>
  )
}

const LeftDetailsDiv = styled.div`
  > * {
    margin-left: ${defaultMargins.xs};

    &:first-child {
      margin-left: 0;
    }
  }
`
const AgeRoundIcon = styled(RoundIcon)`
  &.m {
    font-size: 14px;
  }
`
