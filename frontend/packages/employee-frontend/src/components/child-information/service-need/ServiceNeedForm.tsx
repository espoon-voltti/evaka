// SPDX-FileCopyrightText: 2017-2020 City of Espoo
//
// SPDX-License-Identifier: LGPL-2.1-or-later

import React, { FormEvent, useContext, useEffect, useState } from 'react'
import LocalDate from '@evaka/lib-common/src/local-date'
import { useTranslation } from '~/state/i18n'
import { UIContext } from '~state/ui'
import Checkbox from '~components/shared/atoms/form/Checkbox'
import InputField from '~components/shared/atoms/form/InputField'
import { ServiceNeed } from '~types/child'
import { UUID } from '~types'
import { FormErrors, formHasErrors } from '~utils/validation/validations'
import { DatePicker, DatePickerClearable } from '~components/common/DatePicker'
import { AlertBox, InfoBox } from '~components/common/MessageBoxes'
import FormActions from '~components/common/FormActions'
import LabelValueList from '~components/common/LabelValueList'
import styled from 'styled-components'
import { ChildContext } from '~state'
import { DateRangeOpen, rangeContainsDate } from '~utils/date'
import { DivFitContent } from 'components/common/styled/containers'
import {
  createServiceNeed,
  ServiceNeedRequest,
  updateServiceNeed
} from 'api/child/service-needs'
import { FixedSpaceColumn } from '~components/shared/layout/flex-helpers'

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
  coefficient: boolean
}

const noErrors: ServiceNeedFormErrors = {
  dateRange: {
    inverted: false,
    conflict: false
  },
  coefficient: false
}

function isCreate(props: Props): props is CreateProps {
  return (props as CreateProps).childId !== undefined
}

function ServiceNeedForm(props: Props) {
  const { i18n } = useTranslation()
  const { clearUiMode, setErrorMessage } = useContext(UIContext)
  const { serviceNeeds } = useContext(ChildContext)

  const initialFormState: FormState = isCreate(props)
    ? {
        startDate: LocalDate.today(),
        endDate: null,
        hoursPerWeek: 35,
        partDay: false,
        partWeek: false,
        shiftCare: false,
        temporary: false,
        notes: ''
      }
    : {
        ...props.serviceNeed
      }
  const [form, setForm] = useState<FormState>(initialFormState)

  const [formErrors, setFormErrors] = useState<ServiceNeedFormErrors>(noErrors)

  const [autoCutWarning, setAutoCutWarning] = useState<boolean>(false)

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

  const updateFormState = (value: Partial<FormState>) => {
    const newState = { ...form, ...value }
    setForm(newState)
  }

  useEffect(() => {
    setFormErrors({
      dateRange: {
        inverted: form.endDate ? form.endDate.isBefore(form.startDate) : false,
        conflict: isCreate(props) ? checkHardConflict() : checkAnyConflict()
      },
      coefficient: false
    })

    setAutoCutWarning(
      isCreate(props) && checkSoftConflict() && !checkHardConflict()
    )
  }, [form, serviceNeeds])

  const submitForm = (event: FormEvent<HTMLFormElement>) => {
    event.preventDefault()

    if (formHasErrors(formErrors)) return

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
            title: i18n.common.error.unknown
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
                  <DatePicker
                    date={form.startDate}
                    onChange={(startDate) => updateFormState({ startDate })}
                  />
                  {' - '}
                  <DatePickerClearable
                    date={form.endDate}
                    onChange={(endDate) => updateFormState({ endDate })}
                    onCleared={() => updateFormState({ endDate: null })}
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
                    dataQa="input-service-need-hours-per-week"
                    onChange={(value) =>
                      updateFormState({
                        hoursPerWeek: Number(value)
                      })
                    }
                  />
                  <span>{i18n.childInformation.serviceNeed.hoursInWeek}</span>
                </NumberInputWrapper>
              </div>
            )
          }
        ]}
      />

      <InfoBox
        message={i18n.childInformation.serviceNeed.hoursPerWeekInfo}
        thin
        wide
      />
      <div className="separator-gap-small" />

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
                  dataQa="toggle-service-need-part-day"
                />
                <Checkbox
                  label={i18n.childInformation.serviceNeed.services.partWeek}
                  checked={form.partWeek}
                  onChange={(value: boolean) =>
                    updateFormState({ partWeek: value })
                  }
                  dataQa="toggle-service-need-part-week"
                />
                <Checkbox
                  label={i18n.childInformation.serviceNeed.services.temporary}
                  checked={form.temporary}
                  onChange={(value: boolean) =>
                    updateFormState({ temporary: value })
                  }
                  dataQa="toggle-service-need-temporary"
                />
                <Checkbox
                  label={i18n.childInformation.serviceNeed.services.shiftCare}
                  checked={form.shiftCare}
                  onChange={(value: boolean) =>
                    updateFormState({ shiftCare: value })
                  }
                  dataQa="toggle-service-need-shift-care"
                />
              </FixedSpaceColumn>
            )
          }
        ]}
      />

      {autoCutWarning && (
        <AlertBox
          message={i18n.childInformation.serviceNeed.errors.autoCutWarning}
          thin
          wide
        />
      )}

      <FormActions
        onCancel={() => clearUiMode()}
        disabled={formHasErrors(formErrors)}
        dataQa="submit-service-need-form"
      />
    </form>
  )
}

export default ServiceNeedForm
