// SPDX-FileCopyrightText: 2017-2021 City of Espoo
//
// SPDX-License-Identifier: LGPL-2.1-or-later

import React, { Fragment } from 'react'
import { FontAwesomeIcon } from '@fortawesome/react-fontawesome'
import { faTimes } from 'lib-icons'
import { IconProp } from '@fortawesome/fontawesome-svg-core'
import styled from 'styled-components'

import ModalBackground from './ModalBackground'
import {
  ModalWrapper,
  ModalButtons,
  ModalIcon,
  ModalContainer,
  ModalTitle,
  IconColour
} from './FormModal'
import Title from 'lib-components/atoms/Title'
import { Gap } from 'lib-components/white-space'
import { fontWeights, P } from 'lib-components/typography'
import InlineButton from 'lib-components/atoms/buttons/InlineButton'

const ModalContent = styled.div<{ marginBottom: number }>`
  position: relative;
  max-height: calc(100vh - 40px);
  width: auto;
  overflow: auto;
  margin-bottom: ${(p) => `${p.marginBottom}px`};
`

const CloseButton = styled.button`
  position: absolute;
  top: -20px;
  right: 0px;
  background: none;
  border: none;
  text-transform: uppercase;
  color: white;
  font-size: 14px;
  font-weight: ${fontWeights.semibold};
  cursor: pointer;

  svg {
    font-size: 24px;
    vertical-align: middle;
  }
`

const CloseButtonText = styled.span`
  display: inline-block;
  padding-right: 12px;
  vertical-align: middle;
`

interface Props {
  title?: string
  close?: () => void
  'data-qa'?: string
  icon?: IconProp
  iconColour?: IconColour
  children?: React.ReactNode
  text?: string | React.ReactNode
  resolve?: {
    action: () => void
    label: string
  }
  reject?: {
    action: () => void
    label: string
  }
  resolveDisabled?: boolean
  zIndex?: number
}

function InfoModal({
  'data-qa': dataQa,
  title,
  close,
  children = null,
  icon,
  iconColour = 'blue',
  text,
  resolve,
  reject,
  resolveDisabled,
  zIndex
}: Props) {
  return (
    <ModalBackground zIndex={zIndex}>
      <ModalWrapper data-qa={dataQa} zIndex={zIndex}>
        <ModalContainer data-qa="modal">
          {close && (
            <CloseButton>
              <CloseButtonText>Sulje</CloseButtonText>
              <FontAwesomeIcon icon={faTimes} />
            </CloseButton>
          )}
          <ModalContent marginBottom={resolve ? 0 : 80}>
            <ModalTitle>
              {icon && (
                <Fragment>
                  <ModalIcon colour={iconColour}>
                    <FontAwesomeIcon icon={icon} />
                  </ModalIcon>
                  <Gap size={'m'} />
                </Fragment>
              )}
              <Title size={1} data-qa="title" centered>
                {title}
              </Title>
              {text && (
                <Fragment>
                  <P data-qa="text" centered>
                    {text}
                  </P>
                </Fragment>
              )}
            </ModalTitle>
            {children}
            {resolve && (
              <ModalButtons $singleButton={!reject}>
                {reject && (
                  <>
                    <InlineButton
                      onClick={reject.action}
                      data-qa="modal-cancelBtn"
                      text={reject.label}
                    />
                    <Gap horizontal size={'xs'} />
                  </>
                )}
                <InlineButton
                  data-qa="modal-okBtn"
                  onClick={resolve.action}
                  disabled={resolveDisabled}
                  text={resolve.label}
                />
              </ModalButtons>
            )}
          </ModalContent>
        </ModalContainer>
      </ModalWrapper>
    </ModalBackground>
  )
}

export default InfoModal
