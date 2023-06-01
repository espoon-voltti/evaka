// SPDX-FileCopyrightText: 2017-2023 City of Espoo
//
// SPDX-License-Identifier: LGPL-2.1-or-later

import { faFile, faQuestion, faTrash } from 'Icons'
import React, { useState } from 'react'
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
import InfoModal from 'lib-components/molecules/modals/InfoModal'

import { useTranslation } from '../../state/i18n'
import { renderResult } from '../async-rendering'
import { activeDocumentTemplateSummariesQuery } from '../document-templates/queries'

import {
  childDocumentsQuery,
  createChildDocumentMutation,
  deleteChildDocumentMutation
} from './queries'

const ChildDocuments = React.memo(function ChildDocuments({
  childId,
  documents
}: {
  childId: UUID
  documents: ChildDocumentSummary[]
}) {
  const { i18n } = useTranslation()
  const navigate = useNavigate()

  const { mutateAsync: deleteChildDocument } = useMutationResult(
    deleteChildDocumentMutation
  )

  const [confirmingDelete, setConfirmingDelete] =
    useState<ChildDocumentSummary | null>(null)

  return (
    <div>
      {confirmingDelete && (
        <InfoModal
          type="warning"
          title={i18n.childInformation.childDocuments.removeConfirmation}
          text={confirmingDelete.templateName}
          icon={faQuestion}
          reject={{
            action: () => setConfirmingDelete(null),
            label: i18n.common.cancel
          }}
          resolve={{
            action: async () => {
              const res = await deleteChildDocument({
                childId,
                documentId: confirmingDelete.id
              })
              if (res.isSuccess) {
                setConfirmingDelete(null)
              }
            },
            label: i18n.common.remove
          }}
        />
      )}
      <Table data-qa="table-of-child-documents">
        <Thead>
          <Tr>
            <Th>{i18n.childInformation.childDocuments.table.document}</Th>
            <Th>{i18n.childInformation.childDocuments.table.published}</Th>
            <Th>{i18n.childInformation.childDocuments.table.document}</Th>
            <Th>{i18n.childInformation.childDocuments.table.status}</Th>
            <Th />
          </Tr>
        </Thead>
        <Tbody>
          {documents.map((document) => (
            <Tr key={document.id} data-qa="child-document-row">
              <Td>
                <IconButton
                  icon={faFile}
                  aria-label={i18n.childInformation.childDocuments.table.open}
                  onClick={() => navigate(`/child-documents/${document.id}`)}
                  data-qa="open-document"
                />
              </Td>
              <Td>{document.publishedAt?.format() ?? '-'}</Td>
              <Td>{document.templateName}</Td>
              <Td data-qa="document-status">
                {document.publishedAt
                  ? i18n.childInformation.childDocuments.table.published
                  : i18n.childInformation.childDocuments.table.draft}
              </Td>
              <Td>
                {!document.publishedAt && (
                  <IconButton
                    icon={faTrash}
                    aria-label={i18n.common.remove}
                    onClick={() => setConfirmingDelete(document)}
                  />
                )}
              </Td>
            </Tr>
          ))}
        </Tbody>
      </Table>
    </div>
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
              (template) => template.type === type
            )
            return (
              <AddButtonRow
                key={type}
                text={`${
                  i18n.childInformation.childDocuments.addNew
                } ${i18n.documentTemplates.documentTypes[type].toLowerCase()}`}
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
                data-qa={`create-${type}`}
              />
            )
          })}

          <ChildDocuments childId={childId} documents={documents} />
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
  const { i18n } = useTranslation()

  return (
    <div>
      <Title size={4}>{i18n.childInformation.childDocuments.title}</Title>
      <ChildDocumentsList childId={childId} />
    </div>
  )
})
