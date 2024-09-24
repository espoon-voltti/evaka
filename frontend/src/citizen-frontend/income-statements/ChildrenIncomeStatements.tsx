// SPDX-FileCopyrightText: 2017-2022 City of Espoo
//
// SPDX-License-Identifier: LGPL-2.1-or-later

import React, { useCallback, useContext, useState } from 'react'
import { Link, useNavigate } from 'react-router-dom'
import styled from 'styled-components'

import { renderResult } from 'citizen-frontend/async-rendering'
import { ChildBasicInfo } from 'lib-common/generated/api-types/incomestatement'
import { useMutation, useQueryResult } from 'lib-common/query'
import { UUID } from 'lib-common/types'
import Pagination from 'lib-components/Pagination'
import ResponsiveAddButton from 'lib-components/atoms/buttons/ResponsiveAddButton'
import ResponsiveInlineButton from 'lib-components/atoms/buttons/ResponsiveInlineButton'
import Container, { ContentArea } from 'lib-components/layout/Container'
import { Table, Tbody, Td, Th, Thead, Tr } from 'lib-components/layout/Table'
import { InfoBox } from 'lib-components/molecules/MessageBoxes'
import InfoModal from 'lib-components/molecules/modals/InfoModal'
import { Dimmed, H1, H3 } from 'lib-components/typography'
import { Gap } from 'lib-components/white-space'
import colors from 'lib-customizations/common'
import { faPen, faQuestion, faTrash } from 'lib-icons'

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
const Buttons = styled.div`
  display: flex;
  justify-content: flex-end;
`
const ChildIncomeStatementsContainer = styled.div`
  border-style: solid;
  border-color: ${colors.grayscale.g35};
  border-width: 1px 0 0 0;
`

function getLink(childId: UUID, id: UUID, mode: 'view' | 'edit') {
  return `/child-income/${childId}/${id}/${mode === 'edit' ? 'edit' : ''}`
}

const ChildIncomeStatementsTable = React.memo(
  function ChildIncomeStatementsTable({
    child,
    setDeletionState
  }: {
    child: ChildBasicInfo
    setDeletionState: (deletionState: DeletionState) => void
  }) {
    const t = useTranslation()
    const navigate = useNavigate()

    const [page, setPage] = useState(1)
    const incomeStatements = useQueryResult(
      childIncomeStatementsQuery({ childId: child.id, page })
    )

    const onEdit = useCallback(
      (id: UUID) => () => navigate(getLink(child.id, id, 'edit')),
      [navigate, child.id]
    )

    return (
      <>
        {renderResult(incomeStatements, ({ data, pages }) =>
          data.length > 0 ? (
            <>
              <Table>
                <Thead>
                  <Tr>
                    <Th>{t.income.table.incomeStatementForm}</Th>
                    <Th>{t.income.table.createdAt}</Th>
                    <Th />
                  </Tr>
                </Thead>
                <Tbody>
                  {data.map((item) => (
                    <Tr key={item.id}>
                      <Td>
                        <Link
                          to={getLink(child.id, item.id, 'view')}
                          data-qa="button-open-income-statement"
                        >
                          {item.startDate.format()} - {item.endDate?.format()}
                        </Link>
                      </Td>
                      <Td>{item.created.toLocalDate().format()}</Td>
                      <Td>
                        <Buttons>
                          {item.handled ? (
                            <Dimmed>{t.income.table.handled}</Dimmed>
                          ) : (
                            <>
                              <ResponsiveInlineButton
                                icon={faPen}
                                text={t.common.edit}
                                onClick={onEdit(item.id)}
                                data-qa="edit-income-statement"
                              />
                              <Gap size="xs" horizontal />
                              <ResponsiveInlineButton
                                icon={faTrash}
                                text={t.common.delete}
                                onClick={() =>
                                  setDeletionState({
                                    status: 'confirming',
                                    rowToDelete: item.id,
                                    childId: child.id
                                  })
                                }
                                data-qa="delete-income-statement"
                              />
                            </>
                          )}
                        </Buttons>
                      </Td>
                    </Tr>
                  ))}
                </Tbody>
              </Table>

              <Gap />

              <Pagination
                pages={pages}
                currentPage={page}
                setPage={setPage}
                label={t.common.page}
                hideIfOnlyOnePage={true}
              />
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
  }
)

type DeletionState =
  | {
      status: 'row-not-selected'
    }
  | {
      status: 'confirming' | 'deleting'
      rowToDelete: UUID
      childId: UUID
    }

interface ChildrenIncomeStatementsProps {
  childInfo: ChildBasicInfo[]
}

export default React.memo(function ChildrenIncomeStatements({
  childInfo
}: ChildrenIncomeStatementsProps) {
  const t = useTranslation()
  const navigate = useNavigate()
  const { setErrorMessage } = useContext(OverlayContext)

  const [deletionState, setDeletionState] = useState<DeletionState>({
    status: 'row-not-selected'
  })

  const { mutateAsync: deleteChildIncomeStatement } = useMutation(
    deleteChildIncomeStatementMutation
  )

  const onDelete = useCallback(
    (childId: UUID, id: UUID) => {
      setDeletionState({ status: 'deleting', rowToDelete: id, childId })
      deleteChildIncomeStatement({ childId, id })
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
    [deleteChildIncomeStatement, setErrorMessage, t]
  )

  return (
    <>
      <Container data-qa="children-income-statements">
        <Gap size="L" />
        <ContentArea opaque paddingVertical="L">
          <>
            <H1 noMargin>{t.income.children.title}</H1>
            <Gap size="s" />
            {childInfo.length > 0 ? (
              <span>{t.income.children.description}</span>
            ) : (
              <span>{t.income.children.noChildIncomeStatementsNeeded}</span>
            )}
          </>
          <Gap size="L" />
          {childInfo.map((child) => (
            <ChildIncomeStatementsContainer
              key={child.id}
              data-qa="child-income-statement"
            >
              <Gap size="s" />
              <HeadingContainer>
                <H3 data-qa="child-name" translate="no">
                  {child.firstName} {child.lastName}
                </H3>
                <ResponsiveAddButton
                  onClick={() => navigate(`/child-income/${child.id}/new/edit`)}
                  text={t.income.addNew}
                  data-qa="new-child-income-statement-btn"
                />
              </HeadingContainer>
              <ChildIncomeStatementsTable
                child={child}
                setDeletionState={setDeletionState}
              />
              <Gap size="L" />
            </ChildIncomeStatementsContainer>
          ))}

          {deletionState.status !== 'row-not-selected' && (
            <InfoModal
              type="warning"
              title={t.income.table.deleteConfirm}
              text={t.income.table.deleteDescription}
              icon={faQuestion}
              reject={{
                action: () => setDeletionState({ status: 'row-not-selected' }),
                label: t.common.return
              }}
              resolve={{
                action: () =>
                  onDelete(deletionState.childId, deletionState.rowToDelete),
                label: t.common.delete,
                disabled: deletionState.status === 'deleting'
              }}
            />
          )}
        </ContentArea>
      </Container>
    </>
  )
})
