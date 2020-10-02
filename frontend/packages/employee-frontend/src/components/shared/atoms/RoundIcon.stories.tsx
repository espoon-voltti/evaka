// SPDX-FileCopyrightText: 2017-2020 City of Espoo
//
// SPDX-License-Identifier: LGPL-2.1-or-later

import React from 'react'
import { storiesOf } from '@storybook/react'
import styled from 'styled-components'
import { DefaultMargins } from 'components/shared/layout/white-space'
import { FixedSpaceRow } from 'components/shared/layout/flex-helpers'
import RoundIcon from 'components/shared/atoms/RoundIcon'
import {
  faCheck,
  faExclamation,
  fasInfo,
  faPlus,
  fasExclamationTriangle,
  faEuroSign
} from '@evaka/icons'
import Colors from 'components/shared/Colors'
import { FontAwesomeIcon } from '@fortawesome/react-fontawesome'
import { action } from '@storybook/addon-actions'

const ColItem = styled.div`
  display: flex;
  flex-direction: column;
  justify-content: flex-start;
  align-items: center;
  > * {
    margin-bottom: ${DefaultMargins.s};
  }
`

const onClick = action('clicked')

storiesOf('evaka/atoms/RoundIcon', module)
  .add('default', () => (
    <div>
      <FixedSpaceRow spacing="L">
        <ColItem>
          <span>s</span>
          <RoundIcon content={fasInfo} color={Colors.accents.water} size="s" />
        </ColItem>
        <ColItem>
          <span>m</span>
          <RoundIcon
            content={faExclamation}
            color={Colors.accents.orange}
            size="m"
          />
        </ColItem>
        <ColItem>
          <span>m (text)</span>
          <RoundIcon content="T" color={Colors.accents.emerald} size="m" />
        </ColItem>
        <ColItem>
          <span>L</span>
          <RoundIcon content={faPlus} color={Colors.primary} size="L" />
        </ColItem>
        <ColItem>
          <span>XL</span>
          <RoundIcon content={faCheck} color={Colors.accents.green} size="XL" />
        </ColItem>
      </FixedSpaceRow>
      <p>Note 1: use solid icons for size s, light otherwise</p>
      <p>
        Note 2: xs not included here, use solid icon with font-size 16px and
        pre-included background, e.g. fasExclamationTriangle{' '}
        <FontAwesomeIcon
          icon={fasExclamationTriangle}
          color={Colors.accents.orange}
          style={{ fontSize: '16px' }}
        />
      </p>
    </div>
  ))
  .add('as filters', () => (
    <div>
      <FixedSpaceRow spacing="xs">
        <RoundIcon
          content="T"
          color={Colors.accents.emerald}
          size="m"
          onClick={onClick}
          active={false}
        />
        <RoundIcon
          content="2"
          color={Colors.primary}
          size="m"
          onClick={onClick}
          active={true}
        />
        <RoundIcon
          content={fasInfo}
          color={Colors.accents.violet}
          size="m"
          onClick={onClick}
          active={false}
        />
        <RoundIcon
          content={faEuroSign}
          color={Colors.accents.petrol}
          size="m"
          onClick={onClick}
          active={true}
        />
      </FixedSpaceRow>
    </div>
  ))
