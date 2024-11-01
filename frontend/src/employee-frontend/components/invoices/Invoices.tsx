// SPDX-FileCopyrightText: 2017-2022 City of Espoo
//
// SPDX-License-Identifier: LGPL-2.1-or-later

import { FontAwesomeIcon } from '@fortawesome/react-fontawesome'
import React from 'react'
import { useNavigate } from 'react-router-dom'
import styled from 'styled-components'

import { isLoading, Result } from 'lib-common/api'
import { User } from 'lib-common/api-types/employee-auth'
import { useBoolean } from 'lib-common/form/hooks'
import {
  InvoiceSortParam,
  InvoiceSummaryResponse,
  PagedInvoiceSummaryResponses,
  SortDirection
} from 'lib-common/generated/api-types/invoicing'
import { formatCents } from 'lib-common/money'
import { UUID } from 'lib-common/types'
import Pagination from 'lib-components/Pagination'
import Title from 'lib-components/atoms/Title'
import Tooltip from 'lib-components/atoms/Tooltip'
import { MutateButton } from 'lib-components/atoms/buttons/MutateButton'
import Checkbox from 'lib-components/atoms/form/Checkbox'
import {
  SortableTh,
  Table,
  Tbody,
  Td,
  Th,
  Thead,
  Tr
} from 'lib-components/layout/Table'
import { Gap } from 'lib-components/white-space'
import colors from 'lib-customizations/common'
import { faExclamation, faSync } from 'lib-icons'

import { useTranslation } from '../../state/i18n'
import { renderResult } from '../async-rendering'
import ChildrenCell from '../common/ChildrenCell'
import NameWithSsn from '../common/NameWithSsn'
import { StatusIconContainer } from '../common/StatusIconContainer'

import { createDraftInvoicesMutation } from './queries'

interface Props {
  user: User | undefined
  pagedInvoices: Result<PagedInvoiceSummaryResponses>

  currentPage: number
  setCurrentPage: (p: number) => void
  sortBy: InvoiceSortParam
  setSortBy: (sb: InvoiceSortParam) => void
  sortDirection: SortDirection
  setSortDirection: (sd: SortDirection) => void

  showCheckboxes: boolean

  checked: Set<UUID>
  checkAll: () => void
  clearChecked: () => void
  toggleChecked: (id: UUID) => void

  fullAreaSelection: boolean
  setFullAreaSelection: (checked: boolean) => void
  fullAreaSelectionDisabled: boolean
}

export default React.memo(function Invoices({
  user,
  pagedInvoices,
  currentPage,
  setCurrentPage,
  sortBy,
  setSortBy,
  sortDirection,
  setSortDirection,
  showCheckboxes,
  checked,
  checkAll,
  clearChecked,
  toggleChecked,
  fullAreaSelection,
  setFullAreaSelection,
  fullAreaSelectionDisabled
}: Props) {
  const { i18n } = useTranslation()

  const [
    draftCreationError,
    { on: setDraftCreationError, off: clearDraftCreationError }
  ] = useBoolean(false)

  return (
    <div className="invoices" data-isloading={isLoading(pagedInvoices)}>
      {user?.accessibleFeatures.createDraftInvoices && (
        <RefreshInvoices>
          {draftCreationError && (
            <RefreshError>{i18n.common.error.unknown}</RefreshError>
          )}
          <MutateButton
            appearance="inline"
            icon={faSync}
            mutation={createDraftInvoicesMutation}
            onClick={() => undefined}
            onSuccess={clearDraftCreationError}
            onFailure={setDraftCreationError}
            text={i18n.invoices.buttons.createInvoices}
            data-qa="create-invoices"
          />
        </RefreshInvoices>
      )}
      <TitleRowContainer>
        <SectionTitle size={1}>{i18n.invoices.table.title}</SectionTitle>
        {pagedInvoices?.isSuccess && (
          <ResultsContainer>
            <div>
              {pagedInvoices.value.total
                ? i18n.common.resultCount(pagedInvoices.value.total)
                : null}
            </div>
            <Pagination
              pages={pagedInvoices.value.pages}
              currentPage={currentPage}
              setPage={setCurrentPage}
              label={i18n.common.page}
            />
          </ResultsContainer>
        )}
      </TitleRowContainer>

      {renderResult(pagedInvoices, ({ data: invoices, pages }) => (
        <>
          <ResultsContainer>
            {showCheckboxes && (
              <Checkbox
                checked={fullAreaSelection}
                label={i18n.invoices.table.toggleAll}
                onChange={setFullAreaSelection}
                disabled={fullAreaSelectionDisabled}
              />
            )}
          </ResultsContainer>
          <Gap size="m" />
          <Table data-qa="table-of-invoices">
            <InvoiceTableHeader
              invoices={invoices}
              checked={checked}
              checkAll={checkAll}
              clearChecked={clearChecked}
              sortBy={sortBy}
              setSortBy={setSortBy}
              sortDirection={sortDirection}
              setSortDirection={setSortDirection}
              showCheckboxes={showCheckboxes}
              fullAreaSelection={fullAreaSelection}
            />
            <InvoiceTableBody
              invoices={invoices}
              showCheckboxes={showCheckboxes}
              checked={checked}
              toggleChecked={toggleChecked}
              fullAreaSelection={fullAreaSelection}
            />
          </Table>
          <ResultsContainer>
            <Pagination
              pages={pages}
              currentPage={currentPage}
              setPage={setCurrentPage}
              label={i18n.common.page}
            />
          </ResultsContainer>
        </>
      ))}
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
  invoices,
  checked,
  sortBy,
  setSortBy,
  sortDirection,
  setSortDirection,
  showCheckboxes,
  clearChecked,
  checkAll,
  fullAreaSelection
}: Pick<
  Props,
  | 'checked'
  | 'sortBy'
  | 'setSortBy'
  | 'sortDirection'
  | 'setSortDirection'
  | 'showCheckboxes'
  | 'clearChecked'
  | 'checkAll'
  | 'fullAreaSelection'
> & {
  invoices: InvoiceSummaryResponse[]
}) {
  const { i18n } = useTranslation()

  const allChecked =
    invoices.length > 0 && invoices.every((i) => checked.has(i.data.id))

  const isSorted = (column: InvoiceSortParam) =>
    sortBy === column ? sortDirection : undefined

  const toggleSort = (column: InvoiceSortParam) => () => {
    if (sortBy === column) {
      setSortDirection(sortDirection === 'ASC' ? 'DESC' : 'ASC')
    } else {
      setSortBy(column)
      setSortDirection('ASC')
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
        <SortableTh
          sorted={isSorted('CREATED_AT')}
          onClick={toggleSort('CREATED_AT')}
        >
          {i18n.invoices.table.createdAt}
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
              checked={allChecked || fullAreaSelection}
              disabled={fullAreaSelection}
              onChange={() => (allChecked ? clearChecked() : checkAll())}
              data-qa="toggle-all-invoices"
            />
          </Th>
        ) : null}
      </Tr>
    </Thead>
  )
})

const InvoiceTableBody = React.memo(function InvoiceTableBody({
  invoices,
  showCheckboxes,
  checked,
  toggleChecked,
  fullAreaSelection
}: Pick<
  Props,
  'showCheckboxes' | 'checked' | 'toggleChecked' | 'fullAreaSelection'
> & {
  invoices: InvoiceSummaryResponse[]
}) {
  const { i18n } = useTranslation()
  const navigate = useNavigate()

  return (
    <Tbody>
      {invoices.map(
        ({ data: item, permittedActions }: InvoiceSummaryResponse) => (
          <Tr
            key={item.id}
            onClick={() => navigate(`/finance/invoices/${item.id}`)}
            data-qa="table-invoice-row"
          >
            <Td>
              <NameWithSsn {...item.headOfFamily} i18n={i18n} />
            </Td>
            <Td>
              <ChildrenCell people={item.children} />
            </Td>
            <Td>{`${item.periodStart.format()} - ${item.periodEnd.format()}`}</Td>
            <Td data-qa="invoice-created-at">
              {item.createdAt?.toLocalDate().format() ?? ''}
            </Td>
            <Td data-qa="invoice-total">{formatCents(item.totalPrice)}</Td>
            <Td>
              {item.headOfFamily.restrictedDetailsEnabled && (
                <Tooltip
                  tooltip={i18n.personProfile.restrictedDetails}
                  position="right"
                >
                  <StatusIconContainer color={colors.status.danger}>
                    <FontAwesomeIcon icon={faExclamation} inverse />
                  </StatusIconContainer>
                </Tooltip>
              )}
            </Td>
            <Td>{i18n.invoice.status[item.status]}</Td>
            {showCheckboxes && permittedActions.includes('SEND') ? (
              <Td onClick={(e) => e.stopPropagation()}>
                <Checkbox
                  hiddenLabel
                  label=""
                  checked={checked.has(item.id) || fullAreaSelection}
                  disabled={fullAreaSelection}
                  onChange={() => toggleChecked(item.id)}
                  data-qa="toggle-invoice"
                />
              </Td>
            ) : null}
          </Tr>
        )
      )}
    </Tbody>
  )
})
