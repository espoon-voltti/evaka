// SPDX-FileCopyrightText: 2017-2022 City of Espoo
//
// SPDX-License-Identifier: LGPL-2.1-or-later

import React, { Fragment } from 'react'

import { AbsenceType } from 'lib-common/generated/api-types/absence'
import { ChipWrapper, SelectionChip } from 'lib-components/atoms/Chip'
import { Gap } from 'lib-components/white-space'

import { useTranslation } from '../../common/i18n'

export type AbsenceTypeWithNoAbsence = AbsenceType | 'NO_ABSENCE'

interface Props {
  absenceTypes: AbsenceTypeWithNoAbsence[]
  selectedAbsenceType: AbsenceTypeWithNoAbsence | undefined
  setSelectedAbsenceType: (type: AbsenceTypeWithNoAbsence) => void
}

export default function AbsenceSelector({
  absenceTypes,
  selectedAbsenceType,
  setSelectedAbsenceType
}: Props) {
  const { i18n } = useTranslation()

  return (
    <ChipWrapper margin="xxs">
      {absenceTypes.map((absenceType) => (
        <Fragment key={absenceType}>
          <SelectionChip
            text={i18n.absences.absenceTypes[absenceType]}
            selected={selectedAbsenceType === absenceType}
            onChange={() => setSelectedAbsenceType(absenceType)}
            data-qa={`mark-absent-${absenceType}`}
            hideIcon
          />
          <Gap horizontal size="xxs" />
        </Fragment>
      ))}
    </ChipWrapper>
  )
}
