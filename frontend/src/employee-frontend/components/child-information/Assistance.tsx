// SPDX-FileCopyrightText: 2017-2022 City of Espoo
//
// SPDX-License-Identifier: LGPL-2.1-or-later

import React, { useContext, useState } from 'react'

import type { UUID } from 'lib-common/types'
import HorizontalLine from 'lib-components/atoms/HorizontalLine'
import { CollapsibleContentArea } from 'lib-components/layout/Container'
import { H2 } from 'lib-components/typography'
import { featureFlags } from 'lib-customizations/employee'

import AssistanceAction from '../../components/child-information/AssistanceAction'
import AssistanceNeed from '../../components/child-information/AssistanceNeed'
import type { ChildState } from '../../state/child'
import { ChildContext } from '../../state/child'
import { useTranslation } from '../../state/i18n'

import AssistanceNeedDecisionSection from './AssistanceNeedDecisionSection'
import AssistanceNeedVoucherCoefficientSection from './AssistanceNeedVoucherCoefficientSection'

export interface Props {
  id: UUID
  startOpen: boolean
}

export default React.memo(function Assistance({ id, startOpen }: Props) {
  const { i18n } = useTranslation()
  const { permittedActions, assistanceNeedVoucherCoefficientsEnabled } =
    useContext<ChildState>(ChildContext)

  const [open, setOpen] = useState(startOpen)

  return (
    <div>
      <CollapsibleContentArea
        title={<H2 noMargin>{i18n.childInformation.assistance.title}</H2>}
        open={open}
        toggleOpen={() => setOpen(!open)}
        opaque
        paddingVertical="L"
        data-qa="assistance-collapsible"
      >
        {permittedActions.has('READ_ASSISTANCE_NEED') && (
          <AssistanceNeed id={id} />
        )}
        {permittedActions.has('READ_ASSISTANCE_ACTION') && (
          <>
            <HorizontalLine dashed slim />
            <AssistanceAction id={id} />
          </>
        )}
        {featureFlags.experimental?.assistanceNeedDecisions &&
          permittedActions.has('READ_ASSISTANCE_NEED_DECISIONS') && (
            <>
              <HorizontalLine dashed slim />
              <AssistanceNeedDecisionSection id={id} />
            </>
          )}
        {assistanceNeedVoucherCoefficientsEnabled.getOrElse(false) &&
          permittedActions.has('READ_ASSISTANCE_NEED_VOUCHER_COEFFICIENTS') && (
            <>
              <HorizontalLine dashed slim />
              <AssistanceNeedVoucherCoefficientSection id={id} />
            </>
          )}
      </CollapsibleContentArea>
    </div>
  )
})
