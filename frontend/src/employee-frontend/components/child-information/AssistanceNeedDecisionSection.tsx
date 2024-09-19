// SPDX-FileCopyrightText: 2017-2023 City of Espoo
//
// SPDX-License-Identifier: LGPL-2.1-or-later

import orderBy from 'lodash/orderBy'
import React, { useContext, useRef, useState } from 'react'
import { useNavigate } from 'react-router-dom'
import styled from 'styled-components'

import { ChildState, ChildContext } from 'employee-frontend/state/child'
import { wrapResult } from 'lib-common/api'
import DateRange from 'lib-common/date-range'
import LocalDate from 'lib-common/local-date'
import { UUID } from 'lib-common/types'
import { useApiState } from 'lib-common/utils/useRestApi'
import Title from 'lib-components/atoms/Title'
import AddButton from 'lib-components/atoms/buttons/AddButton'
import { Table, Tbody, Th, Thead, Tr } from 'lib-components/layout/Table'
import InfoModal from 'lib-components/molecules/modals/InfoModal'
import { P } from 'lib-components/typography'
import { faQuestion } from 'lib-icons'

import {
  createAssistanceNeedDecision,
  deleteAssistanceNeedDecision,
  getAssistanceNeedDecisions
} from '../../generated/api-clients/assistanceneed'
import { useTranslation } from '../../state/i18n'
import { renderResult } from '../async-rendering'

import AssistanceNeedDecisionSectionRow from './AssistanceNeedDecisionSectionRow'

const createAssistanceNeedDecisionResult = wrapResult(
  createAssistanceNeedDecision
)
const getAssistanceNeedDecisionsResult = wrapResult(getAssistanceNeedDecisions)
const deleteAssistanceNeedDecisionResult = wrapResult(
  deleteAssistanceNeedDecision
)

export const TitleRow = styled.div`
  display: flex;
  align-items: center;
  justify-content: space-between;
  margin: 30px 0;

  .title {
    margin: 0;
  }
`

export interface Props {
  id: UUID
}

export default React.memo(function AssistanceNeedDecisionSection({
  id
}: Props) {
  const { i18n } = useTranslation()
  const { permittedActions } = useContext<ChildState>(ChildContext)
  const refSectionTop = useRef(null)

  const [assistanceNeedDecisions, reloadDecisions] = useApiState(
    () => getAssistanceNeedDecisionsResult({ childId: id }),
    [id]
  )

  const navigate = useNavigate()

  const [isCreatingDecision, setIsCreatingDecision] = useState(false)

  const [removingDecision, setRemovingDecision] = useState<UUID>()

  return (
    <div ref={refSectionTop}>
      {!!removingDecision && (
        <DeleteDecisionModal
          onClose={(shouldRefresh) => {
            setRemovingDecision(undefined)
            if (shouldRefresh) {
              void reloadDecisions()
            }
          }}
          childId={id}
          decisionId={removingDecision}
        />
      )}

      <TitleRow>
        <Title size={4}>
          {i18n.childInformation.assistanceNeedDecision.sectionTitle}
        </Title>
        {permittedActions.has('CREATE_ASSISTANCE_NEED_DECISION') && (
          <AddButton
            flipped
            text={i18n.childInformation.assistanceNeedDecision.create}
            onClick={() => {
              setIsCreatingDecision(true)
              void createAssistanceNeedDecisionResult({
                childId: id,
                body: {
                  decision: {
                    assistanceLevels: [],
                    careMotivation: null,
                    decisionMade: null,
                    decisionMaker: {
                      employeeId: null,
                      title: null
                    },
                    decisionNumber: null,
                    expertResponsibilities: null,
                    guardianInfo: [],
                    guardiansHeardOn: null,
                    language: 'FI',
                    motivationForDecision: null,
                    otherRepresentativeDetails: null,
                    otherRepresentativeHeard: false,
                    pedagogicalMotivation: null,
                    preparedBy1: {
                      employeeId: null,
                      title: null,
                      phoneNumber: null
                    },
                    preparedBy2: {
                      employeeId: null,
                      title: null,
                      phoneNumber: null
                    },
                    selectedUnit: null,
                    sentForDecision: null,
                    serviceOptions: {
                      consultationSpecialEd: false,
                      fullTimeSpecialEd: false,
                      interpretationAndAssistanceServices: false,
                      partTimeSpecialEd: false,
                      specialAides: false
                    },
                    servicesMotivation: null,
                    status: 'DRAFT',
                    structuralMotivationDescription: null,
                    structuralMotivationOptions: {
                      additionalStaff: false,
                      childAssistant: false,
                      groupAssistant: false,
                      smallGroup: false,
                      smallerGroup: false,
                      specialGroup: false
                    },
                    validityPeriod: new DateRange(
                      LocalDate.todayInHelsinkiTz(),
                      null
                    ),
                    endDateNotKnown: false,
                    viewOfGuardians: null
                  }
                }
              }).then((decision) => {
                void reloadDecisions()
                setIsCreatingDecision(false)

                decision.map(({ id: decisionId }) =>
                  navigate(
                    `/child-information/${id}/assistance-need-decision/${
                      decisionId ?? ''
                    }/edit`
                  )
                )
              })
            }}
            disabled={isCreatingDecision}
            data-qa="assistance-need-decision-create-btn"
          />
        )}
      </TitleRow>
      <P>{i18n.childInformation.assistanceNeedDecision.description}</P>
      {renderResult(assistanceNeedDecisions, (decisions) =>
        decisions.length === 0 ? null : (
          <Table data-qa="table-of-assistance-need-decisions">
            <Thead>
              <Tr>
                <Th minimalWidth>
                  {i18n.childInformation.assistanceNeedDecision.table.form}
                </Th>
                <Th>
                  {i18n.childInformation.assistanceNeedDecision.table.inEffect}
                </Th>
                <Th>
                  {i18n.childInformation.assistanceNeedDecision.table.unit}
                </Th>
                <Th>
                  {
                    i18n.childInformation.assistanceNeedDecision.table
                      .sentToDecisionMaker
                  }
                </Th>
                <Th>
                  {
                    i18n.childInformation.assistanceNeedDecision.table
                      .decisionMadeOn
                  }
                </Th>
                <Th>
                  {i18n.childInformation.assistanceNeedDecision.table.status}
                </Th>
                <Th />
              </Tr>
            </Thead>
            <Tbody>
              {orderBy(decisions, ({ decision }) => decision.created).map(
                (res) => (
                  <AssistanceNeedDecisionSectionRow
                    key={res.decision.id}
                    decision={res}
                    childId={id}
                    onDelete={() => setRemovingDecision(res.decision.id)}
                  />
                )
              )}
            </Tbody>
          </Table>
        )
      )}
    </div>
  )
})

const DeleteDecisionModal = React.memo(function DeleteDecisionModal({
  decisionId,
  onClose
}: {
  childId: UUID
  decisionId: UUID
  onClose: (shouldRefresh: boolean) => void
}) {
  const { i18n } = useTranslation()
  return (
    <InfoModal
      type="warning"
      title={i18n.childInformation.assistanceNeedDecision.modal.title}
      text={i18n.childInformation.assistanceNeedDecision.modal.description}
      icon={faQuestion}
      reject={{
        action: () => onClose(false),
        label: i18n.common.doNotRemove
      }}
      resolve={{
        async action() {
          await deleteAssistanceNeedDecisionResult({ id: decisionId })
          onClose(true)
        },
        label: i18n.childInformation.assistanceNeedDecision.modal.delete
      }}
    />
  )
})
