// SPDX-FileCopyrightText: 2017-2023 City of Espoo
//
// SPDX-License-Identifier: LGPL-2.1-or-later

import orderBy from 'lodash/orderBy'
import React, { useContext, useRef, useState } from 'react'
import { useNavigate } from 'react-router-dom'

import { ChildState, ChildContext } from 'employee-frontend/state/child'
import { useMutationResult, useQueryResult } from 'lib-common/query'
import { UUID } from 'lib-common/types'
import Title from 'lib-components/atoms/Title'
import AddButton from 'lib-components/atoms/buttons/AddButton'
import { Table, Tbody, Th, Thead, Tr } from 'lib-components/layout/Table'
import { AsyncFormModal } from 'lib-components/molecules/modals/FormModal'
import { P } from 'lib-components/typography'
import { faQuestion } from 'lib-icons'

import { useTranslation } from '../../state/i18n'
import { renderResult } from '../async-rendering'

import { TitleRow } from './AssistanceNeedDecisionSection'
import AssistanceNeedPreschoolDecisionSectionRow from './AssistanceNeedPreschoolDecisionSectionRow'
import {
  assistanceNeedPreschoolDecisionBasicsQuery,
  createAssistanceNeedPreschoolDecisionMutation,
  deleteAssistanceNeedPreschoolDecisionMutation
} from './queries'

export interface Props {
  childId: UUID
}

export default React.memo(function AssistanceNeedPreschoolDecisionSection({
  childId
}: Props) {
  const { i18n } = useTranslation()
  const { permittedActions } = useContext<ChildState>(ChildContext)
  const refSectionTop = useRef(null)

  const { mutateAsync: createDecision, isPending: creatingDecision } =
    useMutationResult(createAssistanceNeedPreschoolDecisionMutation)

  const navigate = useNavigate()

  const assistanceNeedDecisions = useQueryResult(
    assistanceNeedPreschoolDecisionBasicsQuery(childId)
  )

  const [removingDecision, setRemovingDecision] = useState<UUID>()

  return (
    <div ref={refSectionTop}>
      {!!removingDecision && (
        <DeleteDecisionModal
          onClose={() => {
            setRemovingDecision(undefined)
          }}
          childId={childId}
          decisionId={removingDecision}
        />
      )}

      <TitleRow>
        <Title size={4}>
          {i18n.childInformation.assistanceNeedPreschoolDecision.sectionTitle}
        </Title>
        {permittedActions.has('CREATE_ASSISTANCE_NEED_PRESCHOOL_DECISION') && (
          <AddButton
            flipped
            text={i18n.childInformation.assistanceNeedDecision.create}
            onClick={async () => {
              const res = await createDecision(childId)
              if (res.isSuccess) {
                navigate(
                  `/child-information/${childId}/assistance-need-preschool-decisions/${res.value.id}/edit`
                )
              }
            }}
            disabled={creatingDecision}
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
                  <AssistanceNeedPreschoolDecisionSectionRow
                    key={res.decision.id}
                    decision={res}
                    childId={childId}
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
  childId,
  decisionId,
  onClose
}: {
  childId: UUID
  decisionId: UUID
  onClose: () => void
}) {
  const { i18n } = useTranslation()
  const { mutateAsync: deleteDecision } = useMutationResult(
    deleteAssistanceNeedPreschoolDecisionMutation
  )
  return (
    <AsyncFormModal
      type="warning"
      title={i18n.childInformation.assistanceNeedDecision.modal.title}
      text={i18n.childInformation.assistanceNeedDecision.modal.description}
      icon={faQuestion}
      rejectAction={onClose}
      rejectLabel={i18n.common.doNotRemove}
      resolveAction={async () => deleteDecision({ childId, id: decisionId })}
      resolveLabel={i18n.childInformation.assistanceNeedDecision.modal.delete}
      onSuccess={onClose}
    />
  )
})
