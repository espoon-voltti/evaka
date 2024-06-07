// SPDX-FileCopyrightText: 2017-2023 City of Espoo
//
// SPDX-License-Identifier: LGPL-2.1-or-later

import React, { useCallback } from 'react'

import { Result } from 'lib-common/api'
import { localDateRange, string } from 'lib-common/form/fields'
import {
  mapped,
  object,
  required,
  transformed,
  validated,
  value
} from 'lib-common/form/form'
import { useForm, useFormFields } from 'lib-common/form/hooks'
import { ValidationError, ValidationSuccess } from 'lib-common/form/types'
import {
  AssistanceFactor,
  AssistanceFactorResponse,
  AssistanceFactorUpdate
} from 'lib-common/generated/api-types/assistance'
import { UUID } from 'lib-common/types'
import { Button } from 'lib-components/atoms/buttons/Button'
import { InputFieldF } from 'lib-components/atoms/form/InputField'
import { Td, Tr } from 'lib-components/layout/Table'
import { FixedSpaceRow } from 'lib-components/layout/flex-helpers'
import { DateRangePickerF } from 'lib-components/molecules/date-picker/DateRangePicker'

import { useTranslation } from '../../../state/i18n'
import { getStatusLabelByDateRange } from '../../../utils/date'
import StatusLabel from '../../common/StatusLabel'

export const assistanceFactorForm = transformed(
  object({
    capacityFactor: validated(mapped(string(), Number.parseFloat), (number) =>
      Number.isFinite(number) && number >= 0.0 ? undefined : 'format'
    ),
    validDuring: required(localDateRange()),
    allRows: value<AssistanceFactorResponse[]>(),
    ignoredId: value<UUID | undefined>()
  }),
  ({ capacityFactor, validDuring, allRows, ignoredId }) => {
    if (
      allRows.some(
        ({ data }) =>
          data.id !== ignoredId && data.validDuring.overlaps(validDuring)
      )
    ) {
      return ValidationError.of('overlap')
    }
    const success: AssistanceFactorUpdate = { capacityFactor, validDuring }
    return ValidationSuccess.of(success)
  }
)

interface Props {
  assistanceFactor?: AssistanceFactor
  allRows: AssistanceFactorResponse[]
  onClose: () => void
  onSubmit: (factor: AssistanceFactorUpdate) => Promise<Result<void>>
}

export const AssistanceFactorForm = React.memo(function AssistanceFactorForm(
  props: Props
) {
  const initialData = props.assistanceFactor
  const { i18n, lang } = useTranslation()

  const form = useForm(
    assistanceFactorForm,
    () => ({
      capacityFactor: initialData?.capacityFactor.toString() ?? '',
      validDuring: localDateRange.fromRange(initialData?.validDuring),
      allRows: props.allRows,
      ignoredId: initialData?.id
    }),
    {
      ...i18n.validationErrors,
      ...i18n.childInformation.assistance.validationErrors
    }
  )
  const { capacityFactor, validDuring } = useFormFields(form)

  const onSubmit = useCallback(
    () =>
      props.onSubmit(form.value()).then((result) => {
        if (result.isSuccess) props.onClose()
      }),
    [props, form]
  )

  return (
    <Tr data-qa="assistance-factor-form">
      <Td>
        <InputFieldF
          bind={capacityFactor}
          type="number"
          hideErrorsBeforeTouched={true}
          data-qa="capacity-factor"
        />
      </Td>
      <Td>
        <DateRangePickerF
          bind={validDuring}
          locale={lang}
          data-qa="valid-during"
          info={form.inputInfo()}
        />
      </Td>
      <Td>
        {validDuring.isValid() ? (
          <StatusLabel
            status={getStatusLabelByDateRange({
              startDate: validDuring.value().start,
              endDate: validDuring.value().end
            })}
          />
        ) : undefined}
      </Td>
      <Td>
        <FixedSpaceRow justifyContent="flex-end" spacing="m">
          <Button
            appearance="inline"
            onClick={props.onClose}
            text={i18n.common.cancel}
            data-qa="cancel"
          />
          <Button
            appearance="inline"
            onClick={onSubmit}
            text={i18n.common.save}
            disabled={!form.isValid()}
            data-qa="save"
          />
        </FixedSpaceRow>
      </Td>
    </Tr>
  )
})
