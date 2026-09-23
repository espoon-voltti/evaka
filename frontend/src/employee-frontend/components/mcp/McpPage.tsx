// SPDX-FileCopyrightText: 2017-2026 City of Espoo
//
// SPDX-License-Identifier: LGPL-2.1-or-later

import React, { useState } from 'react'
import styled from 'styled-components'

import type {
  BatchSummary,
  McpAuthorizationSummary
} from 'lib-common/generated/api-types/mcp'
import type { McpTestDataBatchId } from 'lib-common/generated/api-types/shared'
import { useQueryResult } from 'lib-common/query'
import { Button } from 'lib-components/atoms/buttons/Button'
import Checkbox from 'lib-components/atoms/form/Checkbox'
import { Container, ContentArea } from 'lib-components/layout/Container'
import { Table, Tbody, Td, Th, Thead, Tr } from 'lib-components/layout/Table'
import {
  FixedSpaceColumn,
  FixedSpaceRow
} from 'lib-components/layout/flex-helpers'
import { ConfirmedMutation } from 'lib-components/molecules/ConfirmedMutation'
import { AlertBox } from 'lib-components/molecules/MessageBoxes'
import { MutateFormModal } from 'lib-components/molecules/modals/FormModal'
import { H1, H2, Label, P } from 'lib-components/typography'
import { Gap } from 'lib-components/white-space'
import { faTrash } from 'lib-icons'

import { useTranslation } from '../../state/i18n'
import { useTitle } from '../../utils/useTitle'
import { renderResult } from '../async-rendering'

import {
  deleteMcpTestDataBatchMutation,
  mcpAuthorizationsQuery,
  mcpConfigQuery,
  mcpTestDataBatchDeletionPreviewQuery,
  mcpTestDataBatchQuery,
  mcpTestDataBatchesQuery,
  revokeMcpAuthorizationMutation
} from './queries'
import type { McpTranslations } from './translations'
import { useMcpTranslation } from './translations'

const Mono = styled.code`
  word-break: break-all;
  user-select: all;
`

const EntityList = styled.ul`
  margin: 0;
  padding-left: 20px;
`

/**
 * Admin page: shows which AI tools are authorized to act on behalf of the user and lists the test
 * data batches created via the MCP server, with the option to delete a whole batch.
 */
export default React.memo(function McpPage() {
  const mcp = useMcpTranslation()
  useTitle(mcp.title)
  const config = useQueryResult(mcpConfigQuery())

  return (
    <Container>
      <ContentArea $opaque>
        <H1>{mcp.title}</H1>
        <P>{mcp.description}</P>
        <Gap $size="m" />
        {renderResult(config, (config) => (
          <FixedSpaceColumn $spacing="xs">
            <Label>{mcp.serverUrl}</Label>
            <Mono data-qa="mcp-server-url">{config.serverUrl}</Mono>
            <P $noMargin>{mcp.serverUrlInfo}</P>
          </FixedSpaceColumn>
        ))}
      </ContentArea>
      <Gap $size="m" />
      <ContentArea $opaque>
        <Authorizations />
      </ContentArea>
      <Gap $size="m" />
      <ContentArea $opaque>
        <TestDataBatches />
      </ContentArea>
    </Container>
  )
})

const Authorizations = React.memo(function Authorizations() {
  const mcp = useMcpTranslation()
  const t = mcp.authorizations
  const authorizations = useQueryResult(mcpAuthorizationsQuery())

  const status = (a: McpAuthorizationSummary) =>
    a.revokedAt
      ? t.statusRevoked
      : !a.tokenIssued
        ? t.statusPending
        : a.active
          ? t.statusActive
          : t.statusExpired

  return (
    <>
      <H2>{t.title}</H2>
      {renderResult(authorizations, (authorizations) =>
        authorizations.length === 0 ? (
          <P data-qa="mcp-no-authorizations">{t.empty}</P>
        ) : (
          <Table data-qa="mcp-authorizations">
            <Thead>
              <Tr>
                <Th>{t.client}</Th>
                <Th>{t.createdAt}</Th>
                <Th>{t.expiresAt}</Th>
                <Th>{t.lastUsedAt}</Th>
                <Th>{t.status}</Th>
                <Th />
              </Tr>
            </Thead>
            <Tbody>
              {authorizations.map((a) => (
                <Tr key={a.id} data-qa={`mcp-authorization-${a.id}`}>
                  <Td>
                    {a.clientName}
                    {a.clientUri && (
                      <>
                        <br />
                        <Mono>{a.clientUri}</Mono>
                      </>
                    )}
                  </Td>
                  <Td>{a.createdAt.format()}</Td>
                  <Td>{a.expiresAt.format()}</Td>
                  <Td>{a.lastUsedAt ? a.lastUsedAt.format() : t.never}</Td>
                  <Td data-qa="status">{status(a)}</Td>
                  <Td>
                    {!a.revokedAt && (
                      <ConfirmedMutation
                        buttonStyle="INLINE"
                        buttonText={t.revoke}
                        icon={faTrash}
                        confirmationTitle={t.revokeConfirmTitle}
                        confirmationText={t.revokeConfirmText}
                        mutation={revokeMcpAuthorizationMutation}
                        onClick={() => ({ id: a.id })}
                        data-qa="revoke-btn"
                      />
                    )}
                  </Td>
                </Tr>
              ))}
            </Tbody>
          </Table>
        )
      )}
    </>
  )
})

const entityTypeName = (t: McpTranslations['testData'], tableName: string) =>
  t.entityTypes[tableName] ?? tableName

const TestDataBatches = React.memo(function TestDataBatches() {
  const mcp = useMcpTranslation()
  const t = mcp.testData
  const batches = useQueryResult(mcpTestDataBatchesQuery())
  const [openBatch, setOpenBatch] = useState<McpTestDataBatchId | null>(null)

  return (
    <>
      <H2>{t.title}</H2>
      <P>{t.description}</P>
      {renderResult(batches, (batches) =>
        batches.length === 0 ? (
          <P data-qa="mcp-no-batches">{t.empty}</P>
        ) : (
          <Table data-qa="mcp-batches">
            <Thead>
              <Tr>
                <Th>{t.name}</Th>
                <Th>{t.createdBy}</Th>
                <Th>{t.createdAt}</Th>
                <Th>{t.contents}</Th>
                <Th />
              </Tr>
            </Thead>
            <Tbody>
              {batches.map((batch) => (
                <React.Fragment key={batch.id}>
                  <Tr data-qa={`mcp-batch-${batch.id}`}>
                    <Td>
                      <strong>{batch.name}</strong>
                      {batch.clientName && (
                        <>
                          <br />
                          {t.client}: {batch.clientName}
                        </>
                      )}
                    </Td>
                    <Td>{batch.createdByName}</Td>
                    <Td>{batch.createdAt.format()}</Td>
                    <Td>
                      <EntityCounts batch={batch} />
                      <Gap $size="xs" />
                      <Button
                        appearance="inline"
                        text={
                          openBatch === batch.id ? t.hideDetails : t.details
                        }
                        onClick={() =>
                          setOpenBatch(openBatch === batch.id ? null : batch.id)
                        }
                        data-qa="toggle-details"
                      />
                    </Td>
                    <Td>
                      <DeleteBatchButton id={batch.id} />
                    </Td>
                  </Tr>
                  {openBatch === batch.id && (
                    <Tr>
                      <Td colSpan={5}>
                        <BatchDetails id={batch.id} />
                      </Td>
                    </Tr>
                  )}
                </React.Fragment>
              ))}
            </Tbody>
          </Table>
        )
      )}
    </>
  )

  function EntityCounts({ batch }: { batch: BatchSummary }) {
    const entries = Object.entries(batch.entityCounts)
    return (
      <EntityList>
        {entries.map(([tableName, count]) => (
          <li key={tableName}>
            {entityTypeName(t, tableName)}: {count}
          </li>
        ))}
        <li>
          {t.total}: {batch.totalEntities}
        </li>
      </EntityList>
    )
  }
})

/**
 * Deleting a batch cascades into everything that depends on it, so the confirmation modal shows
 * what the deletion would actually remove before the admin confirms.
 */
const DeleteBatchButton = React.memo(function DeleteBatchButton({
  id
}: {
  id: McpTestDataBatchId
}) {
  const { i18n } = useTranslation()
  const t = useMcpTranslation().testData
  const [confirming, setConfirming] = useState(false)
  const [confirmed, setConfirmed] = useState(false)
  const close = () => {
    setConfirming(false)
    setConfirmed(false)
  }
  return (
    <>
      <Button
        appearance="inline"
        text={t.delete}
        icon={faTrash}
        onClick={() => setConfirming(true)}
        data-qa="delete-batch-btn"
      />
      {confirming && (
        <MutateFormModal
          title={t.deleteConfirmTitle}
          text={t.deleteConfirmText}
          type="warning"
          resolveMutation={deleteMcpTestDataBatchMutation}
          resolveAction={() => ({ id })}
          resolveDisabled={!confirmed}
          resolveLabel={i18n.common.confirm}
          onSuccess={close}
          rejectAction={close}
          rejectLabel={i18n.common.cancel}
          data-qa="delete-batch-modal"
        >
          <DeletionPreview id={id} />
          <Gap $size="s" />
          <FixedSpaceRow $justifyContent="center">
            <Checkbox
              label={t.deleteConfirmCheckbox}
              checked={confirmed}
              onChange={setConfirmed}
              data-qa="modal-extra-confirmation"
            />
          </FixedSpaceRow>
        </MutateFormModal>
      )}
    </>
  )
})

const DeletionPreview = React.memo(function DeletionPreview({
  id
}: {
  id: McpTestDataBatchId
}) {
  const t = useMcpTranslation().testData
  const preview = useQueryResult(mcpTestDataBatchDeletionPreviewQuery({ id }))
  if (preview.isFailure) {
    return (
      <AlertBox
        message={
          preview.statusCode === 409
            ? t.deletePreviewConflict
            : t.deletePreviewFailed
        }
        wide
        noMargin
        data-qa="deletion-preview-error"
      />
    )
  }
  return renderResult(preview, (preview) => {
    const totalRows = Object.values(preview.deletedRowCounts).reduce<number>(
      (sum, count) => sum + (count ?? 0),
      0
    )
    const untracked = Object.entries(preview.untrackedRowCounts)
    const untrackedRows = untracked.reduce(
      (sum, [, count]) => sum + (count ?? 0),
      0
    )
    return (
      <FixedSpaceColumn $spacing="xs" data-qa="deletion-preview">
        <P $noMargin>
          {t.deletePreviewTotal(totalRows, preview.trackedEntities)}
        </P>
        {untrackedRows > 0 && (
          <AlertBox
            wide
            noMargin
            message={
              <>
                {t.deletePreviewUntracked(untrackedRows)}
                <EntityList data-qa="deletion-preview-untracked">
                  {untracked.map(([tableName, count]) => (
                    <li key={tableName}>
                      {entityTypeName(t, tableName)}: {count}
                    </li>
                  ))}
                </EntityList>
              </>
            }
          />
        )}
      </FixedSpaceColumn>
    )
  })
})

const BatchDetails = React.memo(function BatchDetails({
  id
}: {
  id: McpTestDataBatchId
}) {
  const details = useQueryResult(mcpTestDataBatchQuery({ id }))
  return renderResult(details, ({ entities }) => (
    <EntityList data-qa="batch-entities">
      {entities.map((entity) => (
        <li key={`${entity.tableName}-${entity.entityId}`}>
          {entity.description} <Mono>{entity.entityId}</Mono>
        </li>
      ))}
    </EntityList>
  ))
})
