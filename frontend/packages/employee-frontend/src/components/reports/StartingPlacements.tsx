// SPDX-FileCopyrightText: 2017-2020 City of Espoo
//
// SPDX-License-Identifier: LGPL-2.1-or-later

import React, { useEffect, useState } from 'react'
import styled from 'styled-components'
import { fi } from 'date-fns/locale'
import ReactSelect from 'react-select'
import { Link } from 'react-router-dom'

import { Container, ContentArea } from '~components/shared/layout/Container'
import Loader from '~components/shared/atoms/Loader'
import Title from '~components/shared/atoms/Title'
import { Th, Tr, Td, Thead, Tbody } from '~components/shared/layout/Table'
import { reactSelectStyles } from '~components/shared/utils'
import ReturnButton from 'components/shared/atoms/buttons/ReturnButton'
import ReportDownload from '~components/reports/ReportDownload'
import {
  FilterLabel,
  FilterRow,
  RowCountInfo,
  TableScrollable
} from '~components/reports/common'
import { useTranslation, Lang, Translations } from '~state/i18n'
import { Loading, Result, Success } from '~api'
import {
  getStartingPlacementsReport,
  PlacementsReportFilters
} from '~api/reports'
import { StartingPlacementsRow } from '~types/reports'
import LocalDate from '@evaka/lib-common/src/local-date'
import { FlexRow } from 'components/common/styled/containers'

const StyledTd = styled(Td)`
  white-space: nowrap;
`

const locales = {
  fi: fi
}

const Wrapper = styled.div`
  width: 100%;
`

function monthOptions(lang: Lang) {
  const locale = locales[lang]
  const monthOptions = []
  for (let i = 1; i <= 12; i++) {
    monthOptions.push({
      value: i.toString(),
      label: String(locale.localize?.month(i - 1))
    })
  }
  return monthOptions
}

function yearOptions() {
  const currentYear = LocalDate.today().year
  const yearOptions = []
  for (let year = currentYear; year > currentYear - 5; year--) {
    yearOptions.push({
      value: year.toString(),
      label: year.toString()
    })
  }
  return yearOptions
}

function getFilename(i18n: Translations, year: number, month: number) {
  const time = LocalDate.of(year, month, 1).format('yyyy-MM')
  return `${i18n.reports.startingPlacements.reportFileName}-${time}.csv`
}

const StartingPlacements = React.memo(function StartingPlacements() {
  const { i18n, lang } = useTranslation()
  const [rows, setRows] = useState<Result<StartingPlacementsRow[]>>(
    Success.of([])
  )
  const today = LocalDate.today()
  const [filters, setFilters] = useState<PlacementsReportFilters>({
    year: today.year,
    month: today.month
  })

  useEffect(() => {
    setRows(Loading.of())
    void getStartingPlacementsReport(filters).then(setRows)
  }, [filters])

  return (
    <Container>
      <ReturnButton />
      <ContentArea opaque>
        <Title size={1}>{i18n.reports.startingPlacements.title}</Title>
        <FilterRow>
          <FilterLabel>{i18n.reports.common.period}</FilterLabel>
          <FlexRow>
            <Wrapper>
              <ReactSelect
                options={monthOptions(lang)}
                onChange={(value) => {
                  if (value && 'value' in value) {
                    const month = parseInt(value.value)
                    setFilters({ ...filters, month })
                  }
                }}
                styles={reactSelectStyles}
                placeholder={i18n.common.month}
              />
            </Wrapper>
            <Wrapper>
              <ReactSelect
                options={yearOptions()}
                onChange={(value) => {
                  if (value && 'value' in value) {
                    const year = parseInt(value.value)
                    setFilters({ ...filters, year })
                  }
                }}
                styles={reactSelectStyles}
                placeholder={i18n.common.year}
              />
            </Wrapper>
          </FlexRow>
        </FilterRow>
        <ReportDownload
          data={rows
            .map((rs) =>
              rs.map((row) => ({
                firstName: row.firstName,
                lastName: row.lastName,
                ssn: row.ssn ?? row.dateOfBirth.format(),
                placementStart: row.placementStart.format()
              }))
            )
            .getOrElse([])}
          headers={[
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
              <Th>{i18n.reports.common.childName}</Th>
              <Th>{i18n.reports.startingPlacements.ssn}</Th>
              <Th>{i18n.reports.startingPlacements.placementStart}</Th>
            </Tr>
          </Thead>
          {rows.isSuccess && (
            <Tbody>
              {rows.value.map((row) => (
                <Tr key={row.childId}>
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

export default StartingPlacements
