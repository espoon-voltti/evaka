// SPDX-FileCopyrightText: 2017-2023 City of Espoo
//
// SPDX-License-Identifier: LGPL-2.1-or-later

import { faCopy, faTrash } from 'Icons'
import React, { useState } from 'react'
import { Link } from 'react-router-dom'

import { useMutationResult, useQueryResult } from 'lib-common/query'
import { AddButtonRow } from 'lib-components/atoms/buttons/AddButton'
import IconButton from 'lib-components/atoms/buttons/IconButton'
import Container, { ContentArea } from 'lib-components/layout/Container'
import { Table, Tbody, Td, Th, Thead, Tr } from 'lib-components/layout/Table'
import { H1 } from 'lib-components/typography'

import { useTranslation } from '../../../state/i18n'
import { renderResult } from '../../async-rendering'
import {
  deleteDocumentTemplateMutation,
  documentTemplateSummariesQuery
} from '../queries'

import TemplateModal from './TemplateModal'
import { FixedSpaceRow } from 'lib-components/layout/flex-helpers'

export default React.memo(function DocumentTemplatesPage() {
  const { i18n } = useTranslation()
  const t = i18n.documentTemplates

  const [templateModalOpen, setTemplateModalOpen] = useState(false)
  const [duplicatingTemplateId, setDuplicatingTemplateId] = useState<
    string | null
  >(null)

  const templates = useQueryResult(documentTemplateSummariesQuery)
  const { mutateAsync: deleteDocumentTemplate } = useMutationResult(
    deleteDocumentTemplateMutation
  )
  return (
    <Container>
      <ContentArea opaque>
        <H1>{t.title}</H1>
        <AddButtonRow
          onClick={() => {
            setDuplicatingTemplateId(null)
            setTemplateModalOpen(true)
          }}
          text={i18n.documentTemplates.templatesPage.add}
        />
        {templateModalOpen && (
          <TemplateModal
            duplicateFrom={duplicatingTemplateId}
            onClose={() => {
              setTemplateModalOpen(false)
              setDuplicatingTemplateId(null)
            }}
          />
        )}
        {renderResult(templates, (data) => (
          <>
            <Table>
              <Thead>
                <Tr>
                  <Th>{i18n.documentTemplates.templatesPage.name}</Th>
                  <Th>{i18n.documentTemplates.templatesPage.type}</Th>
                  <Th>{i18n.documentTemplates.templatesPage.language}</Th>
                  <Th>{i18n.documentTemplates.templatesPage.validity}</Th>
                  <Th>{i18n.documentTemplates.templatesPage.status}</Th>
                  <Th />
                </Tr>
              </Thead>
              <Tbody>
                {data.map((template) => (
                  <Tr key={template.id}>
                    <Td>
                      <Link to={`/document-templates/${template.id}`}>
                        {template.name}
                      </Link>
                    </Td>
                    <Td>
                      {i18n.documentTemplates.documentTypes[template.type]}
                    </Td>
                    <Td>
                      {i18n.documentTemplates.languages[template.language]}
                    </Td>
                    <Td>{template.validity.format()}</Td>
                    <Td>
                      {template.published
                        ? i18n.documentTemplates.templatesPage.published
                        : i18n.documentTemplates.templatesPage.draft}
                    </Td>
                    <Td>
                      <FixedSpaceRow>
                        <IconButton
                          icon={faCopy}
                          aria-label={i18n.common.copy}
                          onClick={() => {
                            setDuplicatingTemplateId(template.id)
                            setTemplateModalOpen(true)
                          }}
                        />
                        <IconButton
                          icon={faTrash}
                          aria-label={i18n.common.remove}
                          disabled={template.published}
                          onClick={() => deleteDocumentTemplate(template.id)}
                        />
                      </FixedSpaceRow>
                    </Td>
                  </Tr>
                ))}
              </Tbody>
            </Table>
          </>
        ))}
      </ContentArea>
    </Container>
  )
})
