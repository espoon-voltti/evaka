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
import { defaultMargins, SpacingSize } from 'lib-components/white-space'
import { farStickyNote, farUser, farUsers } from 'lib-icons'
import { useTranslation } from '../../state/i18n'
import { DATE_FORMAT_TIME_ONLY, formatDate } from 'lib-common/date'
import { ChildAttendanceContext } from '../../state/child-attendance'
import { Link, useParams } from 'react-router-dom'
import { FixedSpaceRow } from 'lib-components/layout/flex-helpers'
import { DaycareDailyNote } from 'lib-common/generated/api-types/messaging'
import { UUID } from 'lib-common/types'
import { UnitContext } from '../../state/unit'

const ChildBox = styled.div<{ type: AttendanceStatus }>`
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
        return colors.blues.primary
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
        return `0 0 0 2px ${colors.blues.primary}`
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

const StatusDetails = styled.span`
  margin: 0 ${defaultMargins.xs};
`

const RoundImage = styled.img`
  border-radius: 9000px;
  width: ${imageHeight};
  height: ${imageHeight};
  display: block;
`

const FixedSpaceRowWithLeftMargin = styled(FixedSpaceRow)<{
  spacing: SpacingSize
}>`
  margin-left: ${({ spacing }) => defaultMargins[spacing]};
`

interface ChildListItemProps {
  child: Child
  onClick?: () => void
  type: AttendanceStatus
  childAttendanceUrl: string
  groupNote?: DaycareDailyNote | null
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
  const { attendanceResponse } = useContext(ChildAttendanceContext)

  const { unitId, groupId } = useParams<{
    unitId: UUID
    groupId: UUID | 'all'
  }>()

  return (
    <ChildBox type={type} data-qa={`child-${child.id}`}>
      <AttendanceLinkBox to={childAttendanceUrl}>
        <IconBox type={type}>
          {child.imageUrl ? (
            <RoundImage src={child.imageUrl} />
          ) : (
            <RoundIcon
              content={farUser}
              color={
                type === 'ABSENT'
                  ? colors.greyscale.dark
                  : type === 'DEPARTED'
                  ? colors.blues.primary
                  : type === 'PRESENT'
                  ? colors.accents.green
                  : type === 'COMING'
                  ? colors.accents.water
                  : colors.blues.medium
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
            <div data-qa={'child-status'}>
              {child.reservation !== null
                ? i18n.attendances.listChildReservation(
                    child.reservation.startTime,
                    child.reservation.endTime
                  )
                : i18n.attendances.status[child.status]}
              <StatusDetails>
                {attendanceResponse.isSuccess &&
                  unitInfoResponse.isSuccess &&
                  child.status === 'COMING' && (
                    <span>
                      {' '}
                      (
                      {unitInfoResponse.value.groups
                        .find((group) => group.id === child.groupId)
                        ?.name.toUpperCase()}
                      )
                    </span>
                  )}
                {child.status === 'PRESENT' &&
                  `${i18n.attendances.arrived} ${formatDate(
                    child.attendance?.arrived,
                    DATE_FORMAT_TIME_ONLY
                  )}`}
                {child.status === 'DEPARTED' &&
                  `${i18n.attendances.departed} ${formatDate(
                    child.attendance?.departed,
                    DATE_FORMAT_TIME_ONLY
                  )}`}
              </StatusDetails>
              {child.backup && (
                <RoundIcon content="V" size="m" color={colors.accents.green} />
              )}
            </div>
            <FixedSpaceRowWithLeftMargin spacing="m">
              {child.dailyNote && attendanceResponse.isSuccess && (
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
              {groupNote && attendanceResponse.isSuccess && (
                <Link
                  to={`/units/${unitId}/groups/${groupId}/child-attendance/${child.id}/note`}
                  data-qa={'link-child-daycare-daily-note'}
                >
                  <RoundIcon
                    content={farUsers}
                    color={colors.blues.light}
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
