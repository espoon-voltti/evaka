// SPDX-FileCopyrightText: 2017-2021 City of Espoo
//
// SPDX-License-Identifier: LGPL-2.1-or-later

import React, { useState } from 'react'
import { useHistory } from 'react-router'
import styled from 'styled-components'
import { Result } from '../../../lib-common/api'
import { UUID } from '../../../lib-common/types'
import { useRestApi } from '../../../lib-common/utils/useRestApi'
import Button from '../../../lib-components/atoms/buttons/Button'
import RoundIcon from '../../../lib-components/atoms/RoundIcon'
import ErrorSegment from '../../../lib-components/atoms/state/ErrorSegment'
import { SpinnerSegment } from '../../../lib-components/atoms/state/Spinner'
import ButtonContainer from '../../../lib-components/layout/ButtonContainer'
import InfoModal from '../../../lib-components/molecules/modals/InfoModal'
import { defaultMargins } from '../../../lib-components/white-space'
import colors from '../../../lib-customizations/common'
import { faCheck, faQuestion } from '../../../lib-icons'
import { useTranslation } from '../../state/i18n'
import { RequireRole } from '../../utils/roles'
import {
  updateDocumentState,
  VasuDocumentEventType,
  VasuDocumentState
} from './api'
import { FixedSpaceRow } from '../../../lib-components/layout/flex-helpers'

const PublishingDisclaimer = styled(FixedSpaceRow)`
  justify-content: flex-end;
  padding-top: ${defaultMargins.s};
`

export function VasuStateTransitionButtons({
  childId,
  documentId,
  state
}: {
  childId: UUID
  documentId: UUID
  state: VasuDocumentState
}) {
  const { i18n } = useTranslation()
  const history = useHistory()

  const [updateResult, setUpdateResult] = useState<Result<null>>()
  const updateState = useRestApi(updateDocumentState, setUpdateResult)

  const update = (eventType: VasuDocumentEventType) =>
    updateState({ documentId, eventType })

  const [
    selectedEventType,
    setSelectedEventType
  ] = useState<VasuDocumentEventType>()

  const getStateTransitionButton = (
    eventType: VasuDocumentEventType,
    primary = true
  ) => (
    <Button
      text={i18n.vasu.transitions[eventType].buttonText}
      onClick={() => setSelectedEventType(eventType)}
      primary={primary}
    />
  )

  return (
    <div>
      {selectedEventType && !updateResult?.isSuccess && (
        <InfoModal
          iconColour="blue"
          title={i18n.vasu.transitions[selectedEventType].confirmTitle}
          icon={faQuestion}
          text={
            ['PUBLISHED', 'MOVED_TO_READY', 'MOVED_TO_REVIEWED'].includes(
              selectedEventType
            )
              ? i18n.vasu.transitions.guardiansWillBeNotified
              : undefined
          }
          resolveDisabled={updateResult?.isLoading}
          resolve={{
            action: () => update(selectedEventType),
            label: i18n.vasu.transitions[selectedEventType].confirmAction
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
          iconColour="green"
          title={i18n.vasu.transitions[selectedEventType].successTitle}
          icon={faCheck}
          text={i18n.vasu.transitions[selectedEventType].successText}
          resolve={{
            action: () => history.push(`/child-information/${childId}`),
            label: i18n.common.ok
          }}
        />
      )}
      <ButtonContainer>
        {state === 'DRAFT' && getStateTransitionButton('MOVED_TO_READY')}
        {state === 'READY' && getStateTransitionButton('MOVED_TO_REVIEWED')}
        {state === 'REVIEWED' && (
          <>
            <RequireRole oneOf={['ADMIN', 'UNIT_SUPERVISOR']}>
              {getStateTransitionButton('RETURNED_TO_READY')}
              <RequireRole oneOf={['ADMIN']}>
                {getStateTransitionButton('MOVED_TO_CLOSED')}
              </RequireRole>
            </RequireRole>
          </>
        )}
        {state === 'CLOSED' ? (
          <RequireRole oneOf={['ADMIN']}>
            {getStateTransitionButton('RETURNED_TO_REVIEWED', false)}
          </RequireRole>
        ) : (
          <>
            {getStateTransitionButton('PUBLISHED', false)}
            <Button
              text={i18n.common.edit}
              onClick={() => history.push(`/vasu/${documentId}/edit`)}
            />
          </>
        )}
      </ButtonContainer>
      {(state === 'DRAFT' || state === 'READY') && (
        <PublishingDisclaimer alignItems="center" spacing="xs">
          <RoundIcon content="!" color={colors.primary} size="m" />
          <span>{i18n.vasu.transitions.vasuIsPublishedToGuardians}</span>
        </PublishingDisclaimer>
      )}
    </div>
  )
}
