// SPDX-FileCopyrightText: 2017-2020 City of Espoo
//
// SPDX-License-Identifier: LGPL-2.1-or-later

import React, { FormEvent, useContext, useEffect, useState } from 'react'
import styled from 'styled-components'
import { UpdateStateFn } from 'lib-common/form-state'
import LocalDate from 'lib-common/local-date'
import { UUID } from 'lib-common/types'
import { formatDecimal } from 'lib-common/utils/number'
import Checkbox from 'lib-components/atoms/form/Checkbox'
import InputField from 'lib-components/atoms/form/InputField'
import { DatePickerDeprecated } from 'lib-components/molecules/DatePickerDeprecated'
import ExpandingInfo from 'lib-components/molecules/ExpandingInfo'
import { AlertBox } from 'lib-components/molecules/MessageBoxes'
import { Gap } from 'lib-components/white-space'
import {
  AssistanceNeedRequest,
  createAssistanceNeed,
  updateAssistanceNeed
} from '../../../api/child/assistance-needs'
import FormActions from '../../../components/common/FormActions'
import LabelValueList from '../../../components/common/LabelValueList'
import { useTranslation } from '../../../state/i18n'
import { UIContext } from '../../../state/ui'
import {
  AssistanceBasisOption,
  AssistanceNeed,
  AssistanceNeedResponse
} from '../../../types/child'

import { DateRange, rangeContainsDate } from '../../../utils/date'
import {
  FormErrors,
  formHasErrors,
  isDateRangeInverted
} from '../../../utils/validation/validations'
import { DivFitContent } from '../../common/styled/containers'

const CheckboxRow = styled.div`
  display: flex;
  align-items: baseline;
  margin: 4px 0;
`

const CoefficientInputContainer = styled.div`
  display: flex;
  flex-direction: row;
  justify-content: left;
  width: 100px;
  position: relative;

  > div.field {
    margin-bottom: 0;
    padding-bottom: 0;

    div.is-error span.icon {
      display: none;
    }
  }
`

interface FormState {
  startDate: LocalDate
  endDate: LocalDate
  capacityFactor: string
  bases: Set<string>
}

const coefficientRegex = /^\d(([.,])(\d){1,2})?$/

interface CommonProps {
  onReload: () => undefined | void
  assistanceNeeds: AssistanceNeedResponse[]
  assistanceBasisOptions: AssistanceBasisOption[]
}

interface CreateProps extends CommonProps {
  childId: UUID
}

interface UpdateProps extends CommonProps {
  assistanceNeed: AssistanceNeed
}

interface DuplicateProps extends CommonProps, CreateProps {
  assistanceNeed: AssistanceNeed
}

type Props = CreateProps | UpdateProps | DuplicateProps

function isCreate(props: Props): props is CreateProps {
  return (props as CreateProps).childId !== undefined
}

function isDuplicate(props: Props): props is DuplicateProps {
  return (
    isCreate(props) && (props as DuplicateProps).assistanceNeed !== undefined
  )
}

interface AssistanceNeedFormErrors extends FormErrors {
  dateRange: {
    inverted: boolean
    conflict: boolean
  }
  coefficient: boolean
}

const noErrors: AssistanceNeedFormErrors = {
  dateRange: {
    inverted: false,
    conflict: false
  },
  coefficient: false
}

function getExistingAssistanceNeedRanges(props: Props): DateRange[] {
  return props.assistanceNeeds
    .filter(({ need }) => isCreate(props) || need.id != props.assistanceNeed.id)
    .map(({ need: { startDate, endDate } }) => ({ startDate, endDate }))
}

function checkSoftConflict(form: FormState, props: Props): boolean {
  if (isDateRangeInverted(form)) return false
  return getExistingAssistanceNeedRanges(props).some((existing) =>
    rangeContainsDate(existing, form.startDate)
  )
}

function checkHardConflict(form: FormState, props: Props): boolean {
  if (isDateRangeInverted(form)) return false
  return getExistingAssistanceNeedRanges(props).some((existing) =>
    rangeContainsDate(form, existing.startDate)
  )
}

export default React.memo(function AssistanceNeedForm(props: Props) {
  const { i18n } = useTranslation()
  const { clearUiMode, setErrorMessage } = useContext(UIContext)

  const initialFormState: FormState =
    isCreate(props) && !isDuplicate(props)
      ? {
          startDate: LocalDate.today(),
          endDate: LocalDate.today(),
          capacityFactor: '1',
          bases: new Set()
        }
      : {
          ...props.assistanceNeed,
          capacityFactor: formatDecimal(props.assistanceNeed.capacityFactor)
        }
  const [form, setForm] = useState<FormState>(initialFormState)

  const [formErrors, setFormErrors] =
    useState<AssistanceNeedFormErrors>(noErrors)

  const [autoCutWarning, setAutoCutWarning] = useState<boolean>(false)

  const updateFormState: UpdateStateFn<FormState> = (values) => {
    const newState = { ...form, ...values }
    setForm(newState)
  }

  useEffect(() => {
    const isHardConflict = checkHardConflict(form, props)
    const isSoftConflict = checkSoftConflict(form, props)
    const isAnyConflict = isHardConflict || isSoftConflict

    setFormErrors({
      dateRange: {
        inverted: isDateRangeInverted(form),
        conflict: isCreate(props) ? isHardConflict : isAnyConflict
      },
      coefficient:
        !coefficientRegex.test(form.capacityFactor) ||
        Number(form.capacityFactor.replace(',', '.')) < 1
    })

    setAutoCutWarning(isCreate(props) && isAnyConflict && !isHardConflict)
  }, [form, props])

  const submitForm = (event: FormEvent<HTMLFormElement>) => {
    event.preventDefault()

    if (formHasErrors(formErrors)) return

    const data: AssistanceNeedRequest = {
      ...form,
      capacityFactor: Number(form.capacityFactor.replace(',', '.')),
      bases: [...form.bases]
    }

    const apiCall = isCreate(props)
      ? createAssistanceNeed(props.childId, data)
      : updateAssistanceNeed(props.assistanceNeed.id, data)

    void apiCall.then((res) => {
      if (res.isSuccess) {
        clearUiMode()
        props.onReload()
      } else if (res.isFailure) {
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
            label: i18n.childInformation.assistanceNeed.fields.dateRange,
            value: (
              <>
                <DivFitContent>
                  <DatePickerDeprecated
                    date={form.startDate}
                    onChange={(startDate) => updateFormState({ startDate })}
                  />
                  {' - '}
                  <DatePickerDeprecated
                    date={form.endDate}
                    onChange={(endDate) => updateFormState({ endDate })}
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
                      ? i18n.childInformation.assistanceNeed.errors.hardConflict
                      : i18n.childInformation.assistanceNeed.errors.conflict}
                  </span>
                )}
              </>
            )
          },
          {
            label: i18n.childInformation.assistanceNeed.fields.capacityFactor,
            value: (
              <>
                <ExpandingInfo
                  info={
                    i18n.childInformation.assistanceNeed.fields
                      .capacityFactorInfo
                  }
                  ariaLabel={
                    i18n.childInformation.assistanceNeed.fields.capacityFactor
                  }
                  fullWidth={true}
                >
                  <CoefficientInputContainer>
                    <InputField
                      value={form.capacityFactor.toString()}
                      onChange={(value) =>
                        updateFormState({
                          capacityFactor: value
                        })
                      }
                      info={
                        formErrors.coefficient
                          ? { text: 'Virheellinen arvo', status: 'warning' }
                          : undefined
                      }
                      data-qa="input-assistance-need-multiplier"
                    />
                  </CoefficientInputContainer>
                  {formErrors.coefficient && (
                    <span className="error">
                      {
                        i18n.childInformation.assistanceNeed.errors
                          .invalidCoefficient
                      }
                    </span>
                  )}
                </ExpandingInfo>
              </>
            )
          },
          {
            label: i18n.childInformation.assistanceNeed.fields.bases,
            value: (
              <div>
                {props.assistanceBasisOptions.map((basis) =>
                  basis.descriptionFi ? (
                    <ExpandingInfo
                      key={basis.value}
                      info={basis.descriptionFi}
                      ariaLabel=""
                      fullWidth={true}
                    >
                      <CheckboxRow key={basis.value}>
                        <Checkbox
                          label={basis.nameFi}
                          checked={form.bases.has(basis.value)}
                          onChange={(value) => {
                            const bases = new Set([...form.bases])
                            if (value) bases.add(basis.value)
                            else bases.delete(basis.value)
                            updateFormState({ bases: bases })
                          }}
                        />
                      </CheckboxRow>
                    </ExpandingInfo>
                  ) : (
                    <CheckboxRow key={basis.value}>
                      <Checkbox
                        label={basis.nameFi}
                        checked={form.bases.has(basis.value)}
                        onChange={(value) => {
                          const bases = new Set([...form.bases])
                          if (value) bases.add(basis.value)
                          else bases.delete(basis.value)
                          updateFormState({ bases: bases })
                        }}
                      />
                    </CheckboxRow>
                  )
                )}
              </div>
            ),
            valueWidth: '100%'
          }
        ]}
      />

      {autoCutWarning && (
        <>
          <Gap size="xs" />
          <AlertBox
            message={i18n.childInformation.assistanceNeed.errors.autoCutWarning}
            thin
            wide
          />
        </>
      )}

      <Gap size="s" />
      <FormActions
        onCancel={() => clearUiMode()}
        disabled={formHasErrors(formErrors)}
        data-qa="button-assistance-need-confirm"
      />
    </form>
  )
})
