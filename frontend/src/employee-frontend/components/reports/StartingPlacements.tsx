// SPDX-FileCopyrightText: 2017-2022 City of Espoo
//
// SPDX-License-Identifier: LGPL-2.1-or-later

import range from 'lodash/range'
import sortBy from 'lodash/sortBy'
import React, { useEffect, useMemo, useState } from 'react'
import { Link } from 'react-router-dom'
import styled from 'styled-components'

import { Loading, Result, Success } from 'lib-common/api'
import { StartingPlacementsRow } from 'lib-common/generated/api-types/reports'
import LocalDate from 'lib-common/local-date'
import Loader from 'lib-components/atoms/Loader'
import Title from 'lib-components/atoms/Title'
import ReturnButton from 'lib-components/atoms/buttons/ReturnButton'
import Combobox from 'lib-components/atoms/dropdowns/Combobox'
import { Container, ContentArea } from 'lib-components/layout/Container'
import { Tbody, Td, Th, Thead, Tr } from 'lib-components/layout/Table'

import {
  getStartingPlacementsReport,
  PlacementsReportFilters
} from '../../api/reports'
import ReportDownload from '../../components/reports/ReportDownload'
import { Translations, useTranslation } from '../../state/i18n'
import { distinct } from '../../utils'
import { FlexRow } from '../common/styled/containers'

import { FilterLabel, FilterRow, RowCountInfo, TableScrollable } from './common'

const StyledTd = styled(Td)`
  white-space: nowrap;
`

const Wrapper = styled.div`
  width: 100%;
`

const monthOptions = range(1, 13)
const yearOptions = range(
  LocalDate.todayInSystemTz().year + 1,
  LocalDate.todayInSystemTz().year - 4,
  -1
)

function getFilename(i18n: Translations, year: number, month: number) {
  const time = LocalDate.of(year, month, 1).formatExotic('yyyy-MM')
  return `${i18n.reports.startingPlacements.reportFileName}-${time}.csv`
}

interface DisplayFilters {
  careArea: string
}

const emptyDisplayFilters: DisplayFilters = {
  careArea: ''
}

export default React.memo(function StartingPlacements() {
  const { i18n } = useTranslation()
  const [rows, setRows] = useState<Result<StartingPlacementsRow[]>>(
    Success.of([])
  )
  const today = LocalDate.todayInSystemTz()
  const [filters, setFilters] = useState<PlacementsReportFilters>({
    year: today.year,
    month: today.month
  })

  const [displayFilters, setDisplayFilters] =
    useState<DisplayFilters>(emptyDisplayFilters)
  const displayFilter = (row: StartingPlacementsRow): boolean =>
    !(displayFilters.careArea && row.careAreaName !== displayFilters.careArea)

  useEffect(() => {
    setRows(Loading.of())
    setDisplayFilters(emptyDisplayFilters)
    void getStartingPlacementsReport(filters).then(setRows)
  }, [filters])

  const filteredRows: StartingPlacementsRow[] = useMemo(
    () =>
      sortBy(rows.getOrElse([]).filter(displayFilter), [
        (row) => row.careAreaName,
        (row) => row.firstName,
        (row) => row.lastName
      ]),
    [rows, displayFilters] // eslint-disable-line react-hooks/exhaustive-deps
  )

  return (
    <Container>
      <ReturnButton label={i18n.common.goBack} />
      <ContentArea opaque>
        <Title size={1}>{i18n.reports.startingPlacements.title}</Title>
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
                      distinct(rs.map((row) => row.careAreaName)).map((s) => ({
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

        <ReportDownload
          data={filteredRows.map((row) => ({
            careAreaName: row.careAreaName,
            firstName: row.firstName,
            lastName: row.lastName,
            ssn: row.ssn ?? row.dateOfBirth.format(),
            placementStart: row.placementStart.format()
          }))}
          headers={[
            { label: 'Palvelualue', key: 'careAreaName' },
            { label: 'Lapsen sukunimi', key: 'lastName' },
            { label: 'Lapsen etunimi', key: 'firstName' },
            { label: 'Henkilötunnus', key: 'ssn' },
            { label: 'Aloittaa varhaiskasvatuksessa', key: 'placementStart' }
          ]}
          filename={getFilename(i18n, filters.year, filters.month)}
        />
        <TableScrollable>
          <Thead>
            <Tr>
              <Th>{i18n.reports.common.careAreaName}</Th>
              <Th>{i18n.reports.common.childName}</Th>
              <Th>{i18n.reports.startingPlacements.ssn}</Th>
              <Th>{i18n.reports.startingPlacements.placementStart}</Th>
            </Tr>
          </Thead>
          {rows.isSuccess && (
            <Tbody>
              {filteredRows.map((row) => (
                <Tr key={row.childId}>
                  <StyledTd>{row.careAreaName}</StyledTd>
                  <StyledTd>
                    <Link
                      to={`/child-information/${row.childId}`}
                    >{`${row.lastName} ${row.firstName}`}</Link>
                  </StyledTd>
                  <StyledTd>{row.ssn ?? row.dateOfBirth.format()}</StyledTd>
                  <StyledTd>{row.placementStart.format()}</StyledTd>
                </Tr>
              ))}
            </Tbody>
          )}
        </TableScrollable>
        {rows.isSuccess && <RowCountInfo rowCount={rows.value.length} />}
        {rows.isLoading && <Loader />}
        {rows.isFailure && <span>{i18n.common.loadingFailed}</span>}
      </ContentArea>
    </Container>
  )
})
