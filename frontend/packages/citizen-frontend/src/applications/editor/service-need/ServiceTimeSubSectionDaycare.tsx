// SPDX-FileCopyrightText: 2017-2020 City of Espoo
//
// SPDX-License-Identifier: LGPL-2.1-or-later

import React from 'react'
import Checkbox from '@evaka/lib-components/src/atoms/form/Checkbox'
import {
  FixedSpaceColumn,
  FixedSpaceRow
} from '@evaka/lib-components/src/layout/flex-helpers'
import Radio from '@evaka/lib-components/src/atoms/form/Radio'
import { useTranslation } from '~localization'
import { H3, Label, P } from '@evaka/lib-components/src/typography'
import InputField from '@evaka/lib-components/src/atoms/form/InputField'
import { Gap } from '@evaka/lib-components/src/white-space'
import FileUpload from '../FileUpload'
import { deleteAttachment, saveAttachment } from '~applications/api'
import { Result } from '~../../lib-common/src/api'
import { UUID } from '~../../lib-common/src/types'
import { useParams } from 'react-router-dom'
import styled from 'styled-components'
import { errorToInputInfo } from '~form-validation'
import { ServiceNeedSectionProps } from '~applications/editor/service-need/ServiceNeedSection'
import ExpandingInfo from '@evaka/lib-components/src/molecules/ExpandingInfo'

const Hyphenbox = styled.div`
  display: flex;
  flex-direction: column;
  justify-content: flex-end;
`

type ServiceTimeSubSectionProps = Omit<ServiceNeedSectionProps, 'type'>

const applicationType = 'DAYCARE'

export default React.memo(function ServiceTimeSubSectionDaycare({
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
    saveAttachment(applicationId, file, 'EXTENDED_CARE', onUploadProgress)

  const deleteExtendedCareAttachment = deleteAttachment

  return (
    <>
      <H3>
        {t.applications.editor.serviceNeed.dailyTime.label[applicationType]}
      </H3>

      <Gap size={'s'} />

      <FixedSpaceColumn>
        <Radio
          id={`service-need-part-time-true`}
          label={t.applications.editor.serviceNeed.partTime.true}
          checked={formData.partTime}
          dataQa={'partTime-input-true'}
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
          dataQa={'partTime-input-false'}
          onChange={() =>
            updateFormData({
              partTime: false
            })
          }
        />
      </FixedSpaceColumn>

      <Gap size={'m'} />

      <ExpandingInfo
        info={
          t.applications.editor.serviceNeed.dailyTime.instructions[
            applicationType
          ]
        }
      >
        <Label>
          {t.applications.editor.serviceNeed.dailyTime.usualArrivalAndDeparture[
            applicationType
          ] + ' *'}
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
      >
        <Checkbox
          checked={formData.shiftCare}
          dataQa={'shiftCare-input'}
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
          />
        </>
      )}
    </>
  )
})
