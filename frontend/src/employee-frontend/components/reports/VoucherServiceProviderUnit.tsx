// SPDX-FileCopyrightText: 2017-2020 City of Espoo
//
// SPDX-License-Identifier: LGPL-2.1-or-later

import { FontAwesomeIcon } from '@fortawesome/react-fontawesome'
import { fi } from 'date-fns/locale'
import { Loading, Result } from 'lib-common/api'
import ReturnButton from 'lib-components/atoms/buttons/ReturnButton'
import Combobox from 'lib-components/atoms/form/Combobox'
import HorizontalLine from 'lib-components/atoms/HorizontalLine'
import Loader from 'lib-components/atoms/Loader'
import RoundIcon from 'lib-components/atoms/RoundIcon'
import Title from 'lib-components/atoms/Title'
import Tooltip from 'lib-components/atoms/Tooltip'
import { Container, ContentArea } from 'lib-components/layout/Container'
import {
  FixedSpaceColumn,
  FixedSpaceRow
} from 'lib-components/layout/flex-helpers'
import {
  SortableTh,
  Tbody,
  Td,
  Th,
  Thead,
  Tr
} from 'lib-components/layout/Table'
import { defaultMargins } from 'lib-components/white-space'
import colors from 'lib-customizations/common'
import { faLockAlt, fasArrowDown, fasArrowUp } from 'lib-icons'
import { range, sortBy } from 'lodash'
import React, { useEffect, useMemo, useState } from 'react'
import { Link, useLocation, useParams } from 'react-router-dom'
import styled from 'styled-components'
import {
  getVoucherServiceProviderUnitReport,
  VoucherProviderChildrenReportFilters as VoucherServiceProviderUnitFilters
} from '../../api/reports'
import ReportDownload from '../../components/reports/ReportDownload'
import { useTranslation } from '../../state/i18n'
import { UUID } from '../../types'
import {
  VoucherReportRowType,
  VoucherServiceProviderUnitReport,
  VoucherServiceProviderUnitRow
} from '../../types/reports'
import { formatName } from '../../utils'
import { formatCents } from '../../utils/money'
import { useSyncQueryParams } from '../../utils/useSyncQueryParams'
import { SelectOption } from '../common/Select'
import { FilterLabel, FilterRow, TableScrollable } from './common'

const FilterWrapper = styled.div`
  width: 400px;
  margin-right: ${defaultMargins.s};
`

const LockedDate = styled(FixedSpaceRow)`
  float: right;
  color: ${colors.greyscale.dark};
  margin-bottom: ${defaultMargins.xs};
`

const SumRow = styled.div`
  width: 100%;
  display: flex;
  justify-content: space-between;
  align-items: center;
`

const StyledTd = styled(Td)<{ type: VoucherReportRowType | 'NEW' }>`
  ${(p) => {
    const color =
      p.type === 'REFUND'
        ? colors.accents.orange
        : p.type === 'CORRECTION'
        ? colors.accents.yellow
        : p.type === 'NEW'
        ? colors.blues.primary
        : colors.greyscale.white

    return `border-left: 6px solid ${color};`
  }}
`

const StyledTh = styled(Th)`
  white-space: nowrap;
`

const monthOptions: SelectOption[] = range(0, 12).map((num) => ({
  value: String(num + 1),
  label: String(fi.localize?.month(num))
}))

const minYear = new Date().getFullYear() - 4
const maxYear = new Date().getFullYear()
const yearOptions: SelectOption[] = range(maxYear, minYear - 1, -1).map(
  (num) => ({
    value: String(num),
    label: String(num)
  })
)

function VoucherServiceProviderUnit() {
  const location = useLocation()
  const { i18n } = useTranslation()
  const { unitId } = useParams<{ unitId: UUID }>()
  const [report, setReport] = useState<
    Result<VoucherServiceProviderUnitReport>
  >(Loading.of())
  const [sort, setSort] = useState<'child' | 'group'>('child')

  const sortOnClick = (prop: 'child' | 'group') => () => {
    if (sort !== prop) {
      setSort(prop)
    }
  }

  const sortedReport = report.map((rs) =>
    sort === 'group' ? { ...rs, rows: sortBy(rs.rows, 'childGroupName') } : rs
  )

  const [unitName, setUnitName] = useState<string>('')
  const [filters, setFilters] = useState<VoucherServiceProviderUnitFilters>(
    () => {
      const { search } = location
      const queryParams = new URLSearchParams(search)
      const year = Number(queryParams.get('year'))
      const month = Number(queryParams.get('month'))

      return {
        year:
          year >= minYear && year <= maxYear ? year : new Date().getFullYear(),
        month: month >= 1 && month <= 12 ? month : new Date().getMonth() + 1
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
        {sortedReport.isSuccess && <Title size={1}>{unitName}</Title>}
        <Title size={2}>{i18n.reports.voucherServiceProviderUnit.title}</Title>
        <FilterRow>
          <FilterLabel>
            {i18n.reports.voucherServiceProviderUnit.month}
          </FilterLabel>
          <FilterWrapper>
            <Combobox
              items={monthOptions}
              selectedItem={
                monthOptions.find(
                  ({ value }) => Number(value) === filters.month
                ) ?? null
              }
              onChange={(option) =>
                option
                  ? setFilters({
                      ...filters,
                      month: Number(option.value)
                    })
                  : undefined
              }
              getItemLabel={(item) => item.label}
            />
          </FilterWrapper>
          <FilterWrapper>
            <Combobox
              items={yearOptions}
              selectedItem={
                yearOptions.find(
                  ({ value }) => Number(value) === filters.year
                ) ?? null
              }
              onChange={(option) =>
                option
                  ? setFilters({
                      ...filters,
                      year: Number(option.value)
                    })
                  : undefined
              }
              getItemLabel={(item) => item.label}
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
                <strong>{`${i18n.reports.voucherServiceProviderUnit.total}`}</strong>
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
                    serviceVoucherValue: formatCents(
                      r.serviceVoucherValue,
                      true
                    ),
                    serviceVoucherCoPayment: formatCents(
                      r.serviceVoucherCoPayment,
                      true
                    ),
                    realizedAmount: formatCents(r.realizedAmount, true)
                  })),
                  {
                    childFirstName: 'Yhteensä',
                    realizedAmount: formatCents(
                      sortedReport.value.voucherTotal,
                      true
                    )
                  }
                ]}
                headers={[
                  {
                    label:
                      i18n.reports.voucherServiceProviderUnit.childFirstName,
                    key: 'childFirstName'
                  },
                  {
                    label:
                      i18n.reports.voucherServiceProviderUnit.childLastName,
                    key: 'childLastName'
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
                      i18n.reports.voucherServiceProviderUnit
                        .serviceVoucherValue,
                    key: 'serviceVoucherValue'
                  },
                  {
                    label:
                      i18n.reports.voucherServiceProviderUnit
                        .serviceVoucherCoPayment,
                    key: 'serviceVoucherCoPayment'
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
                    {
                      i18n.reports.voucherServiceProviderUnit
                        .serviceVoucherValue
                    }
                  </StyledTh>
                  <StyledTh>
                    {
                      i18n.reports.voucherServiceProviderUnit
                        .serviceVoucherCoPayment
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
                {sortedReport.value.rows.map(
                  (row: VoucherServiceProviderUnitRow) => {
                    const under3YearsOld =
                      row.realizedPeriod.start.differenceInYears(
                        row.childDateOfBirth
                      ) < 3
                    return (
                      <Tr
                        key={`${
                          row.serviceVoucherDecisionId
                        }:${row.realizedPeriod.start.formatIso()}`}
                      >
                        <StyledTd
                          type={
                            row.isNew && row.type === 'ORIGINAL'
                              ? 'NEW'
                              : row.type
                          }
                        />
                        <Td>
                          <FixedSpaceColumn spacing="xs">
                            <Link to={`/child-information/${row.childId}`}>
                              {formatName(
                                row.childFirstName,
                                row.childLastName,
                                i18n
                              )}
                            </Link>
                            <FixedSpaceRow spacing="xs">
                              <Tooltip
                                tooltip={
                                  <div>
                                    {
                                      i18n.reports.voucherServiceProviderUnit[
                                        under3YearsOld ? 'under3' : 'atLeast3'
                                      ]
                                    }
                                  </div>
                                }
                                position="right"
                              >
                                <RoundIcon
                                  content={
                                    under3YearsOld ? fasArrowDown : fasArrowUp
                                  }
                                  color={
                                    under3YearsOld
                                      ? colors.accents.green
                                      : colors.blues.medium
                                  }
                                  size="s"
                                />
                              </Tooltip>
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
                            {row.realizedPeriod.format()}
                          </Tooltip>
                        </Td>
                        <Td>{row.serviceNeedDescription}</Td>
                        <Td>{formatCents(row.serviceVoucherValue, true)}</Td>
                        <Td>
                          {formatCents(row.serviceVoucherCoPayment, true)}
                        </Td>
                        <Td>{formatCents(row.realizedAmount, true)}</Td>
                      </Tr>
                    )
                  }
                )}
              </Tbody>
            </TableScrollable>
          </>
        )}
      </ContentArea>
    </Container>
  )
}

export default VoucherServiceProviderUnit
