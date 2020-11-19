import styled from 'styled-components'

import AsyncButton from '~components/shared/atoms/buttons/AsyncButton'
import Button from '~components/shared/atoms/buttons/Button'
import InlineButton from '~components/shared/atoms/buttons/InlineButton'
import { DefaultMargins } from '~components/shared/layout/white-space'
import { CustomButtonProps } from './AttendanceGroupSelectorPage'

export const CustomAsyncButton = styled(AsyncButton)<CustomButtonProps>`
  @media screen and (max-width: 1023px) {
    margin-bottom: ${DefaultMargins.s};
    width: calc(50vw - 40px);
    white-space: normal;
    height: 64px;
  }

  @media screen and (min-width: 1024px) {
    margin-right: ${DefaultMargins.s};
  }
  ${(p) => (p.color ? `color: ${p.color};` : '')}
  ${(p) => (p.backgroundColor ? `background-color: ${p.backgroundColor};` : '')}
  ${(p) => (p.borderColor ? `border-color: ${p.borderColor};` : '')}

  :hover {
    ${(p) => (p.color ? `color: ${p.color};` : '')}
    ${(p) =>
      p.backgroundColor ? `background-color: ${p.backgroundColor};` : ''}
  ${(p) => (p.borderColor ? `border-color: ${p.borderColor};` : '')}
  }
`

export const WideButton = styled(Button)`
  width: 100%;
`

export const BigWideButton = styled(WideButton)`
  white-space: normal;
  height: 64px;
`

export const BigWideInlineButton = styled(InlineButton)`
  white-space: normal;
  height: 64px;
  width: 100%;
`

export const WideAsyncButton = styled(AsyncButton)`
  @media screen and (max-width: 1023px) {
    margin-bottom: ${DefaultMargins.s};
    width: 100%;
    white-space: normal;
    height: 64px;
  }
`

export const InlineWideAsyncButton = styled(WideAsyncButton)`
  border: none;
`
