// SPDX-FileCopyrightText: 2017-2020 City of Espoo
//
// SPDX-License-Identifier: LGPL-2.1-or-later

import React, { FormEvent, useContext, useEffect, useState } from 'react'
import LocalDate from 'lib-common/local-date'
import DateRange from 'lib-common/date-range'
import { UpdateStateFn } from '../../../../lib-common/form-state'
import { useTranslation } from '../../../state/i18n'
import { UIContext } from '../../../state/ui'
import { Gap } from 'lib-components/white-space'
import Checkbox from 'lib-components/atoms/form/Checkbox'
import InputField from 'lib-components/atoms/form/InputField'
import { Placement, ServiceNeed } from '../../../types/child'
import { UUID } from '../../../types'
import {
  FormErrors,
  formHasErrors
} from '../../../utils/validation/validations'
import {
  DatePickerDeprecated,
  DatePickerClearableDeprecated
} from 'lib-components/molecules/DatePickerDeprecated'
import { AlertBox, InfoBox } from 'lib-components/molecules/MessageBoxes'
import FormActions from '../../../components/common/FormActions'
import LabelValueList from '../../../components/common/LabelValueList'
import styled from 'styled-components'
import { ChildContext } from '../../../state'
import { DateRangeOpen, rangeContainsDate } from '../../../utils/date'
import { DivFitContent } from '../../../components/common/styled/containers'
import {
  createServiceNeed,
  ServiceNeedRequest,
  updateServiceNeed
} from '../../../api/child/service-needs'
import { FixedSpaceColumn } from 'lib-components/layout/flex-helpers'

const NumberInputWrapper = styled.div`
  display: flex;
  align-items: baseline;
  width: 200px;
  margin-bottom: 20px;

  span {
    white-space: nowrap;
    margin-left: 8px;
  }
`

interface CommonProps {
  onReload: () => undefined | void
}

interface CreateProps extends CommonProps {
  childId: UUID
}

interface UpdateProps extends CommonProps {
  serviceNeed: ServiceNeed
}

type Props = CreateProps | UpdateProps

type FormState = ServiceNeedRequest

interface ServiceNeedFormErrors extends FormErrors {
  dateRange: {
    inverted: boolean
    conflict: boolean
  }
  hours: boolean
}

const noErrors: ServiceNeedFormErrors = {
  dateRange: {
    inverted: false,
    conflict: false
  },
  hours: false
}

function isCreate(props: Props): props is CreateProps {
  return (props as CreateProps).childId !== undefined
}

function placementTypeAndHoursMismatch(
  placement: Placement,
  hours: number
): boolean {
  switch (placement.type) {
    case 'CLUB':
    case 'DAYCARE':
    case 'DAYCARE_FIVE_YEAR_OLDS':
    case 'PRESCHOOL_DAYCARE':
    case 'PREPARATORY_DAYCARE':
      return false
    case 'PRESCHOOL':
      return hours > 20
    case 'PREPARATORY':
    case 'DAYCARE_PART_TIME':
    case 'DAYCARE_PART_TIME_FIVE_YEAR_OLDS':
    case 'TEMPORARY_DAYCARE_PART_DAY':
      return hours > 25
    case 'TEMPORARY_DAYCARE':
      return hours <= 25
  }
}

function ServiceNeedForm(props: Props) {
  const { i18n } = useTranslation()
  const { clearUiMode, setErrorMessage } = useContext(UIContext)
  const { serviceNeeds, placements } = useContext(ChildContext)

  const initialFormState: FormState = isCreate(props)
    ? {
        startDate: LocalDate.today(),
        endDate: null,
        hoursPerWeek: 35,
        partDay: false,
        partWeek: false,
        shiftCare: false,
        notes: ''
      }
    : {
        ...props.serviceNeed
      }
  const [form, setForm] = useState<FormState>(initialFormState)

  const [formErrors, setFormErrors] = useState<ServiceNeedFormErrors>(noErrors)

  const getExistingServiceNeedRanges = (): DateRangeOpen[] =>
    serviceNeeds
      .map((needs) =>
        needs
          .filter((sn) => isCreate(props) || sn.id != props.serviceNeed.id)
          .map(({ startDate, endDate }) => ({ startDate, endDate }))
      )
      .getOrElse([])

  const checkSoftConflict = (): boolean => {
    if (form.endDate?.isBefore(form.startDate)) return false
    return getExistingServiceNeedRanges().some((existing) =>
      rangeContainsDate(existing, form.startDate)
    )
  }

  const checkHardConflict = (): boolean => {
    if (form.endDate?.isBefore(form.startDate)) return false
    return getExistingServiceNeedRanges().some((existing) =>
      rangeContainsDate(form, existing.startDate)
    )
  }

  const checkAnyConflict = () => checkHardConflict() || checkSoftConflict()

  const updateFormState: UpdateStateFn<FormState> = (value) => {
    const newState = { ...form, ...value }
    setForm(newState)
  }

  const placementMismatch = (form: FormState) =>
    placements
      .map((ps) => {
        if (form.endDate && !form.startDate.isBefore(form.endDate)) {
          return false
        }

        const snDateRange = new DateRange(form.startDate, form.endDate)
        return ps
          .filter((p) =>
            new DateRange(p.startDate, p.endDate).overlapsWith(snDateRange)
          )
          .some((p) => placementTypeAndHoursMismatch(p, form.hoursPerWeek))
      })
      .getOrElse(false)

  const checkFormErrors = (updatedForm?: Partial<FormState>) => {
    const mergedForm = { ...form, ...updatedForm }
    return {
      dateRange: {
        inverted: mergedForm.endDate
          ? mergedForm.endDate.isBefore(mergedForm.startDate)
          : false,
        conflict: isCreate(props) ? checkHardConflict() : checkAnyConflict()
      },
      hours: placementMismatch(mergedForm)
    }
  }

  useEffect(() => {
    if (!formHasErrors(formErrors)) {
      return
    }

    setFormErrors(checkFormErrors())
  }, [form, serviceNeeds])

  const autoCutWarning =
    isCreate(props) && checkSoftConflict() && !checkHardConflict()

  const submitForm = (event: FormEvent<HTMLFormElement>) => {
    event.preventDefault()

    const errors = checkFormErrors()
    if (formHasErrors(errors)) {
      setFormErrors(errors)
      return
    }

    const apiCall = isCreate(props)
      ? createServiceNeed(props.childId, form)
      : updateServiceNeed(props.serviceNeed.id, form)

    void apiCall.then((res) => {
      if (res.isSuccess) {
        clearUiMode()
        props.onReload()
      }
      if (res.isFailure) {
        if (res.statusCode == 409) {
          setFormErrors({
            ...formErrors,
            dateRange: {
              ...formErrors.dateRange,
              conflict: true
            }
          })
        } else {
          setErrorMessage({
            type: 'error',
            title: i18n.common.error.unknown,
            resolveLabel: i18n.common.ok
          })
        }
      }
    })
  }

  return (
    <form onSubmit={submitForm}>
      <LabelValueList
        spacing="large"
        contents={[
          {
            label: i18n.childInformation.serviceNeed.dateRange,
            value: (
              <>
                <DivFitContent>
                  <DatePickerDeprecated
                    date={form.startDate}
                    onChange={(startDate) => {
                      updateFormState({ startDate })
                      setFormErrors(checkFormErrors({ startDate }))
                    }}
                  />
                  {' - '}
                  <DatePickerClearableDeprecated
                    date={form.endDate}
                    onChange={(endDate) => {
                      updateFormState({ endDate })
                      setFormErrors(checkFormErrors({ endDate }))
                    }}
                    onCleared={() => {
                      updateFormState({ endDate: null })
                      setFormErrors(checkFormErrors({ endDate: null }))
                    }}
                  />
                </DivFitContent>

                {formErrors.dateRange.inverted && (
                  <span className="error">
                    {i18n.validationError.invertedDateRange}
                  </span>
                )}
                {formErrors.dateRange.conflict && (
                  <span className="error">
                    {isCreate(props)
                      ? i18n.childInformation.serviceNeed.errors.hardConflict
                      : i18n.childInformation.serviceNeed.errors.conflict}
                  </span>
                )}
              </>
            )
          },
          {
            label: i18n.childInformation.serviceNeed.hoursPerWeek,
            value: (
              <div>
                <NumberInputWrapper>
                  <InputField
                    value={form.hoursPerWeek.toString()}
                    type={'number'}
                    min={1}
                    step={0.5}
                    data-qa="input-service-need-hours-per-week"
                    onChange={(value) =>
                      updateFormState({
                        hoursPerWeek: Number(value)
                      })
                    }
                    onBlur={() => setFormErrors(checkFormErrors())}
                    info={
                      formErrors.hours
                        ? {
                            text:
                              i18n.childInformation.serviceNeed.errors
                                .checkHours,
                            status: 'warning'
                          }
                        : undefined
                    }
                  />
                  <span>{i18n.childInformation.serviceNeed.hoursInWeek}</span>
                </NumberInputWrapper>
              </div>
            )
          }
        ]}
      />
      {formErrors.hours && (
        <>
          <Gap size="xs" />
          <AlertBox
            message={
              i18n.childInformation.serviceNeed.errors.placementMismatchWarning
            }
            thin
            wide
          />
        </>
      )}
      <Gap size="s" />
      <InfoBox
        message={i18n.childInformation.serviceNeed.hoursPerWeekInfo}
        thin
        wide
      />
      <Gap size="s" />

      <LabelValueList
        spacing="large"
        contents={[
          {
            label: i18n.childInformation.serviceNeed.serviceNeedDetails,
            value: (
              <FixedSpaceColumn>
                <Checkbox
                  label={i18n.childInformation.serviceNeed.services.partDay}
                  checked={form.partDay}
                  onChange={(value: boolean) =>
                    updateFormState({ partDay: value })
                  }
                  data-qa="toggle-service-need-part-day"
                />
                <Checkbox
                  label={i18n.childInformation.serviceNeed.services.partWeek}
                  checked={form.partWeek}
                  onChange={(value: boolean) =>
                    updateFormState({ partWeek: value })
                  }
                  data-qa="toggle-service-need-part-week"
                />
                <Checkbox
                  label={i18n.childInformation.serviceNeed.services.shiftCare}
                  checked={form.shiftCare}
                  onChange={(value: boolean) =>
                    updateFormState({ shiftCare: value })
                  }
                  data-qa="toggle-service-need-shift-care"
                />
              </FixedSpaceColumn>
            )
          }
        ]}
      />

      {autoCutWarning && (
        <>
          <Gap size="xs" />
          <AlertBox
            message={i18n.childInformation.serviceNeed.errors.autoCutWarning}
            thin
            wide
          />
        </>
      )}

      <Gap size="s" />
      <FormActions
        onCancel={() => clearUiMode()}
        disabled={formHasErrors(formErrors)}
        data-qa="submit-service-need-form"
      />
    </form>
  )
}

export default ServiceNeedForm
