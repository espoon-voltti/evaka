// SPDX-FileCopyrightText: 2017-2022 City of Espoo
//
// SPDX-License-Identifier: LGPL-2.1-or-later

import React, { useCallback, useState } from 'react'
import { useHistory } from 'react-router'

import { UUID } from 'lib-common/types'
import { useApiState } from 'lib-common/utils/useRestApi'
import { AddButtonRow } from 'lib-components/atoms/buttons/AddButton'
import IconButton from 'lib-components/atoms/buttons/IconButton'
import Container, { ContentArea } from 'lib-components/layout/Container'
import { Table, Tbody, Td, Th, Thead, Tr } from 'lib-components/layout/Table'
import { FixedSpaceRow } from 'lib-components/layout/flex-helpers'
import { AsyncFormModal } from 'lib-components/molecules/modals/FormModal'
import { H1 } from 'lib-components/typography'
import { faPen, faQuestion, faTrash } from 'lib-icons'

import { useTranslation } from '../../state/i18n'
import { renderResult } from '../async-rendering'

import {
  deleteHolidayPeriod,
  deleteQuestionnaire,
  getHolidayPeriods,
  getQuestionnaires
} from './api'

export default React.memo(function HolidayPeriodsPage() {
  const { i18n } = useTranslation()
  const history = useHistory()

  const [holidayPeriods, refreshPeriods] = useApiState(getHolidayPeriods, [])
  const [questionnaires, refreshQuestionnaires] = useApiState(
    getQuestionnaires,
    []
  )

  const [periodToDelete, setPeriodToDelete] = useState<UUID>()
  const onDeletePeriod = useCallback(
    () =>
      periodToDelete ? deleteHolidayPeriod(periodToDelete) : Promise.reject(),
    [periodToDelete]
  )
  const navigateToNewHolidayPeriod = useCallback(() => {
    history.push('/holiday-periods/new')
  }, [history])

  const [questionnaireToDelete, setQuestionnaireToDelete] = useState<UUID>()
  const onDeleteQuestionnaire = useCallback(
    () =>
      questionnaireToDelete
        ? deleteQuestionnaire(questionnaireToDelete)
        : Promise.reject(),
    [questionnaireToDelete]
  )
  const navigateToNewQuestionnaire = useCallback(() => {
    history.push('/holiday-periods/questionnaire/new')
  }, [history])

  return (
    <Container>
      <ContentArea opaque>
        <H1>{i18n.titles.holidayPeriods}</H1>

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
                          history.push(`/holiday-periods/${holiday.id}`)
                        }
                      />
                      <IconButton
                        icon={faTrash}
                        data-qa="btn-delete"
                        onClick={() => setPeriodToDelete(holiday.id)}
                      />
                    </FixedSpaceRow>
                  </Td>
                </Tr>
              ))}
            </Tbody>
          </Table>
        ))}

        <H1>{i18n.holidayQuestionnaires.questionnaires}</H1>

        <AddButtonRow
          onClick={navigateToNewQuestionnaire}
          text={i18n.common.addNew}
          data-qa="add-questionnaire-button"
        />
        {renderResult(questionnaires, (questionnaires) => (
          <Table>
            <Thead>
              <Tr>
                <Th>{i18n.holidayQuestionnaires.holidayPeriod}</Th>
                <Th>{i18n.holidayQuestionnaires.active}</Th>
                <Th />
              </Tr>
            </Thead>
            <Tbody>
              {questionnaires.map((row) => (
                <Tr key={row.id} data-qa="questionnaire-row">
                  <Td data-qa="period">{row.period.format()}</Td>
                  <Td data-qa="active">{row.active.format()}</Td>
                  <Td>
                    <FixedSpaceRow spacing="s">
                      <IconButton
                        icon={faPen}
                        data-qa="btn-edit"
                        onClick={() =>
                          history.push(
                            `/holiday-periods/questionnaire/${row.id}`
                          )
                        }
                      />
                      <IconButton
                        icon={faTrash}
                        data-qa="btn-delete"
                        onClick={() => setQuestionnaireToDelete(row.id)}
                      />
                    </FixedSpaceRow>
                  </Td>
                </Tr>
              ))}
            </Tbody>
          </Table>
        ))}

        {periodToDelete && (
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
              refreshPeriods()
            }}
          />
        )}

        {questionnaireToDelete && (
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
              refreshQuestionnaires()
            }}
          />
        )}
      </ContentArea>
    </Container>
  )
})
