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
import {
  getQuestionInitialStateByType,
  questionForm,
  questionTypes,
  TemplateQuestionConfigView
} from '../question-descriptors/questions'

interface Props {
  initialState?: StateOf<typeof questionForm>
  onSave: (state: StateOf<typeof questionForm>) => void
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
    questionForm,
    () =>
      initialState ?? {
        branch: 'TEXT' as const,
        state: TextQuestionDescriptor.getInitialState()
      },
    i18n.validationErrors
  )

  return (
    <AsyncFormModal
      title={initialState ? 'Muokkaa kysymystä' : 'Uusi kysymys'}
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
            form.set(getQuestionInitialStateByType(branch))
          }
        }}
      />
      <Gap />
      <TemplateQuestionConfigView bind={form} />
    </AsyncFormModal>
  )
})
