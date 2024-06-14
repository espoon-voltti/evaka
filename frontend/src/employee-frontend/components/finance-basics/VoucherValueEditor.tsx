// SPDX-FileCopyrightText: 2017-2022 City of Espoo
//
// SPDX-License-Identifier: LGPL-2.1-or-later

import React, { useCallback, useState } from 'react'
import styled from 'styled-components'

import DateRange from 'lib-common/date-range'
import { throwIfNull } from 'lib-common/form-validation'
import {
  ServiceNeedOptionVoucherValueRange,
  ServiceNeedOptionVoucherValueRangeWithId
} from 'lib-common/generated/api-types/invoicing'
import { isValidCents, parseCentsOrThrow } from 'lib-common/money'
import { useMutationResult } from 'lib-common/query'
import { UUID } from 'lib-common/types'
import { AsyncButton } from 'lib-components/atoms/buttons/AsyncButton'
import { LegacyButton } from 'lib-components/atoms/buttons/LegacyButton'
import InputField from 'lib-components/atoms/form/InputField'
import { Td, Tr } from 'lib-components/layout/Table'
import DatePicker from 'lib-components/molecules/date-picker/DatePicker'
import colors from 'lib-customizations/common'
import { Translations } from 'lib-customizations/employee'

import { FormState } from './ServiceNeedItem'
import {
  createVoucherValueMutation,
  updateVoucherValueMutation
} from './queries'

export type VoucherValueEditorProps = {
  i18n: Translations
  id: UUID | undefined
  initialState: FormState
  close: () => void
  existingVoucherValues: ServiceNeedOptionVoucherValueRangeWithId[]
}

export default React.memo(function VoucherValueEditor({
  i18n,
  id,
  initialState,
  close,
  existingVoucherValues
}: VoucherValueEditorProps) {
  const [editorState, setEditorState] = useState<FormState>(initialState)
  const validationResult = validateForm(
    i18n,
    editorState,
    existingVoucherValues.filter((voucherValue) => voucherValue.id != id)
  )

  const validationErrorInfo = (
    prop: keyof FormState
  ): { status: 'warning'; text: string } | undefined =>
    'errors' in validationResult && validationResult.errors[prop]
      ? {
          status: 'warning',
          text: ''
        }
      : undefined

  const { mutateAsync: createVoucherValue } = useMutationResult(
    createVoucherValueMutation
  )

  const { mutateAsync: updateVoucherValue } = useMutationResult(
    updateVoucherValueMutation
  )

  const onSubmit = useCallback(() => {
    if (!('payload' in validationResult)) return Promise.reject()
    return id !== undefined
      ? updateVoucherValue({ id: id, body: validationResult.payload })
      : createVoucherValue({ body: validationResult.payload })
  }, [validationResult, createVoucherValue, updateVoucherValue, id])

  return (
    <>
      <Tr key="edit">
        <Td>
          <DatePicker
            locale="fi"
            date={editorState.validFrom}
            onChange={(validFrom) =>
              setEditorState((previousState) => ({
                ...previousState,
                validFrom
              }))
            }
            hideErrorsBeforeTouched
            data-qa="valid-from"
          />
          <span>-</span>
          <DatePicker
            locale="fi"
            date={editorState.validTo}
            onChange={(validTo) =>
              setEditorState((previousState) => ({
                ...previousState,
                validTo
              }))
            }
            hideErrorsBeforeTouched
            data-qa="valid-to"
          />
        </Td>
        <Td>
          <InputField
            id="baseValue"
            width="s"
            value={editorState.baseValue}
            onChange={(baseValue) =>
              setEditorState((previousState) => ({
                ...previousState,
                baseValue,
                value:
                  baseValue == ''
                    ? '0.00'
                    : (
                        parseFloat(baseValue) *
                        parseFloat(previousState.coefficient)
                      ).toFixed(2)
              }))
            }
            placeholder="0.00"
            symbol="€"
            info={validationErrorInfo('baseValue')}
            hideErrorsBeforeTouched
            data-qa="base-value"
          />
        </Td>
        <Td>
          <InputField
            id="coefficient"
            width="s"
            value={editorState.coefficient}
            onChange={(coefficient) =>
              setEditorState((previousState) => ({
                ...previousState,
                coefficient,
                value:
                  coefficient == ''
                    ? '0.00'
                    : (
                        parseFloat(previousState.baseValue) *
                        parseFloat(coefficient)
                      ).toFixed(2)
              }))
            }
            placeholder="0.00"
            symbol=""
            info={validationErrorInfo('coefficient')}
            hideErrorsBeforeTouched
            data-qa="coefficient"
          />
        </Td>
        <Td>
          <InputField
            id="value"
            width="s"
            value={editorState.value}
            onChange={(value) =>
              setEditorState((previousState) => ({
                ...previousState,
                value
              }))
            }
            placeholder="0.00"
            symbol="€"
            info={validationErrorInfo('value')}
            hideErrorsBeforeTouched
            data-qa="value"
          />
        </Td>
        <Td>
          <InputField
            id="baseValueUnder3y"
            width="s"
            value={editorState.baseValueUnder3y}
            onChange={(baseValueUnder3y) =>
              setEditorState((previousState) => ({
                ...previousState,
                baseValueUnder3y,
                valueUnder3y:
                  baseValueUnder3y == ''
                    ? '0.00'
                    : (
                        parseFloat(baseValueUnder3y) *
                        parseFloat(previousState.coefficientUnder3y)
                      ).toFixed(2)
              }))
            }
            placeholder="0.00"
            symbol="€"
            info={validationErrorInfo('baseValueUnder3y')}
            hideErrorsBeforeTouched
            data-qa="base-value"
          />
        </Td>
        <Td>
          <InputField
            id="coefficientUnder3y"
            width="s"
            value={editorState.coefficientUnder3y}
            onChange={(coefficientUnder3y) =>
              setEditorState((previousState) => ({
                ...previousState,
                coefficientUnder3y,
                valueUnder3y:
                  coefficientUnder3y == ''
                    ? '0.00'
                    : (
                        parseFloat(previousState.baseValueUnder3y) *
                        parseFloat(coefficientUnder3y)
                      ).toFixed(2)
              }))
            }
            placeholder="0.00"
            symbol=""
            info={validationErrorInfo('coefficientUnder3y')}
            hideErrorsBeforeTouched
            data-qa="coefficient"
          />
        </Td>
        <Td>
          <InputField
            id="valueUnder3y"
            width="s"
            value={editorState.valueUnder3y}
            onChange={(valueUnder3y) =>
              setEditorState((previousState) => ({
                ...previousState,
                valueUnder3y
              }))
            }
            placeholder="0.00"
            symbol="€"
            info={validationErrorInfo('valueUnder3y')}
            hideErrorsBeforeTouched
            data-qa="value"
          />
        </Td>
        <Td>
          <LegacyButton text={i18n.common.cancel} onClick={close} />
          <AsyncButton
            primary
            text={i18n.common.save}
            onSuccess={close}
            disabled={'errors' in validationResult}
            onClick={onSubmit}
            data-qa="save-btn"
          />
        </Td>
      </Tr>
      <>
        {'errors' in validationResult &&
          Object.entries(validationResult.errors).map(([key, value]) => (
            <Tr key={key}>
              <Td colSpan={8}>
                <ErrorDescription>{value}</ErrorDescription>
              </Td>
            </Tr>
          ))}
      </>
    </>
  )
})

const ErrorDescription = styled.td`
  color: ${colors.grayscale.g70};
  font-style: italic;
  width: 100%;
`

type ValidationErrors = Partial<
  Record<keyof FormState | 'shouldNotHappen', string> &
    Record<'dateOverlap', string>
>

const centProps = [
  'baseValue',
  'value',
  'baseValueUnder3y',
  'valueUnder3y'
] as const

const decimalProps = ['coefficient', 'coefficientUnder3y'] as const

const decimalRegex = /^[0-9]+(([,.])[0-9]+)?$/

function validateForm(
  i18n: Translations,
  form: FormState,
  existingVoucherValues: ServiceNeedOptionVoucherValueRangeWithId[]
):
  | { errors: ValidationErrors }
  | { payload: ServiceNeedOptionVoucherValueRange } {
  const validationErrors: ValidationErrors = {}

  centProps.forEach((prop) => {
    if (!isValidCents(form[prop])) {
      validationErrors[prop] = i18n.validationError.cents
    }
  })

  decimalProps.forEach((prop) => {
    if (!decimalRegex.test(form[prop]))
      validationErrors[prop] = i18n.validationError.decimal
  })

  if (form.validFrom === null) {
    validationErrors.validFrom = i18n.validationError.dateRange
  }

  if (
    form.validFrom !== null &&
    form.validTo !== null &&
    form.validTo.isBefore(form.validFrom)
  ) {
    validationErrors.validFrom = i18n.validationError.invertedDateRange
    validationErrors.validTo = i18n.validationError.invertedDateRange
  }

  existingVoucherValues.forEach((voucherValue) => {
    if (
      form.validFrom !== null &&
      voucherValue.voucherValues.range.start >= form.validFrom
    )
      validationErrors.validFrom =
        i18n.financeBasics.serviceNeeds.errors['date-overlap']
  })

  try {
    return Object.keys(validationErrors).length === 0
      ? { payload: parseFormOrThrow(form) }
      : { errors: validationErrors }
  } catch (e) {
    validationErrors.shouldNotHappen =
      i18n.financeBasics.serviceNeeds.errors.shouldNotHappen
    return { errors: validationErrors }
  }
}

function parseFormOrThrow(form: FormState): ServiceNeedOptionVoucherValueRange {
  return {
    serviceNeedOptionId: form.serviceNeedOptionId,
    range: new DateRange(throwIfNull(form.validFrom), form.validTo),
    baseValue: parseCentsOrThrow(form.baseValue),
    coefficient: parseFloat(form.coefficient),
    value: parseCentsOrThrow(form.value),
    baseValueUnder3y: parseCentsOrThrow(form.baseValueUnder3y),
    coefficientUnder3y: parseFloat(form.coefficientUnder3y),
    valueUnder3y: parseCentsOrThrow(form.valueUnder3y)
  }
}
