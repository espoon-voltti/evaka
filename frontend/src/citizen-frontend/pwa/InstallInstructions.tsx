// SPDX-FileCopyrightText: 2017-2026 City of Espoo
//
// SPDX-License-Identifier: LGPL-2.1-or-later

import type { IconDefinition } from '@fortawesome/fontawesome-svg-core'
import { FontAwesomeIcon } from '@fortawesome/react-fontawesome'
import React from 'react'
import styled from 'styled-components'

import { fontWeights, H4 } from 'lib-components/typography'
import { defaultMargins } from 'lib-components/white-space'
import { faMobileButton, faTabletButton } from 'lib-icons'

import { useTranslation } from '../localization'

export interface InstructionsProps {
  deviceIcons: IconDefinition[]
  device: string
  steps: React.ReactNode[]
}

/** The "Toimi näin" layout: the device the steps apply to, then numbered steps */
export const Instructions = React.memo(function Instructions({
  deviceIcons,
  device,
  steps
}: InstructionsProps) {
  const i18n = useTranslation()
  return (
    <Container>
      <Device>
        <DeviceIcons>
          {deviceIcons.map((icon, index) => (
            <DeviceIcon key={index}>
              <FontAwesomeIcon icon={icon} />
            </DeviceIcon>
          ))}
        </DeviceIcons>
        <DeviceName>{device}</DeviceName>
      </Device>
      <H4 $smaller $noMargin>
        {i18n.pwa.stepsHeading}
      </H4>
      <Steps>
        {steps.map((step, index) => (
          <Step key={index}>
            <StepNumber aria-hidden="true">{index + 1}</StepNumber>
            <div>{step}</div>
          </Step>
        ))}
      </Steps>
    </Container>
  )
})

export const InstallInstructions = React.memo(function InstallInstructions() {
  const i18n = useTranslation()
  return (
    <Instructions
      deviceIcons={[faMobileButton, faTabletButton]}
      device={i18n.pwa.install.device}
      steps={i18n.pwa.install.steps}
    />
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
  padding: ${defaultMargins.xs};
  background-color: ${(p) => p.theme.colors.grayscale.g4};
  border-radius: 4px;
`

const DeviceIcons = styled.div`
  display: flex;
  gap: ${defaultMargins.xxs};
`

const DeviceIcon = styled.div`
  display: flex;
  align-items: center;
  justify-content: center;
  width: 24px;
  height: 24px;
  font-size: 20px;
`

const DeviceName = styled.span`
  font-size: 14px;
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
  background-color: ${(p) => p.theme.colors.main.m3};
  color: ${(p) => p.theme.colors.grayscale.g0};
  font-size: 14px;
  font-weight: ${fontWeights.semibold};
`
