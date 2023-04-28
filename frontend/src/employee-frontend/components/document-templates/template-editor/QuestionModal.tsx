// SPDX-FileCopyrightText: 2017-2023 City of Espoo
//
// SPDX-License-Identifier: LGPL-2.1-or-later

import React from 'react'

import { useForm, useFormUnion } from 'lib-common/form/hooks'
import { StateOf } from 'lib-common/form/types'
import { AsyncFormModal } from 'lib-components/molecules/modals/FormModal'

import { useTranslation } from '../../../state/i18n'
import { questionForm } from '../forms'
import TextQuestionForm from '../questions/TextQuestionForm'

interface Props {
  initialState?: StateOf<typeof questionForm>
  onSave: (state: StateOf<typeof questionForm>) => void
  onCancel: () => void
}

export default React.memo(function QuestionModal({
  initialState,
  onSave,
  onCancel
}: Props) {
  const { i18n } = useTranslation()

  const formUnion = useForm(
    questionForm,
    () =>
      initialState ?? {
        branch: 'TEXT' as const,
        state: {
          id: crypto.randomUUID(),
          label: ''
        }
      },
    {
      ...i18n.validationErrors
    }
  )
  const { branch, form } = useFormUnion(formUnion)

  return (
    <AsyncFormModal
      title={initialState ? 'Muokkaa kysymystä' : 'Uusi kysymys'}
      resolveAction={() => onSave(formUnion.state)}
      onSuccess={onCancel}
      resolveLabel={i18n.common.confirm}
      rejectAction={onCancel}
      rejectLabel={i18n.common.cancel}
      resolveDisabled={!form.isValid()}
    >
      {branch === 'TEXT' ? <TextQuestionForm bind={form} /> : null}
    </AsyncFormModal>
  )
})
