// SPDX-FileCopyrightText: 2017-2020 City of Espoo
//
// SPDX-License-Identifier: LGPL-2.1-or-later

import React, { useEffect, useMemo, useState } from 'react'
import styled from 'styled-components'
import { Link } from 'react-router-dom'

import { Loading, Result } from 'lib-common/api'
import LocalDate from 'lib-common/local-date'
import Combobox from 'lib-components/atoms/form/Combobox'
import Loader from 'lib-components/atoms/Loader'
import Title from 'lib-components/atoms/Title'
import ReturnButton from 'lib-components/atoms/buttons/ReturnButton'
import {
  DatePickerClearableDeprecated,
  DatePickerDeprecated
} from 'lib-components/molecules/DatePickerDeprecated'
import { Container, ContentArea } from 'lib-components/layout/Container'
import { Tbody, Td, Th, Thead, Tr } from 'lib-components/layout/Table'
import { useTranslation } from '../../state/i18n'
import { MissingServiceNeedReportRow } from '../../types/reports'
import {
  getMissingServiceNeedReport,
  MissingServiceNeedReportFilters
} from '../../api/reports'
import ReportDownload from '../../components/reports/ReportDownload'
import { FilterLabel, FilterRow, RowCountInfo, TableScrollable } from './common'
import { distinct } from '../../utils'

interface DisplayFilters {
  careArea: string
}

const emptyDisplayFilters: DisplayFilters = {
  careArea: ''
}

const Wrapper = styled.div`
  width: 100%;
`

function MissingServiceNeed() {
  const { i18n } = useTranslation()
  const [rows, setRows] = useState<Result<MissingServiceNeedReportRow[]>>(
    Loading.of()
  )
  const [filters, setFilters] = useState<MissingServiceNeedReportFilters>({
    startDate: LocalDate.today().subMonths(1).withDate(1),
    endDate: LocalDate.today().addMonths(2).lastDayOfMonth()
  })

  const [displayFilters, setDisplayFilters] =
    useState<DisplayFilters>(emptyDisplayFilters)
  const displayFilter = (row: MissingServiceNeedReportRow): boolean => {
    return !(
      displayFilters.careArea && row.careAreaName !== displayFilters.careArea
    )
  }

  useEffect(() => {
    setRows(Loading.of())
    setDisplayFilters(emptyDisplayFilters)
    void getMissingServiceNeedReport(filters).then(setRows)
  }, [filters])

  const filteredRows: MissingServiceNeedReportRow[] = useMemo(
    () => rows.map((rs) => rs.filter(displayFilter)).getOrElse([]),
    [rows, displayFilters] // eslint-disable-line react-hooks/exhaustive-deps
  )

  return (
    <Container>
      <ReturnButton label={i18n.common.goBack} />
      <ContentArea opaque>
        <Title size={1}>{i18n.reports.missingServiceNeed.title}</Title>

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
                { label: 'Puutteellisia päiviä', key: 'daysWithoutServiceNeed' }
              ]}
              filename={`Puuttuvat palveluntarpeet ${filters.startDate.formatIso()}-${
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
                    {i18n.reports.missingServiceNeed.daysWithoutServiceNeed}
                  </Th>
                </Tr>
              </Thead>
              <Tbody>
                {filteredRows.map((row: MissingServiceNeedReportRow) => (
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
                    <Td>{row.daysWithoutServiceNeed}</Td>
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

export default MissingServiceNeed
