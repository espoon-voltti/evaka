// SPDX-FileCopyrightText: 2017-2020 City of Espoo
//
// SPDX-License-Identifier: LGPL-2.1-or-later

import React, { useCallback, useContext, useEffect, useState } from 'react'
import { Link, Redirect, RouteComponentProps } from 'react-router-dom'
import { FontAwesomeIcon } from '@fortawesome/react-fontawesome'
import { faChevronLeft, faQuestion } from '@evaka/icons'
import { Container, ContentArea } from '~components/shared/alpha'
import InfoModal from '~components/common/InfoModal'
import { useTranslation } from '../../state/i18n'
import { TitleContext, TitleState } from '../../state/title'
import Heading from './Heading'
import ChildSection from './ChildSection'
import Summary from './Summary'
import Actions from './Actions'
import { isFailure, isSuccess, Loading, Result } from '../../api'
import { getDecision } from '../../api/invoicing'
import { FeeDecisionDetailed } from '../../types/invoicing'
import './FeeDecisionDetailsPage.scss'
import styled from 'styled-components'
import { EspooColours } from '~utils/colours'

export const ErrorMessage = styled.div`
  color: ${EspooColours.red};
  margin-right: 20px;
`

const FeeDecisionDetailsPage = React.memo(function FeeDecisionDetailsPage({
  match,
  history
}: RouteComponentProps<{ id?: string }>) {
  const { i18n } = useTranslation()
  const { id } = match.params
  const { setTitle, formatTitleName } = useContext<TitleState>(TitleContext)
  const [decision, setDecision] = useState<Result<FeeDecisionDetailed>>(
    Loading()
  )
  const [modified, setModified] = useState<boolean>(false)
  const [newDecisionType, setNewDecisionType] = useState<string>('')
  const [confirmingBack, setConfirmingBack] = useState<boolean>(false)

  useEffect(() => {
    // id should always be present as otherwise this page should not be rendered
    // eslint-disable-next-line @typescript-eslint/no-non-null-assertion
    void getDecision(id!).then((dec) => setDecision(dec))
  }, [id])

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

  const goBack = () => history.push('/fee-decisions')

  const confirmBack = (event: React.MouseEvent<HTMLAnchorElement>) => {
    event.preventDefault()
    modified ? setConfirmingBack(true) : goBack()
  }

  const goToDecisions = useCallback(() => goBack(), [history])

  if (isFailure(decision)) {
    return <Redirect to="/decisions" />
  }

  return (
    <>
      <div
        className="fee-decision-details-page"
        data-qa="fee-decision-details-page"
      >
        <Container>
          <div className="close-container">
            <Link
              to={`/`}
              data-qa="navigate-back"
              onClick={(event) => confirmBack(event)}
            >
              <FontAwesomeIcon icon={faChevronLeft} />{' '}
              {i18n.feeDecision.form.nav.return}
            </Link>
          </div>
          {isSuccess(decision) && (
            <ContentArea opaque>
              {decision && (
                <Heading
                  {...decision.data}
                  changeDecisionType={changeDecisionType}
                  newDecisionType={newDecisionType}
                />
              )}
              {decision.data.parts.map(
                ({ child, placement, placementUnit }) => (
                  <ChildSection
                    key={child.id}
                    child={child}
                    placement={placement}
                    placementUnit={placementUnit}
                  />
                )
              )}
              <Summary decision={decision.data} />
              <Actions
                decision={decision.data}
                goToDecisions={goToDecisions}
                modified={modified}
                setModified={setModified}
                setDecision={setDecision}
                newDecisionType={newDecisionType}
              />
            </ContentArea>
          )}
        </Container>
      </div>
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

export default FeeDecisionDetailsPage
