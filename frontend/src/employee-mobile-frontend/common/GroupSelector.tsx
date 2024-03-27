// SPDX-FileCopyrightText: 2017-2022 City of Espoo
//
// SPDX-License-Identifier: LGPL-2.1-or-later

import { FontAwesomeIcon } from '@fortawesome/react-fontawesome'
import React from 'react'
import styled from 'styled-components'

import { GroupInfo } from 'lib-common/generated/api-types/attendance'
import { useQueryResult } from 'lib-common/query'
import { UUID } from 'lib-common/types'
import { FixedSpaceRow } from 'lib-components/layout/flex-helpers'
import { fontSizesMobile, fontWeights } from 'lib-components/typography'
import colors, { theme } from 'lib-customizations/common'
import { faCheck } from 'lib-icons'

import { renderResult } from '../async-rendering'
import { unitInfoQuery } from '../units/queries'

import { useTranslation } from './i18n'

interface GroupSelectorProps {
  unitId: UUID
  selectedGroup: GroupInfo | undefined
  onChangeGroup: (group: GroupInfo | undefined) => void
  countInfo?: CountInfo
  groups?: GroupInfo[]
  includeSelectAll: boolean
}

export interface CountInfo {
  getPresentCount: (groupId: UUID | undefined) => number
  getTotalCount: (groupId: UUID | undefined) => number
  infoText: string
}

export default function GroupSelector({
  unitId,
  selectedGroup,
  onChangeGroup,
  countInfo,
  groups,
  includeSelectAll
}: GroupSelectorProps) {
  const { i18n } = useTranslation()

  const unitInfoResponse = useQueryResult(unitInfoQuery({ unitId }))

  return renderResult(unitInfoResponse, (unitInfo) => {
    const realtimeStaffAttendanceEnabled = unitInfo.features.includes(
      'REALTIME_STAFF_ATTENDANCE'
    )
    return (
      <Wrapper>
        {includeSelectAll && (
          <Group
            selected={!selectedGroup}
            onClick={() => onChangeGroup(undefined)}
            data-qa="group--all"
            name={i18n.common.all}
            utilization={
              realtimeStaffAttendanceEnabled ? unitInfo.utilization : undefined
            }
            count={
              countInfo
                ? `${countInfo.getPresentCount(
                    undefined
                  )}/${countInfo.getTotalCount(undefined)}`
                : ''
            }
          />
        )}
        {(groups || unitInfo.groups).map((group) => (
          <Group
            key={group.id}
            name={group.name}
            utilization={
              realtimeStaffAttendanceEnabled ? group.utilization : undefined
            }
            selected={selectedGroup ? selectedGroup.id === group.id : false}
            onClick={() => {
              onChangeGroup(group)
            }}
            data-qa={`group--${group.id}`}
            count={
              countInfo
                ? `${countInfo.getPresentCount(
                    group.id
                  )}/${countInfo.getTotalCount(group.id)}`
                : ''
            }
          />
        ))}
        {countInfo && !realtimeStaffAttendanceEnabled && (
          <Info>{countInfo.infoText}</Info>
        )}
      </Wrapper>
    )
  })
}

const Info = styled.div`
  font-style: normal;
  font-weight: ${fontWeights.semibold};
  font-size: 14px;
  line-height: 21px;
  color: ${colors.grayscale.g70};
  text-align: center;
  width: 100%;
  padding: 15px 16px 15px 16px;
`

type GroupProps = {
  name: string
  count: string
  'data-qa': string
  selected: boolean
  utilization?: number
  onClick: () => void
}

const Group = ({
  name,
  count,
  selected,
  utilization,
  onClick,
  'data-qa': dataQa
}: GroupProps) => (
  <GroupContainer onClick={onClick} data-qa={dataQa} selected={selected}>
    <SelectionIndicator selected={selected}>
      <FontAwesomeIcon icon={faCheck} />
    </SelectionIndicator>
    <GroupName>{name}</GroupName>
    {utilization !== undefined ? (
      <Utilization warn={utilization >= 100}>
        {utilization.toFixed ? utilization.toFixed(1) : '∞'}%
      </Utilization>
    ) : (
      count
    )}
  </GroupContainer>
)

const GroupContainer = styled(FixedSpaceRow)<{ selected: boolean }>`
  padding: 15px 16px 15px 16px;
  border-bottom: 1px solid ${colors.grayscale.g15};
  color: ${(p) => (p.selected ? colors.main.m1 : colors.grayscale.g70)};
  font-size: ${fontSizesMobile.h2};
  font-weight: ${theme.typography.h2.mobile?.weight ??
  theme.typography.h2.weight};
  cursor: pointer;
`

const SelectionIndicator = styled.span<{ selected: boolean }>`
  padding-right: 16px;
  opacity: ${(p) => (p.selected ? 1 : 0)};
`

const GroupName = styled.span`
  flex: 1;
`

const Utilization = styled.span<{ warn: boolean }>`
  align-self: right;
  color: ${(p) => (p.warn ? colors.status.danger : 'inherit')};
`

const Wrapper = styled.div`
  max-height: 60vh;
  overflow: auto;
`
