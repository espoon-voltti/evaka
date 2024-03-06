// SPDX-FileCopyrightText: 2017-2023 City of Espoo
//
// SPDX-License-Identifier: LGPL-2.1-or-later

import React from 'react'

import { useQueryResult } from 'lib-common/query'
import useRequiredParams from 'lib-common/useRequiredParams'
import Container from 'lib-components/layout/Container'

import { renderResult } from '../../async-rendering'
import { documentTemplateQuery } from '../queries'

import TemplateContentEditor from './TemplateContentEditor'

export default React.memo(function TemplateEditorPage() {
  const { templateId } = useRequiredParams('templateId')

  const templateResult = useQueryResult(documentTemplateQuery({ templateId }))

  return (
    <Container>
      {renderResult(templateResult, (template) => (
        <TemplateContentEditor
          template={template}
          readOnly={template.published}
        />
      ))}
    </Container>
  )
})
