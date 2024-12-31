// SPDX-FileCopyrightText: 2017-2023 City of Espoo
//
// SPDX-License-Identifier: LGPL-2.1-or-later

import React from 'react'
import { Link } from 'react-router'
import styled from 'styled-components'

import { VardaChildErrorReportRow } from 'lib-common/generated/api-types/reports'
import HelsinkiDateTime from 'lib-common/helsinki-date-time'
import LocalDate from 'lib-common/local-date'
import { useQueryResult } from 'lib-common/query'
import Title from 'lib-components/atoms/Title'
import { MutateButton } from 'lib-components/atoms/buttons/MutateButton'
import ReturnButton from 'lib-components/atoms/buttons/ReturnButton'
import { Container, ContentArea } from 'lib-components/layout/Container'
import { Table, Tbody, Td, Th, Thead, Tr } from 'lib-components/layout/Table'
import { Gap } from 'lib-components/white-space'

import { useTranslation } from '../../state/i18n'
import { renderResult } from '../async-rendering'

import { resetVardaChildMutation, vardaChildErrorsQuery } from './queries'

export default React.memo(function VardaChildErrors() {
  const { i18n } = useTranslation()
  const vardaErrorsResult = useQueryResult(vardaChildErrorsQuery())

  const ageInDays = (timestamp: HelsinkiDateTime): number =>
    LocalDate.todayInHelsinkiTz().differenceInDays(timestamp.toLocalDate())

  return (
    <Container>
      <ReturnButton label={i18n.common.goBack} />
      <ContentArea opaque>
        <Title size={1}>{i18n.reports.vardaChildErrors.title}</Title>
        <Gap size="xxs" />
        {renderResult(vardaErrorsResult, (rows) => (
          <>
            <Table data-qa="varda-errors-table">
              <Thead>
                <Tr>
                  <Th>{i18n.reports.vardaChildErrors.age}</Th>
                  <Th>{i18n.reports.vardaChildErrors.child}</Th>
                  <Th>{i18n.reports.vardaChildErrors.error}</Th>
                  <Th>{i18n.reports.vardaChildErrors.updated}</Th>
                  <Th />
                </Tr>
              </Thead>
              <Tbody>
                {rows.map((row: VardaChildErrorReportRow) => (
                  <Tr data-qa="varda-error-row" key={row.childId}>
                    <Td data-qa={`age-${row.childId}`}>
                      {ageInDays(row.created)}
                    </Td>
                    <Td data-qa={`child-${row.childId}`}>
                      <Link to={`/child-information/${row.childId}`}>
                        {row.childId}
                      </Link>
                    </Td>

                    <Td data-qa={`errors-${row.childId}`}>
                      <BreakAll>{row.error}</BreakAll>
                    </Td>
                    <Td data-qa={`updated-${row.childId}`}>
                      {row.updated.format()}
                    </Td>
                    <Td data-qa={`last-reset-${row.childId}`}>
                      <MutateButton
                        primary
                        text={i18n.reports.vardaChildErrors.updateChild}
                        mutation={resetVardaChildMutation}
                        onClick={() => ({ childId: row.childId })}
                        data-qa={`reset-button-${row.childId}`}
                      />
                    </Td>
                  </Tr>
                ))}
              </Tbody>
            </Table>
          </>
        ))}
      </ContentArea>
    </Container>
  )
})

const BreakAll = styled.span`
  word-break: break-all;
`
