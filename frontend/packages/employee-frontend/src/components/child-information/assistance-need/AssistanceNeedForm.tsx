// SPDX-FileCopyrightText: 2017-2020 City of Espoo
//
// SPDX-License-Identifier: LGPL-2.1-or-later

import React, { FormEvent, useContext, useEffect, useState } from 'react'
import styled from 'styled-components'
import LocalDate from '@evaka/lib-common/src/local-date'
import { useTranslation } from '~/state/i18n'
import { UIContext } from '~state/ui'
import { Gap } from '@evaka/lib-components/src/white-space'
import Checkbox from '@evaka/lib-components/src/atoms/form/Checkbox'
import InputField, {
  TextArea
} from '@evaka/lib-components/src/atoms/form/InputField'
import InfoBall from '~components/common/InfoBall'
import { AssistanceBasis, AssistanceNeed } from '~types/child'
import { UUID } from '~types'
import { formatDecimal, textAreaRows } from '~components/utils'

import { DatePicker } from '~components/common/DatePicker'
import {
  FormErrors,
  formHasErrors,
  isDateRangeInverted
} from '~utils/validation/validations'
import LabelValueList from '~components/common/LabelValueList'
import { ASSISTANCE_BASIS_LIST } from '~constants'
import FormActions from '~components/common/FormActions'
import { ChildContext } from '~state'
import { DateRange, rangeContainsDate } from '~utils/date'
import { AlertBox } from '@evaka/lib-components/src/molecules/MessageBoxes'
import { DivFitContent } from 'components/common/styled/containers'
import {
  AssistanceNeedRequest,
  createAssistanceNeed,
  updateAssistanceNeed
} from 'api/child/assistance-needs'

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
  description: string
  bases: Set<AssistanceBasis>
  otherBasis: string
}

const coefficientRegex = /^\d(([.,])(\d){1,2})?$/

interface CommonProps {
  onReload: () => undefined | void
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

function AssistanceNeedForm(props: Props) {
  const { i18n } = useTranslation()
  const { clearUiMode, setErrorMessage } = useContext(UIContext)
  const { assistanceNeeds } = useContext(ChildContext)

  const initialFormState: FormState =
    isCreate(props) && !isDuplicate(props)
      ? {
          startDate: LocalDate.today(),
          endDate: LocalDate.today(),
          capacityFactor: '1',
          description: '',
          bases: new Set(),
          otherBasis: ''
        }
      : {
          ...props.assistanceNeed,
          capacityFactor: formatDecimal(props.assistanceNeed.capacityFactor)
        }
  const [form, setForm] = useState<FormState>(initialFormState)

  const [formErrors, setFormErrors] = useState<AssistanceNeedFormErrors>(
    noErrors
  )

  const [autoCutWarning, setAutoCutWarning] = useState<boolean>(false)

  const getExistingAssistanceNeedRanges = (): DateRange[] =>
    assistanceNeeds
      .map((needs) =>
        needs
          .filter((sn) => isCreate(props) || sn.id != props.assistanceNeed.id)
          .map(({ startDate, endDate }) => ({ startDate, endDate }))
      )
      .getOrElse([])

  const checkSoftConflict = (): boolean => {
    if (isDateRangeInverted(form)) return false
    return getExistingAssistanceNeedRanges().some((existing) =>
      rangeContainsDate(existing, form.startDate)
    )
  }

  const checkHardConflict = (): boolean => {
    if (isDateRangeInverted(form)) return false
    return getExistingAssistanceNeedRanges().some((existing) =>
      rangeContainsDate(form, existing.startDate)
    )
  }

  const checkAnyConflict = () => checkHardConflict() || checkSoftConflict()

  const updateFormState = (values: Partial<FormState>) => {
    const newState = { ...form, ...values }
    setForm(newState)
  }

  useEffect(() => {
    setFormErrors({
      dateRange: {
        inverted: isDateRangeInverted(form),
        conflict: isCreate(props) ? checkHardConflict() : checkAnyConflict()
      },
      coefficient:
        !coefficientRegex.test(form.capacityFactor) ||
        Number(form.capacityFactor.replace(',', '.')) < 1
    })

    setAutoCutWarning(
      isCreate(props) && checkSoftConflict() && !checkHardConflict()
    )
  }, [form, assistanceNeeds])

  const submitForm = (event: FormEvent<HTMLFormElement>) => {
    event.preventDefault()

    if (formHasErrors(formErrors)) return

    const data: AssistanceNeedRequest = {
      ...form,
      capacityFactor: Number(form.capacityFactor.replace(',', '.')),
      bases: [...form.bases],
      otherBasis: form.bases.has('OTHER') ? form.otherBasis : ''
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
            label: i18n.childInformation.assistanceNeed.fields.dateRange,
            value: (
              <>
                <DivFitContent>
                  <DatePicker
                    date={form.startDate}
                    onChange={(startDate) => updateFormState({ startDate })}
                  />
                  {' - '}
                  <DatePicker
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
                    dataQa="input-assistance-need-multiplier"
                  />
                  <InfoBall
                    text={
                      i18n.childInformation.assistanceNeed.fields
                        .capacityFactorInfo
                    }
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
              </>
            )
          },
          {
            label: i18n.childInformation.assistanceNeed.fields.description,
            value: (
              <TextArea
                value={form.description}
                onChange={(event: React.ChangeEvent<HTMLTextAreaElement>) =>
                  updateFormState({ description: event.target.value })
                }
                rows={textAreaRows(form.description)}
                placeholder={
                  i18n.childInformation.assistanceNeed.fields
                    .descriptionPlaceholder
                }
              />
            ),
            valueWidth: '100%'
          },
          {
            label: i18n.childInformation.assistanceNeed.fields.bases,
            value: (
              <div>
                {ASSISTANCE_BASIS_LIST.map((basis) => (
                  <CheckboxRow key={basis}>
                    <Checkbox
                      label={
                        i18n.childInformation.assistanceNeed.fields.basisTypes[
                          basis
                        ]
                      }
                      checked={form.bases.has(basis)}
                      onChange={(value) => {
                        const bases = new Set([...form.bases])
                        if (value) bases.add(basis)
                        else bases.delete(basis)
                        updateFormState({ bases: bases })
                      }}
                    />
                    {i18n.childInformation.assistanceNeed.fields.basisTypes[
                      `${basis}_INFO`
                    ] && (
                      <InfoBall
                        text={String(
                          i18n.childInformation.assistanceNeed.fields
                            .basisTypes[`${basis}_INFO`]
                        )}
                      />
                    )}
                  </CheckboxRow>
                ))}
                {form.bases.has('OTHER') && (
                  <div style={{ width: '100%' }}>
                    <InputField
                      value={form.otherBasis}
                      onChange={(value) =>
                        updateFormState({ otherBasis: value })
                      }
                      placeholder={
                        i18n.childInformation.assistanceNeed.fields
                          .otherBasisPlaceholder
                      }
                    />
                  </div>
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
        dataQa="button-assistance-need-confirm"
      />
    </form>
  )
}

export default AssistanceNeedForm
