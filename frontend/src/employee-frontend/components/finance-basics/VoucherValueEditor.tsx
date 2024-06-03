// SPDX-FileCopyrightText: 2017-2022 City of Espoo
//
// SPDX-License-Identifier: LGPL-2.1-or-later

import React, { useCallback, useState } from 'react'

import DateRange from 'lib-common/date-range'
import { throwIfNull } from 'lib-common/form-validation'
import { ServiceNeedOptionVoucherValueRange } from 'lib-common/generated/api-types/invoicing'
import { parseCentsOrThrow } from 'lib-common/money'
import { useMutationResult } from 'lib-common/query'
import { UUID } from 'lib-common/types'
import AsyncButton from 'lib-components/atoms/buttons/AsyncButton'
import Button from 'lib-components/atoms/buttons/Button'
import InputField from 'lib-components/atoms/form/InputField'
import { Td, Tr } from 'lib-components/layout/Table'
import DatePicker from 'lib-components/molecules/date-picker/DatePicker'
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
}

export default React.memo(function VoucherValueEditor({
  i18n,
  id,
  initialState,
  close
}: VoucherValueEditorProps) {
  const [editorState, setEditorState] = useState<FormState>(initialState)

  const { mutateAsync: createVoucherValue } = useMutationResult(
    createVoucherValueMutation
  )

  const { mutateAsync: updateVoucherValue } = useMutationResult(
    updateVoucherValueMutation
  )

  const onSubmit = useCallback(() => {
    try {
      return id !== undefined
        ? updateVoucherValue({ id: id, body: parseFormOrThrow(editorState) })
        : createVoucherValue({ body: parseFormOrThrow(editorState) })
    } catch (e) {
      return Promise.reject(e)
    }
  }, [editorState, createVoucherValue, updateVoucherValue, id])

  return (
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
          hideErrorsBeforeTouched
          data-qa="value"
        />
      </Td>
      <Td>
        <Button text={i18n.common.cancel} onClick={close} />
        <AsyncButton
          primary
          text={i18n.common.save}
          onSuccess={close}
          onClick={onSubmit}
          data-qa="save-btn"
        />
      </Td>
    </Tr>
  )
})

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
