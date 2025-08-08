// SPDX-FileCopyrightText: 2017-2023 City of Espoo
//
// SPDX-License-Identifier: LGPL-2.1-or-later

import { FontAwesomeIcon } from '@fortawesome/react-fontawesome'
import orderBy from 'lodash/orderBy'
import React, { useMemo, useState } from 'react'
import styled from 'styled-components'
import { Link } from 'wouter'

import type DateRange from 'lib-common/date-range'
import { openEndedLocalDateRange } from 'lib-common/form/fields'
import { array, object, oneOf, required, value } from 'lib-common/form/form'
import { useBoolean, useForm, useFormFields } from 'lib-common/form/hooks'
import type {
  DocumentTemplateSummary,
  ChildDocumentType
} from 'lib-common/generated/api-types/document'
import { childDocumentTypes } from 'lib-common/generated/api-types/document'
import type {
  DocumentTemplateId,
  UiLanguage
} from 'lib-common/generated/api-types/shared'
import { uiLanguages } from 'lib-common/generated/api-types/shared'
import LocalDate from 'lib-common/local-date'
import { useMutationResult, useQueryResult } from 'lib-common/query'
import AddButton from 'lib-components/atoms/buttons/AddButton'
import { IconOnlyButton } from 'lib-components/atoms/buttons/IconOnlyButton'
import { SelectF } from 'lib-components/atoms/dropdowns/Select'
import { CheckboxF } from 'lib-components/atoms/form/Checkbox'
import MultiSelect from 'lib-components/atoms/form/MultiSelect'
import Container, { ContentArea } from 'lib-components/layout/Container'
import { Table, Tbody, Td, Th, Thead, Tr } from 'lib-components/layout/Table'
import {
  FixedSpaceColumn,
  FixedSpaceRow
} from 'lib-components/layout/flex-helpers'
import { DateRangePickerF } from 'lib-components/molecules/date-picker/DateRangePicker'
import { H1, Label } from 'lib-components/typography'
import { Gap } from 'lib-components/white-space'
import {
  faCheck,
  faCopy,
  faFileExport,
  faFileImport,
  faPen,
  faTimes,
  faTrash
} from 'lib-icons'

import { exportTemplate } from '../../../generated/api-clients/document'
import { useTranslation } from '../../../state/i18n'
import { renderResult } from '../../async-rendering'
import { FlexRow } from '../../common/styled/containers'
import {
  deleteDocumentTemplateMutation,
  documentTemplateSummariesQuery,
  updateDocumentTemplateValidityMutation
} from '../queries'

import TemplateImportModal from './TemplateImportModal'
import type { TemplateModalMode } from './TemplateModal'
import TemplateModal from './TemplateModal'

const validityForm = required(openEndedLocalDateRange())

const ValidityEditor = React.memo(function ValidityEditor({
  id,
  validity,
  onClose
}: {
  id: DocumentTemplateId
  validity: DateRange
  onClose: () => void
}) {
  const { i18n, lang } = useTranslation()
  const { mutateAsync: updateDocumentTemplateValidity, isIdle } =
    useMutationResult(updateDocumentTemplateValidityMutation)
  const form = useForm(
    validityForm,
    () => openEndedLocalDateRange.fromRange(validity),
    i18n.validationErrors
  )

  return (
    <FixedSpaceRow alignItems="center">
      <DateRangePickerF bind={form} locale={lang} />
      <IconOnlyButton
        icon={faCheck}
        aria-label={i18n.common.save}
        onClick={async () => {
          const result = await updateDocumentTemplateValidity({
            templateId: id,
            body: form.value()
          })
          if (result.isSuccess) {
            onClose()
          }
        }}
        disabled={!form.isValid() || !isIdle}
      />
      <IconOnlyButton
        icon={faTimes}
        aria-label={i18n.common.cancel}
        onClick={onClose}
        disabled={!isIdle}
      />
    </FixedSpaceRow>
  )
})

const TemplateRow = React.memo(function TemplateRow({
  template,
  onDuplicate,
  editingValidity,
  onEditValidity,
  onCloseEditValidity
}: {
  template: DocumentTemplateSummary
  onDuplicate: () => void
  editingValidity: boolean
  onEditValidity: () => void
  onCloseEditValidity: () => void
}) {
  const { i18n } = useTranslation()
  const { mutateAsync: deleteDocumentTemplate } = useMutationResult(
    deleteDocumentTemplateMutation
  )
  const exportUrl = useMemo(
    () => exportTemplate({ templateId: template.id }).url.toString(),
    [template.id]
  )

  return (
    <Tr key={template.id} data-qa="template-row">
      <Td data-qa="name">
        <Link to={`/document-templates/${template.id}`}>{template.name}</Link>
      </Td>
      <Td>{i18n.documentTemplates.documentTypes[template.type]}</Td>
      <Td>{i18n.documentTemplates.languages[template.language]}</Td>
      <Td>
        {editingValidity ? (
          <ValidityEditor
            id={template.id}
            validity={template.validity}
            onClose={onCloseEditValidity}
          />
        ) : (
          <FixedSpaceRow alignItems="center">
            <span>{template.validity.format()}</span>
            <IconOnlyButton
              icon={faPen}
              aria-label={i18n.common.edit}
              onClick={onEditValidity}
            />
          </FixedSpaceRow>
        )}
      </Td>
      <Td>{template.documentCount}</Td>
      <Td>
        {template.published
          ? i18n.documentTemplates.templatesPage.published
          : i18n.documentTemplates.templatesPage.draft}
      </Td>
      <Td>
        <FixedSpaceRow>
          <IconOnlyButton
            icon={faCopy}
            aria-label={i18n.common.copy}
            onClick={onDuplicate}
          />
          <IconOnlyButton
            icon={faTrash}
            aria-label={i18n.common.remove}
            disabled={template.published}
            onClick={() => deleteDocumentTemplate({ templateId: template.id })}
          />
          <a data-qa="export" href={exportUrl} target="_blank" rel="noreferrer">
            <FontAwesomeIcon icon={faFileExport} fontSize="20px" />
          </a>
        </FixedSpaceRow>
      </Td>
    </Tr>
  )
})

const FilterLabel = styled(Label)`
  min-width: 180px;
`

const MultiselectStaticWidth = styled.div`
  min-width: 520px;
`

const filtersForm = object({
  active: required(value<boolean>()),
  draft: required(value<boolean>()),
  future: required(value<boolean>()),
  past: required(value<boolean>()),
  type: oneOf<ChildDocumentType>(),
  language: array(value<UiLanguage>())
})

export default React.memo(function DocumentTemplatesPage() {
  const { i18n } = useTranslation()
  const t = i18n.documentTemplates

  const templates = useQueryResult(documentTemplateSummariesQuery())

  const [importModalOpen, { on: openImportModal, off: closeImportModal }] =
    useBoolean(false)
  const [templateModalMode, setTemplateModalMode] =
    useState<TemplateModalMode | null>(null)
  const [editingValidityId, setEditingValidityId] = useState<string | null>(
    null
  )

  const form = useForm(
    filtersForm,
    () => ({
      active: true,
      draft: true,
      future: true,
      past: false,
      type: {
        domValue: '',
        options: childDocumentTypes.map((t) => ({
          value: t,
          domValue: t,
          label: i18n.documentTemplates.documentTypes[t]
        }))
      },
      language: [...uiLanguages]
    }),
    i18n.validationErrors
  )

  const { active, draft, future, past, type, language } = useFormFields(form)

  const filteredTemplates = useMemo(
    () =>
      templates.map((templates) =>
        templates.filter((template) => {
          const today = LocalDate.todayInHelsinkiTz()
          if (
            !active.value() &&
            template.validity.includes(today) &&
            template.published
          )
            return false
          if (!draft.value() && !template.published) return false
          if (
            !future.value() &&
            template.validity.start.isAfter(today) &&
            template.published
          )
            return false
          if (
            !past.value() &&
            template.validity.end &&
            template.validity.end.isBefore(today)
          )
            return false
          if (type.value() !== undefined && type.value() !== template.type)
            return false
          if (
            language.value().length > 0 &&
            !language.value().includes(template.language)
          )
            return false

          return true
        })
      ),
    [templates, active, draft, future, past, type, language]
  )

  return (
    <Container>
      <ContentArea opaque>
        <H1>{t.title}</H1>
        <FixedSpaceColumn>
          <FixedSpaceRow alignItems="center">
            <FilterLabel>
              {i18n.documentTemplates.templatesPage.filters.validity}
            </FilterLabel>
            <FixedSpaceRow>
              <CheckboxF
                bind={active}
                label={i18n.documentTemplates.templatesPage.filters.active}
              />
              <CheckboxF
                bind={draft}
                label={i18n.documentTemplates.templatesPage.filters.draft}
              />
              <CheckboxF
                bind={future}
                label={i18n.documentTemplates.templatesPage.filters.future}
              />
              <CheckboxF
                bind={past}
                label={i18n.documentTemplates.templatesPage.filters.past}
              />
            </FixedSpaceRow>
          </FixedSpaceRow>
          <FixedSpaceRow alignItems="center">
            <FilterLabel>
              {i18n.documentTemplates.templatesPage.filters.type}
            </FilterLabel>
            <SelectF
              bind={type}
              placeholder={i18n.documentTemplates.templatesPage.filters.all}
            />
          </FixedSpaceRow>
          <FixedSpaceRow alignItems="center">
            <FilterLabel>
              {i18n.documentTemplates.templatesPage.filters.language}
            </FilterLabel>
            <MultiselectStaticWidth>
              <MultiSelect
                value={language.state}
                options={uiLanguages}
                onChange={language.set}
                getOptionId={(lang) => lang}
                getOptionLabel={(lang) =>
                  i18n.documentTemplates.languages[lang]
                }
                placeholder={i18n.documentTemplates.templatesPage.filters.all}
              />
            </MultiselectStaticWidth>
          </FixedSpaceRow>
        </FixedSpaceColumn>
        <Gap />
        <FlexRow justifyContent="flex-end">
          <AddButton
            flipped
            onClick={openImportModal}
            icon={faFileImport}
            text={i18n.documentTemplates.templatesPage.import}
            data-qa="import-template"
          />
          <Gap horizontal size="m" />
          <AddButton
            flipped
            onClick={() => setTemplateModalMode({ type: 'new' })}
            text={i18n.documentTemplates.templatesPage.add}
            data-qa="create-template-button"
          />
        </FlexRow>
        {templateModalMode && (
          <TemplateModal
            mode={templateModalMode}
            onClose={() => setTemplateModalMode(null)}
          />
        )}
        {importModalOpen && (
          <TemplateImportModal
            onClose={closeImportModal}
            onContinue={(data) => {
              closeImportModal()
              setTemplateModalMode({ type: 'import', data })
            }}
          />
        )}
        {renderResult(filteredTemplates, (data) => (
          <>
            <Table>
              <Thead>
                <Tr>
                  <Th>{i18n.documentTemplates.templatesPage.name}</Th>
                  <Th>{i18n.documentTemplates.templatesPage.type}</Th>
                  <Th>{i18n.documentTemplates.templatesPage.language}</Th>
                  <Th>{i18n.documentTemplates.templatesPage.validity}</Th>
                  <Th>{i18n.documentTemplates.templatesPage.documentCount}</Th>
                  <Th>{i18n.documentTemplates.templatesPage.status}</Th>
                  <Th />
                </Tr>
              </Thead>
              <Tbody>
                {orderBy(
                  data,
                  [(t) => t.validity.start, (t) => t.name],
                  ['desc', 'asc']
                ).map((template) => (
                  <TemplateRow
                    key={template.id}
                    template={template}
                    onDuplicate={() =>
                      setTemplateModalMode({
                        type: 'duplicate',
                        from: template.id
                      })
                    }
                    editingValidity={editingValidityId === template.id}
                    onEditValidity={() => setEditingValidityId(template.id)}
                    onCloseEditValidity={() => setEditingValidityId(null)}
                  />
                ))}
              </Tbody>
            </Table>
          </>
        ))}
      </ContentArea>
    </Container>
  )
})
