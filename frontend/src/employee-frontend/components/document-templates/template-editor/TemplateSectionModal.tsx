// SPDX-FileCopyrightText: 2017-2023 City of Espoo
//
// SPDX-License-Identifier: LGPL-2.1-or-later

import React from 'react'

import { useForm, useFormFields } from 'lib-common/form/hooks'
import { StateOf } from 'lib-common/form/types'
import { InputFieldF } from 'lib-components/atoms/form/InputField'
import { AsyncFormModal } from 'lib-components/molecules/modals/FormModal'
import { Label } from 'lib-components/typography'

import { useTranslation } from '../../../state/i18n'
import { templateSectionForm } from '../forms'

interface Props {
  initialState?: StateOf<typeof templateSectionForm>
  onSave: (state: StateOf<typeof templateSectionForm>) => void
  onCancel: () => void
}

export default React.memo(function TemplateSectionModal({
  initialState,
  onSave,
  onCancel
}: Props) {
  const { i18n } = useTranslation()

  const form = useForm(
    templateSectionForm,
    () =>
      initialState ?? {
        id: crypto.randomUUID(),
        label: '',
        questions: []
      },
    {
      ...i18n.validationErrors
    }
  )
  const { label } = useFormFields(form)

  return (
    <AsyncFormModal
      title={initialState ? 'Muokkaa osiota' : 'Uusi osio'}
      resolveAction={() => onSave(form.state)}
      onSuccess={onCancel}
      resolveLabel={i18n.common.confirm}
      rejectAction={onCancel}
      rejectLabel={i18n.common.cancel}
      resolveDisabled={!form.isValid()}
    >
      <Label>Otsikko</Label>
      <InputFieldF bind={label} hideErrorsBeforeTouched />
    </AsyncFormModal>
  )
})
