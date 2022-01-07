// SPDX-FileCopyrightText: 2017-2020 City of Espoo
//
// SPDX-License-Identifier: LGPL-2.1-or-later

import { fasArrowDown, fasArrowUp } from 'lib-icons'
import React from 'react'
import LocalDate from 'lib-common/local-date'
import RoundIcon from 'lib-components/atoms/RoundIcon'
import Tooltip from 'lib-components/atoms/Tooltip'
import colors from 'lib-customizations/common'
import { useTranslation } from '../../state/i18n'

type Props = { isUnder3: boolean } | { dateOfBirth: LocalDate }

export const AgeIndicatorIcon = React.memo(function AgeIndicatorIcon(
  props: Props
) {
  const under3 =
    'isUnder3' in props
      ? props.isUnder3
      : LocalDate.today().differenceInYears(props.dateOfBirth) < 3

  return (
    <RoundIcon
      content={under3 ? fasArrowDown : fasArrowUp}
      color={under3 ? colors.accents.successGreen : colors.main.dark}
      size="s"
    />
  )
})

export default AgeIndicatorIcon

export const AgeIndicatorIconWithTooltip = React.memo(
  function AgeIndicatorIconTooltip(props: Props) {
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
      >
        <AgeIndicatorIcon isUnder3={under3} />
      </Tooltip>
    )
  }
)
