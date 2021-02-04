// SPDX-FileCopyrightText: 2017-2020 City of Espoo
//
// SPDX-License-Identifier: LGPL-2.1-or-later

import React from 'react'
import Checkbox from '@evaka/lib-components/src/atoms/form/Checkbox'
import { useLang, useTranslation } from '~localization'
import { H3, Label, P } from '@evaka/lib-components/src/typography'
import { Gap } from '@evaka/lib-components/src/white-space'
import FileUpload from '../FileUpload'
import { deleteAttachment, saveAttachment } from '~applications/api'
import { Result } from '~../../lib-common/src/api'
import { UUID } from '~../../lib-common/src/types'
import { useParams } from 'react-router-dom'
import { errorToInputInfo } from '~form-validation'
import DatePicker from '@evaka/lib-components/src/molecules/date-picker/DatePicker'
import { ServiceNeedSectionProps } from '~applications/editor/service-need/ServiceNeedSection'
import ExpandingInfo from '@evaka/lib-components/src/molecules/ExpandingInfo'
import { FixedSpaceColumn } from '@evaka/lib-components/src/layout/flex-helpers'

export default React.memo(function PreferredStartSubSection({
  type,
  formData,
  updateFormData,
  errors,
  verificationRequested
}: ServiceNeedSectionProps) {
  const { applicationId } = useParams<{ applicationId: string }>()
  const t = useTranslation()
  const [lang] = useLang()
  const labelId = Math.random().toString(36).substring(2, 15)

  const uploadUrgencyAttachment = (
    file: File,
    onUploadProgress: (progressEvent: ProgressEvent) => void
  ): Promise<Result<UUID>> =>
    saveAttachment(applicationId, file, 'URGENCY', onUploadProgress).then(
      (result) => {
        result.isSuccess &&
          updateFormData({
            urgencyAttachments: [
              ...formData.urgencyAttachments,
              {
                id: result.value,
                name: file.name,
                contentType: file.type,
                updated: new Date(),
                type: 'URGENCY'
              }
            ]
          })
        return result
      }
    )

  const deleteUrgencyAttachment = (id: UUID) =>
    deleteAttachment(id).then((result) => {
      result.isSuccess &&
        updateFormData({
          urgencyAttachments: formData.urgencyAttachments.filter(
            (file) => file.id !== id
          )
        })
      return result
    })

  return (
    <>
      <div>
        <H3>{t.applications.editor.serviceNeed.startDate.header[type]}</H3>

        {Object.values(
          t.applications.editor.serviceNeed.startDate.info[type]
        ).map((info, index) => (
          <P key={index}>{info}</P>
        ))}

        {type === 'CLUB' && (
          <>
            <FixedSpaceColumn>
              <Label>
                {t.applications.editor.serviceNeed.startDate.clubTerm}
              </Label>
              <span>13.08.2020 - 04.06.2021</span>
            </FixedSpaceColumn>
            <Gap size="m" />
          </>
        )}

        {type !== 'CLUB' ? (
          <ExpandingInfo
            info={t.applications.editor.serviceNeed.startDate.instructions}
          >
            <Label htmlFor={labelId}>
              {t.applications.editor.serviceNeed.startDate.label[type]} *
            </Label>
          </ExpandingInfo>
        ) : (
          <Label htmlFor={labelId}>
            {t.applications.editor.serviceNeed.startDate.label[type]} *
          </Label>
        )}

        <Gap size="s" />

        <DatePicker
          date={formData.preferredStartDate}
          onChange={(date) =>
            updateFormData({
              preferredStartDate: date
            })
          }
          locale={lang}
          info={errorToInputInfo(errors.preferredStartDate, t.validationErrors)}
          hideErrorsBeforeTouched={!verificationRequested}
          data-qa={'preferredStartDate-input'}
          id={labelId}
          required={true}
        />

        {type === 'DAYCARE' && (
          <>
            <Gap size="L" />

            <Checkbox
              checked={formData.urgent}
              dataQa={'urgent-input'}
              label={t.applications.editor.serviceNeed.urgent.label}
              onChange={(checked) =>
                updateFormData({
                  urgent: checked
                })
              }
            />
            {formData.urgent && (
              <>
                <Gap size={'s'} />

                <P
                  fitted
                  dangerouslySetInnerHTML={{
                    __html:
                      t.applications.editor.serviceNeed.urgent
                        .attachmentsMessage.text
                  }}
                />

                <Gap size={'s'} />

                <strong>
                  {
                    t.applications.editor.serviceNeed.urgent.attachmentsMessage
                      .subtitle
                  }
                </strong>

                <Gap size="s" />

                <FileUpload
                  files={formData.urgencyAttachments}
                  onUpload={uploadUrgencyAttachment}
                  onDelete={deleteUrgencyAttachment}
                />
              </>
            )}
          </>
        )}

        {type === 'CLUB' && (
          <>
            <Gap size="L" />

            <ExpandingInfo
              info={
                t.applications.editor.serviceNeed.clubDetails.wasOnDaycareInfo
              }
            >
              <Checkbox
                checked={formData.wasOnDaycare}
                dataQa={'wasOnDaycare-input'}
                label={
                  t.applications.editor.serviceNeed.clubDetails.wasOnDaycare
                }
                onChange={(checked) =>
                  updateFormData({
                    wasOnDaycare: checked
                  })
                }
              />
            </ExpandingInfo>
            <Gap size="m" />
            <ExpandingInfo
              info={
                t.applications.editor.serviceNeed.clubDetails.wasOnClubCareInfo
              }
            >
              <Checkbox
                checked={formData.wasOnClubCare}
                dataQa={'wasOnClubCare-input'}
                label={
                  t.applications.editor.serviceNeed.clubDetails.wasOnClubCare
                }
                onChange={(checked) =>
                  updateFormData({
                    wasOnClubCare: checked
                  })
                }
              />
            </ExpandingInfo>
          </>
        )}
      </div>
    </>
  )
})
