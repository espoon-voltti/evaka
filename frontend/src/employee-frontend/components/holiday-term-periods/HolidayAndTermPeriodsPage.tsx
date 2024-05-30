// SPDX-FileCopyrightText: 2017-2022 City of Espoo
//
// SPDX-License-Identifier: LGPL-2.1-or-later

import React, { useCallback, useState } from 'react'
import { useNavigate } from 'react-router-dom'

import { useMutationResult, useQueryResult } from 'lib-common/query'
import { UUID } from 'lib-common/types'
import { AddButtonRow } from 'lib-components/atoms/buttons/AddButton'
import { IconButton } from 'lib-components/atoms/buttons/IconButton'
import Container, { ContentArea } from 'lib-components/layout/Container'
import { Table, Tbody, Td, Th, Thead, Tr } from 'lib-components/layout/Table'
import { FixedSpaceRow } from 'lib-components/layout/flex-helpers'
import { AsyncFormModal } from 'lib-components/molecules/modals/FormModal'
import { H1, H2 } from 'lib-components/typography'
import { Gap } from 'lib-components/white-space'
import { faPen, faQuestion, faTrash } from 'lib-icons'

import { useTranslation } from '../../state/i18n'
import { renderResult } from '../async-rendering'

import ClubsTermsSection from './ClubsTermsSection'
import PreschoolTermsSection from './PreschoolTermsSection'
import {
  deleteHolidayPeriodMutation,
  deleteQuestionnaireMutation,
  holidayPeriodsQuery,
  questionnairesQuery
} from './queries'

export default React.memo(function HolidayAndTermPeriodsPage() {
  const { i18n } = useTranslation()
  const navigate = useNavigate()

  const holidayPeriods = useQueryResult(holidayPeriodsQuery())
  const questionnaires = useQueryResult(questionnairesQuery())

  const [periodToDelete, setPeriodToDelete] = useState<UUID>()
  const { mutateAsync: deleteHolidayPeriod } = useMutationResult(
    deleteHolidayPeriodMutation
  )
  const onDeletePeriod = useCallback(
    () =>
      periodToDelete
        ? deleteHolidayPeriod({ id: periodToDelete })
        : Promise.reject(),
    [deleteHolidayPeriod, periodToDelete]
  )
  const navigateToNewHolidayPeriod = useCallback(() => {
    navigate('/holiday-periods/new')
  }, [navigate])

  const [questionnaireToDelete, setQuestionnaireToDelete] = useState<UUID>()
  const { mutateAsync: deleteQuestionnaire } = useMutationResult(
    deleteQuestionnaireMutation
  )
  const onDeleteQuestionnaire = useCallback(
    () =>
      questionnaireToDelete
        ? deleteQuestionnaire({ id: questionnaireToDelete })
        : Promise.reject(),
    [deleteQuestionnaire, questionnaireToDelete]
  )
  const navigateToNewQuestionnaire = useCallback(() => {
    navigate('/holiday-periods/questionnaire/new')
  }, [navigate])

  return (
    <Container>
      <ContentArea opaque>
        <H1>{i18n.titles.holidayAndTermPeriods}</H1>
      </ContentArea>
      <ContentArea opaque>
        <H2>{i18n.titles.holidayPeriods}</H2>

        <AddButtonRow
          onClick={navigateToNewHolidayPeriod}
          text={i18n.common.addNew}
          data-qa="add-holiday-period-button"
        />
        {renderResult(holidayPeriods, (data) => (
          <Table>
            <Thead>
              <Tr>
                <Th>{i18n.holidayPeriods.period}</Th>
                <Th>{i18n.holidayPeriods.reservationDeadline}</Th>
                <Th />
              </Tr>
            </Thead>
            <Tbody>
              {data.map((holiday) => (
                <Tr key={holiday.id} data-qa="holiday-period-row">
                  <Td data-qa="holiday-period">{holiday.period.format()}</Td>
                  <Td>{holiday.reservationDeadline?.format()}</Td>
                  <Td>
                    <FixedSpaceRow spacing="s">
                      <IconButton
                        icon={faPen}
                        data-qa="btn-edit"
                        onClick={() =>
                          navigate(`/holiday-periods/${holiday.id}`)
                        }
                        aria-label={i18n.common.edit}
                      />
                      <IconButton
                        icon={faTrash}
                        data-qa="btn-delete"
                        onClick={() => setPeriodToDelete(holiday.id)}
                        aria-label={i18n.common.remove}
                      />
                    </FixedSpaceRow>
                  </Td>
                </Tr>
              ))}
            </Tbody>
          </Table>
        ))}

        <H2>{i18n.holidayQuestionnaires.questionnaires}</H2>

        <AddButtonRow
          onClick={navigateToNewQuestionnaire}
          text={i18n.common.addNew}
          data-qa="add-questionnaire-button"
        />
        {renderResult(questionnaires, (questionnaires) => (
          <Table>
            <Thead>
              <Tr>
                <Th>{i18n.holidayQuestionnaires.active}</Th>
                <Th>{i18n.holidayQuestionnaires.title}</Th>
                <Th />
              </Tr>
            </Thead>
            <Tbody>
              {questionnaires.map((row) => (
                <Tr key={row.id} data-qa="questionnaire-row">
                  <Td>{row.active.format()}</Td>
                  <Td>{row.title.fi}</Td>
                  <Td>
                    <FixedSpaceRow spacing="s">
                      <IconButton
                        icon={faPen}
                        data-qa="btn-edit"
                        onClick={() =>
                          navigate(`/holiday-periods/questionnaire/${row.id}`)
                        }
                        aria-label={i18n.common.edit}
                      />
                      <IconButton
                        icon={faTrash}
                        data-qa="btn-delete"
                        onClick={() => setQuestionnaireToDelete(row.id)}
                        aria-label={i18n.common.remove}
                      />
                    </FixedSpaceRow>
                  </Td>
                </Tr>
              ))}
            </Tbody>
          </Table>
        ))}

        {!!periodToDelete && (
          <AsyncFormModal
            type="warning"
            title={i18n.holidayPeriods.confirmDelete}
            icon={faQuestion}
            rejectAction={() => setPeriodToDelete(undefined)}
            rejectLabel={i18n.common.cancel}
            resolveAction={onDeletePeriod}
            resolveLabel={i18n.common.remove}
            onSuccess={() => {
              setPeriodToDelete(undefined)
            }}
          />
        )}

        {!!questionnaireToDelete && (
          <AsyncFormModal
            type="warning"
            title={i18n.holidayQuestionnaires.confirmDelete}
            icon={faQuestion}
            rejectAction={() => setQuestionnaireToDelete(undefined)}
            rejectLabel={i18n.common.cancel}
            resolveAction={onDeleteQuestionnaire}
            resolveLabel={i18n.common.remove}
            onSuccess={() => {
              setQuestionnaireToDelete(undefined)
            }}
          />
        )}

        <Gap size="L" />

        <ClubsTermsSection />

        <Gap size="L" />

        <PreschoolTermsSection />
      </ContentArea>
    </Container>
  )
})
