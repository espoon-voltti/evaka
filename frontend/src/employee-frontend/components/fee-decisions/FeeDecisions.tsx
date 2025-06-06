// SPDX-FileCopyrightText: 2017-2024 City of Espoo
//
// SPDX-License-Identifier: LGPL-2.1-or-later

import React, { useContext } from 'react'
import styled from 'styled-components'

import type {
  FeeDecisionSortParam,
  FeeDecisionSummary,
  SortDirection
} from 'lib-common/generated/api-types/invoicing'
import type { FeeDecisionId } from 'lib-common/generated/api-types/shared'
import { formatCents } from 'lib-common/money'
import Pagination from 'lib-components/Pagination'
import Title from 'lib-components/atoms/Title'
import Checkbox from 'lib-components/atoms/form/Checkbox'
import {
  Table,
  Tr,
  Th,
  Td,
  Thead,
  Tbody,
  SortableTh
} from 'lib-components/layout/Table'

import { getEmployeeUrlPrefix } from '../../constants'
import { useTranslation } from '../../state/i18n'
import { InvoicingUiContext } from '../../state/invoicing-ui'
import ChildrenCell from '../common/ChildrenCell'
import NameWithSsn from '../common/NameWithSsn'

import { FeeDecisionDifferenceIcons } from './FeeDecisionDifferenceIcon'

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

interface Props {
  decisions: FeeDecisionSummary[]
  total: number
  pages: number
  sortBy: FeeDecisionSortParam
  setSortBy: (v: FeeDecisionSortParam) => void
  sortDirection: SortDirection
  setSortDirection: (v: SortDirection) => void
  showCheckboxes: boolean
  isChecked: (id: FeeDecisionId) => boolean
  toggleChecked: (id: FeeDecisionId) => void
  checkAll: () => void
  clearChecked: () => void
}

const FeeDecisions = React.memo(function FeeDecisions({
  decisions,
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

  const {
    feeDecisions: { page, setPage }
  } = useContext(InvoicingUiContext)

  const allChecked =
    decisions.length > 0 && decisions.every((it) => isChecked(it.id))

  const isSorted = (column: FeeDecisionSortParam) =>
    sortBy === column ? sortDirection : undefined

  const toggleSort = (column: FeeDecisionSortParam) => () => {
    if (sortBy === column) {
      setSortDirection(sortDirection === 'ASC' ? 'DESC' : 'ASC')
    } else {
      setSortBy(column)
      setSortDirection('ASC')
    }
  }

  const rows = decisions.map((item) => (
    <Tr
      key={item.id}
      onClick={
        item.annullingDecision
          ? undefined
          : () =>
              window.open(
                `${getEmployeeUrlPrefix()}/employee/finance/fee-decisions/${
                  item.id
                }`,
                '_blank'
              )
      }
      data-qa="table-fee-decision-row"
    >
      <Td>
        <NameWithSsn {...item.headOfFamily} />
      </Td>
      <Td>
        <ChildrenCell people={item.children} />
      </Td>
      <Td>
        {item.annullingDecision
          ? `${i18n.feeDecisions.table.annullingDecision} `
          : ''}
        {item.validDuring.format()}
      </Td>
      <Td>{formatCents(item.finalPrice)}</Td>
      <Td>{item.decisionNumber}</Td>
      <Td>{item.created.toLocalDate().format()}</Td>
      <Td>{item.sentAt?.toLocalDate().format() ?? ''}</Td>
      <Td>
        <FeeDecisionDifferenceIcons difference={item.difference} />
      </Td>
      <Td>{i18n.feeDecision.status[item.status]}</Td>
      {showCheckboxes ? (
        <Td onClick={(e) => e.stopPropagation()}>
          <Checkbox
            label={item.id}
            hiddenLabel
            checked={isChecked(item.id)}
            onChange={() => toggleChecked(item.id)}
            data-qa="toggle-decision"
          />
        </Td>
      ) : null}
    </Tr>
  ))

  return (
    <div className="fee-decisions">
      <TitleRowContainer>
        <SectionTitle size={1}>{i18n.feeDecisions.table.title}</SectionTitle>
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
      <Table data-qa="table-of-decisions">
        <Thead>
          <Tr>
            <SortableTh
              sorted={isSorted('HEAD_OF_FAMILY')}
              onClick={toggleSort('HEAD_OF_FAMILY')}
            >
              {i18n.feeDecisions.table.head}
            </SortableTh>
            <Th>{i18n.feeDecisions.table.children}</Th>
            <SortableTh
              sorted={isSorted('VALIDITY')}
              onClick={toggleSort('VALIDITY')}
            >
              {i18n.feeDecisions.table.validity}
            </SortableTh>
            <SortableTh
              sorted={isSorted('FINAL_PRICE')}
              onClick={toggleSort('FINAL_PRICE')}
            >
              {i18n.feeDecisions.table.price}
            </SortableTh>
            <SortableTh
              sorted={isSorted('NUMBER')}
              onClick={toggleSort('NUMBER')}
            >
              {i18n.feeDecisions.table.number}
            </SortableTh>
            <SortableTh
              sorted={isSorted('CREATED')}
              onClick={toggleSort('CREATED')}
            >
              {i18n.feeDecisions.table.createdAt}
            </SortableTh>
            <SortableTh sorted={isSorted('SENT')} onClick={toggleSort('SENT')}>
              {i18n.feeDecisions.table.sentAt}
            </SortableTh>
            <Th>{i18n.feeDecisions.table.difference.title}</Th>
            <SortableTh
              sorted={isSorted('STATUS')}
              onClick={toggleSort('STATUS')}
            >
              {i18n.feeDecisions.table.status}
            </SortableTh>
            {showCheckboxes ? (
              <Td>
                <Checkbox
                  label="all"
                  hiddenLabel
                  checked={allChecked}
                  onChange={allChecked ? clearChecked : checkAll}
                  data-qa="toggle-all-decisions"
                />
              </Td>
            ) : null}
          </Tr>
        </Thead>
        <Tbody>{rows}</Tbody>
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

export default FeeDecisions
