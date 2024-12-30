import React, { Fragment } from 'react'

import { useTranslation } from 'employee-frontend/state/i18n'
import { Result } from 'lib-common/api'
import { localDateRange } from 'lib-common/form/fields'
import { object, required } from 'lib-common/form/form'
import { useForm, useFormFields } from 'lib-common/form/hooks'
import { StateOf } from 'lib-common/form/types'
import { AsyncButton } from 'lib-components/atoms/buttons/AsyncButton'
import { Button } from 'lib-components/atoms/buttons/Button'
import ButtonContainer from 'lib-components/layout/ButtonContainer'
import { DateRangePickerF } from 'lib-components/molecules/date-picker/DateRangePicker'
import { Gap } from 'lib-components/white-space'

const outOfOfficeForm = object({ period: required(localDateRange()) })

function initialFormState(): StateOf<typeof outOfOfficeForm> {
  return { period: localDateRange.empty() }
}

export interface OutOfOfficeEditorProps {
  onCancel: () => void
}

export default React.memo(function OutOfOfficeEditor({
  onCancel
}: OutOfOfficeEditorProps) {
  const { i18n, lang } = useTranslation()

  const form = useForm(outOfOfficeForm, initialFormState, {
    ...i18n.validationErrors,
    ...i18n.components.datePicker.validationErrors
  })

  const { period } = useFormFields(form)

  return (
    <Fragment>
      <DateRangePickerF bind={period} locale={lang} hideErrorsBeforeTouched />
      <Gap size="X4L" />
      <ButtonContainer>
        <AsyncButton
          text={i18n.common.save}
          primary
          disabled={!form.isValid()}
          onClick={function (): void | Promise<Result<unknown>> {
            throw new Error('Function not implemented.')
          }}
          onSuccess={function (): void {
            throw new Error('Function not implemented.')
          }}
        />
        <Gap size="xs" horizontal />
        <Button
          text={i18n.common.cancel}
          appearance="inline"
          onClick={onCancel}
        />
      </ButtonContainer>
    </Fragment>
  )
})
