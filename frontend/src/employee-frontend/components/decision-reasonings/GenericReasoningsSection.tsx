// SPDX-FileCopyrightText: 2017-2026 City of Espoo
//
// SPDX-License-Identifier: LGPL-2.1-or-later

import { FontAwesomeIcon } from '@fortawesome/react-fontawesome'
import React, { useMemo, useState } from 'react'
import styled from 'styled-components'

import DateRange from 'lib-common/date-range'
import { localDate } from 'lib-common/form/fields'
import { object, required, validated, value } from 'lib-common/form/form'
import { useForm, useFormFields } from 'lib-common/form/hooks'
import { nonBlank } from 'lib-common/form/validators'
import type {
  DecisionGenericReasoning,
  DecisionReasoningCollectionType
} from 'lib-common/generated/api-types/decision'
import type { DecisionGenericReasoningId } from 'lib-common/generated/api-types/shared'
import { first, second, useSelectMutation } from 'lib-common/query'
import { Chip } from 'lib-components/atoms/Chip'
import { Button } from 'lib-components/atoms/buttons/Button'
import { IconOnlyButton } from 'lib-components/atoms/buttons/IconOnlyButton'
import { MutateButton } from 'lib-components/atoms/buttons/MutateButton'
import { TextAreaF } from 'lib-components/atoms/form/TextArea'
import {
  FixedSpaceColumn,
  FixedSpaceRow
} from 'lib-components/layout/flex-helpers'
import { ConfirmedMutation } from 'lib-components/molecules/ConfirmedMutation'
import { DatePickerF } from 'lib-components/molecules/date-picker/DatePicker'
import { H2, H3, Label } from 'lib-components/typography'
import { Gap } from 'lib-components/white-space'
import {
  faChevronDown,
  faChevronRight,
  faPen,
  faPlus,
  faTrash,
  fasInfo
} from 'lib-icons'

import { useTranslation } from '../../state/i18n'

import {
  CollapsibleHeader,
  LanguageGrid,
  PreWrap,
  ReasoningCard
} from './common'
import {
  createGenericReasoningMutation,
  deleteGenericReasoningMutation,
  updateGenericReasoningMutation
} from './queries'

interface Props {
  collectionType: DecisionReasoningCollectionType
  reasonings: DecisionGenericReasoning[]
}

type EditMode =
  | { type: 'none' }
  | { type: 'create' }
  | { type: 'edit'; id: DecisionGenericReasoningId }

const WarningText = styled.span`
  color: ${(p) => p.theme.colors.grayscale.g70};
  font-size: 0.9em;
`

const genericReasoningForm = object({
  validFrom: required(localDate()),
  textFi: validated(required(value<string>()), nonBlank),
  textSv: validated(required(value<string>()), nonBlank)
})

interface GenericReasoningFormProps {
  collectionType: DecisionReasoningCollectionType
  existing?: DecisionGenericReasoning
  onCancel: () => void
  onSuccess: () => void
}

function GenericReasoningForm({
  collectionType,
  existing,
  onCancel,
  onSuccess
}: GenericReasoningFormProps) {
  const { i18n, lang } = useTranslation()
  const t = i18n.decisionReasonings.generic

  const form = useForm(
    genericReasoningForm,
    () => ({
      validFrom: existing
        ? localDate.fromDate(existing.validFrom)
        : localDate.empty(),
      textFi: existing?.textFi ?? '',
      textSv: existing?.textSv ?? ''
    }),
    i18n.validationErrors
  )

  const { validFrom, textFi, textSv } = useFormFields(form)

  const [saveAsNotReadyMutation, saveAsNotReady] = useSelectMutation(
    () => (existing ? first(existing.id) : second()),
    [
      updateGenericReasoningMutation,
      (id) => ({
        id,
        body: { ...form.value(), collectionType, ready: false }
      })
    ],
    [
      createGenericReasoningMutation,
      () => ({ body: { ...form.value(), collectionType, ready: false } })
    ]
  )

  const [saveAndActivateMutation, saveAndActivate] = useSelectMutation(
    () => (existing ? first(existing.id) : second()),
    [
      updateGenericReasoningMutation,
      (id) => ({
        id,
        body: { ...form.value(), collectionType, ready: true }
      })
    ],
    [
      createGenericReasoningMutation,
      () => ({ body: { ...form.value(), collectionType, ready: true } })
    ]
  )

  return (
    <ReasoningCard>
      <FixedSpaceColumn $spacing="s">
        <Label>{t.dateLabel}</Label>
        <DatePickerF
          bind={validFrom}
          locale={lang}
          hideErrorsBeforeTouched
          data-qa="generic-reasoning-valid-from"
        />
      </FixedSpaceColumn>
      <Gap $size="m" />
      <LanguageGrid>
        <FixedSpaceColumn>
          <Label>{i18n.decisionReasonings.fi}</Label>
          <Label>{t.textFi}</Label>
          <TextAreaF
            bind={textFi}
            hideErrorsBeforeTouched
            data-qa="generic-reasoning-text-fi"
          />
        </FixedSpaceColumn>
        <FixedSpaceColumn>
          <Label>{i18n.decisionReasonings.sv}</Label>
          <Label>{t.textSv}</Label>
          <TextAreaF
            bind={textSv}
            hideErrorsBeforeTouched
            data-qa="generic-reasoning-text-sv"
          />
        </FixedSpaceColumn>
      </LanguageGrid>
      <Gap $size="m" />
      <FixedSpaceRow $justifyContent="flex-end">
        <Button
          text={t.cancel}
          onClick={onCancel}
          data-qa="cancel-generic-reasoning-button"
        />
        <MutateButton
          text={t.saveAsNotReady}
          mutation={saveAsNotReadyMutation}
          onClick={saveAsNotReady}
          disabled={!form.isValid()}
          onSuccess={onSuccess}
          data-qa="save-as-not-ready-button"
        />
        <ConfirmedMutation
          buttonText={t.saveAndActivate}
          primary
          mutation={saveAndActivateMutation}
          onClick={saveAndActivate}
          disabled={!form.isValid()}
          onSuccess={onSuccess}
          confirmationTitle={t.saveAndActivateConfirmTitle}
          confirmationText={t.saveAndActivateConfirmText}
          modalIcon={fasInfo}
          modalType="info"
          data-qa="save-and-activate-button"
          data-qa-modal="save-and-activate-modal"
        />
      </FixedSpaceRow>
    </ReasoningCard>
  )
}

interface GenericReasoningCardProps {
  reasoning: DecisionGenericReasoning
  dateSuffix: string
  editMode: EditMode
  onEdit: () => void
  onEditSuccess: () => void
  onEditCancel: () => void
}

function GenericReasoningCard({
  reasoning,
  dateSuffix,
  editMode,
  onEdit,
  onEditSuccess,
  onEditCancel
}: GenericReasoningCardProps) {
  const { i18n } = useTranslation()
  const t = i18n.decisionReasonings.generic

  const isEditing = editMode.type === 'edit' && editMode.id === reasoning.id

  const dateRangeHeading = `${new DateRange(reasoning.validFrom, reasoning.endDate).format()} ${dateSuffix}`

  if (isEditing) {
    return (
      <GenericReasoningForm
        collectionType={reasoning.collectionType}
        existing={reasoning}
        onCancel={onEditCancel}
        onSuccess={onEditSuccess}
      />
    )
  }

  return (
    <ReasoningCard
      data-qa="generic-reasoning-card"
      $state={
        reasoning.outdated
          ? 'inactive'
          : !reasoning.ready
            ? 'warning'
            : 'active'
      }
    >
      <FixedSpaceRow $justifyContent="space-between" $alignItems="center">
        <FixedSpaceRow $alignItems="center" $spacing="s">
          <Chip
            label={
              reasoning.outdated
                ? t.statusOutdated
                : reasoning.ready
                  ? t.statusReady
                  : t.statusNotReady
            }
            size="small"
            colorPalette={
              reasoning.outdated ? 'gray' : reasoning.ready ? 'green' : 'orange'
            }
            data-qa="generic-reasoning-status"
          />
          {!reasoning.ready && !reasoning.outdated && (
            <WarningText>{t.notReadyWarning}</WarningText>
          )}
        </FixedSpaceRow>
        {!reasoning.ready && !reasoning.outdated && (
          <FixedSpaceRow $spacing="s">
            <IconOnlyButton
              icon={faPen}
              onClick={onEdit}
              aria-label={t.edit}
              data-qa="edit-generic-reasoning-button"
            />
            <ConfirmedMutation
              buttonStyle="ICON"
              icon={faTrash}
              buttonAltText={t.delete}
              mutation={deleteGenericReasoningMutation}
              onClick={() => ({ id: reasoning.id })}
              confirmationTitle={t.deleteConfirmTitle}
              confirmationText={t.deleteConfirmText}
              data-qa="delete-generic-reasoning-button"
              data-qa-modal="delete-generic-reasoning-modal"
            />
          </FixedSpaceRow>
        )}
      </FixedSpaceRow>
      <Gap $size="s" />
      <H3 $noMargin>{dateRangeHeading}</H3>
      <Gap $size="s" />
      <LanguageGrid>
        <FixedSpaceColumn>
          <Label>{i18n.decisionReasonings.fi}</Label>
          <PreWrap>{reasoning.textFi}</PreWrap>
        </FixedSpaceColumn>
        <FixedSpaceColumn>
          <Label>{i18n.decisionReasonings.sv}</Label>
          <PreWrap>{reasoning.textSv}</PreWrap>
        </FixedSpaceColumn>
      </LanguageGrid>
    </ReasoningCard>
  )
}

export default React.memo(function GenericReasoningsSection({
  collectionType,
  reasonings
}: Props) {
  const { i18n } = useTranslation()
  const t = i18n.decisionReasonings.generic

  const [editMode, setEditMode] = useState<EditMode>({ type: 'none' })
  const [showOutdated, setShowOutdated] = useState(false)

  const { activeEntries, outdatedEntries } = useMemo(
    () => ({
      activeEntries: reasonings.filter((r) => !r.outdated),
      outdatedEntries: reasonings.filter((r) => r.outdated)
    }),
    [reasonings]
  )

  const closeForm = () => setEditMode({ type: 'none' })

  return (
    <div>
      <FixedSpaceRow $justifyContent="space-between" $alignItems="center">
        <H2>{`${t.title} (${activeEntries.length})`}</H2>
        {editMode.type === 'none' && (
          <Button
            icon={faPlus}
            text={t.addNew}
            onClick={() => setEditMode({ type: 'create' })}
            data-qa="add-generic-reasoning-button"
          />
        )}
      </FixedSpaceRow>

      {editMode.type === 'create' && (
        <>
          <Gap $size="m" />
          <GenericReasoningForm
            collectionType={collectionType}
            onCancel={closeForm}
            onSuccess={closeForm}
          />
        </>
      )}

      <Gap $size="m" />

      <FixedSpaceColumn $spacing="m">
        {activeEntries.map((reasoning) => (
          <GenericReasoningCard
            key={reasoning.id}
            reasoning={reasoning}
            dateSuffix={t.dateSuffix}
            editMode={editMode}
            onEdit={() => setEditMode({ type: 'edit', id: reasoning.id })}
            onEditSuccess={closeForm}
            onEditCancel={closeForm}
          />
        ))}
      </FixedSpaceColumn>

      {outdatedEntries.length > 0 && (
        <>
          <Gap $size="m" />
          <CollapsibleHeader
            onClick={() => setShowOutdated((prev) => !prev)}
            data-qa="toggle-outdated-reasonings"
          >
            <FontAwesomeIcon
              icon={showOutdated ? faChevronDown : faChevronRight}
              size="xs"
            />
            {t.outdated} ({outdatedEntries.length})
          </CollapsibleHeader>
          {showOutdated && (
            <>
              <Gap $size="m" />
              <FixedSpaceColumn $spacing="m">
                {outdatedEntries.map((reasoning) => (
                  <GenericReasoningCard
                    key={reasoning.id}
                    reasoning={reasoning}
                    dateSuffix={t.dateSuffix}
                    editMode={editMode}
                    onEdit={() =>
                      setEditMode({ type: 'edit', id: reasoning.id })
                    }
                    onEditSuccess={closeForm}
                    onEditCancel={closeForm}
                  />
                ))}
              </FixedSpaceColumn>
            </>
          )}
        </>
      )}
    </div>
  )
})
