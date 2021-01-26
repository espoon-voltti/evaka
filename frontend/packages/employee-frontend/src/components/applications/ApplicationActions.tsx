// SPDX-FileCopyrightText: 2017-2020 City of Espoo
//
// SPDX-License-Identifier: LGPL-2.1-or-later

import React, { useContext, useMemo, useState } from 'react'
import { useHistory } from 'react-router-dom'
import { useTranslation } from '~state/i18n'
import {
  cancelApplication,
  cancelPlacementPlan,
  confirmDecisionMailed,
  moveToWaitingPlacement,
  returnToSent,
  sendDecisionsWithoutProposal,
  sendPlacementProposal,
  setUnverified,
  setVerified,
  withdrawPlacementProposal
} from '~api/applications'
import { ApplicationListSummary } from '~types/application'
import { ApplicationSummaryStatusOptions } from '~components/common/Filters'
import ActionMenu from '~components/applications/ActionsMenu'
import PrimaryAction from '~components/applications/PrimaryAction'
import styled from 'styled-components'
import ActionCheckbox from '~components/applications/ActionCheckbox'
import { UIContext } from 'state/ui'

export type Action = {
  id: string
  label: string
  enabled: boolean
  disabled?: boolean
  onClick: () => undefined | void
  primaryStatus?: ApplicationSummaryStatusOptions
}

type Props = {
  application: ApplicationListSummary
  reloadApplications: () => void
}

export default React.memo(function ApplicationActions({
  application,
  reloadApplications
}: Props) {
  const history = useHistory()
  const { i18n } = useTranslation()
  const { setErrorMessage } = useContext(UIContext)
  const [actionInFlight, setActionInFlight] = useState(false)

  const handlePromise = (promise: Promise<void>) => {
    void promise
      .then(() => void reloadApplications())
      .catch(
        () =>
          void setErrorMessage({
            type: 'error',
            title: i18n.common.error.unknown,
            resolveLabel: i18n.common.ok
          })
      )
      .finally(() => void setActionInFlight(false))
  }

  const actions: Action[] = useMemo(
    () => [
      {
        id: 'move-to-waiting-placement',
        label: i18n.applications.actions.moveToWaitingPlacement,
        enabled: application.status === 'SENT',
        disabled: actionInFlight,
        onClick: () => {
          setActionInFlight(true)
          handlePromise(moveToWaitingPlacement(application.id))
        }
      },
      {
        id: 'return-to-sent',
        label: i18n.applications.actions.returnToSent,
        enabled: application.status === 'WAITING_PLACEMENT',
        disabled: actionInFlight,
        onClick: () => {
          setActionInFlight(true)
          handlePromise(returnToSent(application.id))
        }
      },
      {
        id: 'cancel-application',
        label: i18n.applications.actions.cancelApplication,
        enabled:
          application.status === 'SENT' ||
          application.status === 'WAITING_PLACEMENT',
        disabled: actionInFlight,
        onClick: () => {
          setActionInFlight(true)
          handlePromise(cancelApplication(application.id))
        }
      },
      {
        id: 'set-verified',
        label: i18n.applications.actions.setVerified,
        enabled:
          application.status === 'WAITING_PLACEMENT' &&
          !application.checkedByAdmin,
        disabled: actionInFlight,
        onClick: () => {
          setActionInFlight(true)
          handlePromise(setVerified(application.id))
        }
      },
      {
        id: 'set-unverified',
        label: i18n.applications.actions.setUnverified,
        enabled:
          application.status === 'WAITING_PLACEMENT' &&
          application.checkedByAdmin,
        disabled: actionInFlight,
        onClick: () => {
          setActionInFlight(true)
          handlePromise(setUnverified(application.id))
        }
      },
      {
        id: 'create-placement-plan',
        label: i18n.applications.actions.createPlacementPlan,
        enabled:
          application.checkedByAdmin &&
          application.status === 'WAITING_PLACEMENT',
        disabled: actionInFlight,
        onClick: () => {
          setActionInFlight(true)
          history.push(`/applications/${application.id}/placement`)
        },
        primaryStatus: 'WAITING_PLACEMENT'
      },
      {
        id: 'cancel-placement-plan',
        label: i18n.applications.actions.cancelPlacementPlan,
        enabled: application.status === 'WAITING_DECISION',
        disabled: actionInFlight,
        onClick: () => {
          setActionInFlight(true)
          handlePromise(cancelPlacementPlan(application.id))
        }
      },
      {
        id: 'edit-decisions',
        label: i18n.applications.actions.editDecisions,
        enabled: application.status === 'WAITING_DECISION',
        disabled: actionInFlight,
        onClick: () => {
          setActionInFlight(true)
          history.push(`/applications/${application.id}/decisions`)
        },
        primaryStatus: 'WAITING_DECISION'
      },
      {
        id: 'send-decisions-without-proposal',
        label: i18n.applications.actions.sendDecisionsWithoutProposal,
        enabled: application.status === 'WAITING_DECISION',
        disabled: actionInFlight,
        onClick: () => {
          setActionInFlight(true)
          handlePromise(sendDecisionsWithoutProposal(application.id))
        }
      },
      {
        id: 'send-placement-proposal',
        label: i18n.applications.actions.sendPlacementProposal,
        enabled: application.status === 'WAITING_DECISION',
        disabled: actionInFlight,
        onClick: () => {
          setActionInFlight(true)
          handlePromise(sendPlacementProposal(application.id))
        }
      },
      {
        id: 'withdraw-placement-proposal',
        label: i18n.applications.actions.withdrawPlacementProposal,
        enabled: application.status === 'WAITING_UNIT_CONFIRMATION',
        disabled: actionInFlight,
        onClick: () => {
          setActionInFlight(true)
          handlePromise(withdrawPlacementProposal(application.id))
        }
      },
      {
        id: 'confirm-decision-mailed',
        label: i18n.applications.actions.confirmDecisionMailed,
        enabled: application.status === 'WAITING_MAILING',
        disabled: actionInFlight,
        onClick: () => {
          setActionInFlight(true)
          handlePromise(confirmDecisionMailed(application.id))
        }
      }
    ],
    [i18n, application]
  )

  const applicableActions = actions.filter(({ enabled }) => enabled)
  const primaryAction = actions.find(
    (action) => action.primaryStatus === application.status
  )

  return (
    <ActionsContainer>
      <PrimaryAction action={primaryAction} />
      <ActionMenu actions={applicableActions} />
      <ActionCheckbox applicationId={application.id} />
    </ActionsContainer>
  )
})

const ActionsContainer = styled.div`
  display: flex;
  flex-direction: row;
  justify-content: flex-end;
`
