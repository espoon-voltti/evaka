// SPDX-FileCopyrightText: 2017-2022 City of Espoo
//
// SPDX-License-Identifier: LGPL-2.1-or-later

import React, { useState } from 'react'

import FiniteDateRange from 'lib-common/finite-date-range'
import { VasuTemplateSummary } from 'lib-common/generated/api-types/vasu'
import { UUID } from 'lib-common/types'
import InputField from 'lib-components/atoms/form/InputField'
import { FixedSpaceColumn } from 'lib-components/layout/flex-helpers'
import DatePicker from 'lib-components/molecules/date-picker/DatePicker'
import FormModal from 'lib-components/molecules/modals/FormModal'
import { Label } from 'lib-components/typography'

import { useTranslation } from '../../../state/i18n'

import { copyVasuTemplate } from './api'

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
      resolveAction={() => {
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
      }}
      resolveLabel={i18n.common.confirm}
      resolveDisabled={
        submitting ||
        endDate.isEqualOrBefore(startDate) ||
        name.trim().length === 0
      }
      rejectAction={onCancel}
      rejectLabel={i18n.common.cancel}
    >
      <FixedSpaceColumn>
        <FixedSpaceColumn spacing="xxs">
          <Label>{t.name}</Label>
          <InputField value={name} onChange={setName} />
        </FixedSpaceColumn>

        <FixedSpaceColumn spacing="xxs">
          <Label id="template-valid-start">{t.templateModal.validStart}</Label>
          <DatePicker
            date={startDate}
            onChange={(date) => date && setStartDate(date)}
            labels={i18n.common.datePicker}
            locale="fi"
            errorTexts={i18n.validationErrors}
            aria-labelledby="template-valid-start"
          />
        </FixedSpaceColumn>

        <FixedSpaceColumn spacing="xxs">
          <Label id="template-valid-end">{t.templateModal.validEnd}</Label>
          <DatePicker
            date={endDate}
            onChange={(date) => date && setEndDate(date)}
            labels={i18n.common.datePicker}
            locale="fi"
            errorTexts={i18n.validationErrors}
            aria-labelledby="template-valid-end"
          />
        </FixedSpaceColumn>
      </FixedSpaceColumn>
    </FormModal>
  )
})
