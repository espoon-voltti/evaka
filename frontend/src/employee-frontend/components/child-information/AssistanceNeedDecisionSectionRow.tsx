// SPDX-FileCopyrightText: 2017-2022 City of Espoo
//
// SPDX-License-Identifier: LGPL-2.1-or-later

import React from 'react'
import { useNavigate } from 'react-router-dom'

import { AssistanceNeedDecisionBasicsResponse } from 'lib-common/generated/api-types/assistanceneed'
import IconButton from 'lib-components/atoms/buttons/IconButton'
import { Td, Tr } from 'lib-components/layout/Table'
import { FixedSpaceRow } from 'lib-components/layout/flex-helpers'
import { faFileAlt, faPen, faTrash } from 'lib-icons'

import { AssistanceNeedDecisionStatusChip } from './assistance-need/decision/common'

interface Props {
  decision: AssistanceNeedDecisionBasicsResponse
  childId: string
  onDelete: () => void
}

export default React.memo(function AssistanceNeedDecisionSectionRow({
  decision: { decision, permittedActions },
  childId,
  onDelete
}: Props) {
  const navigate = useNavigate()

  return (
    <Tr data-qa="table-assistance-need-decision-row">
      <Td>
        <FixedSpaceRow justifyContent="center">
          <IconButton
            icon={faFileAlt}
            onClick={() =>
              navigate(
                `/child-information/${childId}/assistance-need-decision/${
                  decision.id ?? ''
                }`
              )
            }
          />
        </FixedSpaceRow>
      </Td>
      <Td data-qa="assistance-need-decision-date">
        {!decision.startDate && !decision.endDate
          ? '-'
          : `${decision.startDate?.format() ?? ''}${
              decision.endDate ? ` – ${decision.endDate?.format()}` : ''
            }`}
      </Td>
      <Td data-qa="assistance-need-decision-unit-name">
        {decision.selectedUnit?.name ?? '-'}
      </Td>
      <Td data-qa="assistance-need-decision-sent-date">
        {decision.sentForDecision?.format() ?? '-'}
      </Td>
      <Td data-qa="assistance-need-decision-made-date">
        {decision.decisionMade?.format() ?? '-'}
      </Td>
      <Td>
        <AssistanceNeedDecisionStatusChip
          decisionStatus={decision.status}
          data-qa="decision-status"
        />
      </Td>
      <Td data-qa="assistance-need-decision-actions">
        {(decision.status === 'DRAFT' || decision.status === 'NEEDS_WORK') && (
          <FixedSpaceRow justifyContent="flex-end">
            {permittedActions.includes('UPDATE') && (
              <IconButton
                icon={faPen}
                onClick={() =>
                  navigate(
                    `/child-information/${childId}/assistance-need-decision/${
                      decision.id ?? ''
                    }/edit`
                  )
                }
              />
            )}
            {permittedActions.includes('DELETE') && (
              <IconButton icon={faTrash} onClick={onDelete} />
            )}
          </FixedSpaceRow>
        )}
      </Td>
    </Tr>
  )
})
