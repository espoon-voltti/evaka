// SPDX-FileCopyrightText: 2017-2022 City of Espoo
//
// SPDX-License-Identifier: LGPL-2.1-or-later

import React, { useCallback, useMemo, useState } from 'react'
import styled from 'styled-components'
import { Link } from 'wouter'

import type { PartnersInDifferentAddressReportRow } from 'lib-common/generated/api-types/reports'
import { useQueryResult } from 'lib-common/query'
import Title from 'lib-components/atoms/Title'
import ReturnButton from 'lib-components/atoms/buttons/ReturnButton'
import Combobox from 'lib-components/atoms/dropdowns/Combobox'
import { Container, ContentArea } from 'lib-components/layout/Container'
import { Tbody, Td, Th, Thead, Tr } from 'lib-components/layout/Table'
import { PersonName } from 'lib-components/molecules/PersonNames'

import { useTranslation } from '../../state/i18n'
import { distinct } from '../../utils'
import { renderResult } from '../async-rendering'

import ReportDownload from './ReportDownload'
import { FilterLabel, FilterRow, RowCountInfo, TableScrollable } from './common'
import { partnersInDifferentAddressReportQuery } from './queries'

interface DisplayFilters {
  careArea: string
}

const emptyDisplayFilters: DisplayFilters = {
  careArea: ''
}

const Wrapper = styled.div`
  width: 100%;
`

export default React.memo(function PartnersInDifferentAddress() {
  const { i18n } = useTranslation()

  const [displayFilters, setDisplayFilters] =
    useState<DisplayFilters>(emptyDisplayFilters)
  const displayFilter = useCallback(
    (row: PartnersInDifferentAddressReportRow): boolean =>
      !(
        displayFilters.careArea && row.careAreaName !== displayFilters.careArea
      ),
    [displayFilters.careArea]
  )

  const rows = useQueryResult(partnersInDifferentAddressReportQuery())

  const filteredRows = useMemo(
    () => rows.map((rs) => rs.filter(displayFilter)),
    [rows, displayFilter]
  )

  return (
    <Container>
      <ReturnButton label={i18n.common.goBack} />
      <ContentArea opaque>
        <Title size={1}>{i18n.reports.partnersInDifferentAddress.title}</Title>

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
              data={filteredRows}
              columns={[
                { label: 'Palvelualue', value: (row) => row.careAreaName },
                { label: 'Yksikön nimi', value: (row) => row.unitName },
                { label: 'Sukunimi', value: (row) => row.firstName1 },
                { label: 'Etunimi', value: (row) => row.lastName1 },
                { label: 'Osoite', value: (row) => row.address1 },
                { label: 'Puolison sukunimi', value: (row) => row.firstName2 },
                { label: 'Puolison etunimi', value: (row) => row.lastName2 },
                { label: 'Puolison osoite', value: (row) => row.address2 }
              ]}
              filename="Puolisot eri osoitteissa.csv"
            />
            <TableScrollable>
              <Thead>
                <Tr>
                  <Th>{i18n.reports.common.careAreaName}</Th>
                  <Th>{i18n.reports.common.unitName}</Th>
                  <Th>{i18n.reports.partnersInDifferentAddress.person1}</Th>
                  <Th>{i18n.reports.partnersInDifferentAddress.address1}</Th>
                  <Th>{i18n.reports.partnersInDifferentAddress.person2}</Th>
                  <Th>{i18n.reports.partnersInDifferentAddress.address2}</Th>
                </Tr>
              </Thead>
              <Tbody>
                {filteredRows.map(
                  (row: PartnersInDifferentAddressReportRow) => (
                    <Tr key={`${row.unitId}:${row.personId1}`}>
                      <Td>{row.careAreaName}</Td>
                      <Td>
                        <Link to={`/units/${row.unitId}`}>{row.unitName}</Link>
                      </Td>
                      <Td>
                        <Link to={`/profile/${row.personId1}`}>
                          <PersonName
                            person={{
                              lastName: row.lastName1,
                              firstName: row.firstName1
                            }}
                            format="Last First"
                          />
                        </Link>
                      </Td>
                      <Td>{row.address1}</Td>
                      <Td>
                        <Link to={`/profile/${row.personId2}`}>
                          <PersonName
                            person={{
                              lastName: row.lastName2,
                              firstName: row.firstName2
                            }}
                            format="Last First"
                          />
                        </Link>
                      </Td>
                      <Td>{row.address2}</Td>
                    </Tr>
                  )
                )}
              </Tbody>
            </TableScrollable>
            <RowCountInfo rowCount={filteredRows.length} />
          </>
        ))}
      </ContentArea>
    </Container>
  )
})
