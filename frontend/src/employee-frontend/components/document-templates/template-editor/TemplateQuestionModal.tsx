// SPDX-FileCopyrightText: 2017-2023 City of Espoo
//
// SPDX-License-Identifier: LGPL-2.1-or-later

import React from 'react'

import { useForm } from 'lib-common/form/hooks'
import { StateOf } from 'lib-common/form/types'
import Select from 'lib-components/atoms/dropdowns/Select'
import { AsyncFormModal } from 'lib-components/molecules/modals/FormModal'
import { Gap } from 'lib-components/white-space'

import { useTranslation } from '../../../state/i18n'
import TextQuestionDescriptor from '../question-descriptors/TextQuestionDescriptor'
import { questionTypes } from '../question-descriptors/types'
import {
  getTemplateQuestionInitialStateByType,
  TemplateQuestionConfigView,
  templateQuestionForm
} from '../templates'

interface Props {
  initialState?: StateOf<typeof templateQuestionForm>
  onSave: (state: StateOf<typeof templateQuestionForm>) => void
  onCancel: () => void
}

export default React.memo(function TemplateQuestionModal({
  initialState,
  onSave,
  onCancel
}: Props) {
  const { i18n } = useTranslation()

  // form contents are copied so that original is not changed when clicking cancel
  const form = useForm(
    templateQuestionForm,
    () => initialState ?? TextQuestionDescriptor.template.getInitialState(),
    i18n.validationErrors
  )

  return (
    <AsyncFormModal
      title={
        initialState
          ? i18n.documentTemplates.templateEditor.titleEditQuestion
          : i18n.documentTemplates.templateEditor.titleNewQuestion
      }
      resolveAction={() => onSave(form.state)}
      onSuccess={onCancel}
      resolveLabel={i18n.common.confirm}
      rejectAction={onCancel}
      rejectLabel={i18n.common.cancel}
      resolveDisabled={!form.isValid()}
    >
      <Select
        items={questionTypes}
        selectedItem={form.state.branch}
        getItemLabel={(item) => i18n.documentTemplates.questionTypes[item]}
        onChange={(branch) => {
          if (branch !== null) {
            form.set(getTemplateQuestionInitialStateByType(branch))
          }
        }}
      />
      <Gap />
      <TemplateQuestionConfigView bind={form} />
    </AsyncFormModal>
  )
})
