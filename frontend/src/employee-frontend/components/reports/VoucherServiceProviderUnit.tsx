// SPDX-FileCopyrightText: 2017-2022 City of Espoo
//
// SPDX-License-Identifier: LGPL-2.1-or-later

import { FontAwesomeIcon } from '@fortawesome/react-fontawesome'
import range from 'lodash/range'
import sortBy from 'lodash/sortBy'
import React, { useEffect, useMemo, useState } from 'react'
import { Link, useLocation } from 'react-router-dom'
import styled from 'styled-components'

import { Loading, Result } from 'lib-common/api'
import {
  VoucherReportRowType,
  ServiceVoucherUnitReport,
  ServiceVoucherValueRow
} from 'lib-common/generated/api-types/reports'
import HelsinkiDateTime from 'lib-common/helsinki-date-time'
import { formatCents } from 'lib-common/money'
import { UUID } from 'lib-common/types'
import useNonNullableParams from 'lib-common/useNonNullableParams'
import { formatDecimal } from 'lib-common/utils/number'
import { useSyncQueryParams } from 'lib-common/utils/useSyncQueryParams'
import HorizontalLine from 'lib-components/atoms/HorizontalLine'
import Loader from 'lib-components/atoms/Loader'
import Tooltip, { TooltipWithoutAnchor } from 'lib-components/atoms/Tooltip'
import ReturnButton from 'lib-components/atoms/buttons/ReturnButton'
import Select from 'lib-components/atoms/dropdowns/Select'
import { Container, ContentArea } from 'lib-components/layout/Container'
import {
  SortableTh,
  Tbody,
  Td,
  Th,
  Thead,
  Tr
} from 'lib-components/layout/Table'
import {
  FixedSpaceColumn,
  FixedSpaceRow
} from 'lib-components/layout/flex-helpers'
import { H2, H3 } from 'lib-components/typography'
import { defaultMargins, Gap } from 'lib-components/white-space'
import colors from 'lib-customizations/common'
import { faHome, faLockAlt } from 'lib-icons'

import {
  getVoucherServiceProviderUnitReport,
  VoucherProviderChildrenReportFilters as VoucherServiceProviderUnitFilters
} from '../../api/reports'
import ReportDownload from '../../components/reports/ReportDownload'
import { useTranslation } from '../../state/i18n'
import { formatName } from '../../utils'
import { AgeIndicatorChip } from '../common/AgeIndicatorChip'

import { FilterLabel, FilterRow, TableScrollable } from './common'

const FilterWrapper = styled.div`
  width: 400px;
  margin-right: ${defaultMargins.s};
`

const LockedDate = styled(FixedSpaceRow)`
  float: right;
  color: ${colors.grayscale.g70};
  margin-bottom: ${defaultMargins.xs};
`

const SumRow = styled.div`
  width: 100%;
  display: flex;
  justify-content: space-between;
  align-items: center;
`

const BlankTh = styled(Th)`
  border-color: transparent;
  padding: 0;
`

const BlankTd = styled(Td)`
  border: 1px solid transparent;
  padding: 0;
  position: relative;
  width: 6px;

  &:not(:hover) {
    .tooltip {
      display: none;
    }
  }
`

const TypeIndicator = styled.div<{ type: VoucherReportRowType | 'NEW' }>`
  position: absolute;
  top: -2px;
  bottom: 1px;
  left: -1px;
  right: -1px;
  background-color: ${(p) =>
    p.type === 'REFUND'
      ? colors.status.warning
      : p.type === 'CORRECTION'
        ? colors.accents.a5orangeLight
        : p.type === 'NEW'
          ? colors.main.m2
          : colors.grayscale.g0};
`

const StyledTh = styled(Th)`
  white-space: nowrap;
`

const monthOptions = range(1, 13)

const now = HelsinkiDateTime.now()
const minYear = now.year - 4
const maxYear = now.year
const yearOptions = range(maxYear, minYear - 1, -1)

export default React.memo(function VoucherServiceProviderUnit() {
  const location = useLocation()
  const { i18n } = useTranslation()
  const { unitId } = useNonNullableParams<{ unitId: UUID }>()
  const [report, setReport] = useState<Result<ServiceVoucherUnitReport>>(
    Loading.of()
  )
  const [sort, setSort] = useState<'child' | 'group'>('child')

  const sortOnClick = (prop: 'child' | 'group') => () => {
    if (sort !== prop) {
      setSort(prop)
    }
  }

  const sortedReport = report.map((rs) =>
    sort === 'group'
      ? { ...rs, rows: sortBy(rs.rows, 'childGroupName') }
      : { ...rs, rows: sortBy(rs.rows, ['childLastName', 'childFirstName']) }
  )

  const [unitName, setUnitName] = useState<string>('')
  const [filters, setFilters] = useState<VoucherServiceProviderUnitFilters>(
    () => {
      const { search } = location
      const queryParams = new URLSearchParams(search)
      const year = Number(queryParams.get('year'))
      const month = Number(queryParams.get('month'))
      const now = HelsinkiDateTime.now()

      return {
        year: year >= minYear && year <= maxYear ? year : now.year,
        month: month >= 1 && month <= 12 ? month : now.month
      }
    }
  )

  const memoizedFilters = useMemo(
    () => ({
      year: filters.year.toString(),
      month: filters.month.toString()
    }),
    [filters]
  )
  useSyncQueryParams(memoizedFilters)

  useEffect(() => {
    setReport(Loading.of())
    void getVoucherServiceProviderUnitReport(unitId, filters).then((res) => {
      setReport(res)
      if (res.isSuccess && res.value.rows.length > 0) {
        setUnitName(res.value.rows[0].unitName)
      }
    })
  }, [filters]) // eslint-disable-line react-hooks/exhaustive-deps

  return (
    <Container>
      <ReturnButton label={i18n.common.goBack} />
      <ContentArea opaque>
        <TitleContainer>
          <H2 fitted>{unitName}</H2>
          <LinkInCaps to={`/units/${unitId}`}>
            <FontAwesomeIcon icon={faHome} />{' '}
            {i18n.reports.voucherServiceProviderUnit.unitPageLink}
          </LinkInCaps>
        </TitleContainer>
        <H3 noMargin>{i18n.reports.voucherServiceProviderUnit.title}</H3>
        <Gap size="L" />
        <FilterRow>
          <FilterLabel>
            {i18n.reports.voucherServiceProviderUnit.month}
          </FilterLabel>
          <FilterWrapper>
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
        </FilterRow>

        {sortedReport.isSuccess && sortedReport.value.locked && (
          <LockedDate spacing="xs" alignItems="center">
            <FontAwesomeIcon icon={faLockAlt} />
            <span>
              {`${
                i18n.reports.voucherServiceProviders.locked
              }: ${sortedReport.value.locked.format()}`}
            </span>
          </LockedDate>
        )}

        <HorizontalLine slim />

        {sortedReport.isLoading && <Loader />}
        {sortedReport.isFailure && <span>{i18n.common.loadingFailed}</span>}
        {sortedReport.isSuccess && (
          <>
            <SumRow>
              <FixedSpaceRow>
                <strong>{i18n.reports.voucherServiceProviderUnit.total}</strong>
                <strong>
                  {formatCents(sortedReport.value.voucherTotal, true)} €
                </strong>
              </FixedSpaceRow>

              <ReportDownload
                data={[
                  ...sortedReport.value.rows.map((r) => ({
                    ...r,
                    start: r.realizedPeriod.start.format(),
                    end: r.realizedPeriod.end.format(),
                    note:
                      r.type === 'REFUND'
                        ? 'Hyvitys'
                        : r.type === 'CORRECTION'
                          ? 'Korjaus'
                          : '',
                    serviceNeedDescription: r.serviceNeedDescription,
                    assistanceNeedCapacityFactor: formatDecimal(
                      r.assistanceNeedCoefficient
                    ),
                    serviceVoucherValue: formatCents(
                      r.serviceVoucherValue,
                      true
                    ),
                    serviceVoucherFinalCoPayment: formatCents(
                      r.serviceVoucherFinalCoPayment,
                      true
                    ),
                    realizedAmount: formatCents(r.realizedAmount, true)
                  })),
                  {
                    childFirstName: 'Yhteensä',
                    childLastName: null,
                    childGroupName: null,
                    note: null,
                    start: null,
                    end: null,
                    numberOfDays: null,
                    serviceNeedDescription: null,
                    assistanceNeedCapacityFactor: null,
                    serviceVoucherValue: null,
                    serviceVoucherFinalCoPayment: null,
                    realizedAmount: formatCents(
                      sortedReport.value.voucherTotal,
                      true
                    )
                  }
                ]}
                headers={[
                  {
                    label:
                      i18n.reports.voucherServiceProviderUnit.childLastName,
                    key: 'childLastName'
                  },
                  {
                    label:
                      i18n.reports.voucherServiceProviderUnit.childFirstName,
                    key: 'childFirstName'
                  },
                  {
                    label: i18n.reports.common.groupName,
                    key: 'childGroupName'
                  },
                  {
                    label: i18n.reports.voucherServiceProviderUnit.note,
                    key: 'note'
                  },
                  {
                    label: i18n.reports.voucherServiceProviderUnit.start,
                    key: 'start'
                  },
                  {
                    label: i18n.reports.voucherServiceProviderUnit.end,
                    key: 'end'
                  },
                  {
                    label: i18n.reports.voucherServiceProviderUnit.numberOfDays,
                    key: 'numberOfDays'
                  },
                  {
                    label: i18n.reports.voucherServiceProviderUnit.serviceNeed,
                    key: 'serviceNeedDescription'
                  },
                  {
                    label:
                      i18n.reports.voucherServiceProviderUnit.assistanceNeed,
                    key: 'assistanceNeedCapacityFactor'
                  },
                  {
                    label:
                      i18n.reports.voucherServiceProviderUnit
                        .serviceVoucherValue,
                    key: 'serviceVoucherValue'
                  },
                  {
                    label:
                      i18n.reports.voucherServiceProviderUnit
                        .serviceVoucherFinalCoPayment,
                    key: 'serviceVoucherFinalCoPayment'
                  },
                  {
                    label:
                      i18n.reports.voucherServiceProviderUnit
                        .serviceVoucherRealizedValue,
                    key: 'realizedAmount'
                  }
                ]}
                filename={`${filters.year}-${filters.month} palveluseteliraportti ${unitName}.csv`}
              />
            </SumRow>

            <TableScrollable>
              <Thead>
                <Tr>
                  <BlankTh />
                  <SortableTh
                    sorted={sort === 'child' ? 'ASC' : undefined}
                    onClick={sortOnClick('child')}
                  >
                    {i18n.reports.voucherServiceProviderUnit.child}
                  </SortableTh>
                  <SortableTh
                    sorted={sort === 'group' ? 'ASC' : undefined}
                    onClick={sortOnClick('group')}
                  >
                    {i18n.reports.common.groupName}
                  </SortableTh>
                  <StyledTh>
                    {i18n.reports.voucherServiceProviderUnit.numberOfDays}
                  </StyledTh>
                  <Th>{i18n.reports.voucherServiceProviderUnit.serviceNeed}</Th>
                  <StyledTh>
                    {i18n.reports.voucherServiceProviderUnit.assistanceNeed}
                  </StyledTh>
                  <StyledTh>
                    {
                      i18n.reports.voucherServiceProviderUnit
                        .serviceVoucherValue
                    }
                  </StyledTh>
                  <StyledTh>
                    {
                      i18n.reports.voucherServiceProviderUnit
                        .serviceVoucherFinalCoPayment
                    }
                  </StyledTh>
                  <StyledTh>
                    {
                      i18n.reports.voucherServiceProviderUnit
                        .serviceVoucherRealizedValue
                    }
                  </StyledTh>
                </Tr>
              </Thead>
              <Tbody>
                {sortedReport.value.rows.map((row: ServiceVoucherValueRow) => {
                  const rowType =
                    row.isNew && row.type === 'ORIGINAL' ? 'NEW' : row.type

                  return (
                    <Tr
                      data-qa="child-row"
                      key={`${
                        row.serviceVoucherDecisionId
                      }:${row.realizedPeriod.start.formatIso()}`}
                    >
                      <BlankTd>
                        <TypeIndicator type={rowType} />
                        {rowType !== 'ORIGINAL' ? (
                          <TooltipWithoutAnchor
                            tooltip={
                              i18n.reports.voucherServiceProviderUnit.type[
                                rowType
                              ]
                            }
                            position="right"
                          />
                        ) : null}
                      </BlankTd>
                      <Td>
                        <FixedSpaceColumn spacing="xs">
                          <Link
                            data-qa="child-name"
                            to={`/child-information/${row.childId}`}
                          >
                            {formatName(
                              row.childFirstName,
                              row.childLastName,
                              i18n,
                              true
                            )}
                          </Link>
                          <FixedSpaceRow spacing="xs" alignItems="center">
                            <AgeIndicatorChip
                              age={row.realizedPeriod.start.differenceInYears(
                                row.childDateOfBirth
                              )}
                            />
                            <span>{row.childDateOfBirth.format()}</span>
                          </FixedSpaceRow>
                        </FixedSpaceColumn>
                      </Td>
                      <Td>{row.childGroupName}</Td>
                      <Td>
                        <Tooltip
                          tooltip={<div>{row.numberOfDays}</div>}
                          position="right"
                        >
                          {row.realizedPeriod.formatCompact()}
                        </Tooltip>
                      </Td>
                      <Td>{row.serviceNeedDescription}</Td>
                      <Td>{formatDecimal(row.assistanceNeedCoefficient)}</Td>
                      <Td data-qa="voucher-value">
                        {formatCents(row.serviceVoucherValue, true)}
                      </Td>
                      <Td data-qa="co-payment">
                        {formatCents(row.serviceVoucherFinalCoPayment, true)}
                      </Td>
                      <Td data-qa="realized-amount">
                        {formatCents(row.realizedAmount, true)}
                      </Td>
                    </Tr>
                  )
                })}
              </Tbody>
            </TableScrollable>
          </>
        )}
      </ContentArea>
    </Container>
  )
})

const TitleContainer = styled.div`
  display: flex;
  flex-direction: row;
  justify-content: space-between;
  align-items: flex-start;
`

const LinkInCaps = styled(Link)`
  text-transform: uppercase;
`
