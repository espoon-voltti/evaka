// SPDX-FileCopyrightText: 2017-2021 City of Espoo
//
// SPDX-License-Identifier: LGPL-2.1-or-later

import { FixedSpaceColumn } from 'lib-components/layout/flex-helpers'
import { Label } from 'lib-components/typography'
import React, { useState } from 'react'
import { Failure } from '../../../../lib-common/api'
import FiniteDateRange from '../../../../lib-common/finite-date-range'
import LocalDate from '../../../../lib-common/local-date'
import { UUID } from '../../../../lib-common/types'
import Combobox from '../../../../lib-components/atoms/form/Combobox'
import InputField from '../../../../lib-components/atoms/form/InputField'
import ErrorSegment from '../../../../lib-components/atoms/state/ErrorSegment'
import { DatePickerDeprecated } from '../../../../lib-components/molecules/DatePickerDeprecated'
import FormModal from '../../../../lib-components/molecules/modals/FormModal'
import { useTranslation } from '../../../state/i18n'
import {
  createVasuTemplate,
  editVasuTemplate,
  VasuLanguage,
  vasuLanguages,
  VasuTemplateParams,
  VasuTemplateSummary
} from './api'

interface Props {
  onSuccess: (templateId: UUID) => void
  onCancel: () => void
  template?: VasuTemplateSummary
}

export default React.memo(function CreateOrEditTemplateModal({
  onCancel,
  onSuccess,
  template: templateToEdit
}: Props) {
  const { i18n } = useTranslation()
  const t = i18n.vasuTemplates
  const [name, setName] = useState(templateToEdit?.name ?? '')
  const [startDate, setStartDate] = useState(
    templateToEdit?.valid.start ?? LocalDate.today()
  )
  const [endDate, setEndDate] = useState(
    templateToEdit?.valid.end ?? LocalDate.today().addYears(1)
  )
  const [language, setLanguage] = useState<VasuLanguage>(
    templateToEdit?.language ?? 'FI'
  )
  const [submitting, setSubmitting] = useState(false)
  const [failure, setFailure] = useState<Failure<UUID>>()

  const apiCall = templateToEdit
    ? (params: VasuTemplateParams) =>
        editVasuTemplate(templateToEdit.id, params)
    : createVasuTemplate

  return (
    <FormModal
      title={
        templateToEdit ? t.templateModal.editTitle : t.templateModal.createTitle
      }
      resolve={{
        action: () => {
          setFailure(undefined)
          setSubmitting(true)
          void apiCall({
            name,
            valid: new FiniteDateRange(startDate, endDate),
            language
          }).then((res) => {
            setSubmitting(false)
            if (res.isSuccess) {
              onSuccess(res.value)
            } else if (res.isFailure) {
              setFailure(res)
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
          <Label>{t.templateModal.validStart}</Label>
          <DatePickerDeprecated date={startDate} onChange={setStartDate} />
        </FixedSpaceColumn>

        <FixedSpaceColumn spacing="xxs">
          <Label>{t.templateModal.validEnd}</Label>
          <DatePickerDeprecated date={endDate} onChange={setEndDate} />
        </FixedSpaceColumn>
      </FixedSpaceColumn>
      {failure && (
        <ErrorSegment
          title={i18n.common.checkDates}
          info={
            // eslint-disable-next-line @typescript-eslint/no-unsafe-assignment
            (failure.errorCode &&
              i18n.vasuTemplates.errorCodes[failure.errorCode]) ||
            ''
          }
        />
      )}
    </FormModal>
  )
})
