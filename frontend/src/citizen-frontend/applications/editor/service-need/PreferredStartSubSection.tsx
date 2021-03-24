// SPDX-FileCopyrightText: 2017-2020 City of Espoo
//
// SPDX-License-Identifier: LGPL-2.1-or-later

import React from 'react'
import styled from 'styled-components'
import Checkbox from 'lib-components/atoms/form/Checkbox'
import { useLang, useTranslation } from '../../../localization'
import { H3, Label, P } from 'lib-components/typography'
import { Gap } from 'lib-components/white-space'
import FileUpload from '../FileUpload'
import { deleteAttachment, saveAttachment } from '../../../applications/api'
import { Result } from 'lib-common/api'
import { UUID } from 'lib-common/types'
import { useParams } from 'react-router-dom'
import { errorToInputInfo } from '../../../form-validation'
import DatePicker from 'lib-components/molecules/date-picker/DatePicker'
import { ServiceNeedSectionProps } from '../../../applications/editor/service-need/ServiceNeedSection'
import ExpandingInfo from 'lib-components/molecules/ExpandingInfo'
import { isValidPreferredStartDate } from '../../../applications/editor/validations'
import LocalDate from 'lib-common/local-date'
import { AlertBox } from 'lib-components/molecules/MessageBoxes'

const clubTerms = ['13.08.2020 - 04.06.2021', '11.8.2021-03.06.2022']

export default React.memo(function PreferredStartSubSection({
  status,
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

        {type === 'CLUB' && (
          <>
            <Label>
              {
                t.applications.editor.serviceNeed.startDate[
                  clubTerms.length === 1 ? 'clubTerm' : 'clubTerms'
                ]
              }
            </Label>
            <Gap size="s" />
            <Ul>
              {clubTerms.map((term) => (
                <li key={term}>{term}</li>
              ))}
            </Ul>
            <Gap size="m" />
          </>
        )}

        {type !== 'CLUB' ? (
          <ExpandingInfo
            info={t.applications.editor.serviceNeed.startDate.instructions}
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
            isValidPreferredStartDate(
              date,
              originalPreferredStartDate,
              status,
              type
            )
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
                {t.applications.editor.serviceNeed.urgent.attachmentsMessage.text()}

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
                  dataQa={'urgent-file-upload'}
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
              ariaLabel={t.common.openExpandingInfo}
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

const Ul = styled.ul`
  margin: 0;
`
