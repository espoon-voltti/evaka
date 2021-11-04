// SPDX-FileCopyrightText: 2017-2020 City of Espoo
//
// SPDX-License-Identifier: LGPL-2.1-or-later

import React, { useCallback, useContext, useEffect, useState } from 'react'
import { Redirect, useHistory, useParams } from 'react-router-dom'
import styled from 'styled-components'
import { faQuestion } from 'lib-icons'
import { Container, ContentArea } from 'lib-components/layout/Container'
import ReturnButton from 'lib-components/atoms/buttons/ReturnButton'
import InfoModal from 'lib-components/molecules/modals/InfoModal'
import Heading from './Heading'
import ChildSection from './ChildSection'
import Summary from './Summary'
import Actions from './Actions'
import { Loading, Result } from 'lib-common/api'
import { getFeeDecision } from '../../api/invoicing'
import { useTranslation } from '../../state/i18n'
import { TitleContext, TitleState } from '../../state/title'
import { FeeDecisionDetailed } from '../../types/invoicing'
import colors from 'lib-customizations/common'
import { FeeDecisionType } from 'lib-common/generated/api-types/invoicing'

export const ErrorMessage = styled.div`
  color: ${colors.accents.red};
  margin-right: 20px;
  display: flex;
  align-items: center;
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
  const [newDecisionType, setNewDecisionType] =
    useState<FeeDecisionType>('NORMAL')
  const [confirmingBack, setConfirmingBack] = useState<boolean>(false)

  const loadDecision = () => getFeeDecision(id).then((dec) => setDecision(dec))
  useEffect(() => void loadDecision(), [id]) // eslint-disable-line react-hooks/exhaustive-deps

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
  }, [decision]) // eslint-disable-line react-hooks/exhaustive-deps

  const changeDecisionType = (type: FeeDecisionType) => {
    if (decision.isSuccess) {
      setNewDecisionType(type)
      decision.value.decisionType === type
        ? setModified(false)
        : setModified(true)
    }
  }

  const goBack = () => history.goBack()

  const goToDecisions = useCallback(() => goBack(), [history]) // eslint-disable-line react-hooks/exhaustive-deps

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
            {decision.value.children.map(
              ({
                child,
                placementType,
                placementUnit,
                serviceNeedDescriptionFi
              }) => (
                <ChildSection
                  key={child.id}
                  child={child}
                  placementType={placementType}
                  placementUnit={placementUnit}
                  serviceNeedDescription={serviceNeedDescriptionFi}
                />
              )
            )}
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
          reject={{ action: goBack, label: i18n.feeDecision.modal.cancel }}
          resolve={{
            action: () => {
              setConfirmingBack(false)
            },
            label: i18n.feeDecision.modal.confirm
          }}
        />
      )}
    </>
  )
})
