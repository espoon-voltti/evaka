// SPDX-FileCopyrightText: 2017-2021 City of Espoo
//
// SPDX-License-Identifier: LGPL-2.1-or-later

import { FixedSpaceColumn } from 'lib-components/layout/flex-helpers'
import { Label } from 'lib-components/typography'
import React, { useState } from 'react'
import FiniteDateRange from '../../../../lib-common/finite-date-range'
import { UUID } from '../../../../lib-common/types'
import InputField from '../../../../lib-components/atoms/form/InputField'
import { DatePickerDeprecated } from '../../../../lib-components/molecules/DatePickerDeprecated'
import FormModal from '../../../../lib-components/molecules/modals/FormModal'
import { useTranslation } from '../../../state/i18n'
import { copyVasuTemplate, VasuTemplateSummary } from './api'

interface Props {
  template: VasuTemplateSummary
  onSuccess: (templateId: UUID) => void
  onCancel: () => void
}

export default React.memo(function CopyTemplateModal({
  template,
  onCancel,
  onSuccess
}: Props) {
  const { i18n } = useTranslation()
  const t = i18n.vasuTemplates
  const [name, setName] = useState('')
  const [startDate, setStartDate] = useState(template.valid.start.addYears(1))
  const [endDate, setEndDate] = useState(template.valid.end.addYears(1))
  const [submitting, setSubmitting] = useState(false)

  return (
    <FormModal
      title={t.templateModal.copyTitle}
      resolve={{
        action: () => {
          setSubmitting(true)
          void copyVasuTemplate(
            template.id,
            name,
            new FiniteDateRange(startDate, endDate)
          ).then((res) => {
            setSubmitting(false)
            if (res.isSuccess) {
              onSuccess(res.value)
            }
          })
        },
        label: i18n.common.confirm,
        disabled:
          submitting ||
          endDate.isEqualOrBefore(startDate) ||
          name.trim().length === 0
      }}
      reject={{
        action: onCancel,
        label: i18n.common.cancel
      }}
    >
      <FixedSpaceColumn>
        <FixedSpaceColumn spacing="xxs">
          <Label>{t.name}</Label>
          <InputField value={name} onChange={setName} />
        </FixedSpaceColumn>

        <FixedSpaceColumn spacing="xxs">
          <Label>{t.templateModal.validStart}</Label>
          <DatePickerDeprecated date={startDate} onChange={setStartDate} />
        </FixedSpaceColumn>

        <FixedSpaceColumn spacing="xxs">
          <Label>{t.templateModal.validEnd}</Label>
          <DatePickerDeprecated date={endDate} onChange={setEndDate} />
        </FixedSpaceColumn>
      </FixedSpaceColumn>
    </FormModal>
  )
})
