// SPDX-FileCopyrightText: 2017-2023 City of Espoo
//
// SPDX-License-Identifier: LGPL-2.1-or-later

import React, { useContext, useState } from 'react'

import { Action } from 'lib-common/generated/action'
import { useQueryResult } from 'lib-common/query'
import { UUID } from 'lib-common/types'
import HorizontalLine from 'lib-components/atoms/HorizontalLine'
import { CollapsibleContentArea } from 'lib-components/layout/Container'
import { H2 } from 'lib-components/typography'

import { ChildContext } from '../../state'
import { useTranslation } from '../../state/i18n'

import AssistanceNeedDecisionSection from './AssistanceNeedDecisionSection'
import AssistanceNeedPreschoolDecisionSection from './AssistanceNeedPreschoolDecisionSection'
import AssistanceNeedVoucherCoefficientSection from './AssistanceNeedVoucherCoefficientSection'
import AssistanceAction from './assistance/AssistanceActionSection'
import { AssistanceFactorSection } from './assistance/AssistanceFactorSection'
import { DaycareAssistanceSection } from './assistance/DaycareAssistanceSection'
import { OtherAssistanceMeasureSection } from './assistance/OtherAssistanceMeasureSection'
import { PreschoolAssistanceSection } from './assistance/PreschoolAssistanceSection'
import { assistanceQuery } from './queries'

export interface Props {
  childId: UUID
  startOpen: boolean
}

export default React.memo(function Assistance({ childId, startOpen }: Props) {
  const { i18n } = useTranslation()

  const { permittedActions, assistanceNeedVoucherCoefficientsEnabled } =
    useContext(ChildContext)

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
        {permittedActions.has('READ_ASSISTANCE') && (
          <AssistanceContent id={childId} permittedActions={permittedActions} />
        )}
        {permittedActions.has('READ_ASSISTANCE_NEED_DECISIONS') && (
          <>
            <HorizontalLine dashed slim />
            <AssistanceNeedDecisionSection id={childId} />
          </>
        )}
        {permittedActions.has('READ_ASSISTANCE_NEED_PRESCHOOL_DECISIONS') && (
          <>
            <HorizontalLine dashed slim />
            <AssistanceNeedPreschoolDecisionSection childId={childId} />
          </>
        )}
        {assistanceNeedVoucherCoefficientsEnabled.getOrElse(false) &&
          permittedActions.has('READ_ASSISTANCE_NEED_VOUCHER_COEFFICIENTS') && (
            <>
              <HorizontalLine dashed slim />
              <AssistanceNeedVoucherCoefficientSection childId={childId} />
            </>
          )}
      </CollapsibleContentArea>
    </div>
  )
})

const AssistanceContent = ({
  id,
  permittedActions
}: {
  id: UUID
  permittedActions: Set<Action.Child | Action.Person>
}) => {
  const assistanceResult = useQueryResult(assistanceQuery({ child: id }))
  return (
    <>
      {permittedActions.has('READ_ASSISTANCE_FACTORS') && (
        <AssistanceFactorSection
          childId={id}
          rows={assistanceResult.map(
            ({ assistanceFactors }) => assistanceFactors
          )}
        />
      )}
      {permittedActions.has('READ_DAYCARE_ASSISTANCES') && (
        <>
          <HorizontalLine dashed slim />
          <DaycareAssistanceSection
            childId={id}
            rows={assistanceResult.map(
              ({ daycareAssistances }) => daycareAssistances
            )}
          />
        </>
      )}
      {permittedActions.has('READ_PRESCHOOL_ASSISTANCES') && (
        <>
          <HorizontalLine dashed slim />
          <PreschoolAssistanceSection
            childId={id}
            rows={assistanceResult.map(
              ({ preschoolAssistances }) => preschoolAssistances
            )}
          />
        </>
      )}
      {permittedActions.has('READ_ASSISTANCE_ACTION') && (
        <>
          <HorizontalLine dashed slim />
          <AssistanceAction
            id={id}
            assistanceActions={assistanceResult.map(
              ({ assistanceActions }) => assistanceActions
            )}
          />
        </>
      )}
      {permittedActions.has('READ_OTHER_ASSISTANCE_MEASURES') && (
        <>
          <HorizontalLine dashed slim />
          <OtherAssistanceMeasureSection
            childId={id}
            rows={assistanceResult.map(
              ({ otherAssistanceMeasures }) => otherAssistanceMeasures
            )}
          />
        </>
      )}
    </>
  )
}
