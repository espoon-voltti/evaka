// SPDX-FileCopyrightText: 2017-2024 City of Espoo
//
// SPDX-License-Identifier: LGPL-2.1-or-later

import { FontAwesomeIcon } from '@fortawesome/react-fontawesome'
import React, { useContext } from 'react'
import { useNavigate } from 'react-router'
import styled from 'styled-components'

import {
  Payment,
  PaymentSortParam,
  SortDirection
} from 'lib-common/generated/api-types/invoicing'
import { PaymentId } from 'lib-common/generated/api-types/shared'
import { formatCents } from 'lib-common/money'
import Pagination from 'lib-components/Pagination'
import Title from 'lib-components/atoms/Title'
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
import { faExclamationTriangle, faSync } from 'lib-icons'

import { useTranslation } from '../../state/i18n'
import { InvoicingUiContext } from '../../state/invoicing-ui'

import { createPaymentDraftsMutation } from './queries'

interface Props {
  payments: Payment[]
  total: number
  pages: number
  sortBy: PaymentSortParam
  setSortBy: (v: PaymentSortParam) => void
  sortDirection: SortDirection
  setSortDirection: (v: SortDirection) => void
  showCheckboxes: boolean
  isChecked: (id: PaymentId) => boolean
  toggleChecked: (id: PaymentId) => void
  checkAll: () => void
  clearChecked: () => void
}

export default React.memo(function Payments({
  payments,
  total,
  pages,
  sortBy,
  setSortBy,
  sortDirection,
  setSortDirection,
  showCheckboxes,
  isChecked,
  toggleChecked,
  checkAll,
  clearChecked
}: Props) {
  const { i18n } = useTranslation()
  const navigate = useNavigate()

  const {
    payments: { page, setPage }
  } = useContext(InvoicingUiContext)

  const allChecked =
    payments.length > 0 && payments.every((it) => isChecked(it.id))

  const isSorted = (column: PaymentSortParam) =>
    sortBy === column ? sortDirection : undefined

  const toggleSort = (column: PaymentSortParam) => () => {
    if (sortBy === column) {
      setSortDirection(sortDirection === 'ASC' ? 'DESC' : 'ASC')
    } else {
      setSortBy(column)
      setSortDirection('ASC')
    }
  }

  return (
    <div className="payments">
      <CreatePayments>
        <MutateButton
          appearance="inline"
          icon={faSync}
          mutation={createPaymentDraftsMutation}
          onClick={() => undefined}
          text={i18n.payments.buttons.createPaymentDrafts}
          data-qa="create-payment-drafts"
        />
      </CreatePayments>
      <TitleRowContainer>
        <SectionTitle size={1}>{i18n.payments.table.title}</SectionTitle>
        <ResultsContainer>
          <div>{total ? i18n.common.resultCount(total) : null}</div>
          <Pagination
            pages={pages}
            currentPage={page}
            setPage={setPage}
            label={i18n.common.page}
          />
        </ResultsContainer>
      </TitleRowContainer>

      <Table>
        <Thead>
          <Tr>
            <SortableTh sorted={isSorted('UNIT')} onClick={toggleSort('UNIT')}>
              {i18n.payments.table.unit}
            </SortableTh>
            <SortableTh
              sorted={isSorted('PERIOD')}
              onClick={toggleSort('PERIOD')}
            >
              {i18n.payments.table.period}
            </SortableTh>
            <SortableTh
              sorted={isSorted('CREATED')}
              onClick={toggleSort('CREATED')}
            >
              {i18n.payments.table.createdAt}
            </SortableTh>
            <SortableTh
              sorted={isSorted('NUMBER')}
              onClick={toggleSort('NUMBER')}
            >
              {i18n.payments.table.number}
            </SortableTh>
            <SortableTh
              sorted={isSorted('AMOUNT')}
              onClick={toggleSort('AMOUNT')}
            >
              {i18n.payments.table.amount}
            </SortableTh>
            <Th>{i18n.payments.table.status}</Th>
            <Th>{i18n.payments.table.nb}</Th>
            {showCheckboxes ? (
              <Th>
                <Checkbox
                  hiddenLabel
                  label=""
                  checked={allChecked}
                  onChange={allChecked ? clearChecked : checkAll}
                  data-qa="toggle-all-payments"
                />
              </Th>
            ) : null}
          </Tr>
        </Thead>
        <Tbody>
          {payments.map((item) => (
            <Tr
              key={item.id}
              onClick={() =>
                navigate(
                  `/reports/voucher-service-providers/${item.unit.id}?year=${item.period.start.year}&month=${item.period.start.month}`
                )
              }
              data-qa="table-payment-row"
            >
              <Td>{item.unit.name}</Td>
              <Td>{item.period.format()}</Td>
              <Td>{item.created.toLocalDate().formatExotic('d.M.yyyy')}</Td>
              <Td>{item.number}</Td>
              <Td>{formatCents(item.amount)}</Td>
              <Td>{i18n.payments.status[item.status]}</Td>
              <Td>
                {!item.unit.businessId ||
                !item.unit.iban ||
                !item.unit.providerId ? (
                  <MissingDetailsWarning>
                    <FontAwesomeIcon icon={faExclamationTriangle} />
                    {i18n.payments.table.missingPaymentDetails}
                  </MissingDetailsWarning>
                ) : null}
              </Td>
              {showCheckboxes ? (
                <Td onClick={(e) => e.stopPropagation()}>
                  <Checkbox
                    hiddenLabel
                    label=""
                    checked={isChecked(item.id)}
                    onChange={() => toggleChecked(item.id)}
                  />
                </Td>
              ) : null}
            </Tr>
          ))}
        </Tbody>
      </Table>
      <ResultsContainer>
        <Pagination
          pages={pages}
          currentPage={page}
          setPage={setPage}
          label={i18n.common.page}
        />
      </ResultsContainer>
    </div>
  )
})

const CreatePayments = styled.div`
  position: absolute;
  top: -30px;
  right: 60px;
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

const MissingDetailsWarning = styled.div`
  color: ${(p) => p.theme.colors.status.warning};
`
