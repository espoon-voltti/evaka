// SPDX-FileCopyrightText: 2017-2026 City of Espoo
//
// SPDX-License-Identifier: LGPL-2.1-or-later

import { FontAwesomeIcon } from '@fortawesome/react-fontawesome'
import React from 'react'
import styled from 'styled-components'

import { fontWeights } from 'lib-components/typography'
import { defaultMargins } from 'lib-components/white-space'
import { faMobileButton, faTabletButton } from 'lib-icons'

import { useTranslation } from '../localization'

export const InstallInstructions = React.memo(function InstallInstructions() {
  const i18n = useTranslation()
  return (
    <Container>
      <Device>
        <FontAwesomeIcon icon={faMobileButton} />
        <FontAwesomeIcon icon={faTabletButton} />
        <span>{i18n.pwa.install.device}</span>
      </Device>
      <Heading>{i18n.pwa.install.stepsHeading}</Heading>
      <Steps>
        {i18n.pwa.install.steps.map((step, index) => (
          <Step key={index}>
            <StepNumber aria-hidden="true">{index + 1}</StepNumber>
            <div>{step}</div>
          </Step>
        ))}
      </Steps>
    </Container>
  )
})

const Container = styled.div`
  display: flex;
  flex-direction: column;
  gap: ${defaultMargins.s};
`

const Device = styled.div`
  display: flex;
  align-items: center;
  gap: ${defaultMargins.xs};
  padding: ${defaultMargins.xs} ${defaultMargins.s};
  background-color: ${(p) => p.theme.colors.grayscale.g4};
  border-radius: 4px;
`

const Heading = styled.span`
  font-weight: ${fontWeights.semibold};
`

const Steps = styled.ol`
  display: flex;
  flex-direction: column;
  gap: ${defaultMargins.s};
  margin: 0;
  padding: 0;
  list-style: none;
`

const Step = styled.li`
  display: flex;
  align-items: flex-start;
  gap: ${defaultMargins.s};
`

const StepNumber = styled.span`
  flex: 0 0 auto;
  width: 24px;
  height: 24px;
  border-radius: 50%;
  display: flex;
  align-items: center;
  justify-content: center;
  background-color: ${(p) => p.theme.colors.main.m2};
  color: ${(p) => p.theme.colors.grayscale.g0};
  font-weight: ${fontWeights.semibold};
`
