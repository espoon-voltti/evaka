// SPDX-FileCopyrightText: 2017-2023 City of Espoo
//
// SPDX-License-Identifier: LGPL-2.1-or-later

import React, { useState } from 'react'

import { useQueryResult } from 'lib-common/query'
import { AddButtonRow } from 'lib-components/atoms/buttons/AddButton'
import Container, { ContentArea } from 'lib-components/layout/Container'
import { Table, Tbody, Td, Th, Thead, Tr } from 'lib-components/layout/Table'
import { H1 } from 'lib-components/typography'

import { useTranslation } from '../../state/i18n'
import { renderResult } from '../async-rendering'

import CreateTemplateModal from './CreateTemplateModal'
import { documentTemplateSummariesQuery } from './queries'

export default React.memo(function DocumentTemplatesPage() {
  const { i18n } = useTranslation()
  const t = i18n.documentTemplates

  const [creationModalOpen, setCreationModalOpen] = useState(false)

  const templates = useQueryResult(documentTemplateSummariesQuery)

  return (
    <Container>
      <ContentArea opaque>
        <H1>{t.title}</H1>
        <AddButtonRow
          onClick={() => setCreationModalOpen(true)}
          text="Lisää uusi"
        />
        {creationModalOpen && (
          <CreateTemplateModal onClose={() => setCreationModalOpen(false)} />
        )}
        {renderResult(templates, (data) => (
          <>
            <Table>
              <Thead>
                <Tr>
                  <Th>Tyyppi</Th>
                  <Th>Nimi</Th>
                  <Th>Voimassa</Th>
                  <Th>Tila</Th>
                </Tr>
              </Thead>
              <Tbody>
                {data.map((template) => (
                  <Tr key={template.id}>
                    <Td>Ei määritelty</Td>
                    <Td>{template.name}</Td>
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
