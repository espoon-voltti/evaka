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
import ReturnButton from 'components/shared/atoms/buttons/ReturnButton'
import ReportDownload from '~components/reports/ReportDownload'
import { PartnersInDifferentAddressReportRow } from '~types/reports'
import { getPartnersInDifferentAddressReport } from '~api/reports'
import {
  FilterLabel,
  FilterRow,
  RowCountInfo,
  TableScrollable
} from 'components/reports/common'
import SelectWithIcon from 'components/common/Select'
import { distinct } from 'utils'

interface DisplayFilters {
  careArea: string
}

const emptyDisplayFilters: DisplayFilters = {
  careArea: ''
}

function PartnersInDifferentAddress() {
  const { i18n } = useTranslation()
  const [rows, setRows] = useState<
    Result<PartnersInDifferentAddressReportRow[]>
  >(Loading())

  const [displayFilters, setDisplayFilters] = useState<DisplayFilters>(
    emptyDisplayFilters
  )
  const displayFilter = (row: PartnersInDifferentAddressReportRow): boolean => {
    return !(
      displayFilters.careArea && row.careAreaName !== displayFilters.careArea
    )
  }

  useEffect(() => {
    setRows(Loading())
    setDisplayFilters(emptyDisplayFilters)
    void getPartnersInDifferentAddressReport().then(setRows)
  }, [])

  const filteredRows = useMemo(
    () => (isSuccess(rows) ? rows.data.filter(displayFilter) : []),
    [rows, displayFilters]
  )

  return (
    <Container>
      <ReturnButton />
      <ContentArea opaque>
        <Title size={1}>{i18n.reports.partnersInDifferentAddress.title}</Title>

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
                { label: 'Sukunimi', key: 'firstName1' },
                { label: 'Etunimi', key: 'lastName1' },
                { label: 'Osoite', key: 'address1' },
                { label: 'Puolison sukunimi', key: 'firstName2' },
                { label: 'Puolison etunimi', key: 'lastName2' },
                { label: 'Puolison osoite', key: 'address2' }
              ]}
              filename="Puolisot eri osoitteissa.csv"
            />
            <TableScrollable>
              <Table.Head>
                <Table.Row>
                  <Table.Th>{i18n.reports.common.careAreaName}</Table.Th>
                  <Table.Th>{i18n.reports.common.unitName}</Table.Th>
                  <Table.Th>
                    {i18n.reports.partnersInDifferentAddress.person1}
                  </Table.Th>
                  <Table.Th>
                    {i18n.reports.partnersInDifferentAddress.address1}
                  </Table.Th>
                  <Table.Th>
                    {i18n.reports.partnersInDifferentAddress.person2}
                  </Table.Th>
                  <Table.Th>
                    {i18n.reports.partnersInDifferentAddress.address2}
                  </Table.Th>
                </Table.Row>
              </Table.Head>
              <Table.Body>
                {filteredRows.map(
                  (row: PartnersInDifferentAddressReportRow) => (
                    <Table.Row key={`${row.unitId}:${row.personId1}`}>
                      <Table.Td>{row.careAreaName}</Table.Td>
                      <Table.Td>
                        <Link to={`/units/${row.unitId}`}>{row.unitName}</Link>
                      </Table.Td>
                      <Table.Td>
                        <Link to={`/profile/${row.personId1}`}>
                          {row.lastName1} {row.firstName1}
                        </Link>
                      </Table.Td>
                      <Table.Td>{row.address1}</Table.Td>
                      <Table.Td>
                        <Link to={`/profile/${row.personId2}`}>
                          {row.lastName2} {row.firstName2}
                        </Link>
                      </Table.Td>
                      <Table.Td>{row.address2}</Table.Td>
                    </Table.Row>
                  )
                )}
              </Table.Body>
            </TableScrollable>
            <RowCountInfo rowCount={filteredRows.length} />
          </>
        )}
      </ContentArea>
    </Container>
  )
}

export default PartnersInDifferentAddress
