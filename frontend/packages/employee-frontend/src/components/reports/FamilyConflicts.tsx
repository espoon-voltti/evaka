// SPDX-FileCopyrightText: 2017-2020 City of Espoo
//
// SPDX-License-Identifier: LGPL-2.1-or-later

import React, { useEffect, useMemo, useState } from 'react'
import ReactSelect from 'react-select'
import styled from 'styled-components'

import { Container, ContentArea } from '~components/shared/layout/Container'
import Loader from '~components/shared/atoms/Loader'
import Title from '~components/shared/atoms/Title'
import { Th, Tr, Td, Thead, Tbody } from '~components/shared/layout/Table'
import { reactSelectStyles } from '~components/shared/utils'
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
import { distinct } from 'utils'

interface DisplayFilters {
  careArea: string
}

const emptyDisplayFilters: DisplayFilters = {
  careArea: ''
}

const Wrapper = styled.div`
  width: 100%;
`

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
                  : undefined
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
              <Thead>
                <Tr>
                  <Th>{i18n.reports.common.careAreaName}</Th>
                  <Th>{i18n.reports.common.unitName}</Th>
                  <Th>{i18n.reports.familyConflicts.name}</Th>
                  <Th>{i18n.reports.familyConflicts.ssn}</Th>
                  <Th>{i18n.reports.familyConflicts.partnerConflictCount}</Th>
                  <Th>{i18n.reports.familyConflicts.childConflictCount}</Th>
                </Tr>
              </Thead>
              <Tbody>
                {filteredRows.map((row: FamilyConflictReportRow) => (
                  <Tr key={row.id}>
                    <Td>{row.careAreaName}</Td>
                    <Td>
                      <Link to={`/units/${row.unitId}`}>{row.unitName}</Link>
                    </Td>
                    <Td>
                      <Link to={`/profile/${row.id}`}>
                        {row.lastName} {row.firstName}
                      </Link>
                    </Td>
                    <Td>{row.socialSecurityNumber}</Td>
                    <Td>{row.partnerConflictCount}</Td>
                    <Td>{row.childConflictCount}</Td>
                  </Tr>
                ))}
              </Tbody>
            </TableScrollable>
            <RowCountInfo rowCount={filteredRows.length} />
          </>
        )}
      </ContentArea>
    </Container>
  )
}

export default FamilyConflicts
