// SPDX-FileCopyrightText: 2017-2020 City of Espoo
//
// SPDX-License-Identifier: LGPL-2.1-or-later

import React, { Fragment, useEffect, useState } from 'react'
import { useParams } from 'react-router-dom'
import styled from 'styled-components'

import { Container, ContentArea } from 'components/shared/layout/Container'
import Title from './shared/atoms/Title'
import { DefaultMargins, Gap } from 'components/shared/layout/white-space'
import LocalDate from '@evaka/lib-common/src/local-date'
import Button from './shared/atoms/buttons/Button'
import {
  getUnitData,
  GroupOccupancies,
  UnitData,
  UnitResponse
} from '~api/unit'
import { isFailure, isLoading, isSuccess, Loading, Result, Success } from '~api'
import { getDaycare } from '~api/unit'
import { FixedSpaceColumn } from './shared/layout/flex-helpers'
import Loader from './shared/atoms/Loader'
import { DaycareGroup } from '~types/unit'
import Select from './common/Select'

const CustomButton = styled(Button)`
  @media screen and (max-width: 1023px) {
    margin-bottom: ${DefaultMargins.s};
    width: calc(50vw - 40px);
    text-overflow: ellipsis;
  }

  @media screen and (min-width: 1024px) {
    margin-right: ${DefaultMargins.s};
  }
`

const Flex = styled.div`
  @media screen and (max-width: 1023px) {
    justify-content: space-between;
  }
  display: flex;
  flex-direction: row;
  flex-wrap: wrap;
`

const FullHeightContentArea = styled(ContentArea)`
  min-height: 100vh;
  display: flex;
  flex-direction: column;
`

const Name = styled(Title)`
  margin-top: 64px;
`

const Titles = styled(FixedSpaceColumn)`
  margin-bottom: 32px;
`

const GroupInfo = styled.div`
  display: flex;
  justify-content: space-between;
`

const GroupInfoItem = styled.span``

function AttendancePage() {
  const { id } = useParams<{ id: string }>()

  const [unitData, setUnitData] = useState<Result<UnitData>>(Loading())
  const [unit, setUnit] = useState<Result<UnitResponse>>(Loading())
  const [group, setGroup] = useState<DaycareGroup | undefined>(undefined)
  const [uiMode, setUiMode] = useState<
    'chooseGroup' | 'group' | 'child' | 'absence'
  >('chooseGroup')

  useEffect(() => {
    void getUnitData(id, LocalDate.today(), LocalDate.today()).then(setUnitData)
    void getDaycare(id).then(setUnit)
  }, [])

  const loading = isLoading(unit) || isLoading(unitData)

  function chooseGroup(selectedGroup: DaycareGroup) {
    setGroup(selectedGroup)
    setUiMode('group')
  }

  function getChildMinMaxHeadcounts(
    occupancies: GroupOccupancies | undefined
  ): { min: number; max: number } | undefined {
    if (occupancies && group) {
      const occupancyResponse = occupancies.confirmed?.[group.id]
      const headcounts = occupancyResponse.occupancies?.map(
        ({ headcount }) => headcount
      )
      return headcounts !== undefined
        ? { min: Math.min(...headcounts), max: Math.max(...headcounts) }
        : undefined
    } else {
      return undefined
    }
  }

  const headcounts = isSuccess(unitData)
    ? getChildMinMaxHeadcounts(unitData.data.groupOccupancies)
    : undefined
  const caretakers =
    isSuccess(unitData) && group
      ? unitData.data.caretakers.groupCaretakers[group.id]
      : undefined
  const placements =
    isSuccess(unitData) && group
      ? unitData.data.placements.filter(
          (placement) =>
            placement.groupPlacements.filter(
              (groupPlacement) => groupPlacement.groupId === group.id
            ).length > 0
        )
      : undefined

  console.log('placements: ', placements)
  function renderChooseGroup() {
    return (
      <FullHeightContentArea opaque>
        {loading && <Loader />}
        {isSuccess(unitData) && isSuccess(unit) && (
          <Fragment>
            <Titles spacing={'L'}>
              <Name size={2} centered>
                {unit.data.daycare.name}
              </Name>

              <Title size={3} centered>
                Valitse ryhmä
              </Title>
            </Titles>
            <Flex>
              {unitData.data.groups.map((elem: DaycareGroup) => (
                <CustomButton
                  key={elem.id}
                  text={elem.name}
                  onClick={() => chooseGroup(elem)}
                />
              ))}
            </Flex>
          </Fragment>
        )}
      </FullHeightContentArea>
    )
  }

  function renderGroup() {
    return (
      <FullHeightContentArea opaque>
        {isSuccess(unitData) && isSuccess(unit) && group && (
          <Fragment>
            <Titles spacing={'L'}>
              <Name size={2} centered>
                {unit.data.daycare.name}
              </Name>
            </Titles>
            <Select
              options={unitData.data.groups.map((elem) => {
                return {
                  label: elem.name,
                  value: elem.id
                }
              })}
              onChange={(value) => {
                if (value && 'value' in value) {
                  const selectedGroup = unitData.data.groups.find(
                    (elem) => elem.id === value.value
                  )
                  if (selectedGroup) {
                    chooseGroup(selectedGroup)
                  }
                }
                return undefined
              }}
              value={unitData.data.groups
                .filter((elem) => group.id === elem.id)
                .map((elem) => ({ label: elem.name, value: elem.id }))}
            ></Select>
            <GroupInfo>
              <GroupInfoItem>
                Lapsia{' '}
                {headcounts && headcounts.min === headcounts.max
                  ? headcounts.min
                  : headcounts && `${headcounts.min}-${headcounts.max}`}
              </GroupInfoItem>
              <GroupInfoItem>
                Hlökunta{' '}
                {caretakers && caretakers.maximum == caretakers.minimum
                  ? caretakers.maximum
                  : caretakers && `${caretakers.minimum}-${caretakers.maximum}`}
              </GroupInfoItem>
              <GroupInfoItem>
                {headcounts &&
                caretakers &&
                caretakers.maximum == caretakers.minimum &&
                headcounts.min === headcounts.max &&
                caretakers.minimum > 0
                  ? `Suhdeluku: ${(headcounts.min / caretakers.minimum).toFixed(
                      2
                    )}`
                  : 'Suhdelukua ei voitu laskea'}
              </GroupInfoItem>
            </GroupInfo>

            <div>
              {placements &&
                placements.length > 0 &&
                placements.map((placement) => (
                  <div>{placement.child.firstName}</div>
                ))}
            </div>
          </Fragment>
        )}
      </FullHeightContentArea>
    )
  }

  return (
    <Container>
      {uiMode === 'chooseGroup' && renderChooseGroup()}{' '}
      {uiMode === 'group' && renderGroup()}
    </Container>
  )
}

export default AttendancePage
