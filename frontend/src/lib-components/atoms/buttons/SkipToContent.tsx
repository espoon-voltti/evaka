// SPDX-FileCopyrightText: 2017-2022 City of Espoo
//
// SPDX-License-Identifier: LGPL-2.1-or-later

import React from 'react'
import ReactDOM from 'react-dom'
import styled from 'styled-components'

import { buttonBorderRadius, defaultButtonTextStyle } from './button-commons'

const SkipToContentLink = styled.a`
  position: absolute;
  z-index: 10000;
  left: -10000px;

  padding: 12px 24px;
  margin-top: 12px;
  margin-left: 12px;

  display: block;
  text-align: center;

  border: 1px solid ${(p) => p.theme.colors.main.m2};
  border-radius: ${buttonBorderRadius};
  background: ${(p) => p.theme.colors.grayscale.g0};

  cursor: pointer;

  text-decoration: none;
  ${defaultButtonTextStyle};
  letter-spacing: 0.2px;

  &:focus {
    left: auto;
  }
`

const container = document.createElement('div')
document.body.prepend(container)

export default React.memo(function SkipToContent({
  target,
  children
}: {
  target: string
  children: React.ReactNode
}) {
  return ReactDOM.createPortal(
    <SkipToContentLink href={`#${target}`}>{children}</SkipToContentLink>,
    container
  )
})
