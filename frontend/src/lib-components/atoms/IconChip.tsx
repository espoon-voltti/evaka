// SPDX-FileCopyrightText: 2017-2026 City of Espoo
//
// SPDX-License-Identifier: LGPL-2.1-or-later

import { FontAwesomeIcon } from '@fortawesome/react-fontawesome'
import React from 'react'
import styled from 'styled-components'

import { defaultMargins } from '../white-space'

export interface IconChipVisualProps {
  icon: IconDefinition
  textColor: string
  backgroundColor: string
  iconColor: string
  iconBackgroundColor: string
}
export interface IconChipProps extends IconChipVisualProps {
  label: string
  'data-qa'?: string
}

const IconChip = React.memo(function IconChip({
  label,
  icon,
  backgroundColor,
  textColor,
  iconColor,
  iconBackgroundColor,
  'data-qa': dataQa
}: IconChipProps) {
  return (
    <Container
      backgroundColor={backgroundColor}
      textColor={textColor}
      data-qa={dataQa}
    >
      <IconContainer backgroundColor={iconBackgroundColor}>
        <Icon icon={icon} color={iconColor} />
      </IconContainer>
      {label}
    </Container>
  )
})

const Container = styled.div<{ backgroundColor: string; textColor: string }>`
  display: inline-flex;
  flex-wrap: nowrap;
  gap: ${defaultMargins.xxs};
  align-items: center;
  padding: ${defaultMargins.xxs} ${defaultMargins.xs} ${defaultMargins.xxs}
    ${defaultMargins.xxs};
  border-radius: ${defaultMargins.s};
  background-color: ${(props) => props.backgroundColor};
  color: ${(props) => props.textColor};
  font-size: 16px;
  line-height: 22px;
  font-weight: 600;
  white-space: nowrap;
  width: fit-content;
`

const Icon = styled(FontAwesomeIcon)`
  width: ${defaultMargins.s};
  height: ${defaultMargins.s};
`

const IconContainer = styled.div<{ backgroundColor: string }>`
  display: flex;
  align-items: center;
  justify-content: center;
  width: ${defaultMargins.m};
  min-width: ${defaultMargins.m};
  height: ${defaultMargins.m};
  background: ${(props) => props.backgroundColor};
  border-radius: 100%;
`

export default IconChip
