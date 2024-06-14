// SPDX-FileCopyrightText: 2017-2022 City of Espoo
//
// SPDX-License-Identifier: LGPL-2.1-or-later

import React, { SyntheticEvent, useCallback, useEffect, useState } from 'react'
import ReactCrop, { centerCrop, Crop, makeAspectCrop } from 'react-image-crop'
import { useNavigate } from 'react-router-dom'
import styled from 'styled-components'

import { useMutationResult } from 'lib-common/query'
import { UUID } from 'lib-common/types'
import { AsyncButton } from 'lib-components/atoms/buttons/AsyncButton'
import { LegacyButton } from 'lib-components/atoms/buttons/LegacyButton'
import StickyFooter from 'lib-components/layout/StickyFooter'
import { FixedSpaceRow } from 'lib-components/layout/flex-helpers'
import { InformationText } from 'lib-components/typography'
import { defaultMargins, Gap } from 'lib-components/white-space'

import { uploadChildImageMutation } from '../child-attendance/queries'
import { useTranslation } from '../common/i18n'

import 'react-image-crop/dist/ReactCrop.css'

interface Props {
  unitId: UUID
  childId: UUID
  image: string
  onReturn: () => void
}

export default React.memo(function ImageEditor({
  unitId,
  childId,
  image,
  onReturn
}: Props) {
  const { i18n } = useTranslation()
  const [crop, setCrop] = useState<Crop>()
  const [imageElem, setImageElem] = useState<HTMLImageElement | null>(null)
  const [submitting, setSubmitting] = useState(false)
  const navigate = useNavigate()

  const { mutateAsync: uploadChildImage } = useMutationResult(
    uploadChildImageMutation
  )

  useEffect(() => {
    const htmlNode = document.querySelector('html')
    if (htmlNode) {
      htmlNode.style.overscrollBehavior = 'none'
    }

    const popStateHandler = (e: PopStateEvent) => {
      e.preventDefault()
      navigate(1)
      onReturn()
    }

    window.addEventListener('popstate', popStateHandler)

    return () => {
      if (htmlNode) {
        htmlNode.style.overscrollBehavior = 'auto'
      }
      window.removeEventListener('popstate', popStateHandler)
    }
  }, [onReturn, navigate])

  const onImageLoad = useCallback((e: SyntheticEvent<HTMLImageElement>) => {
    const image = e.currentTarget
    setImageElem(image)
    setCrop(
      centerCrop(
        makeAspectCrop(
          {
            // You don't need to pass a complete crop into
            // makeAspectCrop or centerCrop.
            unit: '%',
            width: 90
          },
          1,
          image.naturalWidth,
          image.naturalHeight
        ),
        image.naturalWidth,
        image.naturalHeight
      )
    )
  }, [])

  const onSave = useCallback(() => {
    if (!imageElem || !crop) return
    return cropImage(imageElem, crop).then((file) =>
      uploadChildImage({ unitId, childId, file })
    )
  }, [childId, crop, imageElem, unitId, uploadChildImage])

  const onSuccess = useCallback(() => {
    setSubmitting(false)
    onReturn()
  }, [onReturn])

  return (
    <div>
      <CropContainer>
        <CropWrapper>
          <ReactCrop
            aspect={1}
            crop={crop}
            onChange={(c) => setCrop(c)}
            circularCrop
          >
            <img src={image} onLoad={onImageLoad} alt="" />
          </ReactCrop>
        </CropWrapper>
      </CropContainer>
      <Gap />
      <StickyFooter>
        <FooterContainer>
          <InformationText centered>
            {i18n.childInfo.image.modalMenu.disclaimer}
          </InformationText>

          <ButtonRow>
            <LegacyButton
              text={i18n.common.cancel}
              onClick={onReturn}
              disabled={submitting}
            />
            <AsyncButton
              text={i18n.common.save}
              primary
              disabled={crop === undefined}
              onClick={onSave}
              onSuccess={onSuccess}
            />
          </ButtonRow>
        </FooterContainer>
      </StickyFooter>
    </div>
  )
})

const CropContainer = styled.div`
  display: flex;
  flex-direction: column;
  justify-content: space-between;
  align-items: stretch;
  margin: ${defaultMargins.m};
`

const FooterContainer = styled.div`
  margin: ${defaultMargins.m};
`

const ButtonRow = styled(FixedSpaceRow)`
  width: 100%;
  justify-content: space-around;
  padding: ${defaultMargins.s};
`

const CropWrapper = styled.div`
  flex-grow: 1;
  flex-shrink: 1;

  .ReactCrop .ord-nw {
    top: -24px;
    left: -24px;
  }

  .ReactCrop .ord-nw::after {
    top: 24px;
    left: 24px;
  }

  .ReactCrop .ord-ne {
    top: -24px;
    right: -24px;
  }

  .ReactCrop .ord-ne::after {
    top: 24px;
    right: 24px;
  }

  .ReactCrop .ord-se {
    bottom: -24px;
    right: -24px;
  }

  .ReactCrop .ord-se::after {
    bottom: 24px;
    right: 24px;
  }

  .ReactCrop .ord-sw {
    bottom: -24px;
    left: -24px;
  }

  .ReactCrop .ord-sw::after {
    bottom: 24px;
    left: 24px;
  }

  .ReactCrop__drag-handle {
    width: 64px;
    height: 64px;
  }
`

function cropImage(image: HTMLImageElement, crop: Crop): Promise<File> {
  let canvas = document.createElement('canvas')
  const ctx = canvas.getContext('2d')
  if (!ctx) {
    throw new Error('Could not get canvas context')
  }

  const scaleX = image.naturalWidth / image.width
  const scaleY = image.naturalHeight / image.height
  const pixelRatio = window.devicePixelRatio

  canvas.width = crop.width * pixelRatio
  canvas.height = crop.height * pixelRatio

  ctx.setTransform(pixelRatio, 0, 0, pixelRatio, 0, 0)
  ctx.imageSmoothingQuality = 'high'

  ctx.drawImage(
    image,
    crop.x * scaleX,
    crop.y * scaleY,
    crop.width * scaleX,
    crop.height * scaleY,
    0,
    0,
    crop.width,
    crop.height
  )

  if (canvas.width > 512 || canvas.height > 512) {
    const resizedCanvas = document.createElement('canvas')
    resizedCanvas.width = Math.min((canvas.width / canvas.height) * 512, 512)
    resizedCanvas.height = Math.min((canvas.height / canvas.width) * 512, 512)
    resizedCanvas
      .getContext('2d')
      ?.drawImage(canvas, 0, 0, resizedCanvas.width, resizedCanvas.height)
    canvas = resizedCanvas
  }

  return new Promise<File>((resolve, reject) =>
    canvas.toBlob(
      (blob) => {
        if (blob) {
          resolve(
            new File([blob], 'cropped-image.jpeg', {
              type: blob.type
            })
          )
        } else {
          reject(new Error('Could not convert canvas to blob'))
        }
      },
      'image/jpeg',
      0.7
    )
  )
}
