// SPDX-FileCopyrightText: 2017-2024 City of Espoo
//
// SPDX-License-Identifier: LGPL-2.1-or-later

import { faPen } from 'Icons'
import React, { useCallback } from 'react'
import { useNavigate } from 'react-router-dom'
import styled from 'styled-components'

import { useQueryResult } from 'lib-common/query'
import { UUID } from 'lib-common/types'
import AddButton from 'lib-components/atoms/buttons/AddButton'
import IconButton from 'lib-components/atoms/buttons/IconButton'
import { Table, Tbody, Td, Th, Thead, Tr } from 'lib-components/layout/Table'
import { FixedSpaceRow } from 'lib-components/layout/flex-helpers'
import { H2 } from 'lib-components/typography'
import { Gap } from 'lib-components/white-space'
import { featureFlags } from 'lib-customizations/employeeMobile'

import { useTranslation } from '../../state/i18n'
import { renderResult } from '../async-rendering'

import { preschoolTermsQuery } from './queries'

const Ul = styled.ul`
  margin: 0;
`

export default React.memo(function TermsSection() {
  const { i18n } = useTranslation()
  const navigate = useNavigate()

  const preschoolTerms = useQueryResult(preschoolTermsQuery())

  const navigateToNewTerm = useCallback(() => {
    navigate('/holiday-periods/preschool-term/new')
  }, [navigate])

  const navigateToEditTerm = useCallback(
    (termId: UUID) => {
      navigate(`/holiday-periods/preschool-term/${termId}`)
    },
    [navigate]
  )

  return (
    <>
      <H2>{i18n.titles.preschoolTerms}</H2>
      <AddButton
        onClick={navigateToNewTerm}
        text={i18n.terms.addTerm}
        data-qa="add-preschool-term-button"
      />

      <Gap size="m" />

      {renderResult(preschoolTerms, (preschoolTerms) => (
        <Table>
          <Thead>
            <Tr>
              <Th>{i18n.terms.finnishPreschool}</Th>
              {featureFlags.extendedPreschoolTerm && (
                <Th>{i18n.terms.extendedTermStart}</Th>
              )}
              <Th>{i18n.terms.applicationPeriodStart}</Th>
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
                  <Td data-qa="finnish-preschool">
                    {row.finnishPreschool.format('dd.MM.yyyy')}
                  </Td>
                  {featureFlags.extendedPreschoolTerm && (
                    <Td data-qa="extended-term-start">
                      {row.extendedTerm.start.format('dd.MM.yyyy')}
                    </Td>
                  )}
                  <Td data-qa="application-period-start">
                    {row.applicationPeriod.start.format('dd.MM.yyyy')}
                  </Td>
                  <Td data-qa="term-breaks">
                    <Ul>
                      {row.termBreaks.map((termBreak, i) => (
                        <li
                          data-qa={`term-break-${termBreak.start.formatIso()}`}
                          key={`term-break-${i}`}
                        >
                          {termBreak.formatCompact()}
                        </li>
                      ))}
                    </Ul>
                  </Td>
                  <Td>
                    <FixedSpaceRow spacing="s">
                      <IconButton
                        icon={faPen}
                        data-qa="btn-edit"
                        onClick={() => navigateToEditTerm(row.id)}
                        aria-label={i18n.common.edit}
                      />
                    </FixedSpaceRow>
                  </Td>
                </Tr>
              ))}
          </Tbody>
        </Table>
      ))}
    </>
  )
})
