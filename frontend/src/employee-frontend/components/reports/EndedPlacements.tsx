// SPDX-FileCopyrightText: 2017-2022 City of Espoo
//
// SPDX-License-Identifier: LGPL-2.1-or-later

import range from 'lodash/range'
import React, { useEffect, useState } from 'react'
import { Link } from 'react-router-dom'
import styled from 'styled-components'

import { Loading, Result, Success, wrapResult } from 'lib-common/api'
import {EndedPlacementsReportRow, StartingPlacementsRow} from 'lib-common/generated/api-types/reports'
import LocalDate from 'lib-common/local-date'
import { Arg0 } from 'lib-common/types'
import Loader from 'lib-components/atoms/Loader'
import Title from 'lib-components/atoms/Title'
import ReturnButton from 'lib-components/atoms/buttons/ReturnButton'
import Combobox from 'lib-components/atoms/dropdowns/Combobox'
import { Container, ContentArea } from 'lib-components/layout/Container'
import { Tbody, Td, Th, Thead, Tr } from 'lib-components/layout/Table'

import { getEndedPlacementsReport } from '../../generated/api-clients/reports'
import { useTranslation } from '../../state/i18n'
import { FlexRow } from '../common/styled/containers'
import ReportDownload from '../reports/ReportDownload'

import { FilterLabel, FilterRow, RowCountInfo, TableScrollable } from './common'
import {distinct} from "../../utils";

const getEndedPlacementsReportResult = wrapResult(getEndedPlacementsReport)

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
  const [rows, setRows] = useState<Result<EndedPlacementsReportRow[]>>(
    Success.of([])
  )
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

  const displayFilter = (row: StartingPlacementsRow): boolean =>
    !(displayFilters.careArea && row.careAreaName !== displayFilters.careArea)

  useEffect(() => {
    setRows(Loading.of())
    void getEndedPlacementsReportResult(filters).then(setRows)
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
              filename={getFilename(filters.year, filters.month)}
            />
            <TableScrollable>
              <Thead>
                <Tr>
                  <Th>{i18n.reports.common.childName}</Th>
                  <Th>{i18n.reports.endedPlacements.ssn}</Th>
                  <Th>{i18n.reports.endedPlacements.placementEnd}</Th>
                  <Th>{i18n.reports.endedPlacements.unit}</Th>
                  <Th>{i18n.reports.endedPlacements.area}</Th>
                  <Th>{i18n.reports.endedPlacements.nextPlacementStart}</Th>
                </Tr>
              </Thead>
              <Tbody>
                {rows.value.map((row) => (
                  <Tr key={row.childId}>
                    <Td>
                      <Link to={`/child-information/${row.childId}`}>{`${
                        row.lastName ?? ''
                      } ${row.firstName ?? ''}`}</Link>
                    </Td>
                    <Td>{row.ssn}</Td>
                    <Td>{row.placementEnd.format()}</Td>
                    <Td>{row.unitName}</Td>
                    <Td>{row.areaName}</Td>
                    <Td>{row.nextPlacementStart?.format()}</Td>
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
})
