// SPDX-FileCopyrightText: 2017-2020 City of Espoo
//
// SPDX-License-Identifier: LGPL-2.1-or-later

import React, { useEffect, useState } from 'react'
import {
  Container,
  ContentArea,
  Loader,
  Table,
  Title
} from '~components/shared/alpha'
import { Translations, useTranslation } from '~state/i18n'
import { isFailure, isLoading, isSuccess, Loading, Result, Success } from '~api'
import { EndedPlacementsReportRow } from '~types/reports'
import { getEndedPlacementsReport, PlacementsReportFilters } from '~api/reports'
import ReturnButton from 'components/shared/atoms/buttons/ReturnButton'
import ReportDownload from '~components/reports/ReportDownload'
import styled from 'styled-components'
import SelectWithIcon, { SelectOptionProps } from '~components/common/Select'
import { fi } from 'date-fns/locale'
import {
  FilterLabel,
  FilterRow,
  RowCountInfo,
  TableScrollable
} from '~components/reports/common'
import { Link } from 'react-router-dom'
import LocalDate from '@evaka/lib-common/src/local-date'
import { FlexRow } from 'components/common/styled/containers'

const StyledTd = styled(Table.Td)`
  white-space: nowrap;
`

function monthOptions(): SelectOptionProps[] {
  const monthOptions = []
  for (let i = 1; i <= 12; i++) {
    monthOptions.push({
      id: i.toString(),
      label: String(fi.localize?.month(i - 1))
    })
  }
  return monthOptions
}

function yearOptions(): SelectOptionProps[] {
  const currentYear = LocalDate.today().year
  const yearOptions = []
  for (let year = currentYear; year > currentYear - 5; year--) {
    yearOptions.push({
      id: year.toString(),
      label: year.toString()
    })
  }
  return yearOptions
}

function getFilename(i18n: Translations, year: number, month: number) {
  const time = LocalDate.of(year, month, 1).format('yyyy-MM')
  return `Päättyvät_sijoitukset-${time}.csv`
}

function EndedPlacements() {
  const { i18n } = useTranslation()
  const [rows, setRows] = useState<Result<EndedPlacementsReportRow[]>>(
    Success([])
  )
  const today = LocalDate.today()
  const [filters, setFilters] = useState<PlacementsReportFilters>({
    year: today.year,
    month: today.month
  })

  useEffect(() => {
    setRows(Loading())
    void getEndedPlacementsReport(filters).then(setRows)
  }, [filters])

  return (
    <Container>
      <ReturnButton />
      <ContentArea opaque>
        <Title size={1}>{i18n.reports.endedPlacements.title}</Title>
        <FilterRow>
          <FilterLabel>{i18n.reports.common.period}</FilterLabel>
          <FlexRow>
            <SelectWithIcon
              options={monthOptions()}
              value={filters.month.toString()}
              onChange={(e) => {
                const month = parseInt(e.target.value)
                setFilters({ ...filters, month })
              }}
            />
            <SelectWithIcon
              options={yearOptions()}
              value={filters.year.toString()}
              onChange={(e) => {
                const year = parseInt(e.target.value)
                setFilters({ ...filters, year })
              }}
            />
          </FlexRow>
        </FilterRow>
        {isLoading(rows) && <Loader />}
        {isFailure(rows) && <span>{i18n.common.loadingFailed}</span>}
        {isSuccess(rows) && (
          <>
            <ReportDownload
              data={rows.data.map((row) => ({
                ...row,
                placementEnd: row.placementEnd.format(),
                nextPlacementStart: row.nextPlacementStart?.format()
              }))}
              headers={[
                { label: 'Lapsen sukunimi', key: 'lastName' },
                { label: 'Lapsen etunimi', key: 'firstName' },
                { label: 'Henkilötunnus', key: 'ssn' },
                { label: 'Lopettaa varhaiskasvatuksessa', key: 'placementEnd' },
                {
                  label: 'Jatkaa varhaiskasvatuksessa',
                  key: 'nextPlacementStart'
                }
              ]}
              filename={getFilename(i18n, filters.year, filters.month)}
            />
            <TableScrollable>
              <Table.Head>
                <Table.Row>
                  <Table.Th>{i18n.reports.common.childName}</Table.Th>
                  <Table.Th>{i18n.reports.endedPlacements.ssn}</Table.Th>
                  <Table.Th>
                    {i18n.reports.endedPlacements.placementEnd}
                  </Table.Th>
                  <Table.Th>
                    {i18n.reports.endedPlacements.nextPlacementStart}
                  </Table.Th>
                </Table.Row>
              </Table.Head>
              <Table.Body>
                {rows.data.map((row) => (
                  <Table.Row key={row.childId}>
                    <StyledTd>
                      <Link to={`/child-information/${row.childId}`}>{`${
                        row.lastName ?? ''
                      } ${row.firstName ?? ''}`}</Link>
                    </StyledTd>
                    <StyledTd>{row.ssn}</StyledTd>
                    <StyledTd>{row.placementEnd.format()}</StyledTd>
                    <StyledTd>{row.nextPlacementStart?.format()}</StyledTd>
                  </Table.Row>
                ))}
              </Table.Body>
            </TableScrollable>
            <RowCountInfo rowCount={rows.data.length} />
          </>
        )}
      </ContentArea>
    </Container>
  )
}

export default EndedPlacements
