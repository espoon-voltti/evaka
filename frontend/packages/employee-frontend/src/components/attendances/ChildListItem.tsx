// SPDX-FileCopyrightText: 2017-2020 City of Espoo
//
// SPDX-License-Identifier: LGPL-2.1-or-later

import React from 'react'
import styled from 'styled-components'

import { AttendanceStatus, ChildInGroup } from '~api/unit'
import RoundIcon from '~components/shared/atoms/RoundIcon'
import Colors from '~components/shared/Colors'
import { DefaultMargins } from '~components/shared/layout/white-space'
import { farUser } from '~icon-set'
import { useTranslation } from '~state/i18n'

const ChildBox = styled.div<{ type: AttendanceStatus }>`
  align-items: center;
  color: ${Colors.greyscale.darkest};
  display: flex;
  padding: 10px;
  border-radius: 2px;
  background-color: ${Colors.greyscale.white};
`

const ChildBoxInfo = styled.div`
  margin-left: 24px;
`

const Bold = styled.div`
  font-weight: 600;

  h2,
  h3 {
    font-weight: 500;
  }
`

const ChildBoxStatus = styled.div`
  color: ${Colors.greyscale.dark};
`

const IconBox = styled.div<{ type: AttendanceStatus }>`
  background-color: ${(props) => {
    switch (props.type) {
      case 'ABSENT':
        return Colors.greyscale.dark
      case 'DEPARTED':
        return Colors.blues.primary
      case 'PRESENT':
        return Colors.accents.green
      case 'COMING':
        return Colors.accents.water
    }
  }};
  border-radius: 4px;
`

const Time = styled.span`
  margin-left: ${DefaultMargins.xs};
`

interface ChildListItemProps {
  childInGroup: ChildInGroup
  onClick?: () => void
  type: AttendanceStatus
}

export default React.memo(function ChildListItem({
  childInGroup,
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
              ? Colors.greyscale.dark
              : type === 'DEPARTED'
              ? Colors.blues.primary
              : type === 'PRESENT'
              ? Colors.accents.green
              : type === 'COMING'
              ? Colors.accents.water
              : Colors.blues.medium
          }
          size="XL"
        />
      </IconBox>
      <ChildBoxInfo onClick={onClick}>
        <Bold>
          {childInGroup.firstName} {childInGroup.lastName}
        </Bold>
        <ChildBoxStatus>
          {i18n.attendances.status[childInGroup.status]}
          <Time>
            {childInGroup.status === 'PRESENT' &&
              childInGroup.arrived &&
              `${
                childInGroup.arrived?.getHours() < 10
                  ? `0${childInGroup.arrived?.getHours()}`
                  : childInGroup.arrived?.getHours()
              }:${
                childInGroup.arrived?.getMinutes() < 10
                  ? `0${childInGroup.arrived?.getMinutes()}`
                  : childInGroup.arrived?.getMinutes()
              }`}
            {childInGroup.status === 'DEPARTED' &&
              childInGroup.departed &&
              `${
                childInGroup.departed?.getHours() < 10
                  ? `0${childInGroup.departed?.getHours()}`
                  : childInGroup.departed?.getHours()
              }:${
                childInGroup.departed?.getMinutes() < 10
                  ? `0${childInGroup.departed?.getMinutes()}`
                  : childInGroup.departed?.getMinutes()
              }`}
          </Time>
        </ChildBoxStatus>
      </ChildBoxInfo>
    </ChildBox>
  )
})
