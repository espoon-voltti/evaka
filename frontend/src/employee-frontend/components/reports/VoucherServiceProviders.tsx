// SPDX-FileCopyrightText: 2017-2020 City of Espoo
//
// SPDX-License-Identifier: LGPL-2.1-or-later

import React, { useEffect, useState } from 'react'
import ReactSelect from 'react-select'
import styled from 'styled-components'
import { Link } from 'react-router-dom'
import { faFile } from '@evaka/lib-icons'
import { Container, ContentArea } from '@evaka/lib-components/layout/Container'
import Loader from '@evaka/lib-components/atoms/Loader'
import Title from '@evaka/lib-components/atoms/Title'
import { Th, Tr, Td, Thead, Tbody } from '@evaka/lib-components/layout/Table'
import { reactSelectStyles } from '../../components/common/Select'
import { useTranslation } from '../../state/i18n'
import { Loading, Result, Success } from '@evaka/lib-common/api'
import { VoucherServiceProviderRow } from '../../types/reports'
import {
  getVoucherServiceProvidersReport,
  VoucherServiceProvidersFilters
} from '../../api/reports'
import InlineButton from '@evaka/lib-components/atoms/buttons/InlineButton'
import ReturnButton from '@evaka/lib-components/atoms/buttons/ReturnButton'
import ReportDownload from '../../components/reports/ReportDownload'
import { formatDate } from '../../utils/date'
import { SelectOptionProps } from '../../components/common/Select'
import { fi } from 'date-fns/locale'
import { CareArea } from '../../types/unit'
import { getAreas } from '../../api/daycare'
import {
  FilterLabel,
  FilterRow,
  TableScrollable
} from '../../components/reports/common'
import { FlexRow } from '../../components/common/styled/containers'
import { formatCents } from '../../utils/money'

const StyledTd = styled(Td)`
  white-space: nowrap;
`

const Wrapper = styled.div`
  width: 100%;
`

function monthOptions(): SelectOptionProps[] {
  const monthOptions = []
  for (let i = 1; i <= 12; i++) {
    monthOptions.push({
      value: i.toString(),
      label: String(fi.localize?.month(i - 1))
    })
  }
  return monthOptions
}

function yearOptions(): SelectOptionProps[] {
  const currentYear = new Date().getFullYear()
  const yearOptions = []
  for (let year = currentYear + 2; year > currentYear - 5; year--) {
    yearOptions.push({
      value: year.toString(),
      label: year.toString()
    })
  }
  return yearOptions
}

function getFilename(year: number, month: number, areaName: string) {
  const time = formatDate(new Date(year, month - 1, 1), 'yyyy-MM')
  return `${time}-${areaName}.csv`.replace(/ /g, '_')
}

function VoucherServiceProviders() {
  const { i18n } = useTranslation()
  const [rows, setRows] = useState<Result<VoucherServiceProviderRow[]>>(
    Success.of([])
  )
  const [areas, setAreas] = useState<CareArea[]>([])
  const [filters, setFilters] = useState<VoucherServiceProvidersFilters>({
    year: new Date().getFullYear(),
    month: new Date().getMonth() + 1,
    areaId: ''
  })

  useEffect(() => {
    void getAreas().then((res) => res.isSuccess && setAreas(res.value))
  }, [])

  useEffect(() => {
    if (filters.areaId == '') return

    setRows(Loading.of())
    void getVoucherServiceProvidersReport(filters).then(setRows)
  }, [filters])

  const months = monthOptions()
  const years = yearOptions()

  const mappedData = rows
    .map((rs) =>
      rs.map(({ unit, childCount, monthlyPaymentSum }) => ({
        unitId: unit.id,
        unitName: unit.name,
        areaName: unit.areaName,
        childCount: childCount,
        sum: formatCents(monthlyPaymentSum)
      }))
    )
    .getOrElse(undefined)

  return (
    <Container>
      <ReturnButton label={i18n.common.goBack} data-qa={'return-button'} />
      <ContentArea opaque>
        <Title size={1}>{i18n.reports.voucherServiceProviders.title}</Title>
        <FilterRow>
          <FilterLabel>{i18n.reports.common.period}</FilterLabel>
          <FlexRow>
            <Wrapper data-qa="select-month">
              <ReactSelect
                options={months}
                value={months.find(
                  (opt) => opt.value === filters.month.toString()
                )}
                onChange={(value) => {
                  if (value && 'value' in value) {
                    const month = parseInt(value.value)
                    setFilters({ ...filters, month })
                  }
                }}
                styles={reactSelectStyles}
              />
            </Wrapper>
            <Wrapper data-qa="select-year">
              <ReactSelect
                options={years}
                value={years.find(
                  (opt) => opt.value === filters.year.toString()
                )}
                onChange={(value) => {
                  if (value && 'value' in value) {
                    const year = parseInt(value.value)
                    setFilters({ ...filters, year })
                  }
                }}
                styles={reactSelectStyles}
              />
            </Wrapper>
          </FlexRow>
        </FilterRow>
        <FilterRow>
          <FilterLabel>{i18n.reports.common.careAreaName}</FilterLabel>
          <Wrapper data-qa="select-area">
            <ReactSelect
              options={[
                ...areas.map((area) => ({ value: area.id, label: area.name }))
              ]}
              onChange={(value) => {
                if (value && 'value' in value) {
                  setFilters({ ...filters, areaId: value.value })
                }
              }}
              styles={reactSelectStyles}
              placeholder={i18n.reports.common.careAreaName}
            />
          </Wrapper>
        </FilterRow>
        {rows.isLoading && <Loader />}
        {rows.isFailure && <span>{i18n.common.loadingFailed}</span>}
        {mappedData && filters.areaId && (
          <>
            <ReportDownload
              data={mappedData}
              headers={[
                { label: 'Palvelualue', key: 'areaName' },
                { label: 'Yksikkö', key: 'unitName' },
                { label: 'PS lasten lkm', key: 'childCount' },
                { label: 'PS summa', key: 'sum' }
              ]}
              filename={getFilename(
                filters.year,
                filters.month,
                areas.find((area) => area.id === filters.areaId)?.name ?? ''
              )}
              dataQa={'download-csv'}
            />
            <TableScrollable>
              <Thead>
                <Tr>
                  <Th>{i18n.reports.common.careAreaName}</Th>
                  <Th>{i18n.reports.common.unitName}</Th>
                  <Th>{i18n.reports.voucherServiceProviders.childCount}</Th>
                  <Th>{i18n.reports.voucherServiceProviders.unitVoucherSum}</Th>
                  <Th>{i18n.reports.voucherServiceProviders.detailsLink}</Th>
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
                      <Link to={`/units/${row.unitId}`}>{row.unitName}</Link>
                    </StyledTd>
                    <StyledTd data-qa={'child-count'}>
                      {row.childCount}
                    </StyledTd>
                    <StyledTd data-qa={'child-sum'}>{row.sum}</StyledTd>
                    <StyledTd>
                      <Link
                        to={`/reports/voucher-service-providers/${row.unitId}`}
                      >
                        <InlineButton
                          icon={faFile}
                          text={i18n.reports.voucherServiceProviders.breakdown}
                          onClick={() => undefined}
                        />
                      </Link>
                    </StyledTd>
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
