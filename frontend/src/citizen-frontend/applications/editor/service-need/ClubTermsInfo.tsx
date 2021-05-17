// SPDX-FileCopyrightText: 2017-2021 City of Espoo
//
// SPDX-License-Identifier: LGPL-2.1-or-later

import { Label } from 'lib-components/typography'
import { Gap } from 'lib-components/white-space'
import React from 'react'
import styled from 'styled-components'
import { useTranslation } from '../../../localization'
import { Term } from '../validations'

const Ul = styled.ul`
  margin: 0;
`

interface Props {
  clubTerms: Term[]
}

export function ClubTermsInfo({ clubTerms }: Props) {
  const i18n = useTranslation()

  return (
    <>
      <Label>
        {
          i18n.applications.editor.serviceNeed.startDate[
            clubTerms.length === 1 ? 'clubTerm' : 'clubTerms'
          ]
        }
      </Label>
      <Gap size="s" />
      <Ul data-qa="club-terms">
        {clubTerms.map(({ end, start }, i) => (
          <li key={i}>
            {start.format()} – {end.format()}
          </li>
        ))}
      </Ul>
      <Gap size="m" />
    </>
  )
}
