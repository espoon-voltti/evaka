// SPDX-FileCopyrightText: 2017-2020 City of Espoo
//
// SPDX-License-Identifier: LGPL-2.1-or-later

import { Result } from 'lib-common/api'
import LocalDate from 'lib-common/local-date'
import { UUID } from 'lib-common/types'
import Checkbox from 'lib-components/atoms/form/Checkbox'
import DatePicker from 'lib-components/molecules/date-picker/DatePicker'
import ExpandingInfo from 'lib-components/molecules/ExpandingInfo'
import FileUpload from 'lib-components/molecules/FileUpload'
import { AlertBox } from 'lib-components/molecules/MessageBoxes'
import { H3, Label, P } from 'lib-components/typography'
import { Gap } from 'lib-components/white-space'
import { featureFlags } from 'lib-customizations/citizen'
import React from 'react'
import { useParams } from 'react-router-dom'
import { errorToInputInfo } from '../../../form-validation'
import { useLang, useTranslation } from '../../../localization'
import { deleteAttachment, getAttachmentBlob, saveAttachment } from '../../api'
import { isValidPreferredStartDate } from '../validations'
import { ClubTermsInfo } from './ClubTermsInfo'
import { ServiceNeedSectionProps } from './ServiceNeedSection'

export default React.memo(function PreferredStartSubSection({
  originalPreferredStartDate,
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
                receivedAt: new Date(),
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

  const showDaycare4MonthWarning = (): boolean => {
    const preferredStartDate = LocalDate.parseFiOrNull(
      formData.preferredStartDate
    )
    return (
      type === 'DAYCARE' &&
      preferredStartDate !== null &&
      preferredStartDate.isBefore(LocalDate.today().addMonths(4))
    )
  }

  return (
    <>
      <div>
        <H3>{t.applications.editor.serviceNeed.startDate.header[type]}</H3>

        {Object.values(
          t.applications.editor.serviceNeed.startDate.info[type]
        ).map((info, index) => (
          <P key={index}>{info}</P>
        ))}

        {type === 'CLUB' && <ClubTermsInfo />}

        {type !== 'CLUB' ? (
          <ExpandingInfo
            info={t.applications.editor.serviceNeed.startDate.instructions()}
            ariaLabel={t.common.openExpandingInfo}
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
          isValidDate={(date: LocalDate) =>
            isValidPreferredStartDate(date, originalPreferredStartDate, type)
          }
          data-qa={'preferredStartDate-input'}
          id={labelId}
          required={true}
        />

        {showDaycare4MonthWarning() ? (
          <>
            <Gap size="xs" />
            <AlertBox
              message={t.applications.creation.daycare4monthWarning}
              data-qa={'daycare-processing-time-warning'}
            />
          </>
        ) : null}

        {type === 'DAYCARE' && (
          <>
            <Gap size="L" />

            <Checkbox
              checked={formData.urgent}
              data-qa={'urgent-input'}
              label={t.applications.editor.serviceNeed.urgent.label}
              onChange={(checked) =>
                updateFormData({
                  urgent: checked
                })
              }
            />
            <Gap size={'s'} />
            {t.applications.editor.serviceNeed.urgent.attachmentsMessage.text()}

            {formData.urgent && featureFlags.urgencyAttachmentsEnabled && (
              <>
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
                  onDownloadFile={getAttachmentBlob}
                  i18n={{ upload: t.fileUpload, download: t.fileDownload }}
                  data-qa={'urgent-file-upload'}
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
              ariaLabel={t.common.openExpandingInfo}
            >
              <Checkbox
                checked={formData.wasOnDaycare}
                data-qa={'wasOnDaycare-input'}
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
              ariaLabel={t.common.openExpandingInfo}
            >
              <Checkbox
                checked={formData.wasOnClubCare}
                data-qa={'wasOnClubCare-input'}
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
