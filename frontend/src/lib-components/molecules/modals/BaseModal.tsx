// SPDX-FileCopyrightText: 2017-2022 City of Espoo
//
// SPDX-License-Identifier: LGPL-2.1-or-later

import { IconProp } from '@fortawesome/fontawesome-svg-core'
import { FontAwesomeIcon } from '@fortawesome/react-fontawesome'
import React from 'react'
import styled, { css } from 'styled-components'

import IconButton from 'lib-components/atoms/buttons/IconButton'
import { tabletMin } from 'lib-components/breakpoints'
import { modalZIndex } from 'lib-components/layout/z-helpers'
import { H1, P } from 'lib-components/typography'
import { defaultMargins, Gap } from 'lib-components/white-space'
import { faTimes } from 'lib-icons'

import ModalBackground from './ModalBackground'

export interface ModalBaseProps {
  title: string
  text?: string
  className?: string
  icon?: IconProp
  type?: ModalType
  mobileFullScreen?: boolean
  zIndex?: number
  children?: React.ReactNode | React.ReactNodeArray
  'data-qa'?: string
}

export type ModalType = 'info' | 'success' | 'warning' | 'danger'

interface Props extends ModalBaseProps {
  close: () => void
  closeLabel: string
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
          margin="auto"
          data-qa="modal"
        >
          <ModalTitle>
            {props.icon && (
              <>
                <ModalIcon type={props.type}>
                  <FontAwesomeIcon icon={props.icon} />
                </ModalIcon>
                <Gap size="m" />
              </>
            )}
            {!!props.title && <H1 data-qa="title">{props.title}</H1>}
            {!!props.text && (
              <P data-qa="text" preserveWhiteSpace>
                {props.text}
              </P>
            )}
          </ModalTitle>
          {props.children}
          <ModalCloseButton close={props.close} closeLabel={props.closeLabel} />
        </ModalContainer>
      </ModalWrapper>
    </ModalBackground>
  )
})

export const ModalButtons = styled.div<{
  $justifyContent?: 'center' | 'space-between'
}>`
  display: flex;
  flex-direction: row-reverse;
  margin-top: ${defaultMargins.XXL};
  margin-bottom: ${defaultMargins.X3L};
  justify-content: ${({ $justifyContent }) => $justifyContent};

  @media (max-width: ${tabletMin}) {
    margin-bottom: ${defaultMargins.L};
  }
`

const CloseButton = styled(IconButton)`
  position: absolute;
  top: ${defaultMargins.s};
  right: ${defaultMargins.s};
  color: ${(p) => p.theme.colors.grayscale.g70};
`

export const ModalCloseButton = React.memo(function ModalCloseButton({
  close,
  closeLabel,
  'data-qa': dataQa
}: {
  close: () => void
  closeLabel: string
  'data-qa'?: string
}) {
  return (
    <CloseButton
      icon={faTimes}
      onClick={close}
      altText={closeLabel}
      data-qa={dataQa}
    />
  )
})

const ModalContainer = styled.div<{
  mobileFullScreen?: boolean
  noPadding?: boolean
  margin: string
}>`
  position: relative;
  width: min(500px, calc(100vw - 2 * ${defaultMargins.xxs}));
  max-height: calc(100vh - 2 * ${defaultMargins.s});
  background: ${(p) => p.theme.colors.grayscale.g0};
  overflow-x: visible;
  box-shadow: 0 15px 75px 0 rgba(0, 0, 0, 0.5);
  border-radius: 2px;
  ${(p) => (p.noPadding ? '' : `padding-left: ${defaultMargins.XXL}`)};
  ${(p) => (p.noPadding ? '' : `padding-right: ${defaultMargins.XXL}`)};
  margin: ${(p) => p.margin};
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

const ModalIcon = styled.div<{ type?: ModalType }>`
  background: ${({ type = 'info', ...p }) => p.theme.colors.status[type]};
  font-size: 36px;
  border-radius: 50%;
  line-height: 60px;
  height: 60px;
  width: 60px;
  text-align: center;
  color: ${(p) => p.theme.colors.grayscale.g0};
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
`

type PlainModalProps = Pick<
  ModalBaseProps,
  'className' | 'zIndex' | 'data-qa' | 'mobileFullScreen' | 'children'
> & {
  margin: string
}

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
          margin={props.margin}
          data-qa="modal"
        >
          {props.children}
        </ModalContainer>
      </StaticallyPositionedModal>
    </ModalBackground>
  )
})
