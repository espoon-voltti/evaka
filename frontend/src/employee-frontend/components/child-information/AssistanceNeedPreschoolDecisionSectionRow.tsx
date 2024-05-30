// SPDX-FileCopyrightText: 2017-2022 City of Espoo
//
// SPDX-License-Identifier: LGPL-2.1-or-later

import React from 'react'
import { useNavigate } from 'react-router-dom'

import { useTranslation } from 'employee-frontend/state/i18n'
import { AssistanceNeedPreschoolDecisionBasicsResponse } from 'lib-common/generated/api-types/assistanceneed'
import { AssistanceNeedDecisionStatusChip } from 'lib-components/assistance-need-decision/AssistanceNeedDecisionStatusChip'
import { IconOnlyButton } from 'lib-components/atoms/buttons/IconOnlyButton'
import { Td, Tr } from 'lib-components/layout/Table'
import { FixedSpaceRow } from 'lib-components/layout/flex-helpers'
import { faFileAlt, faPen, faTrash } from 'lib-icons'

interface Props {
  decision: AssistanceNeedPreschoolDecisionBasicsResponse
  childId: string
  onDelete: () => void
}

export default React.memo(function AssistanceNeedPreschoolDecisionSectionRow({
  decision: { decision, permittedActions },
  childId,
  onDelete
}: Props) {
  const navigate = useNavigate()

  const { i18n } = useTranslation()

  return (
    <Tr data-qa="table-assistance-need-decision-row">
      <Td>
        <FixedSpaceRow justifyContent="center">
          <IconOnlyButton
            icon={faFileAlt}
            onClick={() =>
              navigate(
                `/child-information/${childId}/assistance-need-preschool-decisions/${decision.id}`
              )
            }
            aria-label={i18n.common.open}
          />
        </FixedSpaceRow>
      </Td>
      <Td data-qa="assistance-need-decision-date">
        {decision.validFrom
          ? `${decision.validFrom.format()} - ${
              decision.validTo?.format() ?? ''
            }`
          : '-'}
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
          texts={i18n.childInformation.assistanceNeedDecision.statuses}
          data-qa="decision-status"
        />
      </Td>
      <Td data-qa="assistance-need-decision-actions">
        {(decision.status === 'DRAFT' || decision.status === 'NEEDS_WORK') && (
          <FixedSpaceRow justifyContent="flex-end">
            {permittedActions.includes('UPDATE') && (
              <IconOnlyButton
                icon={faPen}
                onClick={() =>
                  navigate(
                    `/child-information/${childId}/assistance-need-preschool-decisions/${decision.id}/edit`
                  )
                }
                aria-label={i18n.common.edit}
              />
            )}
            {permittedActions.includes('DELETE') && (
              <IconOnlyButton
                icon={faTrash}
                onClick={onDelete}
                aria-label={i18n.common.remove}
              />
            )}
          </FixedSpaceRow>
        )}
      </Td>
    </Tr>
  )
})
