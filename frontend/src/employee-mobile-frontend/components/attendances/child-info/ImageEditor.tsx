{
  /*
SPDX-FileCopyrightText: 2017-2021 City of Espoo

SPDX-License-Identifier: LGPL-2.1-or-later
*/
}

import React, { useEffect, useRef, useState } from 'react'
import ReactCrop from 'react-image-crop'
import { defaultMargins, Gap } from '../../../../lib-components/white-space'
import { FixedSpaceRow } from '../../../../lib-components/layout/flex-helpers'
import Button from '../../../../lib-components/atoms/buttons/Button'
import { uploadChildImage } from '../../../api/childImages'

import 'react-image-crop/dist/ReactCrop.css'
import styled from 'styled-components'
import { useHistory } from 'react-router-dom'
import { useTranslation } from '../../../state/i18n'

const defaultCrop: ReactCrop.Crop = {
  unit: '%',
  width: 80,
  x: 10,
  y: 10,
  aspect: 1
}

interface Props {
  childId: string
  image: string
  onReturn: () => void
}
export default React.memo(function ImageEditor({
  childId,
  image,
  onReturn
}: Props) {
  const { i18n } = useTranslation()
  const [crop, setCrop] = useState<ReactCrop.Crop>(defaultCrop)
  const [completedCrop, setCompletedCrop] = useState<ReactCrop.Crop | null>(
    null
  )
  const [imageElem, setImageElem] = useState<HTMLImageElement | null>(null)
  const previewCanvasRef = useRef<HTMLCanvasElement>(null)
  const [submitting, setSubmitting] = useState(false)
  const h = useHistory()

  useEffect(() => {
    const htmlNode = document.querySelector('html')
    if (htmlNode) {
      htmlNode.style.overscrollBehavior = 'none'
    }

    const popStateHandler = (e: PopStateEvent) => {
      e.preventDefault()
      h.goForward()
      onReturn()
    }

    window.addEventListener('popstate', popStateHandler)

    return () => {
      if (htmlNode) {
        htmlNode.style.overscrollBehavior = 'auto'
      }
      window.removeEventListener('popstate', popStateHandler)
    }
  }, [onReturn, h])

  const onSave = () => {
    if (!crop || !previewCanvasRef.current) {
      return
    }

    previewCanvasRef.current.toBlob(
      (blob) => {
        if (blob) {
          const file = new File([blob], 'cropped-image.jpeg', {
            type: blob.type
          })
          setSubmitting(true)
          void uploadChildImage(childId, file).then((res) => {
            setSubmitting(false)
            if (res.isFailure) {
              console.error('Uploading image failed', res.message)
            } else {
              onReturn()
            }
          })
        }
      },
      'image/jpeg',
      1
    )
  }

  useEffect(() => {
    if (!completedCrop || !previewCanvasRef.current || !imageElem) {
      return
    }

    const canvas = previewCanvasRef.current
    const crop = completedCrop

    const scaleX = imageElem.naturalWidth / imageElem.width
    const scaleY = imageElem.naturalHeight / imageElem.height
    const ctx = canvas.getContext('2d')
    const pixelRatio = window.devicePixelRatio

    canvas.width = (crop.width ?? 0) * pixelRatio
    canvas.height = (crop.height ?? 0) * pixelRatio

    if (ctx) {
      ctx.setTransform(pixelRatio, 0, 0, pixelRatio, 0, 0)
      ctx.imageSmoothingQuality = 'high'

      ctx.drawImage(
        imageElem,
        (crop.x ?? 0) * scaleX,
        (crop.y ?? 0) * scaleY,
        (crop.width ?? 0) * scaleX,
        (crop.height ?? 0) * scaleY,
        0,
        0,
        crop.width ?? 0,
        crop.height ?? 0
      )
    }
  }, [imageElem, completedCrop])

  return (
    <Container>
      <Gap />

      <div style={{ flexGrow: 1, flexShrink: 1, height: '100px' }}>
        <ReactCrop
          src={image}
          crop={crop}
          onImageLoaded={setImageElem}
          onChange={(c) => setCrop(c)}
          onComplete={setCompletedCrop}
          circularCrop
          style={{ maxHeight: '100%' }}
        />
        <canvas
          ref={previewCanvasRef}
          style={{
            width: 256,
            height: 256,
            display: 'none'
          }}
        />
      </div>

      <Gap />

      <ButtonRow>
        <Button
          text={i18n.common.cancel}
          onClick={onReturn}
          disabled={submitting}
        />
        <Button
          text={i18n.common.save}
          primary
          disabled={!completedCrop || submitting}
          onClick={onSave}
        />
      </ButtonRow>
    </Container>
  )
})

const Container = styled.div`
  display: flex;
  flex-direction: column;
  justify-content: space-between;
  align-items: stretch;
  height: 100%;
  max-height: 100%;
  overflow: hidden;
`

const ButtonRow = styled(FixedSpaceRow)`
  width: 100%;
  justify-content: space-around;
  padding: ${defaultMargins.s};
`
