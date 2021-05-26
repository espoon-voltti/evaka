// SPDX-FileCopyrightText: 2017-2020 City of Espoo
//
// SPDX-License-Identifier: LGPL-2.1-or-later

import React from 'react'
import Checkbox from 'lib-components/atoms/form/Checkbox'
import {
  FixedSpaceColumn,
  FixedSpaceRow
} from 'lib-components/layout/flex-helpers'
import { useTranslation } from '../../../localization'
import { H3, Label, P } from 'lib-components/typography'
import InputField from 'lib-components/atoms/form/InputField'
import { Gap } from 'lib-components/white-space'
import FileUpload from 'lib-components/molecules/FileUpload'
import { deleteAttachment, getAttachmentBlob, saveAttachment } from '../../api'
import { Result } from 'lib-common/api'
import { UUID } from 'lib-common/types'
import { useParams } from 'react-router-dom'
import styled from 'styled-components'
import { errorToInputInfo } from '../../../form-validation'
import { ServiceNeedSectionProps } from './ServiceNeedSection'
import ExpandingInfo from 'lib-components/molecules/ExpandingInfo'

const Hyphenbox = styled.div`
  display: flex;
  flex-direction: column;
  justify-content: flex-end;
`

type ServiceTimeSubSectionProps = Omit<ServiceNeedSectionProps, 'type'>

const applicationType = 'PRESCHOOL'

export default React.memo(function ServiceTimeSubSectionPreschool({
  formData,
  updateFormData,
  errors,
  verificationRequested
}: ServiceTimeSubSectionProps) {
  const t = useTranslation()
  const { applicationId } = useParams<{ applicationId: string }>()

  const uploadExtendedCareAttachment = (
    file: File,
    onUploadProgress: (progressEvent: ProgressEvent) => void
  ): Promise<Result<UUID>> =>
    saveAttachment(applicationId, file, 'EXTENDED_CARE', onUploadProgress).then(
      (result) => {
        result.isSuccess &&
          updateFormData({
            shiftCareAttachments: [
              ...formData.shiftCareAttachments,
              {
                id: result.value,
                name: file.name,
                contentType: file.type,
                updated: new Date(),
                receivedAt: new Date(),
                type: 'EXTENDED_CARE'
              }
            ]
          })
        return result
      }
    )

  const deleteExtendedCareAttachment = (id: UUID) =>
    deleteAttachment(id).then((result) => {
      result.isSuccess &&
        updateFormData({
          shiftCareAttachments: formData.shiftCareAttachments.filter(
            (file) => file.id !== id
          )
        })
      return result
    })

  return (
    <>
      <H3>
        {t.applications.editor.serviceNeed.dailyTime.label[applicationType]}
      </H3>

      {t.applications.editor.serviceNeed.dailyTime.connectedDaycareInfo()}

      <Checkbox
        checked={formData.connectedDaycare}
        data-qa={'connectedDaycare-input'}
        label={t.applications.editor.serviceNeed.dailyTime.connectedDaycare}
        onChange={(checked) =>
          updateFormData({
            connectedDaycare: checked
          })
        }
      />

      {formData.connectedDaycare && (
        <>
          <Gap size={'m'} />

          <ExpandingInfo
            info={
              t.applications.editor.serviceNeed.dailyTime.instructions[
                applicationType
              ]
            }
            ariaLabel={t.common.openExpandingInfo}
          >
            <Label>
              {t.applications.editor.serviceNeed.dailyTime
                .usualArrivalAndDeparture[applicationType] + ' *'}
            </Label>
          </ExpandingInfo>

          <Gap size={'s'} />

          <FixedSpaceRow spacing={'m'}>
            <FixedSpaceColumn spacing={'xs'}>
              <Label htmlFor={'daily-time-starts'}>
                {t.applications.editor.serviceNeed.dailyTime.starts}
              </Label>
              <InputField
                id={'daily-time-starts'}
                type={'time'}
                value={formData.startTime}
                data-qa={'startTime-input'}
                onChange={(value) => updateFormData({ startTime: value })}
                width={'s'}
                info={errorToInputInfo(errors.startTime, t.validationErrors)}
                hideErrorsBeforeTouched={!verificationRequested}
              />
            </FixedSpaceColumn>

            <Hyphenbox>-</Hyphenbox>

            <FixedSpaceColumn spacing={'xs'}>
              <Label htmlFor={'daily-time-ends'}>
                {t.applications.editor.serviceNeed.dailyTime.ends}
              </Label>
              <InputField
                id={'daily-time-ends'}
                type={'time'}
                value={formData.endTime}
                data-qa={'endTime-input'}
                onChange={(value) => updateFormData({ endTime: value })}
                width={'s'}
                info={errorToInputInfo(errors.endTime, t.validationErrors)}
                hideErrorsBeforeTouched={!verificationRequested}
              />
            </FixedSpaceColumn>
          </FixedSpaceRow>

          <Gap size={'L'} />

          <ExpandingInfo
            info={t.applications.editor.serviceNeed.shiftCare.instructions}
            ariaLabel={t.common.openExpandingInfo}
            margin="xs"
          >
            <Checkbox
              checked={formData.shiftCare}
              data-qa={'shiftCare-input'}
              label={t.applications.editor.serviceNeed.shiftCare.label}
              onChange={(checked) =>
                updateFormData({
                  shiftCare: checked
                })
              }
            />
          </ExpandingInfo>

          {formData.shiftCare && (
            <>
              <Gap size={'s'} />

              <P fitted>
                {
                  t.applications.editor.serviceNeed.shiftCare.attachmentsMessage
                    .text
                }
              </P>

              <Gap size={'s'} />

              <strong>
                {
                  t.applications.editor.serviceNeed.shiftCare.attachmentsMessage
                    .subtitle
                }
              </strong>

              <Gap size={'s'} />

              <FileUpload
                files={formData.shiftCareAttachments}
                onUpload={uploadExtendedCareAttachment}
                onDelete={deleteExtendedCareAttachment}
                onDownloadFile={getAttachmentBlob}
                i18n={{ upload: t.fileUpload, download: t.fileDownload }}
              />
            </>
          )}
        </>
      )}
    </>
  )
})
