// SPDX-FileCopyrightText: 2017-2022 City of Espoo
//
// SPDX-License-Identifier: LGPL-2.1-or-later

import React, { useState } from 'react'

import { ServiceNeedReportRow } from 'lib-common/generated/api-types/reports'
import LocalDate from 'lib-common/local-date'
import { useQueryResult } from 'lib-common/query'
import Title from 'lib-components/atoms/Title'
import ReturnButton from 'lib-components/atoms/buttons/ReturnButton'
import { Container, ContentArea } from 'lib-components/layout/Container'
import { Th, Tr, Td, Thead, Tbody } from 'lib-components/layout/Table'
import { DatePickerDeprecated } from 'lib-components/molecules/DatePickerDeprecated'

import ReportDownload from '../../components/reports/ReportDownload'
import { useTranslation } from '../../state/i18n'
import { DateFilters } from '../../types/reports'
import { renderResult } from '../async-rendering'

import { FilterLabel, FilterRow, TableScrollable } from './common'
import { serviceNeedReportQuery } from './queries'

export default React.memo(function ServiceNeeds() {
  const { i18n } = useTranslation()
  const [filters, setFilters] = useState<DateFilters>({
    date: LocalDate.todayInSystemTz()
  })
  const rows = useQueryResult(serviceNeedReportQuery(filters))

  return (
    <Container>
      <ReturnButton label={i18n.common.goBack} />
      <ContentArea opaque>
        <Title size={1}>{i18n.reports.serviceNeeds.title}</Title>
        <FilterRow>
          <FilterLabel>{i18n.reports.common.date}</FilterLabel>
          <DatePickerDeprecated
            date={filters.date}
            onChange={(date) => setFilters({ date })}
          />
        </FilterRow>

        {renderResult(rows, (rows) => (
          <>
            <ReportDownload
              data={rows}
              columns={[
                { label: 'Palvelualue', value: (row) => row.careAreaName },
                { label: 'Yksikkö', value: (row) => row.unitName },
                { label: 'Ikä', value: (row) => row.age },
                { label: 'Kokopäiväinen', value: (row) => row.fullDay },
                { label: 'Osapäiväinen', value: (row) => row.partDay },
                { label: 'Kokoviikkoinen', value: (row) => row.fullWeek },
                { label: 'Osaviikkoinen', value: (row) => row.partWeek },
                { label: 'Vuorohoito', value: (row) => row.shiftCare },
                {
                  label: 'Palveluntarve puuttuu',
                  value: (row) => row.missingServiceNeed
                },
                { label: 'Lapsia yhteensä', value: (row) => row.total }
              ]}
              filename={`Lapsien palvelutarpeet ja iät yksiköissä ${filters.date.formatIso()}.csv`}
            />
            <TableScrollable>
              <Thead>
                <Tr>
                  <Th>{i18n.reports.common.careAreaName}</Th>
                  <Th>{i18n.reports.common.unitName}</Th>
                  <Th>{i18n.reports.common.unitType}</Th>
                  <Th>{i18n.reports.common.unitProviderType}</Th>
                  <Th>{i18n.reports.serviceNeeds.age}</Th>
                  <Th>{i18n.reports.serviceNeeds.fullDay}</Th>
                  <Th>{i18n.reports.serviceNeeds.partDay}</Th>
                  <Th>{i18n.reports.serviceNeeds.fullWeek}</Th>
                  <Th>{i18n.reports.serviceNeeds.partWeek}</Th>
                  <Th>{i18n.reports.serviceNeeds.shiftCare}</Th>
                  <Th>{i18n.reports.serviceNeeds.missingServiceNeed}</Th>
                  <Th>{i18n.reports.serviceNeeds.total}</Th>
                </Tr>
              </Thead>
              <Tbody>
                {rows.map((row: ServiceNeedReportRow) => (
                  <Tr key={`${row.unitName}:${row.age}`}>
                    <Td>{row.careAreaName}</Td>
                    <Td>{row.unitName}</Td>
                    <Td>
                      {row.unitType
                        ? i18n.reports.common.unitTypes[row.unitType]
                        : ''}
                    </Td>
                    <Td>
                      {
                        i18n.reports.common.unitProviderTypes[
                          row.unitProviderType
                        ]
                      }
                    </Td>
                    <Td>{row.age}</Td>
                    <Td>{row.fullDay}</Td>
                    <Td>{row.partDay}</Td>
                    <Td>{row.fullWeek}</Td>
                    <Td>{row.partWeek}</Td>
                    <Td>{row.shiftCare}</Td>
                    <Td>{row.missingServiceNeed}</Td>
                    <Td>{row.total}</Td>
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
