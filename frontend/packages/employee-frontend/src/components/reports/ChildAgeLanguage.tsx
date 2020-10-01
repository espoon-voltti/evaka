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
import SelectWithIcon from 'components/common/Select'
import { Link } from 'react-router-dom'

interface DisplayFilters {
  careArea: string
}

const emptyDisplayFilters: DisplayFilters = {
  careArea: ''
}

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
              <Table.Head>
                <Table.Row>
                  <Table.Th>{i18n.reports.common.careAreaName}</Table.Th>
                  <Table.Th>{i18n.reports.common.unitName}</Table.Th>
                  <Table.Th>{i18n.reports.common.unitType}</Table.Th>
                  <Table.Th>{i18n.reports.common.unitProviderType}</Table.Th>
                  <Table.Th>Suomi</Table.Th>
                  <Table.Th>0v</Table.Th>
                  <Table.Th>1v</Table.Th>
                  <Table.Th>2v</Table.Th>
                  <Table.Th>3v</Table.Th>
                  <Table.Th>4v</Table.Th>
                  <Table.Th>5v</Table.Th>
                  <Table.Th>6v</Table.Th>
                  <Table.Th>7v</Table.Th>
                  <Table.Th>Ruotsi</Table.Th>
                  <Table.Th>0v</Table.Th>
                  <Table.Th>1v</Table.Th>
                  <Table.Th>2v</Table.Th>
                  <Table.Th>3v</Table.Th>
                  <Table.Th>4v</Table.Th>
                  <Table.Th>5v</Table.Th>
                  <Table.Th>6v</Table.Th>
                  <Table.Th>7v</Table.Th>
                  <Table.Th>Muut</Table.Th>
                  <Table.Th>0v</Table.Th>
                  <Table.Th>1v</Table.Th>
                  <Table.Th>2v</Table.Th>
                  <Table.Th>3v</Table.Th>
                  <Table.Th>4v</Table.Th>
                  <Table.Th>5v</Table.Th>
                  <Table.Th>6v</Table.Th>
                  <Table.Th>7v</Table.Th>
                </Table.Row>
              </Table.Head>
              <Table.Body>
                {filteredRows.map((row: ChildAgeLanguageReportRow) => (
                  <Table.Row key={row.unitName}>
                    <Table.Td>{row.careAreaName}</Table.Td>
                    <Table.Td>
                      <Link to={`/units/${row.unitId}`}>{row.unitName}</Link>
                    </Table.Td>
                    <Table.Td>
                      {row.unitType
                        ? i18n.reports.common.unitTypes[row.unitType]
                        : ''}
                    </Table.Td>
                    <Table.Td>
                      {
                        i18n.reports.common.unitProviderTypes[
                          row.unitProviderType
                        ]
                      }
                    </Table.Td>
                    <Table.Td />
                    <Table.Td>{row.fi_0y}</Table.Td>
                    <Table.Td>{row.fi_1y}</Table.Td>
                    <Table.Td>{row.fi_2y}</Table.Td>
                    <Table.Td>{row.fi_3y}</Table.Td>
                    <Table.Td>{row.fi_4y}</Table.Td>
                    <Table.Td>{row.fi_5y}</Table.Td>
                    <Table.Td>{row.fi_6y}</Table.Td>
                    <Table.Td>{row.fi_7y}</Table.Td>
                    <Table.Td />
                    <Table.Td>{row.sv_0y}</Table.Td>
                    <Table.Td>{row.sv_1y}</Table.Td>
                    <Table.Td>{row.sv_2y}</Table.Td>
                    <Table.Td>{row.sv_3y}</Table.Td>
                    <Table.Td>{row.sv_4y}</Table.Td>
                    <Table.Td>{row.sv_5y}</Table.Td>
                    <Table.Td>{row.sv_6y}</Table.Td>
                    <Table.Td>{row.sv_7y}</Table.Td>
                    <Table.Td />
                    <Table.Td>{row.other_0y}</Table.Td>
                    <Table.Td>{row.other_1y}</Table.Td>
                    <Table.Td>{row.other_2y}</Table.Td>
                    <Table.Td>{row.other_3y}</Table.Td>
                    <Table.Td>{row.other_4y}</Table.Td>
                    <Table.Td>{row.other_5y}</Table.Td>
                    <Table.Td>{row.other_6y}</Table.Td>
                    <Table.Td>{row.other_7y}</Table.Td>
                  </Table.Row>
                ))}
              </Table.Body>
              <TableFooter>
                <Table.Row>
                  <Table.Td className="bold">
                    {i18n.reports.common.total}
                  </Table.Td>
                  <Table.Td />
                  <Table.Td />
                  <Table.Td />
                  <Table.Td />
                  <Table.Td>
                    {reducePropertySum(filteredRows, (r) => r.other_0y)}
                  </Table.Td>
                  <Table.Td>
                    {reducePropertySum(filteredRows, (r) => r.other_1y)}
                  </Table.Td>
                  <Table.Td>
                    {reducePropertySum(filteredRows, (r) => r.other_2y)}
                  </Table.Td>
                  <Table.Td>
                    {reducePropertySum(filteredRows, (r) => r.other_3y)}
                  </Table.Td>
                  <Table.Td>
                    {reducePropertySum(filteredRows, (r) => r.other_4y)}
                  </Table.Td>
                  <Table.Td>
                    {reducePropertySum(filteredRows, (r) => r.other_5y)}
                  </Table.Td>
                  <Table.Td>
                    {reducePropertySum(filteredRows, (r) => r.other_6y)}
                  </Table.Td>
                  <Table.Td>
                    {reducePropertySum(filteredRows, (r) => r.other_7y)}
                  </Table.Td>
                  <Table.Td />
                  <Table.Td>
                    {reducePropertySum(filteredRows, (r) => r.other_0y)}
                  </Table.Td>
                  <Table.Td>
                    {reducePropertySum(filteredRows, (r) => r.other_1y)}
                  </Table.Td>
                  <Table.Td>
                    {reducePropertySum(filteredRows, (r) => r.other_2y)}
                  </Table.Td>
                  <Table.Td>
                    {reducePropertySum(filteredRows, (r) => r.other_3y)}
                  </Table.Td>
                  <Table.Td>
                    {reducePropertySum(filteredRows, (r) => r.other_4y)}
                  </Table.Td>
                  <Table.Td>
                    {reducePropertySum(filteredRows, (r) => r.other_5y)}
                  </Table.Td>
                  <Table.Td>
                    {reducePropertySum(filteredRows, (r) => r.other_6y)}
                  </Table.Td>
                  <Table.Td>
                    {reducePropertySum(filteredRows, (r) => r.other_7y)}
                  </Table.Td>
                  <Table.Td />
                  <Table.Td>
                    {reducePropertySum(filteredRows, (r) => r.other_0y)}
                  </Table.Td>
                  <Table.Td>
                    {reducePropertySum(filteredRows, (r) => r.other_1y)}
                  </Table.Td>
                  <Table.Td>
                    {reducePropertySum(filteredRows, (r) => r.other_2y)}
                  </Table.Td>
                  <Table.Td>
                    {reducePropertySum(filteredRows, (r) => r.other_3y)}
                  </Table.Td>
                  <Table.Td>
                    {reducePropertySum(filteredRows, (r) => r.other_4y)}
                  </Table.Td>
                  <Table.Td>
                    {reducePropertySum(filteredRows, (r) => r.other_5y)}
                  </Table.Td>
                  <Table.Td>
                    {reducePropertySum(filteredRows, (r) => r.other_6y)}
                  </Table.Td>
                  <Table.Td>
                    {reducePropertySum(filteredRows, (r) => r.other_7y)}
                  </Table.Td>
                </Table.Row>
              </TableFooter>
            </TableScrollable>
          </>
        )}
      </ContentArea>
    </Container>
  )
}

export default ChildAgeLanguage
