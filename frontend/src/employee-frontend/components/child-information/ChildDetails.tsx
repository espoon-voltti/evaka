// SPDX-FileCopyrightText: 2017-2020 City of Espoo
//
// SPDX-License-Identifier: LGPL-2.1-or-later

import React, {useContext, useEffect, useRef, useState} from 'react'
import ReactCrop from 'react-image-crop';
import { useTranslation } from '../../state/i18n'
import {Success} from 'lib-common/api'
import { UUID } from '../../types'
import AdditionalInformation from '../../components/child-information/person-details/AdditionalInformation'
import { ChildContext, ChildState } from '../../state/child'
import PersonDetails from '../../components/person-shared/PersonDetails'
import { CollapsibleContentArea } from '../../../lib-components/layout/Container'
import { H2 } from '../../../lib-components/typography'
import { Gap } from 'lib-components/white-space'
import {client} from "../../api/client";

import 'react-image-crop/dist/ReactCrop.css';
import Button from "../../../lib-components/atoms/buttons/Button";
import { FixedSpaceRow } from 'lib-components/layout/flex-helpers';

interface Props {
  id: UUID
}

export async function uploadImage(
  childId: UUID,
  file: File
): Promise<null> {
  const formData = new FormData()
  formData.append('file', file)

  return client.put(
    `/children/${childId}/image`,
    formData,
    {
      headers: {
        'Content-Type': 'multipart/form-data'
      }
    }
  )
}

const defaultCrop: ReactCrop.Crop = { unit: '%', width: 80, x: 10, y: 10, aspect: 1 }

const ChildDetails = React.memo(function ChildDetails({ id }: Props) {
  const { i18n } = useTranslation()
  const { person, setPerson } = useContext<ChildState>(ChildContext)

  const [open, setOpen] = useState(true)

  const [image, setImage] = useState<string | null>(null)
  const [crop, setCrop] = useState<ReactCrop.Crop>(defaultCrop);
  const [completedCrop, setCompletedCrop] = useState<ReactCrop.Crop | null>(null);
  const [imageElem, setImageElem] = useState<HTMLImageElement | null>(null)
  const previewCanvasRef = useRef<HTMLCanvasElement>(null)

  const onChange = (childId: string, event: React.ChangeEvent<HTMLInputElement>) => {
    if (event.target.files && event.target.files.length > 0) {

      const reader = new FileReader();
      reader.addEventListener('load', () => {
        if(typeof reader.result === 'string') {
          setImage(reader.result)
        }
      });
      reader.readAsDataURL(event.target.files[0]);

      if (event.target.value) event.target.value = ''
    }
  }


  const reset = () => {
    setImage(null)
    setCrop(defaultCrop)
    setCompletedCrop(null)
  }


  const onSave = () => {
    if (!crop || !previewCanvasRef.current) {
      return;
    }

    previewCanvasRef.current.toBlob(
      (blob) => {
        if(blob) {
          console.log(blob.type)
          const file = new File([blob], 'cropped-image.jpeg', { type: blob.type })
          void uploadImage(id, file).then(reset)
        }
      },
      'image/jpeg',
      1
    )

  }

  useEffect(() => {
    if (!completedCrop || !previewCanvasRef.current || !imageElem) {
      return;
    }

    const canvas = previewCanvasRef.current;
    const crop = completedCrop;

    const scaleX = imageElem.naturalWidth / imageElem.width;
    const scaleY = imageElem.naturalHeight / imageElem.height;
    const ctx = canvas.getContext('2d');
    const pixelRatio = window.devicePixelRatio;

    canvas.width = (crop.width ?? 0) * pixelRatio;
    canvas.height = (crop.height ?? 0) * pixelRatio;

    if(ctx) {
      ctx.setTransform(pixelRatio, 0, 0, pixelRatio, 0, 0);
      ctx.imageSmoothingQuality = 'high';

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
      );
    }
  }, [completedCrop]);

  return (
    <div className="person-details-section">
      <CollapsibleContentArea
        title={<H2 noMargin>{i18n.childInformation.personDetails.title}</H2>}
        open={open}
        toggleOpen={() => setOpen(!open)}
        opaque
        paddingVertical="L"
      >
        <PersonDetails
          personResult={person}
          isChild={true}
          onUpdateComplete={(p) => setPerson(Success.of(p))}
        />
        <div className="additional-information">
          <AdditionalInformation id={id} />
        </div>
        <Gap/>
        {!image && (
          <input
            type="file"
            accept="image/jpeg, image/png"
            onChange={e => onChange(id, e)}
          />
        )}
        <Gap/>
        { image && (
          <>
            <div style={{width: '500px'}}>
              <ReactCrop
                src={image}
                crop={crop}
                onImageLoaded={setImageElem}
                onChange={(c) => setCrop(c)}
                onComplete={setCompletedCrop}
                circularCrop
              />
            </div>
            <Gap/>
            <div>
              <canvas
                ref={previewCanvasRef}
                style={{
                  width: 256,
                  height: 256,
                  display: 'none'
                }}
              />
            </div>
            <Gap/>
            <FixedSpaceRow>
              <Button
                text={'Peruuta'}
                onClick={reset}
              />
              <Button
                text={'Tallenna'}
                primary
                disabled={!completedCrop}
                onClick={onSave}
              />
            </FixedSpaceRow>
          </>
        )}
      </CollapsibleContentArea>
    </div>
  )
})

export default ChildDetails
