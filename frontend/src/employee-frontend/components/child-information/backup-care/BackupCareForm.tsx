// SPDX-FileCopyrightText: 2017-2020 City of Espoo
//
// SPDX-License-Identifier: LGPL-2.1-or-later

import React, {
  FormEvent,
  useContext,
  useEffect,
  useMemo,
  useState
} from 'react'
import _ from 'lodash'
import LocalDate from 'lib-common/local-date'
import { UpdateStateFn } from '../../../../lib-common/form-state'
import { UUID } from '../../../types'
import { useTranslation } from '../../../state/i18n'
import { UIContext } from '../../../state/ui'
import { ChildContext } from '../../../state'
import { DateRange } from '../../../utils/date'
import Button from 'lib-components/atoms/buttons/Button'
import { Loading, Result } from 'lib-common/api'
import {
  isDateRangeInverted,
  isDateRangeOverlappingWithExisting
} from '../../../utils/validation/validations'
import { DatePickerDeprecated } from 'lib-components/molecules/DatePickerDeprecated'
import { getUnits } from '../../../api/daycare'
import { Unit } from '../../../types/invoicing'
import {
  createBackupCare,
  getChildBackupCares,
  updateBackupCare
} from '../../../api/child/backup-care'
import ReactSelect, { createFilter, components } from 'react-select'
import styled from 'styled-components'
import { ChildBackupCare } from '../../../types/child'
import { FixedSpaceRow } from 'lib-components/layout/flex-helpers'
import FiniteDateRange from 'lib-common/finite-date-range'

export interface Props {
  childId: UUID
  backupCare?: ChildBackupCare
}

interface FormState {
  unit: { label: string; value: string } | undefined
  startDate: LocalDate
  endDate: LocalDate
}

const FormField = styled.div`
  display: flex;
  align-items: center;
  margin: 20px 0;
`

const FormLabel = styled.div`
  font-weight: 600;
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
    unit: backupCare?.unit && {
      label: backupCare.unit.name,
      value: backupCare.unit.id
    },
    startDate: backupCare?.period.start ?? LocalDate.today(),
    endDate: backupCare?.period.end ?? LocalDate.today()
  }

  const [formState, setFormState] = useState<FormState>(initialFormState)

  const evaluateFormErrors = (form: FormState) => {
    const errors: string[] = []
    const existing: DateRange[] = backupCares
      .map((bcs) =>
        bcs
          .filter((it) => backupCare == undefined || it.id !== backupCare.id)
          .map(({ period }) => ({
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

  const submitForm = (event: FormEvent<HTMLFormElement>) => {
    event.preventDefault()

    if (!formState.unit) throw new Error(`No unit selected`)

    const apiCall: Promise<Result<unknown>> =
      backupCare == undefined
        ? createBackupCare(childId, {
            unitId: formState.unit.value,
            period: new FiniteDateRange(formState.startDate, formState.endDate)
          })
        : updateBackupCare(backupCare.id, {
            period: new FiniteDateRange(formState.startDate, formState.endDate)
          })

    void apiCall
      .then(() => clearUiMode())
      .then(() =>
        getChildBackupCares(childId).then((backupCares) =>
          setBackupCares(backupCares)
        )
      )
  }

  const options = useMemo(
    () =>
      units
        .map((us) =>
          _.orderBy(us, (x) => x.name).map(({ id, name }) => ({
            label: name,
            value: id
          }))
        )
        .getOrElse([]),
    [units]
  )

  return (
    <div>
      <form onSubmit={submitForm} data-qa="backup-care-form">
        <FormField>
          <FormLabel>{i18n.childInformation.backupCares.unit}</FormLabel>
          <Unit>
            {backupCare ? (
              backupCare.unit.name
            ) : (
              <div data-qa="backup-care-select-unit">
                <ReactSelect
                  placeholder="Valitse..."
                  value={formState.unit}
                  components={{
                    Option: function Option(props) {
                      const { value } = props.data as { value: string }
                      return (
                        <div data-qa={`value-${value}`}>
                          <components.Option
                            {...props}
                            data-qa={`value-${value}`}
                          />
                        </div>
                      )
                    }
                  }}
                  onChange={(unit) =>
                    updateFormState({
                      unit: unit && 'label' in unit ? unit : undefined
                    })
                  }
                  filterOption={createFilter({ ignoreAccents: false })}
                  options={options}
                  isLoading={units.isLoading}
                  loadingMessage={() => i18n.common.loading}
                  noOptionsMessage={() => i18n.common.loadingFailed}
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
          <Button
            primary
            type="submit"
            disabled={formErrors.length > 0 || !formState.unit}
            dataQa="submit-backup-care-form"
            text={i18n.common.confirm}
          />
        </ActionButtons>
      </form>
      {uiMode === 'create-new-backup-care' && <div className="separator" />}
    </div>
  )
}
