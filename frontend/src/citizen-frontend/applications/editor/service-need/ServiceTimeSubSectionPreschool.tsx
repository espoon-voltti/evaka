// SPDX-FileCopyrightText: 2017-2022 City of Espoo
//
// SPDX-License-Identifier: LGPL-2.1-or-later

import React, { useEffect, useMemo } from 'react'
import styled from 'styled-components'

import DateRange from 'lib-common/date-range'
import type { PlacementType } from 'lib-common/generated/api-types/placement'
import type { ServiceNeedOptionPublicInfo } from 'lib-common/generated/api-types/serviceneed'
import type { ApplicationId } from 'lib-common/generated/api-types/shared'
import HelsinkiDateTime from 'lib-common/helsinki-date-time'
import { useIdRouteParam } from 'lib-common/useRouteParams'
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
import { AlertBox } from 'lib-components/molecules/MessageBoxes'
import DatePicker from 'lib-components/molecules/date-picker/DatePicker'
import { H3, Label } from 'lib-components/typography'
import { defaultMargins, Gap } from 'lib-components/white-space'
import { featureFlags } from 'lib-customizations/citizen'

import { getAttachmentUrl, applicationAttachment } from '../../../attachments'
import { errorToInputInfo } from '../../../input-info-helper'
import { useLang, useTranslation } from '../../../localization'

import type { ServiceNeedSectionProps } from './ServiceNeedSection'

const Hyphenbox = styled.div`
  padding-top: 36px;
`

type ServiceTimeSubSectionProps = Omit<ServiceNeedSectionProps, 'type'>

const applicationType = 'PRESCHOOL'

export default React.memo(function ServiceTimeSubSectionPreschool({
  minDate,
  maxDate,
  formData,
  updateFormData,
  errors,
  verificationRequested,
  serviceNeedOptions
}: ServiceTimeSubSectionProps) {
  const [lang] = useLang()
  const t = useTranslation()
  const applicationId = useIdRouteParam<ApplicationId>('applicationId')
  const labelId = useUniqueId()

  const serviceNeedOptionsByType = useMemo(
    () =>
      serviceNeedOptions?.reduce<
        Map<PlacementType, ServiceNeedOptionPublicInfo[]>
      >((map, item) => {
        const key = item.validPlacementType
        const list = map.get(key) ?? []
        list.push(item)
        map.set(key, list)
        return map
      }, new Map<PlacementType, ServiceNeedOptionPublicInfo[]>()) ??
      new Map<PlacementType, ServiceNeedOptionPublicInfo[]>(),
    [serviceNeedOptions]
  )

  const preferredStartDate = featureFlags.preschoolApplication
    .connectedDaycarePreferredStartDate
    ? formData.connectedDaycarePreferredStartDate
    : formData.preferredStartDate

  useEffect(() => {
    if (
      featureFlags.preschoolApplication.serviceNeedOption &&
      preferredStartDate &&
      formData.placementType &&
      formData.serviceNeedOption
    ) {
      const validSelectedType = serviceNeedOptionsByType
        .get(formData.placementType)
        ?.find(
          (opt) =>
            opt.id === formData.serviceNeedOption?.id &&
            new DateRange(opt.validFrom, opt.validTo).includes(
              preferredStartDate
            )
        )
      if (!validSelectedType) {
        updateFormData({ serviceNeedOption: null })
      }
    }
  }, [
    preferredStartDate,
    formData.placementType,
    formData.serviceNeedOption,
    serviceNeedOptionsByType,
    updateFormData
  ])

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
                minDate={minDate}
                maxDate={maxDate}
                initialMonth={
                  formData.connectedDaycarePreferredStartDate ??
                  formData.preferredStartDate ??
                  undefined
                }
                data-qa="connectedDaycarePreferredStartDate-input"
                id={labelId}
                required={true}
              />
              <Gap size="m" />
            </>
          ) : null}

          {featureFlags.preschoolApplication.serviceNeedOption ? (
            preferredStartDate ? (
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
                            {options
                              .filter((opt) =>
                                new DateRange(
                                  opt.validFrom,
                                  opt.validTo
                                ).includes(preferredStartDate)
                              )
                              .map((opt) => (
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
                                  data-qa={`service-need-option-${opt.id}`}
                                />
                              ))}
                          </FixedSpaceColumn>
                        </SubRadios>
                      )}
                    </React.Fragment>
                  ))}
                  {errors.serviceNeedOption && verificationRequested && (
                    <div>
                      <AlertBox
                        message={t.validationErrors[errors.serviceNeedOption]}
                        thin
                        noMargin
                      />
                    </div>
                  )}
                </FixedSpaceColumn>
              </>
            ) : (
              <div>
                <AlertBox
                  message={t.applications.editor.serviceNeed.startDate.missing}
                />
              </div>
            )
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

              <Gap size="s" />

              {
                t.applications.editor.serviceNeed.shiftCare.attachmentsMessage
                  .PRESCHOOL
              }

              <Gap size="s" />

              <strong>
                {
                  t.applications.editor.serviceNeed.shiftCare
                    .attachmentsSubtitle
                }
              </strong>

              <Gap size="s" />

              <FileUpload
                files={formData.shiftCareAttachments}
                uploadHandler={applicationAttachment(
                  applicationId,
                  'EXTENDED_CARE'
                )}
                onUploaded={(attachment) =>
                  updateFormData({
                    shiftCareAttachments: [
                      ...formData.shiftCareAttachments,
                      {
                        ...attachment,
                        updated: HelsinkiDateTime.now(),
                        receivedAt: HelsinkiDateTime.now(),
                        type: 'EXTENDED_CARE',
                        uploadedByEmployee: null,
                        uploadedByPerson: null
                      }
                    ]
                  })
                }
                onDeleted={(id) =>
                  updateFormData({
                    shiftCareAttachments: formData.shiftCareAttachments.filter(
                      (file) => file.id !== id
                    )
                  })
                }
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
