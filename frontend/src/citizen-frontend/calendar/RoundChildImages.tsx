// SPDX-FileCopyrightText: 2017-2022 City of Espoo
//
// SPDX-License-Identifier: LGPL-2.1-or-later

import React from 'react'
import styled, { css } from 'styled-components'

import {
  DailyReservationData,
  ReservationChild
} from 'lib-common/generated/api-types/reservations'
import { UUID } from 'lib-common/types'
import { fontWeights } from 'lib-components/typography'
import { theme } from 'lib-customizations/common'

export interface ChildImageData {
  childId: UUID
  imageId: UUID | null
  initialLetter: string
  colorIndex: number
}

export interface Props {
  images: ChildImageData[]
  imageSize: number
  imageBorder: number
  imageOverlap: number
}

export default React.memo(function RoundChildImages({
  images,
  imageSize,
  imageBorder,
  imageOverlap
}: Props) {
  return (
    <RoundChildImagesContainer>
      {images.map((image, index) => (
        <Overlap key={image.childId} overlap={imageOverlap} index={index}>
          <RoundChildImage
            imageId={image.imageId}
            initialLetter={image.initialLetter}
            colorIndex={image.colorIndex}
            size={imageSize}
            border={imageBorder}
          />
        </Overlap>
      ))}
    </RoundChildImagesContainer>
  )
})

const RoundChildImagesContainer = styled.div`
  display: flex;
`

const Overlap = styled.div<{ overlap: number; index: number }>`
  flex: 0 0 auto;
  margin-left: ${(p) => (p.index > 0 ? -p.overlap : 0)}px;
`

export interface RoundChildImageProps {
  imageId: UUID | null
  initialLetter: string
  colorIndex: number
  size: number
  border?: number
}

export const RoundChildImage = React.memo(function RoundChildImage({
  imageId,
  initialLetter,
  colorIndex,
  size,
  border = 0
}: RoundChildImageProps) {
  return imageId !== null ? (
    <ChildImage
      src={`/api/application/citizen/child-images/${imageId}`}
      size={size}
      border={border}
    />
  ) : (
    <ChildInitialLetter colorIndex={colorIndex} size={size} border={border}>
      {initialLetter}
    </ChildInitialLetter>
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

const ChildInitialLetter = styled.div<{
  size: number
  border: number
  colorIndex: number
}>`
  ${roundMixin};
  background-color: ${(p) => accentColors[p.colorIndex % accentColors.length]};
  font-weight: ${fontWeights.semibold};
  display: flex;
  justify-content: center;
  align-items: center;
`

export function getPresentChildImages(
  childData: ReservationChild[],
  day: DailyReservationData
): ChildImageData[] {
  return childData
    .map((child, index) => [child, index] as const)
    .filter(([child, _]) =>
      day.children.some(
        (dc) =>
          dc.childId === child.id &&
          dc.absence === null &&
          (dc.reservations.length > 0 || dc.attendances.length > 0)
      )
    )
    .map(([child, index]) => ({
      childId: child.id,
      imageId: child.imageId,
      initialLetter: (child.preferredName || child.firstName || '?')[0],
      colorIndex: index
    }))
}
