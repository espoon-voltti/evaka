// SPDX-FileCopyrightText: 2017-2021 City of Espoo
//
// SPDX-License-Identifier: LGPL-2.1-or-later

import { IconDefinition } from '@fortawesome/fontawesome-svg-core'
import { FontAwesomeIcon } from '@fortawesome/react-fontawesome'
import React from 'react'
import styled from 'styled-components'
import { desktopMin } from '../breakpoints'
import { diameterByIconSize, fontSizeByIconSize, IconSize } from './icon-size'

interface SizeProps {
  size: IconSize
  sizeDesktop?: IconSize
}

const ImageContainer = styled.div<SizeProps>`
  display: flex;
  justify-content: center;
  align-items: center;
  border-radius: 50%;

  background-color: ${(p) => p.theme.colors.greyscale.white};
  color: ${(p) => p.color};

  font-size: ${(p) => fontSizeByIconSize(p.size)}px;
  width: ${(p) => diameterByIconSize(p.size)}px;
  height: ${(p) => diameterByIconSize(p.size)}px;
  @media (min-width: ${desktopMin}) {
    font-size: ${(p) => fontSizeByIconSize(p.sizeDesktop ?? p.size)}px;
    width: ${(p) => diameterByIconSize(p.sizeDesktop ?? p.size)}px;
    height: ${(p) => diameterByIconSize(p.sizeDesktop ?? p.size)}px;
  }
`

const Image = styled.img<SizeProps>`
  display: block;
  border-radius: 50%;
  width: ${(p) => diameterByIconSize(p.size)}px;
  height: ${(p) => diameterByIconSize(p.size)}px;
  @media (min-width: ${desktopMin}) {
    width: ${(p) => diameterByIconSize(p.sizeDesktop ?? p.size)}px;
    height: ${(p) => diameterByIconSize(p.sizeDesktop ?? p.size)}px;
  }
`

interface Props {
  src?: string | null
  fallbackContent: IconDefinition
  fallbackColor: string
  size: IconSize
  sizeDesktop?: IconSize
}

export const RoundImage = React.memo(function RoundImage({
  fallbackColor,
  fallbackContent,
  size,
  sizeDesktop,
  src
}: Props) {
  return (
    <ImageContainer size={size} sizeDesktop={sizeDesktop} color={fallbackColor}>
      {src ? (
        <Image size={size} sizeDesktop={sizeDesktop} src={src} />
      ) : (
        <FontAwesomeIcon icon={fallbackContent} />
      )}
    </ImageContainer>
  )
})
