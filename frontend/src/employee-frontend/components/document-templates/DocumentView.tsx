// SPDX-FileCopyrightText: 2017-2023 City of Espoo
//
// SPDX-License-Identifier: LGPL-2.1-or-later

import React from 'react'

import { BoundForm, useFormElems, useFormFields } from 'lib-common/form/hooks'
import { FixedSpaceColumn } from 'lib-components/layout/flex-helpers'
import { H2 } from 'lib-components/typography'

import {
  documentForm,
  DocumentQuestionView,
  documentSectionForm
} from './documents'

interface Props {
  bind: BoundForm<typeof documentForm>
  readOnly?: boolean
}

export default React.memo(function DocumentView({ bind, readOnly }: Props) {
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
    </div>
  )
})

interface DocumentSectionProps {
  bind: BoundForm<typeof documentSectionForm>
  readOnly: boolean
}
export const DocumentSectionView = React.memo(function DocumentSectionView({
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
