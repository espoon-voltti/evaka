// SPDX-FileCopyrightText: 2017-2021 City of Espoo
//
// SPDX-License-Identifier: LGPL-2.1-or-later

import { IncomeStatement } from 'lib-common/api-types/incomeStatement'
import { UUID } from 'lib-common/types'
import { useApiState } from 'lib-common/utils/useRestApi'
import Container, { ContentArea } from 'lib-components/layout/Container'
import { Table, Tbody, Td, Th, Thead, Tr } from 'lib-components/layout/Table'
import InfoModal from 'lib-components/molecules/modals/InfoModal'
import { Dimmed, H1, H2 } from 'lib-components/typography'
import { Gap } from 'lib-components/white-space'
import { faPen, faQuestion, faTrash } from 'lib-icons'
import React, { useCallback, useContext, useState } from 'react'
import { Link, useHistory } from 'react-router-dom'
import styled from 'styled-components'
import Pagination from 'lib-components/Pagination'
import Footer from '../Footer'
import { useTranslation } from '../localization'
import { OverlayContext } from '../overlay/state'
import { deleteIncomeStatement, getIncomeStatements } from './api'
import ResponsiveInlineButton from 'lib-components/atoms/buttons/ResponsiveInlineButton'
import ResponsiveAddButton from 'lib-components/atoms/buttons/ResponsiveAddButton'
import { renderResult } from 'citizen-frontend/async-rendering'

const HeadingContainer = styled.div`
  display: flex;
  justify-content: space-between;
`
const Buttons = styled.div`
  display: flex;
  justify-content: flex-end;
`

function IncomeStatementsTable({
  items,
  onRemoveIncomeStatement
}: {
  items: IncomeStatement[]
  onRemoveIncomeStatement: (id: UUID) => void
}) {
  const t = useTranslation()
  const history = useHistory()

  const getLink = (id: UUID, mode: 'view' | 'edit') =>
    `/income/${id}/${mode === 'edit' ? 'edit' : ''}`

  const onEdit = (id: UUID) => () => history.push(getLink(id, 'edit'))

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
                    />
                    <Gap size="xs" horizontal />
                    <ResponsiveInlineButton
                      icon={faTrash}
                      text={t.common.delete}
                      onClick={() => onRemoveIncomeStatement(item.id)}
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
}

type DeletionState =
  | {
      status: 'row-not-selected'
    }
  | {
      status: 'confirming' | 'deleting'
      rowToDelete: UUID
    }

export default function IncomeStatements() {
  const t = useTranslation()
  const history = useHistory()
  const { setErrorMessage } = useContext(OverlayContext)

  const [page, setPage] = useState(1)
  const [incomeStatements, fetchIncomeStatements] = useApiState(
    getIncomeStatements,
    page,
    10
  )

  const [deletionState, setDeletionState] = useState<DeletionState>({
    status: 'row-not-selected'
  })

  const onDelete = useCallback(
    (id: UUID) => {
      setDeletionState({ status: 'deleting', rowToDelete: id })
      void deleteIncomeStatement(id).then((res) => {
        if (res.isFailure) {
          setErrorMessage({
            title: t.income.errors.deleteFailed,
            type: 'error',
            resolveLabel: t.common.ok
          })
        }
        setDeletionState({ status: 'row-not-selected' })
        fetchIncomeStatements()
      })
    },
    [fetchIncomeStatements, setErrorMessage, t]
  )

  return (
    <>
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
              onClick={() => history.push('/income/new/edit')}
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
              />
            </>
          ))}

          {deletionState.status !== 'row-not-selected' && (
            <InfoModal
              iconColour="orange"
              title={t.income.table.deleteConfirm}
              text={t.income.table.deleteDescription}
              icon={faQuestion}
              reject={{
                action: () => setDeletionState({ status: 'row-not-selected' }),
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
      </Container>
      <Footer />
    </>
  )
}
