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
import { ChildrenInDifferentAddressReportRow } from '~types/reports'
import { getChildrenInDifferentAddressReport } from '~api/reports'
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

function ChildrenInDifferentAddress() {
  const { i18n } = useTranslation()
  const [rows, setRows] = useState<
    Result<ChildrenInDifferentAddressReportRow[]>
  >(Loading())

  const [displayFilters, setDisplayFilters] = useState<DisplayFilters>(
    emptyDisplayFilters
  )
  const displayFilter = (row: ChildrenInDifferentAddressReportRow): boolean => {
    return !(
      displayFilters.careArea && row.careAreaName !== displayFilters.careArea
    )
  }

  useEffect(() => {
    setRows(Loading())
    setDisplayFilters(emptyDisplayFilters)
    void getChildrenInDifferentAddressReport().then(setRows)
  }, [])

  const filteredRows = useMemo(
    () => (isSuccess(rows) ? rows.data.filter(displayFilter) : []),
    [rows, displayFilters]
  )

  return (
    <Container>
      <ReturnButton />
      <ContentArea opaque>
        <Title size={1}>{i18n.reports.childrenInDifferentAddress.title}</Title>

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
                { label: 'Päämiehen sukunimi', key: 'firstNameParent' },
                { label: 'Päämiehen etunimi', key: 'lastNameParent' },
                { label: 'Päämiehen osoite', key: 'addressParent' },
                { label: 'Lapsen sukunimi', key: 'firstNameChild' },
                { label: 'Lapsen etunimi', key: 'lastNameChild' },
                { label: 'Lapsen osoite', key: 'addressChild' }
              ]}
              filename="Lapset eri osoitteissa.csv"
            />
            <TableScrollable>
              <Table.Head>
                <Table.Row>
                  <Table.Th>{i18n.reports.common.careAreaName}</Table.Th>
                  <Table.Th>{i18n.reports.common.unitName}</Table.Th>
                  <Table.Th>
                    {i18n.reports.childrenInDifferentAddress.person1}
                  </Table.Th>
                  <Table.Th>
                    {i18n.reports.childrenInDifferentAddress.address1}
                  </Table.Th>
                  <Table.Th>
                    {i18n.reports.childrenInDifferentAddress.person2}
                  </Table.Th>
                  <Table.Th>
                    {i18n.reports.childrenInDifferentAddress.address2}
                  </Table.Th>
                </Table.Row>
              </Table.Head>
              <Table.Body>
                {filteredRows.map(
                  (row: ChildrenInDifferentAddressReportRow) => (
                    <Table.Row
                      key={`${row.unitId}:${row.parentId}:${row.childId}`}
                    >
                      <Table.Td>{row.careAreaName}</Table.Td>
                      <Table.Td>
                        <Link to={`/units/${row.unitId}`}>{row.unitName}</Link>
                      </Table.Td>
                      <Table.Td>
                        <Link to={`/profile/${row.parentId}`}>
                          {row.lastNameParent} {row.firstNameParent}
                        </Link>
                      </Table.Td>
                      <Table.Td>{row.addressParent}</Table.Td>
                      <Table.Td>
                        <Link to={`/child-information/${row.childId}`}>
                          {row.lastNameChild} {row.firstNameChild}
                        </Link>
                      </Table.Td>
                      <Table.Td>{row.addressChild}</Table.Td>
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

export default ChildrenInDifferentAddress
