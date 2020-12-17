{
  /*
SPDX-FileCopyrightText: 2017-2020 City of Espoo

SPDX-License-Identifier: LGPL-2.1-or-later
*/
}

import React from 'react'
import AsyncButton from '@evaka/lib-components/src/atoms/buttons/AsyncButton'
import StickyActionBar from '~components/common/StickyActionBar'
import { markVoucherValueDecisionSent } from '~api/invoicing'
import { VoucherValueDecisionDetailed } from '~types/invoicing'

type Props = {
  decision: VoucherValueDecisionDetailed
  loadDecision: () => void
}

export default React.memo(function VoucherValueDecisionActionBar({
  decision,
  loadDecision
}: Props) {
  return decision.status === 'WAITING_FOR_MANUAL_SENDING' ? (
    <StickyActionBar align="right">
      <AsyncButton
        primary
        text="Merkitse lähetetyksi"
        onClick={() => markVoucherValueDecisionSent([decision.id])}
        onSuccess={loadDecision}
      />
    </StickyActionBar>
  ) : null
})
