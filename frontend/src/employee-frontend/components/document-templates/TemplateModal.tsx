// SPDX-FileCopyrightText: 2017-2023 City of Espoo
//
// SPDX-License-Identifier: LGPL-2.1-or-later

import React from 'react'

import { localOpenEndedDateRange, string } from 'lib-common/form/fields'
import { object, required, validated } from 'lib-common/form/form'
import { useForm, useFormFields } from 'lib-common/form/hooks'
import { nonEmpty } from 'lib-common/form/validators'
import { useMutationResult } from 'lib-common/query'
import { InputFieldF } from 'lib-components/atoms/form/InputField'
import { DateRangePickerF } from 'lib-components/molecules/date-picker/DateRangePicker'
import { AsyncFormModal } from 'lib-components/molecules/modals/FormModal'
import { Label } from 'lib-components/typography'
import { Gap } from 'lib-components/white-space'

import { useTranslation } from '../../state/i18n'

import { createDocumentTemplateMutation } from './queries'

const documentTemplateForm = object({
  name: validated(string(), nonEmpty),
  validity: required(localOpenEndedDateRange)
})

interface Props {
  onClose: () => void
}

export default React.memo(function TemplateModal({ onClose }: Props) {
  const { i18n, lang } = useTranslation()

  const { mutateAsync: createDocumentTemplate } = useMutationResult(
    createDocumentTemplateMutation
  )

  const form = useForm(
    documentTemplateForm,
    () => ({
      name: '',
      validity: {
        startDate: null,
        endDate: null
      }
    }),
    {
      ...i18n.validationErrors
    }
  )

  const { name, validity } = useFormFields(form)

  return (
    <AsyncFormModal
      title="Uusi lomakepohja"
      resolveAction={() => createDocumentTemplate(form.value())}
      onSuccess={onClose}
      resolveLabel={i18n.common.confirm}
      rejectAction={onClose}
      rejectLabel={i18n.common.cancel}
      resolveDisabled={!form.isValid()}
    >
      <Label>Nimi</Label>
      <InputFieldF bind={name} hideErrorsBeforeTouched />
      <Gap />
      <Label>Käytössä ajalla</Label>
      <DateRangePickerF bind={validity} locale={lang} />
    </AsyncFormModal>
  )
})
