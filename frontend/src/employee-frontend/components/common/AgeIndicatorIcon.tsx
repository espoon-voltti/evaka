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

type Props = { position?: TooltipProps['position'] } & (
  | { isUnder3: boolean }
  | { dateOfBirth: LocalDate }
)

export const AgeIndicatorIcon = React.memo(function AgeIndicatorIconTooltip(
  props: Props
) {
  const { i18n } = useTranslation()

  const under3 =
    'isUnder3' in props
      ? props.isUnder3
      : LocalDate.today().differenceInYears(props.dateOfBirth) < 3

  return (
    <Tooltip
      tooltip={
        <span>
          {under3
            ? i18n.applications.list.lessthan3
            : i18n.applications.list.morethan3}
        </span>
      }
      position={props.position}
    >
      <RoundIcon
        content={under3 ? fasArrowDown : fasArrowUp}
        color={under3 ? colors.status.success : colors.main.m1}
        size="s"
      />
    </Tooltip>
  )
})
