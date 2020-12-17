// SPDX-FileCopyrightText: 2017-2020 City of Espoo
//
// SPDX-License-Identifier: LGPL-2.1-or-later

import React, { Fragment } from 'react'
import { FontAwesomeIcon } from '@fortawesome/react-fontawesome'
import { faTimes } from '@evaka/lib-icons'
import { IconProp } from '@fortawesome/fontawesome-svg-core'
import styled from 'styled-components'
import FocusLock from 'react-focus-lock'

import { useTranslation } from '~state/i18n'
import {
  DimmedModal,
  BackgroundOverlay,
  ModalWrapper,
  ModalBackground,
  ModalButtons,
  ModalIcon,
  ModalContainer,
  ModalTitle,
  ModalSize,
  IconColour
} from './FormModal'
import Title from '~components/shared/atoms/Title'
import { Gap } from '@evaka/lib-components/src/white-space'
import Button from '~components/shared/atoms/buttons/Button'
import { P } from '@evaka/lib-components/src/typography'

interface SizeProps {
  size: ModalSize
  customSize?: string
}

interface ModalContentProps {
  marginBottom: number
}

const ModalContent = styled.div<ModalContentProps>`
  position: relative;
  max-height: calc(100vh - 40px);
  width: auto;
  overflow: auto;
  margin-bottom: ${(p) => `${p.marginBottom}px`};
`

const ButtonContainer = styled.div<SizeProps>`
  display: flex;
  width: ${(props: SizeProps) => {
    switch (props.size) {
      case 'xs':
        return '300px'
      case 'sm':
        return '400px'
      case 'md':
        return '500px'
      case 'lg':
        return '600px'
      case 'xlg':
        return '700px'
      case 'custom':
        return props.customSize ?? '500px'
    }
  }};
  justify-content: flex-end;
  position: relative;
`

const CloseButton = styled.button`
  background: none;
  border: none;
  text-transform: uppercase;
  color: white;
  font-size: 14px;
  font-weight: 600;
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
  size?: ModalSize
  customSize?: string
  text?: string | React.ReactNode
  resolve?: () => void
  resolveLabel?: string
  reject?: () => void
  rejectLabel?: string
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
  size = 'lg',
  customSize,
  text,
  resolve,
  resolveLabel,
  reject,
  rejectLabel,
  resolveDisabled,
  zIndex
}: Props) {
  const { i18n } = useTranslation()
  return (
    <FocusLock>
      <DimmedModal>
        <BackgroundOverlay />
        <ModalWrapper data-qa={dataQa} zIndex={zIndex}>
          <ModalBackground />
          {close && (
            <ButtonContainer
              size={size}
              customSize={customSize}
              onClick={close}
            >
              <CloseButton>
                <CloseButtonText>Sulje</CloseButtonText>
                <FontAwesomeIcon icon={faTimes} />
              </CloseButton>
            </ButtonContainer>
          )}
          <ModalContainer size={size} customSize={customSize} data-qa="modal">
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
                <ModalButtons>
                  {reject && (
                    <>
                      <Button
                        onClick={reject}
                        dataQa="modal-cancelBtn"
                        text={rejectLabel ?? i18n.common.cancel}
                      />
                      <Gap horizontal size={'xs'} />
                    </>
                  )}
                  <Button
                    primary
                    dataQa="modal-okBtn"
                    onClick={resolve}
                    disabled={resolveDisabled}
                    text={resolveLabel ?? i18n.common.ok}
                  />
                </ModalButtons>
              )}
            </ModalContent>
          </ModalContainer>
        </ModalWrapper>
      </DimmedModal>
    </FocusLock>
  )
}

export default InfoModal
