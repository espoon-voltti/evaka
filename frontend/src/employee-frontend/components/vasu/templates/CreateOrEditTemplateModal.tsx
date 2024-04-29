// SPDX-FileCopyrightText: 2017-2022 City of Espoo
//
// SPDX-License-Identifier: LGPL-2.1-or-later

import React, { useContext, useState } from 'react'

import { wrapResult } from 'lib-common/api'
import FiniteDateRange from 'lib-common/finite-date-range'
import {
  OfficialLanguage,
  officialLanguages
} from 'lib-common/generated/api-types/shared'
import {
  CreateTemplateRequest,
  CurriculumTemplateError,
  CurriculumType,
  curriculumTypes,
  VasuTemplateSummary
} from 'lib-common/generated/api-types/vasu'
import LocalDate from 'lib-common/local-date'
import { UUID } from 'lib-common/types'
import Select from 'lib-components/atoms/dropdowns/Select'
import InputField from 'lib-components/atoms/form/InputField'
import { FixedSpaceColumn } from 'lib-components/layout/flex-helpers'
import { DatePickerDeprecated } from 'lib-components/molecules/DatePickerDeprecated'
import FormModal from 'lib-components/molecules/modals/FormModal'
import { Label } from 'lib-components/typography'

import { editTemplate, postTemplate } from '../../../generated/api-clients/vasu'
import { useTranslation } from '../../../state/i18n'
import { UIContext } from '../../../state/ui'

const postTemplateResult = wrapResult(postTemplate)
const editTemplateResult = wrapResult(editTemplate)

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
  const { setErrorMessage } = useContext(UIContext)
  const t = i18n.vasuTemplates
  const [name, setName] = useState(templateToEdit?.name ?? '')
  const [startDate, setStartDate] = useState(
    templateToEdit?.valid.start ?? LocalDate.todayInSystemTz()
  )
  const [endDate, setEndDate] = useState(
    templateToEdit?.valid.end ?? LocalDate.todayInSystemTz().addYears(1)
  )
  const [type, setType] = useState<CurriculumType>(
    templateToEdit?.type ?? 'DAYCARE'
  )
  const [language, setLanguage] = useState<OfficialLanguage>(
    templateToEdit?.language ?? 'FI'
  )
  const [submitting, setSubmitting] = useState(false)

  const apiCall = templateToEdit
    ? (body: CreateTemplateRequest) =>
        editTemplateResult({ id: templateToEdit.id, body }).then((res) =>
          res.map(() => templateToEdit.id)
        )
    : (body: CreateTemplateRequest) => postTemplateResult({ body })

  const isEditableName = !templateToEdit || templateToEdit.documentCount == 0
  const isEditableTypeAndLang = !templateToEdit

  return (
    <FormModal
      title={
        templateToEdit ? t.templateModal.editTitle : t.templateModal.createTitle
      }
      resolveAction={() => {
        setSubmitting(true)
        void apiCall({
          name,
          valid: new FiniteDateRange(startDate, endDate),
          type,
          language
        }).then((res) => {
          setSubmitting(false)
          if (res.isSuccess) {
            onSuccess(res.value)
          } else if (res.isFailure) {
            setErrorMessage({
              resolveLabel: i18n.common.ok,
              // eslint-disable-next-line @typescript-eslint/no-unsafe-assignment
              text:
                (res?.errorCode &&
                  t.errorCodes[res.errorCode as CurriculumTemplateError]) ||
                '',
              title: i18n.common.error.saveFailed,
              type: 'error'
            })
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
          {isEditableName ? (
            <InputField
              value={name}
              onChange={setName}
              data-qa="template-name"
            />
          ) : (
            <span>{name}</span>
          )}
        </FixedSpaceColumn>

        <FixedSpaceColumn spacing="xxs">
          <Label>{t.type}</Label>
          {isEditableTypeAndLang ? (
            <Select
              items={curriculumTypes}
              selectedItem={type}
              onChange={(value) => {
                if (value) setType(value)
              }}
              getItemLabel={(option) => t.types[option]}
              data-qa="select-type"
            />
          ) : (
            <span>{t.types[type]}</span>
          )}
        </FixedSpaceColumn>

        <FixedSpaceColumn spacing="xxs">
          <Label>{t.language}</Label>
          {isEditableTypeAndLang ? (
            <Select
              items={officialLanguages}
              selectedItem={language}
              onChange={(value) => {
                if (value) setLanguage(value)
              }}
              getItemLabel={(option) => t.languages[option]}
            />
          ) : (
            <span>{t.languages[language]}</span>
          )}
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
