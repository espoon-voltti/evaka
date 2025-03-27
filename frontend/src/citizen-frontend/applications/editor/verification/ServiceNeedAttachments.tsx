// SPDX-FileCopyrightText: 2017-2022 City of Espoo
//
// SPDX-License-Identifier: LGPL-2.1-or-later

import React from 'react'
import styled from 'styled-components'

import { ApplicationFormData } from 'lib-common/api-types/application/ApplicationFormData'
import FileDownloadButton from 'lib-components/molecules/FileDownloadButton'
import { Label } from 'lib-components/typography'
import { featureFlags } from 'lib-customizations/citizen'

import { getAttachmentUrl } from '../../../attachments'
import { useTranslation } from '../../../localization'

type Props = {
  formData: ApplicationFormData
  userIsApplicationGuardian: boolean
}

export const AttachmentList = styled.ul`
  margin-top: 0;
  padding-left: 0;
  list-style: none;
`

export const ServiceNeedUrgency = React.memo(function ServiceNeedUrgency({
  formData,
  userIsApplicationGuardian
}: Props) {
  const t = useTranslation()

  return (
    <>
      <Label>
        {t.applications.editor.verification.serviceNeed.startDate.urgency}
      </Label>
      <span>
        {formData.serviceNeed.urgent
          ? t.applications.editor.verification.serviceNeed.startDate.withUrgency
          : t.applications.editor.verification.serviceNeed.startDate
              .withoutUrgency}
      </span>

      {formData.serviceNeed.urgent &&
        featureFlags.urgencyAttachments &&
        userIsApplicationGuardian && (
          <>
            <>
              <Label>
                {
                  t.applications.editor.verification.serviceNeed.attachments
                    .label
                }
              </Label>
              <span data-qa="service-need-urgency-attachments">
                {formData.serviceNeed.urgencyAttachments.length > 0 ? (
                  <AttachmentList>
                    {formData.serviceNeed.urgencyAttachments.map((file) => (
                      <li key={file.id}>
                        <FileDownloadButton
                          file={file}
                          getFileUrl={getAttachmentUrl}
                          data-qa="service-need-urgency-attachment-download"
                          icon
                        />
                      </li>
                    ))}
                  </AttachmentList>
                ) : (
                  t.applications.editor.verification.serviceNeed.attachments
                    .withoutAttachments
                )}
              </span>
            </>
          </>
        )}
    </>
  )
})

export const ServiceNeedShiftCare = React.memo(function ServiceNeedShiftCare({
  formData,
  userIsApplicationGuardian
}: Props) {
  const t = useTranslation()

  return (
    <>
      <Label>
        {t.applications.editor.verification.serviceNeed.dailyTime.shiftCare}
      </Label>
      <span>
        {formData.serviceNeed.shiftCare
          ? t.applications.editor.verification.serviceNeed.dailyTime
              .withShiftCare
          : t.applications.editor.verification.serviceNeed.dailyTime
              .withoutShiftCare}
      </span>

      {formData.serviceNeed.shiftCare && userIsApplicationGuardian && (
        <>
          <Label>
            {t.applications.editor.verification.serviceNeed.attachments.label}
          </Label>
          <span data-qa="service-need-shift-care-attachments">
            {formData.serviceNeed.shiftCareAttachments.length > 0 ? (
              <AttachmentList>
                {formData.serviceNeed.shiftCareAttachments.map((file) => (
                  <li key={file.id}>
                    <FileDownloadButton
                      file={file}
                      getFileUrl={getAttachmentUrl}
                      data-qa="service-need-shift-care-attachment-download"
                      icon
                    />
                  </li>
                ))}
              </AttachmentList>
            ) : (
              t.applications.editor.verification.serviceNeed.attachments
                .withoutAttachments
            )}
          </span>
        </>
      )}
    </>
  )
})
