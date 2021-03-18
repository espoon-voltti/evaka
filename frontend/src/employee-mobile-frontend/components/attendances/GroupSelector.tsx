// SPDX-FileCopyrightText: 2017-2020 City of Espoo
//
// SPDX-License-Identifier: LGPL-2.1-or-later

import React, { Fragment, useContext } from 'react'
import styled from 'styled-components'

import { ChoiceChip } from '@evaka/lib-components/atoms/Chip'
import { Gap } from '@evaka/lib-components/white-space'

import { AttendanceUIContext } from '../../state/attendance-ui'
import { useTranslation } from '../../state/i18n'
import { AttendanceResponse, Group } from '../../api/attendances'
import colors from '@evaka/lib-components/colors'

interface GroupSelectorProps {
  groupIdOrAll: string | 'all'
  selectedGroup: Group | undefined
  changeGroup: (groupOrAll: Group | 'all') => void
}

export default function GroupSelector({
  groupIdOrAll,
  selectedGroup,
  changeGroup
}: GroupSelectorProps) {
  const { i18n } = useTranslation()

  const { attendanceResponse } = useContext(AttendanceUIContext)

  function numberOfChildrenPresent(gId: string, res: AttendanceResponse) {
    return res.children
      .filter((child) => child.groupId === gId)
      .filter((child) => {
        return child.status === 'PRESENT'
      }).length
  }

  function getTotalChildren(groupId: string) {
    if (attendanceResponse.isSuccess) {
      return attendanceResponse.value.children.filter((ac) => {
        return ac.groupId === groupId
      }).length
    }
    return 0
  }

  return (
    <GroupChipWrapper>
      {attendanceResponse.isSuccess && (
        <>
          <ChoiceChip
            text={`${i18n.common.all} (${
              attendanceResponse.value.children.filter((child) => {
                return child.status === 'PRESENT'
              }).length
            }/${attendanceResponse.value.children.length})`}
            selected={groupIdOrAll === 'all' ? true : false}
            onChange={() => changeGroup('all')}
          />
          <Gap horizontal size={'xxs'} />
          {attendanceResponse.value.unit.groups.map((group) => (
            <Fragment key={group.id}>
              <ChoiceChip
                text={`${group.name} (${numberOfChildrenPresent(
                  group.id,
                  attendanceResponse.value
                )}/${getTotalChildren(group.id)})`}
                selected={selectedGroup ? selectedGroup.id === group.id : false}
                onChange={() => {
                  changeGroup(group)
                }}
              />
              <Gap horizontal size={'xxs'} />
            </Fragment>
          ))}
        </>
      )}
      <Info>{i18n.attendances.chooseGroupInfo}</Info>
    </GroupChipWrapper>
  )
}

const GroupChipWrapper = styled.div`
  display: flex;
  flex-direction: row;
  flex-wrap: wrap;
  margin: 12px 16px 8px 16px;

  > div {
    margin-bottom: 16px;
  }
`

const Info = styled.span`
  font-style: normal;
  font-weight: 600;
  font-size: 14px;
  line-height: 21px;
  color: ${colors.greyscale.dark};
`
