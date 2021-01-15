// SPDX-FileCopyrightText: 2017-2020 City of Espoo
//
// SPDX-License-Identifier: LGPL-2.1-or-later

import React from 'react'
import { ContentArea } from '@evaka/lib-components/src/layout/Container'
import { ServiceNeedFormData } from '~applications/editor/ApplicationFormData'
import { DatePicker } from '@evaka/lib-components/src/molecules/DatePicker'
import Checkbox from '@evaka/lib-components/src/atoms/form/Checkbox'
import { FixedSpaceColumn } from '@evaka/lib-components/src/layout/flex-helpers'

export type ServiceNeedSectionProps = {
  formData: ServiceNeedFormData
  updateFormData: (update: Partial<ServiceNeedFormData>) => void
}

export default React.memo(function ServiceNeedSection({
  formData,
  updateFormData
}: ServiceNeedSectionProps) {
  return (
    <ContentArea opaque paddingVertical="L">
      <FixedSpaceColumn>
        <DatePicker
          date={formData.preferredStartDate || undefined}
          onChange={(date) =>
            updateFormData({
              preferredStartDate: date
            })
          }
        />
        <Checkbox
          checked={formData.urgent}
          label={'kiireellinen'}
          onChange={(checked) =>
            updateFormData({
              urgent: checked
            })
          }
        />
      </FixedSpaceColumn>
    </ContentArea>
  )
})
