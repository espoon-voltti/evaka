// SPDX-FileCopyrightText: 2017-2024 City of Espoo
//
// SPDX-License-Identifier: LGPL-2.1-or-later

import React, { useMemo, useState } from 'react'
import { useNavigate } from 'react-router'
import styled from 'styled-components'

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
import type { MenuItem } from '../common/EllipsisMenu'
import EllipsisMenu from '../common/EllipsisMenu'

import ActionCheckbox from './ActionCheckbox'
import PrimaryAction from './PrimaryAction'
import {
  cancelApplicationMutation,
  simpleApplicationActionMutation
} from './queries'

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

export function isSimpleApplicationMutationAction(
  action: ApplicationAction
): action is SimpleApplicationMutationAction {
  return 'actionType' in action
}

export type ApplicationAction = SimpleApplicationMutationAction | OnClickAction

type Props = {
  application: ApplicationSummary
  actionInProgress: boolean
  onActionStarted: () => void
  onActionEnded: () => void
}

export default React.memo(function ApplicationActions({
  application,
  actionInProgress,
  onActionStarted,
  onActionEnded
}: Props) {
  const navigate = useNavigate()
  const { i18n } = useTranslation()
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
              void navigate(`/applications/${application.id}/decisions`)
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

  const primaryAction = useMemo(
    () => actions.find((action) => action.primary),
    [actions]
  )

  return (
    <>
      <ActionsContainer>
        <PrimaryAction
          applicationId={application.id}
          action={primaryAction}
          actionInProgress={actionInProgress}
          onActionStarted={onActionStarted}
          onActionEnded={onActionEnded}
        />
        <ActionMenu
          applicationId={application.id}
          actions={actions}
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
            <FixedSpaceRow spacing="XL">
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
