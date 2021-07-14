// SPDX-FileCopyrightText: 2017-2021 City of Espoo
//
// SPDX-License-Identifier: LGPL-2.1-or-later

import {
  FixedSpaceColumn,
  FixedSpaceRow
} from 'lib-components/layout/flex-helpers'
import { Gap } from 'lib-components/white-space'
import React, { Dispatch, SetStateAction, useEffect } from 'react'
import InputField from '../../../../lib-components/atoms/form/InputField'
import { ContentArea } from '../../../../lib-components/layout/Container'
import { H2, Label } from '../../../../lib-components/typography'
import { useTranslation } from '../../../state/i18n'
import { AuthorInfo, AuthorsContent } from '../api'
import { ReadOnlyValue } from '../components/ReadOnlyValue'

interface Props {
  sectionIndex: number
  content: AuthorsContent
  setContent: Dispatch<SetStateAction<AuthorsContent>>
}

export function EditableAuthorsSection({
  sectionIndex,
  content,
  setContent
}: Props) {
  const { i18n } = useTranslation()
  const t = i18n.vasu.staticSections.authors

  useEffect(() => {
    if (
      content.otherAuthors
        .slice(0, content.otherAuthors.length - 1)
        .some((author) => authorIsEmpty(author)) ||
      !authorIsEmpty(content.otherAuthors[content.otherAuthors.length - 1])
    ) {
      setContent((prev) => {
        // remove empty rows except last
        const otherAuthors = prev.otherAuthors.filter(
          (author, i) =>
            !authorIsEmpty(author) || i === prev.otherAuthors.length - 1
        )

        // add empty row at end
        if (!authorIsEmpty(otherAuthors[otherAuthors.length - 1])) {
          otherAuthors.push({ name: '', title: '', phone: '' })
        }

        return { ...prev, otherAuthors }
      })
    }
  }, [content.otherAuthors, setContent])

  const onChangePrimaryAuthor = (key: keyof AuthorInfo) => (value: string) =>
    setContent((prev) => ({
      ...prev,
      primaryAuthor: {
        ...prev.primaryAuthor,
        [key]: value
      }
    }))

  const onChangeOtherAuthor =
    (key: keyof AuthorInfo, i: number) => (value: string) =>
      setContent((prev) => ({
        ...prev,
        otherAuthors: [
          ...prev.otherAuthors.slice(0, i),
          {
            ...prev.otherAuthors[i],
            [key]: value
          },
          ...prev.otherAuthors.slice(i + 1)
        ]
      }))

  return (
    <ContentArea opaque>
      <H2>
        {sectionIndex + 1}. {t.title}
      </H2>

      <Label>
        {sectionIndex + 1}.1 {t.primaryAuthor}
      </Label>
      <Gap size="s" />
      <FixedSpaceRow>
        <FixedSpaceColumn spacing="xxs">
          <Label>{t.authorInfo.name}</Label>
          <InputField
            value={content.primaryAuthor.name}
            onChange={onChangePrimaryAuthor('name')}
          />
        </FixedSpaceColumn>

        <FixedSpaceColumn spacing="xxs">
          <Label>{t.authorInfo.title}</Label>
          <InputField
            value={content.primaryAuthor.title}
            onChange={onChangePrimaryAuthor('title')}
          />
        </FixedSpaceColumn>

        <FixedSpaceColumn spacing="xxs">
          <Label>{t.authorInfo.phone}</Label>
          <InputField
            value={content.primaryAuthor.phone}
            onChange={onChangePrimaryAuthor('phone')}
          />
        </FixedSpaceColumn>
      </FixedSpaceRow>

      <Gap />

      <Label>
        {sectionIndex + 1}.2 {t.otherAuthors}
      </Label>
      <Gap size="s" />

      <FixedSpaceColumn>
        {content.otherAuthors.map((author, i) => (
          <FixedSpaceRow key={`author-${i}`}>
            <FixedSpaceColumn spacing="xxs">
              <Label>{t.authorInfo.name}</Label>
              <InputField
                value={author.name}
                onChange={onChangeOtherAuthor('name', i)}
              />
            </FixedSpaceColumn>

            <FixedSpaceColumn spacing="xxs">
              <Label>{t.authorInfo.title}</Label>
              <InputField
                value={author.title}
                onChange={onChangeOtherAuthor('title', i)}
              />
            </FixedSpaceColumn>

            <FixedSpaceColumn spacing="xxs">
              <Label>{t.authorInfo.phone}</Label>
              <InputField
                value={author.phone}
                onChange={onChangeOtherAuthor('phone', i)}
              />
            </FixedSpaceColumn>
          </FixedSpaceRow>
        ))}
      </FixedSpaceColumn>
    </ContentArea>
  )
}

const authorIsEmpty = (author: AuthorInfo) =>
  author.name.trim() === '' &&
  author.title.trim() === '' &&
  author.phone.trim() === ''

const formatAuthor = ({ name, phone, title }: AuthorInfo) =>
  [name, title ? `(${title}) ` : '', phone].filter(Boolean).join(' ')

export function AuthorsSection({
  sectionIndex,
  content
}: Pick<Props, 'sectionIndex' | 'content'>) {
  const { i18n } = useTranslation()
  const t = i18n.vasu.staticSections.authors

  return (
    <ContentArea opaque>
      <H2>
        {sectionIndex + 1}. {t.title}
      </H2>

      <ReadOnlyValue
        label={`${sectionIndex + 1}.1 ${t.primaryAuthor}`}
        value={formatAuthor(content.primaryAuthor)}
      />

      <Gap />

      <ReadOnlyValue
        label={`${sectionIndex + 1}.2 ${t.otherAuthors}`}
        value={content.otherAuthors
          .filter((a) => !authorIsEmpty(a))
          .map(formatAuthor)
          .join(', ')}
      />
    </ContentArea>
  )
}
