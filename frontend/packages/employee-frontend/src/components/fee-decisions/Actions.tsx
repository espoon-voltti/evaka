// SPDX-FileCopyrightText: 2017-2020 City of Espoo
//
// SPDX-License-Identifier: LGPL-2.1-or-later

import React, { useState } from 'react'
import styled from 'styled-components'
import Button from '~components/shared/atoms/buttons/Button'
import { useTranslation } from '../../state/i18n'
import StickyActionBar from '../common/StickyActionBar'
import { confirmDecisions } from '../../api/invoicing'
import { FeeDecisionStatus } from '../../types/invoicing'
import { EspooColours } from '../../utils/colours'
import { CheckedRowsInfo } from '~components/common/CheckedRowsInfo'

const ErrorMessage = styled.div`
  color: ${EspooColours.red};
  margin: 0 20px;
`

type Action = {
  id: string
  label: string
  primary: boolean
  enabled: boolean
  disabled: boolean
  onClick: () => void
}

type Props = {
  status: FeeDecisionStatus
  checkedIds: string[]
  clearChecked: () => void
  loadDecisions: () => void
}

const Actions = React.memo(function Actions({
  status,
  checkedIds,
  clearChecked,
  loadDecisions
}: Props) {
  const { i18n } = useTranslation()
  const [actionInFlight, setActionInFlight] = useState(false)
  const [error, setError] = useState(false)

  const actions: Action[] = [
    {
      id: 'confirm-decisions',
      label: i18n.feeDecisions.buttons.createDecision(checkedIds.length),
      primary: true,
      enabled: status === 'DRAFT',
      disabled: actionInFlight || checkedIds.length === 0,
      onClick: () => {
        setActionInFlight(true)
        confirmDecisions(checkedIds)
          .then(() => void setError(false))
          .then(() => void clearChecked())
          .then(() => void loadDecisions())
          .catch(() => void setError(true))
          .finally(() => void setActionInFlight(false))
      }
    }
  ].filter(({ enabled }) => enabled)

  return actions.length > 0 ? (
    <StickyActionBar align={'right'}>
      {error ? <ErrorMessage>{i18n.common.error.unknown}</ErrorMessage> : null}
      {checkedIds.length > 0 ? (
        <CheckedRowsInfo>
          {i18n.feeDecisions.buttons.checked(checkedIds.length)}
        </CheckedRowsInfo>
      ) : null}
      {actions.map(({ id, label, primary, disabled, onClick }) => (
        <Button
          key={id}
          primary={primary}
          disabled={disabled}
          onClick={onClick}
          text={label}
          dataQa={id}
        />
      ))}
    </StickyActionBar>
  ) : null
})

export default Actions
