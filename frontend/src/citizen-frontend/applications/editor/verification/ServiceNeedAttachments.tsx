// SPDX-FileCopyrightText: 2017-2021 City of Espoo
//
// SPDX-License-Identifier: LGPL-2.1-or-later

import React, { useState } from 'react'
import styled from 'styled-components'
import { FontAwesomeIcon } from '@fortawesome/react-fontawesome'

import FileDownloadButton from 'lib-components/molecules/FileDownloadButton'
import InfoModal from 'lib-components/molecules/modals/InfoModal'
import { Label } from 'lib-components/typography'
import { Gap } from 'lib-components/white-space'
import { faFile, faInfo } from 'lib-icons'
import { ApplicationFormData } from '../../../applications/editor/ApplicationFormData'
import { useTranslation } from '../../../localization'
import { getAttachmentBlob } from '../../../applications/api'
import { featureFlags } from 'lib-customizations/citizen'

type Props = {
  formData: ApplicationFormData
}

export const AttachmentList = styled.ul`
  margin-top: 0;
  padding-left: 0;
  list-style: none;
`

export const ServiceNeedUrgency = React.memo(function ServiceNeedUrgency({
  formData
}: Props) {
  const t = useTranslation()

  const [errorModalVisible, setErrorModalVisible] = useState<boolean>(false)

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
        featureFlags.urgencyAttachmentsEnabled === true && (
          <>
            {errorModalVisible && (
              <InfoModal
                title={t.fileDownload.modalHeader}
                text={t.fileDownload.modalMessage}
                close={() => setErrorModalVisible(false)}
                icon={faInfo}
              />
            )}
            <>
              <Label>
                {
                  t.applications.editor.verification.serviceNeed.attachments
                    .label
                }
              </Label>
              <span>
                {formData.serviceNeed.urgencyAttachments.length > 0 ? (
                  <AttachmentList>
                    {formData.serviceNeed.urgencyAttachments.map((file) => (
                      <li key={file.id}>
                        <span className="attachment-icon">
                          <FontAwesomeIcon icon={faFile} />
                        </span>
                        <Gap horizontal size={'xs'} />
                        <FileDownloadButton
                          file={file}
                          fileFetchFn={getAttachmentBlob}
                          onFileUnavailable={() => setErrorModalVisible(true)}
                          dataQa={'service-need-urgency-attachment-download'}
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
  formData
}: Props) {
  const t = useTranslation()

  const [errorModalVisible, setErrorModalVisible] = useState<boolean>(false)

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

      {formData.serviceNeed.shiftCare && (
        <>
          {errorModalVisible && (
            <InfoModal
              title={t.fileDownload.modalHeader}
              text={t.fileDownload.modalMessage}
              close={() => setErrorModalVisible(false)}
              icon={faInfo}
            />
          )}
          <Label>
            {t.applications.editor.verification.serviceNeed.attachments.label}
          </Label>
          <span>
            {formData.serviceNeed.shiftCareAttachments.length > 0 ? (
              <AttachmentList>
                {formData.serviceNeed.shiftCareAttachments.map((file) => (
                  <li key={file.id}>
                    <span className="attachment-icon">
                      <FontAwesomeIcon icon={faFile} />
                    </span>
                    <Gap horizontal size={'xs'} />
                    <FileDownloadButton
                      file={file}
                      fileFetchFn={getAttachmentBlob}
                      onFileUnavailable={() => setErrorModalVisible(true)}
                      dataQa={'service-need-shift-care-attachment-download'}
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
