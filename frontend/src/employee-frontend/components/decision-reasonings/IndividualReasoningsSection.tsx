// SPDX-FileCopyrightText: 2017-2026 City of Espoo
//
// SPDX-License-Identifier: LGPL-2.1-or-later

import { FontAwesomeIcon } from '@fortawesome/react-fontawesome'
import React, { useMemo, useState } from 'react'
import styled from 'styled-components'

import { object, required, validated, value } from 'lib-common/form/form'
import { useForm, useFormFields } from 'lib-common/form/hooks'
import { nonBlank } from 'lib-common/form/validators'
import type {
  DecisionIndividualReasoning,
  DecisionReasoningCollectionType
} from 'lib-common/generated/api-types/decision'
import { Chip } from 'lib-components/atoms/Chip'
import { Button } from 'lib-components/atoms/buttons/Button'
import { InputFieldF } from 'lib-components/atoms/form/InputField'
import { TextAreaF } from 'lib-components/atoms/form/TextArea'
import {
  FixedSpaceColumn,
  FixedSpaceRow
} from 'lib-components/layout/flex-helpers'
import { ConfirmedMutation } from 'lib-components/molecules/ConfirmedMutation'
import { H2, Label } from 'lib-components/typography'
import { Gap } from 'lib-components/white-space'
import {
  faChevronDown,
  faChevronRight,
  faPlus,
  fasInfo,
  faTrash
} from 'lib-icons'

import { useTranslation } from '../../state/i18n'

import { CollapsibleHeader, LanguageGrid, ReasoningCard } from './common'
import {
  createIndividualReasoningMutation,
  removeIndividualReasoningMutation
} from './queries'

interface Props {
  collectionType: DecisionReasoningCollectionType
  reasonings: DecisionIndividualReasoning[]
}

const ReasoningTitle = styled.span`
  font-weight: 600;
`

const individualReasoningForm = object({
  titleFi: validated(required(value<string>()), nonBlank),
  titleSv: validated(required(value<string>()), nonBlank),
  textFi: validated(required(value<string>()), nonBlank),
  textSv: validated(required(value<string>()), nonBlank)
})

interface IndividualReasoningCreateFormProps {
  collectionType: DecisionReasoningCollectionType
  onSuccess: () => void
  onCancel: () => void
}

function IndividualReasoningCreateForm({
  collectionType,
  onSuccess,
  onCancel
}: IndividualReasoningCreateFormProps) {
  const { i18n } = useTranslation()
  const t = i18n.decisionReasonings.individual

  const form = useForm(
    individualReasoningForm,
    () => ({ titleFi: '', titleSv: '', textFi: '', textSv: '' }),
    i18n.validationErrors
  )

  const { titleFi, titleSv, textFi, textSv } = useFormFields(form)

  const cancelCreate = () => {
    form.set({ titleFi: '', titleSv: '', textFi: '', textSv: '' })
    onCancel()
  }

  return (
    <ReasoningCard>
      <LanguageGrid>
        <FixedSpaceColumn>
          <Label>{i18n.decisionReasonings.fi}</Label>
          <Label>{t.titleFi}</Label>
          <InputFieldF
            bind={titleFi}
            hideErrorsBeforeTouched
            data-qa="individual-reasoning-title-fi"
          />
          <Label>{t.textFi}</Label>
          <TextAreaF
            bind={textFi}
            hideErrorsBeforeTouched
            data-qa="individual-reasoning-text-fi"
          />
        </FixedSpaceColumn>
        <FixedSpaceColumn>
          <Label>{i18n.decisionReasonings.sv}</Label>
          <Label>{t.titleSv}</Label>
          <InputFieldF
            bind={titleSv}
            hideErrorsBeforeTouched
            data-qa="individual-reasoning-title-sv"
          />
          <Label>{t.textSv}</Label>
          <TextAreaF
            bind={textSv}
            hideErrorsBeforeTouched
            data-qa="individual-reasoning-text-sv"
          />
        </FixedSpaceColumn>
      </LanguageGrid>
      <Gap $size="m" />
      <FixedSpaceRow $justifyContent="flex-end">
        <Button
          appearance="inline"
          text={t.cancel}
          onClick={cancelCreate}
          data-qa="cancel-individual-reasoning-button"
        />
        <ConfirmedMutation
          buttonText={t.saveAndActivate}
          primary
          mutation={createIndividualReasoningMutation}
          onClick={() => ({
            body: { ...form.value(), collectionType }
          })}
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

export default React.memo(function IndividualReasoningsSection({
  collectionType,
  reasonings
}: Props) {
  const { i18n } = useTranslation()
  const t = i18n.decisionReasonings.individual
  const [showCreateForm, setShowCreateForm] = useState(false)
  const [showRemoved, setShowRemoved] = useState(false)

  const { activeReasonings, removedReasonings } = useMemo(() => {
    const sorted = [...reasonings].sort((a, b) =>
      a.createdAt.isBefore(b.createdAt)
        ? 1
        : a.createdAt.isEqual(b.createdAt)
          ? 0
          : -1
    )
    return {
      activeReasonings: sorted.filter((r) => r.removedAt === null),
      removedReasonings: sorted.filter((r) => r.removedAt !== null)
    }
  }, [reasonings])

  return (
    <div>
      <FixedSpaceRow $justifyContent="space-between" $alignItems="center">
        <H2>{`${t.title} (${activeReasonings.length})`}</H2>
        {!showCreateForm && (
          <Button
            icon={faPlus}
            text={t.addNew}
            onClick={() => setShowCreateForm(true)}
            data-qa="add-individual-reasoning-button"
          />
        )}
      </FixedSpaceRow>

      {showCreateForm && (
        <>
          <Gap $size="m" />
          <IndividualReasoningCreateForm
            collectionType={collectionType}
            onSuccess={() => setShowCreateForm(false)}
            onCancel={() => setShowCreateForm(false)}
          />
        </>
      )}

      <Gap $size="m" />

      <FixedSpaceColumn $spacing="m">
        {activeReasonings.map((reasoning) => (
          <ReasoningCard key={reasoning.id} data-qa="individual-reasoning-card">
            <FixedSpaceRow $justifyContent="space-between" $alignItems="center">
              <Chip
                label={t.statusActive}
                size="small"
                colorPalette="green"
                data-qa="individual-reasoning-status"
              />
              <ConfirmedMutation
                buttonStyle="ICON"
                icon={faTrash}
                buttonAltText={t.removeConfirmTitle}
                mutation={removeIndividualReasoningMutation}
                onClick={() => ({ id: reasoning.id })}
                confirmationTitle={t.removeConfirmTitle}
                confirmationText={t.removeConfirmText}
                data-qa="remove-individual-reasoning-button"
                data-qa-modal="remove-individual-reasoning-modal"
              />
            </FixedSpaceRow>
            <Gap $size="s" />
            <LanguageGrid>
              <FixedSpaceColumn>
                <Label>{i18n.decisionReasonings.fi}</Label>
                <ReasoningTitle>{reasoning.titleFi}</ReasoningTitle>
                <span>{reasoning.textFi}</span>
              </FixedSpaceColumn>
              <FixedSpaceColumn>
                <Label>{i18n.decisionReasonings.sv}</Label>
                <ReasoningTitle>{reasoning.titleSv}</ReasoningTitle>
                <span>{reasoning.textSv}</span>
              </FixedSpaceColumn>
            </LanguageGrid>
          </ReasoningCard>
        ))}
      </FixedSpaceColumn>

      {removedReasonings.length > 0 && (
        <>
          <Gap $size="m" />
          <CollapsibleHeader
            onClick={() => setShowRemoved((prev) => !prev)}
            data-qa="toggle-removed-reasonings"
          >
            <FontAwesomeIcon
              icon={showRemoved ? faChevronDown : faChevronRight}
              size="xs"
            />
            {t.removed} ({removedReasonings.length})
          </CollapsibleHeader>
          {showRemoved && (
            <>
              <Gap $size="m" />
              <FixedSpaceColumn $spacing="m">
                {removedReasonings.map((reasoning) => (
                  <ReasoningCard
                    key={reasoning.id}
                    $state="inactive"
                    data-qa="individual-reasoning-card"
                  >
                    <Chip
                      label={t.statusRemoved}
                      size="small"
                      colorPalette="gray"
                      data-qa="individual-reasoning-status"
                    />
                    <Gap $size="s" />
                    <LanguageGrid>
                      <FixedSpaceColumn>
                        <Label>{i18n.decisionReasonings.fi}</Label>
                        <ReasoningTitle>{reasoning.titleFi}</ReasoningTitle>
                        <span>{reasoning.textFi}</span>
                      </FixedSpaceColumn>
                      <FixedSpaceColumn>
                        <Label>{i18n.decisionReasonings.sv}</Label>
                        <ReasoningTitle>{reasoning.titleSv}</ReasoningTitle>
                        <span>{reasoning.textSv}</span>
                      </FixedSpaceColumn>
                    </LanguageGrid>
                  </ReasoningCard>
                ))}
              </FixedSpaceColumn>
            </>
          )}
        </>
      )}
    </div>
  )
})
