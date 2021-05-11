// SPDX-FileCopyrightText: 2017-2021 City of Espoo
//
// SPDX-License-Identifier: LGPL-2.1-or-later

import React from 'react'
import styled from 'styled-components'
import { tabletMin } from 'lib-components/breakpoints'
import colors from 'lib-components/colors'
import { useTranslation } from 'employee-frontend/state/i18n'

type Props = {
}

export default React.memo(function MessagesList({
}: Props) {
  const t = useTranslation()
  console.log(t)

  return (
      <Container>
      </Container>
  )
})


const Container = styled.div`
  min-width: 35%;
  max-width: 400px;
  min-height: 500px;
  background-color: ${colors.greyscale.white};

  @media (max-width: 750px) {
    min-width: 50%;
  }

  @media (max-width: ${tabletMin}) {
    width: 100%;
    max-width: 100%;
  }

  &.desktop-only {
    @media (max-width: ${tabletMin}) {
      display: none;
    }
  }
`