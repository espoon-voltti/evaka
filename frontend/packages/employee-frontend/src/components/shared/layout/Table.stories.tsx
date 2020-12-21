// SPDX-FileCopyrightText: 2017-2020 City of Espoo
//
// SPDX-License-Identifier: LGPL-2.1-or-later

import React from 'react'
import { storiesOf } from '@storybook/react'
import {
  Table,
  Thead,
  Tr,
  Th,
  Td,
  SortableTh
} from '@evaka/lib-components/src/layout/Table'
import { action } from '@storybook/addon-actions'

const onClick = action('clicked')

storiesOf('evaka/layout/table', module)
  .add('default', () => (
    <Table>
      <Thead>
        <Tr>
          <Th>Nimi</Th>
          <Th>Osoite</Th>
          <Th>Jotain</Th>
          <Th>Vähän pidempää</Th>
          <Th>Muuta</Th>
        </Tr>
      </Thead>
      <Tr>
        <Td>Meikäläinen Matti</Td>
        <Td>Turuntie 73 A 14</Td>
        <Td>Lorem ipsum</Td>
        <Td>I see trees of green, red roses too</Td>
        <Td>Hello wonderful</Td>
      </Tr>
      <Tr>
        <Td>Teikäläinen Teppo</Td>
        <Td>Aurajoenkuja 94 F 3</Td>
        <Td>Lorem ipsum</Td>
        <Td>I see them bloom, for me and you</Td>
        <Td>Hello world</Td>
      </Tr>
    </Table>
  ))
  .add('sortable', () => (
    <Table>
      <Thead>
        <Tr>
          <SortableTh onClick={onClick} sorted="ASC">
            Nimi
          </SortableTh>
          <SortableTh onClick={onClick} sorted="DESC">
            Osoite
          </SortableTh>
          <SortableTh onClick={onClick}>Jotain</SortableTh>
          <Th>Vähän pidempää</Th>
          <Th>Muuta</Th>
        </Tr>
      </Thead>
      <Tr>
        <Td>Meikäläinen Matti</Td>
        <Td>Turuntie 73 A 14</Td>
        <Td>Lorem ipsum</Td>
        <Td>I see trees of green, red roses too</Td>
        <Td>Hello wonderful</Td>
      </Tr>
      <Tr>
        <Td>Teikäläinen Teppo</Td>
        <Td>Aurajoenkuja 94 F 3</Td>
        <Td>Lorem ipsum</Td>
        <Td>I see them bloom, for me and you</Td>
        <Td>Hello world</Td>
      </Tr>
    </Table>
  ))
  .add('clickable rowa', () => (
    <Table>
      <Thead>
        <Tr>
          <Th>Nimi</Th>
          <Th>Osoite</Th>
          <Th>Jotain</Th>
          <Th>Vähän pidempää</Th>
          <Th>Muuta</Th>
        </Tr>
      </Thead>
      <Tr onClick={onClick}>
        <Td>Meikäläinen Matti</Td>
        <Td>Turuntie 73 A 14</Td>
        <Td>Lorem ipsum</Td>
        <Td>I see trees of green, red roses too</Td>
        <Td>Hello wonderful</Td>
      </Tr>
      <Tr onClick={onClick}>
        <Td>Teikäläinen Teppo</Td>
        <Td>Aurajoenkuja 94 F 3</Td>
        <Td>Lorem ipsum</Td>
        <Td>I see them bloom, for me and you</Td>
        <Td>Hello world</Td>
      </Tr>
    </Table>
  ))
