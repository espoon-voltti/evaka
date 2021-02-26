// SPDX-FileCopyrightText: 2017-2021 City of Espoo
//
// SPDX-License-Identifier: LGPL-2.1-or-later

import React from 'react'
import styled from 'styled-components'
import { Result } from '@evaka/lib-common/src/api'
import colors from '@evaka/lib-components/src/colors'
import { defaultMargins } from '@evaka/lib-components/src/white-space'
import { H1 } from '@evaka/lib-components/src/typography'
import { SpinnerSegment } from '@evaka/lib-components/src/atoms/state/Spinner'
import ErrorSegment from '@evaka/lib-components/src/atoms/state/ErrorSegment'
import { useTranslation } from '~state/i18n'
import { IdAndName } from './types'

type Props = {
  units: Result<IdAndName[]>
  activeUnit: IdAndName | null
  selectUnit: (unit: IdAndName) => void
}

export default React.memo(function UnitsList({
  units,
  activeUnit,
  selectUnit
}: Props) {
  const t = useTranslation()

  return (
    <>
      <Container>
        <HeaderContainer>
          <H1 noMargin>{t.i18n.messages.unitListTitle}</H1>
        </HeaderContainer>

        {units.isLoading && <SpinnerSegment />}
        {units.isFailure && (
          <ErrorSegment title={t.i18n.common.loadingFailed} />
        )}
        <Units>
          {units.isSuccess &&
            units.value.map((unit) => (
              <Unit
                key={unit.id}
                onClick={() => selectUnit(unit)}
                className={
                  activeUnit && unit.id === activeUnit.id ? 'active' : ''
                }
              >
                {unit.name}
              </Unit>
            ))}
        </Units>
      </Container>
    </>
  )
})

const Units = styled.div`
  display: flex;
  flex-direction: column;
`

const Unit = styled.button`
  background: white;
  border: none;
  padding: ${defaultMargins.s} ${defaultMargins.m};
  margin: 0;
  text-align: left;
  &.active {
    font-weight: 600;
    background: ${colors.brandEspoo.espooTurquoiseLight};
  }
`

const Container = styled.div`
  min-width: 35%;
  max-width: 400px;
  min-height: 500px;
  background-color: ${colors.greyscale.white};
`

const HeaderContainer = styled.div`
  padding: ${defaultMargins.m};
`
