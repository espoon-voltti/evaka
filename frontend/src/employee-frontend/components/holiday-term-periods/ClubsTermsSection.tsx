// SPDX-FileCopyrightText: 2017-2024 City of Espoo
//
// SPDX-License-Identifier: LGPL-2.1-or-later

import React, { useCallback, useState } from 'react'
import { useNavigate } from 'react-router-dom'
import styled from 'styled-components'

import { ClubTerm } from 'lib-common/generated/api-types/daycare'
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
import { faPen, faQuestion, faTrash } from 'lib-icons'

import { useTranslation } from '../../state/i18n'
import { renderResult } from '../async-rendering'

import { clubTermsQuery, deleteClubTermMutation } from './queries'

const Ul = styled.ul`
  margin: 0;
`

export default React.memo(function ClubTermsSection() {
  const { i18n } = useTranslation()
  const navigate = useNavigate()

  const [termToEdit, setTermToEdit] = useState<UUID>()

  const clubTerms = useQueryResult(clubTermsQuery())

  const onEditTermHandle = useCallback(
    (clubTerm: ClubTerm) => {
      if (clubTerm.term.start.isBefore(LocalDate.todayInSystemTz())) {
        setTermToEdit(clubTerm.id)
      } else {
        navigate(`/holiday-periods/club-term/${clubTerm.id}`)
      }
    },
    [setTermToEdit, navigate]
  )

  const onCloseEditModal = useCallback(() => {
    setTermToEdit(undefined)
  }, [setTermToEdit])

  const navigateToNewTerm = useCallback(() => {
    navigate('/holiday-periods/club-term/new')
  }, [navigate])

  const closeModalAndNavigateToEditTerm = useCallback(() => {
    if (termToEdit) {
      navigate(`/holiday-periods/club-term/${termToEdit}`)
    }
  }, [navigate, termToEdit])

  return (
    <>
      <H2>{i18n.titles.clubTerms}</H2>
      <AddButton
        onClick={navigateToNewTerm}
        text={i18n.terms.addTerm}
        data-qa="add-club-term-button"
      />

      <Gap size="m" />

      {renderResult(clubTerms, (clubTerms) => (
        <>
          <Table>
            <Thead>
              <Tr>
                <Th>{i18n.terms.term}</Th>
                <Th>{i18n.terms.applicationPeriodStart}</Th>
                <Th>{i18n.terms.termBreaks}</Th>
                <Th />
              </Tr>
            </Thead>
            <Tbody>
              {clubTerms
                .sort((a, b) => b.term.start.compareTo(a.term.start))
                .map((row, i) => (
                  <Tr key={i} data-qa="club-term-row">
                    <Td data-qa="term">{row.term.format('dd.MM.yyyy')}</Td>
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
                        {row.term.start.isAfter(
                          LocalDate.todayInSystemTz()
                        ) && (
                          <ConfirmedMutation
                            buttonStyle="INLINE"
                            data-qa="btn-delete"
                            icon={faTrash}
                            buttonText=""
                            mutation={deleteClubTermMutation}
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
