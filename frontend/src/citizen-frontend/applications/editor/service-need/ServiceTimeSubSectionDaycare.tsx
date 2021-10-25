// SPDX-FileCopyrightText: 2017-2020 City of Espoo
//
// SPDX-License-Identifier: LGPL-2.1-or-later

import React, { useMemo } from 'react'
import Checkbox from 'lib-components/atoms/form/Checkbox'
import {
  FixedSpaceColumn,
  FixedSpaceRow
} from 'lib-components/layout/flex-helpers'
import Radio from 'lib-components/atoms/form/Radio'
import { useTranslation } from '../../../localization'
import { H3, Label, P } from 'lib-components/typography'
import InputField from 'lib-components/atoms/form/InputField'
import { Gap } from 'lib-components/white-space'
import FileUpload from 'lib-components/molecules/FileUpload'
import { Result } from 'lib-common/api'
import { UUID } from 'lib-common/types'
import { useParams } from 'react-router-dom'
import styled from 'styled-components'
import { ServiceNeedSectionProps } from './ServiceNeedSection'
import ExpandingInfo from 'lib-components/molecules/ExpandingInfo'
import { featureFlags } from 'lib-customizations/citizen'
import { defaultMargins } from 'lib-components/white-space'
import {
  deleteAttachment,
  getAttachmentBlob,
  saveApplicationAttachment
} from '../../../attachments'
import { errorToInputInfo } from '../../../input-info-helper'

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
  verificationRequested,
  serviceNeedOptions
}: ServiceTimeSubSectionProps) {
  const t = useTranslation()
  const { applicationId } = useParams<{ applicationId: string }>()

  const fullTimeOptions = useMemo(
    () =>
      serviceNeedOptions?.filter(
        (opt) => opt.validPlacementType === 'DAYCARE'
      ) ?? [],
    [serviceNeedOptions]
  )
  const partTimeOptions = useMemo(
    () =>
      serviceNeedOptions?.filter(
        (opt) => opt.validPlacementType === 'DAYCARE_PART_TIME'
      ) ?? [],
    [serviceNeedOptions]
  )

  const updateServiceNeed = (partTime: boolean) => {
    updateFormData({
      partTime,
      serviceNeedOption:
        formData.serviceNeedOption !== null ? fullTimeOptions[0] : null
    })
  }

  const uploadExtendedCareAttachment = (
    file: File,
    onUploadProgress: (progressEvent: ProgressEvent) => void
  ): Promise<Result<UUID>> =>
    saveApplicationAttachment(
      applicationId,
      file,
      'EXTENDED_CARE',
      onUploadProgress
    ).then((result) => {
      result.isSuccess &&
        updateFormData({
          shiftCareAttachments: [
            ...formData.shiftCareAttachments,
            {
              id: result.value,
              name: file.name,
              contentType: file.type,
              updated: new Date(),
              receivedAt: new Date(),
              type: 'EXTENDED_CARE'
            }
          ]
        })
      return result
    })

  const deleteExtendedCareAttachment = (id: UUID) =>
    deleteAttachment(id).then((result) => {
      result.isSuccess &&
        updateFormData({
          shiftCareAttachments: formData.shiftCareAttachments.filter(
            (file) => file.id !== id
          )
        })
      return result
    })

  function renderServiceNeedSelection() {
    return (
      <FixedSpaceColumn>
        <Radio
          id={`service-need-part-time-true`}
          label={t.applications.editor.serviceNeed.partTime.true}
          checked={formData.partTime}
          data-qa={'partTime-input-true'}
          onChange={() =>
            updateFormData({
              partTime: true,
              serviceNeedOption: partTimeOptions[0] ?? null
            })
          }
        />
        {partTimeOptions.length > 0 && formData.partTime && (
          <SubRadios>
            <FixedSpaceColumn spacing={'xs'}>
              {partTimeOptions.map((opt) => (
                <Radio
                  key={opt.id}
                  label={opt.name}
                  checked={formData.serviceNeedOption?.id === opt.id}
                  onChange={() => updateFormData({ serviceNeedOption: opt })}
                  data-qa={`part-time-option-${opt.id}`}
                />
              ))}
            </FixedSpaceColumn>
          </SubRadios>
        )}
        <Radio
          id={`service-need-part-time-false`}
          label={t.applications.editor.serviceNeed.partTime.false}
          checked={!formData.partTime}
          data-qa={'partTime-input-false'}
          onChange={() => updateServiceNeed(false)}
        />
        {fullTimeOptions.length > 0 && !formData.partTime && (
          <SubRadios>
            <FixedSpaceColumn spacing={'xs'}>
              {fullTimeOptions.map((opt) => (
                <Radio
                  key={opt.id}
                  label={opt.name}
                  checked={formData.serviceNeedOption?.id === opt.id}
                  onChange={() => updateFormData({ serviceNeedOption: opt })}
                  data-qa={`full-time-option-${opt.id}`}
                />
              ))}
            </FixedSpaceColumn>
          </SubRadios>
        )}
      </FixedSpaceColumn>
    )
  }

  function renderServiceNeedDailyTimeSelection() {
    return (
      featureFlags.daycareApplication.dailyTimesEnabled && (
        <>
          <ExpandingInfo
            info={
              t.applications.editor.serviceNeed.dailyTime.instructions[
                applicationType
              ]
            }
            ariaLabel={t.common.openExpandingInfo}
          >
            <Label>
              {t.applications.editor.serviceNeed.dailyTime
                .usualArrivalAndDeparture[applicationType] + ' *'}
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
        </>
      )
    )
  }

  return (
    <>
      <H3>
        {t.applications.editor.serviceNeed.dailyTime.label[applicationType]}
      </H3>

      <Gap size={'s'} />

      {renderServiceNeedSelection()}

      <Gap size={'m'} />

      {renderServiceNeedDailyTimeSelection()}

      <Gap size={'L'} />

      <ExpandingInfo
        data-qa="shiftcare-instructions"
        info={t.applications.editor.serviceNeed.shiftCare.instructions}
        ariaLabel={t.common.openExpandingInfo}
        margin="xs"
      >
        <Checkbox
          checked={formData.shiftCare}
          data-qa={'shiftCare-input'}
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

          <P fitted data-qa="shiftcare-attachments-message">
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
            onDownloadFile={getAttachmentBlob}
            i18n={{ upload: t.fileUpload, download: t.fileDownload }}
          />
        </>
      )}
    </>
  )
})

const SubRadios = styled.div`
  margin-bottom: ${defaultMargins.s};
  margin-left: ${defaultMargins.XL};
`
