// SPDX-FileCopyrightText: 2017-2021 City of Espoo
//
// SPDX-License-Identifier: LGPL-2.1-or-later

import React, { Fragment } from 'react'

import { useTranslation } from '../../state/i18n'
import { AbsenceType, AbsenceTypes } from '../../types'
import { ChipWrapper, ChoiceChip } from 'lib-components/atoms/Chip'
import { Gap } from 'lib-components/white-space'

interface Props {
  selectedAbsenceType: AbsenceType | undefined
  setSelectedAbsenceType: React.Dispatch<
    React.SetStateAction<AbsenceType | undefined>
  >
  noUnknownAbsences?: boolean
}

export default function AbsenceSelector({
  selectedAbsenceType,
  setSelectedAbsenceType,
  noUnknownAbsences
}: Props) {
  const { i18n } = useTranslation()

  const absenceTypes = AbsenceTypes.filter(
    (type) =>
      type !== 'NO_ABSENCE' &&
      type !== 'PARENTLEAVE' &&
      type !== 'FORCE_MAJEURE' &&
      !(noUnknownAbsences && type === 'UNKNOWN_ABSENCE')
  )

  return (
    <Fragment>
      <ChipWrapper>
        {absenceTypes.map((absenceType) => (
          <Fragment key={absenceType}>
            <ChoiceChip
              text={i18n.absences.absenceTypes[absenceType]}
              selected={selectedAbsenceType === absenceType}
              onChange={() => setSelectedAbsenceType(absenceType)}
              data-qa={`mark-absent-${absenceType}`}
            />
            <Gap horizontal size={'xxs'} />
          </Fragment>
        ))}
      </ChipWrapper>
    </Fragment>
  )
}
