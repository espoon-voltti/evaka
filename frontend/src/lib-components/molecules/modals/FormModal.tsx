// SPDX-FileCopyrightText: 2017-2020 City of Espoo
//
// SPDX-License-Identifier: LGPL-2.1-or-later

import React from 'react'
import { FontAwesomeIcon } from '@fortawesome/react-fontawesome'
import { IconProp } from '@fortawesome/fontawesome-svg-core'
import styled from 'styled-components'
import FocusLock from 'react-focus-lock'
import Button from '../../atoms/buttons/Button'
import AsyncButton from '../../atoms/buttons/AsyncButton'
import UnderRowStatusIcon, { InfoStatus } from '../../atoms/StatusIcon'
import Title from '../../atoms/Title'
import { P } from '../../typography'
import { defaultMargins, Gap } from '../../white-space'
import { modalZIndex } from '../../layout/z-helpers'

export const DimmedModal = styled.div``

interface zIndexProps {
  zIndex?: number
}

export const BackgroundOverlay = styled.div<zIndexProps>`
  position: fixed;
  top: 0;
  left: 0;
  right: 0;
  bottom: 0;
  background-color: rgba(0, 0, 0, 0.5);
  z-index: ${(p) => (p.zIndex ? p.zIndex - 2 : modalZIndex - 2)};
`

const FormModalLifter = styled.div<zIndexProps>`
  z-index: ${(p) => (p.zIndex ? p.zIndex - 1 : modalZIndex - 1)};
`

interface ModalContainerProps {
  size: ModalSize
  customSize?: string
}

export const ModalContainer = styled.div<ModalContainerProps>`
  max-width: ${(props: ModalContainerProps) => {
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
  background: white;
  overflow-x: visible;
  box-shadow: 0 15px 75px 0 rgba(0, 0, 0, 0.5);
  border-radius: 2px;
  padding-left: ${defaultMargins.XXL};
  padding-right: ${defaultMargins.XXL};
  margin-left: ${defaultMargins.xxs};
  margin-right: ${defaultMargins.xxs};
  overflow-y: scroll;
`

export const ModalWrapper = styled.div<zIndexProps>`
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

export type IconColour = 'blue' | 'orange' | 'green' | 'red'

interface ModalIconProps {
  colour: IconColour
}

export const ModalIcon = styled.div<ModalIconProps>`
  background: ${({ theme: { colors }, ...props }) => {
    switch (props.colour) {
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

export const ModalBackground = styled.div`
  background: rgba(15, 15, 15, 0.86);
`

export const ModalButtons = styled.div`
  display: flex;
  flex-direction: row;
  justify-content: center;
  margin-top: ${defaultMargins.XXL};
  margin-bottom: ${defaultMargins.X3L};
`

export const ModalTitle = styled.div`
  margin-bottom: ${defaultMargins.XXL};
  margin-top: ${defaultMargins.XXL};
`

export type ModalSize = 'xs' | 'sm' | 'md' | 'lg' | 'xlg' | 'custom'

type CommonProps = {
  title?: string
  text?: string
  size?: ModalSize
  resolve: {
    action: () => void
    label: string
    disabled?: boolean
    info?: {
      text: string
      status?: InfoStatus
    }
  }
  reject?: {
    action: () => void
    label: string
  }
  className?: string
  icon?: IconProp
  'data-qa'?: string
  children?: React.ReactNode
  iconColour?: IconColour
}

function ModalBase({
  'data-qa': dataQa,
  title,
  text,
  className,
  icon,
  size = 'md',
  children,
  iconColour = 'blue',
  resolve,
  onSubmit
}: CommonProps & { onSubmit?: () => void }) {
  return (
    <FocusLock>
      <DimmedModal>
        <BackgroundOverlay />
        <FormModalLifter>
          <ModalWrapper className={className} data-qa={dataQa}>
            <ModalBackground />
            <ModalContainer size={size} data-qa="form-modal">
              {title && title.length > 0 ? (
                <ModalTitle>
                  {icon && (
                    <>
                      <ModalIcon colour={iconColour}>
                        <FontAwesomeIcon icon={icon} />
                      </ModalIcon>
                      <Gap size={'m'} />
                    </>
                  )}
                  {title && (
                    <Title size={1} data-qa="title" centered>
                      {title}
                    </Title>
                  )}
                  {text && (
                    <P data-qa="text" centered>
                      {text}
                    </P>
                  )}
                </ModalTitle>
              ) : (
                <Gap size={'L'} />
              )}
              <form
                onSubmit={(event) => {
                  event.preventDefault()
                  if (onSubmit) {
                    if (!resolve.disabled) onSubmit()
                  }
                }}
              >
                {children}
              </form>
            </ModalContainer>
          </ModalWrapper>
        </FormModalLifter>
      </DimmedModal>
    </FocusLock>
  )
}

export default React.memo(function FormModal({
  children,
  reject,
  resolve,
  ...props
}: CommonProps) {
  return (
    <ModalBase {...props} resolve={resolve} onSubmit={resolve.action}>
      {children}
      <ModalButtons>
        {reject && (
          <>
            <Button
              onClick={reject.action}
              data-qa="modal-cancelBtn"
              text={reject.label}
            />
            <Gap horizontal size={'xs'} />
          </>
        )}
        <Button
          primary
          data-qa="modal-okBtn"
          onClick={resolve.action}
          disabled={resolve.disabled}
          text={resolve.label}
        />
      </ModalButtons>
      {resolve.info ? (
        <ButtonUnderRow className={resolve.info?.status}>
          <span>{resolve.info?.text}</span>
          <UnderRowStatusIcon status={resolve.info?.status} />
        </ButtonUnderRow>
      ) : null}
    </ModalBase>
  )
})

type AsyncModalProps = CommonProps & {
  resolve: {
    //eslint-disable-next-line @typescript-eslint/no-explicit-any
    action: () => Promise<any>
    label: string
    disabled?: boolean
    onSuccess: () => void
  }
  reject: {
    action: () => void
    label: string
  }
}

export const AsyncFormModal = React.memo(function AsyncFormModal({
  children,
  resolve,
  reject,
  ...props
}: AsyncModalProps) {
  return (
    <ModalBase {...props} resolve={resolve}>
      {children}
      <ModalButtons>
        {reject && (
          <>
            <Button
              onClick={reject.action}
              data-qa="modal-cancelBtn"
              text={reject.label}
            />
            <Gap horizontal size={'xs'} />
          </>
        )}
        <AsyncButton
          primary
          text={resolve.label}
          disabled={resolve.disabled}
          onClick={resolve.action}
          onSuccess={resolve.onSuccess}
          data-qa="modal-okBtn"
        />
      </ModalButtons>
    </ModalBase>
  )
})

const ButtonUnderRow = styled.div`
  margin-top: calc(-${defaultMargins.X3L} + ${defaultMargins.xs});
  display: flex;
  align-items: center;
  justify-content: center;
  position: absolute;
  left: 0;
  right: 0;

  font-size: 12px;
  text-overflow: ellipsis;

  word-wrap: break-word;
  white-space: normal;

  color: ${({ theme: { colors } }) => colors.greyscale.dark};

  &.success {
    color: ${({ theme: { colors } }) => colors.accents.greenDark};
  }

  &.warning {
    color: ${({ theme: { colors } }) => colors.accents.orangeDark};
  }
`
