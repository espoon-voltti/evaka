// SPDX-FileCopyrightText: 2017-2021 City of Espoo
//
// SPDX-License-Identifier: LGPL-2.1-or-later

import { Loading, Result } from 'lib-common/api'
import { IncomeStatement } from 'lib-common/api-types/incomeStatement'
import { UUID } from 'lib-common/types'
import AddButton from 'lib-components/atoms/buttons/AddButton'
import InlineButton from 'lib-components/atoms/buttons/InlineButton'
import ErrorSegment from 'lib-components/atoms/state/ErrorSegment'
import { SpinnerSegment } from 'lib-components/atoms/state/Spinner'
import Container, { ContentArea } from 'lib-components/layout/Container'
import { Table, Tbody, Td, Th, Thead, Tr } from 'lib-components/layout/Table'
import { H1, H2 } from 'lib-components/typography'
import { Gap } from 'lib-components/white-space'
import { faFileAlt, faPen, faQuestion, faTrash } from 'lib-icons'
import React, { useCallback, useContext, useEffect, useState } from 'react'
import { useHistory } from 'react-router-dom'
import styled from 'styled-components'
import { useRestApi } from 'lib-common/utils/useRestApi'
import IconButton from 'lib-components/atoms/buttons/IconButton'
import InfoModal from 'lib-components/molecules/modals/InfoModal'
import Footer from '../Footer'
import { useTranslation } from '../localization'
import { OverlayContext } from '../overlay/state'
import { getIncomeStatements, deleteIncomeStatement } from './api'

const HeadingContainer = styled.div`
  display: flex;
  justify-content: space-between;
`
const Buttons = styled.div`
  display: flex;
  justify-content: space-around;
`

function IncomeStatementsTable({
  items,
  onRemoveIncomeStatement
}: {
  items: IncomeStatement[]
  onRemoveIncomeStatement: (id: UUID) => void
}) {
  const t = useTranslation().income.table
  const history = useHistory()

  const onOpen = useCallback(
    (item: IncomeStatement, mode: 'view' | 'edit') => () =>
      history.push(`/income/${item.id}/${mode === 'edit' ? 'edit' : ''}`),
    [history]
  )

  return (
    <Table>
      <Thead>
        <Tr>
          <Th>{t.incomeStatementForm}</Th>
          <Th>{t.startDate}</Th>
          <Th>{t.endDate}</Th>
          <Th />
        </Tr>
      </Thead>
      <Tbody>
        {items.map((item) => (
          <Tr key={item.id}>
            <Td>
              <InlineButton
                icon={faFileAlt}
                text={t.openIncomeStatement}
                onClick={onOpen(item, 'view')}
                data-qa={`button-open-income-statement-${item.id}`}
              />
            </Td>
            <Td>{item.startDate.format()}</Td>
            <Td>{item.endDate?.format()}</Td>
            <Td>
              {!item.handled ? (
                <Buttons>
                  <IconButton icon={faPen} onClick={onOpen(item, 'edit')} />
                  <Gap size="xs" horizontal />
                  <IconButton
                    icon={faTrash}
                    onClick={() => onRemoveIncomeStatement(item.id)}
                  />
                </Buttons>
              ) : (
                t.handled
              )}
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

  const [incomeStatements, setIncomeStatements] = useState<
    Result<IncomeStatement[]>
  >(Loading.of())

  const loadData = useRestApi(getIncomeStatements, setIncomeStatements)

  useEffect(() => loadData(), [loadData])

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
        loadData()
      })
    },
    [loadData, setErrorMessage, t]
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
            <AddButton
              onClick={() => history.push('/income/new/edit')}
              text={t.income.addNew}
              data-qa="new-income-statement-btn"
            />
          </HeadingContainer>
          {incomeStatements.mapAll({
            loading() {
              return <SpinnerSegment />
            },
            failure() {
              return <ErrorSegment />
            },
            success(items) {
              return (
                items.length > 0 && (
                  <IncomeStatementsTable
                    items={items}
                    onRemoveIncomeStatement={(id) =>
                      setDeletionState({
                        status: 'confirming',
                        rowToDelete: id
                      })
                    }
                  />
                )
              )
            }
          })}

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
                label: t.common.delete
              }}
              resolveDisabled={deletionState.status === 'deleting'}
            />
          )}
        </ContentArea>
      </Container>
      <Footer />
    </>
  )
}
