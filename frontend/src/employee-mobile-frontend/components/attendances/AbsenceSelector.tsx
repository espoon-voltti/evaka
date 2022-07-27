// SPDX-FileCopyrightText: 2017-2022 City of Espoo
//
// SPDX-License-Identifier: LGPL-2.1-or-later

import React, { Fragment } from 'react'

import type { AbsenceType } from 'lib-common/generated/api-types/daycare'
import { ChipWrapper, ChoiceChip } from 'lib-components/atoms/Chip'
import { Gap } from 'lib-components/white-space'

import { useTranslation } from '../../state/i18n'

const allSelectableAbsenceTypes: AbsenceType[] = [
  'OTHER_ABSENCE',
  'SICKLEAVE',
  'UNKNOWN_ABSENCE',
  'PLANNED_ABSENCE'
]
const selectableAbsenceTypesWithoutUnknown = allSelectableAbsenceTypes.filter(
  (type) => type !== 'UNKNOWN_ABSENCE'
)

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
  noUnknownAbsences = false
}: Props) {
  const { i18n } = useTranslation()

  const absenceTypes = noUnknownAbsences
    ? selectableAbsenceTypesWithoutUnknown
    : allSelectableAbsenceTypes

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
            <Gap horizontal size="xxs" />
          </Fragment>
        ))}
      </ChipWrapper>
    </Fragment>
  )
}
