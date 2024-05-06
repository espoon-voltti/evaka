// SPDX-FileCopyrightText: 2017-2024 City of Espoo
//
// SPDX-License-Identifier: LGPL-2.1-or-later

import React from 'react'
import { Link } from 'react-router-dom'
import styled from 'styled-components'

import HelsinkiDateTime from 'lib-common/helsinki-date-time'
import LocalDate from 'lib-common/local-date'
import { useQueryResult } from 'lib-common/query'
import Title from 'lib-components/atoms/Title'
import ReturnButton from 'lib-components/atoms/buttons/ReturnButton'
import { Container, ContentArea } from 'lib-components/layout/Container'
import { Tbody, Td, Th, Thead, Tr } from 'lib-components/layout/Table'
import { Gap } from 'lib-components/white-space'

import { useTranslation } from '../../state/i18n'
import { renderResult } from '../async-rendering'

import { TableScrollable } from './common'
import { vardaUnitErrorsQuery } from './queries'

export default React.memo(function VardaUnitErrors() {
  const { i18n } = useTranslation()
  const vardaErrorsResult = useQueryResult(vardaUnitErrorsQuery())

  const ageInDays = (timestamp: HelsinkiDateTime): number =>
    LocalDate.todayInHelsinkiTz().differenceInDays(timestamp.toLocalDate())

  return (
    <Container>
      <ReturnButton label={i18n.common.goBack} />
      <ContentArea opaque>
        <Title size={1}>{i18n.reports.vardaUnitErrors.title}</Title>

        <Gap size="xxs" />
        {renderResult(vardaErrorsResult, (rows) => (
          <>
            <TableScrollable data-qa="varda-errors-table">
              <Thead>
                <Tr>
                  <Th>{i18n.reports.vardaUnitErrors.age}</Th>
                  <Th>{i18n.reports.vardaUnitErrors.unit}</Th>
                  <Th>{i18n.reports.vardaUnitErrors.error}</Th>
                </Tr>
              </Thead>
              <Tbody>
                {rows.map((row) => (
                  <Tr key={row.unitId}>
                    <Td data-qa={`age-${row.unitId}`}>
                      {ageInDays(row.createdAt)}
                    </Td>
                    <Td data-qa={`unit-${row.unitId}`}>
                      <Link to={`/units/${row.unitId}/unit-info`}>
                        {row.unitName}
                      </Link>
                    </Td>

                    <Td data-qa={`errors-${row.unitId}`}>
                      <BreakAll>{row.error}</BreakAll>
                    </Td>
                  </Tr>
                ))}
              </Tbody>
            </TableScrollable>
          </>
        ))}
      </ContentArea>
    </Container>
  )
})

const BreakAll = styled.span`
  word-break: break-all;
`
