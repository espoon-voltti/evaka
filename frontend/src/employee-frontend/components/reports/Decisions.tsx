// SPDX-FileCopyrightText: 2017-2022 City of Espoo
//
// SPDX-License-Identifier: LGPL-2.1-or-later

import React, { useEffect, useMemo, useState } from 'react'
import { Link } from 'react-router-dom'
import styled from 'styled-components'

import { Loading, Result } from 'lib-common/api'
import { DecisionsReportRow } from 'lib-common/generated/api-types/reports'
import LocalDate from 'lib-common/local-date'
import { useRestApi } from 'lib-common/utils/useRestApi'
import Loader from 'lib-components/atoms/Loader'
import Title from 'lib-components/atoms/Title'
import ReturnButton from 'lib-components/atoms/buttons/ReturnButton'
import Combobox from 'lib-components/atoms/dropdowns/Combobox'
import { Container, ContentArea } from 'lib-components/layout/Container'
import { Tbody, Td, Th, Thead, Tr } from 'lib-components/layout/Table'
import { InfoBox } from 'lib-components/molecules/MessageBoxes'
import DatePicker from 'lib-components/molecules/date-picker/DatePicker'
import { Gap } from 'lib-components/white-space'

import { getDecisionsReport, PeriodFilters } from '../../api/reports'
import ReportDownload from '../../components/reports/ReportDownload'
import { useTranslation } from '../../state/i18n'
import { distinct, reducePropertySum } from '../../utils'
import { FlexRow } from '../common/styled/containers'

import { FilterLabel, FilterRow, TableFooter, TableScrollable } from './common'

interface DisplayFilters {
  careArea: string
}

const emptyDisplayFilters: DisplayFilters = {
  careArea: ''
}

const Wrapper = styled.div`
  width: 100%;
`

export default React.memo(function Decisions() {
  const { i18n } = useTranslation()
  const [rows, setRows] = useState<Result<DecisionsReportRow[]>>(Loading.of())
  const [filters, setFilters] = useState<PeriodFilters>({
    from: LocalDate.todayInSystemTz(),
    to: LocalDate.todayInSystemTz().addMonths(4)
  })

  const [displayFilters, setDisplayFilters] =
    useState<DisplayFilters>(emptyDisplayFilters)
  const displayFilter = (row: DecisionsReportRow): boolean => {
    return !(
      displayFilters.careArea && row.careAreaName !== displayFilters.careArea
    )
  }

  const loadReport = useRestApi(getDecisionsReport, setRows)

  useEffect(() => {
    void loadReport(filters)
  }, [loadReport, filters])

  const filteredRows: DecisionsReportRow[] = useMemo(
    () => rows.map((rs) => rs.filter(displayFilter)).getOrElse([]),
    [rows, displayFilters] // eslint-disable-line react-hooks/exhaustive-deps
  )

  return (
    <Container>
      <ReturnButton label={i18n.common.goBack} />
      <ContentArea opaque>
        <Title size={1}>{i18n.reports.decisions.title}</Title>

        <FilterRow>
          <FilterLabel id="sent-date">
            {i18n.reports.decisions.sentDate}
          </FilterLabel>
          <FlexRow>
            <DatePicker
              date={filters.from}
              onChange={(from) => from && setFilters({ ...filters, from })}
              data-qa="datepicker-from"
              locale="fi"
              errorTexts={i18n.validationErrors}
              labels={i18n.common.datePicker}
              aria-labelledby="sent-date"
            />
            <span>{' - '}</span>
            <DatePicker
              date={filters.to}
              onChange={(to) => to && setFilters({ ...filters, to })}
              data-qa="datepicker-to"
              locale="fi"
              errorTexts={i18n.validationErrors}
              labels={i18n.common.datePicker}
              aria-labelledby="sent-date"
            />
          </FlexRow>
        </FilterRow>

        <FilterRow>
          <FilterLabel>{i18n.reports.common.careAreaName}</FilterLabel>
          <Wrapper data-qa="select-area">
            <Combobox
              items={[
                { label: i18n.common.all, value: '' },
                ...rows
                  .map((rs) =>
                    distinct(rs.map((row) => row.careAreaName)).map((s) => ({
                      value: s,
                      label: s
                    }))
                  )
                  .getOrElse([])
              ]}
              getItemLabel={(item) => item.label}
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
            />
          </Wrapper>
        </FilterRow>

        <Gap />
        <InfoBox message={i18n.reports.decisions.ageInfo} thin />

        {rows.isLoading && <Loader />}
        {rows.isFailure && <span>{i18n.common.loadingFailed}</span>}
        {rows.isSuccess && (
          <>
            <ReportDownload
              data={filteredRows.map((row) => ({
                ...row,
                providerType:
                  i18n.reports.common.unitProviderTypes[row.providerType]
              }))}
              headers={[
                {
                  label: i18n.reports.common.careAreaName,
                  key: 'careAreaName'
                },
                { label: i18n.reports.common.unitName, key: 'unitName' },
                {
                  label: i18n.reports.common.unitProviderType,
                  key: 'providerType'
                },
                {
                  label: i18n.reports.decisions.daycareUnder3,
                  key: 'daycareUnder3'
                },
                {
                  label: i18n.reports.decisions.daycareOver3,
                  key: 'daycareOver3'
                },
                { label: i18n.reports.decisions.preschool, key: 'preschool' },
                {
                  label: i18n.reports.decisions.preschoolDaycare,
                  key: 'preschoolDaycare'
                },
                {
                  label: i18n.reports.decisions.preparatory,
                  key: 'preparatory'
                },
                {
                  label: i18n.reports.decisions.preparatoryDaycare,
                  key: 'preparatoryDaycare'
                },
                { label: i18n.reports.decisions.club, key: 'club' },
                {
                  label: i18n.reports.decisions.preference1,
                  key: 'preference1'
                },
                {
                  label: i18n.reports.decisions.preference2,
                  key: 'preference2'
                },
                {
                  label: i18n.reports.decisions.preference3,
                  key: 'preference3'
                },
                {
                  label: i18n.reports.decisions.preferenceNone,
                  key: 'preferenceNone'
                },
                { label: i18n.reports.decisions.total, key: 'total' }
              ]}
              filename={`${
                i18n.reports.decisions.title
              } ${filters.from.formatIso()}-${filters.to.formatIso()}.csv`}
            />
            <TableScrollable data-qa="report-application-table">
              <Thead>
                <Tr>
                  <Th>{i18n.reports.common.careAreaName}</Th>
                  <Th>{i18n.reports.common.unitName}</Th>
                  <Th>{i18n.reports.common.unitProviderType}</Th>
                  <Th>{i18n.reports.decisions.daycareUnder3}</Th>
                  <Th>{i18n.reports.decisions.daycareOver3}</Th>
                  <Th>{i18n.reports.decisions.preschool}</Th>
                  <Th>{i18n.reports.decisions.preschoolDaycare}</Th>
                  <Th>{i18n.reports.decisions.preparatory}</Th>
                  <Th>{i18n.reports.decisions.preparatoryDaycare}</Th>
                  <Th>{i18n.reports.decisions.club}</Th>
                  <Th>{i18n.reports.decisions.preference1}</Th>
                  <Th>{i18n.reports.decisions.preference2}</Th>
                  <Th>{i18n.reports.decisions.preference3}</Th>
                  <Th>{i18n.reports.decisions.preferenceNone}</Th>
                  <Th>{i18n.reports.decisions.total}</Th>
                </Tr>
              </Thead>
              <Tbody>
                {filteredRows.map((row: DecisionsReportRow) => (
                  <Tr key={row.unitId}>
                    <Td data-qa="care-area-name">{row.careAreaName}</Td>
                    <Td>
                      <Link to={`/units/${row.unitId}`}>{row.unitName}</Link>
                    </Td>
                    <Td data-qa="unit-provider-type">
                      {i18n.reports.common.unitProviderTypes[row.providerType]}
                    </Td>
                    <Td>{row.daycareUnder3}</Td>
                    <Td>{row.daycareOver3}</Td>
                    <Td>{row.preschool}</Td>
                    <Td>{row.preschoolDaycare}</Td>
                    <Td>{row.preparatory}</Td>
                    <Td>{row.preparatoryDaycare}</Td>
                    <Td>{row.club}</Td>
                    <Td>{row.preference1}</Td>
                    <Td>{row.preference2}</Td>
                    <Td>{row.preference3}</Td>
                    <Td>{row.preferenceNone}</Td>
                    <Td>{row.total}</Td>
                  </Tr>
                ))}
              </Tbody>
              <TableFooter>
                <Tr>
                  <Td className="bold">{i18n.reports.common.total}</Td>
                  <Td />
                  <Td />
                  <Td>
                    {reducePropertySum(filteredRows, (r) => r.daycareUnder3)}
                  </Td>
                  <Td>
                    {reducePropertySum(filteredRows, (r) => r.daycareOver3)}
                  </Td>
                  <Td>{reducePropertySum(filteredRows, (r) => r.preschool)}</Td>
                  <Td>
                    {reducePropertySum(filteredRows, (r) => r.preschoolDaycare)}
                  </Td>
                  <Td>
                    {reducePropertySum(filteredRows, (r) => r.preparatory)}
                  </Td>
                  <Td>
                    {reducePropertySum(
                      filteredRows,
                      (r) => r.preparatoryDaycare
                    )}
                  </Td>
                  <Td>{reducePropertySum(filteredRows, (r) => r.club)}</Td>
                  <Td>
                    {reducePropertySum(filteredRows, (r) => r.preference1)}
                  </Td>
                  <Td>
                    {reducePropertySum(filteredRows, (r) => r.preference2)}
                  </Td>
                  <Td>
                    {reducePropertySum(filteredRows, (r) => r.preference3)}
                  </Td>
                  <Td>
                    {reducePropertySum(filteredRows, (r) => r.preferenceNone)}
                  </Td>
                  <Td>{reducePropertySum(filteredRows, (r) => r.total)}</Td>
                </Tr>
              </TableFooter>
            </TableScrollable>
          </>
        )}
      </ContentArea>
    </Container>
  )
})
