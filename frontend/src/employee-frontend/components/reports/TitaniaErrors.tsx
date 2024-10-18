// SPDX-FileCopyrightText: 2017-2023 City of Espoo
//
// SPDX-License-Identifier: LGPL-2.1-or-later

import React from 'react'

import {
  TitaniaErrorReportRow,
  TitaniaErrorUnit,
  TitaniaErrorEmployee
} from 'lib-common/generated/api-types/reports'
import { useQueryResult } from 'lib-common/query'
import Title from 'lib-components/atoms/Title'
import ReturnButton from 'lib-components/atoms/buttons/ReturnButton'
import { Container, ContentArea } from 'lib-components/layout/Container'
import { Table, Thead, Th, Tbody, Td, Tr } from 'lib-components/layout/Table'
import { H1, H2, H3 } from 'lib-components/typography'
import { Gap } from 'lib-components/white-space'

import { useTranslation } from '../../state/i18n'
import { renderResult } from '../async-rendering'

import { titaniaErrorsReportQuery } from './queries'

export default React.memo(function TitaniaErrors() {
  const { i18n } = useTranslation()
  const titaniaErrorsResult = useQueryResult(titaniaErrorsReportQuery())

  return (
    <Container>
      <ReturnButton label={i18n.common.goBack} />
      <ContentArea opaque>
        <Title size={1}>{i18n.reports.titaniaErrors.title}</Title>

        <Gap size="xxs" />
        {renderResult(titaniaErrorsResult, (rows) => (
          <>
            {rows.map((row: TitaniaErrorReportRow) => (
              <>
                <H1 key={row.requestTime.format()}>
                  {i18n.reports.titaniaErrors.header +
                    ' ' +
                    row.requestTime.format()}
                </H1>
                {row.units.map((unit: TitaniaErrorUnit) => (
                  <>
                    <H2 key={unit.unitName}>{unit.unitName}</H2>
                    {unit.employees.map((employee: TitaniaErrorEmployee) => (
                      <>
                        <H3 key={employee.employeeName}>
                          {employee.employeeName +
                            (employee.employeeNumber == ''
                              ? ''
                              : ' (' + employee.employeeNumber + ')')}
                        </H3>
                        <Table>
                          <Thead>
                            <Th>{i18n.reports.titaniaErrors.date}</Th>
                            <Th>{i18n.reports.titaniaErrors.shift1}</Th>
                            <Th>{i18n.reports.titaniaErrors.shift2}</Th>
                          </Thead>
                          <Tbody>
                            {employee.conflictingShifts.map(
                              (conflict, index) => (
                                <Tr key={index}>
                                  <Td>{conflict.shiftDate.format()}</Td>
                                  <Td>
                                    {conflict.shiftBegins.format() +
                                      ' - ' +
                                      conflict.shiftEnds.format()}
                                  </Td>
                                  <Td>
                                    {conflict.overlappingShiftBegins.format() +
                                      ' - ' +
                                      conflict.overlappingShiftEnds.format()}
                                  </Td>
                                </Tr>
                              )
                            )}
                          </Tbody>
                        </Table>
                      </>
                    ))}
                  </>
                ))}
              </>
            ))}
          </>
        ))}
      </ContentArea>
    </Container>
  )
})
