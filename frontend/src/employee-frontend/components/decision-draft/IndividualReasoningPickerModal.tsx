// SPDX-FileCopyrightText: 2017-2026 City of Espoo
//
// SPDX-License-Identifier: LGPL-2.1-or-later

import React from 'react'
import styled from 'styled-components'

import type { DecisionType } from 'lib-common/generated/api-types/decision'
import type {
  DecisionIndividualReasoningId,
  OfficialLanguage
} from 'lib-common/generated/api-types/shared'
import { useQueryResult } from 'lib-common/query'
import { Button } from 'lib-components/atoms/buttons/Button'
import Checkbox from 'lib-components/atoms/form/Checkbox'
import { AlertBox } from 'lib-components/molecules/MessageBoxes'
import BaseModal, {
  ModalButtons
} from 'lib-components/molecules/modals/BaseModal'
import { fontWeights } from 'lib-components/typography'
import { defaultMargins, Gap } from 'lib-components/white-space'
import colors from 'lib-customizations/common'

import { useTranslation } from '../../state/i18n'
import { renderResult } from '../async-rendering'

import { decisionTypeToCollectionType } from './decisionTypeToCollectionType'
import { getIndividualReasoningsQuery } from './queries'

const Subtitle = styled.div`
  color: ${colors.grayscale.g70};
`

const StickyButtons = styled(ModalButtons)`
  position: sticky;
  bottom: 0;
  background: ${colors.grayscale.g0};
  box-shadow: 0 -2px 4px 0 rgba(0, 0, 0, 0.1);
  margin-top: ${defaultMargins.L};
  margin-bottom: 0;
  padding: ${defaultMargins.s} 0;
`

const RowList = styled.div`
  display: flex;
  flex-direction: column;
  gap: 8px;
`

const Row = styled.label<{ $selected: boolean }>`
  display: flex;
  gap: 12px;
  padding: 12px 16px;
  border: 1px solid ${colors.grayscale.g15};
  border-left: 4px solid
    ${({ $selected }) =>
      $selected ? colors.accents.a4violet : colors.grayscale.g15};
  background: ${({ $selected }) =>
    $selected ? `${colors.accents.a4violet}10` : 'white'};
  border-radius: 4px;
  cursor: pointer;
`

const RowBody = styled.div`
  flex: 1;
  display: flex;
  flex-direction: column;
  gap: 4px;
`

const ItemTitle = styled.div`
  font-weight: ${fontWeights.semibold};
`

const TextLabel = styled.div`
  font-weight: ${fontWeights.semibold};
  font-size: 0.9em;
`

interface Props {
  decisionType: DecisionType
  childName: string
  decisionTypeLabel: string
  selectedIds: ReadonlySet<DecisionIndividualReasoningId>
  language: OfficialLanguage
  onChange: (ids: ReadonlySet<DecisionIndividualReasoningId>) => void
  onClose: () => void
}

export default React.memo(function IndividualReasoningPickerModal({
  decisionType,
  childName,
  decisionTypeLabel,
  selectedIds,
  language,
  onChange,
  onClose
}: Props) {
  const { i18n } = useTranslation()
  const collectionType = decisionTypeToCollectionType(decisionType)
  const individualReasonings = useQueryResult(
    getIndividualReasoningsQuery({ collectionType, language })
  )

  return (
    <BaseModal
      title={i18n.decisionDraft.reasonings.modalTitle}
      close={onClose}
      closeLabel={i18n.common.close}
      width="wide"
    >
      <Subtitle>{`${childName} · ${decisionTypeLabel}`}</Subtitle>
      <Gap />
      {renderResult(individualReasonings, (rows) => {
        const eligible = rows
          .filter((r) => r.removedAt === null || selectedIds.has(r.id))
          .sort((a, b) =>
            a.title.localeCompare(b.title, language.toLowerCase())
          )
        return (
          <RowList>
            {eligible.map((r) => {
              const selected = selectedIds.has(r.id)
              return (
                <Row
                  key={r.id}
                  $selected={selected}
                  data-qa={`reasoning-row-${r.id}`}
                >
                  <Checkbox
                    checked={selected}
                    label={r.title}
                    hiddenLabel
                    onChange={(checked: boolean) => {
                      const next = new Set(selectedIds)
                      if (checked) {
                        next.add(r.id)
                      } else {
                        next.delete(r.id)
                      }
                      onChange(next)
                    }}
                  />
                  <RowBody>
                    <ItemTitle>{r.title}</ItemTitle>
                    <TextLabel>
                      {i18n.decisionDraft.reasonings.modalEntryTextLabel}
                    </TextLabel>
                    <span>{r.text}</span>
                    {r.removedAt && (
                      <AlertBox
                        title={i18n.decisionDraft.reasonings.removedFromUse}
                        thin
                        noMargin
                      />
                    )}
                  </RowBody>
                </Row>
              )
            })}
          </RowList>
        )
      })}
      <StickyButtons $justifyContent="center">
        <Button
          primary
          data-qa="modal-closeBtn"
          onClick={onClose}
          text={i18n.common.close}
        />
      </StickyButtons>
    </BaseModal>
  )
})
