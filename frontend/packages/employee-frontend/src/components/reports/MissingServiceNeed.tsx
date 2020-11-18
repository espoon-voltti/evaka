// SPDX-FileCopyrightText: 2017-2020 City of Espoo
//
// SPDX-License-Identifier: LGPL-2.1-or-later

import React, { useEffect, useMemo, useState } from 'react'
import ReactSelect from 'react-select'
import styled from 'styled-components'
import { Link } from 'react-router-dom'

import { Container, ContentArea } from '~components/shared/layout/Container'
import Loader from '~components/shared/atoms/Loader'
import Title from '~components/shared/atoms/Title'
import { Th, Tr, Td, Thead, Tbody } from '~components/shared/layout/Table'
import { reactSelectStyles } from '~components/shared/utils'
import { useTranslation } from '~state/i18n'
import { isFailure, isLoading, isSuccess, Loading, Result } from '~api'
import { MissingServiceNeedReportRow } from '~types/reports'
import {
  getMissingServiceNeedReport,
  MissingServiceNeedReportFilters
} from '~api/reports'
import ReturnButton from 'components/shared/atoms/buttons/ReturnButton'
import ReportDownload from '~components/reports/ReportDownload'
import {
  FilterLabel,
  FilterRow,
  RowCountInfo,
  TableScrollable
} from '~components/reports/common'
import { DatePicker, DatePickerClearable } from '~components/common/DatePicker'
import LocalDate from '@evaka/lib-common/src/local-date'
import { distinct } from 'utils'

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
    Loading()
  )
  const [filters, setFilters] = useState<MissingServiceNeedReportFilters>({
    startDate: LocalDate.today().subMonths(1).withDate(1),
    endDate: LocalDate.today().addMonths(2).lastDayOfMonth()
  })

  const [displayFilters, setDisplayFilters] = useState<DisplayFilters>(
    emptyDisplayFilters
  )
  const displayFilter = (row: MissingServiceNeedReportRow): boolean => {
    return !(
      displayFilters.careArea && row.careAreaName !== displayFilters.careArea
    )
  }

  useEffect(() => {
    setRows(Loading())
    setDisplayFilters(emptyDisplayFilters)
    void getMissingServiceNeedReport(filters).then(setRows)
  }, [filters])

  const filteredRows = useMemo(
    () => (isSuccess(rows) ? rows.data.filter(displayFilter) : []),
    [rows, displayFilters]
  )

  return (
    <Container>
      <ReturnButton />
      <ContentArea opaque>
        <Title size={1}>{i18n.reports.missingServiceNeed.title}</Title>

        <FilterRow>
          <FilterLabel>{i18n.reports.common.startDate}</FilterLabel>
          <DatePicker
            date={filters.startDate}
            onChange={(startDate) => setFilters({ ...filters, startDate })}
          />
        </FilterRow>
        <FilterRow>
          <FilterLabel>{i18n.reports.common.endDate}</FilterLabel>
          <DatePickerClearable
            date={filters.endDate}
            onChange={(endDate) => setFilters({ ...filters, endDate })}
            onCleared={() => setFilters({ ...filters, endDate: null })}
          />
        </FilterRow>

        <FilterRow>
          <FilterLabel>{i18n.reports.common.careAreaName}</FilterLabel>
          <Wrapper>
            <ReactSelect
              options={[
                { value: '', label: i18n.common.all },
                ...(isSuccess(rows)
                  ? distinct(
                      rows.data.map((row) => row.careAreaName)
                    ).map((s) => ({ value: s, label: s }))
                  : [])
              ]}
              onChange={(option) =>
                option && 'value' in option
                  ? setDisplayFilters({
                      ...displayFilters,
                      careArea: option.value
                    })
                  : undefined
              }
              value={
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
              styles={reactSelectStyles}
              placeholder={i18n.reports.occupancies.filters.areaPlaceholder}
            />
          </Wrapper>
        </FilterRow>

        {isLoading(rows) && <Loader />}
        {isFailure(rows) && <span>{i18n.common.loadingFailed}</span>}
        {isSuccess(rows) && (
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
