// SPDX-FileCopyrightText: 2017-2023 City of Espoo
//
// SPDX-License-Identifier: LGPL-2.1-or-later

import React, { useState } from 'react'

import { useQueryResult } from 'lib-common/query'
import useNonNullableParams from 'lib-common/useNonNullableParams'
import Checkbox from 'lib-components/atoms/form/Checkbox'
import Container, { ContentArea } from 'lib-components/layout/Container'
import { H1 } from 'lib-components/typography'

import { renderResult } from '../../async-rendering'
import DocumentView from '../DocumentView'
import { documentTemplateQuery } from '../queries'

// TODO: remove this and route

export default React.memo(function TemplatePreviewerPage() {
  const { templateId } = useNonNullableParams()

  const templateResult = useQueryResult(documentTemplateQuery(templateId))
  const [readOnly, setReadOnly] = useState(false)

  return (
    <Container>
      <ContentArea opaque>
        <Checkbox
          label="Display as read only"
          checked={readOnly}
          onChange={setReadOnly}
        />

        {renderResult(templateResult, (template) => (
          <>
            <H1>{template.name}</H1>
            <DocumentView
              templateContent={template.content}
              readOnly={readOnly}
            />
          </>
        ))}
      </ContentArea>
    </Container>
  )
})
