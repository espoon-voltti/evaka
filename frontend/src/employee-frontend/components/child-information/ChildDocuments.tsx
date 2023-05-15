// SPDX-FileCopyrightText: 2017-2023 City of Espoo
//
// SPDX-License-Identifier: LGPL-2.1-or-later

import { faFile } from 'Icons'
import React from 'react'
import { useNavigate } from 'react-router-dom'

import { combine } from 'lib-common/api'
import {
  ChildDocumentSummary,
  DocumentType
} from 'lib-common/generated/api-types/document'
import { useMutationResult, useQueryResult } from 'lib-common/query'
import { UUID } from 'lib-common/types'
import Title from 'lib-components/atoms/Title'
import { AddButtonRow } from 'lib-components/atoms/buttons/AddButton'
import IconButton from 'lib-components/atoms/buttons/IconButton'
import { Table, Thead, Th, Tbody, Tr, Td } from 'lib-components/layout/Table'
import { FixedSpaceColumn } from 'lib-components/layout/flex-helpers'

import { useTranslation } from '../../state/i18n'
import { renderResult } from '../async-rendering'
import { activeDocumentTemplateSummariesQuery } from '../document-templates/queries'

import { childDocumentsQuery, createChildDocumentMutation } from './queries'

const ChildDocuments = React.memo(function ChildDocuments({
  documents
}: {
  documents: ChildDocumentSummary[]
}) {
  const { i18n } = useTranslation()
  const navigate = useNavigate()

  return (
    <Table>
      <Thead>
        <Tr>
          <Th>Lomake</Th>
          <Th>Tyyppi</Th>
          <Th>Tila</Th>
          <Th />
        </Tr>
      </Thead>
      <Tbody>
        {documents.map((document) => (
          <Tr key={document.id}>
            <Td>
              <IconButton
                icon={faFile}
                aria-label="Avaa lomake"
                onClick={() => navigate(`/child-documents/${document.id}`)}
              />
            </Td>
            <Td>{i18n.documentTemplates.documentTypes[document.type]}</Td>
            <Td>{document.published ? 'Julkaistu' : 'Luonnos'}</Td>
          </Tr>
        ))}
      </Tbody>
    </Table>
  )
})

const ChildDocumentsList = React.memo(function ChildDocumentsList({
  childId
}: {
  childId: UUID
}) {
  const { i18n } = useTranslation()
  const navigate = useNavigate()
  const documentsResult = useQueryResult(childDocumentsQuery(childId))
  const documentTemplatesResult = useQueryResult(
    activeDocumentTemplateSummariesQuery(childId)
  )
  const { mutateAsync: createChildDocument, isLoading: submitting } =
    useMutationResult(createChildDocumentMutation)

  return renderResult(
    combine(documentsResult, documentTemplatesResult),
    ([documents, templates]) => {
      const types: DocumentType[] = [
        'PEDAGOGICAL_REPORT' as const,
        'PEDAGOGICAL_ASSESSMENT' as const
      ]

      return (
        <FixedSpaceColumn>
          {types.map((type) => {
            const templatesOfType = templates.filter(
              (template) => template.type === type // TODO: filtering by unit language?
            )
            return (
              <AddButtonRow
                key={type}
                text={
                  'Luo uusi ' +
                  i18n.documentTemplates.documentTypes[type].toLowerCase()
                }
                onClick={() =>
                  createChildDocument({
                    childId,
                    templateId: templatesOfType[0].id // TODO: selecting if multiple
                  }).then((res) => {
                    if (res.isSuccess) {
                      navigate(`/child-documents/${res.value}`)
                    }
                  })
                }
                disabled={templatesOfType.length < 1 || submitting}
              />
            )
          })}

          <ChildDocuments documents={documents} />
        </FixedSpaceColumn>
      )
    }
  )
})

export default React.memo(function ChildDocumentsWrapper({
  childId
}: {
  childId: UUID
}) {
  return (
    <div>
      <Title size={4}>Lapsen pedagogiset lomakkeet</Title>
      <ChildDocumentsList childId={childId} />
    </div>
  )
})
