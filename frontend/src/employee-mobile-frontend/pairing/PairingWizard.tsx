// SPDX-FileCopyrightText: 2017-2022 City of Espoo
//
// SPDX-License-Identifier: LGPL-2.1-or-later

import React, { Fragment, useContext, useEffect, useState } from 'react'
import styled from 'styled-components'

import { Loading, Result, wrapResult } from 'lib-common/api'
import { Pairing } from 'lib-common/generated/api-types/pairing'
import { IconOnlyButton } from 'lib-components/atoms/buttons/IconOnlyButton'
import InputField from 'lib-components/atoms/form/InputField'
import { fontWeights, P } from 'lib-components/typography'
import colors from 'lib-customizations/common'
import { faArrowRight } from 'lib-icons'

import EvakaLogo from '../assets/EvakaLogo.svg'
import { authMobile } from '../auth/api'
import { UserContext } from '../auth/state'
import { useTranslation } from '../common/i18n'
import {
  getPairingStatus,
  postPairingChallenge
} from '../generated/api-clients/pairing'

import { FullHeightContainer, WideLinkButton } from './components'

const CenteredColumn = styled.div`
  display: flex;
  align-items: center;
  flex-direction: column;
`

const Flex = styled.div`
  display: flex;
  align-items: center;
`

const Img = styled.img`
  max-width: 50%;
`

const PhaseTitle = styled.h1`
  font-family: Montserrat, sans-serif;
  font-style: normal;
  font-weight: ${fontWeights.semibold};
  font-size: 20px;
  line-height: 30px;
  text-align: center;
  color: ${colors.main.m1};
`

export const ResponseKey = styled.div`
  font-family: Montserrat, sans-serif;
  font-style: normal;
  font-weight: ${fontWeights.semibold};
  font-size: 30px;
  line-height: 30px;
  text-align: center;
  letter-spacing: 0.08em;
  color: ${colors.grayscale.g70};
`

const Bottom = styled.div``

const getPairingStatusResult = wrapResult(getPairingStatus)
const postPairingChallengeResult = wrapResult(postPairingChallenge)

export default React.memo(function ParingWizard() {
  const { i18n } = useTranslation()
  const { refreshAuthStatus } = useContext(UserContext)

  const [phase, setPhase] = useState<1 | 2 | 3>(1)
  const [challengeKey, setChallengeKey] = useState<string>('')
  const [pairingResponse, setPairingResponse] = useState<Result<Pairing>>(
    Loading.of()
  )

  useEffect(() => {
    const polling = setInterval(() => {
      if (pairingResponse.isSuccess) {
        void getPairingStatusResult({
          id: pairingResponse.value.id
        }).then((status) => {
          if (status.isSuccess) {
            if (status.value.status === 'READY') {
              if (pairingResponse.value.responseKey) {
                void authMobile(
                  pairingResponse.value.id,
                  pairingResponse.value.challengeKey,
                  pairingResponse.value.responseKey
                )
                  .then(refreshAuthStatus)
                  .then(() => setPhase(3))
              }

              clearInterval(polling)
            }
          }
        })
      }
    }, 1000)
    return () => clearInterval(polling)
  }, [pairingResponse]) // eslint-disable-line react-hooks/exhaustive-deps

  async function sendRequest() {
    await postPairingChallengeResult({ body: { challengeKey } }).then(
      setPairingResponse
    )
    setPhase(2)
  }

  return (
    <Fragment>
      <FullHeightContainer>
        {phase === 1 && (
          <Fragment>
            <CenteredColumn>
              <Img src={EvakaLogo} />
              <PhaseTitle data-qa="mobile-pairing-wizard-title-1">
                {i18n.mobile.wizard.title1}
              </PhaseTitle>
              <section>
                <P centered>{i18n.mobile.wizard.text1}</P>
              </section>
              <Flex>
                <InputField
                  value={challengeKey}
                  onChange={(v) => setChallengeKey(v.toLowerCase())}
                  placeholder={i18n.common.code}
                  data-qa="challenge-key-input"
                />
                <IconOnlyButton
                  icon={faArrowRight}
                  onClick={sendRequest}
                  data-qa="submit-challenge-key-btn"
                  aria-label={i18n.common.confirm}
                />
              </Flex>
            </CenteredColumn>
          </Fragment>
        )}

        {phase === 2 && pairingResponse.isSuccess ? (
          <Fragment>
            <CenteredColumn>
              <Img src={EvakaLogo} />
              <PhaseTitle>{i18n.mobile.wizard.title2}</PhaseTitle>
              <section>
                <P centered>{i18n.mobile.wizard.text2}</P>
              </section>
              <ResponseKey data-qa="response-key">
                {pairingResponse.value.responseKey}
              </ResponseKey>
            </CenteredColumn>
          </Fragment>
        ) : (
          phase === 2 &&
          pairingResponse.isFailure && <div>{i18n.common.loadingFailed}</div>
        )}

        {phase === 3 && pairingResponse.isSuccess && (
          <FullHeightContainer spaced>
            <CenteredColumn>
              <Img src={EvakaLogo} />
              <PhaseTitle data-qa="mobile-pairing-wizard-title-3">
                {i18n.mobile.wizard.title3}
              </PhaseTitle>
              <section>
                <P centered>{i18n.mobile.wizard.text3}</P>
                <P centered>{i18n.mobile.wizard.text4}</P>
              </section>
            </CenteredColumn>
            <Bottom>
              <WideLinkButton to="/" data-qa="start-cta-link">
                {i18n.mobile.actions.START.toUpperCase()}
              </WideLinkButton>
            </Bottom>
          </FullHeightContainer>
        )}
      </FullHeightContainer>
    </Fragment>
  )
})
