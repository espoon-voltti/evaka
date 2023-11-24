// SPDX-FileCopyrightText: 2017-2022 City of Espoo
//
// SPDX-License-Identifier: LGPL-2.1-or-later

import React, { useEffect, useMemo, useState } from 'react'
import { Link } from 'react-router-dom'
import styled from 'styled-components'

import { Loading, Result } from 'lib-common/api'
import { ChildAgeLanguageReportRow } from 'lib-common/generated/api-types/reports'
import LocalDate from 'lib-common/local-date'
import Loader from 'lib-components/atoms/Loader'
import Title from 'lib-components/atoms/Title'
import ReturnButton from 'lib-components/atoms/buttons/ReturnButton'
import Combobox from 'lib-components/atoms/dropdowns/Combobox'
import { Container, ContentArea } from 'lib-components/layout/Container'
import { Tbody, Td, Th, Thead, Tr } from 'lib-components/layout/Table'
import { DatePickerDeprecated } from 'lib-components/molecules/DatePickerDeprecated'

import { DateFilters, getChildAgeLanguageReport } from '../../api/reports'
import ReportDownload from '../../components/reports/ReportDownload'
import { useTranslation } from '../../state/i18n'
import { distinct, reducePropertySum } from '../../utils'

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

export default React.memo(function ChildAgeLanguage() {
  const { i18n } = useTranslation()
  const [rows, setRows] = useState<Result<ChildAgeLanguageReportRow[]>>(
    Loading.of()
  )
  const [filters, setFilters] = useState<DateFilters>({
    date: LocalDate.todayInSystemTz()
  })

  const [displayFilters, setDisplayFilters] =
    useState<DisplayFilters>(emptyDisplayFilters)
  const displayFilter = (row: ChildAgeLanguageReportRow): boolean =>
    !(displayFilters.careArea && row.careAreaName !== displayFilters.careArea)

  useEffect(() => {
    setRows(Loading.of())
    setDisplayFilters(emptyDisplayFilters)
    void getChildAgeLanguageReport(filters).then(setRows)
  }, [filters])

  const filteredRows: ChildAgeLanguageReportRow[] = useMemo(
    () => rows.map((rs) => rs.filter(displayFilter)).getOrElse([]),
    [rows, displayFilters] // eslint-disable-line react-hooks/exhaustive-deps
  )

  return (
    <Container>
      <ReturnButton label={i18n.common.goBack} />
      <ContentArea opaque>
        <Title size={1}>{i18n.reports.childAgeLanguage.title}</Title>
        <FilterRow>
          <FilterLabel>{i18n.reports.common.date}</FilterLabel>
          <DatePickerDeprecated
            date={filters.date}
            onChange={(date) => setFilters({ date })}
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
              data={filteredRows.map((row) => ({
                ...row,
                unitType: row.unitType
                  ? i18n.reports.common.unitTypes[row.unitType]
                  : '',
                unitProviderType:
                  i18n.reports.common.unitProviderTypes[row.unitProviderType]
              }))}
              headers={[
                { label: 'Palvelualue', key: 'careAreaName' },
                { label: 'Yksikkö', key: 'unitName' },
                { label: 'Toimintamuoto', key: 'unitType' },
                { label: 'Järjestemäismuoto', key: 'unitProviderType' },

                { label: 'fi 0v', key: 'fi_0y' },
                { label: 'fi 1v', key: 'fi_1y' },
                { label: 'fi 2v', key: 'fi_2y' },
                { label: 'fi 3v', key: 'fi_3y' },
                { label: 'fi 4v', key: 'fi_4y' },
                { label: 'fi 5v', key: 'fi_5y' },
                { label: 'fi 6v', key: 'fi_6y' },
                { label: 'fi 7v', key: 'fi_7y' },

                { label: 'sv 0v', key: 'sv_0y' },
                { label: 'sv 1v', key: 'sv_1y' },
                { label: 'sv 2v', key: 'sv_2y' },
                { label: 'sv 3v', key: 'sv_3y' },
                { label: 'sv 4v', key: 'sv_4y' },
                { label: 'sv 5v', key: 'sv_5y' },
                { label: 'sv 6v', key: 'sv_6y' },
                { label: 'sv 7v', key: 'sv_7y' },

                { label: 'muu 0v', key: 'other_0y' },
                { label: 'muu 1v', key: 'other_1y' },
                { label: 'muu 2v', key: 'other_2y' },
                { label: 'muu 3v', key: 'other_3y' },
                { label: 'muu 4v', key: 'other_4y' },
                { label: 'muu 5v', key: 'other_5y' },
                { label: 'muu 6v', key: 'other_6y' },
                { label: 'muu 7v', key: 'other_7y' }
              ]}
              filename={`Lapsien kielet ja iät yksiköissä ${filters.date.formatIso()}.csv`}
            />
            <TableScrollable>
              <Thead>
                <Tr>
                  <Th>{i18n.reports.common.careAreaName}</Th>
                  <Th>{i18n.reports.common.unitName}</Th>
                  <Th>{i18n.reports.common.unitType}</Th>
                  <Th>{i18n.reports.common.unitProviderType}</Th>
                  <Th>Suomi</Th>
                  <Th>0v</Th>
                  <Th>1v</Th>
                  <Th>2v</Th>
                  <Th>3v</Th>
                  <Th>4v</Th>
                  <Th>5v</Th>
                  <Th>6v</Th>
                  <Th>7v</Th>
                  <Th>Ruotsi</Th>
                  <Th>0v</Th>
                  <Th>1v</Th>
                  <Th>2v</Th>
                  <Th>3v</Th>
                  <Th>4v</Th>
                  <Th>5v</Th>
                  <Th>6v</Th>
                  <Th>7v</Th>
                  <Th>Muut</Th>
                  <Th>0v</Th>
                  <Th>1v</Th>
                  <Th>2v</Th>
                  <Th>3v</Th>
                  <Th>4v</Th>
                  <Th>5v</Th>
                  <Th>6v</Th>
                  <Th>7v</Th>
                </Tr>
              </Thead>
              <Tbody>
                {filteredRows.map((row: ChildAgeLanguageReportRow) => (
                  <Tr key={row.unitName}>
                    <Td>{row.careAreaName}</Td>
                    <Td>
                      <Link to={`/units/${row.unitId}`}>{row.unitName}</Link>
                    </Td>
                    <Td>
                      {row.unitType
                        ? i18n.reports.common.unitTypes[row.unitType]
                        : ''}
                    </Td>
                    <Td>
                      {
                        i18n.reports.common.unitProviderTypes[
                          row.unitProviderType
                        ]
                      }
                    </Td>
                    <Td />
                    <Td>{row.fi_0y}</Td>
                    <Td>{row.fi_1y}</Td>
                    <Td>{row.fi_2y}</Td>
                    <Td>{row.fi_3y}</Td>
                    <Td>{row.fi_4y}</Td>
                    <Td>{row.fi_5y}</Td>
                    <Td>{row.fi_6y}</Td>
                    <Td>{row.fi_7y}</Td>
                    <Td />
                    <Td>{row.sv_0y}</Td>
                    <Td>{row.sv_1y}</Td>
                    <Td>{row.sv_2y}</Td>
                    <Td>{row.sv_3y}</Td>
                    <Td>{row.sv_4y}</Td>
                    <Td>{row.sv_5y}</Td>
                    <Td>{row.sv_6y}</Td>
                    <Td>{row.sv_7y}</Td>
                    <Td />
                    <Td>{row.other_0y}</Td>
                    <Td>{row.other_1y}</Td>
                    <Td>{row.other_2y}</Td>
                    <Td>{row.other_3y}</Td>
                    <Td>{row.other_4y}</Td>
                    <Td>{row.other_5y}</Td>
                    <Td>{row.other_6y}</Td>
                    <Td>{row.other_7y}</Td>
                  </Tr>
                ))}
              </Tbody>
              <TableFooter>
                <Tr>
                  <Td className="bold">{i18n.reports.common.total}</Td>
                  <Td />
                  <Td />
                  <Td />
                  <Td />
                  <Td>{reducePropertySum(filteredRows, (r) => r.fi_0y)}</Td>
                  <Td>{reducePropertySum(filteredRows, (r) => r.fi_1y)}</Td>
                  <Td>{reducePropertySum(filteredRows, (r) => r.fi_2y)}</Td>
                  <Td>{reducePropertySum(filteredRows, (r) => r.fi_3y)}</Td>
                  <Td>{reducePropertySum(filteredRows, (r) => r.fi_4y)}</Td>
                  <Td>{reducePropertySum(filteredRows, (r) => r.fi_5y)}</Td>
                  <Td>{reducePropertySum(filteredRows, (r) => r.fi_6y)}</Td>
                  <Td>{reducePropertySum(filteredRows, (r) => r.fi_7y)}</Td>
                  <Td />
                  <Td>{reducePropertySum(filteredRows, (r) => r.sv_0y)}</Td>
                  <Td>{reducePropertySum(filteredRows, (r) => r.sv_1y)}</Td>
                  <Td>{reducePropertySum(filteredRows, (r) => r.sv_2y)}</Td>
                  <Td>{reducePropertySum(filteredRows, (r) => r.sv_3y)}</Td>
                  <Td>{reducePropertySum(filteredRows, (r) => r.sv_4y)}</Td>
                  <Td>{reducePropertySum(filteredRows, (r) => r.sv_5y)}</Td>
                  <Td>{reducePropertySum(filteredRows, (r) => r.sv_6y)}</Td>
                  <Td>{reducePropertySum(filteredRows, (r) => r.sv_7y)}</Td>
                  <Td />
                  <Td>{reducePropertySum(filteredRows, (r) => r.other_0y)}</Td>
                  <Td>{reducePropertySum(filteredRows, (r) => r.other_1y)}</Td>
                  <Td>{reducePropertySum(filteredRows, (r) => r.other_2y)}</Td>
                  <Td>{reducePropertySum(filteredRows, (r) => r.other_3y)}</Td>
                  <Td>{reducePropertySum(filteredRows, (r) => r.other_4y)}</Td>
                  <Td>{reducePropertySum(filteredRows, (r) => r.other_5y)}</Td>
                  <Td>{reducePropertySum(filteredRows, (r) => r.other_6y)}</Td>
                  <Td>{reducePropertySum(filteredRows, (r) => r.other_7y)}</Td>
                </Tr>
              </TableFooter>
            </TableScrollable>
          </>
        )}
      </ContentArea>
    </Container>
  )
})
