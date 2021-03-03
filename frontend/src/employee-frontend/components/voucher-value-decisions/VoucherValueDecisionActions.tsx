// SPDX-FileCopyrightText: 2017-2020 City of Espoo
//
// SPDX-License-Identifier: LGPL-2.1-or-later

import React, { useState } from 'react'
import styled from 'styled-components'
import AsyncButton from '@evaka/lib-components/atoms/buttons/AsyncButton'
import { useTranslation } from '../../state/i18n'
import StickyActionBar from '../common/StickyActionBar'
import { sendVoucherValueDecisions } from '../../api/invoicing'
import { VoucherValueDecisionStatus } from '../../types/invoicing'
import { CheckedRowsInfo } from '../../components/common/CheckedRowsInfo'
import colors from '@evaka/lib-components/colors'

const ErrorMessage = styled.div`
  color: ${colors.accents.red};
  margin: 0 20px;
`

type Props = {
  status: VoucherValueDecisionStatus
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
          {i18n.valueDecisions.buttons.checked(checkedIds.length)}
        </CheckedRowsInfo>
      ) : null}
      <AsyncButton
        primary
        text={i18n.valueDecisions.buttons.createDecision(checkedIds.length)}
        disabled={checkedIds.length === 0}
        onClick={() =>
          sendVoucherValueDecisions(checkedIds)
            .then(() => setError(false))
            .catch((e) => {
              setError(true)
              throw e
            })
        }
        onSuccess={() => {
          clearChecked()
          loadDecisions()
        }}
        data-qa="send-decisions"
      />
    </StickyActionBar>
  ) : null
})

export default Actions
