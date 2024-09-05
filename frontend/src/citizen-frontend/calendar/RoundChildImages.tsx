// SPDX-FileCopyrightText: 2017-2022 City of Espoo
//
// SPDX-License-Identifier: LGPL-2.1-or-later

import take from 'lodash/take'
import React, { useMemo } from 'react'
import styled, { css } from 'styled-components'

import { ReservationChild } from 'lib-common/generated/api-types/reservations'
import { formatFirstName } from 'lib-common/names'
import { UUID } from 'lib-common/types'
import { fontWeights } from 'lib-components/typography'
import { theme } from 'lib-customizations/common'

import { API_URL } from '../api-client'

export interface ChildImageData {
  childId: UUID
  imageId: UUID | null
  initialLetter: string
  colorIndex: number
}

export interface Props {
  images: ChildImageData[]
}

export default React.memo(function RoundChildImages({ images }: Props) {
  const imageSize = 32
  const imageBorder = 2
  const imageOverlap = 16
  const maxImages = 4

  const imagesToShow = useMemo(
    () => (images.length <= maxImages ? images : take(images, maxImages - 1)),
    [images]
  )
  return (
    <RoundChildImagesContainer borderSize={imageBorder}>
      {imagesToShow.map((image, index) => (
        <Overlap
          key={image.childId}
          overlap={imageOverlap}
          index={index}
          data-qa="child-image"
          data-qa-child-id={image.childId}
        >
          <RoundChildImage
            imageId={image.imageId}
            fallbackText={image.initialLetter}
            colorIndex={image.colorIndex}
            size={imageSize}
            border={imageBorder}
          />
        </Overlap>
      ))}
      {images.length > maxImages ? (
        <Overlap overlap={imageOverlap} index={imagesToShow.length}>
          <ChildImageFallback
            textColor={theme.colors.grayscale.g0}
            backgroundColor={theme.colors.main.m2}
            size={imageSize}
            border={imageBorder}
          >
            +{images.length - maxImages + 1}
          </ChildImageFallback>
        </Overlap>
      ) : null}
    </RoundChildImagesContainer>
  )
})

const RoundChildImagesContainer = styled.div<{ borderSize: number }>`
  display: flex;
  margin-left: -${(p) => p.borderSize}px;
`

const Overlap = styled.div<{ overlap: number; index: number }>`
  flex: 0 0 auto;
  margin-left: ${(p) => (p.index > 0 ? -p.overlap : 0)}px;
`

export interface RoundChildImageProps {
  imageId: UUID | null
  fallbackText: string
  colorIndex: number
  size: number
  border?: number
}

export const RoundChildImage = React.memo(function RoundChildImage({
  imageId,
  fallbackText,
  colorIndex,
  size,
  border = 0
}: RoundChildImageProps) {
  return imageId !== null ? (
    <ChildImage
      src={`${API_URL}/citizen/child-images/${imageId}`}
      size={size}
      border={border}
    />
  ) : (
    <ChildImageFallback
      textColor={theme.colors.grayscale.g100}
      backgroundColor={accentColors[colorIndex % accentColors.length]}
      size={size}
      border={border}
    >
      {fallbackText}
    </ChildImageFallback>
  )
})

const roundMixin = css<{ size: number; border: number }>`
  width: ${(p) => p.size}px;
  height: ${(p) => p.size}px;
  ${(p) => (p.border > 0 ? `border: ${p.border}px solid #fff;` : undefined)};
  border-radius: 50%;
`

const ChildImage = styled.img<{ size: number; border: number }>`
  ${roundMixin};
`

const accentColors = [
  theme.colors.accents.a6turquoise,
  theme.colors.accents.a9pink,
  theme.colors.accents.a7mint,
  theme.colors.accents.a5orangeLight
]

const ChildImageFallback = styled.div<{
  size: number
  border: number
  textColor: string
  backgroundColor: string
}>`
  ${roundMixin};
  color: ${(p) => p.textColor ?? p.theme.colors.grayscale.g100};
  background-color: ${(p) => p.backgroundColor};
  font-weight: ${fontWeights.semibold};
  display: flex;
  justify-content: center;
  align-items: center;
`

export const getChildImages = (
  childData: ReservationChild[]
): ChildImageData[] =>
  childData.map((child, index) => ({
    childId: child.id,
    imageId: child.imageId,
    initialLetter: (formatFirstName(child) || '?')[0],
    colorIndex: index,
    childName: child.firstName
  }))
