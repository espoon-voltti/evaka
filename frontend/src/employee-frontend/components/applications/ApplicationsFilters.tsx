// SPDX-FileCopyrightText: 2017-2024 City of Espoo
//
// SPDX-License-Identifier: LGPL-2.1-or-later

import isEqual from 'lodash/isEqual'
import React, {
  Fragment,
  useContext,
  useEffect,
  useMemo,
  useState
} from 'react'
import styled from 'styled-components'

import type {
  ApplicationBasis,
  ApplicationStatusOption,
  ApplicationTypeToggle
} from 'lib-common/generated/api-types/application'
import { applicationStatusOptions } from 'lib-common/generated/api-types/application'
import type { DaycareCareArea } from 'lib-common/generated/api-types/daycare'
import type { AreaId, DaycareId } from 'lib-common/generated/api-types/shared'
import LocalDate from 'lib-common/local-date'
import { useQueryResult } from 'lib-common/query'
import Checkbox from 'lib-components/atoms/form/Checkbox'
import MultiSelect from 'lib-components/atoms/form/MultiSelect'
import Radio from 'lib-components/atoms/form/Radio'
import { FixedSpaceColumn } from 'lib-components/layout/flex-helpers'
import { Label } from 'lib-components/typography'
import { Gap } from 'lib-components/white-space'
import colors from 'lib-customizations/common'
import { applicationTypes, featureFlags } from 'lib-customizations/employee'
import { faAngleDown, faAngleUp } from 'lib-icons'

import { unitsQuery } from '../../queries'
import type { VoucherApplicationFilter } from '../../state/application-ui'
import { ApplicationUIContext } from '../../state/application-ui'
import { useTranslation } from '../../state/i18n'
import {
  Filters,
  ApplicationDistinctionsFilter,
  ApplicationStatusFilter,
  ApplicationDateFilter,
  ApplicationBasisFilter,
  MultiSelectUnitFilter,
  TransferApplicationsFilter,
  ApplicationOpenIcon
} from '../common/Filters'
import type {
  ApplicationDateType,
  ApplicationSummaryStatusOptions,
  ApplicationDistinctions
} from '../common/Filters'
import type { UnitFilterMenuItem } from '../common/Filters'

export default React.memo(function ApplicationFilters() {
  const { i18n } = useTranslation()

  const {
    availableAreas,
    searchFilters,
    setSearchFilters,
    confirmSearchFilters,
    clearSearchFilters
  } = useContext(ApplicationUIContext)

  const [showClosedUnits, setShowClosedUnits] = useState(false)

  const allUnits = useQueryResult(
    unitsQuery({
      areaIds: searchFilters.area.length > 0 ? searchFilters.area : null,
      type: searchFilters.type !== 'ALL' ? [searchFilters.type] : null,
      from: null
    })
  )

  const today = useMemo(() => LocalDate.todayInHelsinkiTz(), [])

  const filteredUnits = useMemo(() => {
    const units = allUnits.getOrElse([])
    if (showClosedUnits) {
      return units
    }
    return units.filter(
      (unit) => !unit.closingDate || unit.closingDate.isAfter(today)
    )
  }, [allUnits, showClosedUnits, today])

  // Remove closed units from selected units when showClosedUnits is false
  useEffect(() => {
    if (!showClosedUnits && allUnits.isSuccess) {
      const filteredUnitIds = new Set(filteredUnits.map((unit) => unit.id))
      const newUnits = searchFilters.units.filter((unitId) =>
        filteredUnitIds.has(unitId)
      )

      if (!isEqual(newUnits, searchFilters.units)) {
        setSearchFilters((prevFilters) => ({
          ...prevFilters,
          units: newUnits
        }))
      }
    }
  }, [
    allUnits.isSuccess,
    showClosedUnits,
    filteredUnits,
    searchFilters.units,
    setSearchFilters
  ])

  const unitFilterMenuItems: UnitFilterMenuItem[] = [
    {
      id: 'toggle-closed-units',
      label: showClosedUnits
        ? i18n.filters.hideClosedUnits
        : i18n.filters.showClosedUnits,
      onClick: () => setShowClosedUnits(!showClosedUnits)
    }
  ]

  useEffect(() => {
    if (
      searchFilters.units.length === 0 &&
      searchFilters.distinctions.includes('SECONDARY')
    ) {
      setSearchFilters({
        ...searchFilters,
        distinctions: searchFilters.distinctions.filter(
          (v) => v !== 'SECONDARY'
        )
      })
    }
  }, [searchFilters.units]) // eslint-disable-line react-hooks/exhaustive-deps

  const toggleBasis = (toggledBasis: ApplicationBasis) => () => {
    setSearchFilters({
      ...searchFilters,
      basis: searchFilters.basis.includes(toggledBasis)
        ? searchFilters.basis.filter((v) => v !== toggledBasis)
        : [...searchFilters.basis, toggledBasis]
    })
  }

  const toggleStatus = (newStatus: ApplicationSummaryStatusOptions) => () => {
    if (
      (newStatus === 'ALL' && searchFilters.status !== 'ALL') ||
      searchFilters.allStatuses.length === 0
    ) {
      setSearchFilters({
        ...searchFilters,
        status: newStatus,
        allStatuses: [...applicationStatusOptions]
      })
    } else if (newStatus === 'ALL' && searchFilters.status === 'ALL') {
      setSearchFilters({
        ...searchFilters,
        status: newStatus,
        allStatuses: []
      })
    } else {
      setSearchFilters({
        ...searchFilters,
        status: newStatus
      })
    }
  }

  const toggleApplicationType = (type: ApplicationTypeToggle) => () => {
    setSearchFilters({
      ...searchFilters,
      type,
      preschoolType:
        type === 'PRESCHOOL' ? [...preschoolTypes] : searchFilters.preschoolType
    })
  }

  const toggleDate = (toggledDateType: ApplicationDateType) => () => {
    setSearchFilters({
      ...searchFilters,
      dateType: searchFilters.dateType.includes(toggledDateType)
        ? searchFilters.dateType.filter((v) => v !== toggledDateType)
        : [...searchFilters.dateType, toggledDateType]
    })
  }

  const toggleApplicationPreschoolType = (type: PreschoolType) => () => {
    setSearchFilters({
      ...searchFilters,
      preschoolType: searchFilters.preschoolType.includes(type)
        ? searchFilters.preschoolType.filter((v) => v !== type)
        : [...searchFilters.preschoolType, type]
    })
  }

  const toggleAllStatuses = (status: ApplicationStatusOption) => () => {
    setSearchFilters({
      ...searchFilters,
      allStatuses: searchFilters.allStatuses.includes(status)
        ? searchFilters.allStatuses.filter((v) => v !== status)
        : [...searchFilters.allStatuses, status]
    })
  }

  const changeUnits = (selectedUnits: DaycareId[]) => {
    setSearchFilters({
      ...searchFilters,
      units: selectedUnits.map((selectedUnit) => selectedUnit)
    })
  }

  const toggleApplicationDistinctions =
    (distinction: ApplicationDistinctions) => () => {
      setSearchFilters({
        ...searchFilters,
        distinctions: searchFilters.distinctions.includes(distinction)
          ? searchFilters.distinctions.filter((v) => v !== distinction)
          : [...searchFilters.distinctions, distinction]
      })
    }

  return (
    <Filters
      searchPlaceholder={i18n.applications.searchPlaceholder}
      freeText={searchFilters.searchTerms}
      setFreeText={(searchTerms) =>
        setSearchFilters({
          ...searchFilters,
          searchTerms
        })
      }
      onSearch={confirmSearchFilters}
      clearFilters={clearSearchFilters}
      column1={
        <>
          <AreaMultiSelect
            areas={availableAreas.getOrElse([])}
            selected={searchFilters.area}
            onSelect={(area) =>
              setSearchFilters({
                ...searchFilters,
                area: area
              })
            }
          />
          <Gap size="L" />
          <MultiSelectUnitFilter
            units={filteredUnits}
            selectedUnits={searchFilters.units}
            onChange={changeUnits}
            data-qa="unit-selector"
            menuItems={unitFilterMenuItems}
          />
          <Gap size="m" />
          <ApplicationDistinctionsFilter
            toggle={toggleApplicationDistinctions}
            toggled={searchFilters.distinctions}
            disableSecondary={
              searchFilters.area.length === 0 &&
              searchFilters.units.length === 0
            }
          />
          <Gap size="L" />
          <ApplicationTypeFilter
            toggled={searchFilters.type}
            toggledPreschool={searchFilters.preschoolType}
            toggle={toggleApplicationType}
            togglePreschool={toggleApplicationPreschoolType}
          />
          <Gap size="L" />
          <ApplicationBasisFilter
            toggled={searchFilters.basis}
            toggle={toggleBasis}
          />
        </>
      }
      column2={
        <Fragment>
          <TransferApplicationsFilter
            selected={searchFilters.transferApplications}
            setSelected={(transferApplications) =>
              setSearchFilters({
                ...searchFilters,
                transferApplications
              })
            }
          />
          <Gap size="XL" />
          <VoucherApplicationsFilter
            selected={searchFilters.voucherApplications}
            setSelected={(voucherApplications) =>
              setSearchFilters({
                ...searchFilters,
                voucherApplications
              })
            }
          />
        </Fragment>
      }
      column3={
        <Fragment>
          <ApplicationStatusFilter
            toggled={searchFilters.status}
            toggledAllStatuses={searchFilters.allStatuses}
            toggle={toggleStatus}
            toggleAllStatuses={toggleAllStatuses}
          />
          <Gap size="XL" />
          <ApplicationDateFilter
            startDate={searchFilters.startDate}
            setStartDate={(startDate) =>
              setSearchFilters({
                ...searchFilters,
                startDate
              })
            }
            endDate={searchFilters.endDate}
            setEndDate={(endDate) =>
              setSearchFilters({
                ...searchFilters,
                endDate
              })
            }
            toggled={searchFilters.dateType}
            toggle={toggleDate}
          />
        </Fragment>
      }
    />
  )
})

type AreaMultiSelectProps = {
  areas: DaycareCareArea[]
  selected: AreaId[]
  onSelect: (areaIds: AreaId[]) => void
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

export const preschoolTypes = [
  'PRESCHOOL_ONLY',
  'PRESCHOOL_DAYCARE',
  'PRESCHOOL_CLUB',
  ...(featureFlags.preparatory
    ? (['PREPARATORY_ONLY', 'PREPARATORY_DAYCARE'] as const)
    : ([] as const)),
  'DAYCARE_ONLY'
] as const

export type PreschoolType = (typeof preschoolTypes)[number]

interface ApplicationTypeFilterProps {
  toggled: ApplicationTypeToggle
  toggledPreschool: PreschoolType[]
  toggle: (type: ApplicationTypeToggle) => () => void
  togglePreschool: (type: PreschoolType) => () => void
}

function ApplicationTypeFilter({
  toggled,
  toggledPreschool,
  toggle,
  togglePreschool
}: ApplicationTypeFilterProps) {
  const { i18n } = useTranslation()

  const types: ApplicationTypeToggle[] = [...applicationTypes, 'ALL']

  return (
    <>
      <Label>{i18n.applications.list.type}</Label>
      <Gap size="xs" />
      <FixedSpaceColumn spacing="xs">
        {types.map((id) =>
          id !== 'PRESCHOOL' ? (
            <Radio
              key={id}
              label={i18n.applications.types[id]}
              checked={toggled === id}
              onChange={toggle(id)}
              data-qa={`application-type-filter-${id}`}
              small
            />
          ) : (
            <Fragment key={id}>
              <Radio
                key={id}
                label={
                  <>
                    {i18n.applications.types[id]}
                    <ApplicationOpenIcon
                      icon={toggled === id ? faAngleUp : faAngleDown}
                      size="lg"
                      color={colors.grayscale.g70}
                    />
                  </>
                }
                ariaLabel={i18n.applications.types[id]}
                checked={toggled === id}
                onChange={toggle(id)}
                data-qa={`application-type-filter-${id}`}
                small
              />
              {toggled === id && (
                <CustomDiv spacing="xs">
                  {preschoolTypes.map((type) => (
                    <Checkbox
                      key={type}
                      label={i18n.applications.types[type]}
                      checked={toggledPreschool.includes(type)}
                      onChange={togglePreschool(type)}
                      data-qa={`application-type-filter-preschool-${type}`}
                    />
                  ))}
                </CustomDiv>
              )}
            </Fragment>
          )
        )}
      </FixedSpaceColumn>
    </>
  )
}

const CustomDiv = styled(FixedSpaceColumn)`
  margin-left: 18px;
  padding-left: 32px;
  border-left: 1px solid ${colors.grayscale.g70};
`
