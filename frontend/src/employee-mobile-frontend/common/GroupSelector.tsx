// SPDX-FileCopyrightText: 2017-2022 City of Espoo
//
// SPDX-License-Identifier: LGPL-2.1-or-later

import { FontAwesomeIcon } from '@fortawesome/react-fontawesome'
import styled from 'styled-components'

import type { GroupInfo } from 'lib-common/generated/api-types/attendance'
import type { DaycareId } from 'lib-common/generated/api-types/shared'
import { useQueryResult } from 'lib-common/query'
import type { UUID } from 'lib-common/types'
import { FixedSpaceRow } from 'lib-components/layout/flex-helpers'
import { fontSizesMobile, fontWeights } from 'lib-components/typography'
import { defaultMargins } from 'lib-components/white-space'
import colors from 'lib-customizations/common'
import { faCheck } from 'lib-icons'

import { renderResult } from '../async-rendering'
import { unitInfoQuery } from '../units/queries'

import { useTranslation } from './i18n'

export interface GroupSelectorProps {
  unitId: DaycareId
  selectedGroup: GroupInfo | undefined
  onChangeGroup: (group: GroupInfo | undefined) => void
  countInfo?: CountInfo
  groups?: GroupInfo[]
  includeSelectAll: boolean
  shiftCareSelected?: boolean
  onSelectShiftCare?: () => void
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
  includeSelectAll,
  shiftCareSelected,
  onSelectShiftCare
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
            selected={!selectedGroup && !shiftCareSelected}
            onClick={() => onChangeGroup(undefined)}
            data-qa="group--all"
            name={i18n.common.allGroups}
            utilization={
              realtimeStaffAttendanceEnabled
                ? {
                    percentage: unitInfo.utilization
                  }
                : undefined
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
              realtimeStaffAttendanceEnabled
                ? {
                    childCapacity: group.childCapacity,
                    staffCapacity: group.staffCapacity,
                    percentage: group.utilization
                  }
                : undefined
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
        {onSelectShiftCare && (
          <Group
            selected={shiftCareSelected ?? false}
            onClick={onSelectShiftCare}
            data-qa="group--shift-care"
            name={i18n.common.shiftCare}
            count=""
          />
        )}
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
  padding: 15px ${defaultMargins.s};
`

type GroupProps = {
  name: string
  count: string
  'data-qa': string
  selected: boolean
  utilization?: {
    childCapacity?: number
    staffCapacity?: number
    percentage: number
  }
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
  <GroupContainer onClick={onClick} data-qa={dataQa} $selected={selected}>
    <SelectionIndicator $selected={selected}>
      <FontAwesomeIcon icon={faCheck} />
    </SelectionIndicator>
    <GroupName>{name}</GroupName>
    {utilization !== undefined ? (
      <Utilization $warn={utilization.percentage >= 100}>
        {utilization.percentage.toFixed
          ? utilization.percentage.toFixed(1)
          : '∞'}
        %
        <br />
        {utilization.childCapacity !== undefined &&
          utilization.staffCapacity !== undefined && (
            <Capacity>
              ({utilization.childCapacity.toFixed(2)} /{' '}
              {utilization.staffCapacity})
            </Capacity>
          )}
      </Utilization>
    ) : (
      <Count>{count}</Count>
    )}
  </GroupContainer>
)

const GroupContainer = styled(FixedSpaceRow)<{ $selected: boolean }>`
  align-items: center;
  min-height: 56px;
  &:first-child {
    min-height: 47px;
  }
  padding: 0 ${defaultMargins.s};
  border-bottom: 1px solid ${colors.grayscale.g15};
  &:last-child {
    border-bottom: none;
  }
  color: ${(p) => (p.$selected ? colors.main.m1 : colors.grayscale.g70)};
  font-family: Montserrat, sans-serif;
  font-size: ${fontSizesMobile.h3};
  font-weight: ${fontWeights.semibold};
  cursor: pointer;
`

export const IconSlot = styled.span`
  display: flex;
  justify-content: center;
  width: 24px;
  flex-shrink: 0;
`

const SelectionIndicator = styled(IconSlot)<{ $selected: boolean }>`
  opacity: ${(p) => (p.$selected ? 1 : 0)};
`

const GroupName = styled.span`
  flex: 1;
`

const Utilization = styled.span<{ $warn: boolean }>`
  text-align: right;
  font-size: 14px;
  color: ${(p) => (p.$warn ? colors.status.danger : 'inherit')};
`

const Capacity = styled.span`
  font-size: 12px;
  font-weight: ${fontWeights.normal};
`

const Count = styled.span`
  font-size: 14px;
`

export const listMaxHeightVh = 60

const Wrapper = styled.div`
  max-height: ${listMaxHeightVh}vh;
  overflow: auto;
`
