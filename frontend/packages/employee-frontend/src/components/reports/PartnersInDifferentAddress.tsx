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
          <Wrapper>
            <ReactSelect
              options={[
                { id: '', label: '' },
                ...(isSuccess(rows)
                  ? distinct(
                      rows.data.map((row) => row.careAreaName)
                    ).map((s) => ({ id: s, label: s }))
                  : [])
              ]}
              onChange={(option) =>
                option && 'id' in option
                  ? setDisplayFilters({
                      ...displayFilters,
                      careArea: option.id
                    })
                  : undefined
              }
              styles={reactSelectStyles}
            />
          </Wrapper>
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
                          {row.lastName1} {row.firstName1}
                        </Link>
                      </Td>
                      <Td>{row.address1}</Td>
                      <Td>
                        <Link to={`/profile/${row.personId2}`}>
                          {row.lastName2} {row.firstName2}
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
        )}
      </ContentArea>
    </Container>
  )
}

export default PartnersInDifferentAddress
