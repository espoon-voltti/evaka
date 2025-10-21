// SPDX-FileCopyrightText: 2017-2025 City of Espoo
//
// SPDX-License-Identifier: LGPL-2.1-or-later

import orderBy from 'lodash/orderBy'
import React, { useState } from 'react'

import type {
  AssistanceNeedDecisionId,
  ChildId
} from 'lib-common/generated/api-types/shared'
import { useMutation, useQueryResult } from 'lib-common/query'
import Title from 'lib-components/atoms/Title'
import { Table, Tbody, Th, Thead, Tr } from 'lib-components/layout/Table'
import { InfoBox } from 'lib-components/molecules/MessageBoxes'
import InfoModal from 'lib-components/molecules/modals/InfoModal'
import { P } from 'lib-components/typography'
import { faQuestion } from 'lib-icons'

import { useTranslation } from '../../state/i18n'
import { renderResult } from '../async-rendering'

import AssistanceNeedDecisionSectionRow from './AssistanceNeedDecisionSectionRow'
import {
  assistanceNeedDecisionsQuery,
  deleteAssistanceNeedDecisionMutation
} from './queries'

export interface Props {
  id: ChildId
}

export default React.memo(function AssistanceNeedDecisionSection({
  id
}: Props) {
  const { i18n } = useTranslation()

  const assistanceNeedDecisionsResult = useQueryResult(
    assistanceNeedDecisionsQuery({
      childId: id
    })
  )

  const [removingDecision, setRemovingDecision] =
    useState<AssistanceNeedDecisionId>()

  return (
    <div>
      {!!removingDecision && (
        <DeleteDecisionModal
          onClose={() => setRemovingDecision(undefined)}
          childId={id}
          decisionId={removingDecision}
        />
      )}

      <Title size={4}>
        {i18n.childInformation.assistanceNeedDecision.sectionTitle}
      </Title>
      <P>{i18n.childInformation.assistanceNeedDecision.description}</P>
      <InfoBox
        message={i18n.childInformation.assistanceNeedDecision.deprecated}
      />
      {renderResult(assistanceNeedDecisionsResult, (decisions) =>
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
  childId,
  decisionId,
  onClose
}: {
  childId: ChildId
  decisionId: AssistanceNeedDecisionId
  onClose: () => void
}) {
  const { i18n } = useTranslation()
  const { mutateAsync: deleteAssistanceNeedDecision } = useMutation(
    deleteAssistanceNeedDecisionMutation
  )

  return (
    <InfoModal
      type="warning"
      title={i18n.childInformation.assistanceNeedDecision.modal.title}
      text={i18n.childInformation.assistanceNeedDecision.modal.description}
      icon={faQuestion}
      reject={{
        action: () => onClose(),
        label: i18n.common.doNotRemove
      }}
      resolve={{
        async action() {
          await deleteAssistanceNeedDecision({ id: decisionId, childId })
          onClose()
        },
        label: i18n.childInformation.assistanceNeedDecision.modal.delete
      }}
    />
  )
})
