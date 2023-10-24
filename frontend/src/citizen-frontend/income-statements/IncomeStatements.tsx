// SPDX-FileCopyrightText: 2017-2022 City of Espoo
//
// SPDX-License-Identifier: LGPL-2.1-or-later

import React, { useCallback, useContext, useState } from 'react'
import { Link, useNavigate } from 'react-router-dom'
import styled from 'styled-components'

import { renderResult } from 'citizen-frontend/async-rendering'
import { IncomeStatement } from 'lib-common/generated/api-types/incomestatement'
import { useMutation, useQueryResult } from 'lib-common/query'
import { UUID } from 'lib-common/types'
import Pagination from 'lib-components/Pagination'
import Main from 'lib-components/atoms/Main'
import ResponsiveAddButton from 'lib-components/atoms/buttons/ResponsiveAddButton'
import ResponsiveInlineButton from 'lib-components/atoms/buttons/ResponsiveInlineButton'
import Container, { ContentArea } from 'lib-components/layout/Container'
import { Table, Tbody, Td, Th, Thead, Tr } from 'lib-components/layout/Table'
import InfoModal from 'lib-components/molecules/modals/InfoModal'
import { Dimmed, H1, H2 } from 'lib-components/typography'
import { Gap } from 'lib-components/white-space'
import { faPen, faQuestion, faTrash } from 'lib-icons'

import Footer from '../Footer'
import { useTranslation } from '../localization'
import { OverlayContext } from '../overlay/state'

import ChildrenIncomeStatements from './ChildrenIncomeStatements'
import {
  deleteIncomeStatementMutation,
  guardianIncomeStatementChildrenQuery,
  incomeStatementsQuery
} from './queries'

const HeadingContainer = styled.div`
  display: flex;
  justify-content: space-between;
`
const Buttons = styled.div`
  display: flex;
  justify-content: flex-end;
`

function getLink(id: UUID, mode: 'view' | 'edit') {
  return `/income/${id}/${mode === 'edit' ? 'edit' : ''}`
}

const IncomeStatementsTable = React.memo(function IncomeStatementsTable({
  items,
  onRemoveIncomeStatement
}: {
  items: IncomeStatement[]
  onRemoveIncomeStatement: (id: UUID) => void
}) {
  const t = useTranslation()
  const navigate = useNavigate()

  const onEdit = useCallback(
    (id: UUID) => () => navigate(getLink(id, 'edit')),
    [navigate]
  )

  return (
    <Table>
      <Thead>
        <Tr>
          <Th>{t.income.table.incomeStatementForm}</Th>
          <Th />
        </Tr>
      </Thead>
      <Tbody>
        {items.map((item) => (
          <Tr key={item.id}>
            <Td>
              <Link
                to={getLink(item.id, 'view')}
                data-qa={`button-open-income-statement-${item.id}`}
              >
                {item.startDate.format()} - {item.endDate?.format()}
              </Link>
            </Td>
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
                      altText={t.common.edit}
                    />
                    <Gap size="xs" horizontal />
                    <ResponsiveInlineButton
                      icon={faTrash}
                      text={t.common.delete}
                      onClick={() => onRemoveIncomeStatement(item.id)}
                      altText={t.common.delete}
                    />
                  </>
                )}
              </Buttons>
            </Td>
          </Tr>
        ))}
      </Tbody>
    </Table>
  )
})

type DeletionState =
  | {
      status: 'row-not-selected'
    }
  | {
      status: 'confirming' | 'deleting'
      rowToDelete: UUID
    }

export default React.memo(function IncomeStatements() {
  const t = useTranslation()
  const navigate = useNavigate()
  const { setErrorMessage } = useContext(OverlayContext)

  const [page, setPage] = useState(1)
  const incomeStatements = useQueryResult(incomeStatementsQuery(page, 10))
  const { mutateAsync: deleteIncomeStatement } = useMutation(
    deleteIncomeStatementMutation
  )

  const children = useQueryResult(guardianIncomeStatementChildrenQuery(), {
    staleTime: 1000 * 60 * 60 * 24 // children change rarely
  })

  const [deletionState, setDeletionState] = useState<DeletionState>({
    status: 'row-not-selected'
  })

  const onDelete = useCallback(
    (id: UUID) => {
      setDeletionState({ status: 'deleting', rowToDelete: id })
      deleteIncomeStatement(id)
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
    [deleteIncomeStatement, setErrorMessage, t]
  )

  return (
    <>
      <Main>
        <Container>
          <Gap size="s" />
          <ContentArea opaque paddingVertical="L">
            <H1 noMargin>{t.income.title}</H1>
            {t.income.description}
          </ContentArea>
          <Gap size="s" />
          <ContentArea opaque paddingVertical="L">
            <HeadingContainer>
              <H2>{t.income.table.title}</H2>
              <ResponsiveAddButton
                onClick={() => navigate('/income/new/edit')}
                text={t.income.addNew}
                data-qa="new-income-statement-btn"
              />
            </HeadingContainer>
            {renderResult(incomeStatements, ({ data, pages }) => (
              <>
                <IncomeStatementsTable
                  items={data}
                  onRemoveIncomeStatement={(id) =>
                    setDeletionState({
                      status: 'confirming',
                      rowToDelete: id
                    })
                  }
                />
                <Gap />
                <Pagination
                  pages={pages}
                  currentPage={page}
                  setPage={setPage}
                  label={t.common.page}
                  hideIfOnlyOnePage={true}
                />
              </>
            ))}

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
          </ContentArea>
          <Gap size="s" />
          {renderResult(children, (children) => (
            <>
              <ChildrenIncomeStatements childInfo={children} />
            </>
          ))}
        </Container>
      </Main>
      <Footer />
    </>
  )
})
