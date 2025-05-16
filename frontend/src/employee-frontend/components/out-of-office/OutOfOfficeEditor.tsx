// SPDX-FileCopyrightText: 2017-2024 City of Espoo
//
// SPDX-License-Identifier: LGPL-2.1-or-later

import React from 'react'

import { localDateRange } from 'lib-common/form/fields'
import { object, required, validated } from 'lib-common/form/form'
import { useForm, useFormFields } from 'lib-common/form/hooks'
import { StateOf } from 'lib-common/form/types'
import { OutOfOfficePeriod } from 'lib-common/generated/api-types/outofoffice'
import LocalDate from 'lib-common/local-date'
import { useMutationResult } from 'lib-common/query'
import { AsyncButton } from 'lib-components/atoms/buttons/AsyncButton'
import { Button } from 'lib-components/atoms/buttons/Button'
import ButtonContainer from 'lib-components/layout/ButtonContainer'
import { DateRangePickerF } from 'lib-components/molecules/date-picker/DateRangePicker'
import { Gap } from 'lib-components/white-space'

import { useTranslation } from '../../state/i18n'

import { upsertOutOfOfficePeriodMutation } from './queries'

const outOfOfficeForm = object({
  period: validated(required(localDateRange()), (output) => {
    if (output.end.isBefore(LocalDate.todayInSystemTz())) {
      return {
        end: 'endBeforeToday'
      }
    }
    return undefined
  })
})

function initialFormState(
  editedPeriod: OutOfOfficePeriod | null
): StateOf<typeof outOfOfficeForm> {
  return {
    period: editedPeriod
      ? localDateRange.fromRange(editedPeriod.period)
      : localDateRange.empty()
  }
}

export interface OutOfOfficeEditorProps {
  onClose: () => void
  editedPeriod: OutOfOfficePeriod | null
}

export default React.memo(function OutOfOfficeEditor({
  onClose,
  editedPeriod
}: OutOfOfficeEditorProps) {
  const { i18n, lang } = useTranslation()

  const form = useForm(outOfOfficeForm, () => initialFormState(editedPeriod), {
    ...i18n.validationErrors,
    ...i18n.outOfOffice.validationErrors
  })

  const { period } = useFormFields(form)

  const { mutateAsync: upsertOutOfOffice } = useMutationResult(
    upsertOutOfOfficePeriodMutation
  )

  const onSubmit = () =>
    upsertOutOfOffice({
      body: {
        id: editedPeriod?.id ?? null,
        period: period.value()
      }
    })

  return (
    <div data-qa="out-of-office-editor">
      <DateRangePickerF
        bind={period}
        locale={lang}
        hideErrorsBeforeTouched
        required
      />
      <Gap size="X4L" />
      <ButtonContainer>
        <AsyncButton
          text={i18n.common.save}
          primary
          disabled={!form.isValid()}
          onClick={onSubmit}
          onSuccess={onClose}
          data-qa="save-out-of-office"
        />
        <Gap size="xs" horizontal />
        <Button
          text={i18n.common.cancel}
          appearance="inline"
          onClick={onClose}
        />
      </ButtonContainer>
    </div>
  )
})
