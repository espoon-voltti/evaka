// SPDX-FileCopyrightText: 2017-2022 City of Espoo
//
// SPDX-License-Identifier: LGPL-2.1-or-later

import React, { Fragment, useContext, useEffect } from 'react'

import { Loading, wrapResult } from 'lib-common/api'
import {
  ApplicationStatusOption,
  applicationStatusOptions,
  ApplicationTypeToggle
} from 'lib-common/generated/api-types/application'
import { DaycareCareArea } from 'lib-common/generated/api-types/daycare'
import { UUID } from 'lib-common/types'
import MultiSelect from 'lib-components/atoms/form/MultiSelect'
import Radio from 'lib-components/atoms/form/Radio'
import { Label } from 'lib-components/typography'
import { Gap } from 'lib-components/white-space'

import { getUnits } from '../../generated/api-clients/daycare'
import {
  ApplicationUIContext,
  VoucherApplicationFilter
} from '../../state/application-ui'
import { useTranslation } from '../../state/i18n'
import {
  Filters,
  ApplicationDistinctionsFilter,
  ApplicationStatusFilter,
  ApplicationTypeFilter,
  ApplicationDateFilter,
  ApplicationDateType,
  ApplicationBasisFilter,
  ApplicationBasis,
  ApplicationSummaryStatusOptions,
  PreschoolType,
  preschoolTypes,
  MultiSelectUnitFilter,
  ApplicationDistinctions,
  TransferApplicationsFilter
} from '../common/Filters'

const getUnitsResult = wrapResult(getUnits)

export default React.memo(function ApplicationFilters() {
  const {
    allUnits,
    setAllUnits,
    availableAreas,
    clearSearchFilters,
    setApplicationsResult,
    applicationSearchFilters,
    setApplicationSearchFilters
  } = useContext(ApplicationUIContext)

  const { i18n } = useTranslation()

  useEffect(
    () => {
      const areas =
        applicationSearchFilters.area.length > 0
          ? applicationSearchFilters.area
          : null
      void getUnitsResult({
        areaIds: areas,
        type: applicationSearchFilters.type,
        from: null
      }).then(setAllUnits)
    },
    // eslint-disable-next-line react-hooks/exhaustive-deps
    [
      applicationSearchFilters.type,
      availableAreas,
      applicationSearchFilters.area
    ]
  )

  useEffect(() => {
    if (
      applicationSearchFilters.units.length === 0 &&
      applicationSearchFilters.distinctions.includes('SECONDARY')
    ) {
      setApplicationSearchFilters({
        ...applicationSearchFilters,
        distinctions: applicationSearchFilters.distinctions.filter(
          (v) => v !== 'SECONDARY'
        )
      })
    }
  }, [applicationSearchFilters.units]) // eslint-disable-line react-hooks/exhaustive-deps

  const toggleBasis = (toggledBasis: ApplicationBasis) => () => {
    setApplicationsResult(Loading.of())
    setApplicationSearchFilters({
      ...applicationSearchFilters,
      basis: applicationSearchFilters.basis.includes(toggledBasis)
        ? applicationSearchFilters.basis.filter((v) => v != toggledBasis)
        : [...applicationSearchFilters.basis, toggledBasis]
    })
  }

  const toggleStatus = (newStatus: ApplicationSummaryStatusOptions) => () => {
    setApplicationsResult(Loading.of())
    if (
      (newStatus === 'ALL' && applicationSearchFilters.status !== 'ALL') ||
      applicationSearchFilters.allStatuses.length === 0
    ) {
      setApplicationSearchFilters({
        ...applicationSearchFilters,
        status: newStatus,
        allStatuses: [...applicationStatusOptions]
      })
    } else if (
      newStatus === 'ALL' &&
      applicationSearchFilters.status === 'ALL'
    ) {
      setApplicationSearchFilters({
        ...applicationSearchFilters,
        status: newStatus,
        allStatuses: []
      })
    } else {
      setApplicationSearchFilters({
        ...applicationSearchFilters,
        status: newStatus
      })
    }
  }

  const toggleApplicationType = (type: ApplicationTypeToggle) => () => {
    setApplicationsResult(Loading.of())
    setApplicationSearchFilters({
      ...applicationSearchFilters,
      type,
      preschoolType:
        type === 'PRESCHOOL'
          ? [...preschoolTypes]
          : applicationSearchFilters.preschoolType
    })
  }

  const toggleDate = (toggledDateType: ApplicationDateType) => () => {
    setApplicationsResult(Loading.of())
    setApplicationSearchFilters({
      ...applicationSearchFilters,
      dateType: applicationSearchFilters.dateType.includes(toggledDateType)
        ? applicationSearchFilters.dateType.filter((v) => v !== toggledDateType)
        : [...applicationSearchFilters.dateType, toggledDateType]
    })
  }

  const toggleApplicationPreschoolType = (type: PreschoolType) => () => {
    setApplicationsResult(Loading.of())
    setApplicationSearchFilters({
      ...applicationSearchFilters,
      preschoolType: applicationSearchFilters.preschoolType.includes(type)
        ? applicationSearchFilters.preschoolType.filter((v) => v !== type)
        : [...applicationSearchFilters.preschoolType, type]
    })
  }

  const toggleAllStatuses = (status: ApplicationStatusOption) => () => {
    setApplicationsResult(Loading.of())
    setApplicationSearchFilters({
      ...applicationSearchFilters,
      allStatuses: applicationSearchFilters.allStatuses.includes(status)
        ? applicationSearchFilters.allStatuses.filter((v) => v !== status)
        : [...applicationSearchFilters.allStatuses, status]
    })
  }

  const changeUnits = (selectedUnits: string[]) => {
    setApplicationsResult(Loading.of())
    setApplicationSearchFilters({
      ...applicationSearchFilters,
      units: selectedUnits.map((selectedUnit) => selectedUnit)
    })
  }

  const toggleApplicationDistinctions =
    (distinction: ApplicationDistinctions) => () => {
      setApplicationsResult(Loading.of())
      setApplicationSearchFilters({
        ...applicationSearchFilters,
        distinctions: applicationSearchFilters.distinctions.includes(
          distinction
        )
          ? applicationSearchFilters.distinctions.filter(
              (v) => v !== distinction
            )
          : [...applicationSearchFilters.distinctions, distinction]
      })
    }

  return (
    <Filters
      searchPlaceholder={i18n.applications.searchPlaceholder}
      freeText={applicationSearchFilters.searchTerms}
      setFreeText={(searchTerms) =>
        setApplicationSearchFilters({
          ...applicationSearchFilters,
          searchTerms
        })
      }
      clearFilters={clearSearchFilters}
      clearMargin={applicationSearchFilters.status === 'ALL' ? 0 : -40}
      column1={
        <>
          <AreaMultiSelect
            areas={availableAreas.getOrElse([])}
            selected={applicationSearchFilters.area}
            onSelect={(area) =>
              setApplicationSearchFilters({
                ...applicationSearchFilters,
                area: area
              })
            }
          />
          <Gap size="L" />
          <MultiSelectUnitFilter
            units={allUnits.getOrElse([])}
            selectedUnits={applicationSearchFilters.units}
            onChange={changeUnits}
            data-qa="unit-selector"
          />
          <Gap size="m" />
          <ApplicationDistinctionsFilter
            toggle={toggleApplicationDistinctions}
            toggled={applicationSearchFilters.distinctions}
            disableSecondary={applicationSearchFilters.units.length === 0}
          />
          <Gap size="L" />
          <ApplicationTypeFilter
            toggled={applicationSearchFilters.type}
            toggledPreschool={applicationSearchFilters.preschoolType}
            toggle={toggleApplicationType}
            togglePreschool={toggleApplicationPreschoolType}
          />
          <Gap size="L" />
          <ApplicationBasisFilter
            toggled={applicationSearchFilters.basis}
            toggle={toggleBasis}
          />
        </>
      }
      column2={
        <Fragment>
          <TransferApplicationsFilter
            selected={applicationSearchFilters.transferApplications}
            setSelected={(transferApplications) =>
              setApplicationSearchFilters({
                ...applicationSearchFilters,
                transferApplications
              })
            }
          />
          <Gap size="XL" />
          <VoucherApplicationsFilter
            selected={applicationSearchFilters.voucherApplications}
            setSelected={(voucherApplications) =>
              setApplicationSearchFilters({
                ...applicationSearchFilters,
                voucherApplications
              })
            }
          />
        </Fragment>
      }
      column3={
        <Fragment>
          <ApplicationStatusFilter
            toggled={applicationSearchFilters.status}
            toggledAllStatuses={applicationSearchFilters.allStatuses}
            toggle={toggleStatus}
            toggleAllStatuses={toggleAllStatuses}
          />
          <Gap size="XL" />
          <ApplicationDateFilter
            startDate={applicationSearchFilters.startDate}
            setStartDate={(startDate) =>
              setApplicationSearchFilters({
                ...applicationSearchFilters,
                startDate
              })
            }
            endDate={applicationSearchFilters.endDate}
            setEndDate={(endDate) =>
              setApplicationSearchFilters({
                ...applicationSearchFilters,
                endDate
              })
            }
            toggled={applicationSearchFilters.dateType}
            toggle={toggleDate}
          />
        </Fragment>
      }
    />
  )
})

type AreaMultiSelectProps = {
  areas: DaycareCareArea[]
  selected: UUID[]
  onSelect: (areaIds: UUID[]) => void
}

const AreaMultiSelect = React.memo(function AreaMultiSelect({
  areas,
  selected,
  onSelect
}: AreaMultiSelectProps) {
  const { i18n } = useTranslation()
  return (
    <>
      <Label>{i18n.filters.area}</Label>
      <Gap size="xs" />
      <MultiSelect
        value={areas.filter((area) => selected.includes(area.id))}
        options={areas}
        getOptionId={({ shortName }) => shortName}
        getOptionLabel={({ name }) => name}
        onChange={(areas) => onSelect(areas.map((area) => area.id))}
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
          data-qa="filter-voucher-first-choice"
          label={i18n.applications.list.voucherFilter.firstChoice}
          checked={selected === 'VOUCHER_FIRST_CHOICE'}
          onChange={() => setSelected('VOUCHER_FIRST_CHOICE')}
          small
        />
        <Gap size="xs" />
        <Radio
          data-qa="filter-voucher-all"
          label={i18n.applications.list.voucherFilter.allVoucher}
          checked={selected === 'VOUCHER_ONLY'}
          onChange={() => setSelected('VOUCHER_ONLY')}
          small
        />
        <Gap size="xs" />
        <Radio
          data-qa="filter-voucher-hide"
          label={i18n.applications.list.voucherFilter.hideVoucher}
          checked={selected === 'NO_VOUCHER'}
          onChange={() => setSelected('NO_VOUCHER')}
          small
        />
        <Gap size="xs" />
        <Radio
          data-qa="filter-voucher-no-filter"
          label={i18n.applications.list.voucherFilter.noFilter}
          checked={selected === undefined}
          onChange={() => setSelected(undefined)}
          small
        />
      </>
    )
  }
)
