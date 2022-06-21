// SPDX-FileCopyrightText: 2017-2022 City of Espoo
//
// SPDX-License-Identifier: LGPL-2.1-or-later

import orderBy from 'lodash/orderBy'
import React, {
  useCallback,
  useContext,
  useEffect,
  useMemo,
  useState
} from 'react'
import styled from 'styled-components'

import { Loading, Result } from 'lib-common/api'
import FiniteDateRange from 'lib-common/finite-date-range'
import { UpdateStateFn } from 'lib-common/form-state'
import { ChildBackupCare } from 'lib-common/generated/api-types/backupcare'
import LocalDate from 'lib-common/local-date'
import { UUID } from 'lib-common/types'
import AsyncButton from 'lib-components/atoms/buttons/AsyncButton'
import Button from 'lib-components/atoms/buttons/Button'
import Combobox from 'lib-components/atoms/dropdowns/Combobox'
import { FixedSpaceRow } from 'lib-components/layout/flex-helpers'
import { DatePickerDeprecated } from 'lib-components/molecules/DatePickerDeprecated'
import { fontWeights } from 'lib-components/typography'

import {
  createBackupCare,
  getChildBackupCares,
  updateBackupCare
} from '../../../api/child/backup-care'
import { getUnits, Unit } from '../../../api/daycare'
import { ChildContext } from '../../../state'
import { useTranslation } from '../../../state/i18n'
import { UIContext } from '../../../state/ui'
import { DateRange } from '../../../utils/date'
import {
  isDateRangeInverted,
  isDateRangeOverlappingWithExisting
} from '../../../utils/validation/validations'

export interface Props {
  childId: UUID
  backupCare?: ChildBackupCare
}

interface FormState {
  unit: Unit | undefined
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

export default function BackupCareForm({
  childId,
  backupCare
}: Props): JSX.Element {
  const { i18n } = useTranslation()
  const { uiMode, clearUiMode } = useContext(UIContext)
  const { backupCares, setBackupCares } = useContext(ChildContext)

  const [units, setUnits] = useState<Result<Unit[]>>(Loading.of())

  useEffect(() => {
    void getUnits([], 'DAYCARE').then(setUnits)
  }, [])

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

  const submitForm = useCallback((): Promise<Result<unknown>> | undefined => {
    if (!formState.unit) return

    return backupCare == undefined
      ? createBackupCare(childId, {
          unitId: formState.unit.id,
          period: new FiniteDateRange(formState.startDate, formState.endDate)
        })
      : updateBackupCare(backupCare.id, {
          period: new FiniteDateRange(formState.startDate, formState.endDate)
        })
  }, [
    backupCare,
    childId,
    formState.endDate,
    formState.startDate,
    formState.unit
  ])

  const submitSuccess = useCallback(() => {
    clearUiMode()
    void getChildBackupCares(childId).then((backupCares) =>
      setBackupCares(backupCares)
    )
  }, [childId, clearUiMode, setBackupCares])

  const options = useMemo(
    () =>
      units
        .map((us) =>
          orderBy(us, (x) => x.name).map(({ id, name }) => ({ id, name }))
        )
        .getOrElse([]),
    [units]
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
          <div className="error" key={index}>
            {error}
          </div>
        ))}
        <ActionButtons>
          <Button onClick={() => clearUiMode()} text={i18n.common.cancel} />
          <AsyncButton
            primary
            type="submit"
            onClick={submitForm}
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
