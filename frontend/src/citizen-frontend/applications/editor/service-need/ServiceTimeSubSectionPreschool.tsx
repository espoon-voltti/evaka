// SPDX-FileCopyrightText: 2017-2022 City of Espoo
//
// SPDX-License-Identifier: LGPL-2.1-or-later

import React from 'react'
import styled from 'styled-components'

import { Result } from 'lib-common/api'
import { PlacementType } from 'lib-common/generated/api-types/placement'
import { ServiceNeedOptionPublicInfo } from 'lib-common/generated/api-types/serviceneed'
import HelsinkiDateTime from 'lib-common/helsinki-date-time'
import LocalDate from 'lib-common/local-date'
import { UUID } from 'lib-common/types'
import useRequiredParams from 'lib-common/useRequiredParams'
import { useUniqueId } from 'lib-common/utils/useUniqueId'
import Checkbox from 'lib-components/atoms/form/Checkbox'
import Radio from 'lib-components/atoms/form/Radio'
import TimeInput from 'lib-components/atoms/form/TimeInput'
import {
  FixedSpaceColumn,
  FixedSpaceRow
} from 'lib-components/layout/flex-helpers'
import ExpandingInfo from 'lib-components/molecules/ExpandingInfo'
import FileUpload from 'lib-components/molecules/FileUpload'
import DatePicker from 'lib-components/molecules/date-picker/DatePicker'
import { H3, Label, P } from 'lib-components/typography'
import { defaultMargins, Gap } from 'lib-components/white-space'
import { featureFlags } from 'lib-customizations/citizen'

import {
  deleteAttachment,
  getAttachmentUrl,
  saveApplicationAttachment
} from '../../../attachments'
import { errorToInputInfo } from '../../../input-info-helper'
import { useLang, useTranslation } from '../../../localization'
import { isValidPreferredStartDate } from '../validations'

import { ServiceNeedSectionProps } from './ServiceNeedSection'

const Hyphenbox = styled.div`
  padding-top: 36px;
`

type ServiceTimeSubSectionProps = Omit<ServiceNeedSectionProps, 'type'>

const applicationType = 'PRESCHOOL'

export default React.memo(function ServiceTimeSubSectionPreschool({
  originalPreferredStartDate,
  minDate,
  maxDate,
  formData,
  updateFormData,
  errors,
  verificationRequested,
  terms,
  serviceNeedOptions
}: ServiceTimeSubSectionProps) {
  const [lang] = useLang()
  const t = useTranslation()
  const { applicationId } = useRequiredParams('applicationId')
  const labelId = useUniqueId()

  const serviceNeedOptionsByType =
    serviceNeedOptions?.reduce<
      Map<PlacementType, ServiceNeedOptionPublicInfo[]>
    >((map, item) => {
      const key = item.validPlacementType
      const list = map.get(key) ?? []
      list.push(item)
      map.set(key, list)
      return map
    }, new Map<PlacementType, ServiceNeedOptionPublicInfo[]>()) ??
    new Map<PlacementType, ServiceNeedOptionPublicInfo[]>()

  const uploadExtendedCareAttachment = (
    file: File,
    onUploadProgress: (percentage: number) => void
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
              updated: HelsinkiDateTime.now(),
              receivedAt: HelsinkiDateTime.now(),
              type: 'EXTENDED_CARE',
              uploadedByEmployee: null,
              uploadedByPerson: null
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

  return (
    <>
      <H3>
        {t.applications.editor.serviceNeed.dailyTime.label[applicationType]}
      </H3>

      {t.applications.editor.serviceNeed.dailyTime.connectedDaycareInfo}

      <Checkbox
        checked={formData.connectedDaycare}
        data-qa="connectedDaycare-input"
        label={t.applications.editor.serviceNeed.dailyTime.connectedDaycare}
        onChange={(checked) =>
          updateFormData({
            connectedDaycare: checked
          })
        }
      />

      {formData.connectedDaycare && (
        <>
          <Gap size="m" />

          {featureFlags.preschoolApplication
            .connectedDaycarePreferredStartDate ? (
            <>
              <Label htmlFor={labelId}>
                {t.applications.editor.serviceNeed.startDate.label.PRESCHOOL} *
              </Label>

              <Gap size="s" />

              <DatePicker
                date={formData.connectedDaycarePreferredStartDate}
                onChange={(date) =>
                  updateFormData({
                    connectedDaycarePreferredStartDate: date
                  })
                }
                locale={lang}
                info={errorToInputInfo(
                  errors.connectedDaycarePreferredStartDate,
                  t.validationErrors
                )}
                hideErrorsBeforeTouched={!verificationRequested}
                isInvalidDate={(date: LocalDate) =>
                  isValidPreferredStartDate(
                    date,
                    originalPreferredStartDate,
                    'PRESCHOOL',
                    terms
                  )
                    ? null
                    : t.validationErrors.unselectableDate
                }
                minDate={minDate}
                maxDate={maxDate}
                data-qa="connectedDaycarePreferredStartDate-input"
                id={labelId}
                required={true}
              />
              <Gap size="m" />
            </>
          ) : null}

          {featureFlags.preschoolApplication.serviceNeedOption ? (
            <>
              <FixedSpaceColumn>
                {[...serviceNeedOptionsByType].map(([type, options]) => (
                  <React.Fragment key={type}>
                    <Radio
                      label={t.placement.type[type]}
                      checked={formData.placementType === type}
                      onChange={() => {
                        updateFormData({
                          placementType: type,
                          serviceNeedOption: null
                        })
                      }}
                    />
                    {formData.placementType === type && (
                      <SubRadios>
                        <FixedSpaceColumn spacing="xs">
                          {options.map((opt) => (
                            <Radio
                              key={opt.id}
                              label={
                                (lang === 'fi' && opt.nameFi) ||
                                (lang === 'sv' && opt.nameSv) ||
                                (lang === 'en' && opt.nameEn) ||
                                opt.id
                              }
                              checked={
                                formData.serviceNeedOption?.id === opt.id
                              }
                              onChange={() =>
                                updateFormData({ serviceNeedOption: opt })
                              }
                            />
                          ))}
                        </FixedSpaceColumn>
                      </SubRadios>
                    )}
                  </React.Fragment>
                ))}
              </FixedSpaceColumn>
            </>
          ) : (
            <>
              <ExpandingInfo
                info={
                  t.applications.editor.serviceNeed.dailyTime.instructions[
                    applicationType
                  ]
                }
                inlineChildren
              >
                <Label>
                  {t.applications.editor.serviceNeed.dailyTime
                    .usualArrivalAndDeparture[applicationType] + ' *'}
                </Label>
              </ExpandingInfo>

              <Gap size="s" />

              <FixedSpaceRow spacing="m">
                <FixedSpaceColumn spacing="xs">
                  <Label htmlFor="daily-time-starts">
                    {t.applications.editor.serviceNeed.dailyTime.starts}
                  </Label>
                  <TimeInput
                    id="daily-time-starts"
                    value={formData.startTime}
                    data-qa="startTime-input"
                    onChange={(value) => updateFormData({ startTime: value })}
                    info={errorToInputInfo(
                      errors.startTime,
                      t.validationErrors
                    )}
                    hideErrorsBeforeTouched={!verificationRequested}
                  />
                </FixedSpaceColumn>

                <Hyphenbox>–</Hyphenbox>

                <FixedSpaceColumn spacing="xs">
                  <Label htmlFor="daily-time-ends">
                    {t.applications.editor.serviceNeed.dailyTime.ends}
                  </Label>
                  <TimeInput
                    id="daily-time-ends"
                    value={formData.endTime}
                    data-qa="endTime-input"
                    onChange={(value) => updateFormData({ endTime: value })}
                    info={errorToInputInfo(errors.endTime, t.validationErrors)}
                    hideErrorsBeforeTouched={!verificationRequested}
                  />
                </FixedSpaceColumn>
              </FixedSpaceRow>
            </>
          )}

          <Gap size="L" />

          <ExpandingInfo
            info={t.applications.editor.serviceNeed.shiftCare.instructions}
            margin="xs"
          >
            <Checkbox
              checked={formData.shiftCare}
              data-qa="shiftCare-input"
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
              <Gap size="xxs" />

              <P fitted>
                {t.applications.editor.serviceNeed.shiftCare.instructions2}
              </P>

              <Gap size="s" />

              <P fitted>
                {
                  t.applications.editor.serviceNeed.shiftCare.attachmentsMessage
                    .text
                }
              </P>

              <Gap size="s" />

              <strong>
                {
                  t.applications.editor.serviceNeed.shiftCare.attachmentsMessage
                    .subtitle
                }
              </strong>

              <Gap size="s" />

              <FileUpload
                files={formData.shiftCareAttachments}
                onUpload={uploadExtendedCareAttachment}
                onDelete={deleteExtendedCareAttachment}
                getDownloadUrl={getAttachmentUrl}
              />
            </>
          )}
        </>
      )}
    </>
  )
})

const SubRadios = styled.div`
  margin-bottom: ${defaultMargins.s};
  margin-left: ${defaultMargins.XL};
`
