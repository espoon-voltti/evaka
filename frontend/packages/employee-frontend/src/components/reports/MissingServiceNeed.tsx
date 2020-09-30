// SPDX-FileCopyrightText: 2017-2020 City of Espoo
//
// SPDX-License-Identifier: LGPL-2.1-or-later

import React, { useEffect, useMemo, useState } from 'react'
import {
  Container,
  ContentArea,
  Loader,
  Table,
  Title
} from '~components/shared/alpha'
import { useTranslation } from '~state/i18n'
import { Link } from 'react-router-dom'
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
import SelectWithIcon from 'components/common/Select'
import { distinct } from 'utils'

interface DisplayFilters {
  careArea: string
}

const emptyDisplayFilters: DisplayFilters = {
  careArea: ''
}

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
          <SelectWithIcon
            options={[
              { id: '', label: '' },
              ...(isSuccess(rows)
                ? distinct(
                    rows.data.map((row) => row.careAreaName)
                  ).map((s) => ({ id: s, label: s }))
                : [])
            ]}
            value={displayFilters.careArea}
            onChange={(e) =>
              setDisplayFilters({ ...displayFilters, careArea: e.target.value })
            }
            fullWidth
          />
        </FilterRow>

        {isLoading(rows) && <Loader />}
        {isFailure(rows) && <span>{i18n.common.loadingFailed}</span>}
        {isSuccess(rows) && (
          <>
            <ReportDownload
              data={rows.data}
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
              <Table.Head>
                <Table.Row>
                  <Table.Th>{i18n.reports.common.careAreaName}</Table.Th>
                  <Table.Th>{i18n.reports.common.unitName}</Table.Th>
                  <Table.Th>{i18n.reports.common.childName}</Table.Th>
                  <Table.Th>
                    {i18n.reports.missingServiceNeed.daysWithoutServiceNeed}
                  </Table.Th>
                </Table.Row>
              </Table.Head>
              <Table.Body>
                {filteredRows.map((row: MissingServiceNeedReportRow) => (
                  <Table.Row key={`${row.unitId}:${row.childId}`}>
                    <Table.Td>{row.careAreaName}</Table.Td>
                    <Table.Td>
                      <Link to={`/units/${row.unitId}`}>{row.unitName}</Link>
                    </Table.Td>
                    <Table.Td>
                      <Link to={`/child-information/${row.childId}`}>
                        {row.lastName} {row.firstName}
                      </Link>
                    </Table.Td>
                    <Table.Td>{row.daysWithoutServiceNeed}</Table.Td>
                  </Table.Row>
                ))}
              </Table.Body>
            </TableScrollable>
            <RowCountInfo rowCount={filteredRows.length} />
          </>
        )}
      </ContentArea>
    </Container>
  )
}

export default MissingServiceNeed
