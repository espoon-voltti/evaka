// SPDX-FileCopyrightText: 2017-2022 City of Espoo
//
// SPDX-License-Identifier: LGPL-2.1-or-later

import React, { useContext, useEffect, useMemo, useState } from 'react'
import styled, { css } from 'styled-components'

import { useTranslation } from 'employee-frontend/state/i18n'
import { Followup, FollowupEntry } from 'lib-common/api-types/vasu'
import HelsinkiDateTime from 'lib-common/helsinki-date-time'
import LocalDate from 'lib-common/local-date'
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
  date,
  textValue,
  onChange,
  'data-qa': dataQa
}: {
  entry?: FollowupEntry
  date: LocalDate | null
  textValue: string
  onChange: (date: LocalDate | null, textValue: string) => void
  'data-qa': string
}) {
  const { user } = useContext(UserContext)
  const { i18n } = useTranslation()

  const currentUserCaption = useMemo(
    () => `${LocalDate.todayInHelsinkiTz().format()} ${user?.name ?? ''}`,
    [user]
  )

  const [internalDate, setInternalDate] = useState<LocalDate | null>(
    date ?? LocalDate.todayInHelsinkiTz()
  )

  useEffect(() => {
    if (
      date !== null &&
      (internalDate === null || !internalDate.isEqual(date))
    ) {
      setInternalDate(date)
    }
    // eslint-disable-next-line react-hooks/exhaustive-deps
  }, [date])

  return (
    <div>
      <FixedSpaceRow alignItems="flex-end" spacing="s">
        <DatePicker
          locale="fi"
          date={internalDate}
          onChange={(date) => {
            setInternalDate(date)
            if (textValue.length > 0 && date) {
              onChange(date, textValue)
            }
          }}
          info={
            !internalDate
              ? {
                  status: 'warning',
                  text: i18n.validationErrors.required
                }
              : undefined
          }
          errorTexts={i18n.validationErrors}
          hideErrorsBeforeTouched
          data-qa={`${dataQa}-date`}
        />
        <TextArea
          value={textValue}
          onChange={(textValue) =>
            internalDate && onChange(internalDate, textValue)
          }
          data-qa={`${dataQa}-input`}
          placeholder={
            entry ? undefined : i18n.vasu.newFollowUpEntryPlaceholder
          }
        />
      </FixedSpaceRow>
      <Gap size="xxs" />
      {(entry || textValue.length > 0) && (
        <InformationText data-qa={`${dataQa}-meta`}>
          {entry
            ? `${entry.createdDate?.toLocalDate().format() ?? ''} ${
                entry.authorName
              }${
                textValue !== entry.text
                  ? `, ${i18n.vasu.edited} ${currentUserCaption}`
                  : entry.edited
                  ? `, ${i18n.vasu.edited} ${entry.edited.editedAt.format()} ${
                      entry.edited.editorName
                    }`
                  : ''
              }`
            : currentUserCaption}
        </InformationText>
      )}
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
  const entries = useMemo(
    () =>
      [...value, undefined].map((entry) => ({
        entry,
        date: entry?.date ?? null,
        textValue: entry?.text ?? ''
      })),
    [value]
  )

  const { user } = useContext(UserContext)

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
          entries.map(({ entry, date, textValue }, ix) => (
            <FollowupEntryEditor
              key={ix}
              entry={entry}
              date={date}
              textValue={textValue}
              data-qa={`follow-up-${ix}`}
              onChange={(date, text) =>
                onChange?.(
                  entries
                    .map((entry, i) => {
                      if (i !== ix) {
                        return entry.entry
                      }

                      if (entry.entry) {
                        const authorEditingInGracePeriod =
                          !entry.entry.createdDate ||
                          (HelsinkiDateTime.now().timestamp -
                            entry.entry.createdDate.timestamp <
                            15 * 60 * 1000 &&
                            entry.entry.authorId === user?.id)

                        return {
                          ...entry.entry,
                          date,
                          text,
                          edited: !authorEditingInGracePeriod
                            ? {
                                editorId: user?.id,
                                editorName: user?.name,
                                editedAt: LocalDate.todayInHelsinkiTz()
                              }
                            : entry.entry.edited
                        }
                      }

                      if (text.length > 0) {
                        return {
                          date,
                          text,
                          authorId: user?.id,
                          authorName: user?.name,
                          createdDate: HelsinkiDateTime.now()
                        }
                      }

                      return undefined
                    })
                    .filter((entry): entry is FollowupEntry => !!entry)
                )
              }
            />
          ))
        ) : (
          <ul>
            {value.map((entry, ix) => (
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
