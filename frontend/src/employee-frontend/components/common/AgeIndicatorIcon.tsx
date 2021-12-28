// SPDX-FileCopyrightText: 2017-2020 City of Espoo
//
// SPDX-License-Identifier: LGPL-2.1-or-later

import React from 'react'
import { fasArrowDown, fasArrowUp } from 'lib-icons'
import LocalDate from 'lib-common/local-date'
import RoundIcon from 'lib-components/atoms/RoundIcon'
import colors from 'lib-customizations/common'

type Props = { isUnder3: boolean } | { dateOfBirth: LocalDate }

export default React.memo(function AgeIndicatorIcon(props: Props) {
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
