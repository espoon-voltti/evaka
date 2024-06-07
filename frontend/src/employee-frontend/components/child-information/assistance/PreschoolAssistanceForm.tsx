// SPDX-FileCopyrightText: 2017-2023 City of Espoo
//
// SPDX-License-Identifier: LGPL-2.1-or-later

import React, { useCallback } from 'react'

import { Result } from 'lib-common/api'
import { localDateRange } from 'lib-common/form/fields'
import {
  object,
  oneOf,
  OneOfOption,
  required,
  transformed,
  value
} from 'lib-common/form/form'
import { useForm, useFormFields } from 'lib-common/form/hooks'
import { ValidationError, ValidationSuccess } from 'lib-common/form/types'
import {
  PreschoolAssistance,
  PreschoolAssistanceLevel,
  PreschoolAssistanceResponse,
  PreschoolAssistanceUpdate
} from 'lib-common/generated/api-types/assistance'
import { UUID } from 'lib-common/types'
import { Button } from 'lib-components/atoms/buttons/Button'
import { SelectF } from 'lib-components/atoms/dropdowns/Select'
import { Td, Tr } from 'lib-components/layout/Table'
import { FixedSpaceRow } from 'lib-components/layout/flex-helpers'
import { DateRangePickerF } from 'lib-components/molecules/date-picker/DateRangePicker'
import { preschoolAssistanceLevels } from 'lib-customizations/employee'

import { Translations, useTranslation } from '../../../state/i18n'
import { getStatusLabelByDateRange } from '../../../utils/date'
import StatusLabel from '../../common/StatusLabel'

export const preschoolAssistanceForm = transformed(
  object({
    level: required(oneOf<PreschoolAssistanceLevel>()),
    validDuring: required(localDateRange()),
    allRows: value<PreschoolAssistanceResponse[]>(),
    ignoredId: value<UUID | undefined>()
  }),
  ({ level, validDuring, allRows, ignoredId }) => {
    if (
      allRows.some(
        ({ data }) =>
          data.id !== ignoredId && data.validDuring.overlaps(validDuring)
      )
    ) {
      return ValidationError.of('overlap')
    }
    const success: PreschoolAssistanceUpdate = { level, validDuring }
    return ValidationSuccess.of(success)
  }
)

interface Props {
  preschoolAssistance?: PreschoolAssistance
  allRows: PreschoolAssistanceResponse[]
  onClose: () => void
  onSubmit: (factor: PreschoolAssistanceUpdate) => Promise<Result<void>>
}

const levelOptions = (
  i18n: Translations
): OneOfOption<PreschoolAssistanceLevel>[] =>
  preschoolAssistanceLevels.map((level) => ({
    value: level,
    domValue: level,
    label:
      i18n.childInformation.assistance.types.preschoolAssistanceLevel[level]
  }))

export const PreschoolAssistanceForm = React.memo(
  function PreschoolAssistanceForm(props: Props) {
    const initialData = props.preschoolAssistance
    const { i18n, lang } = useTranslation()

    const form = useForm(
      preschoolAssistanceForm,
      () => ({
        level: {
          domValue: initialData?.level ?? preschoolAssistanceLevels[0],
          options: levelOptions(i18n)
        },
        validDuring: localDateRange.fromRange(initialData?.validDuring),
        allRows: props.allRows,
        ignoredId: initialData?.id
      }),
      {
        ...i18n.validationErrors,
        ...i18n.childInformation.assistance.validationErrors
      }
    )
    const { level, validDuring } = useFormFields(form)

    const onSubmit = useCallback(
      () =>
        props.onSubmit(form.value()).then((result) => {
          if (result.isSuccess) props.onClose()
        }),
      [props, form]
    )

    return (
      <Tr data-qa="preschool-assistance-form">
        <Td>
          <SelectF data-qa="level" bind={level} />
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
  }
)
