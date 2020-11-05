// SPDX-FileCopyrightText: 2017-2020 City of Espoo
//
// SPDX-License-Identifier: LGPL-2.1-or-later

import React, { useCallback, useContext, useEffect, useState } from 'react'
import { Redirect, useHistory, useParams } from 'react-router-dom'
import styled from 'styled-components'
import { faQuestion } from '~icon-set'
import { Container, ContentArea } from '~components/shared/layout/Container'
import ReturnButton from '~components/shared/atoms/buttons/ReturnButton'
import InfoModal from '~components/common/InfoModal'
import Heading from './Heading'
import ChildSection from './ChildSection'
import Summary from './Summary'
import Actions from './Actions'
import { isFailure, isSuccess, Loading, Result } from '~api'
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
    Loading()
  )
  const [modified, setModified] = useState<boolean>(false)
  const [newDecisionType, setNewDecisionType] = useState<string>('')
  const [confirmingBack, setConfirmingBack] = useState<boolean>(false)

  const loadDecision = () => getFeeDecision(id).then((dec) => setDecision(dec))
  useEffect(() => void loadDecision(), [id])

  useEffect(() => {
    if (isSuccess(decision)) {
      const name = formatTitleName(
        decision.data.headOfFamily.firstName,
        decision.data.headOfFamily.lastName
      )
      decision.data.status === 'DRAFT'
        ? setTitle(`${name} | ${i18n.titles.feeDecisionDraft}`)
        : setTitle(`${name} | ${i18n.titles.feeDecision}`)
      setNewDecisionType(decision.data.decisionType)
    }
  }, [decision])

  const changeDecisionType = (type: string) => {
    if (isSuccess(decision)) {
      setNewDecisionType(type)
      decision.data.decisionType === type
        ? setModified(false)
        : setModified(true)
    }
  }

  const goBack = () => history.goBack()

  const goToDecisions = useCallback(() => goBack(), [history])

  if (isFailure(decision)) {
    return <Redirect to="/finance/fee-decisions" />
  }

  return (
    <>
      <Container
        className="fee-decision-details-page"
        data-qa="fee-decision-details-page"
      >
        <ReturnButton dataQa="navigate-back" />
        {isSuccess(decision) && (
          <ContentArea opaque>
            <Heading
              {...decision.data}
              changeDecisionType={changeDecisionType}
              newDecisionType={newDecisionType}
            />
            {decision.data.parts.map(({ child, placement, placementUnit }) => (
              <ChildSection
                key={child.id}
                child={child}
                placement={placement}
                placementUnit={placementUnit}
              />
            ))}
            <Summary decision={decision.data} />
            <Actions
              decision={decision.data}
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
