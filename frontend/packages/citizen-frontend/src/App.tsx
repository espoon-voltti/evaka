// SPDX-FileCopyrightText: 2017-2020 City of Espoo
//
// SPDX-License-Identifier: LGPL-2.1-or-later

import React from 'react'
import styled from 'styled-components'
import { FontAwesomeIcon } from '@fortawesome/react-fontawesome'
import { faClock } from '@evaka/lib-icons'
import Header from './Header'

const Title = styled.h1`
  color: pink;
`

export default function App() {
  return (
    <>
      <Header />
      <Title>
        hello word <FontAwesomeIcon icon={faClock} />
      </Title>
    </>
  )
}
