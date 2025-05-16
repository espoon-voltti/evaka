// SPDX-FileCopyrightText: 2017-2022 City of Espoo
//
// SPDX-License-Identifier: LGPL-2.1-or-later

import React from 'react'
import { useTheme } from 'styled-components'

import { FeeDecisionDifference } from 'lib-common/generated/api-types/invoicing'
import RoundIcon from 'lib-components/atoms/RoundIcon'
import Tooltip from 'lib-components/atoms/Tooltip'

import { useTranslation } from '../../state/i18n'

interface IconsProps {
  difference: readonly FeeDecisionDifference[]
  toggle?: (difference: FeeDecisionDifference) => void
  toggled?: FeeDecisionDifference[]
}

export const FeeDecisionDifferenceIcons = (props: IconsProps) => {
  const { i18n, lang } = useTranslation()
  return (
    <>
      {props.difference
        .map((difference) => ({
          value: difference,
          label: i18n.feeDecisions.table.difference.value[difference],
          labelShort: i18n.feeDecisions.table.difference.valueShort[difference]
        }))
        .sort((a, b) => a.labelShort.localeCompare(b.labelShort, lang))
        .map((item) => (
          <FeeDecisionDifferenceIcon
            key={item.value}
            item={item}
            selected={props.toggled?.includes(item.value)}
            toggle={props.toggle}
          />
        ))}
    </>
  )
}

interface IconProps {
  item: {
    value: FeeDecisionDifference
    label: string
    labelShort: string
  }
  selected?: boolean
  toggle?: (difference: FeeDecisionDifference) => void
}

export const FeeDecisionDifferenceIcon = (props: IconProps) => {
  const theme = useTheme()
  return (
    <Tooltip key={props.item.value} tooltip={props.item.label} position="top">
      <RoundIcon
        content={props.item.labelShort}
        color={theme.colors.main.m1}
        size="L"
        onClick={() =>
          props.toggle !== undefined
            ? props.toggle(props.item.value)
            : undefined
        }
        active={props.selected}
      />
    </Tooltip>
  )
}
