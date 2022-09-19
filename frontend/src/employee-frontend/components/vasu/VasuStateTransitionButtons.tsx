// SPDX-FileCopyrightText: 2017-2022 City of Espoo
//
// SPDX-License-Identifier: LGPL-2.1-or-later

import React, { useState } from 'react'
import { useNavigate } from 'react-router-dom'
import styled from 'styled-components'

import { Result } from 'lib-common/api'
import { Action } from 'lib-common/generated/action'
import {
  VasuDocumentEventType,
  VasuDocumentState
} from 'lib-common/generated/api-types/vasu'
import { UUID } from 'lib-common/types'
import { useRestApi } from 'lib-common/utils/useRestApi'
import RoundIcon from 'lib-components/atoms/RoundIcon'
import Button from 'lib-components/atoms/buttons/Button'
import ErrorSegment from 'lib-components/atoms/state/ErrorSegment'
import { SpinnerSegment } from 'lib-components/atoms/state/Spinner'
import ButtonContainer from 'lib-components/layout/ButtonContainer'
import FullWidthDiv from 'lib-components/layout/FullWidthDiv'
import { FixedSpaceRow } from 'lib-components/layout/flex-helpers'
import InfoModal from 'lib-components/molecules/modals/InfoModal'
import { defaultMargins } from 'lib-components/white-space'
import colors from 'lib-customizations/common'
import { faCheck, faQuestion } from 'lib-icons'

import { useTranslation } from '../../state/i18n'

import { updateDocumentState } from './api'
import { LeaveVasuPageButton } from './components/LeaveVasuPageButton'

const PublishingDisclaimer = styled(FixedSpaceRow)`
  justify-content: flex-end;
  padding-top: ${defaultMargins.s};
`

export function VasuStateTransitionButtons({
  childId,
  documentId,
  permittedActions,
  state
}: {
  childId: UUID
  documentId: UUID
  permittedActions: Action.VasuDocument[]
  state: VasuDocumentState
}) {
  const { i18n } = useTranslation()
  const navigate = useNavigate()

  const [updateResult, setUpdateResult] = useState<Result<null>>()
  const updateState = useRestApi(updateDocumentState, setUpdateResult)

  const update = (eventType: VasuDocumentEventType) =>
    updateState({ documentId, eventType })

  const [selectedEventType, setSelectedEventType] =
    useState<VasuDocumentEventType>()

  const getStateTransitionButton = (
    eventType: VasuDocumentEventType,
    primary = true
  ) => (
    <Button
      text={i18n.vasu.transitions[eventType].buttonText}
      onClick={() => setSelectedEventType(eventType)}
      primary={primary}
      data-qa={`transition-button-${eventType}`}
    />
  )

  const isMovedToReadyAllowed =
    state === 'DRAFT' && permittedActions.includes('EVENT_MOVED_TO_READY')
  const isMovedToReviewedAllowed =
    state === 'READY' && permittedActions.includes('EVENT_MOVED_TO_REVIEWED')

  return (
    <FullWidthDiv>
      {selectedEventType && !updateResult?.isSuccess && (
        <InfoModal
          type="info"
          title={i18n.vasu.transitions[selectedEventType].confirmTitle}
          icon={faQuestion}
          text={
            ['PUBLISHED', 'MOVED_TO_READY', 'MOVED_TO_REVIEWED'].includes(
              selectedEventType
            )
              ? i18n.vasu.transitions.guardiansWillBeNotified
              : undefined
          }
          resolve={{
            action: () => update(selectedEventType),
            label: i18n.vasu.transitions[selectedEventType].confirmAction,
            disabled: updateResult?.isLoading
          }}
          reject={{
            action: () => {
              setUpdateResult(undefined)
              setSelectedEventType(undefined)
            },
            label: i18n.common.goBack
          }}
        >
          {updateResult?.isLoading && <SpinnerSegment />}
          {updateResult?.isFailure && <ErrorSegment />}
        </InfoModal>
      )}
      {selectedEventType && updateResult?.isSuccess && (
        <InfoModal
          type="success"
          title={i18n.vasu.transitions[selectedEventType].successTitle}
          icon={faCheck}
          text={i18n.vasu.transitions[selectedEventType].successText}
          resolve={{
            action: () => navigate(`/child-information/${childId}`),
            label: i18n.common.ok
          }}
        />
      )}
      <ButtonContainer>
        {isMovedToReadyAllowed && getStateTransitionButton('MOVED_TO_READY')}
        {isMovedToReviewedAllowed &&
          getStateTransitionButton('MOVED_TO_REVIEWED')}
        {state === 'REVIEWED' && (
          <>
            {permittedActions.includes('EVENT_RETURNED_TO_READY') &&
              getStateTransitionButton('RETURNED_TO_READY')}
            {permittedActions.includes('EVENT_MOVED_TO_CLOSED') &&
              getStateTransitionButton('MOVED_TO_CLOSED')}
          </>
        )}
        {state === 'CLOSED' ? (
          permittedActions.includes('EVENT_RETURNED_TO_REVIEWED') &&
          getStateTransitionButton('RETURNED_TO_REVIEWED', false)
        ) : (
          <>
            {permittedActions.includes('EVENT_PUBLISHED') &&
              getStateTransitionButton('PUBLISHED', false)}
            {permittedActions.includes('UPDATE') && (
              <Button
                data-qa="edit-button"
                text={i18n.common.edit}
                disabled={!permittedActions.includes('UPDATE')}
                onClick={() => navigate(`/vasu/${documentId}/edit`)}
              />
            )}
          </>
        )}
        <LeaveVasuPageButton childId={childId} />
      </ButtonContainer>
      {(isMovedToReadyAllowed || isMovedToReviewedAllowed) && (
        <PublishingDisclaimer alignItems="center" spacing="xs">
          <RoundIcon content="!" color={colors.main.m2} size="m" />
          <span>{i18n.vasu.transitions.vasuIsPublishedToGuardians}</span>
        </PublishingDisclaimer>
      )}
    </FullWidthDiv>
  )
}
