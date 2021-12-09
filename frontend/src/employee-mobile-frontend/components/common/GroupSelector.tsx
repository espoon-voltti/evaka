// SPDX-FileCopyrightText: 2017-2021 City of Espoo
//
// SPDX-License-Identifier: LGPL-2.1-or-later

import React, { Fragment, useContext } from 'react'
import styled from 'styled-components'
import { ChipWrapper, ChoiceChip } from 'lib-components/atoms/Chip'
import { Gap } from 'lib-components/white-space'
import { fontWeights } from 'lib-components/typography'
import { useTranslation } from '../../state/i18n'
import colors from 'lib-customizations/common'
import { UnitContext } from '../../state/unit'
import { UUID } from 'lib-common/types'
import { GroupInfo } from 'lib-common/generated/api-types/attendance'
import { renderResult } from '../async-rendering'

interface GroupSelectorProps {
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
  selectedGroup,
  onChangeGroup,
  countInfo,
  groups,
  includeSelectAll
}: GroupSelectorProps) {
  const { i18n } = useTranslation()

  const { unitInfoResponse } = useContext(UnitContext)

  return renderResult(unitInfoResponse, (unitInfo) => (
    <Wrapper>
      <ChipWrapper>
        <>
          {includeSelectAll && (
            <ChoiceChip
              text={`${i18n.common.all}${
                countInfo
                  ? `(${countInfo.getPresentCount(
                      undefined
                    )}/${countInfo.getTotalCount(undefined)})`
                  : ''
              }`}
              selected={!selectedGroup}
              onChange={() => onChangeGroup(undefined)}
              data-qa="group--all"
            />
          )}
          <Gap horizontal size={'xxs'} />
          {(groups || unitInfo.groups).map((group) => (
            <Fragment key={group.id}>
              <ChoiceChip
                text={`${group.name}${
                  countInfo
                    ? `(${countInfo.getPresentCount(
                        group.id
                      )}/${countInfo.getTotalCount(group.id)})`
                    : ''
                }`}
                selected={selectedGroup ? selectedGroup.id === group.id : false}
                onChange={() => {
                  onChangeGroup(group)
                }}
                data-qa={`group--${group.id}`}
              />
              <Gap horizontal size={'xxs'} />
            </Fragment>
          ))}
        </>
        {countInfo && <Info>{countInfo.infoText}</Info>}
      </ChipWrapper>
    </Wrapper>
  ))
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
