// SPDX-FileCopyrightText: 2017-2025 City of Espoo
//
// SPDX-License-Identifier: LGPL-2.1-or-later

import orderBy from 'lodash/orderBy'
import React, { useState } from 'react'

import type {
  AssistanceNeedPreschoolDecisionId,
  ChildId
} from 'lib-common/generated/api-types/shared'
import { useMutationResult, useQueryResult } from 'lib-common/query'
import Title from 'lib-components/atoms/Title'
import { Table, Tbody, Th, Thead, Tr } from 'lib-components/layout/Table'
import { InfoBox } from 'lib-components/molecules/MessageBoxes'
import { AsyncFormModal } from 'lib-components/molecules/modals/FormModal'
import { P } from 'lib-components/typography'
import { faQuestion } from 'lib-icons'

import { useTranslation } from '../../state/i18n'
import { renderResult } from '../async-rendering'

import AssistanceNeedPreschoolDecisionSectionRow from './AssistanceNeedPreschoolDecisionSectionRow'
import {
  assistanceNeedPreschoolDecisionBasicsQuery,
  deleteAssistanceNeedPreschoolDecisionMutation
} from './queries'

export interface Props {
  childId: ChildId
}

export default React.memo(function AssistanceNeedPreschoolDecisionSection({
  childId
}: Props) {
  const { i18n } = useTranslation()

  const assistanceNeedDecisions = useQueryResult(
    assistanceNeedPreschoolDecisionBasicsQuery({ childId })
  )

  const [removingDecision, setRemovingDecision] =
    useState<AssistanceNeedPreschoolDecisionId>()

  return (
    <div>
      {!!removingDecision && (
        <DeleteDecisionModal
          onClose={() => {
            setRemovingDecision(undefined)
          }}
          childId={childId}
          decisionId={removingDecision}
        />
      )}

      <Title size={4}>
        {i18n.childInformation.assistanceNeedPreschoolDecision.sectionTitle}
      </Title>
      <P>{i18n.childInformation.assistanceNeedDecision.description}</P>
      <InfoBox
        message={i18n.childInformation.assistanceNeedDecision.deprecated}
      />
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
              {orderBy(decisions, ({ decision }) => decision.createdAt).map(
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
  childId: ChildId
  decisionId: AssistanceNeedPreschoolDecisionId
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
