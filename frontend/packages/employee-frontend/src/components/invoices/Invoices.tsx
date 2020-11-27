// SPDX-FileCopyrightText: 2017-2020 City of Espoo
//
// SPDX-License-Identifier: LGPL-2.1-or-later

import React, { useState } from 'react'
import styled from 'styled-components'
import { useHistory } from 'react-router-dom'
import { FontAwesomeIcon } from '@fortawesome/react-fontawesome'
import { faExclamation, faSync } from 'icon-set'
import { Gap } from '~components/shared/layout/white-space'
import InlineButton from '~components/shared/atoms/buttons/InlineButton'
import {
  Table,
  Tr,
  Th,
  Td,
  Thead,
  Tbody,
  SortableTh
} from '~components/shared/layout/Table'
import Title from '~components/shared/atoms/Title'
import Loader from '~components/shared/atoms/Loader'
import Checkbox from '~components/shared/atoms/form/Checkbox'
import { useTranslation } from '../../state/i18n'
import NameWithSsn from '../common/NameWithSsn'
import ChildrenCell from '../common/ChildrenCell'
import { InvoiceSummary } from '../../types/invoicing'
import { isFailure, isLoading, isSuccess, Result } from '../../api'
import { formatCents } from '../../utils/money'
import Tooltip from '~components/common/Tooltip'
import { StatusIconContainer } from '~components/common/StatusIconContainer'
import { EspooColours } from '~utils/colours'
import { SearchOrder } from '~types'
import { SortByInvoices } from '~api/invoicing'
import Pagination from '~components/shared/Pagination'
import { InvoicesAction } from './invoices-state'

interface Props {
  dispatch: React.Dispatch<InvoicesAction>
  invoices?: Result<InvoiceSummary[]>
  refreshInvoices: () => Promise<void>
  total?: number
  pages?: number
  currentPage: number
  sortBy: SortByInvoices
  sortDirection: SearchOrder
  showCheckboxes: boolean
  checked: Record<string, true>
  allInvoicesToggle: boolean
  allInvoicesToggleDisabled: boolean
}

export default React.memo(function Invoices({
  dispatch,
  invoices,
  refreshInvoices,
  total,
  pages,
  currentPage,
  sortBy,
  sortDirection,
  showCheckboxes,
  checked,
  allInvoicesToggle,
  allInvoicesToggleDisabled
}: Props) {
  const { i18n } = useTranslation()
  const [refreshError, setRefreshError] = useState(false)

  return (
    <div className="invoices">
      <RefreshInvoices>
        {refreshError ? (
          <RefreshError>{i18n.common.error.unknown}</RefreshError>
        ) : null}
        <InlineButton
          icon={faSync}
          onClick={() => {
            setRefreshError(false)
            refreshInvoices().catch(() => setRefreshError(true))
          }}
          text={i18n.invoices.buttons.createInvoices}
          dataQa="create-invoices"
        />
      </RefreshInvoices>
      <TitleRowContainer>
        <SectionTitle size={1}>{i18n.invoices.table.title}</SectionTitle>
        {invoices && isSuccess(invoices) && (
          <ResultsContainer>
            <div>{total ? i18n.feeDecisions.table.rowCount(total) : null}</div>
            <Pagination
              pages={pages}
              currentPage={currentPage}
              setPage={(payload: number) =>
                dispatch({ type: 'SET_PAGE', payload })
              }
            />
          </ResultsContainer>
        )}
      </TitleRowContainer>
      {invoices && isLoading(invoices) ? <Loader /> : null}
      {!invoices || isFailure(invoices) ? (
        <div>{i18n.common.error.unknown}</div>
      ) : null}
      {invoices && isSuccess(invoices) && (
        <>
          <ResultsContainer>
            <Checkbox
              checked={allInvoicesToggle}
              label={i18n.invoices.table.toggleAll}
              onChange={() => dispatch({ type: 'ALL_INVOICES_TOGGLE' })}
              disabled={allInvoicesToggleDisabled}
            />
          </ResultsContainer>
          <Gap size="m" />
          <Table data-qa="table-of-invoices">
            <InvoiceTableHeader
              dispatch={dispatch}
              invoices={invoices}
              checked={checked}
              sortBy={sortBy}
              sortDirection={sortDirection}
              showCheckboxes={showCheckboxes}
              allInvoicesToggle={allInvoicesToggle}
            />
            <InvoiceTableBody
              dispatch={dispatch}
              invoices={invoices.data}
              showCheckboxes={showCheckboxes}
              checked={checked}
              allInvoicesToggle={allInvoicesToggle}
            />
          </Table>
          <ResultsContainer>
            <Pagination
              pages={pages}
              currentPage={currentPage}
              setPage={(payload: number) =>
                dispatch({ type: 'SET_PAGE', payload })
              }
            />
          </ResultsContainer>
        </>
      )}
    </div>
  )
})

const RefreshInvoices = styled.div`
  position: absolute;
  top: -30px;
  right: 60px;
`

const RefreshError = styled.span`
  margin-right: 20px;
  font-size: 14px;
`

const TitleRowContainer = styled.div`
  display: flex;
  justify-content: space-between;
  margin-bottom: 1.5rem;
`

const ResultsContainer = styled.div`
  display: flex;
  flex-direction: column;
  align-items: flex-end;
  justify-content: flex-end;
`

const SectionTitle = styled(Title)`
  margin-bottom: 0 !important;
`

const InvoiceTableHeader = React.memo(function InvoiceTableHeader({
  dispatch,
  invoices,
  checked,
  sortBy,
  sortDirection,
  showCheckboxes,
  allInvoicesToggle
}: Pick<
  Props,
  | 'dispatch'
  | 'invoices'
  | 'checked'
  | 'sortBy'
  | 'sortDirection'
  | 'showCheckboxes'
  | 'allInvoicesToggle'
>) {
  const { i18n } = useTranslation()

  const allChecked =
    invoices && isSuccess(invoices)
      ? invoices.data.length > 0 && invoices.data.every((it) => checked[it.id])
      : false

  const isSorted = (column: SortByInvoices) =>
    sortBy === column ? sortDirection : undefined

  const toggleSort = (column: SortByInvoices) => () => {
    if (sortBy === column) {
      dispatch({
        type: 'SET_SORT_DIRECTION',
        payload: sortDirection === 'ASC' ? 'DESC' : 'ASC'
      })
    } else {
      dispatch({ type: 'SET_SORT_BY', payload: column })
      dispatch({ type: 'SET_SORT_DIRECTION', payload: 'ASC' })
    }
  }

  return (
    <Thead>
      <Tr>
        <SortableTh
          sorted={isSorted('HEAD_OF_FAMILY')}
          onClick={toggleSort('HEAD_OF_FAMILY')}
        >
          {i18n.invoices.table.head}
        </SortableTh>
        <SortableTh
          sorted={isSorted('CHILDREN')}
          onClick={toggleSort('CHILDREN')}
        >
          {i18n.invoices.table.children}
        </SortableTh>
        <SortableTh sorted={isSorted('START')} onClick={toggleSort('START')}>
          {i18n.invoices.table.period}
        </SortableTh>
        <SortableTh sorted={isSorted('SUM')} onClick={toggleSort('SUM')}>
          {i18n.invoices.table.totalPrice}
        </SortableTh>
        <Th>{i18n.invoices.table.nb}</Th>
        <SortableTh sorted={isSorted('STATUS')} onClick={toggleSort('STATUS')}>
          {i18n.invoices.table.status}
        </SortableTh>
        {showCheckboxes ? (
          <Th>
            <Checkbox
              hiddenLabel
              label=""
              checked={allChecked || allInvoicesToggle}
              disabled={allInvoicesToggle}
              onChange={() =>
                allChecked
                  ? dispatch({ type: 'CLEAR_CHECKED' })
                  : dispatch({ type: 'CHECK_ALL' })
              }
              dataQa="toggle-all-invoices"
            />
          </Th>
        ) : null}
      </Tr>
    </Thead>
  )
})

const InvoiceTableBody = React.memo(function InvoiceTableBody({
  dispatch,
  invoices,
  showCheckboxes,
  checked,
  allInvoicesToggle
}: Pick<
  Props,
  'dispatch' | 'showCheckboxes' | 'checked' | 'allInvoicesToggle'
> & {
  invoices: InvoiceSummary[]
}) {
  const { i18n } = useTranslation()
  const history = useHistory()

  return (
    <Tbody>
      {invoices.map((item: InvoiceSummary) => (
        <Tr
          key={item.id}
          onClick={() => history.push(`/finance/invoices/${item.id}`)}
          data-qa="table-invoice-row"
        >
          <Td>
            <NameWithSsn {...item.headOfFamily} i18n={i18n} />
          </Td>
          <Td>
            <ChildrenCell people={item.rows.map(({ child }) => child)} />
          </Td>
          <Td>{`${item.periodStart.format()} - ${item.periodEnd.format()}`}</Td>
          <Td>{formatCents(item.totalPrice)}</Td>
          <Td>
            {item.headOfFamily.restrictedDetailsEnabled && (
              <Tooltip
                tooltipId={`restricted-details-${item.id}`}
                tooltipText={`${i18n.personProfile.restrictedDetails}`}
                place={'right'}
              >
                <StatusIconContainer color={EspooColours.red}>
                  <FontAwesomeIcon icon={faExclamation} inverse />
                </StatusIconContainer>
              </Tooltip>
            )}
          </Td>
          <Td>{i18n.invoice.status[item.status]}</Td>
          {showCheckboxes ? (
            <Td onClick={(e) => e.stopPropagation()}>
              <Checkbox
                hiddenLabel
                label=""
                checked={!!checked[item.id] || allInvoicesToggle}
                disabled={allInvoicesToggle}
                onChange={() =>
                  dispatch({ type: 'TOGGLE_CHECKED', payload: item.id })
                }
                dataQa="toggle-invoice"
              />
            </Td>
          ) : null}
        </Tr>
      ))}
    </Tbody>
  )
})
