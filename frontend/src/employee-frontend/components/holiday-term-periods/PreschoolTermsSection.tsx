// SPDX-FileCopyrightText: 2017-2024 City of Espoo
//
// SPDX-License-Identifier: LGPL-2.1-or-later

import React, { useCallback, useState } from 'react'
import { useNavigate } from 'react-router-dom'
import styled from 'styled-components'

import { PreschoolTerm } from 'lib-common/generated/api-types/daycare'
import LocalDate from 'lib-common/local-date'
import { useQueryResult } from 'lib-common/query'
import { UUID } from 'lib-common/types'
import AddButton from 'lib-components/atoms/buttons/AddButton'
import { IconOnlyButton } from 'lib-components/atoms/buttons/IconOnlyButton'
import { Table, Tbody, Td, Th, Thead, Tr } from 'lib-components/layout/Table'
import { FixedSpaceRow } from 'lib-components/layout/flex-helpers'
import { ConfirmedMutation } from 'lib-components/molecules/ConfirmedMutation'
import InfoModal from 'lib-components/molecules/modals/InfoModal'
import { H2 } from 'lib-components/typography'
import { Gap } from 'lib-components/white-space'
import { featureFlags } from 'lib-customizations/employeeMobile'
import { faPen, faQuestion, faTrash } from 'lib-icons'

import { useTranslation } from '../../state/i18n'
import { renderResult } from '../async-rendering'

import { deletePreschoolTermMutation, preschoolTermsQuery } from './queries'

const Ul = styled.ul`
  margin: 0;
`

export default React.memo(function PreschoolTermsSection() {
  const { i18n } = useTranslation()
  const navigate = useNavigate()

  const [termToEdit, setTermToEdit] = useState<UUID>()

  const preschoolTerms = useQueryResult(preschoolTermsQuery())

  const onEditTermHandle = useCallback(
    (term: PreschoolTerm) => {
      if (term.finnishPreschool.start.isBefore(LocalDate.todayInSystemTz())) {
        setTermToEdit(term.id)
      } else {
        navigate(`/holiday-periods/preschool-term/${term.id}`)
      }
    },
    [setTermToEdit, navigate]
  )

  const onCloseEditModal = useCallback(() => {
    setTermToEdit(undefined)
  }, [setTermToEdit])

  const navigateToNewTerm = useCallback(() => {
    navigate('/holiday-periods/preschool-term/new')
  }, [navigate])

  const closeModalAndNavigateToEditTerm = useCallback(() => {
    if (termToEdit) {
      navigate(`/holiday-periods/preschool-term/${termToEdit}`)
    }
  }, [navigate, termToEdit])

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
        <>
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
                        <IconOnlyButton
                          icon={faPen}
                          data-qa="btn-edit"
                          onClick={() => onEditTermHandle(row)}
                          aria-label={i18n.common.edit}
                        />
                        {row.finnishPreschool.start.isAfter(
                          LocalDate.todayInSystemTz()
                        ) && (
                          <ConfirmedMutation
                            buttonStyle="INLINE"
                            data-qa="btn-delete"
                            icon={faTrash}
                            buttonText=""
                            mutation={deletePreschoolTermMutation}
                            onClick={() => ({ id: row.id })}
                            confirmationTitle={
                              i18n.terms.modals.deleteTerm.title
                            }
                          />
                        )}
                      </FixedSpaceRow>
                    </Td>
                  </Tr>
                ))}
            </Tbody>
          </Table>
          {termToEdit ? (
            <InfoModal
              icon={faQuestion}
              type="danger"
              title={i18n.terms.modals.editTerm.title}
              text={i18n.terms.modals.editTerm.text}
              reject={{
                action: onCloseEditModal,
                label: i18n.terms.modals.editTerm.reject
              }}
              resolve={{
                action: closeModalAndNavigateToEditTerm,
                label: i18n.terms.modals.editTerm.resolve
              }}
            />
          ) : null}
        </>
      ))}
    </>
  )
})
