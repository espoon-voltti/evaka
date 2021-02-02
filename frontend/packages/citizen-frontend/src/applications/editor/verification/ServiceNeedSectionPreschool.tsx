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

export default React.memo(function ServiceNeedSectionPreschool({
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
      </ListGrid>

      <Gap size={'s'} />
      <H3>
        {t.applications.editor.verification.serviceNeed.connectedDaycare.title}
      </H3>
      <ListGrid
        labelWidth={ApplicationDataGridLabelWidth}
        rowGap="s"
        columnGap="L"
      >
        <Label>
          {
            t.applications.editor.verification.serviceNeed.connectedDaycare
              .label
          }
        </Label>
        <span>
          {formData.serviceNeed.connectedDaycare
            ? t.applications.editor.verification.serviceNeed.connectedDaycare
                .withConnectedDaycare
            : t.applications.editor.verification.serviceNeed.connectedDaycare
                .withoutConnectedDaycare}
        </span>

        {formData.serviceNeed.connectedDaycare && (
          <>
            <Label>
              {
                t.applications.editor.verification.serviceNeed.dailyTime
                  .dailyTime
              }
            </Label>
            <span>
              {formData.serviceNeed.startTime} - {formData.serviceNeed.endTime}
            </span>
          </>
        )}

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

        {formData.serviceNeed.assistanceNeeded && (
          <>
            <Label>
              {
                t.applications.editor.verification.serviceNeed.assistanceNeed
                  .description
              }
            </Label>
            <span>{formData.serviceNeed.assistanceDescription}</span>
          </>
        )}

        {formData.serviceNeed.preparatory && (
          <>
            <Label>
              {
                t.applications.editor.verification.serviceNeed
                  .preparatoryEducation.label
              }
            </Label>
            <span>
              {formData.serviceNeed.preparatory
                ? t.applications.editor.verification.serviceNeed
                    .preparatoryEducation.withPreparatory
                : t.applications.editor.verification.serviceNeed
                    .preparatoryEducation.withoutPreparatory}
            </span>
          </>
        )}
      </ListGrid>
    </div>
  )
})
