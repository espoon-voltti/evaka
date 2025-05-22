// SPDX-FileCopyrightText: 2017-2023 City of Espoo
//
// SPDX-License-Identifier: LGPL-2.1-or-later

import React from 'react'
import styled from 'styled-components'

import { useForm } from 'lib-common/form/hooks'
import type { StateOf } from 'lib-common/form/types'
import { questionTypes } from 'lib-common/generated/api-types/document'
import Select from 'lib-components/atoms/dropdowns/Select'
import TextQuestionDescriptor from 'lib-components/document-templates/question-descriptors/TextQuestionDescriptor'
import { AsyncFormModal } from 'lib-components/molecules/modals/FormModal'
import { defaultMargins, Gap } from 'lib-components/white-space'
import colors from 'lib-customizations/common'

import { useTranslation } from '../../../state/i18n'
import {
  getTemplateQuestionInitialStateByType,
  templateQuestionForm
} from '../forms'
import {
  TemplateQuestionConfigView,
  TemplateQuestionPreview
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
      width="extra-wide"
    >
      <SplitView>
        <FormPanel>
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
        </FormPanel>
        <PreviewPanel>
          <TemplateQuestionPreview bind={form} />
        </PreviewPanel>
      </SplitView>
    </AsyncFormModal>
  )
})

const SplitView = styled.div`
  display: flex;
`

const FormPanel = styled.div`
  width: 50%;
  padding-right: ${defaultMargins.m};
  border-right: dashed 1px ${colors.grayscale.g35};
`

const PreviewPanel = styled.div`
  width: 50%;
  padding-left: ${defaultMargins.m};
`
