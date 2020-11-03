// SPDX-FileCopyrightText: 2017-2020 City of Espoo
//
// SPDX-License-Identifier: LGPL-2.1-or-later

import LocalDate from '@evaka/lib-common/src/local-date'
import React, { Fragment, useEffect, useState } from 'react'
import { useParams } from 'react-router-dom'
import styled from 'styled-components'
import MetaTags from 'react-meta-tags'

import { isLoading, isSuccess, Loading, Result } from '~api'
import { getDaycare, getUnitData, UnitData, UnitResponse } from '~api/unit'
import Button from '~components/shared/atoms/buttons/Button'
import Loader from '~components/shared/atoms/Loader'
import Title from '~components/shared/atoms/Title'
import { ContentArea } from '~components/shared/layout/Container'
import { DefaultMargins } from '~components/shared/layout/white-space'
import { DaycareGroup } from '~types/unit'
import { useTranslation } from '~state/i18n'

// const FullHeightContentArea = styled(ContentArea)`
//   min-height: 100vh;
//   display: flex;
//   flex-direction: column;
// `

export const Flex = styled.div`
  @media screen and (max-width: 1023px) {
    justify-content: space-between;
  }
  display: flex;
  flex-direction: row;
  flex-wrap: wrap;
`

export const FlexColumn = styled.div`
  @media screen and (max-width: 1023px) {
    justify-content: space-between;
  }
  display: flex;
  flex-direction: column;
  flex-wrap: wrap;
`

interface CustomButtonProps {
  color?: string
  backgroundColor?: string
  borderColor?: string
}

export const CustomButton = styled(Button)<CustomButtonProps>`
  @media screen and (max-width: 1023px) {
    margin-bottom: ${DefaultMargins.s};
    width: calc(50vw - 40px);
    white-space: normal;
    height: 64px;
  }

  @media screen and (min-width: 1024px) {
    margin-right: ${DefaultMargins.s};
  }
  ${(p) => (p.color ? `color: ${p.color};` : '')}
  ${(p) => (p.backgroundColor ? `background-color: ${p.backgroundColor};` : '')}
  ${(p) => (p.borderColor ? `border-color: ${p.borderColor};` : '')}

  :hover {
    ${(p) => (p.color ? `color: ${p.color};` : '')}
    ${(p) =>
      p.backgroundColor ? `background-color: ${p.backgroundColor};` : ''}
  ${(p) => (p.borderColor ? `border-color: ${p.borderColor};` : '')}
  }
`

const AllCapsTitle = styled(Title)`
  text-transform: uppercase;
  font-size: 15px;
  font-family: 'Open Sans';
`

const Padding = styled.div`
  padding: 0 ${DefaultMargins.s};
`

export default React.memo(function AttendanceGroupSelectorPage() {
  const { i18n } = useTranslation()
  const { id } = useParams<{ id: string }>()
  const [unitData, setUnitData] = useState<Result<UnitData>>(Loading())
  const [unit, setUnit] = useState<Result<UnitResponse>>(Loading())

  const loading = isLoading(unit) || isLoading(unitData)

  useEffect(() => {
    void getUnitData(id, LocalDate.today(), LocalDate.today()).then(setUnitData)
    void getDaycare(id).then(setUnit)
  }, [])

  function chooseGroup(selectedGroup: DaycareGroup) {
    console.log('selectedGroup: ', selectedGroup)
  }

  return (
    <Fragment>
      <MetaTags>
        <meta name="viewport" content="width=device-width, initial-scale=1" />
      </MetaTags>

      {loading && <Loader />}
      {isSuccess(unitData) && isSuccess(unit) && (
        <Fragment>
          <Title size={1} centered smaller bold>
            {unit.data.daycare.name}
          </Title>
          <Padding>
            <ContentArea paddingHorozontal={'s'} opaque>
              <AllCapsTitle size={2} centered bold>
                {i18n.attendances.chooseGroup}
              </AllCapsTitle>
              <Flex>
                <a href={`attendance/all/coming`}>
                  <CustomButton primary text={'Kaikki'} />
                </a>
                {unitData.data.groups.map((elem: DaycareGroup) => (
                  <a key={elem.id} href={`attendance/${elem.id}/coming`}>
                    <CustomButton
                      primary
                      key={elem.id}
                      text={elem.name}
                      onClick={() => chooseGroup(elem)}
                    />
                  </a>
                ))}
              </Flex>
            </ContentArea>
          </Padding>
        </Fragment>
      )}
    </Fragment>
  )
})
