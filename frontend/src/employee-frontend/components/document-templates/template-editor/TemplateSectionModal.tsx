// SPDX-FileCopyrightText: 2017-2023 City of Espoo
//
// SPDX-License-Identifier: LGPL-2.1-or-later

import React from 'react'
import { v4 as uuidv4 } from 'uuid'

import { useForm, useFormFields } from 'lib-common/form/hooks'
import { StateOf } from 'lib-common/form/types'
import { InputFieldF } from 'lib-components/atoms/form/InputField'
import { TextAreaF } from 'lib-components/atoms/form/TextArea'
import { FixedSpaceColumn } from 'lib-components/layout/flex-helpers'
import { AsyncFormModal } from 'lib-components/molecules/modals/FormModal'
import { Label } from 'lib-components/typography'

import { useTranslation } from '../../../state/i18n'
import { templateSectionForm } from '../templates'

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
        id: uuidv4(),
        label: '',
        questions: [],
        infoText: ''
      },
    {
      ...i18n.validationErrors
    }
  )
  const { label, infoText } = useFormFields(form)

  return (
    <AsyncFormModal
      title={
        initialState
          ? i18n.documentTemplates.templateEditor.titleEditSection
          : i18n.documentTemplates.templateEditor.titleNewSection
      }
      resolveAction={() => onSave(form.state)}
      onSuccess={onCancel}
      resolveLabel={i18n.common.confirm}
      rejectAction={onCancel}
      rejectLabel={i18n.common.cancel}
      resolveDisabled={!form.isValid()}
    >
      <FixedSpaceColumn>
        <FixedSpaceColumn>
          <Label>{i18n.documentTemplates.templateEditor.sectionName}</Label>
          <InputFieldF
            bind={label}
            hideErrorsBeforeTouched
            data-qa="name-input"
          />
        </FixedSpaceColumn>
        <FixedSpaceColumn>
          <Label>{i18n.documentTemplates.templateEditor.infoText}</Label>
          <TextAreaF bind={infoText} hideErrorsBeforeTouched />
        </FixedSpaceColumn>
      </FixedSpaceColumn>
    </AsyncFormModal>
  )
})
