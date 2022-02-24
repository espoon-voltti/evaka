// SPDX-FileCopyrightText: 2017-2022 City of Espoo
//
// SPDX-License-Identifier: LGPL-2.1-or-later

import React from 'react'

import LocalDate from 'lib-common/local-date'
import RoundIcon from 'lib-components/atoms/RoundIcon'
import Tooltip, { TooltipProps } from 'lib-components/atoms/Tooltip'
import colors from 'lib-customizations/common'
import { fasArrowDown, fasArrowUp } from 'lib-icons'

import { useTranslation } from '../../state/i18n'

type Props = {
  position?: TooltipProps['position']
  tooltipText?: 'placement-start'
} & ({ isUnder3: boolean } | { dateOfBirth: LocalDate })

export const AgeIndicatorIcon = React.memo(function AgeIndicatorIconTooltip({
  tooltipText,
  ...props
}: Props) {
  const { i18n } = useTranslation()

  const under3 =
    'isUnder3' in props
      ? props.isUnder3
      : LocalDate.today().differenceInYears(props.dateOfBirth) < 3

  const tooltip =
    tooltipText === 'placement-start'
      ? under3
        ? i18n.ageIndicator.under3AtPlacementStart
        : i18n.ageIndicator.over3AtPlacementStart
      : under3
      ? i18n.ageIndicator.under3
      : i18n.ageIndicator.over3

  return (
    <Tooltip tooltip={<span>{tooltip}</span>} position={props.position}>
      <RoundIcon
        content={under3 ? fasArrowDown : fasArrowUp}
        color={under3 ? colors.status.success : colors.main.m1}
        size="s"
      />
    </Tooltip>
  )
})
