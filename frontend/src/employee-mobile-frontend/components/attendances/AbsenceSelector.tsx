// SPDX-FileCopyrightText: 2017-2021 City of Espoo
//
// SPDX-License-Identifier: LGPL-2.1-or-later

import React, { Fragment } from 'react'

import { FixedSpaceColumn } from '@evaka/lib-components/layout/flex-helpers'
import Radio from '@evaka/lib-components/atoms/form/Radio'
import { useTranslation } from '../../state/i18n'
import { AbsenceType, AbsenceTypes } from '../../types'

interface Props {
  selectedAbsenceType: AbsenceType | undefined
  setSelectedAbsenceType: React.Dispatch<
    React.SetStateAction<AbsenceType | undefined>
  >
}

export default function AbsenceSelector({
  selectedAbsenceType,
  setSelectedAbsenceType
}: Props) {
  const { i18n } = useTranslation()

  return (
    <Fragment>
      <FixedSpaceColumn spacing={'s'}>
        {AbsenceTypes.filter(
          (absenceType) =>
            absenceType !== 'PRESENCE' &&
            absenceType !== 'PARENTLEAVE' &&
            absenceType !== 'FORCE_MAJEURE'
        ).map((absenceType) => (
          <Radio
            key={absenceType}
            label={i18n.absences.absenceTypes[absenceType]}
            onChange={() => setSelectedAbsenceType(absenceType)}
            checked={selectedAbsenceType === absenceType}
            data-qa={`mark-absent-${absenceType}`}
          />
        ))}
      </FixedSpaceColumn>
    </Fragment>
  )
}
