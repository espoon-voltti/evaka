// SPDX-FileCopyrightText: 2017-2021 City of Espoo
//
// SPDX-License-Identifier: LGPL-2.1-or-later

import { ApplicationFormData } from '~applications/editor/ApplicationFormData'
import React from 'react'
import { useTranslation } from '~localization'
import { Label } from '@evaka/lib-components/src/typography'
import { Gap } from '@evaka/lib-components/src/white-space'
import { faFile } from '@evaka/lib-icons'
import { FontAwesomeIcon } from '@fortawesome/react-fontawesome'
import styled from 'styled-components'
import { espooBrandColors } from '@evaka/lib-components/src/colors'

type ServiceNeedSectionProps = {
  formData: ApplicationFormData
}

export const AttachmentList = styled.ul`
  margin-top: 0;
  padding-left: 0;
  list-style: none;
`

export const AttachmentDownload = styled.a`
  color: ${espooBrandColors.espooTurquoise};
  text-decoration: none;
`

export default React.memo(function ServiceNeedUrgency({
  formData
}: ServiceNeedSectionProps) {
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

      {formData.serviceNeed.urgent && (
        <>
          <Label>
            {t.applications.editor.verification.serviceNeed.attachments.label}
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
                    <AttachmentDownload
                      href={`/api/application/attachments/${file.id}/download`}
                      target="_blank"
                      rel="noreferrer"
                    >
                      {file.name}
                    </AttachmentDownload>
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
