// SPDX-FileCopyrightText: 2017-2020 City of Espoo
//
// SPDX-License-Identifier: LGPL-2.1-or-later

import React, { useEffect, useState } from 'react'
import { Link, useParams } from 'react-router-dom'
import ReactSelect from 'react-select'
import styled from 'styled-components'
import { range } from 'lodash'

import { Container, ContentArea } from '@evaka/lib-components/layout/Container'
import Loader from '@evaka/lib-components/atoms/Loader'
import Title from '@evaka/lib-components/atoms/Title'
import { Th, Tr, Td, Thead, Tbody } from '@evaka/lib-components/layout/Table'
import { useTranslation } from '../../state/i18n'
import { Loading, Result } from '@evaka/lib-common/api'
import {
  VoucherReportRowType,
  VoucherServiceProviderUnitReport,
  VoucherServiceProviderUnitRow
} from '../../types/reports'
import {
  getVoucherServiceProviderUnitReport,
  VoucherProviderChildrenReportFilters as VoucherServiceProviderUnitFilters
} from '../../api/reports'
import ReturnButton from '@evaka/lib-components/atoms/buttons/ReturnButton'
import ReportDownload from '../../components/reports/ReportDownload'
import {
  FilterLabel,
  FilterRow,
  TableScrollable
} from '../../components/reports/common'
import { UUID } from '../../types'
import { reactSelectStyles } from '../../components/common/Select'

import {defaultMargins, Gap} from '@evaka/lib-components/white-space'

import { formatCents } from '../../utils/money'
import { capitalizeFirstLetter, formatName } from '../../utils'
import Tooltip from '@evaka/lib-components/atoms/Tooltip'
import LocalDate from "@evaka/lib-common/local-date";
import {InfoBox} from "@evaka/lib-components/molecules/MessageBoxes";
import colors from "@evaka/lib-components/colors";
import HorizontalLine from "@evaka/lib-components/atoms/HorizontalLine";
import {FixedSpaceRow} from "@evaka/lib-components/layout/flex-helpers";
import { FontAwesomeIcon } from '@fortawesome/react-fontawesome'
import {faLockAlt} from "@evaka/lib-icons";

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

const StyledTd = styled(Td)<{ type: VoucherReportRowType }>`
  ${p => p.type === 'REFUND' ? `
    border-left: 6px solid ${colors.accents.orange};
  `: p.type === 'CORRECTION' ? `
    border-left: 6px solid ${colors.accents.yellow};
  ` : `
    border-left: 6px solid ${colors.greyscale.white};
  `}
`

function VoucherServiceProviderUnit() {
  const { i18n } = useTranslation()
  const { unitId } = useParams<{ unitId: UUID }>()
  const [report, setReport] = useState<Result<VoucherServiceProviderUnitReport>>(
    Loading.of()
  )
  const [unitName, setUnitName] = useState<string>('')

  const now = new Date()

  const [filters, setFilters] = useState<VoucherServiceProviderUnitFilters>({
    month: now.getMonth() + 1,
    year: now.getFullYear()
  })

  const futureSelected = LocalDate.of(filters.year, filters.month, 1)
    .isAfter(LocalDate.today().withDate(1))

  useEffect(() => {
    if(futureSelected) return

    setReport(Loading.of())
    void getVoucherServiceProviderUnitReport(unitId, filters).then(
      (res) => {
        setReport(res)
        if (res.isSuccess && res.value.rows.length > 0) {
          setUnitName(res.value.rows[0].unitName)
        }
      }
    )
  }, [filters])

  const monthOptions = range(0, 12).map((num) => ({
    value: num + 1,
    label: capitalizeFirstLetter(i18n.datePicker.months[num])
  }))

  const yearOptions = range(2019, now.getFullYear() + 10).map((num) => ({
    value: num,
    label: num
  }))

  const formatServiceNeed = (amount: number) =>
    `${amount} ${i18n.reports.voucherServiceProviderUnit.serviceNeedType}`

  return (
    <Container>
      <ReturnButton label={i18n.common.goBack} />
      <ContentArea opaque>
        {report.isSuccess && <Title size={1}>{unitName}</Title>}
        <Title size={2}>{i18n.reports.voucherServiceProviderUnit.title}</Title>
        <FilterRow>
          <FilterLabel>
            {i18n.reports.voucherServiceProviderUnit.month}
          </FilterLabel>
          <FilterWrapper>
            <ReactSelect
              options={monthOptions}
              defaultValue={monthOptions.find(
                ({ value }) => value === filters.month
              )}
              onChange={(option) =>
                option && 'value' in option
                  ? setFilters({
                      ...filters,
                      month: option.value
                    })
                  : undefined
              }
              styles={reactSelectStyles}
            />
          </FilterWrapper>
          <FilterWrapper>
            <ReactSelect
              options={yearOptions}
              defaultValue={yearOptions.find(
                ({ value }) => value === filters.year
              )}
              onChange={(option) =>
                option && 'value' in option
                  ? setFilters({
                      ...filters,
                      year: option.value
                    })
                  : undefined
              }
              styles={reactSelectStyles}
            />
          </FilterWrapper>
        </FilterRow>

        { report.isSuccess && report.value.locked && (
          <LockedDate spacing='xs' alignItems='center'>
            <FontAwesomeIcon icon={faLockAlt}/>
            <span>
              {`${i18n.reports.voucherServiceProviders.locked}: ${report.value.locked.format()}`}
            </span>
          </LockedDate>
        )}

        <HorizontalLine slim/>

        {report.isLoading && <Loader />}
        {report.isFailure && <span>{i18n.common.loadingFailed}</span>}
        {futureSelected && <>
          <Gap />
          <InfoBox wide message={i18n.reports.voucherServiceProviders.filters.noFuture}/>
        </>}
        {!futureSelected && report.isSuccess && (
          <>
            <SumRow>
              <FixedSpaceRow>
                <strong>{`${i18n.reports.voucherServiceProviderUnit.total}`}</strong>
                <strong>{formatCents(report.value.voucherTotal)}</strong>
              </FixedSpaceRow>

              <ReportDownload
                data={[
                  ...report.value.rows.map(r => ({
                    ...r,
                    start: r.realizedPeriod.start.format(),
                    end: r.realizedPeriod.end.format(),
                    note: r.type === 'REFUND' ? 'Hyvitys' : r.type === 'CORRECTION' ? 'Korjaus' : '',
                    serviceVoucherServiceCoefficient: (r.serviceVoucherServiceCoefficient / 100.0).toFixed(2).replace('.', ','),
                    serviceVoucherValue: formatCents(r.serviceVoucherValue),
                    serviceVoucherCoPayment: formatCents(r.serviceVoucherCoPayment),
                    realizedAmount: formatCents(r.realizedAmount)
                  })),
                  {
                    childFirstName: 'Yhteensä',
                    realizedAmount: formatCents(report.value.voucherTotal)
                  }
                ]}
                headers={[
                  {
                    label: i18n.reports.voucherServiceProviderUnit.childFirstName,
                    key: 'childFirstName'
                  },
                  {
                    label: i18n.reports.voucherServiceProviderUnit.childLastName,
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
                    label:
                    i18n.reports.voucherServiceProviderUnit.serviceVoucherValue,
                    key: 'serviceVoucherValue'
                  },
                  {
                    label: i18n.reports.voucherServiceProviderUnit.serviceNeed,
                    key: 'serviceVoucherHoursPerWeek'
                  },
                  {
                    label: i18n.reports.voucherServiceProviderUnit.coefficient,
                    key: 'serviceVoucherServiceCoefficient'
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
                  <Th>{i18n.reports.voucherServiceProviderUnit.child}</Th>
                  <Th>{i18n.reports.common.groupName}</Th>
                  <Th>
                    {i18n.reports.voucherServiceProviderUnit.numberOfDays}
                  </Th>
                  <Th>
                    {
                      i18n.reports.voucherServiceProviderUnit
                        .serviceVoucherValue
                    }
                  </Th>
                  <Th>{i18n.reports.voucherServiceProviderUnit.serviceNeed}</Th>
                  <Th>{i18n.reports.voucherServiceProviderUnit.coefficient}</Th>
                  <Th>
                    {
                      i18n.reports.voucherServiceProviderUnit
                        .serviceVoucherCoPayment
                    }
                  </Th>
                  <Th>
                    {
                      i18n.reports.voucherServiceProviderUnit
                        .serviceVoucherRealizedValue
                    }
                  </Th>
                </Tr>
              </Thead>
              <Tbody>
                {report.value.rows.map((row: VoucherServiceProviderUnitRow) => (
                  <Tr key={`${row.serviceVoucherPartId}:${row.realizedPeriod.start.formatIso()}`}>
                    <StyledTd type={row.type}>
                      <Link to={`/child-information/${row.childId}`}>
                        {formatName(
                          row.childFirstName,
                          row.childLastName,
                          i18n
                        )}
                      </Link>
                      <br />
                      {row.childDateOfBirth.format()}
                    </StyledTd>
                    <Td>{row.childGroupName}</Td>
                    <Td>
                      <Tooltip
                        up
                        tooltip={
                          <div>
                            {`${row.realizedPeriod.start.format()} - ${row.realizedPeriod.end.format()}`}
                          </div>
                        }
                      >
                        {row.numberOfDays}
                      </Tooltip>
                    </Td>
                    <Td>{formatCents(row.serviceVoucherValue)}</Td>
                    <Td>{formatServiceNeed(row.serviceVoucherHoursPerWeek)}</Td>
                    <Td>{(row.serviceVoucherServiceCoefficient / 100.0).toFixed(2).replace('.', ',')}</Td>
                    <Td>{formatCents(row.serviceVoucherCoPayment)}</Td>
                    <Td>{formatCents(row.realizedAmount)}</Td>
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

export default VoucherServiceProviderUnit
