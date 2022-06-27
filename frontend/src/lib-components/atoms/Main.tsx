// SPDX-FileCopyrightText: 2017-2022 City of Espoo
//
// SPDX-License-Identifier: LGPL-2.1-or-later

import React from 'react'
import styled from 'styled-components'

const MainContainer = styled.main`
  outline: none;
`

export default React.memo(function Loader({
  children
}: {
  children: React.ReactNode
}) {
  return (
    <MainContainer tabIndex={-1} id="main">
      {children}
    </MainContainer>
  )
})
