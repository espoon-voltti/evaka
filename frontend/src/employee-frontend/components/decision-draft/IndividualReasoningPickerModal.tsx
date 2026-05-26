// SPDX-FileCopyrightText: 2017-2026 City of Espoo
//
// SPDX-License-Identifier: LGPL-2.1-or-later

import React from 'react'
import styled from 'styled-components'

import type {
  DecisionIndividualReasoning,
  DecisionType
} from 'lib-common/generated/api-types/decision'
import type {
  DecisionId,
  DecisionIndividualReasoningId
} from 'lib-common/generated/api-types/shared'
import { useMutationResult, useQueryResult } from 'lib-common/query'
import { Button } from 'lib-components/atoms/buttons/Button'
import Checkbox from 'lib-components/atoms/form/Checkbox'
import { PlainModal } from 'lib-components/molecules/modals/BaseModal'
import { fontWeights } from 'lib-components/typography'
import colors from 'lib-customizations/common'

import { useTranslation } from '../../state/i18n'
import { renderResult } from '../async-rendering'

import { decisionTypeToCollectionType } from './decisionTypeToCollectionType'
import {
  eligibleIndividualReasoningsQuery,
  linkIndividualReasoningMutation,
  unlinkIndividualReasoningMutation
} from './queries'

const ModalShell = styled.div`
  padding: 24px;
  display: flex;
  flex-direction: column;
  gap: 16px;
`

const HeadingRow = styled.div`
  display: flex;
  flex-direction: column;
  gap: 4px;
`

const HeadingTitle = styled.h2`
  margin: 0;
`

const Subtitle = styled.div`
  color: ${colors.grayscale.g70};
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

const Footer = styled.div`
  display: flex;
  justify-content: flex-end;
`

interface Props {
  decisionId: DecisionId
  decisionType: DecisionType
  childName: string
  decisionTypeLabel: string
  linkedIds: ReadonlySet<DecisionIndividualReasoningId>
  onClose: () => void
}

export default React.memo(function IndividualReasoningPickerModal({
  decisionId,
  decisionType,
  childName,
  decisionTypeLabel,
  linkedIds,
  onClose
}: Props) {
  const { i18n, lang } = useTranslation()
  const collectionType = decisionTypeToCollectionType(decisionType)
  const eligibleResult = useQueryResult(
    eligibleIndividualReasoningsQuery({ collectionType })
  )

  const { mutateAsync: link } = useMutationResult(
    linkIndividualReasoningMutation
  )
  const { mutateAsync: unlink } = useMutationResult(
    unlinkIndividualReasoningMutation
  )

  return (
    <PlainModal margin="auto" width="wide" onEscapeKey={onClose}>
      <ModalShell>
        <HeadingRow>
          <HeadingTitle>
            {i18n.decisionDraft.reasonings.modalTitle}
          </HeadingTitle>
          <Subtitle>{`${childName} · ${decisionTypeLabel}`}</Subtitle>
        </HeadingRow>
        {renderResult(eligibleResult, (rows) => {
          const text = (r: DecisionIndividualReasoning) =>
            lang === 'sv' ? r.textSv : r.textFi
          const title = (r: DecisionIndividualReasoning) =>
            lang === 'sv' ? r.titleSv : r.titleFi
          const eligible = rows
            .filter((r) => r.removedAt === null)
            .sort((a, b) => title(a).localeCompare(title(b), lang))
          return (
            <RowList>
              {eligible.map((r) => {
                const selected = linkedIds.has(r.id)
                return (
                  <Row
                    key={r.id}
                    $selected={selected}
                    data-qa={`reasoning-row-${r.id}`}
                  >
                    <Checkbox
                      checked={selected}
                      label={title(r)}
                      hiddenLabel
                      onChange={(checked: boolean) => {
                        void (checked
                          ? link({
                              id: decisionId,
                              body: { reasoningId: r.id }
                            })
                          : unlink({ id: decisionId, reasoningId: r.id }))
                      }}
                    />
                    <RowBody>
                      <ItemTitle>{title(r)}</ItemTitle>
                      <TextLabel>
                        {i18n.decisionDraft.reasonings.modalEntryTextLabel}
                      </TextLabel>
                      <span>{text(r)}</span>
                    </RowBody>
                  </Row>
                )
              })}
            </RowList>
          )
        })}
        <Footer>
          <Button
            primary
            onClick={onClose}
            text={i18n.decisionDraft.reasonings.modalCloseButton}
            data-qa="picker-close"
          />
        </Footer>
      </ModalShell>
    </PlainModal>
  )
})
