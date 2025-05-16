// SPDX-FileCopyrightText: 2017-2024 City of Espoo
//
// SPDX-License-Identifier: LGPL-2.1-or-later

import React, { useContext, useState } from 'react'

import type {
  ApplicationSortColumn,
  PagedApplicationSummaries
} from 'lib-common/generated/api-types/application'
import { constantQuery, useQueryResult } from 'lib-common/query'
import { Container, ContentArea } from 'lib-components/layout/Container'
import { Gap } from 'lib-components/white-space'

import ApplicationsList from '../../components/applications/ApplicationsList'
import { ApplicationUIContext } from '../../state/application-ui'
import type { SearchOrder } from '../../types'
import { renderResult } from '../async-rendering'

import ApplicationFilters from './ApplicationsFilters'
import { getApplicationSummariesQuery } from './queries'

export default React.memo(function ApplicationsPage() {
  const [sortBy, setSortBy] =
    useState<ApplicationSortColumn>('APPLICATION_TYPE')
  const [sortDirection, setSortDirection] = useState<SearchOrder>('ASC')

  const { confirmedSearchFilters: searchFilters, page } =
    useContext(ApplicationUIContext)

  const applications = useQueryResult(
    searchFilters
      ? getApplicationSummariesQuery({
          body: {
            page,
            sortBy,
            sortDir: sortDirection,
            areas: searchFilters.area.length > 0 ? searchFilters.area : null,
            units: searchFilters.units.length > 0 ? searchFilters.units : null,
            basis: searchFilters.basis.length > 0 ? searchFilters.basis : null,
            type: searchFilters.type,
            preschoolType:
              searchFilters.type === 'PRESCHOOL' &&
              searchFilters.preschoolType.length > 0
                ? searchFilters.preschoolType
                : null,
            statuses:
              searchFilters.status === 'ALL'
                ? searchFilters.allStatuses.length > 0
                  ? searchFilters.allStatuses
                  : null
                : [searchFilters.status],
            dateType:
              searchFilters.dateType.length > 0 ? searchFilters.dateType : null,
            distinctions:
              searchFilters.distinctions.length > 0
                ? searchFilters.distinctions
                : null,
            periodStart:
              searchFilters.startDate && searchFilters.dateType.length > 0
                ? searchFilters.startDate
                : null,
            periodEnd:
              searchFilters.endDate && searchFilters.dateType.length > 0
                ? searchFilters.endDate
                : null,
            searchTerms: searchFilters.searchTerms,
            transferApplications: searchFilters.transferApplications,
            voucherApplications: searchFilters.voucherApplications ?? null
          }
        })
      : constantQuery<PagedApplicationSummaries>({
          data: [],
          pages: 0,
          total: 0
        })
  )

  return (
    <Container data-qa="applications-page">
      <ContentArea opaque>
        <Gap size="xs" />
        <ApplicationFilters />
      </ContentArea>
      <Gap size="XL" />
      {searchFilters &&
        renderResult(applications, (applications) => (
          <ApplicationsList
            applicationsResult={applications}
            sortBy={sortBy}
            setSortBy={setSortBy}
            sortDirection={sortDirection}
            setSortDirection={setSortDirection}
          />
        ))}
    </Container>
  )
})
