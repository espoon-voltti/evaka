// SPDX-FileCopyrightText: 2017-2020 City of Espoo
//
// SPDX-License-Identifier: LGPL-2.1-or-later

import React, { useContext } from 'react'
import styled from 'styled-components'

import { AttendanceChild, AttendanceStatus, Group } from '~api/attendances'
import RoundIcon from '@evaka/lib-components/src/atoms/RoundIcon'
import colors from '@evaka/lib-components/src/colors'
import { defaultMargins } from '@evaka/lib-components/src/white-space'
import { farUser } from '@evaka/lib-icons'
import { useTranslation } from '~state/i18n'
import { formatDateTimeOnly } from '~utils/date'
import { AttendanceUIContext } from '~state/attendance-ui'

const ChildBox = styled.div<{ type: AttendanceStatus }>`
  align-items: center;
  color: ${colors.greyscale.darkest};
  display: flex;
  padding: ${defaultMargins.xs} ${defaultMargins.s};
  border-radius: 2px;
  background-color: ${colors.greyscale.white};
`

const ChildBoxInfo = styled.div`
  margin-left: 24px;
  flex-grow: 1;
`

const Bold = styled.div`
  font-weight: 600;

  h2,
  h3 {
    font-weight: 500;
  }
`

const IconBox = styled.div<{ type: AttendanceStatus }>`
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
  font-size: 14px;
`

const Time = styled.span`
  margin-left: ${defaultMargins.xs};
`

const ToolsColumn = styled.div`
  display: flex;
  flex-direction: column;
`

interface ChildListItemProps {
  attendanceChild: AttendanceChild
  onClick?: () => void
  type: AttendanceStatus
}

export default React.memo(function ChildListItem({
  attendanceChild,
  onClick,
  type
}: ChildListItemProps) {
  const { i18n } = useTranslation()
  const { attendanceResponse } = useContext(AttendanceUIContext)

  return (
    <ChildBox type={type}>
      <IconBox type={type}>
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
      </IconBox>
      <ChildBoxInfo onClick={onClick}>
        <Bold>
          {attendanceChild.firstName} {attendanceChild.lastName}
        </Bold>
        <DetailsRow>
          <div>
            {i18n.attendances.status[attendanceChild.status]}
            {attendanceResponse.isSuccess &&
              attendanceChild.status === 'COMING' && (
                <span>
                  {' '}
                  (
                  {attendanceResponse.value.unit.groups
                    .find((elem: Group) => elem.id === attendanceChild.groupId)
                    ?.name.toUpperCase()}
                  )
                </span>
              )}
            <Time>
              {attendanceChild.status === 'PRESENT' &&
                formatDateTimeOnly(attendanceChild.attendance?.arrived)}
              {attendanceChild.status === 'DEPARTED' &&
                formatDateTimeOnly(attendanceChild.attendance?.departed)}
            </Time>
          </div>
          {attendanceChild.backup && (
            <RoundIcon content="V" size="m" color={colors.blues.primary} />
          )}
        </DetailsRow>
      </ChildBoxInfo>
      <ToolsColumn>TESTING TESTING</ToolsColumn>
    </ChildBox>
  )
})
