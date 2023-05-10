import { faFile } from 'Icons'
import React from 'react'

import { combine } from 'lib-common/api'
import {
  ChildDocument,
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
  documents: ChildDocument[]
}) {
  const { i18n } = useTranslation()

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
                onClick={() => undefined}
              />
            </Td>
            <Td>
              {i18n.documentTemplates.documentTypes[document.template.type]}
            </Td>
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
  const documentsResult = useQueryResult(childDocumentsQuery(childId))
  const documentTemplatesResult = useQueryResult(
    activeDocumentTemplateSummariesQuery
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
