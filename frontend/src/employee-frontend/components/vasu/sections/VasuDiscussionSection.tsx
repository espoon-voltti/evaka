// SPDX-FileCopyrightText: 2017-2021 City of Espoo
//
// SPDX-License-Identifier: LGPL-2.1-or-later

import React, { Dispatch, SetStateAction } from 'react'
import { ContentArea } from '../../../../lib-components/layout/Container'
import { H2, H3, Label } from '../../../../lib-components/typography'
import { VasuDiscussionContent } from '../api'
import TextArea from '../../../../lib-components/atoms/form/TextArea'
import { Gap } from 'lib-components/white-space'
import { DatePickerClearableDeprecated } from '../../../../lib-components/molecules/DatePickerDeprecated'
import { useTranslation } from '../../../state/i18n'

interface Props {
  content: VasuDiscussionContent
  setContent: Dispatch<SetStateAction<VasuDiscussionContent>>
}
export function VasuDiscussionSection({ content, setContent }: Props) {
  const { i18n } = useTranslation()
  const t = i18n.vasu.staticSections.vasuDiscussion
  return (
    <ContentArea opaque>
      <H2>{t.title}</H2>

      <H3>{t.title2}</H3>

      <Label>{t.discussionDate}</Label>
      <DatePickerClearableDeprecated
        date={content.discussionDate ?? undefined}
        onChange={(date) =>
          setContent((prev) => ({
            ...prev,
            discussionDate: date
          }))
        }
        onCleared={() =>
          setContent((prev) => ({
            ...prev,
            discussionDate: null
          }))
        }
        type="short"
      />

      <Gap />

      <Label>{t.participants}</Label>
      <TextArea
        value={content.participants}
        onChange={(e) =>
          setContent((prev) => ({
            ...prev,
            participants: e.target.value
          }))
        }
      />

      <Gap />

      <Label>{t.guardianViewsAndCollaboration}</Label>
      <TextArea
        value={content.guardianViewsAndCollaboration}
        onChange={(e) =>
          setContent((prev) => ({
            ...prev,
            guardianViewsAndCollaboration: e.target.value
          }))
        }
      />
    </ContentArea>
  )
}
