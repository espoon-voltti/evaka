// SPDX-FileCopyrightText: 2017-2022 City of Espoo
//
// SPDX-License-Identifier: LGPL-2.1-or-later

import { FontAwesomeIcon } from '@fortawesome/react-fontawesome'
import React, { Fragment, useCallback, useContext, useEffect } from 'react'

import type {
  PaymentDistinctiveParams,
  PaymentStatus
} from 'lib-common/generated/api-types/invoicing'
import type { DaycareId } from 'lib-common/generated/api-types/shared'
import type LocalDate from 'lib-common/local-date'
import Checkbox from 'lib-components/atoms/form/Checkbox'
import Radio from 'lib-components/atoms/form/Radio'
import { FixedSpaceColumn } from 'lib-components/layout/flex-helpers'
import DatePicker from 'lib-components/molecules/date-picker/DatePicker'
import { Label } from 'lib-components/typography'
import { Gap } from 'lib-components/white-space'
import colors from 'lib-customizations/common'
import { fasExclamationTriangle } from 'lib-icons'

import { useTranslation } from '../../state/i18n'
import { InvoicingUiContext } from '../../state/invoicing-ui'
import { AreaFilter, Filters, UnitFilter } from '../common/Filters'
import { FlexRow } from '../common/styled/containers'

export default React.memo(function PaymentFilters() {
  const {
    payments: {
      searchFilters,
      setSearchFilters,
      confirmSearchFilters,
      clearSearchFilters
    },
    shared: { availableAreas, allDaycareUnits: units }
  } = useContext(InvoicingUiContext)

  const { i18n } = useTranslation()

  // remove selected unit filter if the unit is not included in the selected areas
  useEffect(() => {
    if (
      searchFilters.unit &&
      units.isSuccess &&
      !units.value.map(({ id }) => id).includes(searchFilters.unit)
    ) {
      setSearchFilters({ ...searchFilters, unit: null })
    }
  }, [searchFilters, setSearchFilters, units])

  const toggleArea = useCallback(
    (code: string) => () => {
      setSearchFilters((old) =>
        old.area.includes(code)
          ? {
              ...old,
              area: old.area.filter((v) => v !== code)
            }
          : {
              ...old,
              area: [...old.area, code]
            }
      )
    },
    [setSearchFilters]
  )

  const selectUnit = useCallback(
    (unit: DaycareId | undefined) =>
      setSearchFilters((old) => ({ ...old, unit: unit ?? null })),
    [setSearchFilters]
  )

  const toggleStatus = useCallback(
    (status: PaymentStatus) => () =>
      setSearchFilters((old) => ({ ...old, status })),
    [setSearchFilters]
  )

  const toggleServiceNeed = useCallback(
    (id: PaymentDistinctiveParams) => () =>
      setSearchFilters((old) =>
        old.distinctions.includes(id)
          ? {
              ...old,
              distinctions: old.distinctions.filter((v) => v !== id)
            }
          : {
              ...old,
              distinctions: [...old.distinctions, id]
            }
      ),
    [setSearchFilters]
  )

  const setPaymentDateStart = useCallback(
    (paymentDateStart: LocalDate | null) =>
      setSearchFilters((old) => ({ ...old, paymentDateStart })),
    [setSearchFilters]
  )

  const setPaymentDateEnd = useCallback(
    (paymentDateEnd: LocalDate | null) =>
      setSearchFilters((old) => ({ ...old, paymentDateEnd })),
    [setSearchFilters]
  )

  return (
    <Filters
      searchPlaceholder={i18n.filters.paymentFreeTextPlaceholder}
      freeText={searchFilters.searchTerms}
      setFreeText={(s) =>
        setSearchFilters((prev) => ({ ...prev, searchTerms: s }))
      }
      onSearch={confirmSearchFilters}
      clearFilters={clearSearchFilters}
      column1={
        <>
          <AreaFilter
            areas={availableAreas.getOrElse([])}
            toggled={searchFilters.area}
            toggle={toggleArea}
          />
          <Gap size="L" />
          <UnitFilter
            units={units.getOrElse([])}
            selected={units
              .map((us) => us.find((unit) => unit.id === searchFilters.unit))
              .getOrElse(undefined)}
            select={selectUnit}
          />
        </>
      }
      column2={
        <Fragment>
          <PaymentDistinctionsFilter
            toggled={searchFilters.distinctions}
            toggle={toggleServiceNeed}
          />
        </Fragment>
      }
      column3={
        <Fragment>
          <PaymentStatusFilter
            toggled={searchFilters.status}
            toggle={toggleStatus}
          />
          <Gap size="L" />
          <PaymentDateFilter
            startDate={searchFilters.paymentDateStart}
            setStartDate={setPaymentDateStart}
            endDate={searchFilters.paymentDateEnd}
            setEndDate={setPaymentDateEnd}
          />
        </Fragment>
      }
    />
  )
})

interface PaymentStatusFilterProps {
  toggled: PaymentStatus
  toggle: (status: PaymentStatus) => () => void
}

export function PaymentStatusFilter({
  toggled,
  toggle
}: PaymentStatusFilterProps) {
  const { i18n } = useTranslation()

  const statuses: PaymentStatus[] = ['DRAFT', 'CONFIRMED', 'SENT']

  return (
    <>
      <Label>{i18n.filters.status}</Label>
      <Gap size="xs" />
      <FixedSpaceColumn spacing="xs">
        {statuses.map((id) => (
          <Radio
            key={id}
            label={i18n.payments.status[id]}
            checked={toggled === id}
            onChange={toggle(id)}
            small
            data-qa={`status-filter-${id}`}
          />
        ))}
      </FixedSpaceColumn>
    </>
  )
}

interface PaymentDateFilterProps {
  startDate: LocalDate | null
  setStartDate: (startDate: LocalDate | null) => void
  endDate: LocalDate | null
  setEndDate: (endDate: LocalDate | null) => void
}

export function PaymentDateFilter({
  startDate,
  setStartDate,
  endDate,
  setEndDate
}: PaymentDateFilterProps) {
  const { i18n } = useTranslation()

  return (
    <>
      <Label>{i18n.filters.paymentDate}</Label>
      <FlexRow>
        <DatePicker date={startDate} onChange={setStartDate} locale="fi" />
        <Gap horizontal size="xs" />
        <DatePicker date={endDate} onChange={setEndDate} locale="fi" />
      </FlexRow>
      {startDate && endDate && startDate.isAfter(endDate) ? (
        <>
          <Gap size="xs" />
          <span>
            {i18n.common.checkDates}
            <Gap size="xs" horizontal />
            <FontAwesomeIcon
              icon={fasExclamationTriangle}
              color={colors.status.warning}
            />
          </span>
        </>
      ) : null}
    </>
  )
}

interface PaymentDistinctionsFilterProps {
  toggled: PaymentDistinctiveParams[]
  toggle: (distinctions: PaymentDistinctiveParams) => () => void
}

export function PaymentDistinctionsFilter({
  toggled,
  toggle
}: PaymentDistinctionsFilterProps) {
  const { i18n } = useTranslation()

  const distinctiveDetails: PaymentDistinctiveParams[] = [
    'MISSING_PAYMENT_DETAILS'
  ]

  return (
    <>
      <Label>{i18n.filters.distinctiveDetails}</Label>
      <Gap size="xs" />
      {distinctiveDetails.map((id) => (
        <Checkbox
          key={id}
          label={i18n.payments.distinctiveDetails[id]}
          checked={toggled.includes(id)}
          onChange={toggle(id)}
          data-qa={`fee-decision-distinction-filter-${id}`}
        />
      ))}
    </>
  )
}
