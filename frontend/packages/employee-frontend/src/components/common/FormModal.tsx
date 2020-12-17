// SPDX-FileCopyrightText: 2017-2020 City of Espoo
//
// SPDX-License-Identifier: LGPL-2.1-or-later

import React from 'react'
import { FontAwesomeIcon } from '@fortawesome/react-fontawesome'
import { IconProp } from '@fortawesome/fontawesome-svg-core'
import styled from 'styled-components'
import FocusLock from 'react-focus-lock'

import Title from '@evaka/lib-components/src/atoms/Title'
import Button from '@evaka/lib-components/src/atoms/buttons/Button'
import AsyncButton from '@evaka/lib-components/src/atoms/buttons/AsyncButton'
import colors from '@evaka/lib-components/src/colors'
import { defaultMargins, Gap } from '@evaka/lib-components/src/white-space'
import { useTranslation } from '~state/i18n'
import { modalZIndex } from '~components/shared/layout/z-helpers'
import { InfoStatus } from '@evaka/lib-components/src/atoms/StatusIcon'
import { P } from '@evaka/lib-components/src/typography'

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
  padding-left: ${defaultMargins.XXL};
  padding-right: ${defaultMargins.XXL};
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
        return colors.blues.medium
      case 'orange':
        return colors.accents.orange
      case 'green':
        return colors.accents.green
      case 'red':
        return colors.accents.red
      default:
        return colors.blues.medium
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
  resolveInfo?: {
    text: string
    status?: InfoStatus
  }
}

function ModalBase({
  'data-qa': dataQa,
  title,
  text,
  className,
  icon,
  size = 'md',
  resolveDisabled = false,
  children,
  iconColour = 'blue',
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
                    if (!resolveDisabled) onSubmit()
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

type FormModalProps = CommonProps & {
  resolve: () => void
  resolveLabel?: string
  resolveDisabled?: boolean
  resolveInfo?: {
    text: string
    status?: InfoStatus
  }
  reject?: () => void
  rejectLabel?: string
}

export default React.memo(function FormModal({
  children,
  reject,
  rejectLabel,
  resolve,
  resolveLabel,
  resolveDisabled,
  resolveInfo,
  ...props
}: FormModalProps) {
  const { i18n } = useTranslation()
  return (
    <ModalBase {...props} onSubmit={resolve}>
      {children}
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
          info={resolveInfo}
          dataQa="modal-okBtn"
          onClick={resolve}
          disabled={resolveDisabled}
          text={resolveLabel ?? i18n.common.confirm}
        />
      </ModalButtons>
    </ModalBase>
  )
})

type AsyncModalProps = CommonProps & {
  resolve: () => Promise<void>
  onResolveSuccess: () => void
  resolveLabel?: string
  resolveDisabled?: boolean
  reject?: () => void
  rejectLabel?: string
}

export const AsyncFormModal = React.memo(function AsyncFormModal({
  children,
  reject,
  rejectLabel,
  resolve,
  resolveLabel,
  resolveDisabled,
  onResolveSuccess,
  ...props
}: AsyncModalProps) {
  const { i18n } = useTranslation()
  return (
    <ModalBase {...props}>
      {children}
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
        <AsyncButton
          primary
          text={resolveLabel ?? i18n.common.confirm}
          disabled={resolveDisabled}
          onClick={resolve}
          onSuccess={onResolveSuccess}
          data-qa="modal-okBtn"
        />
      </ModalButtons>
    </ModalBase>
  )
})
