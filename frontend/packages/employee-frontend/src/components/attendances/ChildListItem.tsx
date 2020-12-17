// SPDX-FileCopyrightText: 2017-2020 City of Espoo
//
// SPDX-License-Identifier: LGPL-2.1-or-later

import React from 'react'
import styled from 'styled-components'

import { AttendanceChild, AttendanceStatus } from '~api/attendances'
import RoundIcon from '~components/shared/atoms/RoundIcon'
import colors from '@evaka/lib-components/src/colors'
import { defaultMargins } from '@evaka/lib-components/src/white-space'
import { DATE_FORMAT_TIME_ONLY } from '~constants'
import { farUser } from '@evaka/lib-icons'
import { useTranslation } from '~state/i18n'
import { formatDate } from '~utils/date'

const ChildBox = styled.div<{ type: AttendanceStatus }>`
  align-items: center;
  color: ${colors.greyscale.darkest};
  display: flex;
  padding: 10px;
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
  border-radius: 4px;
`

const DetailsRow = styled.div`
  display: flex;
  flex-direction: row;
  justify-content: space-between;
  align-items: center;
  color: ${colors.greyscale.dark};
`

const Time = styled.span`
  margin-left: ${defaultMargins.xs};
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
            <Time>
              {attendanceChild.status === 'PRESENT' &&
                formatDate(
                  attendanceChild.attendance?.arrived,
                  DATE_FORMAT_TIME_ONLY
                )}
              {attendanceChild.status === 'DEPARTED' &&
                formatDate(
                  attendanceChild.attendance?.departed,
                  DATE_FORMAT_TIME_ONLY
                )}
            </Time>
          </div>
          {attendanceChild.backup && (
            <RoundIcon content="V" size="m" color={colors.blues.primary} />
          )}
        </DetailsRow>
      </ChildBoxInfo>
    </ChildBox>
  )
})
