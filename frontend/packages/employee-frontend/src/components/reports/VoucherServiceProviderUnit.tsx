// SPDX-FileCopyrightText: 2017-2020 City of Espoo
//
// SPDX-License-Identifier: LGPL-2.1-or-later

import React, { useEffect, useState } from 'react'
import { useParams } from 'react-router-dom'
import ReactSelect from 'react-select'
import styled from 'styled-components'
import { range } from 'lodash'

import { Container, ContentArea } from '~components/shared/layout/Container'
import Loader from '~components/shared/atoms/Loader'
import Title from '~components/shared/atoms/Title'
import { Th, Tr, Td, Thead, Tbody } from '~components/shared/layout/Table'
import { useTranslation } from '~state/i18n'
import { isFailure, isLoading, isSuccess, Loading, Result } from '~api'
import { VoucherServiceProviderUnitRow } from '~types/reports'
import {
  getVoucherServiceProviderUnitReport,
  VoucherProviderChildrenReportFilters as VoucherServiceProviderUnitFilters
} from '~api/reports'
import ReturnButton from 'components/shared/atoms/buttons/ReturnButton'
import ReportDownload from '~components/reports/ReportDownload'
import {
  FilterLabel,
  FilterRow,
  TableScrollable
} from '~components/reports/common'
import { UUID } from '~types'
import { reactSelectStyles } from '~components/shared/utils'

import { DefaultMargins } from 'components/shared/layout/white-space'

import { formatCents } from '~utils/money'
import { capitalizeFirstLetter, formatName } from '~utils'
import Tooltip from '~components/shared/atoms/Tooltip'

const Wrapper = styled.div`
  width: 100%;
  margin: 0 ${DefaultMargins.m};
`

function VoucherServiceProviderUnit() {
  const { i18n } = useTranslation()
  const { unitId } = useParams<{ unitId: UUID }>()
  const [rows, setRows] = useState<Result<VoucherServiceProviderUnitRow[]>>(
    Loading()
  )

  const now = new Date()

  const [filters, setFilters] = useState<VoucherServiceProviderUnitFilters>({
    month: now.getMonth() + 1,
    year: now.getFullYear()
  })

  useEffect(() => {
    setRows(Loading())
    void getVoucherServiceProviderUnitReport(unitId, filters).then(setRows)
  }, [filters])

  const monthOptions = range(0, 12).map((num) => ({
    value: num + 1,
    label: capitalizeFirstLetter(i18n.datePicker.months[num])
  }))

  const yearOptions = range(2019, now.getFullYear() + 10).map((num) => ({
    value: num,
    label: num
  }))

  return (
    <Container>
      <ReturnButton />
      <ContentArea opaque>
        {isSuccess(rows) && rows.data.length > 0 && (
          <Title size={1}>{rows.data[0].unitName}</Title>
        )}
        <Title size={2}>{i18n.reports.voucherServiceProviderUnit.title}</Title>
        <FilterRow>
          <FilterLabel>
            {i18n.reports.voucherServiceProviderUnit.month}
          </FilterLabel>
          <Wrapper>
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
          </Wrapper>
          <Wrapper>
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
          </Wrapper>
        </FilterRow>

        {isLoading(rows) && <Loader />}
        {isFailure(rows) && <span>{i18n.common.loadingFailed}</span>}
        {isSuccess(rows) && (
          <>
            <ReportDownload
              data={rows.data}
              headers={[
                {
                  label: i18n.reports.voucherServiceProviderUnit.child,
                  key: 'childName'
                },
                {
                  label: i18n.reports.common.groupName,
                  key: 'groupName'
                },
                {
                  label: i18n.reports.voucherServiceProviderUnit.numberOfDays,
                  key: 'serviceVoucherValueSum'
                },
                {
                  label:
                    i18n.reports.voucherServiceProviderUnit
                      .serviceVoucherValueSum,
                  key: 'serviceVoucherValueSum'
                },
                {
                  label:
                    i18n.reports.voucherServiceProviderUnit.serviceVoucherValue,
                  key: 'serviceVoucherValue'
                },
                {
                  label:
                    i18n.reports.voucherServiceProviderUnit
                      .serviceVoucherCoPayment,
                  key: 'serviceVoucherCoPayment'
                },
                {
                  label: i18n.reports.voucherServiceProviderUnit.coefficient,
                  key: 'serviceVoucherServiceCoefficient'
                }
              ]}
              filename={`Palvelusetelilapset yksikössä.csv`}
            />
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
                        .serviceVoucherValueSum
                    }
                  </Th>
                  <Th>
                    {
                      i18n.reports.voucherServiceProviderUnit
                        .serviceVoucherCoPayment
                    }
                  </Th>
                  <Th>
                    {
                      i18n.reports.voucherServiceProviderUnit
                        .serviceVoucherValue
                    }
                  </Th>
                  <Th>{i18n.reports.voucherServiceProviderUnit.coefficient}</Th>
                  <Th>
                    {i18n.reports.voucherServiceProviderUnit.serviceNeedDays}
                  </Th>
                  <Th>{i18n.reports.voucherServiceProviderUnit.partTime}</Th>
                </Tr>
              </Thead>
              <Tbody>
                {rows.data.map((row: VoucherServiceProviderUnitRow) => (
                  <Tr key={`${row.childId}`}>
                    <Td>
                      {formatName(row.childFirstName, row.childLastName, i18n)}{' '}
                      <br />
                      {row.childDateOfBirth.format()}
                    </Td>
                    <Td>{row.childGroupName}</Td>
                    <Td>
                      <Tooltip
                        tooltip={
                          <div>
                            {row.derivatives.realizedPeriod.start.format()} -{' '}
                            {row.derivatives.realizedPeriod.end.format()}
                          </div>
                        }
                      >
                        {row.derivatives.numberOfDays}
                      </Tooltip>
                    </Td>
                    <Td>
                      {formatCents(
                        row.serviceVoucherValue + row.serviceVoucherCoPayment
                      )}
                    </Td>
                    <Td>{formatCents(row.serviceVoucherCoPayment)}</Td>
                    <Td>{formatCents(row.derivatives.realizedAmount)}</Td>
                    <Td>{row.derivatives.coefficient}</Td>
                    <Td></Td>
                    <Td></Td>
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
