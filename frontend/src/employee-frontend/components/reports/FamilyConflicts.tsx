// SPDX-FileCopyrightText: 2017-2020 City of Espoo
//
// SPDX-License-Identifier: LGPL-2.1-or-later

import React, { useEffect, useMemo, useState } from 'react'
import ReactSelect from 'react-select'
import styled from 'styled-components'

import { Container, ContentArea } from '@evaka/lib-components/layout/Container'
import Loader from '@evaka/lib-components/atoms/Loader'
import Title from '@evaka/lib-components/atoms/Title'
import { Th, Tr, Td, Thead, Tbody } from '@evaka/lib-components/layout/Table'
import { reactSelectStyles } from '../../components/common/Select'
import { useTranslation } from '../../state/i18n'
import { Link } from 'react-router-dom'
import { Loading, Result } from '@evaka/lib-common/api'
import { FamilyConflictReportRow } from '../../types/reports'
import { getFamilyConflictsReport } from '../../api/reports'
import ReturnButton from '@evaka/lib-components/atoms/buttons/ReturnButton'
import ReportDownload from '../../components/reports/ReportDownload'
import {
  FilterLabel,
  FilterRow,
  RowCountInfo,
  TableScrollable
} from '../../components/reports/common'
import { distinct } from '../../utils'

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
  const [rows, setRows] = useState<Result<FamilyConflictReportRow[]>>(
    Loading.of()
  )

  const [displayFilters, setDisplayFilters] = useState<DisplayFilters>(
    emptyDisplayFilters
  )
  const displayFilter = (row: FamilyConflictReportRow): boolean => {
    return !(
      displayFilters.careArea && row.careAreaName !== displayFilters.careArea
    )
  }

  useEffect(() => {
    setRows(Loading.of())
    setDisplayFilters(emptyDisplayFilters)
    void getFamilyConflictsReport().then(setRows)
  }, [])

  const filteredRows: FamilyConflictReportRow[] = useMemo(
    () => rows.map((rs) => rs.filter(displayFilter)).getOrElse([]),
    [rows, displayFilters]
  )

  return (
    <Container>
      <ReturnButton label={i18n.common.goBack} />
      <ContentArea opaque>
        <Title size={1}>{i18n.reports.familyConflicts.title}</Title>

        <FilterRow>
          <FilterLabel>{i18n.reports.common.careAreaName}</FilterLabel>
          <Wrapper>
            <ReactSelect
              options={[
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
                  : {
                      label: i18n.common.all,
                      value: ''
                    }
              }
              styles={reactSelectStyles}
              placeholder={i18n.reports.occupancies.filters.areaPlaceholder}
            />
          </Wrapper>
        </FilterRow>

        {rows.isLoading && <Loader />}
        {rows.isFailure && <span>{i18n.common.loadingFailed}</span>}
        {rows.isSuccess && (
          <>
            <ReportDownload
              data={filteredRows.map((row) => ({
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
