import React from 'react'
import styled from 'styled-components'
import { ContentArea } from '@evaka/lib-components/src/layout/Container'
import { UnitLanguage } from '@evaka/lib-common/src/api-types/units/enums'
import {
  FixedSpaceColumn,
  FixedSpaceRow
} from '@evaka/lib-components/src/layout/flex-helpers'
import { SelectionChip } from '@evaka/lib-components/src/atoms/Chip'
import { H1, Label, P } from '@evaka/lib-components/src/typography'
import { useTranslation } from '~localization'
import { mapViewBreakpoint, MobileMode } from '~map/const'
import HorizontalLine from '../../../lib-components/src/atoms/HorizontalLine'
import InlineButton from '../../../lib-components/src/atoms/buttons/InlineButton'
import { faList, fasMapMarkerAlt } from '@evaka/lib-icons'
import colors from '@evaka/lib-components/src/colors'

type Props = {
  languages: UnitLanguage[]
  onChangeLanguages: (val: UnitLanguage[]) => void
  mobileMode: MobileMode
  setMobileMode: (mode: MobileMode) => void
}

export default React.memo(function SearchSection({
  languages,
  onChangeLanguages,
  mobileMode,
  setMobileMode
}: Props) {
  const t = useTranslation()

  return (
    <Wrapper opaque>
      <H1>{t.map.title}</H1>
      <P>{t.map.mainInfo}</P>
      <FixedSpaceColumn spacing="xs">
        <Label>{t.map.language}</Label>
        <FixedSpaceRow>
          <SelectionChip
            text={t.common.unit.languages.fi}
            selected={languages.includes('fi')}
            onChange={(selected) => {
              const nextValue = languages.filter((l) => l !== 'fi')
              if (selected) nextValue.push('fi')
              onChangeLanguages(nextValue)
            }}
          />
          <SelectionChip
            text={t.common.unit.languages.sv}
            selected={languages.includes('sv')}
            onChange={(selected) => {
              const nextValue = languages.filter((l) => l !== 'sv')
              if (selected) nextValue.push('sv')
              onChangeLanguages(nextValue)
            }}
          />
        </FixedSpaceRow>
      </FixedSpaceColumn>

      <div className="mobile-tabs">
        <HorizontalLine />
        <MobileTabs>
          <InlineButton
            onClick={() => setMobileMode('map')}
            text={t.map.mobileTabs.map}
            icon={fasMapMarkerAlt}
            className={mobileMode !== 'map' ? 'inactive' : undefined}
          />
          <InlineButton
            onClick={() => setMobileMode('list')}
            text={t.map.mobileTabs.list}
            icon={faList}
            className={mobileMode !== 'list' ? 'inactive' : undefined}
          />
        </MobileTabs>
      </div>
    </Wrapper>
  )
})

const Wrapper = styled(ContentArea)`
  box-sizing: border-box;
  width: 100%;

  .mobile-tabs {
    display: none;
  }
  @media (max-width: ${mapViewBreakpoint}) {
    .mobile-tabs {
      display: block !important;
    }
  }
`

const MobileTabs = styled.div`
  display: flex;
  justify-content: space-evenly;

  .inactive {
    color: ${colors.greyscale.medium} !important;
  }
`
