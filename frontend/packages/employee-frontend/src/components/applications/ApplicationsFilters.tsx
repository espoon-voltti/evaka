// SPDX-FileCopyrightText: 2017-2020 City of Espoo
//
// SPDX-License-Identifier: LGPL-2.1-or-later

import React, { Fragment, useContext, useEffect } from 'react'
import { Label } from '@evaka/lib-components/src/typography'
import MultiSelect from '@evaka/lib-components/src/atoms/form/MultiSelect'
import Radio from '@evaka/lib-components/src/atoms/form/Radio'
import {
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
import {
  ApplicationUIContext,
  VoucherApplicationFilter
} from '../../state/application-ui'
import { Loading } from '@evaka/lib-common/src/api'
import { getAreas, getUnits } from '../../api/daycare'
import { Gap } from '@evaka/lib-components/src/white-space'
import { useTranslation } from '~state/i18n'

export default React.memo(function ApplicationFilters() {
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
    setTransferApplications,
    voucherApplications,
    setVoucherApplications,
    setApplicationsResult
  } = useContext(ApplicationUIContext)

  const { i18n } = useTranslation()

  useEffect(() => {
    void getAreas().then(setAvailableAreas)
  }, [])

  useEffect(() => {
    const areas = availableAreas
      .map((areas) => areas.map(({ shortName }) => shortName))
      .getOrElse([])
    void getUnits(areas, type).then(setAllUnits)
  }, [type, availableAreas])

  useEffect(() => {
    if (units.length === 0 && distinctions.includes('SECONDARY')) {
      setDistinctions(distinctions.filter((v) => v !== 'SECONDARY'))
    }
  }, [units])

  const toggleBasis = (toggledBasis: ApplicationBasis) => () => {
    setApplicationsResult(Loading.of())
    basis.includes(toggledBasis)
      ? setBasis(basis.filter((v) => v != toggledBasis))
      : setBasis([...basis, toggledBasis])
  }

  const toggleStatus = (newStatus: ApplicationSummaryStatusOptions) => () => {
    setApplicationsResult(Loading.of())
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
    setApplicationsResult(Loading.of())
    setType(type)
    if (type === 'PRESCHOOL') {
      setPreschoolType([...preschoolTypes])
    }
  }
  const toggleDate = (toggledDateType: ApplicationDateType) => () => {
    setApplicationsResult(Loading.of())
    dateType.includes(toggledDateType)
      ? setDateType(dateType.filter((v) => v !== toggledDateType))
      : setDateType([...dateType, toggledDateType])
  }

  const toggleApplicationPreschoolType = (type: PreschoolType) => () => {
    setApplicationsResult(Loading.of())
    preschoolType.includes(type)
      ? setPreschoolType(preschoolType.filter((v) => v !== type))
      : setPreschoolType([...preschoolType, type])
  }

  const toggleAllStatuses = (
    status: ApplicationSummaryStatusAllOptions
  ) => () => {
    setApplicationsResult(Loading.of())
    allStatuses.includes(status)
      ? setAllStatuses(allStatuses.filter((v) => v !== status))
      : setAllStatuses([...allStatuses, status])
  }

  const changeUnits = (selectedUnits: string[]) => {
    setApplicationsResult(Loading.of())
    setUnits(selectedUnits.map((selectedUnit) => selectedUnit))
  }

  const toggleApplicationDistinctions = (
    distinction: ApplicationDistinctions
  ) => () => {
    setApplicationsResult(Loading.of())
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
          <AreaMultiSelect
            areas={availableAreas.getOrElse([])}
            selected={area}
            onSelect={setArea}
          />
          <Gap size="L" />
          <MultiSelectUnitFilter
            units={allUnits.getOrElse([])}
            selectedUnits={units}
            onChange={changeUnits}
            dataQa={'unit-selector'}
          />
          <Gap size="xs" />
          <ApplicationDistinctionsFilter
            toggle={toggleApplicationDistinctions}
            toggled={distinctions}
            disableSecondary={units.length === 0}
          />
          <Gap size="L" />
          <ApplicationTypeFilter
            toggled={type}
            toggledPreschool={preschoolType}
            toggle={toggleApplicationType}
            togglePreschool={toggleApplicationPreschoolType}
          />
          <Gap size="L" />
          <ApplicationBasisFilter toggled={basis} toggle={toggleBasis} />
        </>
      }
      column2={
        <Fragment>
          <TransferApplicationsFilter
            selected={transferApplications}
            setSelected={setTransferApplications}
          />
          <Gap size="XL" />
          <VoucherApplicationsFilter
            selected={voucherApplications}
            setSelected={setVoucherApplications}
          />
        </Fragment>
      }
      column3={
        <Fragment>
          <ApplicationStatusFilter
            toggled={status}
            toggledAllStatuses={allStatuses}
            toggle={toggleStatus}
            toggleAllStatuses={toggleAllStatuses}
          />
          <Gap size="XL" />
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
})

type AreaMultiSelectProps = {
  areas: { name: string; shortName: string }[]
  selected: string[]
  onSelect: (areas: string[]) => void
}

const AreaMultiSelect = React.memo(function AreaMultiSelect({
  areas,
  selected,
  onSelect
}: AreaMultiSelectProps) {
  const { i18n } = useTranslation()
  const value = areas.filter((area) => selected.includes(area.shortName))
  const onChange = (selected: { shortName: string }[]) =>
    onSelect(selected.map(({ shortName }) => shortName))
  return (
    <>
      <Label>{i18n.filters.area}</Label>
      <MultiSelect
        value={value}
        options={areas}
        getOptionId={({ shortName }) => shortName}
        getOptionLabel={({ name }) => name}
        onChange={onChange}
        placeholder={i18n.applications.list.areaPlaceholder}
        data-qa="area-filter"
      />
    </>
  )
})

type VoucherApplicationsFilterProps = {
  selected: VoucherApplicationFilter
  setSelected: (v: VoucherApplicationFilter) => void
}

const VoucherApplicationsFilter = React.memo(
  function VoucherApplicationsFilter({
    selected,
    setSelected
  }: VoucherApplicationsFilterProps) {
    const { i18n } = useTranslation()
    return (
      <>
        <Label>{i18n.applications.list.voucherFilter.title}</Label>
        <Gap size="xs" />
        <Radio
          dataQa="filter-voucher-first-choice"
          label={i18n.applications.list.voucherFilter.firstChoice}
          checked={selected === 'VOUCHER_FIRST_CHOICE'}
          onChange={() => setSelected('VOUCHER_FIRST_CHOICE')}
          small
        />
        <Gap size="xs" />
        <Radio
          dataQa="filter-voucher-all"
          label={i18n.applications.list.voucherFilter.allVoucher}
          checked={selected === 'VOUCHER_ONLY'}
          onChange={() => setSelected('VOUCHER_ONLY')}
          small
        />
        <Gap size="xs" />
        <Radio
          dataQa="filter-voucher-hide"
          label={i18n.applications.list.voucherFilter.hideVoucher}
          checked={selected === 'NO_VOUCHER'}
          onChange={() => setSelected('NO_VOUCHER')}
          small
        />
        <Gap size="xs" />
        <Radio
          dataQa="filter-voucher-no-filter"
          label={i18n.applications.list.voucherFilter.noFilter}
          checked={selected === undefined}
          onChange={() => setSelected(undefined)}
          small
        />
      </>
    )
  }
)
