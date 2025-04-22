// SPDX-FileCopyrightText: 2017-2025 City of Espoo
//
// SPDX-License-Identifier: LGPL-2.1-or-later

import sortBy from 'lodash/sortBy'
import React, { useCallback, useMemo } from 'react'

import {
  array,
  mapped,
  object,
  required,
  validated,
  value
} from 'lib-common/form/form'
import { useForm, useFormField } from 'lib-common/form/hooks'
import {
  ChildDocumentsCreateRequest,
  DocumentTemplateSummary
} from 'lib-common/generated/api-types/document'
import { GroupId } from 'lib-common/generated/api-types/shared'
import { useQueryResult } from 'lib-common/query'
import Combobox from 'lib-components/atoms/dropdowns/Combobox'
import MultiSelect from 'lib-components/atoms/form/MultiSelect'
import { FixedSpaceColumn } from 'lib-components/layout/flex-helpers'
import { MutateFormModal } from 'lib-components/molecules/modals/FormModal'
import { Label } from 'lib-components/typography'

import { useTranslation } from '../../../../state/i18n'
import { DaycareGroupPlacementDetailed } from '../../../../types/unit'
import { formatPersonName } from '../../../../utils'
import { renderResult } from '../../../async-rendering'

import {
  createDocumentsMutation,
  getActiveTemplatesByGroupIdQuery
} from './queries'

interface Props {
  groupId: GroupId
  placements: DaycareGroupPlacementDetailed[]
  onClose: () => void
}

const formModel = mapped(
  object({
    template: required(value<DocumentTemplateSummary | undefined>()),
    placements: validated(
      array(value<DaycareGroupPlacementDetailed>()),
      (placements) => (placements.length === 0 ? 'required' : undefined)
    )
  }),
  (output): ChildDocumentsCreateRequest => ({
    templateId: output.template.id,
    childIds: output.placements.map(({ child }) => child.id)
  })
)

const selectablePlacement =
  (template: DocumentTemplateSummary, groupId: GroupId) =>
  (placement: DaycareGroupPlacementDetailed) =>
    template.placementTypes.includes(placement.type) &&
    groupId === placement.groupId

export const CreateChildDocumentsModal = (props: Props) => {
  const { i18n } = useTranslation()
  const templatesResult = useQueryResult(
    getActiveTemplatesByGroupIdQuery({
      groupId: props.groupId,
      types: ['CITIZEN_BASIC']
    })
  )
  const form = useForm(
    formModel,
    () => ({
      template: undefined,
      placements: []
    }),
    i18n.validationErrors
  )
  const placementsFormField = useFormField(form, 'placements')
  const setTemplate = useCallback(
    (template: DocumentTemplateSummary | null) => {
      if (template !== null)
        form.update((state) => ({
          template,
          placements: state.placements.filter(
            selectablePlacement(template, props.groupId)
          )
        }))
    },
    [form, props.groupId]
  )
  const placements = useMemo(
    () =>
      sortBy(
        form.state.template !== undefined
          ? props.placements.filter(
              selectablePlacement(form.state.template, props.groupId)
            )
          : [],
        [({ child }) => child.lastName, ({ child }) => child.firstName]
      ),
    [form.state.template, props.groupId, props.placements]
  )

  return (
    <MutateFormModal
      title={i18n.unit.groups.childDocuments.createModal.title}
      resolveMutation={createDocumentsMutation}
      resolveAction={() => ({
        body: form.value()
      })}
      resolveLabel={i18n.common.send}
      resolveDisabled={!form.isValid()}
      onSuccess={props.onClose}
      rejectAction={props.onClose}
      rejectLabel={i18n.common.cancel}
    >
      {renderResult(templatesResult, (templates) => (
        <FixedSpaceColumn>
          <div>
            <Label>
              {i18n.unit.groups.childDocuments.createModal.template}
            </Label>
            <Combobox
              items={templates}
              selectedItem={form.state.template ?? null}
              onChange={setTemplate}
              getItemLabel={(template) => template.name}
              placeholder={i18n.common.select}
              data-qa="create-child-documents-modal-select-template"
              getItemDataQa={(template) =>
                `create-child-documents-modal-select-template-${template.id}`
              }
            />
          </div>
          <div>
            <Label>
              {i18n.unit.groups.childDocuments.createModal.placements}
            </Label>
            <MultiSelect
              options={placements}
              value={placementsFormField.state}
              onChange={placementsFormField.set}
              getOptionId={({ child }) => child.id}
              getOptionLabel={({ child }) =>
                formatPersonName(child, i18n, true)
              }
              placeholder={i18n.common.select}
              data-qa="create-child-documents-modal-select-children"
            />
          </div>
        </FixedSpaceColumn>
      ))}
    </MutateFormModal>
  )
}
