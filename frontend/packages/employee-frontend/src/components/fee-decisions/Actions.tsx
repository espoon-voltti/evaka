// SPDX-FileCopyrightText: 2017-2020 City of Espoo
//
// SPDX-License-Identifier: LGPL-2.1-or-later

import React, { useState } from 'react'
import styled from 'styled-components'
import AsyncButton from '~components/shared/atoms/buttons/AsyncButton'
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
  const [error, setError] = useState(false)

  return status === 'DRAFT' ? (
    <StickyActionBar align={'right'}>
      {error ? <ErrorMessage>{i18n.common.error.unknown}</ErrorMessage> : null}
      {checkedIds.length > 0 ? (
        <CheckedRowsInfo>
          {i18n.feeDecisions.buttons.checked(checkedIds.length)}
        </CheckedRowsInfo>
      ) : null}
      <AsyncButton
        text={i18n.feeDecisions.buttons.createDecision(checkedIds.length)}
        disabled={checkedIds.length === 0}
        onClick={() =>
          confirmDecisions(checkedIds)
            .then(() => void setError(false))
            .catch(() => void setError(true))
        }
        onSuccess={() => {
          clearChecked()
          loadDecisions()
        }}
        data-qa="confirm-decisions"
      />
    </StickyActionBar>
  ) : null
})

export default Actions
