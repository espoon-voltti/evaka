// SPDX-FileCopyrightText: 2017-2022 City of Espoo
//
// SPDX-License-Identifier: LGPL-2.1-or-later

import React, { useCallback, useContext, useEffect, useState } from 'react'
import styled from 'styled-components'

import { Paged, Result } from 'lib-common/api'
import { ApplicationSummary } from 'lib-common/generated/api-types/application'
import { useRestApi } from 'lib-common/utils/useRestApi'
import ErrorSegment from 'lib-components/atoms/state/ErrorSegment'
import { SpinnerSegment } from 'lib-components/atoms/state/Spinner'
import { Container, ContentArea } from 'lib-components/layout/Container'
import { H1 } from 'lib-components/typography'
import { defaultMargins, Gap } from 'lib-components/white-space'

import { getApplications } from '../../api/applications'
import ApplicationsList from '../../components/applications/ApplicationsList'
import { ApplicationUIContext } from '../../state/application-ui'
import { useTranslation } from '../../state/i18n'
import { SearchOrder } from '../../types'
import {
  ApplicationSearchParams,
  SortByApplications
} from '../../types/application'

import ApplicationFilters from './ApplicationsFilters'

const PaddedDiv = styled.div`
  padding: ${defaultMargins.m} ${defaultMargins.L};
`

const pageSize = 50

export default React.memo(function ApplicationsPage() {
  const { i18n } = useTranslation()
  const [page, setPage] = useState(1)
  const [sortBy, setSortBy] = useState<SortByApplications>('APPLICATION_TYPE')
  const [sortDirection, setSortDirection] = useState<SearchOrder>('ASC')

  const {
    applicationsResult,
    setApplicationsResult,
    //area,
    units,
    basis,
    type,
    preschoolType,
    status,
    allStatuses,
    dateType,
    distinctions,
    transferApplications,
    voucherApplications,
    startDate,
    endDate,
    debouncedSearchTerms,
    debouncedApplicationSearchFilters,
    setCheckedIds
  } = useContext(ApplicationUIContext)

  const onApplicationsResponse = useCallback(
    (result: Result<Paged<ApplicationSummary>>) => {
      setApplicationsResult(result)

      // ensure current page is within range
      if (result.isSuccess && result.value.pages < page) {
        setPage(result.value.pages)
      }
    },
    [setApplicationsResult] // eslint-disable-line react-hooks/exhaustive-deps
  )

  const reloadApplications = useRestApi(getApplications, onApplicationsResponse)

  const loadApplications = useCallback(() => {
    const params: ApplicationSearchParams = {
      area: debouncedApplicationSearchFilters.area.includes('All')
        ? undefined
        : debouncedApplicationSearchFilters.area.length > 0
        ? debouncedApplicationSearchFilters.area.join(',')
        : undefined,
      units: units.length > 0 ? units.join(',') : undefined,
      basis: basis.length > 0 ? basis.join(',') : undefined,
      type: type,
      preschoolType:
        type === 'PRESCHOOL' && preschoolType.length > 0
          ? preschoolType.join(',')
          : undefined,
      status:
        status === 'ALL'
          ? allStatuses.length > 0
            ? allStatuses.join(',')
            : undefined
          : status,
      dateType: dateType.length > 0 ? dateType.join(',') : undefined,
      distinctions:
        distinctions.length > 0 ? distinctions.join(',') : undefined,
      periodStart:
        startDate && dateType.length > 0 ? startDate.formatIso() : undefined,
      periodEnd:
        endDate && dateType.length > 0 ? endDate.formatIso() : undefined,
      searchTerms: debouncedSearchTerms,
      transferApplications,
      voucherApplications
    }

    reloadApplications(page, pageSize, sortBy, sortDirection, params)
  }, [
    page,
    sortBy,
    sortDirection,
    units,
    basis,
    type,
    preschoolType,
    status,
    allStatuses,
    dateType,
    distinctions,
    transferApplications,
    voucherApplications,
    startDate,
    endDate,
    debouncedSearchTerms,
    debouncedApplicationSearchFilters,
    reloadApplications
  ])

  useEffect(() => {
    loadApplications()
  }, [loadApplications])

  // when changing filters, sorting, etc, set page to 1 and reload
  useEffect(() => {
    setPage(1)
    setCheckedIds([])
  }, [setPage, units, basis, type, preschoolType, status, allStatuses, dateType, distinctions, startDate, endDate, debouncedSearchTerms, debouncedApplicationSearchFilters, setCheckedIds])

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
              {status === 'ALL'
                ? i18n.applications.list.title
                : i18n.application.statuses[status]}
            </H1>
            <ErrorSegment />
          </PaddedDiv>
        )}

        {applicationsResult.isLoading && (
          <PaddedDiv>
            <H1>
              {status === 'ALL'
                ? i18n.applications.list.title
                : i18n.application.statuses[status]}
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
