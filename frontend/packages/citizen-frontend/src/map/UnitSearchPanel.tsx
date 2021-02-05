import React, { useEffect, useState } from 'react'
import { Gap } from '~../../lib-components/src/white-space'
import SearchSection from '~map/SearchSection'
import UnitList from '~map/UnitList'
import styled from 'styled-components'
import { Failure, Loading, Result, Success } from '@evaka/lib-common/src/api'
import { PublicUnit } from '@evaka/lib-common/src/api-types/units/PublicUnit'
import { UnitLanguage } from '@evaka/lib-common/src/api-types/units/enums'
import _ from 'lodash'
import { MobileMode } from '~map/const'

type Props = {
  unitsResult: Result<PublicUnit[]>
  mobileMode: MobileMode
  setMobileMode: (mode: MobileMode) => void
}

export default React.memo(function UnitSearchPanel({
  unitsResult,
  mobileMode,
  setMobileMode
}: Props) {
  const [languages, setLanguages] = useState<UnitLanguage[]>([])
  const [filteredUnits, setFilteredUnits] = useState<Result<PublicUnit[]>>(
    filterUnits(unitsResult, languages)
  )

  useEffect(() => {
    setFilteredUnits(filterUnits(unitsResult, languages))
  }, [unitsResult, languages])

  return (
    <Wrapper>
      <SearchSection
        languages={languages}
        onChangeLanguages={setLanguages}
        mobileMode={mobileMode}
        setMobileMode={setMobileMode}
      />
      <Gap size="xs" />
      <UnitList filteredUnits={filteredUnits} />
    </Wrapper>
  )
})

const filterUnits = (
  unitsResult: Result<PublicUnit[]>,
  languages: UnitLanguage[]
): Result<PublicUnit[]> => {
  if (unitsResult.isLoading) return Loading.of()
  if (unitsResult.isFailure)
    return Failure.of({
      message: unitsResult.message,
      statusCode: unitsResult.statusCode
    })

  const filteredUnits = unitsResult.value.filter(
    (u) =>
      languages.length == 0 ||
      (!(u.language === 'fi' && !languages.includes('fi')) &&
        !(u.language === 'sv' && !languages.includes('sv')))
  )
  const sortedUnits = _.sortBy(filteredUnits, (u) => u.name)
  return Success.of(sortedUnits)
}

const Wrapper = styled.div`
  width: 400px;
  min-width: 300px;
  flex-grow: 1;
  flex-shrink: 1;

  display: flex;
  flex-direction: column;

  .mobile-tabs {
    display: none;
  }
`
