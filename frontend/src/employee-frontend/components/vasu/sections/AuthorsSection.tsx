// SPDX-FileCopyrightText: 2017-2021 City of Espoo
//
// SPDX-License-Identifier: LGPL-2.1-or-later

import React, { Dispatch, SetStateAction, useEffect } from 'react'
import { ContentArea } from '../../../../lib-components/layout/Container'
import { H2, Label } from '../../../../lib-components/typography'
import { AuthorInfo, AuthorsContent } from '../api'
import { useTranslation } from '../../../state/i18n'
import {
  FixedSpaceColumn,
  FixedSpaceRow
} from 'lib-components/layout/flex-helpers'
import InputField from '../../../../lib-components/atoms/form/InputField'
import { Gap } from 'lib-components/white-space'

interface Props {
  sectionIndex: number
  content: AuthorsContent
  setContent: Dispatch<SetStateAction<AuthorsContent>>
}
export function AuthorsSection({ sectionIndex, content, setContent }: Props) {
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
  }, [content, setContent])

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
            onChange={(value) =>
              setContent((prev) => ({
                ...prev,
                primaryAuthor: {
                  ...prev.primaryAuthor,
                  name: value
                }
              }))
            }
          />
        </FixedSpaceColumn>

        <FixedSpaceColumn spacing="xxs">
          <Label>{t.authorInfo.title}</Label>
          <InputField
            value={content.primaryAuthor.title}
            onChange={(value) =>
              setContent((prev) => ({
                ...prev,
                primaryAuthor: {
                  ...prev.primaryAuthor,
                  title: value
                }
              }))
            }
          />
        </FixedSpaceColumn>

        <FixedSpaceColumn spacing="xxs">
          <Label>{t.authorInfo.phone}</Label>
          <InputField
            value={content.primaryAuthor.phone}
            onChange={(value) =>
              setContent((prev) => ({
                ...prev,
                primaryAuthor: {
                  ...prev.primaryAuthor,
                  phone: value
                }
              }))
            }
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
                onChange={(value) =>
                  setContent((prev) => ({
                    ...prev,
                    otherAuthors: [
                      ...prev.otherAuthors.slice(0, i),
                      {
                        ...prev.otherAuthors[i],
                        name: value
                      },
                      ...prev.otherAuthors.slice(i + 1)
                    ]
                  }))
                }
              />
            </FixedSpaceColumn>

            <FixedSpaceColumn spacing="xxs">
              <Label>{t.authorInfo.title}</Label>
              <InputField
                value={author.title}
                onChange={(value) =>
                  setContent((prev) => ({
                    ...prev,
                    otherAuthors: [
                      ...prev.otherAuthors.slice(0, i),
                      {
                        ...prev.otherAuthors[i],
                        title: value
                      },
                      ...prev.otherAuthors.slice(i + 1)
                    ]
                  }))
                }
              />
            </FixedSpaceColumn>

            <FixedSpaceColumn spacing="xxs">
              <Label>{t.authorInfo.phone}</Label>
              <InputField
                value={author.phone}
                onChange={(value) =>
                  setContent((prev) => ({
                    ...prev,
                    otherAuthors: [
                      ...prev.otherAuthors.slice(0, i),
                      {
                        ...prev.otherAuthors[i],
                        phone: value
                      },
                      ...prev.otherAuthors.slice(i + 1)
                    ]
                  }))
                }
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
