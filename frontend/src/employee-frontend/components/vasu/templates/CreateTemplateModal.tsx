// SPDX-FileCopyrightText: 2017-2021 City of Espoo
//
// SPDX-License-Identifier: LGPL-2.1-or-later

import React, { useState } from 'react'
import FormModal from '../../../../lib-components/molecules/modals/FormModal'
import InputField from '../../../../lib-components/atoms/form/InputField'
import { Label } from 'lib-components/typography'
import { useTranslation } from '../../../state/i18n'
import { FixedSpaceColumn } from 'lib-components/layout/flex-helpers'
import Combobox from '../../../../lib-components/atoms/form/Combobox'
import { UUID } from '../../../../lib-common/types'
import LocalDate from '../../../../lib-common/local-date'
import { createVasuTemplate, VasuLanguage, vasuLanguages } from './api'
import FiniteDateRange from '../../../../lib-common/finite-date-range'
import { DatePickerDeprecated } from '../../../../lib-components/molecules/DatePickerDeprecated'

interface Props {
  onSuccess: (templateId: UUID) => void
  onCancel: () => void
}

export default React.memo(function CreateTemplateModal({
  onCancel,
  onSuccess
}: Props) {
  const { i18n } = useTranslation()
  const t = i18n.vasuTemplates
  const [name, setName] = useState('')
  const [startDate, setStartDate] = useState(LocalDate.today())
  const [endDate, setEndDate] = useState(LocalDate.today().addYears(1))
  const [language, setLanguage] = useState<VasuLanguage>('FI')
  const [submitting, setSubmitting] = useState(false)

  return (
    <FormModal
      title={t.newTemplateModal.title}
      resolve={{
        action: () => {
          setSubmitting(true)
          void createVasuTemplate(
            name,
            new FiniteDateRange(startDate, endDate),
            language
          ).then((res) => {
            setSubmitting(false)
            if (res.isSuccess) {
              onSuccess(res.value)
            }
          })
        },
        label: i18n.common.confirm,
        disabled:
          submitting || endDate.isBefore(startDate) || name.trim().length === 0
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
          <Label>{t.language}</Label>
          <Combobox
            items={[...vasuLanguages]}
            selectedItem={language}
            onChange={(value) => {
              if (value) setLanguage(value)
            }}
            getItemLabel={(option) => t.languages[option]}
          />
        </FixedSpaceColumn>

        <FixedSpaceColumn spacing="xxs">
          <Label>{t.newTemplateModal.validStart}</Label>
          <DatePickerDeprecated date={startDate} onChange={setStartDate} />
        </FixedSpaceColumn>

        <FixedSpaceColumn spacing="xxs">
          <Label>{t.newTemplateModal.validEnd}</Label>
          <DatePickerDeprecated date={endDate} onChange={setEndDate} />
        </FixedSpaceColumn>
      </FixedSpaceColumn>
    </FormModal>
  )
})
