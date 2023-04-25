// SPDX-FileCopyrightText: 2017-2022 City of Espoo
//
// SPDX-License-Identifier: LGPL-2.1-or-later

import type { IconDefinition } from '@fortawesome/fontawesome-svg-core'
import { FontAwesomeIcon } from '@fortawesome/react-fontawesome'
import React from 'react'
import styled from 'styled-components'

import { desktopMin } from '../breakpoints'

import type { IconSize } from './icon-size'
import { diameterByIconSize, fontSizeByIconSize } from './icon-size'

interface SizeProps {
  size: IconSize
  sizeDesktop?: IconSize
}

const ImageContainer = styled.div<SizeProps>`
  display: flex;
  justify-content: center;
  align-items: center;
  border-radius: 50%;

  background-color: ${(p) => p.theme.colors.grayscale.g0};
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
  alt?: string
  className?: string
}

export const RoundImage = React.memo(function RoundImage({
  fallbackColor,
  fallbackContent,
  size,
  sizeDesktop,
  src,
  alt,
  className
}: Props) {
  return (
    <ImageContainer
      size={size}
      sizeDesktop={sizeDesktop}
      color={fallbackColor}
      className={className}
    >
      {src ? (
        <Image size={size} sizeDesktop={sizeDesktop} src={src} alt={alt} />
      ) : (
        <FontAwesomeIcon icon={fallbackContent} />
      )}
    </ImageContainer>
  )
})
