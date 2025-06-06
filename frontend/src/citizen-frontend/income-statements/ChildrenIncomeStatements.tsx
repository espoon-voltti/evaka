// SPDX-FileCopyrightText: 2017-2024 City of Espoo
//
// SPDX-License-Identifier: LGPL-2.1-or-later

import noop from 'lodash/noop'
import React, { Fragment, useCallback, useContext, useState } from 'react'
import styled from 'styled-components'
import { Link, useLocation } from 'wouter'

import type {
  ChildBasicInfo,
  IncomeStatement
} from 'lib-common/generated/api-types/incomestatement'
import type {
  ChildId,
  IncomeStatementId,
  PersonId
} from 'lib-common/generated/api-types/shared'
import { useMutation, useQueryResult } from 'lib-common/query'
import Pagination from 'lib-components/Pagination'
import HorizontalLine from 'lib-components/atoms/HorizontalLine'
import { Button } from 'lib-components/atoms/buttons/Button'
import ResponsiveAddButton from 'lib-components/atoms/buttons/ResponsiveAddButton'
import Container, { ContentArea } from 'lib-components/layout/Container'
import { Table, Tbody, Td, Th, Thead, Tr } from 'lib-components/layout/Table'
import {
  FixedSpaceColumn,
  FixedSpaceRow
} from 'lib-components/layout/flex-helpers'
import {
  MobileOnly,
  TabletAndDesktop
} from 'lib-components/layout/responsive-layout'
import { InfoBox } from 'lib-components/molecules/MessageBoxes'
import { PersonName } from 'lib-components/molecules/PersonNames'
import InfoModal from 'lib-components/molecules/modals/InfoModal'
import { Dimmed, H1, H3 } from 'lib-components/typography'
import { Gap } from 'lib-components/white-space'
import colors from 'lib-customizations/common'
import { faFile } from 'lib-icons'
import { faPen, faQuestion, faTrash } from 'lib-icons'

import { renderResult } from '../async-rendering'
import { useTranslation } from '../localization'
import { OverlayContext } from '../overlay/state'

import {
  childIncomeStatementsQuery,
  deleteChildIncomeStatementMutation
} from './queries'

const HeadingContainer = styled.div`
  display: flex;
  justify-content: space-between;
`
const ChildIncomeStatementsContainer = styled.div`
  border-style: solid;
  border-color: ${colors.grayscale.g35};
  border-width: 1px 0 0 0;
`

function getLink(childId: ChildId, { id, status }: IncomeStatement) {
  const suffix = status === 'DRAFT' ? 'edit' : ''
  return `/child-income/${childId}/${id}/${suffix}`
}

interface TableOrListProps {
  childId: PersonId
  items: IncomeStatement[]
  onRemoveIncomeStatement: (id: IncomeStatementId) => void
}

const ChildIncomeStatementsTable = React.memo(
  function ChildIncomeStatementsTable({
    childId,
    items,
    onRemoveIncomeStatement
  }: TableOrListProps) {
    const t = useTranslation()

    return (
      <Table data-qa="child-income-statement-table">
        <Thead>
          <Tr>
            <Th>{t.income.table.incomeStatementForm}</Th>
            <Th>{t.income.table.createdAt}</Th>
            <Th>{t.income.table.sentAt}</Th>
            <Th />
          </Tr>
        </Thead>
        <Tbody>
          {items.map((item) => (
            <Tr key={item.id} data-qa="child-income-statement-row">
              <Td>
                <Link
                  to={getLink(childId, item)}
                  data-qa="view-income-statement"
                >
                  {item.startDate.format()} - {item.endDate?.format()}
                </Link>
              </Td>
              <Td>{item.createdAt.toLocalDate().format()}</Td>
              <Td>
                {item.sentAt
                  ? item.sentAt.toLocalDate().format()
                  : t.income.table.notSent}
              </Td>
              <Td>
                <FixedSpaceRow justifyContent="flex-end">
                  {item.status === 'HANDLED' ? (
                    <Dimmed>{t.income.table.handled}</Dimmed>
                  ) : (
                    <>
                      <Link to={getLink(childId, item)}>
                        <Button
                          appearance="inline"
                          icon={faPen}
                          text={
                            item.status === 'DRAFT'
                              ? t.common.edit
                              : t.income.table.actions.addDetails
                          }
                          onClick={noop}
                          data-qa={`edit-income-statement-${item.status}`}
                        />
                      </Link>
                      <Button
                        appearance="inline"
                        icon={faTrash}
                        text={
                          item.status === 'DRAFT'
                            ? t.common.delete
                            : t.income.table.actions.cancel
                        }
                        onClick={() => onRemoveIncomeStatement(item.id)}
                        data-qa="delete-income-statement"
                      />
                    </>
                  )}
                </FixedSpaceRow>
              </Td>
            </Tr>
          ))}
        </Tbody>
      </Table>
    )
  }
)

const ChildIncomeStatementsList = React.memo(
  function ChildIncomeStatementsList({
    childId,
    items,
    onRemoveIncomeStatement
  }: TableOrListProps) {
    const t = useTranslation()

    return (
      <div data-qa="child-income-statement-list">
        {items.map((item, i) => (
          <Fragment key={item.id}>
            {i > 0 && <HorizontalLine />}
            <FixedSpaceColumn
              spacing="s"
              alignItems="flex-start"
              data-qa="child-income-statement-row"
            >
              <H3>
                {item.startDate.format()} - {item.endDate?.format()}
              </H3>
              <div>
                {t.income.table.status}: {t.income.table.statuses[item.status]}
              </div>
              <div>
                {t.income.table.createdAt}:{' '}
                {item.createdAt.toLocalDate().format()}
              </div>
              <div>
                {t.income.table.sentAt}:{' '}
                {item.sentAt?.toLocalDate()?.format() ?? '-'}
              </div>
              <Link to={getLink(childId, item)} data-qa="view-income-statement">
                <Button
                  appearance="inline"
                  icon={faFile}
                  text={t.income.table.actions.view}
                  onClick={noop}
                />
              </Link>
              {item.status !== 'HANDLED' && (
                <>
                  <Link to={getLink(childId, item)}>
                    <Button
                      appearance="inline"
                      icon={faPen}
                      text={
                        item.status === 'DRAFT'
                          ? t.common.edit
                          : t.income.table.actions.addDetails
                      }
                      onClick={noop}
                      data-qa={`edit-income-statement-${item.status}`}
                    />
                  </Link>
                  <Button
                    appearance="inline"
                    icon={faTrash}
                    text={
                      item.status === 'DRAFT'
                        ? t.common.delete
                        : t.income.table.actions.cancel
                    }
                    onClick={() => onRemoveIncomeStatement(item.id)}
                    data-qa="delete-income-statement"
                  />
                </>
              )}
            </FixedSpaceColumn>
          </Fragment>
        ))}
      </div>
    )
  }
)

const ChildIncomeStatements = React.memo(function ChildIncomeStatements({
  child
}: {
  child: ChildBasicInfo
}) {
  const t = useTranslation()
  const childId = child.id

  const [page, setPage] = useState(1)
  const incomeStatements = useQueryResult(
    childIncomeStatementsQuery({ childId: child.id, page })
  )

  const { setErrorMessage } = useContext(OverlayContext)

  const [deletionState, setDeletionState] = useState<DeletionState>({
    status: 'row-not-selected'
  })

  const { mutateAsync: deleteIncomeStatement } = useMutation(
    deleteChildIncomeStatementMutation
  )

  const onDelete = useCallback(
    (id: IncomeStatementId) => {
      setDeletionState({ status: 'deleting', rowToDelete: id })
      deleteIncomeStatement({ childId, id })
        .then(() => {
          setDeletionState({ status: 'row-not-selected' })
        })
        .catch(() => {
          setErrorMessage({
            title: t.income.errors.deleteFailed,
            type: 'error',
            resolveLabel: t.common.ok
          })
        })
    },
    [deleteIncomeStatement, childId, setErrorMessage, t]
  )

  return (
    <>
      {renderResult(incomeStatements, ({ data, pages }) =>
        data.length > 0 ? (
          <>
            <TabletAndDesktop>
              <ChildIncomeStatementsTable
                childId={childId}
                items={data}
                onRemoveIncomeStatement={(id) =>
                  setDeletionState({
                    status: 'confirming',
                    rowToDelete: id
                  })
                }
              />
            </TabletAndDesktop>
            <MobileOnly>
              <ChildIncomeStatementsList
                childId={childId}
                items={data}
                onRemoveIncomeStatement={(id) =>
                  setDeletionState({
                    status: 'confirming',
                    rowToDelete: id
                  })
                }
              />
            </MobileOnly>
            <Gap />
            <Pagination
              pages={pages}
              currentPage={page}
              setPage={setPage}
              label={t.common.page}
              hideIfOnlyOnePage={true}
            />

            {deletionState.status !== 'row-not-selected' && (
              <InfoModal
                type="warning"
                title={t.income.table.deleteConfirm}
                text={t.income.table.deleteDescription}
                icon={faQuestion}
                reject={{
                  action: () =>
                    setDeletionState({ status: 'row-not-selected' }),
                  label: t.common.return
                }}
                resolve={{
                  action: () => onDelete(deletionState.rowToDelete),
                  label: t.common.delete,
                  disabled: deletionState.status === 'deleting'
                }}
              />
            )}
          </>
        ) : (
          <InfoBox
            message={t.income.incomeStatementMissing}
            data-qa="child-income-statement-missing-warning"
          />
        )
      )}
    </>
  )
})

type DeletionState =
  | {
      status: 'row-not-selected'
    }
  | {
      status: 'confirming' | 'deleting'
      rowToDelete: IncomeStatementId
    }

interface ChildrenIncomeStatementsProps {
  childInfo: ChildBasicInfo[]
}

export default React.memo(function ChildrenIncomeStatements({
  childInfo
}: ChildrenIncomeStatementsProps) {
  const t = useTranslation()
  const [, navigate] = useLocation()

  return (
    <>
      <Container data-qa="children-income-statements">
        <Gap size="L" />
        <ContentArea opaque paddingVertical="L">
          <>
            <H1 noMargin>{t.income.children.title}</H1>
            <Gap size="s" />
            <span>{t.income.children.description}</span>
          </>
          <Gap size="L" />
          {childInfo.map((child) => (
            <ChildIncomeStatementsContainer
              key={child.id}
              data-qa="child-income-statements"
            >
              <Gap size="s" />
              <HeadingContainer>
                <H3 data-qa="child-name" translate="no">
                  <PersonName person={child} format="First Last" />
                </H3>
                <ResponsiveAddButton
                  onClick={() => navigate(`/child-income/${child.id}/new/edit`)}
                  text={t.income.addNew}
                  data-qa="new-child-income-statement-btn"
                />
              </HeadingContainer>
              <ChildIncomeStatements child={child} />
              <Gap size="L" />
            </ChildIncomeStatementsContainer>
          ))}
        </ContentArea>
      </Container>
    </>
  )
})
