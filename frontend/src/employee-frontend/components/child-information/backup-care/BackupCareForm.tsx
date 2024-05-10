// SPDX-FileCopyrightText: 2017-2022 City of Espoo
//
// SPDX-License-Identifier: LGPL-2.1-or-later

import orderBy from 'lodash/orderBy'
import React, { useCallback, useContext, useMemo, useState } from 'react'
import styled from 'styled-components'

import FiniteDateRange from 'lib-common/finite-date-range'
import { UpdateStateFn } from 'lib-common/form-state'
import {
  BackupCareUnit,
  ChildBackupCare
} from 'lib-common/generated/api-types/backupcare'
import { UnitStub } from 'lib-common/generated/api-types/daycare'
import LocalDate from 'lib-common/local-date'
import {
  first,
  second,
  useQueryResult,
  useSelectMutation
} from 'lib-common/query'
import { UUID } from 'lib-common/types'
import Button from 'lib-components/atoms/buttons/Button'
import MutateButton, {
  cancelMutation
} from 'lib-components/atoms/buttons/MutateButton'
import Combobox from 'lib-components/atoms/dropdowns/Combobox'
import { FixedSpaceRow } from 'lib-components/layout/flex-helpers'
import { DatePickerDeprecated } from 'lib-components/molecules/DatePickerDeprecated'
import { fontWeights } from 'lib-components/typography'

import { ChildContext } from '../../../state'
import { useTranslation } from '../../../state/i18n'
import { UIContext } from '../../../state/ui'
import { DateRange } from '../../../utils/date'
import {
  isDateRangeInverted,
  isDateRangeOverlappingWithExisting
} from '../../../utils/validation/validations'
import {
  createBackupCareMutation,
  updateBackupCareMutation
} from '../../unit/queries'
import { unitsQuery } from '../queries'

export interface Props {
  childId: UUID
  backupCare?: ChildBackupCare
}

interface FormState {
  unit: UnitStub | BackupCareUnit | undefined
  startDate: LocalDate
  endDate: LocalDate
}

const FormField = styled.div`
  display: flex;
  align-items: center;
  margin: 20px 0;
`

const FormLabel = styled.div`
  font-weight: ${fontWeights.semibold};
  flex: 0 1 auto;
  width: 300px;
`

const Unit = styled.div`
  flex: 1 1 auto;
`

const ActionButtons = styled(FixedSpaceRow)`
  display: flex;
  align-items: center;
  justify-content: flex-end;
`

export default function BackupCareForm({ childId, backupCare }: Props) {
  const { i18n } = useTranslation()
  const { uiMode, clearUiMode } = useContext(UIContext)
  const { backupCares, consecutivePlacementRanges, loadBackupCares } =
    useContext(ChildContext)

  const units = useQueryResult(
    unitsQuery({
      areaIds: null,
      type: 'DAYCARE',
      from: LocalDate.todayInHelsinkiTz()
    })
  )

  const initialFormState: FormState = {
    unit: backupCare?.unit,
    startDate: backupCare?.period.start ?? LocalDate.todayInSystemTz(),
    endDate: backupCare?.period.end ?? LocalDate.todayInSystemTz()
  }

  const [formState, setFormState] = useState<FormState>(initialFormState)

  const evaluateFormErrors = (form: FormState) => {
    const errors: string[] = []
    const existing: DateRange[] = backupCares
      .map((bcs) =>
        bcs
          .filter(
            (it) =>
              backupCare == undefined || it.backupCare.id !== backupCare.id
          )
          .map(({ backupCare: { period } }) => ({
            startDate: period.start,
            endDate: period.end
          }))
      )
      .getOrElse([])

    if (isDateRangeInverted(form))
      errors.push(i18n.validationError.invertedDateRange)
    else if (isDateRangeOverlappingWithExisting(form, existing))
      errors.push(i18n.validationError.existingDateRangeError)
    else if (
      !consecutivePlacementRanges.some((placementRange) =>
        placementRange.contains(
          new FiniteDateRange(form.startDate, form.endDate)
        )
      )
    )
      errors.push(
        i18n.childInformation.backupCares.validationNoMatchingPlacement
      )

    return errors
  }

  const initialErrors = evaluateFormErrors(initialFormState)
  const [formErrors, setFormErrors] = useState<string[]>(initialErrors)

  const validateForm = (form: FormState) => {
    setFormErrors(evaluateFormErrors(form))
  }

  const updateFormState: UpdateStateFn<FormState> = (value) => {
    const newState = { ...formState, ...value }
    setFormState(newState)
    validateForm(newState)
  }

  const submitSuccess = useCallback(() => {
    clearUiMode()
    return loadBackupCares()
  }, [clearUiMode, loadBackupCares])

  const options = useMemo(
    () =>
      units
        .map((us) =>
          orderBy(us, (x) => x.name).map(({ id, name }) => ({ id, name }))
        )
        .getOrElse([]),
    [units]
  )

  const [createOrUpdateBackupCare, onClick] = useSelectMutation(
    () => (backupCare === undefined ? first() : second(backupCare)),
    [
      createBackupCareMutation,
      () =>
        formState.unit !== undefined
          ? {
              childId,
              body: {
                groupId: null,
                unitId: formState.unit.id,
                period: new FiniteDateRange(
                  formState.startDate,
                  formState.endDate
                )
              }
            }
          : cancelMutation
    ],
    [
      updateBackupCareMutation,
      (backupCare) => ({
        id: backupCare.id,
        unitId: backupCare.unit.id,
        body: {
          groupId: null,
          period: new FiniteDateRange(formState.startDate, formState.endDate)
        }
      })
    ]
  )

  return (
    <div>
      <form data-qa="backup-care-form">
        <FormField>
          <FormLabel>{i18n.childInformation.backupCares.unit}</FormLabel>
          <Unit>
            {backupCare ? (
              backupCare.unit.name
            ) : (
              <div data-qa="backup-care-select-unit">
                <Combobox
                  items={options}
                  selectedItem={formState.unit ?? null}
                  onChange={(unit) =>
                    updateFormState({ unit: unit || undefined })
                  }
                  getItemLabel={({ name }) => name}
                  getItemDataQa={({ id }) => `unit-${id}`}
                />
              </div>
            )}
          </Unit>
        </FormField>
        <FormField>
          <FormLabel>{i18n.childInformation.backupCares.dateRange}</FormLabel>
          <div data-qa="dates">
            <DatePickerDeprecated
              date={formState.startDate}
              onChange={(startDate) => updateFormState({ startDate })}
            />
            {' - '}
            <DatePickerDeprecated
              date={formState.endDate}
              onChange={(endDate) => updateFormState({ endDate })}
            />
          </div>
        </FormField>
        {formErrors.map((error, index) => (
          <div className="error" key={index} data-qa="form-error">
            {error}
          </div>
        ))}
        <ActionButtons>
          <Button onClick={() => clearUiMode()} text={i18n.common.cancel} />
          <MutateButton
            primary
            type="submit"
            mutation={createOrUpdateBackupCare}
            onClick={onClick}
            onSuccess={submitSuccess}
            disabled={formErrors.length > 0 || !formState.unit}
            data-qa="submit-backup-care-form"
            text={i18n.common.confirm}
          />
        </ActionButtons>
      </form>
      {uiMode === 'create-new-backup-care' && <div className="separator" />}
    </div>
  )
}
