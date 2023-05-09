// SPDX-FileCopyrightText: 2017-2023 City of Espoo
//
// SPDX-License-Identifier: LGPL-2.1-or-later

import React from 'react'

import {
  BoundForm,
  useForm,
  useFormElems,
  useFormFields
} from 'lib-common/form/hooks'
import { DocumentTemplateContent } from 'lib-common/generated/api-types/document'
import { FixedSpaceColumn } from 'lib-components/layout/flex-helpers'
import { H2, Label } from 'lib-components/typography'
import { Gap } from 'lib-components/white-space'

import { useTranslation } from '../../state/i18n'

import {
  documentForm,
  DocumentQuestionView,
  documentSectionForm,
  getDocumentFormInitialState
} from './documents'

interface Props {
  templateContent: DocumentTemplateContent
  readOnly?: boolean
}

export default React.memo(function DocumentView({
  templateContent,
  readOnly
}: Props) {
  const { i18n } = useTranslation()
  const bind = useForm(
    documentForm,
    () => getDocumentFormInitialState(templateContent),
    i18n.validationErrors
  )
  const sectionElems = useFormElems(bind)

  return (
    <div>
      <FixedSpaceColumn>
        {sectionElems.map((section) => (
          <DocumentSectionView
            key={section.state.id}
            bind={section}
            readOnly={readOnly ?? false}
          />
        ))}
      </FixedSpaceColumn>

      {/*TODO: Remove the debug stuff below*/}
      <Gap size="XL" />
      <Label>JSON that would be sent to backend</Label>
      <div style={{ fontFamily: 'monospace' }}>
        {JSON.stringify(bind.value())}
      </div>
    </div>
  )
})

interface DocumentSectionProps {
  bind: BoundForm<typeof documentSectionForm>
  readOnly: boolean
}
const DocumentSectionView = React.memo(function DocumentSectionView({
  bind,
  readOnly
}: DocumentSectionProps) {
  const { label, questions } = useFormFields(bind)
  const questionElems = useFormElems(questions)
  return (
    <div>
      <H2>{label.state}</H2>
      <FixedSpaceColumn>
        {questionElems.map((question) => (
          <DocumentQuestionView
            bind={question}
            key={question.state.state.template.id}
            readOnly={readOnly}
          />
        ))}
      </FixedSpaceColumn>
    </div>
  )
})
