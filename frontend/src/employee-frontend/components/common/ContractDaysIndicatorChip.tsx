// SPDX-FileCopyrightText: 2017-2022 City of Espoo
//
// SPDX-License-Identifier: LGPL-2.1-or-later

import React from 'react'
import styled from 'styled-components'

import { ChildServiceNeedInfo } from 'lib-common/generated/api-types/absence'
import Tooltip from 'lib-components/atoms/Tooltip'
import { defaultMargins } from 'lib-components/white-space'
import colors from 'lib-customizations/common'

import { OneLetterChip } from './OneLetterChip'

type Props = { contractDayServiceNeeds: ChildServiceNeedInfo[] }

const TooltipP = styled.p`
  margin-top: 0;
  margin-bottom: 0;
  width: 100%;
`
const TooltipDiv = styled.div`
  :not(:first-child) {
    margin-top: ${defaultMargins.xs};
  }
  white-space: nowrap;
  margin-left: 0;
`

export const ContractDaysIndicatorChip = React.memo(
  function ContractDaysIndicatorChip({ contractDayServiceNeeds }: Props) {
    return (
      <Tooltip
        position="right"
        width="large"
        tooltip={contractDayServiceNeeds.map((c, i) => (
          <TooltipDiv key={i}>
            <TooltipP>{`${c.optionName}:`}</TooltipP>
            <TooltipP>
              {c.validDuring.start.format()} - {c.validDuring.end.format()}
            </TooltipP>
          </TooltipDiv>
        ))}
      >
        <OneLetterChip color={colors.accents.a1greenDark}>S</OneLetterChip>
      </Tooltip>
    )
  }
)
