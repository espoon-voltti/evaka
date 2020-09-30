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
import { FamilyConflictReportRow } from '~types/reports'
import { getFamilyConflictsReport } from '~api/reports'
import ReturnButton from 'components/shared/atoms/buttons/ReturnButton'
import ReportDownload from '~components/reports/ReportDownload'
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

function FamilyConflicts() {
  const { i18n } = useTranslation()
  const [rows, setRows] = useState<Result<FamilyConflictReportRow[]>>(Loading())

  const [displayFilters, setDisplayFilters] = useState<DisplayFilters>(
    emptyDisplayFilters
  )
  const displayFilter = (row: FamilyConflictReportRow): boolean => {
    return !(
      displayFilters.careArea && row.careAreaName !== displayFilters.careArea
    )
  }

  useEffect(() => {
    setRows(Loading())
    setDisplayFilters(emptyDisplayFilters)
    void getFamilyConflictsReport().then(setRows)
  }, [])

  const filteredRows = useMemo(
    () => (isSuccess(rows) ? rows.data.filter(displayFilter) : []),
    [rows, displayFilters]
  )

  return (
    <Container>
      <ReturnButton />
      <ContentArea opaque>
        <Title size={1}>{i18n.reports.familyConflicts.title}</Title>

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
              data={rows.data.map((row) => ({
                careAreaName: row.careAreaName,
                unitName: row.unitName,
                firstName: row.firstName,
                lastName: row.lastName,
                ssn: row.socialSecurityNumber,
                partnerConflictCount: row.partnerConflictCount,
                childConflictCount: row.childConflictCount
              }))}
              headers={[
                { label: 'Palvelualue', key: 'careAreaName' },
                { label: 'Yksikön nimi', key: 'unitName' },
                { label: 'Etunimi', key: 'firstName' },
                { label: 'Sukunimi', key: 'lastName' },
                { label: 'Hetu', key: 'ssn' },
                {
                  label: 'Konflikteja puolisoissa',
                  key: 'partnerConflictCount'
                },
                { label: 'Konflikteja lapsissa', key: 'childConflictCount' }
              ]}
              filename="Perhekonfliktit.csv"
            />
            <TableScrollable>
              <Table.Head>
                <Table.Row>
                  <Table.Th>{i18n.reports.common.careAreaName}</Table.Th>
                  <Table.Th>{i18n.reports.common.unitName}</Table.Th>
                  <Table.Th>{i18n.reports.familyConflicts.name}</Table.Th>
                  <Table.Th>{i18n.reports.familyConflicts.ssn}</Table.Th>
                  <Table.Th>
                    {i18n.reports.familyConflicts.partnerConflictCount}
                  </Table.Th>
                  <Table.Th>
                    {i18n.reports.familyConflicts.childConflictCount}
                  </Table.Th>
                </Table.Row>
              </Table.Head>
              <Table.Body>
                {filteredRows.map((row: FamilyConflictReportRow) => (
                  <Table.Row key={row.id}>
                    <Table.Td>{row.careAreaName}</Table.Td>
                    <Table.Td>
                      <Link to={`/units/${row.unitId}`}>{row.unitName}</Link>
                    </Table.Td>
                    <Table.Td>
                      <Link to={`/profile/${row.id}`}>
                        {row.lastName} {row.firstName}
                      </Link>
                    </Table.Td>
                    <Table.Td>{row.socialSecurityNumber}</Table.Td>
                    <Table.Td>{row.partnerConflictCount}</Table.Td>
                    <Table.Td>{row.childConflictCount}</Table.Td>
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

export default FamilyConflicts
