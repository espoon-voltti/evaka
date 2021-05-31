// SPDX-FileCopyrightText: 2017-2020 City of Espoo
//
// SPDX-License-Identifier: LGPL-2.1-or-later

import React, { useEffect, useState } from 'react'
import styled from 'styled-components'

import { Container, ContentArea } from 'lib-components/layout/Container'
import Loader from 'lib-components/atoms/Loader'
import Title from 'lib-components/atoms/Title'
import { Tbody, Td, Th, Thead, Tr } from 'lib-components/layout/Table'
import { Translations, useTranslation } from '../../state/i18n'
import { Loading, Result, Success } from 'lib-common/api'
import { EndedPlacementsReportRow } from '../../types/reports'
import {
  getEndedPlacementsReport,
  PlacementsReportFilters
} from '../../api/reports'
import ReturnButton from 'lib-components/atoms/buttons/ReturnButton'
import ReportDownload from '../../components/reports/ReportDownload'
import { SelectOptionProps } from '../../components/common/Select'
import { fi } from 'date-fns/locale'
import {
  FilterLabel,
  FilterRow,
  RowCountInfo,
  TableScrollable
} from '../../components/reports/common'
import { Link } from 'react-router-dom'
import LocalDate from 'lib-common/local-date'
import { FlexRow } from '../../components/common/styled/containers'
import Combobox from 'lib-components/atoms/form/Combobox'

const StyledTd = styled(Td)`
  white-space: nowrap;
`

const Wrapper = styled.div`
  width: 100%;
`

function monthOptions(): SelectOptionProps[] {
  const monthOptions = []
  for (let i = 1; i <= 12; i++) {
    monthOptions.push({
      value: i.toString(),
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
      value: year.toString(),
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
    Success.of([])
  )
  const today = LocalDate.today()
  const [filters, setFilters] = useState<PlacementsReportFilters>({
    year: today.year,
    month: today.month
  })

  useEffect(() => {
    setRows(Loading.of())
    void getEndedPlacementsReport(filters).then(setRows)
  }, [filters])

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
                items={monthOptions()}
                selectedItem={
                  monthOptions().find(
                    ({ value }) => Number(value) === filters.month
                  ) ?? null
                }
                onChange={(value) => {
                  if (value) {
                    const month = parseInt(value.value)
                    setFilters({ ...filters, month })
                  }
                }}
                placeholder={i18n.common.month}
                getItemLabel={(item) => item.label}
              />
            </Wrapper>
            <Wrapper>
              <Combobox
                items={yearOptions()}
                selectedItem={
                  yearOptions().find(
                    ({ value }) => Number(value) === filters.year
                  ) ?? null
                }
                onChange={(value) => {
                  if (value) {
                    const year = parseInt(value.value)
                    setFilters({ ...filters, year })
                  }
                }}
                placeholder={i18n.common.year}
                getItemLabel={(item) => item.label}
              />
            </Wrapper>
          </FlexRow>
        </FilterRow>
        {rows.isLoading && <Loader />}
        {rows.isFailure && <span>{i18n.common.loadingFailed}</span>}
        {rows.isSuccess && (
          <>
            <ReportDownload
              data={rows.value.map((row) => ({
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
              <Thead>
                <Tr>
                  <Th>{i18n.reports.common.childName}</Th>
                  <Th>{i18n.reports.endedPlacements.ssn}</Th>
                  <Th>{i18n.reports.endedPlacements.placementEnd}</Th>
                  <Th>{i18n.reports.endedPlacements.nextPlacementStart}</Th>
                </Tr>
              </Thead>
              <Tbody>
                {rows.value.map((row) => (
                  <Tr key={row.childId}>
                    <StyledTd>
                      <Link to={`/child-information/${row.childId}`}>{`${
                        row.lastName ?? ''
                      } ${row.firstName ?? ''}`}</Link>
                    </StyledTd>
                    <StyledTd>{row.ssn}</StyledTd>
                    <StyledTd>{row.placementEnd.format()}</StyledTd>
                    <StyledTd>{row.nextPlacementStart?.format()}</StyledTd>
                  </Tr>
                ))}
              </Tbody>
            </TableScrollable>
            <RowCountInfo rowCount={rows.value.length} />
          </>
        )}
      </ContentArea>
    </Container>
  )
}

export default EndedPlacements
