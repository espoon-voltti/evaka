// SPDX-FileCopyrightText: 2017-2021 City of Espoo
//
// SPDX-License-Identifier: LGPL-2.1-or-later

import React, { Fragment, useContext } from 'react'
import styled from 'styled-components'
import { ChoiceChip } from 'lib-components/atoms/Chip'
import { Gap } from 'lib-components/white-space'
import { fontWeights } from 'lib-components/typography'
import { AttendanceResponse } from 'lib-common/generated/api-types/attendance'
import { ChildAttendanceContext } from '../../state/child-attendance'
import { useTranslation } from '../../state/i18n'
import { Group } from '../../api/attendances'
import colors from 'lib-customizations/common'
import { ChipWrapper } from '../mobile/components'
import { UnitContext } from '../../state/unit'

interface GroupSelectorProps {
  selectedGroup: Group | undefined
  onChangeGroup: (group: Group | undefined) => void
}

export default function GroupSelector({
  selectedGroup,
  onChangeGroup
}: GroupSelectorProps) {
  const { i18n } = useTranslation()

  const { unitInfoResponse } = useContext(UnitContext)
  const { attendanceResponse } = useContext(ChildAttendanceContext)

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
        {unitInfoResponse.isSuccess && attendanceResponse.isSuccess && (
          <>
            <ChoiceChip
              text={`${i18n.common.all} (${
                attendanceResponse.value.children.filter((child) => {
                  return child.status === 'PRESENT'
                }).length
              }/${attendanceResponse.value.children.length})`}
              selected={!selectedGroup}
              onChange={() => onChangeGroup(undefined)}
              data-qa="group--all"
            />
            <Gap horizontal size={'xxs'} />
            {unitInfoResponse.value.groups.map((group) => (
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
  font-weight: ${fontWeights.semibold};
  font-size: 14px;
  line-height: 21px;
  color: ${colors.greyscale.dark};
`

const Wrapper = styled.div`
  margin: 12px 16px 8px 16px;
  max-height: 60vh;
  overflow: scroll;
`
