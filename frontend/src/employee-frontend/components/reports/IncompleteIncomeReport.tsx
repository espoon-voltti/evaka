// SPDX-FileCopyrightText: 2017-2024 City of Espoo
//
// SPDX-License-Identifier: LGPL-2.1-or-later

import React from 'react'
import { Link } from 'wouter'

import type { IncompleteIncomeDbRow } from 'lib-common/generated/api-types/reports'
import { useQueryResult } from 'lib-common/query'
import Title from 'lib-components/atoms/Title'
import ReturnButton from 'lib-components/atoms/buttons/ReturnButton'
import { Container, ContentArea } from 'lib-components/layout/Container'
import { Tbody, Td, Th, Thead, Tr } from 'lib-components/layout/Table'

import { useTranslation } from '../../state/i18n'
import { renderResult } from '../async-rendering'

import ReportDownload from './ReportDownload'
import { TableScrollable } from './common'
import { incompleteIncomeReportQuery } from './queries'

export default React.memo(function IncompleteIncomes() {
  const { i18n } = useTranslation()
  const report = useQueryResult(incompleteIncomeReportQuery())

  return (
    <Container>
      <ReturnButton label={i18n.common.goBack} />
      <ContentArea opaque>
        <Title size={1}>{i18n.reports.incompleteIncomes.title}</Title>
        {renderResult(report, (rows) => (
          <>
            <ReportDownload<IncompleteIncomeDbRow>
              data={rows}
              columns={[
                { label: 'Etunimi', value: (row) => row.firstName },
                { label: 'Sukunimi', value: (row) => row.lastName },
                {
                  label: 'Alkupäivämäärä',
                  value: (row) => row.validFrom.format()
                },
                { label: 'Päiväkoti', value: (row) => row.daycareName },
                { label: 'Palvelualue', value: (row) => row.careareaName }
              ]}
              filename="Puuttuvat_tulotiedot.csv"
            />
            <TableScrollable>
              <Thead>
                <Tr>
                  <Th>{i18n.reports.incompleteIncomes.fullName}</Th>
                  <Th>{i18n.reports.incompleteIncomes.validFrom}</Th>
                  <Th>{i18n.reports.incompleteIncomes.daycareName}</Th>
                  <Th>{i18n.reports.incompleteIncomes.careareaName}</Th>
                </Tr>
              </Thead>
              <Tbody>
                {rows.map((row, index) => (
                  <Tr key={index}>
                    <Td>
                      <Link to={`/profile/${row.personId}`}>
                        {row.firstName} {row.lastName}
                      </Link>
                    </Td>
                    <Td>{row.validFrom.toString()}</Td>
                    <Td>{row.daycareName}</Td>
                    <Td>{row.careareaName}</Td>
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
