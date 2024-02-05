// SPDX-FileCopyrightText: 2017-2024 City of Espoo
//
// SPDX-License-Identifier: LGPL-2.1-or-later

import React from 'react'

import { useQueryResult } from 'lib-common/query'
import { Table, Tbody, Td, Th, Thead, Tr } from 'lib-components/layout/Table'
import { H2 } from 'lib-components/typography'

import { useTranslation } from '../../state/i18n'
import { renderResult } from '../async-rendering'

import { preschoolTermsQuery } from './queries'

export default React.memo(function TermsSection() {
  const { i18n } = useTranslation()

  const preschoolTerms = useQueryResult(preschoolTermsQuery())

  return (
    <>
      <H2>{i18n.titles.preschoolTerms}</H2>

      {renderResult(preschoolTerms, (preschoolTerms) => (
        <Table>
          <Thead>
            <Tr>
              <Th>{i18n.terms.finnishPreschool}</Th>
              <Th>{i18n.terms.extendedPeriodStarts}</Th>
              <Th>{i18n.terms.applicationPeriodStarts}</Th>
              <Th>{i18n.terms.termBreaks}</Th>
              <Th />
            </Tr>
          </Thead>
          <Tbody>
            {preschoolTerms
              .sort((a, b) =>
                b.finnishPreschool.start.compareTo(a.finnishPreschool.start)
              )
              .map((row, i) => (
                <Tr key={i} data-qa="preschool-term-row">
                  <Td>{row.finnishPreschool.format('dd.MM.yyyy')}</Td>
                  <Td>{row.extendedTerm.start.format('dd.MM.yyyy')}</Td>
                  <Td>{row.applicationPeriod.start.format('dd.MM.yyyy')}</Td>
                  <Td>
                    <ul>
                      {row.termBreaks.map((termBreak) => (
                        <li key={termBreak.start.format('dd.MM.yyyy')}>
                          {termBreak.formatCompact()}
                        </li>
                      ))}
                    </ul>
                  </Td>
                  <Td />
                </Tr>
              ))}
          </Tbody>
        </Table>
      ))}
    </>
  )
})
