// SPDX-FileCopyrightText: 2017-2022 City of Espoo
//
// SPDX-License-Identifier: LGPL-2.1-or-later

import React, { useEffect, useState } from 'react'
import { Link } from 'react-router-dom'
import styled from 'styled-components'

import { Loading, Result } from 'lib-common/api'
import FiniteDateRange from 'lib-common/finite-date-range'
import { VardaErrorReportRow } from 'lib-common/generated/api-types/reports'
import LocalDate from 'lib-common/local-date'
import Loader from 'lib-components/atoms/Loader'
import Title from 'lib-components/atoms/Title'
import AsyncButton from 'lib-components/atoms/buttons/AsyncButton'
import ReturnButton from 'lib-components/atoms/buttons/ReturnButton'
import { InlineAsyncButton } from 'lib-components/employee/notes/InlineAsyncButton'
import { Container, ContentArea } from 'lib-components/layout/Container'
import { Tbody, Td, Th, Thead, Tr } from 'lib-components/layout/Table'

import {
  getVardaErrorsReport,
  markChildForVardaReset,
  runResetVardaChildren
} from '../../api/reports'
import { useTranslation } from '../../state/i18n'

import { TableScrollable } from './common'

const FlatList = styled.ul`
  list-style: none;
  padding-left: 0;
  margin-top: 0;
`

function VardaErrors() {
  const { i18n } = useTranslation()
  const [rows, setRows] = useState<Result<VardaErrorReportRow[]>>(Loading.of())
  const [dirty, setDirty] = useState<boolean>(false)

  useEffect(() => {
    setRows(Loading.of())
    void getVardaErrorsReport()
      .then(setRows)
      .then(() => setDirty(false))
  }, [dirty])

  const ageInDays = (timestamp: Date): number => {
    const diff = new Date().getTime() - timestamp.getTime()
    return Math.round(diff / (1000 * 3600 * 24))
  }

  const formatTimestamp = (timestamp: Date): string => {
    return `${LocalDate.fromSystemTzDate(
      timestamp
    ).format()} ${timestamp.toLocaleTimeString()}`
  }

  const markChildForResetAndReload = async (childId: string) => {
    await markChildForVardaReset(childId)
    setDirty(true)
  }

  const startResetVardaChildren = async () => {
    await runResetVardaChildren()
  }

  return (
    <Container>
      <ReturnButton label={i18n.common.goBack} />
      <ContentArea opaque>
        <Title size={1}>{i18n.reports.vardaErrors.title}</Title>
        <AsyncButton
          text={i18n.reports.vardaErrors.vardaResetButton}
          onClick={startResetVardaChildren}
          onSuccess={() => null}
        />
        {rows.isLoading && <Loader />}
        {rows.isFailure && <span>{i18n.common.loadingFailed}</span>}
        {rows.isSuccess && (
          <>
            <TableScrollable data-qa="varda-errors-table">
              <Thead>
                <Tr>
                  <Th>{i18n.reports.vardaErrors.age}</Th>
                  <Th>{i18n.reports.vardaErrors.child}</Th>
                  <Th>{i18n.reports.vardaErrors.error}</Th>
                  <Th>{i18n.reports.vardaErrors.serviceNeed}</Th>
                  <Th>{i18n.reports.vardaErrors.updated}</Th>
                  <Th>{i18n.reports.vardaErrors.childLastReset}</Th>
                </Tr>
              </Thead>
              <Tbody>
                {rows.value.map((row: VardaErrorReportRow) => (
                  <Tr data-qa="varda-error-row" key={`${row.serviceNeedId}`}>
                    <Td data-qa={`age-${row.childId}`}>
                      {ageInDays(row.created)}
                    </Td>
                    <Td data-qa={`child-${row.childId}`}>
                      <Link to={`/child-information/${row.childId}`}>
                        {row.childId}
                      </Link>
                    </Td>

                    <Td data-qa={`errors-${row.childId}`}>
                      {row.errors.join('\n')}
                    </Td>
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
                    <Td data-qa={`updated-${row.childId}`}>
                      {formatTimestamp(row.updated)}
                    </Td>
                    <Td data-qa={`last-reset-${row.childId}`}>
                      {row.resetTimeStamp ? (
                        <>
                          <span>{formatTimestamp(row.resetTimeStamp)}</span>
                          <InlineAsyncButton
                            data-qa={`reset-button-${row.childId}`}
                            onClick={() =>
                              markChildForResetAndReload(row.childId)
                            }
                            onSuccess={() => null}
                            textInProgress={i18n.reports.vardaErrors.updated}
                            text={i18n.reports.vardaErrors.resetChild}
                          />
                        </>
                      ) : (
                        i18n.reports.vardaErrors.childMarkedForRest
                      )}
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
