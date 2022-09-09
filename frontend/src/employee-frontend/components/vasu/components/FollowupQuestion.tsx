// SPDX-FileCopyrightText: 2017-2022 City of Espoo
//
// SPDX-License-Identifier: LGPL-2.1-or-later

import React, { useContext } from 'react'
import styled, { css } from 'styled-components'

import { useTranslation } from 'employee-frontend/state/i18n'
import { Followup, FollowupEntry } from 'lib-common/api-types/vasu'
import HelsinkiDateTime from 'lib-common/helsinki-date-time'
import LocalDate from 'lib-common/local-date'
import AddButton from 'lib-components/atoms/buttons/AddButton'
import TextArea from 'lib-components/atoms/form/TextArea'
import { FixedSpaceRow } from 'lib-components/layout/flex-helpers'
import DatePicker from 'lib-components/molecules/date-picker/DatePicker'
import { H2, InformationText, Label } from 'lib-components/typography'
import { defaultMargins, Gap } from 'lib-components/white-space'
import { colors } from 'lib-customizations/common'
import { VasuTranslations } from 'lib-customizations/employee'

import { UserContext } from '../../../state/user'
import QuestionInfo from '../QuestionInfo'

import { QuestionProps } from './question-props'

const FollowupEntryEditor = React.memo(function FollowupEntryEditor({
  entry,
  onChange,
  'data-qa': dataQa
}: {
  entry: FollowupEntry
  onChange: (date: LocalDate, textValue: string) => void
  'data-qa': string
}) {
  const { i18n } = useTranslation()

  return (
    <div>
      <FixedSpaceRow alignItems="flex-end" spacing="s">
        <DatePicker
          locale="fi"
          required
          date={entry.date}
          onChange={(newDate) => {
            if (newDate !== null) onChange(newDate, entry.text)
          }}
          errorTexts={i18n.validationErrors}
          hideErrorsBeforeTouched
          data-qa={`${dataQa}-date`}
        />
        <TextArea
          value={entry.text}
          onChange={(newTextValue) => onChange(entry.date, newTextValue)}
          data-qa={`${dataQa}-input`}
          placeholder={
            entry ? undefined : i18n.vasu.newFollowUpEntryPlaceholder
          }
        />
      </FixedSpaceRow>
      <Gap size="xxs" />
      <InformationText data-qa={`${dataQa}-meta`}>
        {`${entry.createdDate?.toLocalDate().format() ?? ''} ${
          entry.authorName
        }${
          entry.edited
            ? `, ${i18n.vasu.edited} ${entry.edited.editedAt.format()} ${
                entry.edited.editorName
              }`
            : ''
        }`}
      </InformationText>
    </div>
  )
})

interface FollowupQuestionProps extends QuestionProps<Followup> {
  onChange?: (value: FollowupEntry[]) => void
  translations: VasuTranslations
}

export default React.memo(function FollowupQuestion({
  onChange,
  question: { title, name, value, info, continuesNumbering },
  questionNumber
}: FollowupQuestionProps) {
  const { i18n } = useTranslation()
  const { user } = useContext(UserContext)
  if (!user) return null

  return (
    <FollowupQuestionContainer
      editable={!!onChange}
      data-qa="vasu-followup-question"
    >
      <H2>{title}</H2>
      <QuestionInfo info={info}>
        <Label>{`${
          continuesNumbering ? `${questionNumber} ` : ''
        }${name}`}</Label>
      </QuestionInfo>

      <div>
        {onChange ? (
          <>
            {value.map((entry, ix) => (
              <FollowupEntryEditor
                key={ix}
                entry={entry}
                data-qa={`follow-up-${ix}`}
                onChange={(date, text) =>
                  onChange(
                    value.map((entry, i) => {
                      if (i !== ix) {
                        return entry
                      }

                      const now = HelsinkiDateTime.now()
                      const today = now.toLocalDate()

                      const isAuthor = entry.authorId === user.id
                      const inGracePeriod =
                        !entry.createdDate ||
                        now.isBefore(entry.createdDate.addMinutes(15))

                      return {
                        ...entry,
                        date,
                        text,
                        edited:
                          !isAuthor || !inGracePeriod
                            ? {
                                editorId: user.id,
                                editorName: user.name,
                                editedAt: today
                              }
                            : entry.edited
                      }
                    })
                  )
                }
              />
            ))}
            <Gap size="s" />
            <AddButton
              text={i18n.common.add}
              onClick={() =>
                onChange([
                  ...value,
                  {
                    date: LocalDate.todayInHelsinkiTz(),
                    text: '',
                    authorId: user.id,
                    authorName: user.name,
                    createdDate: HelsinkiDateTime.now()
                  }
                ])
              }
              data-qa="followup-add-btn"
            />
          </>
        ) : (
          <ul>
            {/* Render items with non-empty text. It's not allowed to remove an item
            to retain the creator/editor info. */}
            {value
              .filter((entry) => entry.text.trim() !== '')
              .map((entry, ix) => (
                <li key={ix} data-qa="follow-up-entry">
                  {entry.date.format()}: {entry.text}
                </li>
              ))}
          </ul>
        )}
      </div>
    </FollowupQuestionContainer>
  )
})

const FollowupQuestionContainer = styled.div<{ editable: boolean }>`
  ${(p) =>
    p.editable &&
    css`
      border-left: 5px solid ${colors.main.m1};
      box-shadow: 0px -4px 4px rgba(15, 15, 15, 0.1);
      width: calc(100% + 2 * ${defaultMargins.L});
      padding: ${defaultMargins.L};
      padding-left: calc(${defaultMargins.L} - 5px);
      position: relative;
      left: -${defaultMargins.L};
    `}
`
