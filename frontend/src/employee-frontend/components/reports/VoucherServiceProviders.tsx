// SPDX-FileCopyrightText: 2017-2020 City of Espoo
//
// SPDX-License-Identifier: LGPL-2.1-or-later

import React, { useContext, useEffect, useMemo, useState } from 'react'
import styled from 'styled-components'
import { Link, useLocation } from 'react-router-dom'
import { range } from 'lodash'
import { faLockAlt, faSearch } from 'lib-icons'
import { Container, ContentArea } from 'lib-components/layout/Container'
import Loader from 'lib-components/atoms/Loader'
import { H2 } from 'lib-components/typography'
import { Tbody, Td, Th, Thead, Tr } from 'lib-components/layout/Table'
import { SelectOption } from '../common/Select'
import { useTranslation } from '../../state/i18n'
import { Loading, Result, Success } from 'lib-common/api'
import { ServiceVoucherReport } from 'lib-common/generated/api-types/reports'
import {
  getVoucherServiceProvidersReport,
  VoucherServiceProvidersFilters
} from '../../api/reports'
import ReturnButton from 'lib-components/atoms/buttons/ReturnButton'
import InputField from 'lib-components/atoms/form/InputField'
import ReportDownload from '../../components/reports/ReportDownload'
import { formatDate } from 'lib-common/date'
import { fi } from 'date-fns/locale'
import { CareArea } from '../../types/unit'
import { getAreas } from '../../api/daycare'
import { FilterLabel, FilterRow, TableScrollable } from './common'
import { FlexRow } from '../common/styled/containers'
import { formatCents } from 'lib-common/money'
import { useSyncQueryParams } from 'lib-common/utils/useSyncQueryParams'
import { defaultMargins, Gap } from 'lib-components/white-space'
import { FixedSpaceRow } from 'lib-components/layout/flex-helpers'
import colors from 'lib-customizations/common'
import { FontAwesomeIcon } from '@fortawesome/react-fontawesome'
import HorizontalLine from 'lib-components/atoms/HorizontalLine'
import { UserContext } from '../../state/user'
import Combobox from 'lib-components/atoms/dropdowns/Combobox'

const StyledTd = styled(Td)`
  white-space: nowrap;
`

const FilterWrapper = styled.div`
  width: 100%;
`

const LockedDate = styled(FixedSpaceRow)`
  float: right;
  color: ${colors.greyscale.dark};
  margin-bottom: ${defaultMargins.xs};
`

const monthOptions: SelectOption[] = range(0, 12).map((num) => ({
  value: String(num + 1),
  label: String(fi.localize?.month(num))
}))

const minYear = new Date().getFullYear() - 4
// Max year is next year if current date is in December and current year otherwise
const maxYear = new Date().getFullYear() + (new Date().getMonth() == 11 ? 1 : 0)
const yearOptions: SelectOption[] = range(maxYear, minYear - 1, -1).map(
  (num) => ({
    value: String(num),
    label: String(num)
  })
)

function getFilename(year: number, month: number, areaName: string) {
  const time = formatDate(new Date(year, month - 1, 1), 'yyyy-MM')
  return `${time}-${areaName}.csv`.replace(/ /g, '_')
}

function VoucherServiceProviders() {
  const location = useLocation()
  const { i18n } = useTranslation()
  const { roles } = useContext(UserContext)
  const [report, setReport] = useState<Result<ServiceVoucherReport>>(
    Success.of({ locked: null, rows: [] })
  )
  const allAreasOption = useMemo(
    () => ({
      id: 'all',
      shortName: 'all',
      name: i18n.reports.voucherServiceProviders.filters.allAreas
    }),
    [i18n]
  )
  const [areaOptions, setAreaOptions] = useState<CareArea[]>([allAreasOption])
  const [filters, setFilters] = useState<VoucherServiceProvidersFilters>(() => {
    const { search } = location
    const queryParams = new URLSearchParams(search)
    const year = Number(queryParams.get('year'))
    const month = Number(queryParams.get('month'))
    // intentionally converts empty string into undefined
    const areaId = queryParams.get('areaId') || undefined

    return {
      year:
        year >= minYear && year <= maxYear ? year : new Date().getFullYear(),
      month: month >= 1 && month <= 12 ? month : new Date().getMonth() + 1,
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

  useEffect(() => {
    void getAreas().then((res) =>
      setAreaOptions(
        res
          .map((areas) => [allAreasOption, ...areas])
          .getOrElse([allAreasOption])
      )
    )
  }, [allAreasOption])

  const allAreas = !roles.find((r) =>
    ['ADMIN', 'FINANCE_ADMIN', 'DIRECTOR'].includes(r)
  )
  useEffect(() => {
    if (!allAreas && filters.areaId === undefined) return

    setReport(Loading.of())
    const { areaId, ...otherFilters } = filters
    void getVoucherServiceProvidersReport(
      allAreas || areaId === 'all' ? otherFilters : filters
    ).then(setReport)
  }, [filters, allAreas])

  const months = monthOptions
  const years = yearOptions

  const mappedData = report
    .map((rs) =>
      rs.rows
        .filter(({ unit }) =>
          unit.name.toLowerCase().includes(unitFilter.toLowerCase())
        )
        .map(({ unit, childCount, monthlyPaymentSum }) => ({
          unitId: unit.id,
          unitName: unit.name,
          areaName: unit.areaName,
          childCount: childCount,
          sum: formatCents(monthlyPaymentSum, true)
        }))
    )
    .getOrElse(undefined)

  return (
    <Container>
      <ReturnButton label={i18n.common.goBack} data-qa={'return-button'} />
      <ContentArea opaque>
        <H2>{i18n.reports.voucherServiceProviders.title}</H2>
        <Gap size="m" />
        <FilterRow>
          <FilterLabel>{i18n.reports.common.period}</FilterLabel>
          <FlexRow>
            <FilterWrapper data-qa="select-month">
              <Combobox
                items={months}
                selectedItem={
                  months.find(
                    (opt) => opt.value === filters.month.toString()
                  ) ?? null
                }
                onChange={(value) => {
                  if (value) {
                    const month = parseInt(value.value)
                    setFilters({ ...filters, month })
                  }
                }}
                getItemLabel={(item) => item.label}
              />
            </FilterWrapper>
            <FilterWrapper data-qa="select-year">
              <Combobox
                items={years}
                selectedItem={
                  years.find((opt) => opt.value === filters.year.toString()) ??
                  null
                }
                onChange={(value) => {
                  if (value) {
                    const year = parseInt(value.value)
                    setFilters({ ...filters, year })
                  }
                }}
                getItemLabel={(item) => item.label}
              />
            </FilterWrapper>
          </FlexRow>
        </FilterRow>
        {!allAreas ? (
          <>
            <FilterRow>
              <FilterLabel>{i18n.reports.common.careAreaName}</FilterLabel>
              <FilterWrapper data-qa="select-area">
                <Combobox
                  items={areaOptions}
                  selectedItem={
                    areaOptions.find(({ id }) => id === filters.areaId) ?? null
                  }
                  onChange={(area) =>
                    setFilters({ ...filters, areaId: area?.id })
                  }
                  placeholder={i18n.reports.common.careAreaName}
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

        {report.isSuccess && report.value.locked && (
          <LockedDate spacing="xs" alignItems="center">
            <FontAwesomeIcon icon={faLockAlt} />
            <span>
              {`${
                i18n.reports.voucherServiceProviders.locked
              }: ${report.value.locked.format()}`}
            </span>
          </LockedDate>
        )}

        <HorizontalLine slim />

        {report.isLoading && <Loader />}
        {report.isFailure && <span>{i18n.common.loadingFailed}</span>}
        {mappedData && (
          <>
            <ReportDownload
              data={mappedData}
              headers={[
                { label: i18n.reports.common.careAreaName, key: 'areaName' },
                { label: i18n.reports.common.unitName, key: 'unitName' },
                {
                  label: i18n.reports.voucherServiceProviders.childCount,
                  key: 'childCount'
                },
                {
                  label: i18n.reports.voucherServiceProviders.unitVoucherSum,
                  key: 'sum'
                }
              ]}
              filename={getFilename(
                filters.year,
                filters.month,
                areaOptions.find((area) => area.id === filters.areaId)?.name ??
                  ''
              )}
              data-qa={'download-csv'}
            />
            <TableScrollable>
              <Thead>
                <Tr>
                  <Th>{i18n.reports.common.careAreaName}</Th>
                  <Th>{i18n.reports.common.unitName}</Th>
                  <Th>{i18n.reports.voucherServiceProviders.childCount}</Th>
                  <Th>{i18n.reports.voucherServiceProviders.unitVoucherSum}</Th>
                </Tr>
              </Thead>
              <Tbody>
                {mappedData.map((row) => (
                  <Tr
                    key={row.unitId}
                    data-qa={row.unitId}
                    className={'reportRow'}
                  >
                    <StyledTd>{row.areaName}</StyledTd>
                    <StyledTd>
                      <Link
                        to={`/reports/voucher-service-providers/${row.unitId}?${query}`}
                      >
                        {row.unitName}
                      </Link>
                    </StyledTd>
                    <StyledTd data-qa={'child-count'}>
                      {row.childCount}
                    </StyledTd>
                    <StyledTd data-qa={'child-sum'}>{row.sum}</StyledTd>
                  </Tr>
                ))}
              </Tbody>
            </TableScrollable>
          </>
        )}
      </ContentArea>
    </Container>
  )
}

export default VoucherServiceProviders
