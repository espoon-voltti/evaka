// SPDX-FileCopyrightText: 2017-2024 City of Espoo
//
// SPDX-License-Identifier: LGPL-2.1-or-later

import React, { Fragment } from 'react'

import { useTranslation } from 'employee-frontend/state/i18n'
import { localDateRange } from 'lib-common/form/fields'
import { object, required } from 'lib-common/form/form'
import { useForm, useFormFields } from 'lib-common/form/hooks'
import { StateOf } from 'lib-common/form/types'
import LocalDate from 'lib-common/local-date'
import { useMutationResult } from 'lib-common/query'
import { AsyncButton } from 'lib-components/atoms/buttons/AsyncButton'
import { Button } from 'lib-components/atoms/buttons/Button'
import ButtonContainer from 'lib-components/layout/ButtonContainer'
import { DateRangePickerF } from 'lib-components/molecules/date-picker/DateRangePicker'
import { Gap } from 'lib-components/white-space'

import { upsertOutOfOfficePeriodMutation } from './queries'

const outOfOfficeForm = object({ period: required(localDateRange()) })

function initialFormState(): StateOf<typeof outOfOfficeForm> {
  return {
    period: localDateRange.empty({ minDate: LocalDate.todayInSystemTz() })
  }
}

export interface OutOfOfficeEditorProps {
  onClose: () => void
}

export default React.memo(function OutOfOfficeEditor({
  onClose
}: OutOfOfficeEditorProps) {
  const { i18n, lang } = useTranslation()

  const form = useForm(outOfOfficeForm, initialFormState, {
    ...i18n.validationErrors
  })

  const { period } = useFormFields(form)

  const { mutateAsync: upsertOutOfOffice } = useMutationResult(
    upsertOutOfOfficePeriodMutation
  )

  const onSubmit = () =>
    upsertOutOfOffice({
      body: {
        id: null,
        period: period.value()
      }
    })

  return (
    <Fragment>
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
        />
        <Gap size="xs" horizontal />
        <Button
          text={i18n.common.cancel}
          appearance="inline"
          onClick={onClose}
        />
      </ButtonContainer>
    </Fragment>
  )
})
