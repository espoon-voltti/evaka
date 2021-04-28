// SPDX-FileCopyrightText: 2017-2021 City of Espoo
//
// SPDX-License-Identifier: LGPL-2.1-or-later

import React, { Fragment, useContext } from 'react'
import styled from 'styled-components'

import { ChoiceChip } from 'lib-components/atoms/Chip'
import { Gap } from 'lib-components/white-space'

import { AttendanceUIContext } from '../../state/attendance-ui'
import { useTranslation } from '../../state/i18n'
import { AttendanceResponse, Group } from '../../api/attendances'
import colors from 'lib-components/colors'
import { ChipWrapper } from '../mobile/components'

interface GroupSelectorProps {
  selectedGroup: Group | undefined
  allowAllGroups: boolean
  onChangeGroup: (group: Group | undefined) => void
}

export default function GroupSelector({
  selectedGroup,
  allowAllGroups,
  onChangeGroup
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
    <Wrapper>
      <ChipWrapper>
        {attendanceResponse.isSuccess && (
          <>
            {allowAllGroups && (
              <>
                <ChoiceChip
                  text={`${i18n.common.all} (${
                    attendanceResponse.value.children.filter((child) => {
                      return child.status === 'PRESENT'
                    }).length
                  }/${attendanceResponse.value.children.length})`}
                  selected={selectedGroup ? false : true}
                  onChange={() => onChangeGroup(undefined)}
                  data-qa="group--all"
                />
                <Gap horizontal size={'xxs'} />
              </>
            )}
            {attendanceResponse.value.unit.groups.map((group) => (
              <Fragment key={group.id}>
                <ChoiceChip
                  text={`${group.name} (${numberOfChildrenPresent(
                    group.id,
                    attendanceResponse.value
                  )}/${getTotalChildren(group.id)})`}
                  selected={
                    selectedGroup ? selectedGroup.id === group.id : false
                  }
                  onChange={() => {
                    onChangeGroup(group)
                  }}
                  data-qa={`group--${group.id}`}
                />
                <Gap horizontal size={'xxs'} />
              </Fragment>
            ))}
          </>
        )}
        <Info>{i18n.attendances.chooseGroupInfo}</Info>
      </ChipWrapper>
    </Wrapper>
  )
}

const Info = styled.span`
  font-style: normal;
  font-weight: 600;
  font-size: 14px;
  line-height: 21px;
  color: ${colors.greyscale.dark};
`

const Wrapper = styled.div`
  margin: 12px 16px 8px 16px;
`
