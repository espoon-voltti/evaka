// SPDX-FileCopyrightText: 2017-2022 City of Espoo
//
// SPDX-License-Identifier: LGPL-2.1-or-later

import React, { useCallback, useEffect, useMemo, useState } from 'react'
import { Link } from 'react-router-dom'
import styled from 'styled-components'

import { wrapResult } from 'lib-common/api'
import { MissingServiceNeedReportRow } from 'lib-common/generated/api-types/reports'
import LocalDate from 'lib-common/local-date'
import { Arg0 } from 'lib-common/types'
import { useApiState } from 'lib-common/utils/useRestApi'
import Title from 'lib-components/atoms/Title'
import ReturnButton from 'lib-components/atoms/buttons/ReturnButton'
import Combobox from 'lib-components/atoms/dropdowns/Combobox'
import { Container, ContentArea } from 'lib-components/layout/Container'
import { Tbody, Td, Th, Thead, Tr } from 'lib-components/layout/Table'
import {
  DatePickerClearableDeprecated,
  DatePickerDeprecated
} from 'lib-components/molecules/DatePickerDeprecated'
import { Gap } from 'lib-components/white-space'

import ReportDownload from '../../components/reports/ReportDownload'
import { getMissingServiceNeedReport } from '../../generated/api-clients/reports'
import { useTranslation } from '../../state/i18n'
import { distinct } from '../../utils'
import { renderResult } from '../async-rendering'

import { FilterLabel, FilterRow, RowCountInfo, TableScrollable } from './common'

const getMissingServiceNeedReportResult = wrapResult(
  getMissingServiceNeedReport
)

type MissingServiceNeedReportFilters = Arg0<typeof getMissingServiceNeedReport>

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
  const [rows] = useApiState(
    () => getMissingServiceNeedReportResult(filters),
    [filters]
  )

  const [displayFilters, setDisplayFilters] =
    useState<DisplayFilters>(emptyDisplayFilters)
  const displayFilter = useCallback(
    (row: MissingServiceNeedReportRow): boolean =>
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
          <DatePickerDeprecated
            date={filters.from}
            onChange={(from) => setFilters({ ...filters, from })}
          />
        </FilterRow>
        <FilterRow>
          <FilterLabel>{i18n.reports.common.endDate}</FilterLabel>
          <DatePickerClearableDeprecated
            date={filters.to}
            onChange={(to) => setFilters({ ...filters, to })}
            onCleared={() => setFilters({ ...filters, to: null })}
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
                headers={[
                  { label: 'Palvelualue', key: 'careAreaName' },
                  { label: 'Yksikön nimi', key: 'unitName' },
                  { label: 'Lapsen sukunimi', key: 'lastName' },
                  { label: 'Lapsen etunimi', key: 'firstName' },
                  {
                    label: 'Puutteellisia päiviä',
                    key: 'daysWithoutServiceNeed'
                  }
                ]}
                filename={`Puuttuvat palveluntarpeet ${filters.from.formatIso()}-${
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
