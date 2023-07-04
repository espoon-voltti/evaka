// SPDX-FileCopyrightText: 2017-2023 City of Espoo
//
// SPDX-License-Identifier: LGPL-2.1-or-later

import React, { useCallback } from 'react'

import { Result } from 'lib-common/api'
import { localDateRange } from 'lib-common/form/fields'
import {
  mapped,
  object,
  oneOf,
  OneOfOption,
  required
} from 'lib-common/form/form'
import { useForm, useFormFields } from 'lib-common/form/hooks'
import {
  DaycareAssistance,
  DaycareAssistanceLevel,
  daycareAssistanceLevels,
  DaycareAssistanceUpdate
} from 'lib-common/generated/api-types/assistance'
import InlineButton from 'lib-components/atoms/buttons/InlineButton'
import { SelectF } from 'lib-components/atoms/dropdowns/Select'
import { Td, Tr } from 'lib-components/layout/Table'
import { FixedSpaceRow } from 'lib-components/layout/flex-helpers'
import { DateRangePickerF } from 'lib-components/molecules/date-picker/DateRangePicker'

import { Translations, useTranslation } from '../../../state/i18n'
import { getStatusLabelByDateRange } from '../../../utils/date'
import StatusLabel from '../../common/StatusLabel'

export const daycareAssistanceForm = mapped(
  object({
    level: required(oneOf<DaycareAssistanceLevel>()),
    validDuring: required(localDateRange)
  }),
  (fields): DaycareAssistanceUpdate => ({
    level: fields.level,
    validDuring: fields.validDuring
  })
)

interface Props {
  daycareAssistance?: DaycareAssistance
  onClose: () => void
  onSubmit: (factor: DaycareAssistanceUpdate) => Promise<Result<void>>
}

const levelOptions = (
  i18n: Translations
): Array<OneOfOption<DaycareAssistanceLevel>> =>
  daycareAssistanceLevels.map((level) => ({
    value: level,
    domValue: level,
    label: i18n.childInformation.assistance.types.daycareAssistanceLevel[level]
  }))

export const DaycareAssistanceForm = React.memo(function DaycareAssistanceForm(
  props: Props
) {
  const initialData = props.daycareAssistance
  const { i18n, lang } = useTranslation()
  const form = useForm(
    daycareAssistanceForm,
    () => ({
      level: {
        domValue: initialData?.level ?? 'GENERAL_SUPPORT',
        options: levelOptions(i18n)
      },
      validDuring: {
        startDate: initialData?.validDuring.start ?? null,
        endDate: initialData?.validDuring.end ?? null
      }
    }),
    i18n.validationErrors
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
    <Tr data-qa="daycare-assistance-form">
      <Td>
        <SelectF data-qa="level" bind={level} />
      </Td>
      <Td>
        <DateRangePickerF
          bind={validDuring}
          locale={lang}
          data-qa="valid-during"
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
          <InlineButton
            onClick={props.onClose}
            text={i18n.common.cancel}
            data-qa="cancel"
          />
          <InlineButton
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
