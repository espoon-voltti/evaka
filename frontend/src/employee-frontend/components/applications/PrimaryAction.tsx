// SPDX-FileCopyrightText: 2017-2024 City of Espoo
//
// SPDX-License-Identifier: LGPL-2.1-or-later

import React from 'react'
import styled from 'styled-components'

import type { ApplicationId } from 'lib-common/generated/api-types/shared'
import { Button } from 'lib-components/atoms/buttons/Button'
import { MutateButton } from 'lib-components/atoms/buttons/MutateButton'

import type { ApplicationAction } from './ApplicationActions'
import { simpleApplicationActionMutation } from './queries'
import { isSimpleApplicationMutationAction } from './utils'

type Props = {
  applicationId: ApplicationId
  action: ApplicationAction | undefined
  actionInProgress: boolean
  onActionStarted: () => void
  onActionEnded: () => void
  onDecisionReasoningBlocked: (applicationCount: number) => void
}

const PrimaryActionContainer = styled.div`
  padding-right: 16px;
`

export default React.memo(function PrimaryAction({
  applicationId,
  action,
  actionInProgress,
  onActionStarted,
  onActionEnded,
  onDecisionReasoningBlocked
}: Props) {
  return (
    <>
      {action && (
        <PrimaryActionContainer onClick={(e) => e.stopPropagation()}>
          {isSimpleApplicationMutationAction(action) ? (
            <MutateButton
              appearance="inline"
              text={action.label}
              mutation={simpleApplicationActionMutation}
              onClick={() => {
                onActionStarted()
                return { applicationId, action: action.actionType }
              }}
              onSuccess={onActionEnded}
              onFailure={(failure) => {
                if (failure.errorCode === 'DECISION_REASONING_NOT_FINALIZED') {
                  onDecisionReasoningBlocked(1)
                }
                onActionEnded()
              }}
              disabled={actionInProgress}
              data-qa={`primary-action-${action.id}`}
            />
          ) : (
            <Button
              appearance="inline"
              text={action.label}
              onClick={action.onClick}
              disabled={actionInProgress}
              data-qa={`primary-action-${action.id}`}
            />
          )}
        </PrimaryActionContainer>
      )}
    </>
  )
})
