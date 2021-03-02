// SPDX-FileCopyrightText: 2017-2020 City of Espoo
//
// SPDX-License-Identifier: LGPL-2.1-or-later

import React, { useEffect, useMemo, useState } from 'react'
import ReactSelect from 'react-select'
import styled from 'styled-components'
import { Link } from 'react-router-dom'

import {
  Container,
  ContentArea
} from '@evaka/lib-components/src/layout/Container'
import Loader from '@evaka/lib-components/src/atoms/Loader'
import Title from '@evaka/lib-components/src/atoms/Title'
import {
  Th,
  Tr,
  Td,
  Thead,
  Tbody
} from '@evaka/lib-components/src/layout/Table'
import { reactSelectStyles } from '../../components/common/Select'
import { useTranslation } from '../../state/i18n'
import { Loading, Result } from '@evaka/lib-common/src/api'
import ReturnButton from '@evaka/lib-components/src/atoms/buttons/ReturnButton'
import ReportDownload from '../../components/reports/ReportDownload'
import { PartnersInDifferentAddressReportRow } from '../../types/reports'
import { getPartnersInDifferentAddressReport } from '../../api/reports'
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

function PartnersInDifferentAddress() {
  const { i18n } = useTranslation()
  const [rows, setRows] = useState<
    Result<PartnersInDifferentAddressReportRow[]>
  >(Loading.of())

  const [displayFilters, setDisplayFilters] = useState<DisplayFilters>(
    emptyDisplayFilters
  )
  const displayFilter = (row: PartnersInDifferentAddressReportRow): boolean => {
    return !(
      displayFilters.careArea && row.careAreaName !== displayFilters.careArea
    )
  }

  useEffect(() => {
    setRows(Loading.of())
    setDisplayFilters(emptyDisplayFilters)
    void getPartnersInDifferentAddressReport().then(setRows)
  }, [])

  const filteredRows: PartnersInDifferentAddressReportRow[] = useMemo(
    () => rows.map((rs) => rs.filter(displayFilter)).getOrElse([]),
    [rows, displayFilters]
  )

  return (
    <Container>
      <ReturnButton label={i18n.common.goBack} />
      <ContentArea opaque>
        <Title size={1}>{i18n.reports.partnersInDifferentAddress.title}</Title>

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
              data={filteredRows}
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
