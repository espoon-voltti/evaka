// SPDX-FileCopyrightText: 2017-2020 City of Espoo
//
// SPDX-License-Identifier: LGPL-2.1-or-later

import React, { Fragment, useContext, useEffect } from 'react'
import styled from 'styled-components'
import {
  AreaFilter,
  Filters,
  ApplicationDistinctionsFilter,
  ApplicationStatusFilter,
  ApplicationTypeFilter,
  ApplicationDateFilter,
  ApplicationTypeToggle,
  ApplicationDateType,
  ApplicationBasisFilter,
  ApplicationBasis,
  ApplicationSummaryStatusOptions,
  PreschoolType,
  preschoolTypes,
  ApplicationSummaryStatusAllOptions,
  MultiSelectUnitFilter,
  ApplicationDistinctions,
  TransferApplicationsFilter
} from '../common/Filters'
import { ApplicationUIContext } from '../../state/application-ui'
import { isSuccess } from '../../api'
import { getAreas, getUnits } from '../../api/daycare'
import { Gap } from '~components/shared/layout/white-space'
import { useTranslation } from '~state/i18n'
import { SelectOptionProp } from '~components/common/MultiSelect'

const CustomGap = styled.div`
  height: 52px;
`

function ApplicationFilters() {
  const {
    units,
    setUnits,
    allUnits,
    setAllUnits,
    availableAreas,
    setAvailableAreas,
    area,
    setArea,
    status,
    setStatus,
    type,
    setType,
    dateType,
    setDateType,
    startDate,
    setStartDate,
    endDate,
    setEndDate,
    searchTerms,
    setSearchTerms,
    clearSearchFilters,
    basis,
    setBasis,
    preschoolType,
    setPreschoolType,
    allStatuses,
    setAllStatuses,
    distinctions,
    setDistinctions,
    transferApplications,
    setTransferApplications
  } = useContext(ApplicationUIContext)

  const { i18n } = useTranslation()

  useEffect(() => {
    void getAreas().then(setAvailableAreas)
  }, [])

  useEffect(() => {
    void getUnits([]).then(setAllUnits)
  }, [])

  useEffect(() => {
    if (units.length === 0 && distinctions.includes('SECONDARY')) {
      setDistinctions(distinctions.filter((v) => v !== 'SECONDARY'))
    }
  }, [units])

  const ALL = 'All'

  const toggleArea = (code: string) => () => {
    if (isSuccess(availableAreas)) {
      const newAreas =
        code === ALL
          ? area.includes(ALL)
            ? []
            : [...availableAreas.data.map((a) => a.shortName), ALL]
          : area.includes(code)
          ? area
              .filter((v) => v !== code)
              .filter((v) => v !== ALL && code != ALL)
          : [...area, code]

      setArea(newAreas)
    }
  }

  const toggleBasis = (toggledBasis: ApplicationBasis) => () => {
    basis.includes(toggledBasis)
      ? setBasis(basis.filter((v) => v != toggledBasis))
      : setBasis([...basis, toggledBasis])
  }
  const toggleStatus = (newStatus: ApplicationSummaryStatusOptions) => () => {
    if ((newStatus === 'ALL' && status !== 'ALL') || allStatuses.length === 0) {
      setAllStatuses([
        'SENT',
        'WAITING_PLACEMENT',
        'WAITING_DECISION',
        'WAITING_UNIT_CONFIRMATION',
        'WAITING_MAILING',
        'WAITING_CONFIRMATION',
        'REJECTED',
        'ACTIVE',
        'CANCELLED'
      ])
    } else if (newStatus === 'ALL' && status === 'ALL') {
      setAllStatuses([])
    }
    setStatus(newStatus)
  }
  const toggleApplicationType = (type: ApplicationTypeToggle) => () => {
    setType(type)
    if (type === 'PRESCHOOL') {
      setPreschoolType([...preschoolTypes])
    }
  }
  const toggleDate = (toggledDateType: ApplicationDateType) => () => {
    dateType.includes(toggledDateType)
      ? setDateType(dateType.filter((v) => v !== toggledDateType))
      : setDateType([...dateType, toggledDateType])
  }

  const toggleApplicationPreschoolType = (type: PreschoolType) => () => {
    preschoolType.includes(type)
      ? setPreschoolType(preschoolType.filter((v) => v !== type))
      : setPreschoolType([...preschoolType, type])
  }

  const toggleAllStatuses = (
    status: ApplicationSummaryStatusAllOptions
  ) => () => {
    allStatuses.includes(status)
      ? setAllStatuses(allStatuses.filter((v) => v !== status))
      : setAllStatuses([...allStatuses, status])
  }

  const changeUnits = (selectedUnits: SelectOptionProp[]) => {
    setUnits(selectedUnits.map((selectedUnit) => selectedUnit.id))
  }

  const toggleApplicationDistinctions = (
    distinction: ApplicationDistinctions
  ) => () => {
    distinctions.includes(distinction)
      ? setDistinctions(distinctions.filter((v) => v !== distinction))
      : setDistinctions([...distinctions, distinction])
  }

  return (
    <Filters
      searchPlaceholder={i18n.applications.searchPlaceholder}
      freeText={searchTerms}
      setFreeText={setSearchTerms}
      clearFilters={clearSearchFilters}
      clearMargin={status === 'ALL' ? 0 : -40}
      column1={
        <>
          <Gap size="s" />
          <AreaFilter
            areas={isSuccess(availableAreas) ? availableAreas.data : []}
            toggled={area}
            toggle={toggleArea}
            showAll
          />
          <Gap size="L" />
          <MultiSelectUnitFilter
            units={
              isSuccess(allUnits)
                ? allUnits.data.map(({ id, name }) => ({ id, label: name }))
                : []
            }
            onSelect={changeUnits}
            onRemove={changeUnits}
          />
          <Gap size="m" />
          <ApplicationBasisFilter toggled={basis} toggle={toggleBasis} />
        </>
      }
      column2={
        <Fragment>
          <Gap size="s" />
          <ApplicationTypeFilter
            toggled={type}
            toggledPreschool={preschoolType}
            toggle={toggleApplicationType}
            togglePreschool={toggleApplicationPreschoolType}
          />
          <CustomGap />
          <TransferApplicationsFilter
            selected={transferApplications}
            setSelected={setTransferApplications}
          />
          <CustomGap />
          <ApplicationDistinctionsFilter
            toggle={toggleApplicationDistinctions}
            toggled={distinctions}
            disableSecondary={units.length === 0}
          />
        </Fragment>
      }
      column3={
        <Fragment>
          <Gap size="s" />
          <ApplicationStatusFilter
            toggled={status}
            toggledAllStatuses={allStatuses}
            toggle={toggleStatus}
            toggleAllStatuses={toggleAllStatuses}
          />
          <CustomGap />
          <ApplicationDateFilter
            startDate={startDate}
            setStartDate={setStartDate}
            endDate={endDate}
            setEndDate={setEndDate}
            toggled={dateType}
            toggle={toggleDate}
          />
        </Fragment>
      }
    />
  )
}

export default ApplicationFilters
