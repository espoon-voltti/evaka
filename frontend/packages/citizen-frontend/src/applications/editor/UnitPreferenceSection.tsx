// SPDX-FileCopyrightText: 2017-2020 City of Espoo
//
// SPDX-License-Identifier: LGPL-2.1-or-later

import React, { useEffect, useState } from 'react'
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
import { Loading, Result, Success } from '@evaka/lib-common/src/api'
import { PublicUnit } from '@evaka/lib-common/src/api-types/units'
import { useRestApi } from '@evaka/lib-common/src/utils/useRestApi'
import { ApplicationUnitType, getApplicationUnits } from '~applications/api'
import { ApplicationType, PreferredUnit } from '~applications/types'
import LocalDate from '@evaka/lib-common/src/local-date'
import { AlertBox } from '@evaka/lib-components/src/molecules/MessageBoxes'
import { SpinnerSegment } from '@evaka/lib-components/src/atoms/state/Spinner'
import ErrorSegment from '@evaka/lib-components/src/atoms/state/ErrorSegment'
import PreferredUnitBox from '~applications/editor/PreferredUnitBox'

export type UnitPreferenceSectionProps = {
  formData: UnitPreferenceFormData
  updateFormData: (update: Partial<UnitPreferenceFormData>) => void
  applicationType: ApplicationType
  preparatory: boolean
  preferredStartDate: LocalDate | null
}

export default React.memo(function UnitPreferenceSection({
  formData,
  updateFormData,
  applicationType,
  preparatory,
  preferredStartDate
}: UnitPreferenceSectionProps) {
  const [units, setUnits] = useState<Result<PublicUnit[]>>(Loading.of())
  const loadUnits = useRestApi(getApplicationUnits, setUnits)

  useEffect(() => {
    if (!preferredStartDate) {
      setUnits(Success.of([]))
    } else {
      const unitType: ApplicationUnitType =
        applicationType === 'club'
          ? 'CLUB'
          : applicationType === 'daycare'
          ? 'DAYCARE'
          : preparatory
          ? 'PREPARATORY'
          : 'PRESCHOOL'

      loadUnits(unitType, preferredStartDate)
    }
  }, [applicationType, preparatory, preferredStartDate])

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

      {!preferredStartDate ? (
        <div>
          <AlertBox title={'Valitse ensin toivottu aloituspäivä'} />
        </div>
      ) : (
        <>
          <p>
            Voit hakea paikkaa 1-3 varhaiskasvatusyksiköstä toivomassasi
            järjestyksessä. Hakutoiveet eivät takaa paikkaa toivotussa
            yksikössä, mutta mahdollisuus toivotun paikan saamiseen kasvaa
            antamalla useamman vaihtoehdon.
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

          {units.isLoading && <SpinnerSegment />}
          {units.isFailure && <ErrorSegment />}
          {units.isSuccess && (
            <FlexContainer>
              <FlexChild>
                <FixedSpaceColumn spacing={'xs'}>
                  <Label>Valitse hakutoiveet *</Label>
                  <ReactSelect
                    isMulti
                    placeholder={'placeholder'}
                    value={units.value
                      .filter(
                        (u) =>
                          !!formData.preferredUnits.find((u2) => u2.id === u.id)
                      )
                      .map(({ id, name }) => ({ id, name }))}
                    options={units.value.map(({ id, name }) => ({ id, name }))}
                    onChange={(selected) => {
                      if (
                        selected &&
                        'length' in selected &&
                        selected.length > 3
                      )
                        return

                      updateFormData({
                        preferredUnits:
                          selected && 'length' in selected
                            ? selected.map(({ id, name }) => ({ id, name }))
                            : []
                      })
                    }}
                    getOptionValue={(unit: PreferredUnit) => unit.id}
                    getOptionLabel={(unit: PreferredUnit) => unit.name}
                    isClearable={false}
                  />
                </FixedSpaceColumn>
              </FlexChild>
              <FlexChild>
                <FixedSpaceColumn spacing={'s'}>
                  <Label>Valitsemasi hakutoiveet</Label>
                  {formData.preferredUnits
                    .map((u) => units.value.find((u2) => u.id === u2.id))
                    .map((unit, i) =>
                      unit ? (
                        <PreferredUnitBox
                          key={unit.id}
                          unit={unit}
                          n={i + 1}
                          remove={() =>
                            updateFormData({
                              preferredUnits: [
                                ...formData.preferredUnits.slice(0, i),
                                ...formData.preferredUnits.slice(i + 1)
                              ]
                            })
                          }
                          moveUp={
                            i > 0
                              ? () =>
                                  updateFormData({
                                    preferredUnits: [
                                      ...formData.preferredUnits.slice(
                                        0,
                                        i - 1
                                      ),
                                      formData.preferredUnits[i],
                                      formData.preferredUnits[i - 1],
                                      ...formData.preferredUnits.slice(i + 1)
                                    ]
                                  })
                              : null
                          }
                          moveDown={
                            i < formData.preferredUnits.length - 1
                              ? () =>
                                  updateFormData({
                                    preferredUnits: [
                                      ...formData.preferredUnits.slice(0, i),
                                      formData.preferredUnits[i + 1],
                                      formData.preferredUnits[i],
                                      ...formData.preferredUnits.slice(i + 2)
                                    ]
                                  })
                              : null
                          }
                        />
                      ) : null
                    )}
                </FixedSpaceColumn>
              </FlexChild>
            </FlexContainer>
          )}
        </>
      )}
    </ContentArea>
  )
})

const FlexContainer = styled.div`
  display: flex;
  flex-wrap: wrap;
`

const FlexChild = styled.div`
  width: 480px;
  margin-right: ${defaultMargins.m};
  margin-bottom: ${defaultMargins.m};
`
