// SPDX-FileCopyrightText: 2017-2024 City of Espoo
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
import { constantQuery, useQuery, useQueryResult } from 'lib-common/query'
import RoundIcon from 'lib-components/atoms/RoundIcon'
import { FixedSpaceRow } from 'lib-components/layout/flex-helpers'
import { Bold, InformationText } from 'lib-components/typography'
import { defaultMargins } from 'lib-components/white-space'
import colors, { attendanceColors } from 'lib-customizations/common'
import { fasExclamation } from 'lib-icons'
import { farStickyNote, farUser, farUsers } from 'lib-icons'

import { routes } from '../App'
import { groupNotesQuery } from '../child-notes/queries'
import { getTodaysServiceTimes } from '../common/dailyServiceTimes'
import { useTranslation } from '../common/i18n'
import { UnitOrGroup } from '../common/unit-or-group'
import { unitInfoQuery } from '../units/queries'

import { ListItem } from './ChildList'
import { Reservations } from './Reservations'

const imageHeight = '56px'

const ChildBox = styled.div`
  align-items: center;
  display: flex;
  padding: ${defaultMargins.xs} ${defaultMargins.s};
  border-radius: 2px;
  background-color: ${colors.grayscale.g0};
`

const AttendanceLinkBox = styled(Link)`
  display: flex;
  flex-direction: row;
  align-items: center;
  justify-content: space-between;
  width: 100%;
`

const MultiselectBox = styled.div`
  display: flex;
  flex-direction: row;
  align-items: center;
  justify-content: space-between;
  width: 100%;
`

export const IconBox = styled.div<{ type: AttendanceStatus }>`
  background-color: ${(props) => attendanceColors[props.type]};
  border-radius: 50%;
  box-shadow: 0 0 0 2px ${(props) => attendanceColors[props.type]};
  border: 2px solid ${colors.grayscale.g0};
`

const MainInfoColumn = styled.div`
  margin-left: 24px;
  flex-grow: 1;
  display: flex;
  flex-direction: column;
  justify-content: space-between;
  min-height: ${imageHeight};
`

const RightColumn = styled.div`
  display: flex;
  flex-direction: column;
  justify-content: space-between;
  align-items: flex-end;
  min-height: ${imageHeight};
  margin-left: ${defaultMargins.xs};
`

const NameRow = styled.div`
  display: flex;
  justify-content: space-between;
  width: 100%;
  word-break: break-word;
`

const DetailsText = styled.div`
  color: ${colors.grayscale.g70};
  font-size: 0.875em;
`

const RoundImage = styled.img`
  border-radius: 9000px;
  width: ${imageHeight};
  height: ${imageHeight};
  display: block;
`

const GroupName = styled(InformationText)`
  text-align: right;
`

const RoundIconOnTop = styled(RoundIcon)`
  position: absolute;
  left: 40px;
  top: -20px;
  z-index: 2;
`

const IconPlacementBox = styled.div`
  position: relative;
  width: 0;
  height: 0;
  &.m {
    font-size: 14px;
  }
`

interface ChildListItemProps {
  unitOrGroup: UnitOrGroup
  child: ListItem
  type?: AttendanceStatus
  childAttendanceUrl: string
  selected: boolean | null // null = not in multiselect mode
  onChangeSelected: (selected: boolean) => void
}

export default React.memo(function ChildListItem({
  unitOrGroup,
  child,
  type,
  selected,
  onChangeSelected,
  childAttendanceUrl
}: ChildListItemProps) {
  const { i18n } = useTranslation()
  const unitId = unitOrGroup.unitId
  const unitInfoResponse = useQueryResult(unitInfoQuery({ unitId }))

  const { data: groupNotes = [] } = useQuery(
    child.groupId
      ? groupNotesQuery({ groupId: child.groupId })
      : constantQuery([])
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

  const hasActiveStickyNote = useMemo(
    () => child.stickyNotes.some((n) => n.expires.isEqualOrAfter(today)),
    [child, today]
  )

  return (
    <ChildBox data-qa={`child-${child.id}`}>
      {selected === null ? (
        <AttendanceLinkBox to={childAttendanceUrl}>
          <ChildImage child={child} type={type} />
          <MainInfoColumn>
            <NameRow>
              <Bold data-qa="child-name">
                {child.firstName} {child.lastName}
                {child.preferredName ? ` (${child.preferredName})` : null}
              </Bold>
            </NameRow>
            <FixedSpaceRow spacing="xs">
              <DetailsText>{infoText}</DetailsText>
              {child.backup && (
                <RoundIcon content="V" size="m" color={colors.main.m1} />
              )}
            </FixedSpaceRow>
          </MainInfoColumn>
          <RightColumn>
            <GroupName data-qa={`child-group-name-${child.id}`}>
              {maybeGroupName}
            </GroupName>
            <FixedSpaceRow alignItems="center">
              {hasActiveStickyNote && (
                <Link
                  to={routes.childNotes(unitId, child.id).value}
                  data-qa="link-child-daycare-daily-note"
                >
                  <RoundIcon
                    content={fasExclamation}
                    color={colors.accents.a5orangeLight}
                    size="m"
                  />
                </Link>
              )}
              {child.dailyNote && (
                <Link
                  to={routes.childNotes(unitId, child.id).value}
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
                  to={routes.childNotes(unitId, child.id).value}
                  data-qa="link-child-daycare-daily-note"
                >
                  <RoundIcon
                    content={farUsers}
                    color={colors.main.m4}
                    size="m"
                  />
                </Link>
              ) : null}
            </FixedSpaceRow>
          </RightColumn>
        </AttendanceLinkBox>
      ) : (
        <MultiselectBox onClick={() => onChangeSelected(!selected)}>
          <ChildImage child={child} type={type} />
          <MainInfoColumn>
            <NameRow>
              <Bold data-qa="child-name">
                {child.firstName} {child.lastName}
                {child.preferredName ? ` (${child.preferredName})` : null}
              </Bold>
            </NameRow>
            <DetailsText>
              {selected
                ? i18n.attendances.actions.arrivalMultiselect.selected
                : i18n.attendances.actions.arrivalMultiselect.select}
            </DetailsText>
          </MainInfoColumn>
          <RightColumn>{selected ? '[x]' : '[ ]'}</RightColumn>
        </MultiselectBox>
      )}
    </ChildBox>
  )
})

export const ChildImage = React.memo(function ChildImage({
  child,
  type
}: {
  child: AttendanceChild
  type?: AttendanceStatus
}) {
  const childAge = LocalDate.todayInHelsinkiTz().differenceInYears(
    child.dateOfBirth
  )
  return (
    <IconBox type={type ?? 'COMING'}>
      {child.imageUrl ? (
        <RoundImage src={child.imageUrl} />
      ) : (
        <RoundIcon
          content={farUser}
          color={type ? attendanceColors[type] : colors.main.m1}
          size="XL"
        />
      )}
      <IconPlacementBox>
        <RoundIconOnTop
          content={`${childAge}v`}
          color={childAge < 3 ? colors.accents.a6turquoise : colors.main.m1}
          size="m"
        />
      </IconPlacementBox>
    </IconBox>
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
