// SPDX-FileCopyrightText: 2017-2021 City of Espoo
//
// SPDX-License-Identifier: LGPL-2.1-or-later

import React from 'react'
import styled, { css } from 'styled-components'
import { IconProp } from '@fortawesome/fontawesome-svg-core'
import { FontAwesomeIcon } from '@fortawesome/react-fontawesome'
import { faTimes } from 'lib-icons'
import { tabletMin } from 'lib-components/breakpoints'
import { H1, P } from 'lib-components/typography'
import { defaultMargins, Gap } from 'lib-components/white-space'
import { modalZIndex } from 'lib-components/layout/z-helpers'
import IconButton from 'lib-components/atoms/buttons/IconButton'
import ModalBackground from './ModalBackground'

export interface ModalBaseProps {
  title: string
  text?: string
  className?: string
  icon?: IconProp
  iconColour?: IconColor
  mobileFullScreen?: boolean
  zIndex?: number
  children?: React.ReactNode | React.ReactNodeArray
  'data-qa'?: string
}

export type IconColor = 'blue' | 'orange' | 'green' | 'red'

interface Props extends ModalBaseProps {
  close: () => void
}

export default React.memo(function BaseModal(props: Props) {
  return (
    <ModalBackground>
      <ModalWrapper
        className={props.className}
        zIndex={props.zIndex}
        data-qa={props['data-qa']}
      >
        <ModalContainer
          mobileFullScreen={props.mobileFullScreen}
          data-qa="modal"
        >
          <CloseButton icon={faTimes} onClick={props.close} />
          <ModalTitle>
            {props.icon && (
              <>
                <ModalIcon color={props.iconColour}>
                  <FontAwesomeIcon icon={props.icon} />
                </ModalIcon>
                <Gap size={'m'} />
              </>
            )}
            {props.title && (
              <H1 data-qa="title" centered>
                {props.title}
              </H1>
            )}
            {props.text && (
              <P data-qa="text" centered>
                {props.text}
              </P>
            )}
          </ModalTitle>
          {props.children}
        </ModalContainer>
      </ModalWrapper>
    </ModalBackground>
  )
})

export const ModalButtons = styled.div<{ $singleButton?: boolean }>`
  display: flex;
  flex-direction: row;
  margin-top: ${defaultMargins.XXL};
  margin-bottom: ${defaultMargins.X3L};
  justify-content: ${(p) => (p.$singleButton ? `center` : `space-between`)};

  @media (max-width: ${tabletMin}) {
    margin-bottom: ${defaultMargins.L};
  }
`

const CloseButton = styled(IconButton)`
  position: absolute;
  top: ${defaultMargins.s};
  right: ${defaultMargins.s};
  color: ${({ theme }) => theme.colors.greyscale.dark};
`

const ModalContainer = styled.div<{
  mobileFullScreen?: boolean
  noPadding?: boolean
}>`
  position: relative;
  width: min(500px, calc(100vw - 2 * ${defaultMargins.xxs}));
  max-height: calc(100vh - 2 * ${defaultMargins.s});
  background: white;
  overflow-x: visible;
  box-shadow: 0 15px 75px 0 rgba(0, 0, 0, 0.5);
  border-radius: 2px;
  ${(p) => (p.noPadding ? '' : `padding-left: ${defaultMargins.XXL}`)};
  ${(p) => (p.noPadding ? '' : `padding-right: ${defaultMargins.XXL}`)};
  margin-left: ${defaultMargins.xxs};
  margin-right: ${defaultMargins.xxs};
  overflow-y: auto;

  @media (max-width: ${tabletMin}) {
    ${(p) => (p.noPadding ? '' : `padding-left: ${defaultMargins.s}`)};
    ${(p) => (p.noPadding ? '' : `padding-right: ${defaultMargins.s}`)};
    margin-left: ${defaultMargins.s};
    margin-right: ${defaultMargins.s};

    ${(p) =>
      p.mobileFullScreen
        ? css`
            margin-left: 0;
            margin-right: 0;
            max-width: 100vw;
            max-height: 100vh;
            width: 100vw;
            height: 100vh;
          `
        : ''}
  }
`

const ModalWrapper = styled.div<{ zIndex?: number }>`
  align-items: center;
  display: flex;
  flex-direction: column;
  justify-content: center;
  overflow: hidden;
  position: fixed;
  z-index: ${(p) => (p.zIndex ? p.zIndex : modalZIndex)};
  bottom: 0;
  left: 0;
  right: 0;
  top: 0;
`

const ModalIcon = styled.div<{ color?: IconColor }>`
  background: ${({ theme: { colors }, ...props }) => {
    switch (props.color) {
      case 'blue':
        return colors.main.medium
      case 'orange':
        return colors.accents.orange
      case 'green':
        return colors.accents.green
      case 'red':
        return colors.accents.red
      default:
        return colors.main.medium
    }
  }};
  font-size: 36px;
  border-radius: 50%;
  line-height: 60px;
  height: 60px;
  width: 60px;
  text-align: center;
  color: #fff;
  margin: auto;
`

const ModalTitle = styled.div`
  margin-bottom: ${defaultMargins.XXL};
  margin-top: ${defaultMargins.XXL};

  @media (max-width: ${tabletMin}) {
    margin-bottom: ${defaultMargins.L};
    margin-top: ${defaultMargins.L};
  }
`

const StaticallyPositionedModal = styled(ModalWrapper)`
  justify-content: flex-start;
  padding-top: ${defaultMargins.XL};
`

type PlainModalProps = Pick<
  ModalBaseProps,
  'className' | 'zIndex' | 'data-qa' | 'mobileFullScreen' | 'children'
>

export const PlainModal = React.memo(function PlainModal(
  props: PlainModalProps
) {
  return (
    <ModalBackground>
      <StaticallyPositionedModal
        className={props.className}
        zIndex={props.zIndex}
        data-qa={props['data-qa']}
      >
        <ModalContainer
          noPadding
          mobileFullScreen={props.mobileFullScreen}
          data-qa="modal"
        >
          {props.children}
        </ModalContainer>
      </StaticallyPositionedModal>
    </ModalBackground>
  )
})
