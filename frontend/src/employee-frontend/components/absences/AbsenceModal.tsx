// SPDX-FileCopyrightText: 2017-2021 City of Espoo
//
// SPDX-License-Identifier: LGPL-2.1-or-later

import React from 'react'
import { AbsenceType } from 'lib-common/generated/enums'
import Checkbox from 'lib-components/atoms/form/Checkbox'
import Radio from 'lib-components/atoms/form/Radio'
import { FixedSpaceColumn } from 'lib-components/layout/flex-helpers'
import FormModal from 'lib-components/molecules/modals/FormModal'
import { Label } from 'lib-components/typography'
import { useTranslation } from '../../state/i18n'
import {
  AbsenceTypes,
  CareTypeCategories,
  CareTypeCategory
} from '../../types/absence'

export default React.memo(function AbsenceModal({
  onSave,
  saveDisabled,
  onCancel,
  selectedAbsenceType,
  setSelectedAbsenceType,
  selectedCareTypeCategories,
  updateCareTypes
}: {
  onSave: () => void
  saveDisabled: boolean
  onCancel: () => void
  selectedAbsenceType: AbsenceType | null
  setSelectedAbsenceType: (value: AbsenceType | null) => void
  selectedCareTypeCategories: CareTypeCategory[]
  updateCareTypes: (value: CareTypeCategory) => void
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
          {AbsenceTypes.map((absenceType, index) => (
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
          {CareTypeCategories.map((careTypeCategory, index) => (
            <Checkbox
              key={index}
              label={i18n.absences.careTypeCategories[careTypeCategory]}
              checked={selectedCareTypeCategories.includes(careTypeCategory)}
              onChange={() => updateCareTypes(careTypeCategory)}
              data-qa={`absences-select-caretype-${careTypeCategory}`}
            />
          ))}
        </FixedSpaceColumn>
      </FixedSpaceColumn>
    </FormModal>
  )
})
