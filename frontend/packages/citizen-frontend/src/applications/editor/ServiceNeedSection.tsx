// SPDX-FileCopyrightText: 2017-2020 City of Espoo
//
// SPDX-License-Identifier: LGPL-2.1-or-later

import React from 'react'
import { ServiceNeedFormData } from '~applications/editor/ApplicationFormData'
import Checkbox from '@evaka/lib-components/src/atoms/form/Checkbox'
import {
  FixedSpaceColumn,
  FixedSpaceRow
} from '@evaka/lib-components/src/layout/flex-helpers'
import Radio from '@evaka/lib-components/src/atoms/form/Radio'
import { useTranslation } from '~localization'
import HorizontalLine from '@evaka/lib-components/src/atoms/HorizontalLine'
import { H3, Label, P } from '@evaka/lib-components/src/typography'
import InputField from '@evaka/lib-components/src/atoms/form/InputField'
import Tooltip from '@evaka/lib-components/src/atoms/Tooltip'
import { Gap } from '@evaka/lib-components/src/white-space'
import EditorSection from '~applications/editor/EditorSection'
import FileUpload from './FileUpload'
import { deleteAttachment, saveAttachment } from '~applications/api'
import { Result } from '~../../lib-common/src/api'
import { UUID } from '~../../lib-common/src/types'
import { useParams } from 'react-router-dom'
import styled from 'styled-components'
import { DatePickerDeprecated } from '@evaka/lib-components/src/molecules/DatePickerDeprecated'

export type ServiceNeedSectionProps = {
  formData: ServiceNeedFormData
  updateFormData: (update: Partial<ServiceNeedFormData>) => void
}

const Hyphenbox = styled.div`
  display: flex;
  flex-direction: column;
  justify-content: flex-end;
`

export default React.memo(function ServiceNeedSection({
  formData,
  updateFormData
}: ServiceNeedSectionProps) {
  const t = useTranslation()
  const { applicationId } = useParams<{ applicationId: string }>()

  const uploadExtendedCareAttachment = (
    file: File,
    onUploadProgress: (progressEvent: ProgressEvent) => void
  ): Promise<Result<UUID>> =>
    saveAttachment(applicationId, file, 'EXTENDED_CARE', onUploadProgress)

  const uploadUrgencyAttachment = (
    file: File,
    onUploadProgress: (progressEvent: ProgressEvent) => void
  ): Promise<Result<UUID>> =>
    saveAttachment(applicationId, file, 'URGENCY', onUploadProgress)

  const deleteExtendedCareAttachment = deleteAttachment
  const deleteUrgencyAttachment = deleteAttachment

  return (
    <EditorSection
      title={t.applications.editor.serviceNeed.serviceNeed}
      validationErrors={0}
      openInitially
    >
      <FixedSpaceColumn>
        <H3>{t.applications.editor.serviceNeed.startDate.header}</H3>
        <Tooltip
          tooltip={
            <span>
              {t.applications.editor.serviceNeed.startDate.instructions}
            </span>
          }
        >
          <Label>{t.applications.editor.serviceNeed.startDate.label}</Label>
        </Tooltip>

        <DatePickerDeprecated
          date={formData.preferredStartDate || undefined}
          onChange={(date) =>
            updateFormData({
              preferredStartDate: date
            })
          }
          type={'short'}
        />
        <Checkbox
          checked={formData.urgent}
          label={t.applications.editor.serviceNeed.urgent.label}
          onChange={(checked) =>
            updateFormData({
              urgent: checked
            })
          }
        />
        {formData.urgent && (
          <>
            <P>
              {t.applications.editor.serviceNeed.urgent.attachmentsMessage.text}
            </P>
            <strong>
              {
                t.applications.editor.serviceNeed.urgent.attachmentsMessage
                  .subtitle
              }
            </strong>
            <FileUpload
              files={formData.urgencyAttachments}
              onUpload={uploadUrgencyAttachment}
              onDelete={deleteUrgencyAttachment}
            />
          </>
        )}
      </FixedSpaceColumn>

      <HorizontalLine />

      <FixedSpaceColumn>
        <H3>{t.applications.editor.serviceNeed.dailyTime.label}</H3>
        <Radio
          id={`service-need-part-time-true`}
          label={t.applications.editor.serviceNeed.partTime.true}
          checked={formData.partTime}
          onChange={() =>
            updateFormData({
              partTime: true
            })
          }
        />
        <Radio
          id={`service-need-part-time-false`}
          label={t.applications.editor.serviceNeed.partTime.false}
          checked={!formData.partTime}
          onChange={() =>
            updateFormData({
              partTime: false
            })
          }
        />
      </FixedSpaceColumn>

      <Gap size={'s'} />

      <FixedSpaceColumn>
        <Tooltip
          tooltip={
            <span>
              {t.applications.editor.serviceNeed.dailyTime.instructions}
            </span>
          }
        >
          <Label>
            {
              t.applications.editor.serviceNeed.dailyTime
                .usualArrivalAndDeparture
            }
          </Label>
        </Tooltip>
        <FixedSpaceRow spacing={'m'}>
          <FixedSpaceColumn spacing={'xs'}>
            <Label htmlFor={'daily-time-starts'}>
              {t.applications.editor.serviceNeed.dailyTime.starts}
            </Label>
            <InputField
              id={'daily-time-starts'}
              type={'time'}
              value={formData.startTime}
              onChange={(value) => updateFormData({ startTime: value })}
              width={'s'}
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
              onChange={(value) => updateFormData({ endTime: value })}
              width={'s'}
            />
          </FixedSpaceColumn>
        </FixedSpaceRow>

        <Gap size={'s'} />
        <Tooltip
          tooltip={
            <span>
              {t.applications.editor.serviceNeed.shiftCare.instructions}
            </span>
          }
        >
          <Checkbox
            checked={formData.shiftCare}
            label={t.applications.editor.serviceNeed.shiftCare.label}
            onChange={(checked) =>
              updateFormData({
                shiftCare: checked
              })
            }
          />
        </Tooltip>

        {formData.shiftCare && (
          <>
            <p>
              {
                t.applications.editor.serviceNeed.shiftCare.attachmentsMessage
                  .text
              }
            </p>
            <strong>
              {
                t.applications.editor.serviceNeed.shiftCare.attachmentsMessage
                  .subtitle
              }
            </strong>
            <FileUpload
              files={formData.shiftCareAttachments}
              onUpload={uploadExtendedCareAttachment}
              onDelete={deleteExtendedCareAttachment}
            />
          </>
        )}
      </FixedSpaceColumn>

      <HorizontalLine />

      <FixedSpaceColumn>
        <H3>{t.applications.editor.serviceNeed.assistanceNeed}</H3>
        <Tooltip
          tooltip={
            <span>
              {t.applications.editor.serviceNeed.assistanceNeedInstructions}
            </span>
          }
        >
          <Checkbox
            checked={formData.assistanceNeeded}
            label={t.applications.editor.serviceNeed.assistanceNeeded}
            onChange={(checked) =>
              updateFormData({
                assistanceNeeded: checked
              })
            }
          />
        </Tooltip>
        {formData.assistanceNeeded && (
          <InputField
            value={formData.assistanceDescription}
            onChange={(value) =>
              updateFormData({ assistanceDescription: value })
            }
            placeholder={
              t.applications.editor.serviceNeed.assistanceNeedPlaceholder
            }
          />
        )}
      </FixedSpaceColumn>
    </EditorSection>
  )
})
