import React from 'react'

import { BoundForm, useFormElems, useFormFields } from 'lib-common/form/hooks'
import { DocumentTemplateContent } from 'lib-common/generated/api-types/document'
import { FixedSpaceColumn } from 'lib-components/layout/flex-helpers'
import { H2, Label } from 'lib-components/typography'
import { Gap } from 'lib-components/white-space'

import {
  documentSectionForm,
  serializeDocumentAnswers,
  useTemplateContentAsForm
} from './forms'
import {
  DocumentQuestionReadOnlyView,
  DocumentQuestionView
} from './question-descriptors/questions'

interface Props {
  templateContent: DocumentTemplateContent
  readOnly?: boolean
}

export default React.memo(function DocumentView({
  templateContent,
  readOnly
}: Props) {
  const bind = useTemplateContentAsForm(templateContent)
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
        {JSON.stringify(serializeDocumentAnswers(bind))}
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
        {questionElems.map((question) =>
          readOnly ? (
            <DocumentQuestionReadOnlyView
              bind={question}
              key={question.state.state.id}
            />
          ) : (
            <DocumentQuestionView
              bind={question}
              key={question.state.state.id}
            />
          )
        )}
      </FixedSpaceColumn>
    </div>
  )
})
