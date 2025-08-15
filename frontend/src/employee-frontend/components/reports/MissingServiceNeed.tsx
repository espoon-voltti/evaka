// SPDX-FileCopyrightText: 2017-2022 City of Espoo
//
// SPDX-License-Identifier: LGPL-2.1-or-later

import React, { useCallback, useEffect, useMemo, useState } from 'react'
import styled from 'styled-components'
import { Link } from 'wouter'

import type { MissingServiceNeedReportResultRow } from 'lib-common/generated/api-types/reports'
import LocalDate from 'lib-common/local-date'
import { constantQuery, useQueryResult } from 'lib-common/query'
import Title from 'lib-components/atoms/Title'
import ReturnButton from 'lib-components/atoms/buttons/ReturnButton'
import Combobox from 'lib-components/atoms/dropdowns/Combobox'
import { Container, ContentArea } from 'lib-components/layout/Container'
import { Tbody, Td, Th, Thead, Tr } from 'lib-components/layout/Table'
import { PersonName } from 'lib-components/molecules/PersonNames'
import DatePicker from 'lib-components/molecules/date-picker/DatePicker'
import { Gap } from 'lib-components/white-space'

import { useTranslation } from '../../state/i18n'
import { distinct } from '../../utils'
import { renderResult } from '../async-rendering'

import ReportDownload from './ReportDownload'
import { FilterLabel, FilterRow, RowCountInfo, TableScrollable } from './common'
import { missingServiceNeedReportQuery } from './queries'

interface MissingServiceNeedReportFilters {
  from: LocalDate | null
  to: LocalDate | null
}

interface DisplayFilters {
  careArea: string
}

const emptyDisplayFilters: DisplayFilters = {
  careArea: ''
}

const Wrapper = styled.div`
  width: 100%;
`

export default React.memo(function MissingServiceNeed() {
  const { i18n } = useTranslation()
  const [filters, setFilters] = useState<MissingServiceNeedReportFilters>({
    from: LocalDate.todayInSystemTz().subMonths(1).withDate(1),
    to: LocalDate.todayInSystemTz().addMonths(2).lastDayOfMonth()
  })
  const rows = useQueryResult(
    filters.from !== null
      ? missingServiceNeedReportQuery({ from: filters.from, to: filters.to })
      : constantQuery([])
  )

  const [displayFilters, setDisplayFilters] =
    useState<DisplayFilters>(emptyDisplayFilters)
  const displayFilter = useCallback(
    (row: MissingServiceNeedReportResultRow): boolean =>
      !(
        displayFilters.careArea && row.careAreaName !== displayFilters.careArea
      ),
    [displayFilters.careArea]
  )

  useEffect(() => {
    setDisplayFilters(emptyDisplayFilters)
  }, [filters])

  const filteredRows = useMemo(
    () => rows.map((rs) => rs.filter(displayFilter)),
    [rows, displayFilter]
  )

  return (
    <Container>
      <ReturnButton label={i18n.common.goBack} />
      <ContentArea opaque>
        <Title size={1}>{i18n.reports.missingServiceNeed.title}</Title>

        <FilterRow>
          <FilterLabel>{i18n.reports.common.startDate}</FilterLabel>
          <DatePicker
            date={filters.from}
            onChange={(from) => setFilters({ ...filters, from })}
            locale="fi"
          />
        </FilterRow>
        <FilterRow>
          <FilterLabel>{i18n.reports.common.endDate}</FilterLabel>
          <DatePicker
            date={filters.to}
            onChange={(to) => setFilters({ ...filters, to })}
            locale="fi"
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

        {renderResult(filteredRows, (filteredRows) =>
          filteredRows.length > 0 ? (
            <>
              <ReportDownload
                data={filteredRows}
                columns={[
                  { label: 'Palvelualue', value: (row) => row.careAreaName },
                  { label: 'Yksikön nimi', value: (row) => row.unitName },
                  { label: 'Lapsen sukunimi', value: (row) => row.lastName },
                  { label: 'Lapsen etunimi', value: (row) => row.firstName },
                  {
                    label: 'Puutteellisia päiviä',
                    value: (row) => row.daysWithoutServiceNeed
                  },
                  {
                    label: i18n.reports.missingServiceNeed.defaultOption,
                    value: (row) => row.defaultOption?.nameFi ?? ''
                  }
                ]}
                filename={`Puuttuvat palveluntarpeet ${filters.from?.formatIso()}-${
                  filters.to?.formatIso() ?? ''
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
                    <Th>{i18n.reports.missingServiceNeed.defaultOption}</Th>
                  </Tr>
                </Thead>
                <Tbody>
                  {filteredRows.map((row) => (
                    <Tr
                      key={`${row.unitId}:${row.childId}:${row.defaultOption?.id ?? ''}`}
                    >
                      <Td>{row.careAreaName}</Td>
                      <Td>
                        <Link to={`/units/${row.unitId}`}>{row.unitName}</Link>
                      </Td>
                      <Td>
                        <Link to={`/child-information/${row.childId}`}>
                          <PersonName person={row} format="Last First" />
                        </Link>
                      </Td>
                      <Td>{row.daysWithoutServiceNeed}</Td>
                      <Td>{row.defaultOption?.nameFi ?? '-'}</Td>
                    </Tr>
                  ))}
                </Tbody>
              </TableScrollable>
              <RowCountInfo rowCount={filteredRows.length} />
            </>
          ) : (
            <>
              <Gap size="L" />
              <div>{i18n.common.noResults}</div>
            </>
          )
        )}
      </ContentArea>
    </Container>
  )
})
