// SPDX-FileCopyrightText: 2017-2020 City of Espoo
//
// SPDX-License-Identifier: LGPL-2.1-or-later

import React, { Fragment } from 'react'
import { FontAwesomeIcon } from '@fortawesome/react-fontawesome'
import { IconProp } from '@fortawesome/fontawesome-svg-core'
import styled from 'styled-components'
import FocusLock from 'react-focus-lock'

import Title from '~components/shared/atoms/Title'
import Button from '~components/shared/atoms/buttons/Button'
import Colors from '~components/shared/Colors'
import { DefaultMargins, Gap } from 'components/shared/layout/white-space'
import { useTranslation } from '~state/i18n'
import { modalZIndex } from '~components/shared/layout/z-helpers'

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
  width: ${(props: ModalContainerProps) => {
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
  max-width: 100vw;
  background: white;
  overflow-x: visible;
  box-shadow: 0px 15px 75px 0px rgba(0, 0, 0, 0.5);
  border-radius: 2px;
  padding-left: ${DefaultMargins.XXL};
  padding-right: ${DefaultMargins.XXL};
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
  background: ${(props: ModalIconProps) => {
    switch (props.colour) {
      case 'blue':
        return Colors.blues.medium
      case 'orange':
        return Colors.accents.orange
      case 'green':
        return Colors.accents.green
      case 'red':
        return Colors.accents.red
      default:
        return Colors.blues.medium
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
  margin-top: ${DefaultMargins.XXL};
  margin-bottom: ${DefaultMargins.X3L};
`

export const ModalTitle = styled.div`
  margin-bottom: ${DefaultMargins.XXL};
  margin-top: ${DefaultMargins.XXL};
`

export const Text = styled.div`
  text-align: center;
`

export type ModalSize = 'xs' | 'sm' | 'md' | 'lg' | 'xlg' | 'custom'

interface Props {
  title?: string
  resolve: () => void
  reject?: () => void
  resolveDisabled?: boolean
  text?: string
  size?: ModalSize
  resolveLabel?: string
  rejectLabel?: string
  className?: string
  icon?: IconProp
  'data-qa'?: string
  children: React.ReactNode
  iconColour?: IconColour
}

const handleClick = (click: () => void) => () => click()

function FormModal({
  'data-qa': dataQa,
  title,
  text,
  resolveLabel,
  rejectLabel,
  className,
  reject,
  icon,
  resolve,
  size = 'md',
  resolveDisabled = false,
  children,
  iconColour = 'blue'
}: Props) {
  const { i18n } = useTranslation()
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
                    <Fragment>
                      <ModalIcon colour={iconColour}>
                        <FontAwesomeIcon icon={icon} />
                      </ModalIcon>
                      <Gap size={'m'} />
                    </Fragment>
                  )}
                  {title && (
                    <Title size={1} data-qa="title" centered>
                      {title}
                    </Title>
                  )}
                  {text && (
                    <Fragment>
                      <Text data-qa="text">{text}</Text>
                    </Fragment>
                  )}
                </ModalTitle>
              ) : (
                <Gap size={'L'} />
              )}
              <form
                onSubmit={(event) => {
                  event.preventDefault()
                  if (!resolveDisabled) resolve()
                }}
              >
                {children}
                <ModalButtons>
                  {reject && (
                    <>
                      <Button
                        onClick={handleClick(reject)}
                        dataQa="modal-cancelBtn"
                        text={rejectLabel ?? i18n.common.cancel}
                      />
                      <Gap horizontal size={'xs'} />
                    </>
                  )}
                  <Button
                    primary
                    dataQa="modal-okBtn"
                    onClick={handleClick(resolve)}
                    disabled={resolveDisabled}
                    text={resolveLabel ?? i18n.common.confirm}
                  />
                </ModalButtons>
              </form>
            </ModalContainer>
          </ModalWrapper>
        </FormModalLifter>
      </DimmedModal>
    </FocusLock>
  )
}

export default FormModal
