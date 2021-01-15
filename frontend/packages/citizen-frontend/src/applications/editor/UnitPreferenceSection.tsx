// SPDX-FileCopyrightText: 2017-2020 City of Espoo
//
// SPDX-License-Identifier: LGPL-2.1-or-later

import React from 'react'
import styled from 'styled-components'
import ReactSelect from 'react-select'
import { ContentArea } from '@evaka/lib-components/src/layout/Container'
import { H2, H3, Label } from '@evaka/lib-components/src/typography'
import Checkbox from '@evaka/lib-components/src/atoms/form/Checkbox'
import { UnitPreferenceFormData } from '~applications/editor/ApplicationFormData'
import {
  FixedSpaceColumn,
  FixedSpaceRow
} from '@evaka/lib-components/src/layout/flex-helpers'
import InputField from '@evaka/lib-components/src/atoms/form/InputField'
import { defaultMargins, Gap } from '@evaka/lib-components/src/white-space'
import HorizontalLine from '@evaka/lib-components/src/atoms/HorizontalLine'

const FlexContainer = styled.div`
  display: flex;
  flex-wrap: wrap;
`

const FlexChild = styled.div`
  margin-right: ${defaultMargins.s};
  margin-bottom: ${defaultMargins.s};
`

export type UnitPreferenceSectionProps = {
  formData: UnitPreferenceFormData
  updateFormData: (update: Partial<UnitPreferenceFormData>) => void
}

export default React.memo(function UnitPreferenceSection({
  formData,
  updateFormData
}: UnitPreferenceSectionProps) {
  return (
    <ContentArea opaque paddingVertical="L">
      <H2>Hakutoive</H2>

      <H3>Haku sisarperusteella</H3>
      <p>
        Lapsella on sisarusperuste samaan varhaiskasvatuspaikkaan, jossa hänen
        sisaruksensa on päätöksentekohetkellä. Sisarukseksi katsotaan kaikki
        samassa osoitteessa asuvat lapset. Tavoitteena on sijoittaa sisarukset
        samaan varhaiskasvatuspaikkaan perheen niin toivoessa. Jos haet paikkaa
        sisaruksille, jotka eivät vielä ole varhaiskasvatuksessa, kirjoita tieto
        lisätietokenttään.
      </p>
      <p>
        Täytä nämä tiedot vain, jos käytät sisarusperustetta, sekä valitse alla
        olevissa hakutoiveissa ensisijaiseksi toiveeksi sama
        varhaiskasvatusyksikkö, jossa lapsen sisarus on.
      </p>

      <Checkbox
        checked={formData.siblingBasis}
        label={
          'Haen ensisijaisesti samaan paikkaan, jossa lapsen sisarus on jo varhaiskasvatuksessa.'
        }
        onChange={(checked) => updateFormData({ siblingBasis: checked })}
      />
      {formData.siblingBasis && (
        <>
          <Gap size={'s'} />
          <FixedSpaceRow spacing={'m'}>
            <FixedSpaceColumn spacing={'xs'}>
              <Label>Sisaruksen etunimet ja sukunimi *</Label>
              <InputField
                value={formData.siblingName}
                onChange={(value) => updateFormData({ siblingName: value })}
                width={'L'}
              />
            </FixedSpaceColumn>
            <FixedSpaceColumn spacing={'xs'}>
              <Label>Sisaruksen henkilötunnus *</Label>
              <InputField
                value={formData.siblingSsn}
                onChange={(value) => updateFormData({ siblingSsn: value })}
              />
            </FixedSpaceColumn>
          </FixedSpaceRow>
        </>
      )}

      <HorizontalLine />

      <H3>Hakutoiveet</H3>

      <p>
        Voit hakea paikkaa 1-3 varhaiskasvatusyksiköstä toivomassasi
        järjestyksessä. Hakutoiveet eivät takaa paikkaa toivotussa yksikössä,
        mutta mahdollisuus toivotun paikan saamiseen kasvaa antamalla useamman
        vaihtoehdon.
      </p>
      <p>
        Näet eri varhaiskasvatusyksiköiden sijainnin valitsemalla ‘Yksiköt
        kartalla’.
      </p>

      <Label>Yksikön kieli</Label>
      <FixedSpaceRow>
        <div>suomi-chip</div>
        <div>ruotsi-chip</div>
      </FixedSpaceRow>

      <Gap size={'s'} />

      <FlexContainer>
        <FlexChild>
          <FixedSpaceColumn spacing={'xs'}>
            <Label>Valitse hakutoiveet *</Label>
            <ReactSelect
              isMulti
              placeholder={'placeholder'}
              value={[]}
              options={[]}
              onChange={() => undefined}
              getOptionValue={() => ''}
              getOptionLabel={() => ''}
            />
          </FixedSpaceColumn>
        </FlexChild>
        <FlexChild>
          <FixedSpaceColumn spacing={'xs'}>
            <Label>Valitsemasi hakutoiveet</Label>
            <div>yksikköboxit</div>
          </FixedSpaceColumn>
        </FlexChild>
      </FlexContainer>
    </ContentArea>
  )
})
