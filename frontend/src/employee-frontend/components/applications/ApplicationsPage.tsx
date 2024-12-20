// SPDX-FileCopyrightText: 2017-2022 City of Espoo
//
// SPDX-License-Identifier: LGPL-2.1-or-later

import React, { useCallback, useContext, useEffect, useState } from 'react'
import styled from 'styled-components'

import { Result, wrapResult } from 'lib-common/api'
import {
  ApplicationSortColumn,
  PagedApplicationSummaries,
  SearchApplicationRequest
} from 'lib-common/generated/api-types/application'
import { useRestApi } from 'lib-common/utils/useRestApi'
import ErrorSegment from 'lib-components/atoms/state/ErrorSegment'
import { SpinnerSegment } from 'lib-components/atoms/state/Spinner'
import { Container, ContentArea } from 'lib-components/layout/Container'
import { H1 } from 'lib-components/typography'
import { defaultMargins, Gap } from 'lib-components/white-space'

import ApplicationsList from '../../components/applications/ApplicationsList'
import { getApplicationSummaries } from '../../generated/api-clients/application'
import { ApplicationUIContext } from '../../state/application-ui'
import { useTranslation } from '../../state/i18n'
import { SearchOrder } from '../../types'

import ApplicationFilters from './ApplicationsFilters'

const getApplicationSummariesResult = wrapResult(getApplicationSummaries)

const PaddedDiv = styled.div`
  padding: ${defaultMargins.m} ${defaultMargins.L};
`

export default React.memo(function ApplicationsPage() {
  const { i18n } = useTranslation()
  const [page, setPage] = useState(1)
  const [sortBy, setSortBy] =
    useState<ApplicationSortColumn>('APPLICATION_TYPE')
  const [sortDirection, setSortDirection] = useState<SearchOrder>('ASC')

  const {
    applicationsResult,
    setApplicationsResult,
    debouncedApplicationSearchFilters,
    setCheckedIds
  } = useContext(ApplicationUIContext)

  const onApplicationsResponse = useCallback(
    (result: Result<PagedApplicationSummaries>) => {
      setApplicationsResult(result)

      // ensure current page is within range
      if (result.isSuccess && result.value.pages < page) {
        setPage(result.value.pages)
      }
    },
    [setApplicationsResult] // eslint-disable-line react-hooks/exhaustive-deps
  )

  const reloadApplications = useRestApi(
    getApplicationSummariesResult,
    onApplicationsResponse
  )

  const loadApplications = useCallback(() => {
    const params: SearchApplicationRequest = {
      page,
      sortBy,
      sortDir: sortDirection,
      areas:
        debouncedApplicationSearchFilters.area.length > 0
          ? debouncedApplicationSearchFilters.area
          : null,
      units:
        debouncedApplicationSearchFilters.units.length > 0
          ? debouncedApplicationSearchFilters.units
          : null,
      basis:
        debouncedApplicationSearchFilters.basis.length > 0
          ? debouncedApplicationSearchFilters.basis
          : null,
      type: debouncedApplicationSearchFilters.type,
      preschoolType:
        debouncedApplicationSearchFilters.type === 'PRESCHOOL' &&
        debouncedApplicationSearchFilters.preschoolType.length > 0
          ? debouncedApplicationSearchFilters.preschoolType
          : null,
      statuses:
        debouncedApplicationSearchFilters.status === 'ALL'
          ? debouncedApplicationSearchFilters.allStatuses.length > 0
            ? debouncedApplicationSearchFilters.allStatuses
            : null
          : [debouncedApplicationSearchFilters.status],
      dateType:
        debouncedApplicationSearchFilters.dateType.length > 0
          ? debouncedApplicationSearchFilters.dateType
          : null,
      distinctions:
        debouncedApplicationSearchFilters.distinctions.length > 0
          ? debouncedApplicationSearchFilters.distinctions
          : null,
      periodStart:
        debouncedApplicationSearchFilters.startDate &&
        debouncedApplicationSearchFilters.dateType.length > 0
          ? debouncedApplicationSearchFilters.startDate
          : null,
      periodEnd:
        debouncedApplicationSearchFilters.endDate &&
        debouncedApplicationSearchFilters.dateType.length > 0
          ? debouncedApplicationSearchFilters.endDate
          : null,
      searchTerms: debouncedApplicationSearchFilters.searchTerms,
      transferApplications:
        debouncedApplicationSearchFilters.transferApplications,
      voucherApplications:
        debouncedApplicationSearchFilters.voucherApplications ?? null
    }

    void reloadApplications({ body: params })
  }, [
    page,
    sortBy,
    sortDirection,
    debouncedApplicationSearchFilters,
    reloadApplications
  ])

  useEffect(() => {
    loadApplications()
  }, [loadApplications])

  useEffect(() => {
    const onVisibilityChange = () => {
      if (document.visibilityState === 'visible') {
        void loadApplications()
      }
    }

    document.addEventListener('visibilitychange', onVisibilityChange)

    return () =>
      document.removeEventListener('visibilitychange', onVisibilityChange)
  }, [loadApplications])

  // when changing filters, sorting, etc, set page to 1 and reload
  useEffect(() => {
    setPage(1)
    setCheckedIds([])
  }, [setPage, debouncedApplicationSearchFilters, setCheckedIds])

  return (
    <Container data-qa="applications-page">
      <ContentArea opaque>
        <Gap size="xs" />
        <ApplicationFilters />
      </ContentArea>
      <Gap size="XL" />
      <ContentArea opaque paddingVertical="zero" paddingHorizontal="zero">
        {applicationsResult.isFailure && (
          <PaddedDiv>
            <H1>
              {debouncedApplicationSearchFilters.status === 'ALL'
                ? i18n.applications.list.title
                : i18n.application.statuses[
                    debouncedApplicationSearchFilters.status
                  ]}
            </H1>
            <ErrorSegment />
          </PaddedDiv>
        )}

        {applicationsResult.isLoading && (
          <PaddedDiv>
            <H1>
              {debouncedApplicationSearchFilters.status === 'ALL'
                ? i18n.applications.list.title
                : i18n.application.statuses[
                    debouncedApplicationSearchFilters.status
                  ]}
            </H1>
            <SpinnerSegment data-qa="applications-spinner" />
          </PaddedDiv>
        )}

        {applicationsResult.isSuccess && (
          <ApplicationsList
            applicationsResult={applicationsResult.value}
            reloadApplications={loadApplications}
            currentPage={page}
            setPage={setPage}
            sortBy={sortBy}
            setSortBy={setSortBy}
            sortDirection={sortDirection}
            setSortDirection={setSortDirection}
          />
        )}
      </ContentArea>
    </Container>
  )
})
