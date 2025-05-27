// SPDX-FileCopyrightText: 2017-2025 City of Espoo
//
// SPDX-License-Identifier: LGPL-2.1-or-later

import React from 'react'
import styled from 'styled-components'

import { Label } from 'lib-components/typography'
import { Gap } from 'lib-components/white-space'

import { useTranslation } from '../../../localization'
import type { Term } from '../ApplicationEditor'

const Ul = styled.ul`
  margin: 0;
`

interface Props {
  preschoolTerms: Term[] | undefined
}

export function PreschoolTermsInfoSection({ preschoolTerms }: Props) {
  const i18n = useTranslation()
  return (
    <>
      <Label>
        {
          i18n.applications.editor.serviceNeed.startDate[
            preschoolTerms?.length === 1 ? 'preschoolTerm' : 'preschoolTerms'
          ]
        }
      </Label>
      <Gap size="s" />
      <Ul data-qa="preschool-terms">
        {preschoolTerms?.map((term, i) => (
          <li key={i}>
            <Label>{`${term.extendedTerm.start.year}-${term.extendedTerm.end.year}`}</Label>
            <p>{`${term.extendedTerm.format()}`}</p>
          </li>
        ))}
      </Ul>
      <Gap size="m" />
    </>
  )
}
