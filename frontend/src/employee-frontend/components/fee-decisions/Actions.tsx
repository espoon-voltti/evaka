// SPDX-FileCopyrightText: 2017-2022 City of Espoo
//
// SPDX-License-Identifier: LGPL-2.1-or-later

import React, { useState } from 'react'
import styled from 'styled-components'

import { FeeDecisionStatus } from 'lib-common/generated/api-types/invoicing'
import AsyncButton from 'lib-components/atoms/buttons/AsyncButton'

import { confirmFeeDecisions } from '../../api/invoicing'
import { useTranslation } from '../../state/i18n'
import { CheckedRowsInfo } from '../common/CheckedRowsInfo'
import StickyActionBar from '../common/StickyActionBar'

const ErrorMessage = styled.div`
  color: ${(p) => p.theme.colors.accents.a2orangeDark};
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
  const [error, setError] = useState<string>()

  return status === 'DRAFT' ? (
    <StickyActionBar align="right">
      {error ? <ErrorMessage>{error}</ErrorMessage> : null}
      {checkedIds.length > 0 ? (
        <CheckedRowsInfo>
          {i18n.feeDecisions.buttons.checked(checkedIds.length)}
        </CheckedRowsInfo>
      ) : null}
      <AsyncButton
        primary
        text={i18n.feeDecisions.buttons.createDecision(checkedIds.length)}
        disabled={checkedIds.length === 0}
        onClick={() =>
          confirmFeeDecisions(checkedIds).then((result) => {
            if (result.isSuccess) {
              setError(undefined)
            }

            if (result.isFailure) {
              setError(
                // eslint-disable-next-line @typescript-eslint/no-unsafe-argument
                i18n.feeDecisions.buttons.errors[result.errorCode ?? ''] ??
                  i18n.common.error.unknown
              )
            }

            return result
          })
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
