// SPDX-FileCopyrightText: 2017-2020 City of Espoo
//
// SPDX-License-Identifier: LGPL-2.1-or-later

import React, { useEffect, useMemo, useState } from 'react'
import styled from 'styled-components'
import { Link } from 'react-router-dom'

import { Container, ContentArea } from 'lib-components/layout/Container'
import Loader from 'lib-components/atoms/Loader'
import Title from 'lib-components/atoms/Title'
import { Tbody, Td, Th, Thead, Tr } from 'lib-components/layout/Table'
import { useTranslation } from '../../state/i18n'
import { Loading, Result } from 'lib-common/api'
import { MissingHeadOfFamilyReportRow } from '../../types/reports'
import {
  getMissingHeadOfFamilyReport,
  MissingHeadOfFamilyReportFilters
} from '../../api/reports'
import ReturnButton from 'lib-components/atoms/buttons/ReturnButton'
import ReportDownload from '../../components/reports/ReportDownload'
import { FilterLabel, FilterRow, RowCountInfo, TableScrollable } from './common'
import {
  DatePickerClearableDeprecated,
  DatePickerDeprecated
} from 'lib-components/molecules/DatePickerDeprecated'
import LocalDate from 'lib-common/local-date'
import { distinct } from '../../utils'
import Combobox from 'lib-components/atoms/dropdowns/Combobox'

interface DisplayFilters {
  careArea: string
}

const emptyDisplayFilters: DisplayFilters = {
  careArea: ''
}

const Wrapper = styled.div`
  width: 100%;
`

function MissingHeadOfFamily() {
  const { i18n } = useTranslation()
  const [rows, setRows] = useState<Result<MissingHeadOfFamilyReportRow[]>>(
    Loading.of()
  )
  const [filters, setFilters] = useState<MissingHeadOfFamilyReportFilters>({
    startDate: LocalDate.today().subMonths(1).withDate(1),
    endDate: LocalDate.today().addMonths(2).lastDayOfMonth()
  })

  const [displayFilters, setDisplayFilters] =
    useState<DisplayFilters>(emptyDisplayFilters)
  const displayFilter = (row: MissingHeadOfFamilyReportRow): boolean => {
    return !(
      displayFilters.careArea && row.careAreaName !== displayFilters.careArea
    )
  }

  useEffect(() => {
    setRows(Loading.of())
    setDisplayFilters(emptyDisplayFilters)
    void getMissingHeadOfFamilyReport(filters).then(setRows)
  }, [filters])

  const filteredRows: MissingHeadOfFamilyReportRow[] = useMemo(
    () => rows.map((rs) => rs.filter(displayFilter)).getOrElse([]),
    [rows, displayFilters] // eslint-disable-line react-hooks/exhaustive-deps
  )

  return (
    <Container>
      <ReturnButton label={i18n.common.goBack} />
      <ContentArea opaque>
        <Title size={1}>{i18n.reports.missingHeadOfFamily.title}</Title>

        <FilterRow>
          <FilterLabel>{i18n.reports.common.startDate}</FilterLabel>
          <DatePickerDeprecated
            date={filters.startDate}
            onChange={(startDate) => setFilters({ ...filters, startDate })}
          />
        </FilterRow>
        <FilterRow>
          <FilterLabel>{i18n.reports.common.endDate}</FilterLabel>
          <DatePickerClearableDeprecated
            date={filters.endDate}
            onChange={(endDate) => setFilters({ ...filters, endDate })}
            onCleared={() => setFilters({ ...filters, endDate: null })}
          />
        </FilterRow>

        <FilterRow>
          <FilterLabel>{i18n.reports.common.careAreaName}</FilterLabel>
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
        </FilterRow>

        {rows.isLoading && <Loader />}
        {rows.isFailure && <span>{i18n.common.loadingFailed}</span>}
        {rows.isSuccess && (
          <>
            <ReportDownload
              data={filteredRows}
              headers={[
                { label: 'Palvelualue', key: 'careAreaName' },
                { label: 'Yksikön nimi', key: 'unitName' },
                { label: 'Lapsen sukunimi', key: 'lastName' },
                { label: 'Lapsen etunimi', key: 'firstName' },
                { label: 'Puutteellisia päiviä', key: 'daysWithoutHead' }
              ]}
              filename={`Puuttuvat päämiehet ${filters.startDate.formatIso()}-${
                filters.endDate?.formatIso() ?? ''
              }.csv`}
            />
            <TableScrollable>
              <Thead>
                <Tr>
                  <Th>{i18n.reports.common.careAreaName}</Th>
                  <Th>{i18n.reports.common.unitName}</Th>
                  <Th>{i18n.reports.common.childName}</Th>
                  <Th>
                    {i18n.reports.missingHeadOfFamily.daysWithoutHeadOfFamily}
                  </Th>
                </Tr>
              </Thead>
              <Tbody>
                {filteredRows.map((row: MissingHeadOfFamilyReportRow) => (
                  <Tr key={`${row.unitId}:${row.childId}`}>
                    <Td>{row.careAreaName}</Td>
                    <Td>
                      <Link to={`/units/${row.unitId}`}>{row.unitName}</Link>
                    </Td>
                    <Td>
                      <Link to={`/child-information/${row.childId}`}>
                        {row.lastName} {row.firstName}
                      </Link>
                    </Td>
                    <Td>{row.daysWithoutHead}</Td>
                  </Tr>
                ))}
              </Tbody>
            </TableScrollable>
            <RowCountInfo rowCount={filteredRows.length} />
          </>
        )}
      </ContentArea>
    </Container>
  )
}

export default MissingHeadOfFamily
