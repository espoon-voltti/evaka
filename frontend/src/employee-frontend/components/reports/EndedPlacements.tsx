// SPDX-FileCopyrightText: 2017-2022 City of Espoo
//
// SPDX-License-Identifier: LGPL-2.1-or-later

import range from 'lodash/range'
import sortBy from 'lodash/sortBy'
import React, { useCallback, useMemo, useState } from 'react'
import { Link } from 'react-router'
import styled from 'styled-components'

import { combine } from 'lib-common/api'
import type { EndedPlacementsReportRow } from 'lib-common/generated/api-types/reports'
import LocalDate from 'lib-common/local-date'
import { useQueryResult } from 'lib-common/query'
import type { Arg0 } from 'lib-common/types'
import Title from 'lib-components/atoms/Title'
import ReturnButton from 'lib-components/atoms/buttons/ReturnButton'
import Combobox from 'lib-components/atoms/dropdowns/Combobox'
import { Container, ContentArea } from 'lib-components/layout/Container'
import { Tbody, Td, Th, Thead, Tr } from 'lib-components/layout/Table'

import type { getEndedPlacementsReport } from '../../generated/api-clients/reports'
import { useTranslation } from '../../state/i18n'
import { distinct } from '../../utils'
import { renderResult } from '../async-rendering'
import { FlexRow } from '../common/styled/containers'
import ReportDownload from '../reports/ReportDownload'

import { FilterLabel, FilterRow, RowCountInfo, TableScrollable } from './common'
import { endedPlacementsReportQuery } from './queries'

type PlacementsReportFilters = Arg0<typeof getEndedPlacementsReport>

const Wrapper = styled.div`
  width: 100%;
`

const monthOptions = range(1, 13)
const yearOptions = range(
  LocalDate.todayInSystemTz().year + 1,
  LocalDate.todayInSystemTz().year - 4,
  -1
)

function getFilename(year: number, month: number) {
  const time = LocalDate.of(year, month, 1).formatExotic('yyyy-MM')
  return `Päättyvät_sijoitukset-${time}.csv`
}

export default React.memo(function EndedPlacements() {
  const { i18n } = useTranslation()
  const today = LocalDate.todayInSystemTz()
  const [filters, setFilters] = useState<PlacementsReportFilters>({
    year: today.year,
    month: today.month
  })

  interface DisplayFilters {
    careArea: string
  }

  const emptyDisplayFilters: DisplayFilters = {
    careArea: ''
  }

  const [displayFilters, setDisplayFilters] =
    useState<DisplayFilters>(emptyDisplayFilters)

  const displayFilter = useCallback(
    (row: EndedPlacementsReportRow): boolean =>
      !(displayFilters.careArea && row.areaName !== displayFilters.careArea),
    [displayFilters.careArea]
  )

  const rows = useQueryResult(endedPlacementsReportQuery(filters))

  const filteredRows = useMemo(
    () =>
      rows.map((rows) =>
        sortBy(rows.filter(displayFilter), [
          (row) => row.areaName,
          (row) => row.firstName,
          (row) => row.lastName
        ])
      ),
    [rows, displayFilter]
  )

  return (
    <Container>
      <ReturnButton label={i18n.common.goBack} />
      <ContentArea opaque>
        <Title size={1}>{i18n.reports.endedPlacements.title}</Title>
        <FilterRow>
          <FilterLabel>{i18n.reports.common.period}</FilterLabel>
          <FlexRow>
            <Wrapper>
              <Combobox
                items={monthOptions}
                selectedItem={filters.month}
                onChange={(month) => {
                  if (month !== null) {
                    setFilters({ ...filters, month })
                  }
                }}
                placeholder={i18n.common.month}
                getItemLabel={(month) => i18n.datePicker.months[month - 1]}
              />
            </Wrapper>
            <Wrapper>
              <Combobox
                items={yearOptions}
                selectedItem={filters.year}
                onChange={(year) => {
                  if (year !== null) {
                    setFilters({ ...filters, year })
                  }
                }}
                placeholder={i18n.common.year}
              />
            </Wrapper>
          </FlexRow>
        </FilterRow>

        <FilterRow>
          <FilterLabel>{i18n.reports.common.careAreaName}</FilterLabel>
          <FlexRow>
            <Wrapper>
              <Combobox
                items={[
                  { value: '', label: i18n.common.all },
                  ...rows
                    .map((rs) =>
                      distinct(rs.map((row) => row.areaName)).map((s) => ({
                        value: s,
                        label: s
                      }))
                    )
                    .getOrElse([])
                ]}
                onChange={(option) =>
                  option
                    ? setDisplayFilters({
                        ...displayFilters,
                        careArea: option.value
                      })
                    : undefined
                }
                selectedItem={
                  displayFilters.careArea !== ''
                    ? {
                        label: displayFilters.careArea,
                        value: displayFilters.careArea
                      }
                    : {
                        label: i18n.common.all,
                        value: ''
                      }
                }
                placeholder={i18n.reports.occupancies.filters.areaPlaceholder}
                getItemLabel={(item) => item.label}
              />
            </Wrapper>
          </FlexRow>
        </FilterRow>

        {renderResult(combine(rows, filteredRows), ([rows, filteredRows]) => (
          <>
            <ReportDownload
              data={filteredRows.map((row) => ({
                ...row,
                dateOfBirth: row.dateOfBirth?.format(),
                placementEnd: row.placementEnd.format(),
                nextPlacementStart: row.nextPlacementStart?.format()
              }))}
              columns={[
                { label: 'Lapsen sukunimi', value: (row) => row.lastName },
                { label: 'Lapsen etunimi', value: (row) => row.firstName },
                {
                  label: 'Lapsen syntymäaika',
                  value: (row) => row.dateOfBirth
                },
                {
                  label: 'Lopettaa varhaiskasvatuksessa',
                  value: (row) => row.placementEnd
                },
                {
                  label: 'Jatkaa varhaiskasvatuksessa',
                  value: (row) => row.nextPlacementStart
                },
                {
                  label: 'Jatkaa yksikössä',
                  value: (row) => row.nextPlacementUnitName
                }
              ]}
              filename={getFilename(filters.year, filters.month)}
            />
            <TableScrollable>
              <Thead>
                <Tr>
                  <Th>{i18n.reports.common.childName}</Th>
                  <Th>{i18n.reports.common.dateOfBirth}</Th>
                  <Th>{i18n.reports.endedPlacements.placementEnd}</Th>
                  <Th>{i18n.reports.endedPlacements.unit}</Th>
                  <Th>{i18n.reports.endedPlacements.area}</Th>
                  <Th>{i18n.reports.endedPlacements.nextPlacementStart}</Th>
                  <Th>{i18n.reports.endedPlacements.nextPlacementUnitName}</Th>
                </Tr>
              </Thead>
              <Tbody>
                {filteredRows.map((row) => (
                  <Tr key={row.childId} data-qa="report-row">
                    <Td>
                      <Link
                        to={`/child-information/${row.childId}`}
                        data-qa="child-name"
                      >{`${row.lastName ?? ''} ${row.firstName ?? ''}`}</Link>
                    </Td>
                    <Td data-qa="child-date-of-birth">
                      {row.dateOfBirth?.format()}
                    </Td>
                    <Td data-qa="placement-end-date">
                      {row.placementEnd.format()}
                    </Td>
                    <Td data-qa="unit-name">{row.unitName}</Td>
                    <Td data-qa="area-name">{row.areaName}</Td>
                    <Td data-qa="next-placement-start-date">
                      {row.nextPlacementStart?.format()}
                    </Td>
                    <Td data-qa="next-placement-unit-name">
                      {row.nextPlacementUnitName}
                    </Td>
                  </Tr>
                ))}
              </Tbody>
            </TableScrollable>
            <RowCountInfo rowCount={rows.length} />
          </>
        ))}
      </ContentArea>
    </Container>
  )
})
