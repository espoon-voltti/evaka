// SPDX-FileCopyrightText: 2017-2021 City of Espoo
//
// SPDX-License-Identifier: LGPL-2.1-or-later

import React, { Fragment } from 'react'

import { useTranslation } from '../../state/i18n'
import { AbsenceType, AbsenceTypes } from '../../types'
import { ChoiceChip } from 'lib-components/atoms/Chip'
import { Gap } from 'lib-components/white-space'
import { ChipWrapper } from '../mobile/components'

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

  let absenceTypes = AbsenceTypes.filter(
    (absenceType) =>
      absenceType !== 'PRESENCE' &&
      absenceType !== 'PARENTLEAVE' &&
      absenceType !== 'FORCE_MAJEURE'
  )

  if (noUnknownAbsences) {
    absenceTypes = absenceTypes.filter(
      (absenceType) => absenceType !== 'UNKNOWN_ABSENCE'
    )
  }

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
