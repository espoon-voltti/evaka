// SPDX-FileCopyrightText: 2017-2022 City of Espoo
//
// SPDX-License-Identifier: LGPL-2.1-or-later

import React, { useEffect, useMemo, useState } from 'react'
import { Link } from 'react-router-dom'
import styled from 'styled-components'

import { Loading, Result, wrapResult } from 'lib-common/api'
import { ChildrenInDifferentAddressReportRow } from 'lib-common/generated/api-types/reports'
import Loader from 'lib-components/atoms/Loader'
import Title from 'lib-components/atoms/Title'
import ReturnButton from 'lib-components/atoms/buttons/ReturnButton'
import Combobox from 'lib-components/atoms/dropdowns/Combobox'
import { Container, ContentArea } from 'lib-components/layout/Container'
import { Tbody, Td, Th, Thead, Tr } from 'lib-components/layout/Table'

import ReportDownload from '../../components/reports/ReportDownload'
import { getChildrenInDifferentAddressReport } from '../../generated/api-clients/reports'
import { useTranslation } from '../../state/i18n'
import { distinct } from '../../utils'

import { FilterLabel, FilterRow, RowCountInfo, TableScrollable } from './common'

const getChildrenInDifferentAddressReportResult = wrapResult(
  getChildrenInDifferentAddressReport
)

interface DisplayFilters {
  careArea: string
}

const emptyDisplayFilters: DisplayFilters = {
  careArea: ''
}

const Wrapper = styled.div`
  width: 100%;
`

export default React.memo(function ChildrenInDifferentAddress() {
  const { i18n } = useTranslation()
  const [rows, setRows] = useState<
    Result<ChildrenInDifferentAddressReportRow[]>
  >(Loading.of())

  const [displayFilters, setDisplayFilters] =
    useState<DisplayFilters>(emptyDisplayFilters)
  const displayFilter = (row: ChildrenInDifferentAddressReportRow): boolean =>
    !(displayFilters.careArea && row.careAreaName !== displayFilters.careArea)

  useEffect(() => {
    setRows(Loading.of())
    setDisplayFilters(emptyDisplayFilters)
    void getChildrenInDifferentAddressReportResult().then(setRows)
  }, [])

  const filteredRows: ChildrenInDifferentAddressReportRow[] = useMemo(
    () => rows.map((rs) => rs.filter(displayFilter)).getOrElse([]),
    [rows, displayFilters] // eslint-disable-line react-hooks/exhaustive-deps
  )

  return (
    <Container>
      <ReturnButton label={i18n.common.goBack} />
      <ContentArea opaque>
        <Title size={1}>{i18n.reports.childrenInDifferentAddress.title}</Title>

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
                  : {
                      label: i18n.common.all,
                      value: ''
                    }
              }
              selectedItem={
                displayFilters.careArea !== ''
                  ? {
                      label: displayFilters.careArea,
                      value: displayFilters.careArea
                    }
                  : null
              }
              placeholder={i18n.reports.occupancies.filters.areaPlaceholder}
              getItemLabel={(item) => item.label}
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
              <Thead>
                <Tr>
                  <Th>{i18n.reports.common.careAreaName}</Th>
                  <Th>{i18n.reports.common.unitName}</Th>
                  <Th>{i18n.reports.childrenInDifferentAddress.person1}</Th>
                  <Th>{i18n.reports.childrenInDifferentAddress.address1}</Th>
                  <Th>{i18n.reports.childrenInDifferentAddress.person2}</Th>
                  <Th>{i18n.reports.childrenInDifferentAddress.address2}</Th>
                </Tr>
              </Thead>
              <Tbody>
                {filteredRows.map(
                  (row: ChildrenInDifferentAddressReportRow) => (
                    <Tr key={`${row.unitId}:${row.parentId}:${row.childId}`}>
                      <Td>{row.careAreaName}</Td>
                      <Td>
                        <Link to={`/units/${row.unitId}`}>{row.unitName}</Link>
                      </Td>
                      <Td>
                        <Link to={`/profile/${row.parentId}`}>
                          {row.lastNameParent} {row.firstNameParent}
                        </Link>
                      </Td>
                      <Td>{row.addressParent}</Td>
                      <Td>
                        <Link to={`/child-information/${row.childId}`}>
                          {row.lastNameChild} {row.firstNameChild}
                        </Link>
                      </Td>
                      <Td>{row.addressChild}</Td>
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
})
