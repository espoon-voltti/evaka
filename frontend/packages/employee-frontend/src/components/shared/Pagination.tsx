// SPDX-FileCopyrightText: 2017-2020 City of Espoo
//
// SPDX-License-Identifier: LGPL-2.1-or-later

import React from 'react'
import styled from 'styled-components'
import { useTranslation } from '~state/i18n'

const NUMBER_OF_PAGES_TO_SHOW = 5

const PageItem = styled.a`
  margin-left: 4px;
`

const PageSpacer = styled.span`
  margin-left: 4px;
`

const ActivePageItem = styled.span`
  margin-left: 4px;
  font-weight: 600;
`

interface Props {
  pages: number | undefined
  currentPage: number
  setPage: (page: number) => void
}

const Pagination = ({ pages = 0, currentPage, setPage }: Props) => {
  const { i18n } = useTranslation()

  const firstPage = 1
  const lastPage = pages

  const tooManyPages = pages > NUMBER_OF_PAGES_TO_SHOW

  const firstPageToShow = tooManyPages
    ? Math.ceil(Math.max(currentPage - NUMBER_OF_PAGES_TO_SHOW / 2, firstPage))
    : firstPage

  const lastPageToShow = tooManyPages
    ? Math.ceil(Math.min(currentPage + NUMBER_OF_PAGES_TO_SHOW / 2, lastPage))
    : lastPage

  return (
    <div>
      <span>{i18n.feeDecisions.table.paging}:</span>

      {firstPageToShow > 1 && (
        <PageItem
          key={1}
          onClick={() => setPage(1)}
          data-qa={`page-selector-1`}
        >
          {1}
        </PageItem>
      )}

      {firstPageToShow > 2 && <PageSpacer>...</PageSpacer>}

      {[...Array(lastPageToShow + 1 - firstPageToShow).keys()]
        .map((index) => index + firstPageToShow)
        .map((index) =>
          index === currentPage ? (
            <ActivePageItem data-qa={`active-page-${index}`} key={index}>
              {index}
            </ActivePageItem>
          ) : (
            <PageItem
              key={index}
              onClick={() => setPage(index)}
              data-qa={`page-selector-${index}`}
            >
              {index}
            </PageItem>
          )
        )}

      {lastPageToShow < lastPage - 1 && <PageSpacer>...</PageSpacer>}

      {lastPageToShow < lastPage && (
        <PageItem
          key={lastPage}
          onClick={() => setPage(lastPage)}
          data-qa={`page-selector-${lastPage}`}
        >
          {lastPage}
        </PageItem>
      )}
    </div>
  )
}

export default Pagination
