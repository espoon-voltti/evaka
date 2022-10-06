// SPDX-FileCopyrightText: 2017-2022 City of Espoo
//
// SPDX-License-Identifier: LGPL-2.1-or-later

import React from 'react'
import { useTheme } from 'styled-components'

import { useTranslation } from 'employee-frontend/state/i18n'
import { VoucherValueDecisionDifference } from 'lib-common/generated/api-types/invoicing'
import RoundIcon from 'lib-components/atoms/RoundIcon'
import Tooltip from 'lib-components/atoms/Tooltip'

interface Props {
  value: VoucherValueDecisionDifference
  selected?: boolean
  toggle?: (difference: VoucherValueDecisionDifference) => () => void
}

export const VoucherValueDecisionDifferenceIcon = (props: Props) => {
  const { i18n } = useTranslation()
  const theme = useTheme()
  return (
    <Tooltip
      key={props.value}
      tooltip={i18n.valueDecisions.table.difference.value[props.value]}
      position="top"
    >
      <RoundIcon
        content={i18n.valueDecisions.table.difference.valueShort[props.value]}
        color={theme.colors.main.m1}
        size="L"
        onClick={
          props.toggle !== undefined ? props.toggle(props.value) : undefined
        }
        active={props.selected}
      />
    </Tooltip>
  )
}
