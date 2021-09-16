import {useTranslation} from "../../state/i18n";

{
  /*
SPDX-FileCopyrightText: 2017-2020 City of Espoo

SPDX-License-Identifier: LGPL-2.1-or-later
*/
}

import React, {useState} from 'react'
import AsyncButton from 'lib-components/atoms/buttons/AsyncButton'
import StickyActionBar from '../../components/common/StickyActionBar'
import {markVoucherValueDecisionSent, sendVoucherValueDecisions} from '../../api/invoicing'
import { VoucherValueDecisionDetailed } from '../../types/invoicing'
import styled from "styled-components";

const ErrorMessage = styled.div`
  color: ${(p) => p.theme.colors.accents.orangeDark};
  margin: 0 20px;
`

type Props = {
  decision: VoucherValueDecisionDetailed
  loadDecision: () => void
}

export default React.memo(function VoucherValueDecisionActionBar({
  decision,
  loadDecision
}: Props) {
  const { i18n } = useTranslation()
  const [error, setError] = useState<string>()

  return decision.status === 'WAITING_FOR_MANUAL_SENDING' ? (
    <StickyActionBar align="right">
      {error ? <ErrorMessage>{error}</ErrorMessage> : null}
      <AsyncButton
        primary
        text="Merkitse lähetetyksi"
        onClick={() => markVoucherValueDecisionSent([decision.id])}
        onSuccess={loadDecision}
      />
    </StickyActionBar>
  ) : decision.status === 'DRAFT' ? (
    <StickyActionBar align="right">
      <AsyncButton
        primary
        text="Lähetä"
        onClick={() =>
          sendVoucherValueDecisions([decision.id]).then((result) => {
            if (result.isSuccess) {
              setError(undefined)
            }

            if (result.isFailure) {
              setError(
                i18n.valueDecisions.buttons.errors[result.errorCode ?? ''] ??
                i18n.common.error.unknown
              )
            }

            return result
          })
        }
        onSuccess={loadDecision}
      />
    </StickyActionBar>
  ) : null
})
