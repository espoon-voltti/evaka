// SPDX-FileCopyrightText: 2017-2022 City of Espoo
//
// SPDX-License-Identifier: LGPL-2.1-or-later

import React, { useContext, useMemo, useState } from 'react'
import { useNavigate } from 'react-router-dom'
import styled from 'styled-components'

import { ApplicationSummary } from 'lib-common/generated/api-types/application'
import InfoModal from 'lib-components/molecules/modals/InfoModal'

import ActionCheckbox from '../../components/applications/ActionCheckbox'
import PrimaryAction from '../../components/applications/PrimaryAction'
import { simpleApplicationAction } from '../../generated/api-clients/application'
import { useTranslation } from '../../state/i18n'
import { UIContext } from '../../state/ui'
import EllipsisMenu, { MenuItem } from '../common/EllipsisMenu'
import { ApplicationSummaryStatusOptions } from '../common/Filters'

export type Action = {
  id: string
  label: string
  enabled: boolean
  disabled?: boolean
  onClick: () => undefined | void
  primaryStatus?: ApplicationSummaryStatusOptions
}

type Props = {
  application: ApplicationSummary
  reloadApplications: () => void
}

export default React.memo(function ApplicationActions({
  application,
  reloadApplications
}: Props) {
  const navigate = useNavigate()
  const { i18n } = useTranslation()
  const { setErrorMessage } = useContext(UIContext)
  const [actionInFlight, setActionInFlight] = useState(false)
  const [confirmingApplicationCancel, setConfirmingApplicationCancel] =
    useState(false)

  const handlePromise = (promise: Promise<void>) => {
    void promise
      .then(() => reloadApplications())
      .catch(() =>
        setErrorMessage({
          type: 'error',
          title: i18n.common.error.unknown,
          resolveLabel: i18n.common.ok
        })
      )
      .finally(() => {
        setActionInFlight(false)
        setConfirmingApplicationCancel(false)
      })
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
          handlePromise(
            simpleApplicationAction({
              applicationId: application.id,
              action: 'move-to-waiting-placement'
            })
          )
        }
      },
      {
        id: 'return-to-sent',
        label: i18n.applications.actions.returnToSent,
        enabled: application.status === 'WAITING_PLACEMENT',
        disabled: actionInFlight,
        onClick: () => {
          setActionInFlight(true)
          handlePromise(
            simpleApplicationAction({
              applicationId: application.id,
              action: 'return-to-sent'
            })
          )
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
          setConfirmingApplicationCancel(true)
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
          handlePromise(
            simpleApplicationAction({
              applicationId: application.id,
              action: 'set-verified'
            })
          )
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
          handlePromise(
            simpleApplicationAction({
              applicationId: application.id,
              action: 'set-unverified'
            })
          )
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
          navigate(`/applications/${application.id}/placement`)
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
          handlePromise(
            simpleApplicationAction({
              applicationId: application.id,
              action: 'cancel-placement-plan'
            })
          )
        }
      },
      {
        id: 'edit-decisions',
        label: i18n.applications.actions.editDecisions,
        enabled: application.status === 'WAITING_DECISION',
        disabled: actionInFlight,
        onClick: () => {
          setActionInFlight(true)
          navigate(`/applications/${application.id}/decisions`)
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
          handlePromise(
            simpleApplicationAction({
              applicationId: application.id,
              action: 'send-decisions-without-proposal'
            })
          )
        }
      },
      {
        id: 'send-placement-proposal',
        label: i18n.applications.actions.sendPlacementProposal,
        enabled: application.status === 'WAITING_DECISION',
        disabled: actionInFlight,
        onClick: () => {
          setActionInFlight(true)
          handlePromise(
            simpleApplicationAction({
              applicationId: application.id,
              action: 'send-placement-proposal'
            })
          )
        }
      },
      {
        id: 'withdraw-placement-proposal',
        label: i18n.applications.actions.withdrawPlacementProposal,
        enabled: application.status === 'WAITING_UNIT_CONFIRMATION',
        disabled: actionInFlight,
        onClick: () => {
          setActionInFlight(true)
          handlePromise(
            simpleApplicationAction({
              applicationId: application.id,
              action: 'withdraw-placement-proposal'
            })
          )
        }
      },
      {
        id: 'confirm-decision-mailed',
        label: i18n.applications.actions.confirmDecisionMailed,
        enabled: application.status === 'WAITING_MAILING',
        disabled: actionInFlight,
        onClick: () => {
          setActionInFlight(true)
          handlePromise(
            simpleApplicationAction({
              applicationId: application.id,
              action: 'confirm-decision-mailed'
            })
          )
        }
      }
    ],
    [i18n, application] // eslint-disable-line react-hooks/exhaustive-deps
  )

  const applicableActions = useMemo(
    () => actions.filter(({ enabled }) => enabled),
    [actions]
  )
  const primaryAction = useMemo(
    () => actions.find((action) => action.primaryStatus === application.status),
    [application.status, actions]
  )

  return (
    <>
      <ActionsContainer>
        <PrimaryAction action={primaryAction} />
        <ActionMenu actions={applicableActions} />
        <ActionCheckbox applicationId={application.id} />
      </ActionsContainer>
      {confirmingApplicationCancel && (
        <InfoModal
          type="warning"
          title={i18n.applications.actions.cancelApplicationConfirm}
          resolve={{
            action: () => {
              setActionInFlight(true)
              handlePromise(
                simpleApplicationAction({
                  applicationId: application.id,
                  action: 'cancel-application'
                })
              )
            },
            label: i18n.common.confirm
          }}
          reject={{
            action: () => setConfirmingApplicationCancel(false),
            label: i18n.common.cancel
          }}
        />
      )}
    </>
  )
})

const ActionsContainer = styled.div`
  display: flex;
  flex-direction: row;
  justify-content: flex-end;
`

const ActionMenu = React.memo(function ActionMenu({
  actions
}: {
  actions: MenuItem[]
}) {
  return <EllipsisMenu items={actions} data-qa="application-actions-menu" />
})
