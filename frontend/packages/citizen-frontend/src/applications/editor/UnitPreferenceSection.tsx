// SPDX-FileCopyrightText: 2017-2020 City of Espoo
//
// SPDX-License-Identifier: LGPL-2.1-or-later

import React, { useEffect, useState } from 'react'
import styled from 'styled-components'
import ReactSelect from 'react-select'
import { ContentArea } from '@evaka/lib-components/src/layout/Container'
import { H2, H3, Label } from '@evaka/lib-components/src/typography'
import Checkbox, {
  StaticCheckBox
} from '@evaka/lib-components/src/atoms/form/Checkbox'
import { UnitPreferenceFormData } from '~applications/editor/ApplicationFormData'
import {
  FixedSpaceColumn,
  FixedSpaceRow,
  FixedSpaceFlexWrap
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
import { SelectionChip } from '@evaka/lib-components/src/atoms/Chip'
import colors from '@evaka/lib-components/src/colors'

const maxUnits = 3

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
  const [displayFinnish, setDisplayFinnish] = useState(true)
  const [displaySwedish, setDisplaySwedish] = useState(false)

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
          <AlertBox
            thin
            message={
              'Päästäksesi valitsemaan hakutoiveet valitse ensin toivottu aloituspäivä "Palvelun tarve" -osiosta'
            }
          />
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
          <Gap size={'xs'} />
          <FixedSpaceRow>
            <SelectionChip
              text={'Suomi'}
              selected={displayFinnish}
              onClick={setDisplayFinnish}
            />
            <SelectionChip
              text={'Ruotsi'}
              selected={displaySwedish}
              onClick={setDisplaySwedish}
            />
          </FixedSpaceRow>

          <Gap size={'s'} />

          {units.isLoading && <SpinnerSegment />}
          {units.isFailure && <ErrorSegment />}
          {units.isSuccess && (
            <FixedSpaceFlexWrap horizontalSpacing={'L'} verticalSpacing={'s'}>
              <FixedWidthDiv>
                <Label>Valitse hakutoiveet *</Label>
                <Gap size={'xs'} />
                <SelectWrapper>
                  <ReactSelect
                    isMulti
                    isSearchable
                    options={units.value}
                    filterOption={(
                      { data }: { data: PublicUnit },
                      searchString
                    ) =>
                      (!searchString ||
                        data.name
                          .toLowerCase()
                          .includes(searchString.toLowerCase())) &&
                      ((displayFinnish && data.language === 'fi') ||
                        (displaySwedish && data.language === 'sv'))
                    }
                    value={units.value.filter(
                      (u) =>
                        !!formData.preferredUnits.find((u2) => u2.id === u.id)
                    )}
                    onChange={(selected) => {
                      if (
                        selected &&
                        'length' in selected &&
                        selected.length > maxUnits
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
                    placeholder={'Hae yksiköitä'}
                    noOptionsMessage={() => 'Ei hakuehtoja vastaavia yksiköitä'}
                    hideSelectedOptions={false}
                    closeMenuOnSelect={false}
                    backspaceRemovesValue={false}
                    components={{
                      MultiValueContainer: () => null,
                      Option: function Option({
                        innerRef,
                        innerProps,
                        ...props
                      }) {
                        const data = props.data as PublicUnit
                        return (
                          <OptionWrapper ref={innerRef} {...innerProps}>
                            <OptionContents
                              name={data.name}
                              address={data.streetAddress}
                              selected={props.isSelected}
                            />
                          </OptionWrapper>
                        )
                      }
                    }}
                  />
                </SelectWrapper>
              </FixedWidthDiv>
              <FixedWidthDiv>
                <Label>Valitsemasi hakutoiveet</Label>
                <Gap size={'xs'} />
                <FixedSpaceColumn spacing={'s'}>
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
              </FixedWidthDiv>
            </FixedSpaceFlexWrap>
          )}
        </>
      )}
    </ContentArea>
  )
})

const FixedWidthDiv = styled.div`
  width: 100%;
  max-width: 480px;
`

const SelectWrapper = styled.div`
  .multi-value {
    display: none;
    &:first-child {
      display: unset;
    }
  }
`

const OptionWrapper = styled.div`
  cursor: pointer;
  &:hover {
    background-color: ${colors.blues.light};
  }
  padding: ${defaultMargins.xxs} ${defaultMargins.s};
`

const OptionContents = React.memo(function Option({
  name,
  address,
  selected
}: {
  name: string
  address: string
  selected: boolean
}) {
  return (
    <FixedSpaceRow alignItems={'center'}>
      <StaticCheckBox checked={selected} />
      <FixedSpaceColumn spacing="zero">
        <span>{name}</span>
        <AddressInfo>{address}</AddressInfo>
      </FixedSpaceColumn>
    </FixedSpaceRow>
  )
})

const AddressInfo = styled.span`
  font-size: 14px;
  line-height: 21px;
  font-weight: 600;
  color: ${colors.greyscale.dark};
`
