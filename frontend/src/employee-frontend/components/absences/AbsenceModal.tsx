// SPDX-FileCopyrightText: 2017-2022 City of Espoo
//
// SPDX-License-Identifier: LGPL-2.1-or-later

import React, { useCallback, useState } from 'react'

import { Result } from 'lib-common/api'
import {
  AbsenceCategory,
  AbsenceType
} from 'lib-common/generated/api-types/absence'
import Checkbox from 'lib-components/atoms/form/Checkbox'
import Radio from 'lib-components/atoms/form/Radio'
import { FixedSpaceColumn } from 'lib-components/layout/flex-helpers'
import { AsyncFormModal } from 'lib-components/molecules/modals/FormModal'
import { Label } from 'lib-components/typography'
import { absenceTypes } from 'lib-customizations/employee'

import { useTranslation } from '../../state/i18n'
import { AbsenceUpdate } from '../../types/absence'

const defaultAbsenceType = 'SICKLEAVE'
const defaultAbsenceCategories: AbsenceCategory[] = []
const absenceCategories: AbsenceCategory[] = ['NONBILLABLE', 'BILLABLE']

type AbsenceTypeState =
  | { type: 'absence'; absenceType: AbsenceType }
  | { type: 'noAbsence' }
  | { type: 'missingHolidayReservation' }

export default React.memo(function AbsenceModal({
  showCategorySelection,
  showMissingHolidayReservation,
  onSave,
  onSuccess,
  onClose
}: {
  showCategorySelection: boolean
  showMissingHolidayReservation: boolean
  onSave: (value: AbsenceUpdate) => Promise<Result<unknown>>
  onSuccess: () => void
  onClose: () => void
}) {
  const { i18n } = useTranslation()

  const [selectedAbsenceType, setSelectedAbsenceType] =
    useState<AbsenceTypeState>({
      type: 'absence',
      absenceType: defaultAbsenceType
    })
  const [selectedCategories, setSelectedCategories] = useState(
    defaultAbsenceCategories
  )

  const updateCategories = useCallback((category: AbsenceCategory) => {
    setSelectedCategories((categories) =>
      categories.includes(category)
        ? categories.filter((c) => c !== category)
        : [...categories, category]
    )
  }, [])

  return (
    <AsyncFormModal
      title=""
      resolveAction={() =>
        onSave(
          selectedAbsenceType.type === 'missingHolidayReservation'
            ? { type: 'missingHolidayReservation' }
            : { ...selectedAbsenceType, absenceCategories: selectedCategories }
        )
      }
      onSuccess={onSuccess}
      resolveLabel={i18n.absences.modal.saveButton}
      resolveDisabled={showCategorySelection && selectedCategories.length === 0}
      rejectAction={onClose}
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
              checked={
                selectedAbsenceType.type === 'absence' &&
                selectedAbsenceType.absenceType === absenceType
              }
              onChange={() =>
                setSelectedAbsenceType({ type: 'absence', absenceType })
              }
              data-qa={`absence-type-${absenceType}`}
            />
          ))}
          <Radio
            id="NO_ABSENCE"
            label={i18n.absences.modal.absenceTypes.NO_ABSENCE}
            checked={selectedAbsenceType.type === 'noAbsence'}
            onChange={() => setSelectedAbsenceType({ type: 'noAbsence' })}
            data-qa="absence-type-NO_ABSENCE"
          />
          {showMissingHolidayReservation ? (
            <Radio
              id="MISSING_HOLIDAY_RESERVATION"
              label={
                i18n.absences.modal.absenceTypes.MISSING_HOLIDAY_RESERVATION
              }
              checked={selectedAbsenceType.type === 'missingHolidayReservation'}
              onChange={() =>
                setSelectedAbsenceType({ type: 'missingHolidayReservation' })
              }
              data-qa="absence-type-MISSING_HOLIDAY_RESERVATION"
            />
          ) : null}
        </FixedSpaceColumn>

        {showCategorySelection && (
          <>
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
          </>
        )}
      </FixedSpaceColumn>
    </AsyncFormModal>
  )
})
