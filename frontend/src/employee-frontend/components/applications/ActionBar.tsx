// SPDX-FileCopyrightText: 2017-2024 City of Espoo
//
// SPDX-License-Identifier: LGPL-2.1-or-later

import React, { useContext, useMemo } from 'react'

import { MutateButton } from 'lib-components/atoms/buttons/MutateButton'
import { FixedSpaceRow } from 'lib-components/layout/flex-helpers'

import StickyActionBar from '../../components/common/StickyActionBar'
import { ApplicationUIContext } from '../../state/application-ui'
import { useTranslation } from '../../state/i18n'
import { CheckedRowsInfo } from '../common/CheckedRowsInfo'

import { SimpleApplicationMutationAction } from './ApplicationActions'
import { simpleBatchActionMutation } from './queries'

export default React.memo(function ActionBar({
  actionInProgress,
  onActionStarted,
  onActionEnded
}: {
  actionInProgress: boolean
  onActionStarted: () => void
  onActionEnded: () => void
}) {
  const { i18n } = useTranslation()

  const {
    checkedIds,
    setCheckedIds,
    confirmedSearchFilters: searchFilters
  } = useContext(ApplicationUIContext)

  const actions: SimpleApplicationMutationAction[] = useMemo(() => {
    if (!searchFilters) return []

    switch (searchFilters.status) {
      case 'ALL':
        return []
      case 'SENT':
        return [
          {
            id: 'moveToWaitingPlacement',
            label: i18n.applications.actions.moveToWaitingPlacement,
            actionType: 'MOVE_TO_WAITING_PLACEMENT',
            primary: true
          }
        ]
      case 'WAITING_PLACEMENT':
        return [
          {
            id: 'returnToSent',
            label: i18n.applications.actions.returnToSent,
            actionType: 'RETURN_TO_SENT'
          }
        ]
      case 'WAITING_DECISION':
        return [
          {
            id: 'cancelPlacementPlan',
            label: i18n.applications.actions.cancelPlacementPlan,
            actionType: 'CANCEL_PLACEMENT_PLAN'
          },
          {
            id: 'sendDecisionsWithoutProposal',
            label: i18n.applications.actions.sendDecisionsWithoutProposal,
            actionType: 'SEND_DECISIONS_WITHOUT_PROPOSAL'
          },
          {
            id: 'sendPlacementProposal',
            label: i18n.applications.actions.sendPlacementProposal,
            primary: true,
            actionType: 'SEND_PLACEMENT_PROPOSAL'
          }
        ]
      case 'WAITING_UNIT_CONFIRMATION':
        return [
          {
            id: 'withdrawPlacementProposal',
            label: i18n.applications.actions.withdrawPlacementProposal,
            actionType: 'WITHDRAW_PLACEMENT_PROPOSAL'
          }
        ]
    }
  }, [searchFilters, i18n.applications.actions])

  return actions.length > 0 ? (
    <StickyActionBar align="right" data-qa="action-bar">
      {checkedIds.length > 0 ? (
        <CheckedRowsInfo>
          {i18n.applications.actions.checked(checkedIds.length)}
        </CheckedRowsInfo>
      ) : null}
      <FixedSpaceRow>
        {actions.map(({ id, label, actionType, primary }) => (
          <MutateButton
            key={id}
            mutation={simpleBatchActionMutation}
            onClick={() => {
              onActionStarted()
              return {
                action: actionType,
                body: { applicationIds: checkedIds }
              }
            }}
            disabled={actionInProgress || checkedIds.length === 0}
            onSuccess={() => {
              setCheckedIds([])
              onActionEnded()
            }}
            onFailure={onActionEnded}
            text={label}
            primary={primary}
            data-qa={`action-bar-${id}`}
          />
        ))}
      </FixedSpaceRow>
    </StickyActionBar>
  ) : null
})
