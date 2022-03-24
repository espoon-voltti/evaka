// SPDX-FileCopyrightText: 2017-2022 City of Espoo
//
// SPDX-License-Identifier: LGPL-2.1-or-later

import React, {
  FormEvent,
  useContext,
  useEffect,
  useMemo,
  useState
} from 'react'
import styled from 'styled-components'

import { UpdateStateFn } from 'lib-common/form-state'
import {
  AssistanceActionOption,
  AssistanceActionRequest,
  AssistanceMeasure
} from 'lib-common/generated/api-types/assistanceaction'
import LocalDate from 'lib-common/local-date'
import { UUID } from 'lib-common/types'
import Checkbox from 'lib-components/atoms/form/Checkbox'
import InputField from 'lib-components/atoms/form/InputField'
import { DatePickerDeprecated } from 'lib-components/molecules/DatePickerDeprecated'
import ExpandingInfo from 'lib-components/molecules/ExpandingInfo'
import { AlertBox } from 'lib-components/molecules/MessageBoxes'
import { Gap } from 'lib-components/white-space'
import { assistanceMeasures, featureFlags } from 'lib-customizations/employee'

import {
  createAssistanceAction,
  updateAssistanceAction
} from '../../../api/child/assistance-actions'
import FormActions from '../../../components/common/FormActions'
import LabelValueList from '../../../components/common/LabelValueList'
import { useTranslation } from '../../../state/i18n'
import { UIContext } from '../../../state/ui'
import {
  AssistanceAction,
  AssistanceActionResponse
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

interface FormState {
  startDate: LocalDate
  endDate: LocalDate
  actions: Set<string>
  otherSelected: boolean
  otherAction: string
  measures: Set<AssistanceMeasure>
}

interface CommonProps {
  onReload: () => undefined | void
  assistanceActions: AssistanceActionResponse[]
  assistanceActionOptions: AssistanceActionOption[]
}

interface CreateProps extends CommonProps {
  childId: UUID
}

interface UpdateProps extends CommonProps {
  assistanceAction: AssistanceAction
}

interface DuplicateProps extends CommonProps, CreateProps {
  assistanceAction: AssistanceAction
}

type Props = CreateProps | UpdateProps | DuplicateProps

function isCreate(props: Props): props is CreateProps {
  return (props as CreateProps).childId !== undefined
}

function isDuplicate(props: Props): props is DuplicateProps {
  return (
    isCreate(props) && (props as DuplicateProps).assistanceAction !== undefined
  )
}

interface AssistanceActionFormErrors extends FormErrors {
  dateRange: {
    inverted: boolean
    conflict: boolean
  }
}

const noErrors: AssistanceActionFormErrors = {
  dateRange: {
    inverted: false,
    conflict: false
  }
}

const getExistingAssistanceActionRanges = (props: Props): DateRange[] =>
  props.assistanceActions
    .filter(
      ({ action }) => isCreate(props) || action.id != props.assistanceAction.id
    )
    .map(({ action: { startDate, endDate } }) => ({ startDate, endDate }))

function checkSoftConflict(form: FormState, props: Props): boolean {
  if (isDateRangeInverted(form)) return false
  return getExistingAssistanceActionRanges(props).some((existing) =>
    rangeContainsDate(existing, form.startDate)
  )
}

function checkHardConflict(form: FormState, props: Props): boolean {
  if (isDateRangeInverted(form)) return false
  return getExistingAssistanceActionRanges(props).some((existing) =>
    rangeContainsDate(form, existing.startDate)
  )
}

export default React.memo(function AssistanceActionForm(props: Props) {
  const { i18n } = useTranslation()
  const { clearUiMode, setErrorMessage } = useContext(UIContext)

  const initialFormState: FormState = useMemo(
    () =>
      isCreate(props) && !isDuplicate(props)
        ? {
            startDate: LocalDate.today(),
            endDate: LocalDate.today(),
            actions: new Set(),
            otherSelected: false,
            otherAction: '',
            measures: new Set()
          }
        : {
            ...props.assistanceAction,
            otherSelected: props.assistanceAction.otherAction !== ''
          },
    [props]
  )
  const [form, setForm] = useState<FormState>(initialFormState)

  const [formErrors, setFormErrors] =
    useState<AssistanceActionFormErrors>(noErrors)

  const [autoCutWarning, setAutoCutWarning] = useState<boolean>(false)

  const updateFormState: UpdateStateFn<FormState> = (values) => {
    const newState = { ...form, ...values }
    setForm(newState)
  }

  useEffect(() => {
    const isSoftConflict = checkSoftConflict(form, props)
    const isHardConflict = checkHardConflict(form, props)
    const isAnyConflict = isSoftConflict || isHardConflict

    setFormErrors({
      dateRange: {
        inverted: isDateRangeInverted(form),
        conflict: isCreate(props) ? isHardConflict : isAnyConflict
      }
    })

    setAutoCutWarning(isCreate(props) && isSoftConflict && !isHardConflict)
  }, [form, props])

  const submitForm = (event: FormEvent<HTMLFormElement>) => {
    event.preventDefault()

    if (formHasErrors(formErrors)) return

    const data: AssistanceActionRequest = {
      ...form,
      actions: [...form.actions],
      measures: [...form.measures]
    }

    const apiCall = isCreate(props)
      ? createAssistanceAction(props.childId, data)
      : updateAssistanceAction(props.assistanceAction.id, data)

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
            label: i18n.childInformation.assistanceAction.fields.dateRange,
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
                      ? i18n.childInformation.assistanceAction.errors
                          .hardConflict
                      : i18n.childInformation.assistanceAction.errors.conflict}
                  </span>
                )}
              </>
            )
          },
          {
            label: i18n.childInformation.assistanceAction.fields.actions,
            value: (
              <div>
                {props.assistanceActionOptions.map((option) =>
                  option.descriptionFi ? (
                    <ExpandingInfo
                      key={option.value}
                      info={option.descriptionFi}
                      ariaLabel=""
                      width="full"
                    >
                      <CheckboxRow>
                        <Checkbox
                          label={option.nameFi}
                          checked={form.actions.has(option.value)}
                          onChange={(value) => {
                            const actions = new Set([...form.actions])
                            if (value) actions.add(option.value)
                            else actions.delete(option.value)
                            updateFormState({ actions: actions })
                          }}
                        />
                      </CheckboxRow>
                    </ExpandingInfo>
                  ) : (
                    <CheckboxRow key={option.value}>
                      <Checkbox
                        label={option.nameFi}
                        checked={form.actions.has(option.value)}
                        onChange={(value) => {
                          const actions = new Set([...form.actions])
                          if (value) actions.add(option.value)
                          else actions.delete(option.value)
                          updateFormState({ actions: actions })
                        }}
                      />
                    </CheckboxRow>
                  )
                )}
                {featureFlags.assistanceActionOtherEnabled ? (
                  <>
                    <CheckboxRow>
                      <Checkbox
                        label={
                          i18n.childInformation.assistanceAction.fields
                            .actionTypes.OTHER
                        }
                        checked={form.otherSelected}
                        onChange={(value) => {
                          updateFormState({
                            otherSelected: value,
                            otherAction: ''
                          })
                        }}
                      />
                    </CheckboxRow>
                    {form.otherSelected && (
                      <InputField
                        value={form.otherAction}
                        onChange={(value) =>
                          updateFormState({ otherAction: value })
                        }
                        placeholder={
                          i18n.childInformation.assistanceAction.fields
                            .otherActionPlaceholder
                        }
                      />
                    )}
                  </>
                ) : null}
              </div>
            ),
            valueWidth: '100%'
          },
          assistanceMeasures.length > 0 && {
            label: i18n.childInformation.assistanceAction.fields.measures,
            value: (
              <div>
                {assistanceMeasures.map((measure) =>
                  i18n.childInformation.assistanceAction.fields.measureTypes[
                    `${measure}_INFO`
                  ] ? (
                    <ExpandingInfo
                      key={measure}
                      info={String(
                        i18n.childInformation.assistanceAction.fields
                          .measureTypes[`${measure}_INFO`]
                      )}
                      ariaLabel=""
                      width="full"
                    >
                      <CheckboxRow key={measure}>
                        <Checkbox
                          label={
                            i18n.childInformation.assistanceAction.fields
                              .measureTypes[measure]
                          }
                          checked={form.measures.has(measure)}
                          onChange={(value) => {
                            const measures = new Set([...form.measures])
                            if (value) measures.add(measure)
                            else measures.delete(measure)
                            updateFormState({ measures: measures })
                          }}
                        />
                      </CheckboxRow>
                    </ExpandingInfo>
                  ) : (
                    <CheckboxRow key={measure}>
                      <Checkbox
                        label={
                          i18n.childInformation.assistanceAction.fields
                            .measureTypes[measure]
                        }
                        checked={form.measures.has(measure)}
                        onChange={(value) => {
                          const measures = new Set([...form.measures])
                          if (value) measures.add(measure)
                          else measures.delete(measure)
                          updateFormState({ measures: measures })
                        }}
                      />
                    </CheckboxRow>
                  )
                )}
              </div>
            )
          }
        ]}
      />

      {autoCutWarning && (
        <>
          <Gap size="xs" />
          <AlertBox
            message={
              i18n.childInformation.assistanceAction.errors.autoCutWarning
            }
            thin
            wide
          />
        </>
      )}

      <Gap size="s" />
      <FormActions
        onCancel={() => clearUiMode()}
        disabled={formHasErrors(formErrors)}
        data-qa="button-assistance-action-confirm"
      />
    </form>
  )
})
