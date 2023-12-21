// SPDX-FileCopyrightText: 2017-2022 City of Espoo
//
// SPDX-License-Identifier: LGPL-2.1-or-later

import React from 'react'

import { AbsenceType } from 'lib-common/generated/api-types/daycare'
import RoundIcon from 'lib-components/atoms/RoundIcon'
import IconButton from 'lib-components/atoms/buttons/IconButton'
import { FixedSpaceRow } from 'lib-components/layout/flex-helpers'
import { absenceColors, absenceIcons } from 'lib-customizations/common'
import { faTimes } from 'lib-icons'

import { useTranslation } from '../../../state/i18n'

interface Props {
  type: AbsenceType
  onDelete?: () => void
}

export default React.memo(function AbsenceDay({ type, onDelete }: Props) {
  const { i18n } = useTranslation()
  return (
    <FixedSpaceRow spacing="xs" alignItems="center" data-qa="absence">
      <RoundIcon
        content={absenceIcons[type]}
        color={absenceColors[type]}
        size="m"
      />
      <span>{i18n.absences.absenceTypesShort[type]}</span>
      {onDelete && (
        <IconButton
          icon={faTimes}
          onClick={onDelete}
          aria-label={i18n.common.remove}
        />
      )}
    </FixedSpaceRow>
  )
})
