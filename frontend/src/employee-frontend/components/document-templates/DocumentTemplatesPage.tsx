// SPDX-FileCopyrightText: 2017-2023 City of Espoo
//
// SPDX-License-Identifier: LGPL-2.1-or-later

import React, { useState } from 'react'
import { Link } from 'react-router-dom'

import { useQueryResult } from 'lib-common/query'
import { AddButtonRow } from 'lib-components/atoms/buttons/AddButton'
import Container, { ContentArea } from 'lib-components/layout/Container'
import { Table, Tbody, Td, Th, Thead, Tr } from 'lib-components/layout/Table'
import { H1 } from 'lib-components/typography'

import { useTranslation } from '../../state/i18n'
import { renderResult } from '../async-rendering'

import TemplateModal from './TemplateModal'
import { documentTemplateSummariesQuery } from './queries'

export default React.memo(function DocumentTemplatesPage() {
  const { i18n } = useTranslation()
  const t = i18n.documentTemplates

  const [templateModalOpen, setTemplateModalOpen] = useState(false)

  const templates = useQueryResult(documentTemplateSummariesQuery)

  return (
    <Container>
      <ContentArea opaque>
        <H1>{t.title}</H1>
        <AddButtonRow
          onClick={() => setTemplateModalOpen(true)}
          text="Lisää uusi"
        />
        {templateModalOpen && (
          <TemplateModal onClose={() => setTemplateModalOpen(false)} />
        )}
        {renderResult(templates, (data) => (
          <>
            <Table>
              <Thead>
                <Tr>
                  <Th>Nimi</Th>
                  <Th>Tyyppi</Th>
                  <Th>Voimassa</Th>
                  <Th>Tila</Th>
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
                    <Td>Ei määritelty</Td>
                    <Td>{template.validity.format()}</Td>
                    <Td>{template.published ? 'Julkaistu' : 'Luonnos'}</Td>
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
