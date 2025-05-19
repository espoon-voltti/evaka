// SPDX-FileCopyrightText: 2017-2022 City of Espoo
//
// SPDX-License-Identifier: LGPL-2.1-or-later

import { FontAwesomeIcon } from '@fortawesome/react-fontawesome'
import range from 'lodash/range'
import React, { useContext, useMemo, useState } from 'react'
import { Link, useLocation } from 'react-router'
import styled from 'styled-components'

import { combine } from 'lib-common/api'
import HelsinkiDateTime from 'lib-common/helsinki-date-time'
import LocalDate from 'lib-common/local-date'
import { formatCents } from 'lib-common/money'
import { useQueryResult } from 'lib-common/query'
import type { UUID } from 'lib-common/types'
import { useSyncQueryParams } from 'lib-common/utils/useSyncQueryParams'
import HorizontalLine from 'lib-components/atoms/HorizontalLine'
import ReturnButton from 'lib-components/atoms/buttons/ReturnButton'
import Select from 'lib-components/atoms/dropdowns/Select'
import InputField from 'lib-components/atoms/form/InputField'
import { Container, ContentArea } from 'lib-components/layout/Container'
import { Tbody, Td, Th, Thead, Tr } from 'lib-components/layout/Table'
import { FixedSpaceRow } from 'lib-components/layout/flex-helpers'
import { H2 } from 'lib-components/typography'
import { defaultMargins, Gap } from 'lib-components/white-space'
import colors from 'lib-customizations/common'
import { featureFlags } from 'lib-customizations/employee'
import { faLockAlt, faSearch } from 'lib-icons'

import ReportDownload from '../../components/reports/ReportDownload'
import { areasQuery } from '../../queries'
import { useTranslation } from '../../state/i18n'
import { UserContext } from '../../state/user'
import { renderResult } from '../async-rendering'
import { FlexRow } from '../common/styled/containers'

import { FilterLabel, FilterRow, TableScrollable } from './common'
import { voucherServiceProvidersReportQuery } from './queries'

interface VoucherServiceProvidersFilters {
  year: number
  month: number
  areaId?: UUID
}

const StyledTd = styled(Td)`
  white-space: nowrap;
`

const FilterWrapper = styled.div`
  width: 100%;
`

const LockedDate = styled(FixedSpaceRow)`
  float: right;
  color: ${colors.grayscale.g70};
  margin-bottom: ${defaultMargins.xs};
`

const monthOptions = range(1, 13)

const now = HelsinkiDateTime.now()
const minYear = now.year - 4
// Max year is next year if current date is in December and current year otherwise
const maxYear = now.year + (now.month === 12 ? 1 : 0)
const yearOptions = range(maxYear, minYear - 1, -1)

function getFilename(year: number, month: number, areaName: string) {
  const time = LocalDate.of(year, month, 1).formatExotic('yyyy-MM')
  return `${time}-${areaName}.csv`.replace(/ /g, '_')
}

export default React.memo(function VoucherServiceProviders() {
  const location = useLocation()
  const { i18n } = useTranslation()
  const { roles } = useContext(UserContext)
  const areas = useQueryResult(areasQuery())
  const allAreasOption = useMemo(
    () => ({
      id: 'all',
      shortName: 'all',
      name: i18n.reports.voucherServiceProviders.filters.allAreas
    }),
    [i18n]
  )
  const areaOptions = useMemo(
    () =>
      areas
        .map((areas) => [allAreasOption, ...areas])
        .getOrElse([allAreasOption]),
    [allAreasOption, areas]
  )
  const [filters, setFilters] = useState<VoucherServiceProvidersFilters>(() => {
    const { search } = location
    const queryParams = new URLSearchParams(search)
    const year = Number(queryParams.get('year'))
    const month = Number(queryParams.get('month'))
    // intentionally converts empty string into undefined
    const areaId = queryParams.get('areaId') || undefined

    const now = HelsinkiDateTime.now()

    return {
      year: year >= minYear && year <= maxYear ? year : now.year,
      month: month >= 1 && month <= 12 ? month : now.month,
      areaId
    }
  })
  const [unitFilter, setUnitFilter] = useState<string>(() => {
    const { search } = location
    const queryParams = new URLSearchParams(search)
    return queryParams.get('unit') ?? ''
  })

  const params = {
    year: filters.year.toString(),
    month: filters.month.toString(),
    areaId: filters.areaId ?? '',
    unit: unitFilter
  }
  useSyncQueryParams(params)
  const query = new URLSearchParams(params).toString()

  const allAreas = !roles.find((r) =>
    ['ADMIN', 'FINANCE_ADMIN', 'DIRECTOR'].includes(r)
  )

  const { areaId, ...otherFilters } = filters
  const report = useQueryResult(
    voucherServiceProvidersReportQuery(
      allAreas || areaId === 'all' ? otherFilters : filters
    )
  )

  const mappedData = useMemo(
    () =>
      report.map((rs) =>
        rs.rows
          .filter(({ unit }) =>
            unit.name.toLowerCase().includes(unitFilter.toLowerCase())
          )
          .map(
            ({
              unit,
              childCount,
              monthlyPaymentSumBeforeAssistanceNeed,
              monthlyPaymentSum
            }) => ({
              unitId: unit.id,
              unitName: unit.name,
              areaName: unit.areaName,
              childCount: childCount,
              sumBeforeAssistanceNeed: formatCents(
                monthlyPaymentSumBeforeAssistanceNeed,
                true
              ),
              assistanceNeedSum: formatCents(
                monthlyPaymentSum - monthlyPaymentSumBeforeAssistanceNeed,
                true
              ),
              sum: formatCents(monthlyPaymentSum, true)
            })
          )
          .sort((l, r) => l.unitName.localeCompare(r.unitName, 'fi'))
      ),
    [report, unitFilter]
  )

  return (
    <Container>
      <ReturnButton label={i18n.common.goBack} data-qa="return-button" />
      <ContentArea opaque>
        <H2>{i18n.reports.voucherServiceProviders.title}</H2>
        <Gap size="m" />
        <FilterRow>
          <FilterLabel>{i18n.reports.common.period}</FilterLabel>
          <FlexRow>
            <FilterWrapper data-qa="select-month">
              <Select
                items={monthOptions}
                selectedItem={filters.month}
                onChange={(month) => {
                  if (month !== null) {
                    setFilters({ ...filters, month })
                  }
                }}
                getItemLabel={(month) => i18n.datePicker.months[month - 1]}
              />
            </FilterWrapper>
            <FilterWrapper data-qa="select-year">
              <Select
                items={yearOptions}
                selectedItem={filters.year}
                onChange={(year) => {
                  if (year !== null) {
                    setFilters({ ...filters, year })
                  }
                }}
              />
            </FilterWrapper>
          </FlexRow>
        </FilterRow>
        {!allAreas ? (
          <>
            <FilterRow>
              <FilterLabel>{i18n.reports.common.careAreaName}</FilterLabel>
              <FilterWrapper data-qa="select-area">
                <Select
                  items={areaOptions}
                  selectedItem={
                    areaOptions.find(({ id }) => id === filters.areaId) ?? null
                  }
                  onChange={(area) =>
                    setFilters({ ...filters, areaId: area?.id })
                  }
                  getItemValue={({ id }) => id}
                  getItemLabel={({ name }) => name}
                />
              </FilterWrapper>
            </FilterRow>
            <FilterRow>
              <FilterLabel>{i18n.reports.common.unitName}</FilterLabel>
              <FilterWrapper data-qa="unit-name-input">
                <InputField
                  value={unitFilter}
                  onChange={(value) => setUnitFilter(value)}
                  placeholder={
                    i18n.reports.voucherServiceProviders.filters.unitPlaceholder
                  }
                  icon={faSearch}
                />
              </FilterWrapper>
            </FilterRow>
          </>
        ) : null}

        {renderResult(combine(report, mappedData), ([report, mappedData]) => (
          <>
            {report.locked && (
              <LockedDate spacing="xs" alignItems="center">
                <FontAwesomeIcon icon={faLockAlt} />
                <span>
                  {`${
                    i18n.reports.voucherServiceProviders.locked
                  }: ${report.locked.format()}`}
                </span>
              </LockedDate>
            )}

            <HorizontalLine slim />

            <ReportDownload
              data={mappedData}
              columns={[
                {
                  label: i18n.reports.common.careAreaName,
                  value: (row) => row.areaName
                },
                {
                  label: i18n.reports.common.unitName,
                  value: (row) => row.unitName
                },
                {
                  label: i18n.reports.voucherServiceProviders.childCount,
                  value: (row) => row.childCount
                },
                {
                  label:
                    i18n.reports.voucherServiceProviders
                      .sumBeforeAssistanceNeed,
                  value: (row) => row.sumBeforeAssistanceNeed,
                  exclude: !featureFlags.voucherValueSeparation
                },
                {
                  label: i18n.reports.voucherServiceProviders.assistanceNeedSum,
                  value: (row) => row.assistanceNeedSum,
                  exclude: !featureFlags.voucherValueSeparation
                },
                {
                  label: i18n.reports.voucherServiceProviders.unitVoucherSum,
                  value: (row) => row.sum
                }
              ]}
              filename={getFilename(
                filters.year,
                filters.month,
                areaOptions.find((area) => area.id === filters.areaId)?.name ??
                  ''
              )}
              data-qa="download-csv"
            />
            <TableScrollable>
              <Thead>
                <Tr>
                  <Th>{i18n.reports.common.careAreaName}</Th>
                  <Th>{i18n.reports.common.unitName}</Th>
                  <Th>{i18n.reports.voucherServiceProviders.childCount}</Th>
                  {featureFlags.voucherValueSeparation && (
                    <>
                      <Th>
                        {
                          i18n.reports.voucherServiceProviders
                            .sumBeforeAssistanceNeed
                        }
                      </Th>
                      <Th>
                        {i18n.reports.voucherServiceProviders.assistanceNeedSum}
                      </Th>
                    </>
                  )}
                  <Th>{i18n.reports.voucherServiceProviders.unitVoucherSum}</Th>
                </Tr>
              </Thead>
              <Tbody>
                {mappedData.map((row) => (
                  <Tr
                    key={row.unitId}
                    data-qa={row.unitId}
                    className="reportRow"
                  >
                    <StyledTd>{row.areaName}</StyledTd>
                    <StyledTd>
                      <Link
                        data-qa="unit-link"
                        to={`/reports/voucher-service-providers/${row.unitId}?${query}`}
                      >
                        {row.unitName}
                      </Link>
                    </StyledTd>
                    <StyledTd data-qa="child-count">{row.childCount}</StyledTd>
                    {featureFlags.voucherValueSeparation && (
                      <>
                        <StyledTd>{row.sumBeforeAssistanceNeed}</StyledTd>
                        <StyledTd>{row.assistanceNeedSum}</StyledTd>
                      </>
                    )}
                    <StyledTd data-qa="child-sum">{row.sum}</StyledTd>
                  </Tr>
                ))}
              </Tbody>
            </TableScrollable>
          </>
        ))}
      </ContentArea>
    </Container>
  )
})
