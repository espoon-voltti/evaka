// SPDX-FileCopyrightText: 2017-2020 City of Espoo
//
// SPDX-License-Identifier: LGPL-2.1-or-later

import React from 'react'
import styled from 'styled-components'
import { NavLink } from 'react-router-dom'
import Colors from '~components/shared/Colors'

type Props = {
  tabs: Array<{
    id: string
    link: string
    label: string
  }>
}

export default React.memo(function Tabs({ tabs }: Props) {
  return (
    <TabsContainer>
      {tabs.map(({ id, link, label }) => (
        <Tab key={id} to={link} data-qa={`${id}-tab`}>
          {label}
        </Tab>
      ))}
    </TabsContainer>
  )
})

const TabsContainer = styled.div`
  display: flex;
  flex-direction: row;
`

const Tab = styled(NavLink)`
  flex: 1;
  padding: 12px;
  font-family: Montserrat, sans-serif;
  font-size: 15px;
  text-align: center;
  text-transform: uppercase;
  letter-spacing: 1px;
  color: ${Colors.greyscale.dark};
  background-color: ${Colors.greyscale.white};

  &.active {
    font-weight: 700;
    color: ${Colors.blues.medium};
    background-color: ${Colors.blues.light}33;
  }
`
