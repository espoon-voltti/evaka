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
import { ChildAgeLanguageReportRow } from '~types/reports'
import { DateFilters, getChildAgeLanguageReport } from '~api/reports'
import ReturnButton from 'components/shared/atoms/buttons/ReturnButton'
import ReportDownload from '~components/reports/ReportDownload'
import { DatePicker } from '~components/common/DatePicker'
import {
  FilterLabel,
  FilterRow,
  TableFooter,
  TableScrollable
} from '~components/reports/common'
import { distinct, reducePropertySum } from 'utils'
import LocalDate from '@evaka/lib-common/src/local-date'

interface DisplayFilters {
  careArea: string
}

const emptyDisplayFilters: DisplayFilters = {
  careArea: ''
}

const Wrapper = styled.div`
  width: 100%;
`

function ChildAgeLanguage() {
  const { i18n } = useTranslation()
  const [rows, setRows] = useState<Result<ChildAgeLanguageReportRow[]>>(
    Loading()
  )
  const [filters, setFilters] = useState<DateFilters>({
    date: LocalDate.today()
  })

  const [displayFilters, setDisplayFilters] = useState<DisplayFilters>(
    emptyDisplayFilters
  )
  const displayFilter = (row: ChildAgeLanguageReportRow): boolean => {
    return !(
      displayFilters.careArea && row.careAreaName !== displayFilters.careArea
    )
  }

  useEffect(() => {
    setRows(Loading())
    setDisplayFilters(emptyDisplayFilters)
    void getChildAgeLanguageReport(filters).then(setRows)
  }, [filters])

  const filteredRows = useMemo(
    () => (isSuccess(rows) ? rows.data.filter(displayFilter) : []),
    [rows, displayFilters]
  )

  return (
    <Container>
      <ReturnButton />
      <ContentArea opaque>
        <Title size={1}>{i18n.reports.childAgeLanguage.title}</Title>
        <FilterRow>
          <FilterLabel>{i18n.reports.common.date}</FilterLabel>
          <DatePicker
            date={filters.date}
            onChange={(date) => setFilters({ date })}
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
                  : null
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
                  <Td>{reducePropertySum(filteredRows, (r) => r.other_0y)}</Td>
                  <Td>{reducePropertySum(filteredRows, (r) => r.other_1y)}</Td>
                  <Td>{reducePropertySum(filteredRows, (r) => r.other_2y)}</Td>
                  <Td>{reducePropertySum(filteredRows, (r) => r.other_3y)}</Td>
                  <Td>{reducePropertySum(filteredRows, (r) => r.other_4y)}</Td>
                  <Td>{reducePropertySum(filteredRows, (r) => r.other_5y)}</Td>
                  <Td>{reducePropertySum(filteredRows, (r) => r.other_6y)}</Td>
                  <Td>{reducePropertySum(filteredRows, (r) => r.other_7y)}</Td>
                  <Td />
                  <Td>{reducePropertySum(filteredRows, (r) => r.other_0y)}</Td>
                  <Td>{reducePropertySum(filteredRows, (r) => r.other_1y)}</Td>
                  <Td>{reducePropertySum(filteredRows, (r) => r.other_2y)}</Td>
                  <Td>{reducePropertySum(filteredRows, (r) => r.other_3y)}</Td>
                  <Td>{reducePropertySum(filteredRows, (r) => r.other_4y)}</Td>
                  <Td>{reducePropertySum(filteredRows, (r) => r.other_5y)}</Td>
                  <Td>{reducePropertySum(filteredRows, (r) => r.other_6y)}</Td>
                  <Td>{reducePropertySum(filteredRows, (r) => r.other_7y)}</Td>
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
}

export default ChildAgeLanguage
