// SPDX-FileCopyrightText: 2017-2020 City of Espoo
//
// SPDX-License-Identifier: LGPL-2.1-or-later

import React, { useEffect, useState } from 'react'
import { Link } from 'react-router-dom'
import styled from 'styled-components'
import { Loading, Result } from 'lib-common/api'
import FiniteDateRange from 'lib-common/finite-date-range'
import LocalDate from 'lib-common/local-date'
import Loader from 'lib-components/atoms/Loader'
import Title from 'lib-components/atoms/Title'
import ReturnButton from 'lib-components/atoms/buttons/ReturnButton'
import { Container, ContentArea } from 'lib-components/layout/Container'
import { Tbody, Td, Th, Thead, Tr } from 'lib-components/layout/Table'
import { getVardaErrorsReport } from '../../api/reports'
import { useTranslation } from '../../state/i18n'
import { VardaErrorReportRow } from '../../types/reports'
import { TableScrollable } from './common'

const FlatList = styled.ul`
  list-style: none;
  padding-left: 0;
  margin-top: 0;
`

function VardaErrors() {
  const { i18n } = useTranslation()
  const [rows, setRows] = useState<Result<VardaErrorReportRow[]>>(Loading.of())

  useEffect(() => {
    setRows(Loading.of())
    void getVardaErrorsReport().then(setRows)
  }, [])

  const ageInDays = (timestamp: Date): number => {
    const diff = new Date().getTime() - timestamp.getTime()
    return Math.round(diff / (1000 * 3600 * 24))
  }

  return (
    <Container>
      <ReturnButton label={i18n.common.goBack} />
      <ContentArea opaque>
        <Title size={1}>{i18n.reports.vardaErrors.title}</Title>
        {rows.isLoading && <Loader />}
        {rows.isFailure && <span>{i18n.common.loadingFailed}</span>}
        {rows.isSuccess && (
          <>
            <TableScrollable>
              <Thead>
                <Tr>
                  <Th>{i18n.reports.vardaErrors.age}</Th>
                  <Th>{i18n.reports.vardaErrors.child}</Th>
                  <Th>{i18n.reports.vardaErrors.error}</Th>
                  <Th>{i18n.reports.vardaErrors.serviceNeed}</Th>
                  <Th>{i18n.reports.vardaErrors.updated}</Th>
                </Tr>
              </Thead>
              <Tbody>
                {rows.value.map((row: VardaErrorReportRow) => (
                  <Tr key={`${row.serviceNeedId}`}>
                    <Td>{ageInDays(row.created)}</Td>
                    <Td>
                      <Link to={`/child-information/${row.childId}`}>
                        {row.childId}
                      </Link>
                    </Td>

                    <Td>{row.errors.join('\n')}</Td>
                    <Td>
                      <FlatList>
                        <li>{row.serviceNeedOptionName}</li>
                        <li>
                          {FiniteDateRange.parseJson({
                            start: row.serviceNeedStartDate,
                            end: row.serviceNeedEndDate
                          }).format()}
                        </li>
                        <li>{row.serviceNeedId}</li>
                      </FlatList>
                    </Td>
                    <Td>
                      {LocalDate.fromSystemTzDate(row.updated).format()}{' '}
                      {row.updated.toLocaleTimeString()}
                    </Td>
                  </Tr>
                ))}
              </Tbody>
            </TableScrollable>
          </>
        )}
      </ContentArea>
    </Container>
  )
}

export default VardaErrors
