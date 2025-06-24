// SPDX-FileCopyrightText: 2017-2022 City of Espoo
//
// SPDX-License-Identifier: LGPL-2.1-or-later

import sortBy from 'lodash/sortBy'
import type { FormEvent } from 'react'
import React, { useContext, useEffect, useMemo, useState } from 'react'
import styled from 'styled-components'

import type { UpdateStateFn } from 'lib-common/form-state'
import type {
  AssistanceAction,
  AssistanceActionOption,
  AssistanceActionOptionCategory,
  AssistanceActionRequest,
  AssistanceActionResponse
} from 'lib-common/generated/api-types/assistanceaction'
import { assistanceActionOptionCategories } from 'lib-common/generated/api-types/assistanceaction'
import type { ChildId } from 'lib-common/generated/api-types/shared'
import LocalDate from 'lib-common/local-date'
import { useMutationResult } from 'lib-common/query'
import Checkbox from 'lib-components/atoms/form/Checkbox'
import InputField from 'lib-components/atoms/form/InputField'
import {
  FixedSpaceColumn,
  FixedSpaceRow
} from 'lib-components/layout/flex-helpers'
import ExpandingInfo from 'lib-components/molecules/ExpandingInfo'
import LabelValueList from 'lib-components/molecules/LabelValueList'
import { AlertBox } from 'lib-components/molecules/MessageBoxes'
import DatePicker from 'lib-components/molecules/date-picker/DatePicker'
import { DatePickerSpacer } from 'lib-components/molecules/date-picker/DateRangePicker'
import { Gap } from 'lib-components/white-space'
import type { Translations } from 'lib-customizations/employee'
import { featureFlags } from 'lib-customizations/employee'

import { useTranslation } from '../../../state/i18n'
import { UIContext } from '../../../state/ui'
import type { DateRange } from '../../../utils/date'
import { rangeContainsDate } from '../../../utils/date'
import { isDateRangeInverted } from '../../../utils/validation/validations'
import FormActions from '../../common/FormActions'
import {
  createAssistanceActionMutation,
  updateAssistanceActionMutation
} from '../queries'

const CheckboxList = styled(FixedSpaceColumn).attrs({ spacing: 'xxs' })``

interface FormState {
  startDate: LocalDate | null
  endDate: LocalDate | null
  actions: string[]
  otherSelected: boolean
  otherAction: string
}

interface CommonProps {
  assistanceActions: AssistanceActionResponse[]
  assistanceActionOptions: AssistanceActionOption[]
}

interface CreateProps extends CommonProps {
  childId: ChildId
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

interface AssistanceActionFormErrors {
  dateRange: 'required' | 'inverted' | 'conflict' | undefined
  invalidOption: boolean
}

const noErrors: AssistanceActionFormErrors = {
  dateRange: undefined,
  invalidOption: false
}

const getExistingAssistanceActionRanges = (props: Props): DateRange[] =>
  props.assistanceActions
    .filter(
      ({ action }) => isCreate(props) || action.id !== props.assistanceAction.id
    )
    .map(({ action: { startDate, endDate } }) => ({ startDate, endDate }))

function checkSoftConflict(form: FormState, props: Props): boolean {
  const { startDate, endDate } = form
  if (!startDate || !endDate) return false
  if (isDateRangeInverted({ startDate, endDate })) return false
  return getExistingAssistanceActionRanges(props).some((existing) =>
    rangeContainsDate(existing, startDate)
  )
}

function checkHardConflict(form: FormState, props: Props): boolean {
  const { startDate, endDate } = form
  if (!startDate || !endDate) return false
  if (isDateRangeInverted({ startDate, endDate })) return false
  return getExistingAssistanceActionRanges(props).some((existing) =>
    rangeContainsDate({ startDate, endDate }, existing.startDate)
  )
}

function validateOption(
  i18n: Translations,
  assistanceActionStart: LocalDate | null,
  assistanceActionEnd: LocalDate | null,
  optionValidityStart: LocalDate | null,
  optionValidityEnd: LocalDate | null
) {
  // date range not selected or invalid, cannot validate yet
  if (
    !assistanceActionStart ||
    !assistanceActionEnd ||
    assistanceActionStart.isAfter(assistanceActionEnd)
  )
    return undefined

  // hide option that is no longer valid during any part of the selected range
  if (optionValidityEnd && assistanceActionStart.isAfter(optionValidityEnd))
    return false

  // fail validation if option is not valid for the action's entire duration
  if (
    optionValidityStart &&
    assistanceActionStart.isBefore(optionValidityStart)
  )
    return i18n.childInformation.assistanceAction.errors.startBeforeMinDate(
      optionValidityStart
    )
  if (optionValidityEnd && assistanceActionEnd.isAfter(optionValidityEnd))
    return i18n.childInformation.assistanceAction.errors.endAfterMaxDate(
      optionValidityEnd
    )

  // option is valid
  return true
}

export default React.memo(function AssistanceActionForm(props: Props) {
  const { i18n } = useTranslation()
  const { clearUiMode, setErrorMessage } = useContext(UIContext)

  const initialFormState: FormState = useMemo(
    () =>
      isCreate(props) && !isDuplicate(props)
        ? {
            startDate: LocalDate.todayInSystemTz(),
            endDate: LocalDate.todayInSystemTz(),
            actions: [],
            otherSelected: false,
            otherAction: ''
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

  const { mutateAsync: createAssistanceAction } = useMutationResult(
    createAssistanceActionMutation
  )
  const { mutateAsync: updateAssistanceAction } = useMutationResult(
    updateAssistanceActionMutation
  )

  const optionsWithValidation = useMemo(
    () =>
      props.assistanceActionOptions.map((o) => ({
        ...o,
        validation: validateOption(
          i18n,
          form.startDate,
          form.endDate,
          o.validFrom,
          o.validTo
        )
      })),
    [i18n, props.assistanceActionOptions, form.startDate, form.endDate]
  )

  const sortedOptions = useMemo(
    () =>
      assistanceActionOptionCategories.reduce(
        (acc, category) => ({
          ...acc,
          [category]: sortBy(
            optionsWithValidation.filter(
              (o) => o.category === category && o.validation !== false
            ),
            [(o) => o.displayOrder, (o) => o.nameFi]
          )
        }),
        {} as Record<
          AssistanceActionOptionCategory,
          typeof optionsWithValidation
        >
      ),
    [optionsWithValidation]
  )

  useEffect(() => {
    const isSoftConflict = checkSoftConflict(form, props)
    const isHardConflict = checkHardConflict(form, props)
    const isAnyConflict = isSoftConflict || isHardConflict

    const { startDate, endDate } = form
    const required = startDate === null || endDate === null
    const inverted = !required && isDateRangeInverted({ startDate, endDate })
    const conflict = isCreate(props) ? isHardConflict : isAnyConflict

    setFormErrors({
      dateRange: required
        ? 'required'
        : inverted
          ? 'inverted'
          : conflict
            ? 'conflict'
            : undefined,
      invalidOption: !form.actions.every((action) =>
        optionsWithValidation.some(
          (option) =>
            option.value === action && typeof option.validation !== 'string'
        )
      )
    })

    setAutoCutWarning(isCreate(props) && isSoftConflict && !isHardConflict)
  }, [form, props, optionsWithValidation])

  const submitForm = (event: FormEvent<HTMLFormElement>) => {
    event.preventDefault()

    const { startDate, endDate } = form
    if (!startDate || !endDate || formHasErrors(formErrors)) return

    const body: AssistanceActionRequest = {
      ...form,
      startDate,
      endDate,
      actions: form.actions.filter((action) =>
        // filter out hidden options that are still selected in the state
        optionsWithValidation.some(
          (option) => option.value === action && option.validation !== false
        )
      )
    }

    const apiCall = isCreate(props)
      ? createAssistanceAction({ childId: props.childId, body })
      : updateAssistanceAction({
          id: props.assistanceAction.id,
          childId: props.assistanceAction.childId,
          body
        })

    void apiCall.then((res) => {
      if (res.isSuccess) {
        clearUiMode()
      } else if (res.isFailure) {
        if (res.statusCode === 409) {
          setFormErrors({ ...formErrors, dateRange: 'conflict' })
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
        alignItems="flex-start"
        contents={[
          {
            label: i18n.childInformation.assistanceAction.fields.dateRange,
            value: (
              <>
                <FixedSpaceRow>
                  <DatePicker
                    date={form.startDate}
                    onChange={(startDate) => updateFormState({ startDate })}
                    locale="fi"
                  />
                  <DatePickerSpacer />
                  <DatePicker
                    date={form.endDate}
                    onChange={(endDate) => updateFormState({ endDate })}
                    locale="fi"
                  />
                </FixedSpaceRow>

                {formErrors.dateRange === 'required' && (
                  <span className="error">
                    {i18n.validationError.mandatoryField}
                  </span>
                )}
                {formErrors.dateRange === 'inverted' && (
                  <span className="error">
                    {i18n.validationError.invertedDateRange}
                  </span>
                )}
                {formErrors.dateRange === 'conflict' && (
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
          ...assistanceActionOptionCategories.map((category) => ({
            label:
              i18n.childInformation.assistanceAction.fields.actionsByCategory[
                category
              ],
            value: (
              <CheckboxList>
                {sortedOptions[category].map((option) => (
                  <ExpandingInfo
                    key={option.value}
                    info={option.descriptionFi}
                    width="full"
                  >
                    <Checkbox
                      label={option.nameFi}
                      checked={form.actions.includes(option.value)}
                      onChange={(value) => {
                        const actions = new Set([...form.actions])
                        if (value) actions.add(option.value)
                        else actions.delete(option.value)
                        updateFormState({ actions: Array.from(actions) })
                      }}
                    />
                    {form.actions.includes(option.value) &&
                      typeof option.validation === 'string' && (
                        <>
                          <Gap size="xs" />
                          <AlertBox message={option.validation} thin noMargin />
                          <Gap />
                        </>
                      )}
                  </ExpandingInfo>
                ))}
              </CheckboxList>
            )
          })),
          ...(featureFlags.assistanceActionOther
            ? [
                {
                  label:
                    i18n.childInformation.assistanceAction.fields
                      .actionsByCategory.OTHER,
                  value: (
                    <div>
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
                    </div>
                  ),
                  valueWidth: '100%'
                }
              ]
            : [])
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

function formHasErrors(errors: AssistanceActionFormErrors) {
  return errors.dateRange !== undefined || errors.invalidOption
}
