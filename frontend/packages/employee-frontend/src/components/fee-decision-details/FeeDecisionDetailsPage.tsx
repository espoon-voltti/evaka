// SPDX-FileCopyrightText: 2017-2020 City of Espoo
//
// SPDX-License-Identifier: LGPL-2.1-or-later

import React, { useCallback, useContext, useEffect, useState } from 'react'
import { Redirect, useHistory, useParams } from 'react-router-dom'
import styled from 'styled-components'
import { faQuestion } from '@evaka/lib-icons'
import {
  Container,
  ContentArea
} from '@evaka/lib-components/src/layout/Container'
import ReturnButton from '@evaka/lib-components/src/atoms/buttons/ReturnButton'
import InfoModal from '~components/common/InfoModal'
import Heading from './Heading'
import ChildSection from './ChildSection'
import Summary from './Summary'
import Actions from './Actions'
import { Loading, Result } from '~api'
import { getFeeDecision } from '~api/invoicing'
import { useTranslation } from '~state/i18n'
import { TitleContext, TitleState } from '~state/title'
import { FeeDecisionDetailed } from '~types/invoicing'
import { EspooColours } from '~utils/colours'

export const ErrorMessage = styled.div`
  color: ${EspooColours.red};
  margin-right: 20px;
`

export default React.memo(function FeeDecisionDetailsPage() {
  const history = useHistory()
  const { id } = useParams<{ id: string }>()
  const { i18n } = useTranslation()
  const { setTitle, formatTitleName } = useContext<TitleState>(TitleContext)
  const [decision, setDecision] = useState<Result<FeeDecisionDetailed>>(
    Loading.of()
  )
  const [modified, setModified] = useState<boolean>(false)
  const [newDecisionType, setNewDecisionType] = useState<string>('')
  const [confirmingBack, setConfirmingBack] = useState<boolean>(false)

  const loadDecision = () => getFeeDecision(id).then((dec) => setDecision(dec))
  useEffect(() => void loadDecision(), [id])

  useEffect(() => {
    if (decision.isSuccess) {
      const name = formatTitleName(
        decision.value.headOfFamily.firstName,
        decision.value.headOfFamily.lastName
      )
      decision.value.status === 'DRAFT'
        ? setTitle(`${name} | ${i18n.titles.feeDecisionDraft}`)
        : setTitle(`${name} | ${i18n.titles.feeDecision}`)
      setNewDecisionType(decision.value.decisionType)
    }
  }, [decision])

  const changeDecisionType = (type: string) => {
    if (decision.isSuccess) {
      setNewDecisionType(type)
      decision.value.decisionType === type
        ? setModified(false)
        : setModified(true)
    }
  }

  const goBack = () => history.goBack()

  const goToDecisions = useCallback(() => goBack(), [history])

  if (decision.isFailure) {
    return <Redirect to="/finance/fee-decisions" />
  }

  return (
    <>
      <Container
        className="fee-decision-details-page"
        data-qa="fee-decision-details-page"
      >
        <ReturnButton label={i18n.common.goBack} data-qa="navigate-back" />
        {decision.isSuccess && (
          <ContentArea opaque>
            <Heading
              {...decision.value}
              changeDecisionType={changeDecisionType}
              newDecisionType={newDecisionType}
            />
            {decision.value.parts.map(({ child, placement, placementUnit }) => (
              <ChildSection
                key={child.id}
                child={child}
                placement={placement}
                placementUnit={placementUnit}
              />
            ))}
            <Summary decision={decision.value} />
            <Actions
              decision={decision.value}
              goToDecisions={goToDecisions}
              loadDecision={loadDecision}
              modified={modified}
              setModified={setModified}
              newDecisionType={newDecisionType}
            />
          </ContentArea>
        )}
      </Container>
      {confirmingBack && (
        <InfoModal
          title={i18n.feeDecision.modal.title}
          iconColour={'orange'}
          icon={faQuestion}
          resolveLabel={i18n.feeDecision.modal.confirm}
          reject={goBack}
          rejectLabel={i18n.feeDecision.modal.cancel}
          resolve={() => {
            setConfirmingBack(false)
          }}
        />
      )}
    </>
  )
})
