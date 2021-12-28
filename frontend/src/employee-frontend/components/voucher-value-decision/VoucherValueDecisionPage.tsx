// SPDX-FileCopyrightText: 2017-2020 City of Espoo
//
// SPDX-License-Identifier: LGPL-2.1-or-later

import React, { useCallback, useContext, useEffect, useState } from 'react'
import { Redirect, useParams } from 'react-router-dom'
import styled from 'styled-components'
import ReturnButton from 'lib-components/atoms/buttons/ReturnButton'
import { Container, ContentArea } from 'lib-components/layout/Container'
import VoucherValueDecisionHeading from './VoucherValueDecisionHeading'
import VoucherValueDecisionChildSection from './VoucherValueDecisionChildSection'
import VoucherValueDecisionSummary from './VoucherValueDecisionSummary'
import VoucherValueDecisionActionBar from './VoucherValueDecisionActionBar'
import { Loading, Result } from 'lib-common/api'
import { getVoucherValueDecision } from '../../api/invoicing'
import { useTranslation } from '../../state/i18n'
import { TitleContext, TitleState } from '../../state/title'
import { VoucherValueDecisionDetailed } from '../../types/invoicing'
import colors from 'lib-customizations/common'
import { VoucherValueDecisionType } from 'lib-common/generated/api-types/invoicing'

export const ErrorMessage = styled.div`
  color: ${colors.accents.dangerRed};
  margin-right: 20px;
`

export default React.memo(function VoucherValueDecisionPage() {
  const { id } = useParams<{ id: string }>()
  const { i18n } = useTranslation()
  const { setTitle, formatTitleName } = useContext<TitleState>(TitleContext)
  const [decision, setDecision] = useState<
    Result<VoucherValueDecisionDetailed>
  >(Loading.of())
  const [modified, setModified] = useState<boolean>(false)
  const [newDecisionType, setNewDecisionType] =
    useState<VoucherValueDecisionType>('NORMAL')

  const loadDecision = useCallback(
    () => getVoucherValueDecision(id).then(setDecision),
    [id]
  )
  useEffect(() => void loadDecision(), [loadDecision])

  useEffect(() => {
    if (decision.isSuccess) {
      const name = formatTitleName(
        decision.value.headOfFamily.firstName,
        decision.value.headOfFamily.lastName
      )
      decision.value.status === 'DRAFT'
        ? setTitle(`${name} | ${i18n.titles.valueDecisionDraft}`)
        : setTitle(`${name} | ${i18n.titles.valueDecision}`)
      setNewDecisionType(decision.value.decisionType)
    }
  }, [decision, formatTitleName, setTitle, i18n])

  const changeDecisionType = (type: VoucherValueDecisionType) => {
    if (decision.isSuccess) {
      setNewDecisionType(type)
      decision.value.decisionType === type
        ? setModified(false)
        : setModified(true)
    }
  }

  if (decision.isFailure) {
    return <Redirect to="/finance/value-decisions" />
  }

  return (
    <Container data-qa="voucher-value-decision-page">
      <ReturnButton label={i18n.common.goBack} data-qa="navigate-back" />
      {decision.isSuccess && (
        <>
          <ContentArea opaque>
            <VoucherValueDecisionHeading
              {...decision.value}
              changeDecisionType={changeDecisionType}
              newDecisionType={newDecisionType}
            />
            <VoucherValueDecisionChildSection
              key={decision.value.child.id}
              child={decision.value.child}
              placement={decision.value.placement}
              serviceNeed={decision.value.serviceNeed}
            />
            <VoucherValueDecisionSummary decision={decision.value} />
            <VoucherValueDecisionActionBar
              decision={decision.value}
              loadDecision={loadDecision}
              modified={modified}
              setModified={setModified}
              newDecisionType={newDecisionType}
            />
          </ContentArea>
        </>
      )}
    </Container>
  )
})
