// SPDX-FileCopyrightText: 2017-2021 City of Espoo
//
// SPDX-License-Identifier: LGPL-2.1-or-later

import { Loading, Result } from 'lib-common/api'
import { ClubTerm } from 'lib-common/api-types/units/ClubTerm'
import { useRestApi } from 'lib-common/utils/useRestApi'
import Loader from 'lib-components/atoms/Loader'
import { Label } from 'lib-components/typography'
import { Gap } from 'lib-components/white-space'
import React, { useEffect, useState } from 'react'
import styled from 'styled-components'
import { useTranslation } from '../../../localization'
import { getClubTerms } from '../../api'

const Ul = styled.ul`
  margin: 0;
`

export function ClubTermsInfo() {
  const i18n = useTranslation()

  const [clubTerms, setClubTerms] = useState<Result<ClubTerm[]>>(Loading.of())
  const loadClubTerms = useRestApi(getClubTerms, setClubTerms)
  useEffect(() => loadClubTerms(), [])

  return (
    <>
      <Label>
        {
          i18n.applications.editor.serviceNeed.startDate[
            clubTerms.isSuccess && clubTerms.value.length === 1
              ? 'clubTerm'
              : 'clubTerms'
          ]
        }
      </Label>
      <Gap size="s" />
      {clubTerms.isSuccess && (
        <Ul>
          {clubTerms.value.map(({ term: { end, start } }, i) => (
            <li key={i}>
              {start.format()} – {end.format()}
            </li>
          ))}
        </Ul>
      )}
      {clubTerms.isLoading && <Loader />}
      {clubTerms.isFailure && i18n.common.errors.genericGetError}
      <Gap size="m" />
    </>
  )
}
