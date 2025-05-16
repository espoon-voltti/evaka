// SPDX-FileCopyrightText: 2017-2025 City of Espoo
//
// SPDX-License-Identifier: LGPL-2.1-or-later

import orderBy from 'lodash/orderBy'
import React, { useMemo, useState } from 'react'
import { useNavigate } from 'react-router'
import styled from 'styled-components'

import { combine } from 'lib-common/api'
import { oneOf, required } from 'lib-common/form/form'
import { useForm } from 'lib-common/form/hooks'
import type {
  ChildDocumentSummaryWithPermittedActions,
  DocumentTemplateSummary
} from 'lib-common/generated/api-types/document'
import type {
  ChildId,
  DocumentTemplateId
} from 'lib-common/generated/api-types/shared'
import {
  constantQuery,
  useMutationResult,
  useQueryResult
} from 'lib-common/query'
import type { UUID } from 'lib-common/types'
import { AddButtonRow } from 'lib-components/atoms/buttons/AddButton'
import { Button } from 'lib-components/atoms/buttons/Button'
import { SelectF } from 'lib-components/atoms/dropdowns/Select'
import { ChildDocumentStateChip } from 'lib-components/document-templates/ChildDocumentStateChip'
import type { ChildDocumentCategory } from 'lib-components/document-templates/documents'
import { getDocumentCategory } from 'lib-components/document-templates/documents'
import { Table, Tbody, Td, Th, Thead, Tr } from 'lib-components/layout/Table'
import {
  FixedSpaceColumn,
  FixedSpaceRow
} from 'lib-components/layout/flex-helpers'
import { AsyncFormModal } from 'lib-components/molecules/modals/FormModal'
import { H3, Label } from 'lib-components/typography'
import { featureFlags } from 'lib-customizations/employee'
import { faFile } from 'lib-icons'

import { useTranslation } from '../../state/i18n'
import { renderResult } from '../async-rendering'
import { activeDocumentTemplateSummariesQuery } from '../document-templates/queries'

import { childDocumentsQuery, createChildDocumentMutation } from './queries'

const WiderTd = styled(Td)`
  width: 50%;
`

const StatusTd = styled(Td)`
  text-align: right;
  width: 150px;
`

const InternalChildDocuments = React.memo(function InternalChildDocuments({
  childId,
  documents
}: {
  childId: UUID
  documents: ChildDocumentSummaryWithPermittedActions[]
}) {
  const { i18n } = useTranslation()
  const navigate = useNavigate()

  return (
    <div>
      <Table data-qa="table-of-internal-child-documents">
        <Thead>
          <Tr>
            <Th>{i18n.childInformation.childDocuments.table.document}</Th>
            <Th>{i18n.childInformation.childDocuments.table.modified}</Th>
            <Th>{i18n.childInformation.childDocuments.table.published}</Th>
            <Th>{i18n.childInformation.childDocuments.table.status}</Th>
          </Tr>
        </Thead>
        <Tbody>
          {orderBy(
            documents,
            (document) => document.data.modifiedAt,
            'desc'
          ).map(({ data: document, permittedActions }) => (
            <Tr key={document.id} data-qa="child-document-row">
              <WiderTd data-qa={`child-document-${document.id}`}>
                <Button
                  appearance="inline"
                  aria-label={i18n.childInformation.childDocuments.table.open}
                  text={document.templateName}
                  icon={faFile}
                  disabled={!permittedActions.includes('READ')}
                  onClick={() =>
                    navigate({
                      pathname: `/child-documents/${document.id}`,
                      search: `?childId=${childId}`
                    })
                  }
                  data-qa="open-document"
                />
              </WiderTd>
              <Td>{document.modifiedAt.format()}</Td>
              <Td data-qa="document-published-at">
                {document.publishedAt?.format() ?? '-'}
              </Td>
              <StatusTd data-qa="document-status">
                <ChildDocumentStateChip
                  status={document.decision?.status ?? document.status}
                />
              </StatusTd>
            </Tr>
          ))}
        </Tbody>
      </Table>
    </div>
  )
})

const DecisionChildDocuments = React.memo(function DecisionChildDocuments({
  childId,
  documents
}: {
  childId: UUID
  documents: ChildDocumentSummaryWithPermittedActions[]
}) {
  const { i18n } = useTranslation()
  const navigate = useNavigate()

  return (
    <div>
      <Table data-qa="table-of-decision-child-documents">
        <Thead>
          <Tr>
            <Th>{i18n.childInformation.childDocuments.table.document}</Th>
            <Th>{i18n.childInformation.childDocuments.table.modified}</Th>
            <Th>{i18n.childInformation.childDocuments.table.valid}</Th>
            <Th>{i18n.childInformation.childDocuments.table.status}</Th>
          </Tr>
        </Thead>
        <Tbody>
          {orderBy(
            documents,
            (document) => document.data.modifiedAt,
            'desc'
          ).map(({ data: document, permittedActions }) => (
            <Tr key={document.id} data-qa="child-document-row">
              <WiderTd data-qa={`child-document-${document.id}`}>
                <Button
                  appearance="inline"
                  aria-label={i18n.childInformation.childDocuments.table.open}
                  text={document.templateName}
                  icon={faFile}
                  disabled={!permittedActions.includes('READ')}
                  onClick={() =>
                    navigate({
                      pathname: `/child-documents/${document.id}`,
                      search: `?childId=${childId}`
                    })
                  }
                  data-qa="open-document"
                />
              </WiderTd>
              <Td>{document.modifiedAt.format()}</Td>
              <Td>
                {document.decision?.validity
                  ? document.decision.validity.format()
                  : ''}
              </Td>
              <StatusTd data-qa="document-status">
                <ChildDocumentStateChip
                  status={document.decision?.status ?? document.status}
                />
              </StatusTd>
            </Tr>
          ))}
        </Tbody>
      </Table>
    </div>
  )
})

const ExternalChildDocuments = React.memo(function ExternalChildDocuments({
  childId,
  documents
}: {
  childId: UUID
  documents: ChildDocumentSummaryWithPermittedActions[]
}) {
  const { i18n } = useTranslation()
  const navigate = useNavigate()

  return (
    <div>
      <Table data-qa="table-of-external-child-documents">
        <Thead>
          <Tr>
            <Th>{i18n.childInformation.childDocuments.table.document}</Th>
            <Th>{i18n.childInformation.childDocuments.table.sent}</Th>
            <Th>{i18n.childInformation.childDocuments.table.answered}</Th>
            <Th>{i18n.childInformation.childDocuments.table.status}</Th>
          </Tr>
        </Thead>
        <Tbody>
          {orderBy(
            documents,
            (document) => document.data.modifiedAt,
            'desc'
          ).map(({ data: document, permittedActions }) => (
            <Tr key={document.id} data-qa="child-document-row">
              <WiderTd data-qa={`child-document-${document.id}`}>
                <Button
                  appearance="inline"
                  aria-label={i18n.childInformation.childDocuments.table.open}
                  text={document.templateName}
                  icon={faFile}
                  disabled={!permittedActions.includes('READ')}
                  onClick={() =>
                    navigate({
                      pathname: `/child-documents/${document.id}`,
                      search: `?childId=${childId}`
                    })
                  }
                  data-qa="open-document"
                />
              </WiderTd>
              <Td data-qa="document-sent-at">
                {document.publishedAt !== null ? (
                  <span>{document.publishedAt.toLocalDate().format()}</span>
                ) : (
                  <span>
                    {i18n.childInformation.childDocuments.table.notSent}
                  </span>
                )}
              </Td>
              <Td data-qa="document-answered-at">
                {document.answeredAt !== null ? (
                  <>
                    <span>{document.answeredAt?.toLocalDate().format()}</span>
                    {document.answeredBy !== null && (
                      <span>, {document.answeredBy.name}</span>
                    )}
                  </>
                ) : (
                  <span>
                    {i18n.childInformation.childDocuments.table.unanswered}
                  </span>
                )}
              </Td>
              <StatusTd data-qa="document-status">
                <ChildDocumentStateChip
                  status={document.decision?.status ?? document.status}
                />
              </StatusTd>
            </Tr>
          ))}
        </Tbody>
      </Table>
    </div>
  )
})

const ChildDocumentTables = ({
  childId,
  hasCreatePermission,
  documents,
  templates
}: {
  childId: ChildId
  hasCreatePermission: boolean
  documents: ChildDocumentSummaryWithPermittedActions[]
  templates: DocumentTemplateSummary[]
}) => {
  const { i18n } = useTranslation()
  const [creationModalState, setCreationModalState] = useState<
    ChildDocumentCategory | undefined
  >(undefined)

  const creatableTemplates = useMemo(
    () =>
      templates.filter(
        (template) =>
          !documents.some(
            ({ data: doc }) =>
              doc.templateId === template.id && doc.status !== 'COMPLETED'
          ) && !template.type.startsWith('MIGRATED_')
      ),
    [documents, templates]
  )

  const validTemplatesByCategory: Record<
    ChildDocumentCategory,
    DocumentTemplateSummary[]
  > = useMemo(
    () => ({
      internal: creatableTemplates.filter(
        (t) => getDocumentCategory(t.type) === 'internal'
      ),
      decision: creatableTemplates.filter(
        (t) => getDocumentCategory(t.type) === 'decision'
      ),
      external: creatableTemplates.filter(
        (t) => getDocumentCategory(t.type) === 'external'
      )
    }),
    [creatableTemplates]
  )

  const documentsByCategory: Record<
    ChildDocumentCategory,
    ChildDocumentSummaryWithPermittedActions[]
  > = useMemo(
    () => ({
      internal: documents.filter(
        (d) => getDocumentCategory(d.data.type) === 'internal'
      ),
      decision: documents.filter(
        (d) => getDocumentCategory(d.data.type) === 'decision'
      ),
      external: documents.filter(
        (d) => getDocumentCategory(d.data.type) === 'external'
      )
    }),
    [documents]
  )

  const enabledCategories: ChildDocumentCategory[] = [
    'internal',
    ...(featureFlags.decisionChildDocumentTypes ? ['decision' as const] : []),
    ...(featureFlags.citizenChildDocumentTypes ? ['external' as const] : [])
  ]

  return (
    <FixedSpaceColumn spacing="L">
      {creationModalState && (
        <CreationModal
          childId={childId}
          title={
            i18n.childInformation.childDocuments.addNew[creationModalState]
          }
          templates={validTemplatesByCategory[creationModalState]}
          onClose={() => setCreationModalState(undefined)}
        />
      )}

      {enabledCategories.map((category) => (
        <FixedSpaceColumn spacing="zero" key={category}>
          <FixedSpaceRow justifyContent="space-between">
            <H3>{i18n.childInformation.childDocuments.title[category]}</H3>
            {hasCreatePermission && (
              <AddButtonRow
                text={i18n.childInformation.childDocuments.addNew[category]}
                onClick={() => setCreationModalState(category)}
                disabled={validTemplatesByCategory[category].length === 0}
                data-qa={`create-${category}-document`}
              />
            )}
          </FixedSpaceRow>
          {category === 'internal' && (
            <InternalChildDocuments
              childId={childId}
              documents={documentsByCategory[category]}
            />
          )}
          {category === 'decision' && (
            <DecisionChildDocuments
              childId={childId}
              documents={documentsByCategory[category]}
            />
          )}
          {category === 'external' && (
            <ExternalChildDocuments
              childId={childId}
              documents={documentsByCategory[category]}
            />
          )}
        </FixedSpaceColumn>
      ))}
    </FixedSpaceColumn>
  )
}

const CreationModal = React.memo(function CreationModal({
  childId,
  title,
  templates,
  onClose
}: {
  childId: ChildId
  title: string
  templates: DocumentTemplateSummary[]
  onClose: () => void
}) {
  const { i18n } = useTranslation()
  const { mutateAsync: createChildDocument } = useMutationResult(
    createChildDocumentMutation
  )
  const navigate = useNavigate()

  const form = required(oneOf<DocumentTemplateId>())
  const bind = useForm(
    form,
    () => ({
      domValue: templates[0]?.id,
      options: templates.map((t) => ({
        domValue: t.id,
        value: t.id,
        label: t.name
      }))
    }),
    i18n.validationErrors
  )

  const submit = async () => {
    const res = await createChildDocument({
      body: {
        childId,
        templateId: bind.value()
      }
    })
    if (res.isSuccess) {
      void navigate(`/child-documents/${res.value}`)
    }
    return res
  }

  return (
    <AsyncFormModal
      title={title}
      resolveAction={submit}
      onSuccess={onClose}
      resolveLabel={i18n.common.confirm}
      rejectAction={onClose}
      rejectLabel={i18n.common.cancel}
      resolveDisabled={!bind.isValid()}
    >
      <FixedSpaceColumn>
        <Label>{i18n.childInformation.childDocuments.select}</Label>
        <SelectF bind={bind} data-qa="template-select" />
      </FixedSpaceColumn>
    </AsyncFormModal>
  )
})

export default React.memo(function ChildDocumentsList({
  childId,
  hasCreatePermission
}: {
  childId: ChildId
  hasCreatePermission: boolean
}) {
  const documentsResult = useQueryResult(childDocumentsQuery({ childId }))
  const documentTemplatesResult = useQueryResult(
    hasCreatePermission
      ? activeDocumentTemplateSummariesQuery({ childId })
      : constantQuery([])
  )

  return renderResult(
    combine(documentsResult, documentTemplatesResult),
    ([documents, templates]) => (
      <ChildDocumentTables
        childId={childId}
        hasCreatePermission={hasCreatePermission}
        documents={documents}
        templates={templates}
      />
    )
  )
})
