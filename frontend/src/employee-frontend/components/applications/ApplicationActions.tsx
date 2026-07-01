// SPDX-FileCopyrightText: 2017-2024 City of Espoo
//
// SPDX-License-Identifier: LGPL-2.1-or-later

import React, { useContext, useMemo, useState } from 'react'
import styled from 'styled-components'
import { useLocation } from 'wouter'

import type { Action } from 'lib-common/generated/action'
import type {
  ApplicationSummary,
  SimpleApplicationAction as SimpleApplicationActionType
} from 'lib-common/generated/api-types/application'
import type { ApplicationId } from 'lib-common/generated/api-types/shared'
import { useMutation } from 'lib-common/query'
import Radio from 'lib-components/atoms/form/Radio'
import {
  FixedSpaceColumn,
  FixedSpaceRow
} from 'lib-components/layout/flex-helpers'
import { MutateFormModal } from 'lib-components/molecules/modals/FormModal'
import { Label } from 'lib-components/typography'

import { useTranslation } from '../../state/i18n'
import { UserContext } from '../../state/user'
import type { MenuItem } from '../common/EllipsisMenu'
import EllipsisMenu from '../common/EllipsisMenu'

import ActionCheckbox from './ActionCheckbox'
import DecisionReasoningChips from './DecisionReasoningChips'
import PrimaryAction from './PrimaryAction'
import {
  cancelApplicationMutation,
  simpleApplicationActionMutation
} from './queries'
import { isSimpleApplicationMutationAction } from './utils'

export type BaseAction = {
  id: string
  label: string
  primary?: boolean
}

export type SimpleApplicationMutationAction = BaseAction & {
  actionType: SimpleApplicationActionType
}

export type OnClickAction = BaseAction & {
  onClick: () => void
}

export type ApplicationAction = SimpleApplicationMutationAction | OnClickAction

export const ACTION_TYPE_TO_PERMISSION: Partial<
  Record<SimpleApplicationActionType, Action.Application>
> = {
  MOVE_TO_WAITING_PLACEMENT: 'MOVE_TO_WAITING_PLACEMENT',
  RETURN_TO_SENT: 'RETURN_TO_SENT',
  CANCEL_PLACEMENT_PLAN: 'CANCEL_PLACEMENT_PLAN',
  SEND_DECISIONS_WITHOUT_PROPOSAL: 'SEND_DECISIONS_WITHOUT_PROPOSAL',
  SEND_PLACEMENT_PROPOSAL: 'SEND_PLACEMENT_PROPOSAL',
  WITHDRAW_PLACEMENT_PROPOSAL: 'WITHDRAW_PLACEMENT_PROPOSAL',
  CONFIRM_DECISION_MAILED: 'CONFIRM_DECISIONS_MAILED'
}

// Every menu item is gated on the action it ultimately performs or navigates to, so items
// whose underlying action is not permitted — including ones that open a confirmation modal
// (cancel) or navigate to a permission-gated page (verify / placement / decisions) — are
// hidden rather than leading the user to something they cannot do or reach.
const ACTION_ID_TO_PERMISSION: Record<string, Action.Application> = {
  'move-to-waiting-placement': 'MOVE_TO_WAITING_PLACEMENT',
  'return-to-sent': 'RETURN_TO_SENT',
  'cancel-application': 'CANCEL',
  'create-placement-plan': 'READ_PLACEMENT_PLAN_DRAFT',
  check: 'VERIFY',
  'cancel-placement-plan': 'CANCEL_PLACEMENT_PLAN',
  'edit-decisions': 'READ_DECISION_DRAFT',
  'send-decisions-without-proposal': 'SEND_DECISIONS_WITHOUT_PROPOSAL',
  'send-placement-proposal': 'SEND_PLACEMENT_PROPOSAL',
  'withdraw-placement-proposal': 'WITHDRAW_PLACEMENT_PROPOSAL',
  'confirm-decision-mailed': 'CONFIRM_DECISIONS_MAILED'
}

type Props = {
  application: ApplicationSummary
  permittedActions: Action.Application[]
  actionInProgress: boolean
  onActionStarted: () => void
  onActionEnded: () => void
}

export default React.memo(function ApplicationActions({
  application,
  permittedActions,
  actionInProgress,
  onActionStarted,
  onActionEnded
}: Props) {
  const [, navigate] = useLocation()
  const { i18n } = useTranslation()
  const { featureConfig } = useContext(UserContext)
  const [confirmingApplicationCancel, setConfirmingApplicationCancel] =
    useState(false)

  const actions: ApplicationAction[] = useMemo(() => {
    switch (application.status) {
      case 'CREATED':
        return []
      case 'SENT':
        return [
          {
            id: 'move-to-waiting-placement',
            label: i18n.applications.actions.moveToWaitingPlacement,
            actionType: 'MOVE_TO_WAITING_PLACEMENT'
          },
          {
            id: 'cancel-application',
            label: i18n.applications.actions.cancelApplication,
            onClick: () => setConfirmingApplicationCancel(true)
          }
        ]
      case 'WAITING_PLACEMENT':
        return [
          {
            id: 'return-to-sent',
            label: i18n.applications.actions.returnToSent,
            actionType: 'RETURN_TO_SENT'
          },
          {
            id: 'cancel-application',
            label: i18n.applications.actions.cancelApplication,
            onClick: () => setConfirmingApplicationCancel(true)
          },
          ...(application.checkedByAdmin
            ? [
                {
                  id: 'create-placement-plan',
                  label: i18n.applications.actions.createPlacementPlan,
                  onClick: () =>
                    navigate(`/applications/${application.id}/placement`),
                  primary: true
                }
              ]
            : [
                {
                  id: 'check',
                  label: i18n.applications.actions.check,
                  onClick: () => navigate(`/applications/${application.id}`),
                  primary: true
                }
              ])
        ]
      case 'WAITING_DECISION':
        return [
          {
            id: 'cancel-placement-plan',
            label: i18n.applications.actions.cancelPlacementPlan,
            actionType: 'CANCEL_PLACEMENT_PLAN'
          },
          {
            id: 'edit-decisions',
            label: i18n.applications.actions.editDecisions,
            onClick: () => {
              navigate(`/applications/${application.id}/decisions`)
            },
            primary: true
          },
          {
            id: 'send-decisions-without-proposal',
            label: i18n.applications.actions.sendDecisionsWithoutProposal,
            actionType: 'SEND_DECISIONS_WITHOUT_PROPOSAL'
          },
          {
            id: 'send-placement-proposal',
            label: i18n.applications.actions.sendPlacementProposal,
            actionType: 'SEND_PLACEMENT_PROPOSAL'
          }
        ]
      case 'WAITING_UNIT_CONFIRMATION':
        return [
          {
            id: 'withdraw-placement-proposal',
            label: i18n.applications.actions.withdrawPlacementProposal,
            actionType: 'WITHDRAW_PLACEMENT_PROPOSAL'
          }
        ]
      case 'WAITING_MAILING':
        return [
          {
            id: 'confirm-decision-mailed',
            label: i18n.applications.actions.confirmDecisionMailed,
            actionType: 'CONFIRM_DECISION_MAILED'
          }
        ]
      case 'WAITING_CONFIRMATION':
        return []
      case 'ACTIVE':
        return []
      case 'REJECTED':
        return []
      case 'CANCELLED':
        return [
          {
            id: 'return-to-sent',
            label: i18n.applications.actions.returnToSent,
            actionType: 'RETURN_TO_SENT'
          }
        ]
    }
  }, [application, navigate, i18n.applications.actions])

  const permittedActionsList = useMemo(
    () =>
      actions.filter((action) => {
        const permission = ACTION_ID_TO_PERMISSION[action.id]
        return permission !== undefined && permittedActions.includes(permission)
      }),
    [actions, permittedActions]
  )

  const primaryAction = useMemo(
    () => permittedActionsList.find((action) => action.primary),
    [permittedActionsList]
  )

  const showReasoningChips =
    featureConfig?.decisionReasoningsEnabled === true &&
    primaryAction?.id === 'edit-decisions'

  return (
    <>
      <ActionsContainer>
        <FixedSpaceColumn $spacing="xs" $alignItems="flex-start">
          <PrimaryAction
            applicationId={application.id}
            action={primaryAction}
            actionInProgress={actionInProgress}
            onActionStarted={onActionStarted}
            onActionEnded={onActionEnded}
          />
          {showReasoningChips && (
            <DecisionReasoningChips
              individualReasoningCount={application.individualReasoningCount}
              reasoningWarningCount={application.reasoningWarningCount}
              individualTooltip={i18n.applications.decisionReasoning.individualCountTooltip(
                application.individualReasoningCount
              )}
              warningTooltip={
                i18n.applications.decisionReasoning.genericNotReadyTooltip
              }
            />
          )}
        </FixedSpaceColumn>
        <ActionMenu
          applicationId={application.id}
          actions={permittedActionsList}
          actionInProgress={actionInProgress}
        />
        <ActionCheckbox applicationId={application.id} />
      </ActionsContainer>
      {confirmingApplicationCancel && (
        <ConfirmCancelApplicationModal
          application={application}
          onClose={() => setConfirmingApplicationCancel(false)}
        />
      )}
    </>
  )
})

const ConfirmCancelApplicationModal = React.memo(
  function ConfirmCancelApplicationModal({
    application,
    onClose
  }: {
    application: ApplicationSummary
    onClose: () => void
  }) {
    const { i18n } = useTranslation()
    const [confidential, setConfidential] = useState<boolean | null>(null)
    return (
      <MutateFormModal
        type="warning"
        title={i18n.applications.actions.cancelApplicationConfirm}
        resolveMutation={cancelApplicationMutation}
        resolveAction={() => ({
          applicationId: application.id,
          confidential
        })}
        resolveLabel={i18n.common.confirm}
        resolveDisabled={
          application.confidential === null && confidential === null
        }
        rejectAction={onClose}
        rejectLabel={i18n.common.cancel}
        onSuccess={onClose}
      >
        {application.confidential === null && (
          <FixedSpaceColumn>
            <Label>
              {i18n.applications.actions.cancelApplicationConfidentiality}
            </Label>
            <FixedSpaceRow $spacing="XL">
              <Radio
                checked={confidential === true}
                label={i18n.common.yes}
                onChange={() => setConfidential(true)}
                data-qa="confidential-yes"
              />
              <Radio
                checked={confidential === false}
                label={i18n.common.no}
                onChange={() => setConfidential(false)}
                data-qa="confidential-no"
              />
            </FixedSpaceRow>
          </FixedSpaceColumn>
        )}
      </MutateFormModal>
    )
  }
)

const ActionsContainer = styled.div`
  display: flex;
  flex-direction: row;
  justify-content: flex-end;
`

const ActionMenu = React.memo(function ActionMenu({
  applicationId,
  actions,
  actionInProgress
}: {
  applicationId: ApplicationId
  actions: ApplicationAction[]
  actionInProgress: boolean
}) {
  const { mutateAsync } = useMutation(simpleApplicationActionMutation)
  const menuItems: MenuItem[] = useMemo(
    () =>
      actions.map((action) => ({
        id: action.id,
        label: action.label,
        onClick: isSimpleApplicationMutationAction(action)
          ? () => mutateAsync({ applicationId, action: action.actionType })
          : action.onClick,
        disabled: actionInProgress
      })),
    [applicationId, actions, actionInProgress, mutateAsync]
  )
  return <EllipsisMenu items={menuItems} data-qa="application-actions-menu" />
})
