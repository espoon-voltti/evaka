// SPDX-FileCopyrightText: 2017-2022 City of Espoo
//
// SPDX-License-Identifier: LGPL-2.1-or-later

import React, { useCallback, useMemo, useState } from 'react'
import { Link } from 'react-router'
import styled from 'styled-components'

import type { FamilyConflictReportRow } from 'lib-common/generated/api-types/reports'
import { useQueryResult } from 'lib-common/query'
import Title from 'lib-components/atoms/Title'
import ReturnButton from 'lib-components/atoms/buttons/ReturnButton'
import Combobox from 'lib-components/atoms/dropdowns/Combobox'
import { Container, ContentArea } from 'lib-components/layout/Container'
import { Tbody, Td, Th, Thead, Tr } from 'lib-components/layout/Table'

import ReportDownload from '../../components/reports/ReportDownload'
import { useTranslation } from '../../state/i18n'
import { distinct } from '../../utils'
import { renderResult } from '../async-rendering'

import { FilterLabel, FilterRow, RowCountInfo, TableScrollable } from './common'
import { familyConflictsReportQuery } from './queries'

interface DisplayFilters {
  careArea: string
}

const emptyDisplayFilters: DisplayFilters = {
  careArea: ''
}

const Wrapper = styled.div`
  width: 100%;
`

export default React.memo(function FamilyConflicts() {
  const { i18n } = useTranslation()

  const [displayFilters, setDisplayFilters] =
    useState<DisplayFilters>(emptyDisplayFilters)
  const displayFilter = useCallback(
    (row: FamilyConflictReportRow): boolean =>
      !(
        displayFilters.careArea && row.careAreaName !== displayFilters.careArea
      ),
    [displayFilters.careArea]
  )

  const rows = useQueryResult(familyConflictsReportQuery())

  const filteredRows = useMemo(
    () => rows.map((rs) => rs.filter(displayFilter)),
    [rows, displayFilter]
  )

  return (
    <Container>
      <ReturnButton label={i18n.common.goBack} />
      <ContentArea opaque>
        <Title size={1}>{i18n.reports.familyConflicts.title}</Title>

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

        {renderResult(filteredRows, (filteredRows) => (
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
              columns={[
                { label: 'Palvelualue', value: (row) => row.careAreaName },
                { label: 'Yksikön nimi', value: (row) => row.unitName },
                { label: 'Etunimi', value: (row) => row.firstName },
                { label: 'Sukunimi', value: (row) => row.lastName },
                { label: 'Hetu', value: (row) => row.ssn },
                {
                  label: 'Konflikteja puolisoissa',
                  value: (row) => row.partnerConflictCount
                },
                {
                  label: 'Konflikteja lapsissa',
                  value: (row) => row.childConflictCount
                }
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
        ))}
      </ContentArea>
    </Container>
  )
})
