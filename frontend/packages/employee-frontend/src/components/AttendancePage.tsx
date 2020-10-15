// SPDX-FileCopyrightText: 2017-2020 City of Espoo
//
// SPDX-License-Identifier: LGPL-2.1-or-later

import React, { Fragment, useEffect, useState } from 'react'
import { useParams } from 'react-router-dom'
import styled from 'styled-components'
import MetaTags from 'react-meta-tags'

import { Container, ContentArea } from 'components/shared/layout/Container'
import Title from './shared/atoms/Title'
import { DefaultMargins, Gap } from 'components/shared/layout/white-space'
import LocalDate from '@evaka/lib-common/src/local-date'
import Button from './shared/atoms/buttons/Button'
import { faChild } from 'icon-set'
import {
  AttendanceStatus,
  childArrives,
  childDeparts,
  ChildInGroup,
  getAttendance,
  getUnitData,
  UnitData,
  UnitResponse
} from '~api/unit'
import { isLoading, isSuccess, Loading, Result } from '~api'
import { getDaycare } from '~api/unit'
import { FixedSpaceColumn } from './shared/layout/flex-helpers'
import Loader from './shared/atoms/Loader'
import { DaycareGroup } from '~types/unit'
import Select from './common/Select'
import Colors from './shared/Colors'
import RoundIcon from './shared/atoms/RoundIcon'
import { AbsenceType, AbsenceTypes } from '~types/absence'
import { useTranslation } from '~state/i18n'
import { postChildAbsence } from '~api/absences'
import { isNotProduction } from '~constants'

const CustomButton = styled(Button)`
  @media screen and (max-width: 1023px) {
    margin-bottom: ${DefaultMargins.s};
    width: calc(50vw - 40px);
    white-space: normal;
    height: 64px;
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

const Centered = styled.div`
  display: flex;
  justify-content: center;
`

const WideButton = styled(Button)`
  width: 100%;
`

const ChildStatus = styled.div`
  color: ${Colors.greyscale.medium};
`

const Bold = styled.div`
  font-weight: 600;

  h2,
  h3 {
    font-weight: 500;
  }
`

const NoResults = styled.div`
  font-style: italic;
  color: ${Colors.greyscale.medium};
`

function AttendancePage() {
  const { i18n } = useTranslation()
  const { id } = useParams<{ id: string }>()

  const [unitData, setUnitData] = useState<Result<UnitData>>(Loading())
  const [unit, setUnit] = useState<Result<UnitResponse>>(Loading())
  const [child, setChild] = useState<ChildInGroup | undefined>(undefined)
  const [groupAttendances, setGoupAttendances] = useState<
    Result<ChildInGroup[]>
  >(Loading())
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
    void getAttendance(selectedGroup.id).then(setGoupAttendances)
    setUiMode('group')
  }

  function openChildView(child: ChildInGroup) {
    setChild(child)
    setUiMode('child')
  }

  function openAbsenceView() {
    setUiMode('absence')
  }

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
        {loading && <Loader />}
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

            <ChildAttendances
              groupAttendances={groupAttendances}
              openChildView={openChildView}
            />
          </Fragment>
        )}
      </FullHeightContentArea>
    )
  }

  function renderChildView() {
    async function markChildPresent(child: ChildInGroup) {
      if (group) {
        await childArrives(child.childId)
        chooseGroup(group)
      }
    }

    async function markChildDeparted(child: ChildInGroup) {
      if (group) {
        await childDeparts(child.childId)
        chooseGroup(group)
      }
    }

    function markChildAbsent() {
      openAbsenceView()
    }

    return (
      <FullHeightContentArea opaque>
        {child && group ? (
          <Fragment>
            <Centered>
              <RoundIcon
                active={false}
                content={faChild}
                color={Colors.blues.dark}
                size="XXL"
              />
            </Centered>
            <Gap size={'s'} />
            <Bold>
              <Title size={2} centered>
                {child.firstName} {child.lastName}
              </Title>
              <Title size={3} centered>
                {group.name}
              </Title>
            </Bold>
            <Centered>
              <ChildStatus>
                {i18n.attendances.types[child.status].toUpperCase()}
              </ChildStatus>
            </Centered>
            <Gap size={'s'} />
            <FixedSpaceColumn>
              {child.status === 'COMING' && (
                <Fragment>
                  <WideButton
                    text={'PAIKALLA'}
                    onClick={() => markChildPresent(child)}
                  />

                  <WideButton
                    text={'POISSA'}
                    onClick={() => markChildAbsent()}
                  />
                </Fragment>
              )}
              {child.status === 'PRESENT' && (
                <Fragment>
                  <WideButton
                    text={'MERKITSE ULOS'}
                    onClick={() => markChildDeparted(child)}
                  />
                </Fragment>
              )}
              {child.status === 'DEPARTED' && (
                <Fragment>
                  <WideButton
                    text={'MERKITSE TULEVAKSI'}
                    onClick={() => markChildDeparted(child)}
                  />
                </Fragment>
              )}
            </FixedSpaceColumn>
          </Fragment>
        ) : (
          <Loader />
        )}
      </FullHeightContentArea>
    )
  }

  function renderAbsenceView() {
    async function selectAbsenceType(absenceType: AbsenceType) {
      if (child && group) {
        // TODO: add careType selector to UI
        await postChildAbsence(absenceType, 'PRESCHOOL', child.childId)
        chooseGroup(group)
      }
    }

    return (
      <FullHeightContentArea opaque>
        {child && group ? (
          <Fragment>
            <Centered>
              <RoundIcon
                active={false}
                content={faChild}
                color={Colors.blues.dark}
                size="XXL"
              />
            </Centered>
            <Gap size={'s'} />
            <Bold>
              <Title size={2} centered>
                {child.firstName} {child.lastName}
              </Title>
              <Title size={4} centered>
                {group.name}
              </Title>
            </Bold>
            <Gap size={'L'} />
            <Bold>
              <Title size={3} centered>
                Valitse poissaolon syy
              </Title>
            </Bold>
            <Gap size={'s'} />
            <Flex>
              {AbsenceTypes.map((absenceType) => (
                <CustomButton
                  key={absenceType}
                  text={i18n.absences.absenceTypes[absenceType]}
                  onClick={() => selectAbsenceType(absenceType)}
                />
              ))}
            </Flex>
          </Fragment>
        ) : (
          <Loader />
        )}
      </FullHeightContentArea>
    )
  }

  return (
    <Container>
      {isNotProduction() ? (
        <>
          <MetaTags>
            <meta
              name="viewport"
              content="width=device-width, initial-scale=1"
            />
          </MetaTags>
          {uiMode === 'chooseGroup' && renderChooseGroup()}
          {uiMode === 'child' && renderChildView()}
          {uiMode === 'absence' && renderAbsenceView()}
          {uiMode === 'group' && isSuccess(groupAttendances) && renderGroup()}
        </>
      ) : (
        <div>Sorry only for testing</div>
      )}
    </Container>
  )
}

interface ChildAttendancesProps {
  groupAttendances: Result<ChildInGroup[]>
  openChildView: (child: ChildInGroup) => void
}

function ChildAttendances({
  groupAttendances,
  openChildView
}: ChildAttendancesProps) {
  return (
    <div>
      <Gap size={'L'} />
      <Title size={3}>Ei vielä tullut</Title>
      <FixedSpaceColumn>
        {isSuccess(groupAttendances) &&
          groupAttendances.data
            .filter((elem) => elem.status === 'COMING')
            .map((groupAttendance) => (
              <ChildListItem
                type={'COMING'}
                onClick={() => openChildView(groupAttendance)}
                key={groupAttendance.childId}
                childInGroup={groupAttendance}
              />
            ))}
      </FixedSpaceColumn>
      {isSuccess(groupAttendances) &&
        groupAttendances.data.filter((elem) => elem.status === 'COMING')
          .length === 0 && <NoResults>Ei tulossa olevia</NoResults>}
      <Gap size={'m'} />
      <Title size={3}>Paikalla olevat</Title>
      <FixedSpaceColumn>
        {isSuccess(groupAttendances) &&
          groupAttendances.data
            .filter((elem) => elem.status === 'PRESENT')
            .map((groupAttendance) => (
              <ChildListItem
                type={'PRESENT'}
                onClick={() => openChildView(groupAttendance)}
                key={groupAttendance.childId}
                childInGroup={groupAttendance}
              />
            ))}
      </FixedSpaceColumn>
      {isSuccess(groupAttendances) &&
        groupAttendances.data.filter((elem) => elem.status === 'PRESENT')
          .length === 0 && <NoResults>Ei paikalla olevia</NoResults>}
      <Gap size={'m'} />
      <Title size={3}>Lähteneet</Title>
      <FixedSpaceColumn>
        {isSuccess(groupAttendances) &&
          groupAttendances.data
            .filter((elem) => elem.status === 'DEPARTED')
            .map((groupAttendance) => (
              <ChildListItem
                type={'DEPARTED'}
                key={groupAttendance.childId}
                childInGroup={groupAttendance}
              />
            ))}
      </FixedSpaceColumn>
      {isSuccess(groupAttendances) &&
        groupAttendances.data.filter((elem) => elem.status === 'DEPARTED')
          .length === 0 && <NoResults>Ei lähteneitä</NoResults>}
      <Gap size={'m'} />
      <Title size={3}>Poissa tänään</Title>
      <FixedSpaceColumn>
        {isSuccess(groupAttendances) &&
          groupAttendances.data
            .filter((elem) => elem.status === 'ABSENT')
            .map((groupAttendance) => (
              <ChildListItem
                type={'ABSENT'}
                key={groupAttendance.childId}
                childInGroup={groupAttendance}
              />
            ))}
      </FixedSpaceColumn>
      {isSuccess(groupAttendances) &&
        groupAttendances.data.filter((elem) => elem.status === 'ABSENT')
          .length === 0 && <NoResults>Ei poissaolevia</NoResults>}
    </div>
  )
}

const ChildBox = styled.div<{ type: AttendanceStatus }>`
  align-items: center;
  background-color: ${(props) => {
    switch (props.type) {
      case 'ABSENT':
        return Colors.greyscale.white
      case 'DEPARTED':
        return Colors.blues.medium
      case 'PRESENT':
        return Colors.accents.green
      case 'COMING':
        return Colors.accents.water
    }
  }};
  color: ${(props) =>
    props.type === 'DEPARTED' ? 'white' : Colors.greyscale.darkest};
  display: flex;
  padding: 10px;
  border: 1px solid;
  border-color: ${(props) => {
    switch (props.type) {
      case 'ABSENT':
        return Colors.greyscale.dark
      case 'DEPARTED':
        return Colors.blues.medium
      case 'PRESENT':
        return Colors.accents.green
      case 'COMING':
        return Colors.accents.water
    }
  }};
  border-radius: 2px;
`

const ChildBoxInfo = styled.div`
  margin-left: 40px;
`

const ChildBoxStatus = styled.div``

interface ChildListItemProps {
  childInGroup: ChildInGroup
  onClick?: () => void
  type: AttendanceStatus
}

function ChildListItem({ childInGroup, onClick, type }: ChildListItemProps) {
  const { i18n } = useTranslation()
  return (
    <ChildBox type={type}>
      <RoundIcon
        active={false}
        content={faChild}
        color={
          type === 'ABSENT'
            ? Colors.greyscale.dark
            : type === 'DEPARTED'
            ? Colors.blues.medium
            : type === 'PRESENT'
            ? Colors.accents.green
            : type === 'COMING'
            ? Colors.accents.water
            : Colors.blues.medium
        }
        size="XL"
      />
      <ChildBoxInfo onClick={onClick}>
        <Bold>
          {childInGroup.firstName} {childInGroup.lastName}
        </Bold>
        <ChildBoxStatus>
          {i18n.attendances.types[childInGroup.status].toUpperCase()}
        </ChildBoxStatus>
      </ChildBoxInfo>
    </ChildBox>
  )
}

export default AttendancePage
