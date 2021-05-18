// SPDX-FileCopyrightText: 2017-2020 City of Espoo
//
// SPDX-License-Identifier: LGPL-2.1-or-later

import React from 'react'
import styled, { useTheme } from 'styled-components'
import { storiesOf } from '@storybook/react'
import { action } from '@storybook/addon-actions'
import { FontAwesomeIcon } from '@fortawesome/react-fontawesome'
import {
  faCheck,
  faExclamation,
  fasInfo,
  faPlus,
  fasExclamationTriangle,
  faEuroSign
} from 'lib-icons'
import { defaultMargins } from '../white-space'
import { FixedSpaceRow } from '../layout/flex-helpers'
import RoundIcon from './RoundIcon'

const ColItem = styled.div`
  display: flex;
  flex-direction: column;
  justify-content: flex-start;
  align-items: center;
  > * {
    margin-bottom: ${defaultMargins.s};
  }
`

const onClick = action('clicked')

storiesOf('evaka/atoms/RoundIcon', module)
  .add('default', () =>
    React.createElement(() => {
      const theme = useTheme()
      return (
        <div>
          <FixedSpaceRow spacing="L">
            <ColItem>
              <span>s</span>
              <RoundIcon
                content={fasInfo}
                color={theme.colors.accents.water}
                size="s"
              />
            </ColItem>
            <ColItem>
              <span>m</span>
              <RoundIcon
                content={faExclamation}
                color={theme.colors.accents.orange}
                size="m"
              />
            </ColItem>
            <ColItem>
              <span>m (text)</span>
              <RoundIcon
                content="T"
                color={theme.colors.accents.emerald}
                size="m"
              />
            </ColItem>
            <ColItem>
              <span>L</span>
              <RoundIcon
                content={faPlus}
                color={theme.colors.main.primary}
                size="L"
              />
            </ColItem>
            <ColItem>
              <span>XL</span>
              <RoundIcon
                content={faCheck}
                color={theme.colors.accents.green}
                size="XL"
              />
            </ColItem>
            <ColItem>
              <span>XXL</span>
              <RoundIcon
                content={faCheck}
                color={theme.colors.accents.green}
                size="XXL"
              />
            </ColItem>
          </FixedSpaceRow>
          <p>Note 1: use solid icons for size s, light otherwise</p>
          <p>
            Note 2: xs not included here, use solid icon with font-size 16px and
            pre-included background, e.g. fasExclamationTriangle{' '}
            <FontAwesomeIcon
              icon={fasExclamationTriangle}
              color={theme.colors.accents.orange}
              style={{ fontSize: '16px' }}
            />
          </p>
          <br />
          <FixedSpaceRow>
            <ColItem>
              <span>With label</span>
              <RoundIcon
                content={faCheck}
                color={theme.colors.main.primary}
                size="XL"
                label={'label'}
              />
            </ColItem>
            <ColItem>
              <span>With label and a bubble</span>
              <RoundIcon
                content={faCheck}
                color={theme.colors.main.primary}
                size="XL"
                label={'label'}
                bubble
              />
            </ColItem>
            <ColItem>
              <span>With label and a bubble with a number</span>
              <RoundIcon
                content={faCheck}
                color={theme.colors.main.primary}
                size="XL"
                label={'label'}
                number={3}
                bubble
              />
            </ColItem>
            <ColItem>
              <span>Large with label and a bubble</span>
              <RoundIcon
                content={faCheck}
                color={theme.colors.main.primary}
                size="L"
                label={'label'}
                bubble
              />
            </ColItem>
            <ColItem>
              <span>Large with label and a bubble</span>
              <RoundIcon
                content={faCheck}
                color={theme.colors.main.primary}
                size="L"
                label={'label'}
                number={2}
                bubble
              />
            </ColItem>
          </FixedSpaceRow>

          <p>Note 3: the XL size on mobile will be 56px instead of 64px.</p>
          <p>
            Note 4: the bubble will only work with XL and L sizes and RoundIcons
            with a label
          </p>
        </div>
      )
    })
  )
  .add('as filters', () =>
    React.createElement(() => {
      const theme = useTheme()
      return (
        <div>
          <FixedSpaceRow spacing="xs">
            <RoundIcon
              content="T"
              color={theme.colors.accents.emerald}
              size="m"
              onClick={onClick}
              active={false}
            />
            <RoundIcon
              content="2"
              color={theme.colors.main.primary}
              size="m"
              onClick={onClick}
              active={true}
            />
            <RoundIcon
              content={fasInfo}
              color={theme.colors.accents.violet}
              size="m"
              onClick={onClick}
              active={false}
            />
            <RoundIcon
              content={faEuroSign}
              color={theme.colors.accents.petrol}
              size="m"
              onClick={onClick}
              active={true}
            />
          </FixedSpaceRow>
        </div>
      )
    })
  )
