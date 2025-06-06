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
import type { BoundForm } from 'lib-common/form/hooks'
import { useForm, useFormField } from 'lib-common/form/hooks'
import type {
  ChildDocumentsCreateRequest,
  DocumentTemplateSummary
} from 'lib-common/generated/api-types/document'
import type { ChildId, GroupId } from 'lib-common/generated/api-types/shared'
import LocalDate from 'lib-common/local-date'
import { formatPersonName } from 'lib-common/names'
import { constantQuery, useQueryResult } from 'lib-common/query'
import Combobox from 'lib-components/atoms/dropdowns/Combobox'
import MultiSelect from 'lib-components/atoms/form/MultiSelect'
import { FixedSpaceColumn } from 'lib-components/layout/flex-helpers'
import { MutateFormModal } from 'lib-components/molecules/modals/FormModal'
import { Label } from 'lib-components/typography'

import { useTranslation } from '../../../../state/i18n'
import type { DaycareGroupPlacementDetailed } from '../../../../types/unit'
import { renderResult } from '../../../async-rendering'

import {
  createDocumentsMutation,
  getActiveTemplatesByGroupIdQuery,
  getNonCompletedChildDocumentChildIdsQuery
} from './queries'

interface Props {
  groupId: GroupId
  placements: DaycareGroupPlacementDetailed[]
  onClose: () => void
}

const placementsModel = validated(
  array(value<DaycareGroupPlacementDetailed>()),
  (placements) => (placements.length === 0 ? 'required' : undefined)
)

const formModel = mapped(
  object({
    template: required(value<DocumentTemplateSummary | undefined>()),
    placements: placementsModel
  }),
  (output): ChildDocumentsCreateRequest => ({
    templateId: output.template.id,
    childIds: output.placements.map(({ child }) => child.id)
  })
)

const selectablePlacement =
  (
    template: DocumentTemplateSummary,
    groupId: GroupId,
    nonCompleted: ChildId[]
  ) =>
  (placement: DaycareGroupPlacementDetailed) =>
    template.placementTypes.includes(placement.type) &&
    groupId === placement.groupId &&
    LocalDate.todayInHelsinkiTz().isBetween(
      placement.startDate,
      placement.endDate
    ) &&
    !nonCompleted.includes(placement.child.id)

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
  const templateFormField = useFormField(form, 'template')
  const placementsFormField = useFormField(form, 'placements')
  const nonCompletedChildDocumentChildIdsResult = useQueryResult(
    templateFormField.isValid()
      ? getNonCompletedChildDocumentChildIdsQuery({
          templateId: templateFormField.value().id,
          groupId: props.groupId
        })
      : constantQuery([])
  )
  const setTemplate = useCallback(
    (template: DocumentTemplateSummary | null) => {
      if (template !== null) form.set({ template, placements: [] })
    },
    [form]
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
              selectedItem={templateFormField.state ?? null}
              onChange={setTemplate}
              getItemLabel={(template) => template.name}
              placeholder={i18n.common.select}
              data-qa="create-child-documents-modal-select-template"
              getItemDataQa={(template) =>
                `create-child-documents-modal-select-template-${template.id}`
              }
            />
          </div>
          {templateFormField.isValid() &&
            renderResult(
              nonCompletedChildDocumentChildIdsResult,
              (nonCompletedChildDocumentChildIds) => (
                <PlacementsSelect
                  template={templateFormField.value()}
                  groupId={props.groupId}
                  nonCompletedChildDocumentChildIds={
                    nonCompletedChildDocumentChildIds
                  }
                  placements={props.placements}
                  placementsFormField={placementsFormField}
                />
              )
            )}
        </FixedSpaceColumn>
      ))}
    </MutateFormModal>
  )
}

const PlacementsSelect = (props: {
  template: DocumentTemplateSummary
  groupId: GroupId
  nonCompletedChildDocumentChildIds: ChildId[]
  placements: DaycareGroupPlacementDetailed[]
  placementsFormField: BoundForm<typeof placementsModel>
}) => {
  const { i18n } = useTranslation()
  const placements = useMemo(
    () =>
      sortBy(
        props.placements.filter(
          selectablePlacement(
            props.template,
            props.groupId,
            props.nonCompletedChildDocumentChildIds
          )
        ),
        [({ child }) => child.lastName, ({ child }) => child.firstName]
      ),
    [
      props.groupId,
      props.nonCompletedChildDocumentChildIds,
      props.placements,
      props.template
    ]
  )

  return (
    <div>
      <Label>{i18n.unit.groups.childDocuments.createModal.placements}</Label>
      <MultiSelect
        options={placements}
        value={props.placementsFormField.state}
        onChange={props.placementsFormField.set}
        getOptionId={({ child }) => child.id}
        getOptionLabel={({ child }) => formatPersonName(child, 'Last First')}
        placeholder={i18n.common.select}
        data-qa="create-child-documents-modal-select-children"
      />
    </div>
  )
}
