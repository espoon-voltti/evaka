// SPDX-FileCopyrightText: 2017-2024 City of Espoo
//
// SPDX-License-Identifier: LGPL-2.1-or-later

import React from 'react'

import { wrapResult } from 'lib-common/api'
import { string } from 'lib-common/form/fields'
import { object, validated } from 'lib-common/form/form'
import { useForm, useFormFields } from 'lib-common/form/hooks'
import { nonBlank } from 'lib-common/form/validators'
import { UUID } from 'lib-common/types'
import { InputFieldF } from 'lib-components/atoms/form/InputField'
import { AsyncFormModal } from 'lib-components/molecules/modals/FormModal'
import { Label } from 'lib-components/typography'

import { migrateVasuDocuments } from '../../../generated/api-clients/vasu'
import { useTranslation } from '../../../state/i18n'

const migrateVasuDocumentsResult = wrapResult(migrateVasuDocuments)

const form = object({
  processDefinitionNumber: validated(string(), nonBlank)
})

export default React.memo(function MigrateTemplateModal({
  templateId,
  onClose
}: {
  templateId: UUID
  onClose: () => void
}) {
  const { i18n } = useTranslation()

  const boundForm = useForm(
    form,
    () => ({
      processDefinitionNumber: ''
    }),
    i18n.validationErrors
  )
  const { processDefinitionNumber } = useFormFields(boundForm)

  return (
    <AsyncFormModal
      title={i18n.vasuTemplates.migrateModal.title}
      resolveAction={() =>
        migrateVasuDocumentsResult({
          id: templateId,
          body: { processDefinitionNumber: processDefinitionNumber.value() }
        })
      }
      resolveLabel={i18n.vasuTemplates.migrateModal.resolve}
      resolveDisabled={!boundForm.isValid()}
      onSuccess={onClose}
      rejectAction={onClose}
      rejectLabel={i18n.common.cancel}
    >
      <Label>{i18n.vasuTemplates.migrateModal.processDefinitionNumber}</Label>
      <InputFieldF bind={processDefinitionNumber} />
    </AsyncFormModal>
  )
})
