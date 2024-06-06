// SPDX-FileCopyrightText: 2017-2022 City of Espoo
//
// SPDX-License-Identifier: LGPL-2.1-or-later

import React, { useContext, useEffect, useRef, useState } from 'react'

import { Button } from 'lib-components/atoms/buttons/Button'
import { FixedSpaceRow } from 'lib-components/layout/flex-helpers'

import StickyActionBar from '../../components/common/StickyActionBar'
import { simpleBatchAction } from '../../generated/api-clients/application'
import { ApplicationUIContext } from '../../state/application-ui'
import { useTranslation } from '../../state/i18n'
import { UIContext } from '../../state/ui'
import { CheckedRowsInfo } from '../common/CheckedRowsInfo'

type Action = {
  id: string
  label: string
  primary: boolean
  enabled: boolean
  disabled: boolean
  onClick: () => void
}

type Props = {
  reloadApplications: () => void
  fullWidth?: boolean
}

export default React.memo(function ActionBar({ reloadApplications }: Props) {
  const { i18n } = useTranslation()

  const isMounted = useRef(true)
  useEffect(
    () => () => {
      isMounted.current = false
    },
    []
  )

  const { checkedIds, setCheckedIds, applicationSearchFilters } =
    useContext(ApplicationUIContext)
  const [actionInFlight, setActionInFlight] = useState(false)
  const { setErrorMessage } = useContext(UIContext)
  const clearApplicationList = () => {
    setCheckedIds([])
    reloadApplications()
  }

  const disabled = actionInFlight || checkedIds.length === 0
  const handlePromise = (promise: Promise<void>) => {
    void promise
      .then(() => {
        if (!isMounted.current) return
        clearApplicationList()
      })
      .catch(() => {
        if (!isMounted.current) return
        setErrorMessage({
          type: 'error',
          title: i18n.common.error.unknown,
          resolveLabel: i18n.common.ok
        })
      })
      .finally(() => {
        if (!isMounted.current) return
        setActionInFlight(false)
      })
  }

  const actions: Action[] = [
    {
      id: 'moveToWaitingPlacement',
      label: i18n.applications.actions.moveToWaitingPlacement,
      primary: true,
      enabled: applicationSearchFilters.status === 'SENT',
      disabled,
      onClick: () =>
        handlePromise(
          simpleBatchAction({
            action: 'move-to-waiting-placement',
            body: { applicationIds: checkedIds }
          })
        )
    },
    {
      id: 'returnToSent',
      label: i18n.applications.actions.returnToSent,
      primary: false,
      enabled: applicationSearchFilters.status === 'WAITING_PLACEMENT',
      disabled,
      onClick: () =>
        handlePromise(
          simpleBatchAction({
            action: 'return-to-sent',
            body: { applicationIds: checkedIds }
          })
        )
    },
    {
      id: 'cancelPlacementPlan',
      label: i18n.applications.actions.cancelPlacementPlan,
      primary: false,
      enabled: applicationSearchFilters.status === 'WAITING_DECISION',
      disabled,
      onClick: () =>
        handlePromise(
          simpleBatchAction({
            action: 'cancel-placement-plan',
            body: { applicationIds: checkedIds }
          })
        )
    },
    {
      id: 'sendDecisionsWithoutProposal',
      label: i18n.applications.actions.sendDecisionsWithoutProposal,
      primary: false,
      enabled: applicationSearchFilters.status === 'WAITING_DECISION',
      disabled,
      onClick: () =>
        handlePromise(
          simpleBatchAction({
            action: 'send-decisions-without-proposal',
            body: { applicationIds: checkedIds }
          })
        )
    },
    {
      id: 'sendPlacementProposal',
      label: i18n.applications.actions.sendPlacementProposal,
      primary: true,
      enabled: applicationSearchFilters.status === 'WAITING_DECISION',
      disabled,
      onClick: () =>
        handlePromise(
          simpleBatchAction({
            action: 'send-placement-proposal',
            body: { applicationIds: checkedIds }
          })
        )
    },
    {
      id: 'withdrawPlacementProposal',
      label: i18n.applications.actions.withdrawPlacementProposal,
      primary: false,
      enabled: applicationSearchFilters.status === 'WAITING_UNIT_CONFIRMATION',
      disabled,
      onClick: () =>
        handlePromise(
          simpleBatchAction({
            action: 'withdraw-placement-proposal',
            body: { applicationIds: checkedIds }
          })
        )
    }
  ].filter(({ enabled }) => enabled)

  return actions.length > 0 ? (
    <StickyActionBar align="right">
      {checkedIds.length > 0 ? (
        <CheckedRowsInfo>
          {i18n.applications.actions.checked(checkedIds.length)}
        </CheckedRowsInfo>
      ) : null}
      <FixedSpaceRow>
        {actions.map(({ id, label, disabled, onClick, primary }) => (
          <Button
            key={id}
            onClick={onClick}
            text={label}
            disabled={disabled}
            primary={primary}
            data-qa={`action-bar-${id}`}
          />
        ))}
      </FixedSpaceRow>
    </StickyActionBar>
  ) : null
})
