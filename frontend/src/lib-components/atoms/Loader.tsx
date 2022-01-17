// SPDX-FileCopyrightText: 2017-2022 City of Espoo
//
// SPDX-License-Identifier: LGPL-2.1-or-later

import React from 'react'
import styled, { keyframes } from 'styled-components'
import { fontWeights } from '../typography'

const rotate = keyframes`
  from {
    transform: rotate(0deg);
  }

  to {
    transform: rotate(360deg);
  }
`

const Spinner = styled.div`
  font-weight: ${fontWeights.normal};
  border-collapse: collapse;
  border-spacing: 0;
  color: ${(p) => p.theme.colors.grayscale.g100};
  text-align: left !important;
  line-height: 1.3em;
  box-sizing: inherit;
  border-radius: 50%;
  min-width: 65px;
  min-height: 65px;
  margin: 2em;
  font-size: 10px;
  text-indent: -9999em;
  border: 8px solid ${(p) => p.theme.colors.main.m3}32; // hex 32 is 0.2 alpha
  border-left-color: ${(p) => p.theme.colors.main.m3};
  transform: translateZ(0);
  animation: ${rotate} 1.1s linear infinite;
`

const Wrapper = styled.div`
  font-family: Open Sans, Arial, sans-serif;
  font-size: 1em;
  font-weight: ${fontWeights.normal};
  border-collapse: collapse;
  border-spacing: 0;
  color: ${(p) => p.theme.colors.grayscale.g100};
  text-align: left !important;
  line-height: 1.3em;
  box-sizing: inherit;
  display: flex;
  flex-direction: column;
  justify-content: center;
  align-items: center;
`

export default React.memo(function Loader() {
  return (
    <Wrapper>
      <Spinner />
    </Wrapper>
  )
})
