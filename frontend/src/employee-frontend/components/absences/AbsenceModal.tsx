// SPDX-FileCopyrightText: 2017-2021 City of Espoo
//
// SPDX-License-Identifier: LGPL-2.1-or-later

import React from 'react'
import { AbsenceType } from 'lib-common/generated/enums'
import {
  AbsenceTypes,
  CareTypeCategories,
  CareTypeCategory
} from '../../types/absence'
import { useTranslation } from '../../state/i18n'
import FormModal from 'lib-components/molecules/modals/FormModal'
import { FixedSpaceColumn } from 'lib-components/layout/flex-helpers'
import { Label, LabelText } from '../common/styled/common'
import Radio from 'lib-components/atoms/form/Radio'
import Checkbox from 'lib-components/atoms/form/Checkbox'

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
      className="absence-modal"
      resolve={{
        action: onSave,
        label: i18n.absences.modal.saveButton,
        disabled: saveDisabled
      }}
      reject={{
        action: onCancel,
        label: i18n.absences.modal.cancelButton
      }}
    >
      <FixedSpaceColumn spacing={'L'}>
        <Label>
          <LabelText>{i18n.absences.modal.absenceSectionLabel}</LabelText>
        </Label>
        <FixedSpaceColumn spacing={'xs'}>
          {AbsenceTypes.map((absenceType, index) => (
            <Radio
              key={index}
              id={absenceType}
              label={i18n.absences.modal.absenceTypes[absenceType]}
              checked={selectedAbsenceType === absenceType}
              onChange={() => setSelectedAbsenceType(absenceType)}
            />
          ))}
          <Radio
            id={'PRESENCE'}
            label={i18n.absences.modal.absenceTypes.PRESENCE}
            checked={selectedAbsenceType === null}
            onChange={() => setSelectedAbsenceType(null)}
          />
        </FixedSpaceColumn>

        <Label>
          <LabelText>{i18n.absences.modal.placementSectionLabel}</LabelText>
        </Label>
        <FixedSpaceColumn spacing={'xs'}>
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
