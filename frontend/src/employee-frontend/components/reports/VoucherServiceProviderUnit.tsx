// SPDX-FileCopyrightText: 2017-2022 City of Espoo
//
// SPDX-License-Identifier: LGPL-2.1-or-later

import { FontAwesomeIcon } from '@fortawesome/react-fontawesome'
import orderBy from 'lodash/orderBy'
import range from 'lodash/range'
import React, { useMemo, useState } from 'react'
import styled from 'styled-components'
import { Link, useSearchParams } from 'wouter'

import type {
  VoucherReportRowType,
  ServiceVoucherValueRow
} from 'lib-common/generated/api-types/reports'
import type { DaycareId } from 'lib-common/generated/api-types/shared'
import HelsinkiDateTime from 'lib-common/helsinki-date-time'
import { formatCents } from 'lib-common/money'
import { useQueryResult } from 'lib-common/query'
import type { Arg0 } from 'lib-common/types'
import { useIdRouteParam } from 'lib-common/useRouteParams'
import { formatDecimal } from 'lib-common/utils/number'
import { useSyncQueryParams } from 'lib-common/utils/useSyncQueryParams'
import HorizontalLine from 'lib-components/atoms/HorizontalLine'
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
import { PersonName } from 'lib-components/molecules/PersonNames'
import { H2, H3 } from 'lib-components/typography'
import { defaultMargins, Gap } from 'lib-components/white-space'
import colors from 'lib-customizations/common'
import { featureFlags } from 'lib-customizations/employee'
import { faHome, faLockAlt } from 'lib-icons'

import type { getServiceVoucherReportForUnit } from '../../generated/api-clients/reports'
import { useTranslation } from '../../state/i18n'
import { renderResult } from '../async-rendering'
import { AgeIndicatorChip } from '../common/AgeIndicatorChip'

import ReportDownload from './ReportDownload'
import { FilterLabel, FilterRow, TableScrollable } from './common'
import { serviceVoucherReportForUnitQuery } from './queries'

type VoucherServiceProviderUnitFilters = Omit<
  Arg0<typeof getServiceVoucherReportForUnit>,
  'unitId'
>

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

type SortOption = 'child' | 'group' | 'assistanceNeedCoefficient'
const sortOptions: Record<
  SortOption,
  {
    columns: (keyof ServiceVoucherValueRow)[]
    directions: ('asc' | 'desc')[]
  }
> = {
  child: {
    columns: ['childLastName', 'childFirstName'],
    directions: ['asc', 'asc']
  },
  group: {
    columns: ['childGroupName', 'childLastName', 'childFirstName'],
    directions: ['asc', 'asc', 'asc']
  },
  assistanceNeedCoefficient: {
    columns: ['assistanceNeedCoefficient', 'childLastName', 'childFirstName'],
    directions: ['desc', 'asc', 'asc']
  }
}

export default React.memo(function VoucherServiceProviderUnit() {
  const [queryParams] = useSearchParams()
  const { i18n } = useTranslation()
  const unitId = useIdRouteParam<DaycareId>('unitId')
  const [sort, setSort] = useState<SortOption>('child')

  const sortOnClick = (prop: SortOption) => () => {
    if (sort !== prop) {
      setSort(prop)
    }
  }

  const [filters, setFilters] = useState<VoucherServiceProviderUnitFilters>(
    () => {
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

  const report = useQueryResult(
    serviceVoucherReportForUnitQuery({ unitId, ...filters })
  )
  const sortedReport = useMemo(() => {
    const { columns, directions } = sortOptions[sort]
    return report.map((rs) => ({
      ...rs,
      rows: orderBy(rs.rows, columns, directions)
    }))
  }, [report, sort])
  const unitName = useMemo(
    () => report.map((r) => r.rows[0].unitName).getOrElse(''),
    [report]
  )

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

        {renderResult(sortedReport, (sortedReport) => (
          <>
            <SumRow>
              <FixedSpaceRow>
                <strong>{i18n.reports.voucherServiceProviderUnit.total}</strong>
                <strong>
                  {formatCents(sortedReport.voucherTotal, true)} €
                </strong>
              </FixedSpaceRow>

              <ReportDownload
                data={[
                  ...sortedReport.rows.map((r) => ({
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
                    realizedAmountBeforeAssistanceNeed: formatCents(
                      r.realizedAmountBeforeAssistanceNeed,
                      true
                    ),
                    realizedAssistanceNeedAmount: formatCents(
                      r.realizedAmount - r.realizedAmountBeforeAssistanceNeed,
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
                    realizedAmountBeforeAssistanceNeed: null,
                    realizedAssistanceNeedAmount: null,
                    realizedAmount: formatCents(sortedReport.voucherTotal, true)
                  }
                ]}
                columns={[
                  {
                    label:
                      i18n.reports.voucherServiceProviderUnit.childLastName,
                    value: (row) => row.childLastName
                  },
                  {
                    label:
                      i18n.reports.voucherServiceProviderUnit.childFirstName,
                    value: (row) => row.childFirstName
                  },
                  {
                    label: i18n.reports.common.groupName,
                    value: (row) => row.childGroupName
                  },
                  {
                    label: i18n.reports.voucherServiceProviderUnit.note,
                    value: (row) => row.note
                  },
                  {
                    label: i18n.reports.voucherServiceProviderUnit.start,
                    value: (row) => row.start
                  },
                  {
                    label: i18n.reports.voucherServiceProviderUnit.end,
                    value: (row) => row.end
                  },
                  {
                    label: i18n.reports.voucherServiceProviderUnit.numberOfDays,
                    value: (row) => row.numberOfDays
                  },
                  {
                    label: i18n.reports.voucherServiceProviderUnit.serviceNeed,
                    value: (row) => row.serviceNeedDescription
                  },
                  {
                    label:
                      i18n.reports.voucherServiceProviderUnit.assistanceNeed,
                    value: (row) => row.assistanceNeedCapacityFactor
                  },
                  {
                    label:
                      i18n.reports.voucherServiceProviderUnit
                        .serviceVoucherValue,
                    value: (row) => row.serviceVoucherValue
                  },
                  {
                    label:
                      i18n.reports.voucherServiceProviderUnit
                        .serviceVoucherFinalCoPayment,
                    value: (row) => row.serviceVoucherFinalCoPayment
                  },
                  {
                    label:
                      i18n.reports.voucherServiceProviderUnit
                        .serviceVoucherRealizedValueBeforeAssistanceNeed,
                    value: (row) => row.realizedAmountBeforeAssistanceNeed,
                    exclude: !featureFlags.voucherValueSeparation
                  },
                  {
                    label:
                      i18n.reports.voucherServiceProviderUnit
                        .serviceVoucherRealizedAssistanceNeedValue,
                    value: (row) => row.realizedAssistanceNeedAmount,
                    exclude: !featureFlags.voucherValueSeparation
                  },
                  {
                    label:
                      i18n.reports.voucherServiceProviderUnit
                        .serviceVoucherRealizedValue,
                    value: (row) => row.realizedAmount
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
                  <SortableTh
                    sorted={
                      sort === 'assistanceNeedCoefficient' ? 'DESC' : undefined
                    }
                    onClick={sortOnClick('assistanceNeedCoefficient')}
                  >
                    {i18n.reports.voucherServiceProviderUnit.assistanceNeed}
                  </SortableTh>
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
                  {featureFlags.voucherValueSeparation && (
                    <>
                      <StyledTh>
                        {
                          i18n.reports.voucherServiceProviderUnit
                            .serviceVoucherRealizedValueBeforeAssistanceNeed
                        }
                      </StyledTh>
                      <StyledTh>
                        {
                          i18n.reports.voucherServiceProviderUnit
                            .serviceVoucherRealizedAssistanceNeedValue
                        }
                      </StyledTh>
                    </>
                  )}
                  <StyledTh>
                    {
                      i18n.reports.voucherServiceProviderUnit
                        .serviceVoucherRealizedValue
                    }
                  </StyledTh>
                </Tr>
              </Thead>
              <Tbody>
                {sortedReport.rows.map((row: ServiceVoucherValueRow) => {
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
                            <PersonName
                              person={{
                                firstName: row.childFirstName,
                                lastName: row.childLastName
                              }}
                              format="Last First"
                            />
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
                      {featureFlags.voucherValueSeparation && (
                        <>
                          <Td>
                            {formatCents(
                              row.realizedAmountBeforeAssistanceNeed,
                              true
                            )}
                          </Td>
                          <Td>
                            {formatCents(
                              row.realizedAmount -
                                row.realizedAmountBeforeAssistanceNeed,
                              true
                            )}
                          </Td>
                        </>
                      )}
                      <Td data-qa="realized-amount">
                        {formatCents(row.realizedAmount, true)}
                      </Td>
                    </Tr>
                  )
                })}
              </Tbody>
            </TableScrollable>
          </>
        ))}
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
