// SPDX-FileCopyrightText: 2017-2022 City of Espoo
//
// SPDX-License-Identifier: LGPL-2.1-or-later

import React, { useCallback, useContext, useEffect, useState } from 'react'
import { Navigate, useNavigate } from 'react-router-dom'

import { Loading, Result } from 'lib-common/api'
import {
  FeeDecisionDetailed,
  FeeDecisionType
} from 'lib-common/generated/api-types/invoicing'
import useNonNullableParams from 'lib-common/useNonNullableParams'
import ReturnButton from 'lib-components/atoms/buttons/ReturnButton'
import { Container, ContentArea } from 'lib-components/layout/Container'
import InfoModal from 'lib-components/molecules/modals/InfoModal'
import { faQuestion } from 'lib-icons'

import { getFeeDecision } from '../../api/invoicing'
import { useTranslation } from '../../state/i18n'
import { TitleContext, TitleState } from '../../state/title'

import Actions from './Actions'
import ChildSection from './ChildSection'
import Heading from './Heading'
import Summary from './Summary'

export default React.memo(function FeeDecisionDetailsPage() {
  const navigate = useNavigate()
  const { id } = useNonNullableParams<{ id: string }>()
  const { i18n } = useTranslation()
  const { setTitle, formatTitleName } = useContext<TitleState>(TitleContext)
  const [decision, setDecision] = useState<Result<FeeDecisionDetailed>>(
    Loading.of()
  )
  const [modified, setModified] = useState<boolean>(false)
  const [newDecisionType, setNewDecisionType] =
    useState<FeeDecisionType>('NORMAL')
  const [confirmingBack, setConfirmingBack] = useState<boolean>(false)

  const loadDecision = useCallback(
    () => getFeeDecision(id).then((dec) => setDecision(dec)),
    [id]
  )
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

  const decisionType = decision.map(({ decisionType }) => decisionType)
  const changeDecisionType = useCallback(
    (type: FeeDecisionType) => {
      if (decisionType.isSuccess) {
        setNewDecisionType(type)
        decisionType.value === type ? setModified(false) : setModified(true)
      }
    },
    [decisionType]
  )

  const goBack = useCallback(() => navigate(-1), [navigate])

  if (decision.isFailure) {
    return <Navigate replace to="/finance/fee-decisions" />
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
              goToDecisions={goBack}
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
          type="warning"
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
