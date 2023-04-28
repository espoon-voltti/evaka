import React from 'react'

import { useQueryResult } from 'lib-common/query'
import useNonNullableParams from 'lib-common/useNonNullableParams'
import Container from 'lib-components/layout/Container'
import { H1 } from 'lib-components/typography'

import { renderResult } from '../../async-rendering'
import { documentTemplateQuery } from '../queries'

import TemplateContentEditor from './TemplateContentEditor'

export default React.memo(function TemplateEditorPage() {
  const { templateId } = useNonNullableParams()

  const templateResult = useQueryResult(documentTemplateQuery(templateId))

  return (
    <Container>
      {renderResult(templateResult, (template) => (
        <>
          <H1>{template.name}</H1>
          <TemplateContentEditor
            templateId={template.id}
            templateContent={template.content}
            readOnly={template.published}
          />
        </>
      ))}
    </Container>
  )
})
