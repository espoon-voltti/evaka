// SPDX-FileCopyrightText: 2017-2020 City of Espoo
//
// SPDX-License-Identifier: LGPL-2.1-or-later

import React from 'react'
import { ContentArea } from '@evaka/lib-components/src/layout/Container'
import { ServiceNeedFormData } from '~applications/editor/ApplicationFormData'
import { DatePicker } from '@evaka/lib-components/src/molecules/DatePicker'
import Checkbox from '@evaka/lib-components/src/atoms/form/Checkbox'
import {
  FixedSpaceColumn,
  FixedSpaceRow
} from '@evaka/lib-components/src/layout/flex-helpers'
import Radio from '@evaka/lib-components/src/atoms/form/Radio'
import { useTranslation } from '~localization'
import HorizontalLine from '@evaka/lib-components/src/atoms/HorizontalLine'
import { H3, H4 } from '@evaka/lib-components/src/typography'
import InputField from '@evaka/lib-components/src/atoms/form/InputField'
import Tooltip from '@evaka/lib-components/src/atoms/Tooltip'
import colors from '@evaka/lib-components/src/colors'
import RoundIcon from '@evaka/lib-components/src/atoms/RoundIcon'
import { faInfo } from '@evaka/lib-icons'
import { Gap } from '@evaka/lib-components/src/white-space'

export type ServiceNeedSectionProps = {
  formData: ServiceNeedFormData
  updateFormData: (update: Partial<ServiceNeedFormData>) => void
}

const RoundInfoIcon = () => (
  <>
    <Gap horizontal size={'xs'} />
    <RoundIcon
      content={faInfo}
      color={colors.brandEspoo.espooTurquoise}
      size="s"
    />
  </>
)

export default React.memo(function ServiceNeedSection({
  formData,
  updateFormData
}: ServiceNeedSectionProps) {
  const t = useTranslation()
  return (
    <ContentArea opaque paddingVertical="L">
      <FixedSpaceColumn>
        <H3>{t.applications.editor.serviceNeed.serviceNeed}</H3>
        <H4>
          {t.applications.editor.serviceNeed.startDate.label}
          <Tooltip
            tooltip={
              <span>
                {t.applications.editor.serviceNeed.startDate.instructions}
              </span>
            }
          >
            <RoundInfoIcon />
          </Tooltip>
        </H4>

        <DatePicker
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
          <p>
            {t.applications.editor.serviceNeed.urgent.attachmentsMessage.text}
          </p>
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

      <FixedSpaceColumn>
        <H4>
          {t.applications.editor.serviceNeed.dailyTime.usualArrivalAndDeparture}
          <Tooltip
            tooltip={
              <span>
                {t.applications.editor.serviceNeed.dailyTime.instructions}
              </span>
            }
          >
            <RoundInfoIcon />
          </Tooltip>
        </H4>
        <FixedSpaceRow spacing={'m'}>
          <FixedSpaceColumn spacing={'xs'}>
            <InputField
              value={formData.startTime}
              onChange={(value) => updateFormData({ startTime: value })}
              width={'s'}
            />
          </FixedSpaceColumn>

          <FixedSpaceColumn spacing={'xs'}>-</FixedSpaceColumn>

          <FixedSpaceColumn spacing={'xs'}>
            <InputField
              value={formData.endTime}
              onChange={(value) => updateFormData({ endTime: value })}
              width={'s'}
            />
          </FixedSpaceColumn>
        </FixedSpaceRow>

        <H3>
          {t.applications.editor.serviceNeed.shiftCare.label}
          <Tooltip
            tooltip={
              <span>
                {t.applications.editor.serviceNeed.shiftCare.instructions}
              </span>
            }
          >
            <RoundInfoIcon />
          </Tooltip>
        </H3>
        <Checkbox
          checked={formData.shiftCare}
          label={t.applications.editor.serviceNeed.shiftCare.label}
          onChange={(checked) =>
            updateFormData({
              shiftCare: checked
            })
          }
        />

        {formData.shiftCare && (
          <p>
            {
              t.applications.editor.serviceNeed.shiftCare.attachmentsMessage
                .text
            }
          </p>
        )}
      </FixedSpaceColumn>

      <HorizontalLine />

      <FixedSpaceColumn>
        <H3>
          {t.applications.editor.serviceNeed.assistanceNeed}
          <Tooltip
            tooltip={
              <span>
                {t.applications.editor.serviceNeed.assistanceNeedInstructions}
              </span>
            }
          >
            <RoundInfoIcon />
          </Tooltip>
        </H3>
        <Checkbox
          checked={formData.assistanceNeeded}
          label={t.applications.editor.serviceNeed.assistanceNeeded}
          onChange={(checked) =>
            updateFormData({
              assistanceNeeded: checked
            })
          }
        />
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
    </ContentArea>
  )
})
