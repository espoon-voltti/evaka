import { ApplicationFormData } from '~applications/editor/ApplicationFormData'
import React from 'react'
import { useTranslation } from '~localization'
import { H2, H3, Label } from '@evaka/lib-components/src/typography'
import ListGrid from '@evaka/lib-components/src/layout/ListGrid'
import { ApplicationDataGridLabelWidth } from '~applications/editor/verification/const'
import { Gap } from '@evaka/lib-components/src/white-space'
import { faFile } from '@evaka/lib-icons'
import { FontAwesomeIcon } from '@fortawesome/react-fontawesome'
import styled from 'styled-components'
import { espooBrandColors } from '@evaka/lib-components/src/colors'

type ServiceNeedSectionProps = {
  formData: ApplicationFormData
}

const AttachmentList = styled.ul`
  margin-top: 0;
  padding-left: 0;
  list-style: none;
`

const AttachmentDownload = styled.a`
  color: ${espooBrandColors.espooTurquoise};
  text-decoration: none;
`

export default React.memo(function ServiceNeedSection({
  formData
}: ServiceNeedSectionProps) {
  const t = useTranslation()

  return (
    <div>
      <H2>{t.applications.editor.verification.serviceNeed.title}</H2>

      <Gap size={'s'} />
      <H3>{t.applications.editor.verification.serviceNeed.startDate.title}</H3>
      <ListGrid
        labelWidth={ApplicationDataGridLabelWidth}
        rowGap="s"
        columnGap="L"
      >
        <Label>
          {
            t.applications.editor.verification.serviceNeed.startDate
              .preferredStartDate
          }
        </Label>
        <span>{formData.serviceNeed.preferredStartDate}</span>

        <Label>
          {t.applications.editor.verification.serviceNeed.startDate.urgency}
        </Label>
        <span>
          {formData.serviceNeed.urgent
            ? t.applications.editor.verification.serviceNeed.startDate
                .withUrgency
            : t.applications.editor.verification.serviceNeed.startDate
                .withoutUrgency}
        </span>

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
      </ListGrid>

      <Gap size={'s'} />
      <H3>{t.applications.editor.verification.serviceNeed.dailyTime.title}</H3>
      <ListGrid
        labelWidth={ApplicationDataGridLabelWidth}
        rowGap="s"
        columnGap="L"
      >
        <Label>
          {t.applications.editor.verification.serviceNeed.dailyTime.partTime}
        </Label>
        <span>
          {formData.serviceNeed.partTime
            ? t.applications.editor.verification.serviceNeed.dailyTime
                .withPartTime
            : t.applications.editor.verification.serviceNeed.dailyTime
                .withoutPartTime}
        </span>

        <Label>
          {t.applications.editor.verification.serviceNeed.dailyTime.dailyTime}
        </Label>
        <span>
          {formData.serviceNeed.startTime} - {formData.serviceNeed.endTime}
        </span>

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
      </ListGrid>

      <Gap size={'s'} />
      <H3>
        {t.applications.editor.verification.serviceNeed.assistanceNeed.title}
      </H3>
      <ListGrid
        labelWidth={ApplicationDataGridLabelWidth}
        rowGap="s"
        columnGap="L"
      >
        <Label>
          {
            t.applications.editor.verification.serviceNeed.assistanceNeed
              .assistanceNeed
          }
        </Label>
        <span>
          {formData.serviceNeed.assistanceNeeded
            ? t.applications.editor.verification.serviceNeed.assistanceNeed
                .withAssistanceNeed
            : t.applications.editor.verification.serviceNeed.assistanceNeed
                .withoutAssistanceNeed}
        </span>

        <Label>
          {
            t.applications.editor.verification.serviceNeed.assistanceNeed
              .description
          }
        </Label>
        <span>{formData.serviceNeed.assistanceDescription}</span>
      </ListGrid>
    </div>
  )
})
