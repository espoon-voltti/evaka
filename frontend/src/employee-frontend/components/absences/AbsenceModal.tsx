// SPDX-FileCopyrightText: 2017-2022 City of Espoo
//
// SPDX-License-Identifier: LGPL-2.1-or-later

import React from 'react'

import {
  AbsenceCategory,
  AbsenceType
} from 'lib-common/generated/api-types/daycare'
import Checkbox from 'lib-components/atoms/form/Checkbox'
import Radio from 'lib-components/atoms/form/Radio'
import { FixedSpaceColumn } from 'lib-components/layout/flex-helpers'
import FormModal from 'lib-components/molecules/modals/FormModal'
import { Label } from 'lib-components/typography'
import { absenceTypes } from 'lib-customizations/employee'

import { useTranslation } from '../../state/i18n'
import { absenceCategories } from '../../types/absence'

export default React.memo(function AbsenceModal({
  onSave,
  saveDisabled,
  onCancel,
  selectedAbsenceType,
  setSelectedAbsenceType,
  selectedCategories,
  updateCategories
}: {
  onSave: () => void
  saveDisabled: boolean
  onCancel: () => void
  selectedAbsenceType: AbsenceType | null
  setSelectedAbsenceType: (value: AbsenceType | null) => void
  selectedCategories: AbsenceCategory[]
  updateCategories: (value: AbsenceCategory) => void
}) {
  const { i18n } = useTranslation()
  return (
    <FormModal
      title=""
      resolveAction={onSave}
      resolveLabel={i18n.absences.modal.saveButton}
      resolveDisabled={saveDisabled}
      rejectAction={onCancel}
      rejectLabel={i18n.absences.modal.cancelButton}
      data-qa="absence-modal"
    >
      <FixedSpaceColumn spacing="L">
        <Label>{i18n.absences.modal.absenceSectionLabel}</Label>
        <FixedSpaceColumn spacing="xs">
          {absenceTypes.map((absenceType, index) => (
            <Radio
              key={index}
              id={absenceType}
              label={i18n.absences.modal.absenceTypes[absenceType]}
              checked={selectedAbsenceType === absenceType}
              onChange={() => setSelectedAbsenceType(absenceType)}
              data-qa={`absence-type-${absenceType}`}
            />
          ))}
          <Radio
            id="NO_ABSENCE"
            label={i18n.absences.modal.absenceTypes.NO_ABSENCE}
            checked={selectedAbsenceType === null}
            onChange={() => setSelectedAbsenceType(null)}
            data-qa="absence-type-NO_ABSENCE"
          />
        </FixedSpaceColumn>

        <Label>{i18n.absences.modal.placementSectionLabel}</Label>
        <FixedSpaceColumn spacing="xs">
          {absenceCategories.map((category, index) => (
            <Checkbox
              key={index}
              label={i18n.absences.absenceCategories[category]}
              checked={selectedCategories.includes(category)}
              onChange={() => updateCategories(category)}
              data-qa={`absences-select-${category}`}
            />
          ))}
        </FixedSpaceColumn>
      </FixedSpaceColumn>
    </FormModal>
  )
})
