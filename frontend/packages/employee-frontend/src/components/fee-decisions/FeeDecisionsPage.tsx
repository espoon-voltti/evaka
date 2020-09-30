// SPDX-FileCopyrightText: 2017-2020 City of Espoo
//
// SPDX-License-Identifier: LGPL-2.1-or-later

import React, { useCallback, useContext, useEffect, useState } from 'react'
import { Gap } from 'components/shared/layout/white-space'
import { Container, ContentArea } from 'components/shared/layout/Container'
import { InvoicingUiContext } from '../../state/invoicing-ui'
import FeeDecisions from './FeeDecisions'
import FeeDecisionFilters from './FeeDecisionFilters'
import Actions from './Actions'
import GeneratorButton from './generation/GeneratorButton'
import { isSuccess, Result, Success } from '../../api'
import {
  getDecisions,
  FeeDecisionSearchParams,
  SortByFeeDecisions,
  FeeDecisionSearchResponse
} from '../../api/invoicing'
import { useRestApi } from '~utils/useRestApi'
import { FeeDecisionSummary } from '../../types/invoicing'
import { SearchOrder } from '../../types'

const pageSize = 200

export type PagedFeeDecisions = {
  [k: number]: Result<FeeDecisionSummary[]>
}

const FeeDecisionsPage = React.memo(function FeeDecisionsPage() {
  const [page, setPage] = useState(1)
  const [sortBy, setSortBy] = useState<SortByFeeDecisions>('HEAD_OF_FAMILY')
  const [sortDirection, setSortDirection] = useState<SearchOrder>('ASC')
  const [totalDecisions, setTotalDecisions] = useState<number>()
  const [totalPages, setTotalPages] = useState<number>()
  const [decisions, setDecisions] = useState<PagedFeeDecisions>({})
  const setDecisionsResult = useCallback(
    (result: Result<FeeDecisionSearchResponse>) => {
      if (isSuccess(result)) {
        setTotalDecisions(result.data.total)
        setTotalPages(result.data.pages)
        setDecisions((prev) => ({
          ...prev,
          [page]: Success(result.data.data)
        }))
      } else {
        setDecisions((prev) => ({ ...prev, [page]: result }))
      }
    },
    [page, setTotalDecisions, setTotalPages, setDecisions]
  )
  const reloadDecisions = useRestApi(getDecisions, setDecisionsResult)

  const {
    decisions: {
      searchFilters,
      debouncedSearchTerms,
      checked,
      toggleChecked,
      checkIds,
      clearChecked
    }
  } = useContext(InvoicingUiContext)

  const loadDecisions = useCallback(() => {
    const status = searchFilters.status
    const area = searchFilters.area.join(',')
    const distinctiveDetails = searchFilters.distinctiveDetails.join(',')
    const params: FeeDecisionSearchParams = {
      status: status.length > 0 ? status : undefined,
      area: area.length > 0 ? area : undefined,
      unit: searchFilters.unit ? searchFilters.unit : undefined,
      distinctions: distinctiveDetails ? distinctiveDetails : undefined,
      searchTerms: debouncedSearchTerms ? debouncedSearchTerms : undefined,
      startDate: searchFilters.startDate?.formatIso(),
      endDate: searchFilters.endDate?.formatIso(),
      searchByStartDate: searchFilters.searchByStartDate
    }
    reloadDecisions(page, pageSize, sortBy, sortDirection, params)
  }, [
    page,
    pageSize,
    sortBy,
    sortDirection,
    searchFilters,
    debouncedSearchTerms,
    reloadDecisions
  ])

  useEffect(() => {
    loadDecisions()
  }, [loadDecisions])

  useEffect(() => {
    setPage(1)
    setTotalDecisions(undefined)
    setTotalPages(undefined)
  }, [setPage, setTotalPages, setTotalDecisions, searchFilters, debouncedSearchTerms])

  const checkAll = useCallback(() => {
    const currentPage = decisions[page]
    if (isSuccess(currentPage)) {
      checkIds(currentPage.data.map((decision) => decision.id))
    }
  }, [decisions, checkIds])

  const checkedIds = Object.keys(checked).filter((id) => !!checked[id])

  return (
    <div className="fee-decisions-page" data-qa="fee-decisions-page">
      <Container>
        <Gap size={'L'} />
        {window['secretButton'] && <GeneratorButton reload={loadDecisions} />}
        <ContentArea opaque>
          <FeeDecisionFilters />
        </ContentArea>
        <Gap size={'XL'} />
        <ContentArea opaque>
          <FeeDecisions
            decisions={decisions[page]}
            total={totalDecisions}
            pages={totalPages}
            currentPage={page}
            setPage={setPage}
            sortBy={sortBy}
            setSortBy={setSortBy}
            sortDirection={sortDirection}
            setSortDirection={setSortDirection}
            showCheckboxes={searchFilters.status === 'DRAFT'}
            checked={checked}
            toggleChecked={toggleChecked}
            checkAll={checkAll}
            clearChecked={clearChecked}
          />
          <Actions
            status={searchFilters.status}
            checkedIds={checkedIds}
            clearChecked={clearChecked}
            loadDecisions={loadDecisions}
          />
        </ContentArea>
      </Container>
    </div>
  )
})

export default FeeDecisionsPage
