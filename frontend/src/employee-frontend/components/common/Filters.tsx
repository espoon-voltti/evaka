// SPDX-FileCopyrightText: 2017-2022 City of Espoo
//
// SPDX-License-Identifier: LGPL-2.1-or-later

import { FontAwesomeIcon } from '@fortawesome/react-fontawesome'
import React, { Fragment, useCallback, useMemo } from 'react'
import styled from 'styled-components'

import {
  ApplicationTypeToggle,
  TransferApplicationFilter
} from 'lib-common/generated/api-types/application'
import {
  DaycareCareArea,
  ProviderType
} from 'lib-common/generated/api-types/daycare'
import {
  DistinctiveParams,
  feeDecisionDistinctiveParams,
  FeeDecisionDifference,
  feeDecisionDifferences,
  FeeDecisionStatus,
  InvoiceDistinctiveParams,
  InvoiceStatus,
  VoucherValueDecisionDifference,
  voucherValueDecisionDifferences,
  voucherValueDecisionDistinctiveParams,
  VoucherValueDecisionDistinctiveParams,
  VoucherValueDecisionStatus
} from 'lib-common/generated/api-types/invoicing'
import LocalDate from 'lib-common/local-date'
import RoundIcon from 'lib-components/atoms/RoundIcon'
import Tooltip from 'lib-components/atoms/Tooltip'
import IconButton from 'lib-components/atoms/buttons/IconButton'
import InlineButton from 'lib-components/atoms/buttons/InlineButton'
import Combobox from 'lib-components/atoms/dropdowns/Combobox'
import Checkbox from 'lib-components/atoms/form/Checkbox'
import MultiSelect from 'lib-components/atoms/form/MultiSelect'
import Radio from 'lib-components/atoms/form/Radio'
import {
  FixedSpaceColumn,
  FixedSpaceRow
} from 'lib-components/layout/flex-helpers'
import { DatePickerClearableDeprecated } from 'lib-components/molecules/DatePickerDeprecated'
import { Label } from 'lib-components/typography'
import { defaultMargins, Gap } from 'lib-components/white-space'
import colors, { applicationBasisColors } from 'lib-customizations/common'
import {
  applicationTypes,
  featureFlags,
  unitProviderTypes
} from 'lib-customizations/employee'
import {
  faAngleDown,
  faAngleUp,
  faPaperclip,
  faSearch,
  fasExclamationTriangle,
  faTimes,
  faTrash
} from 'lib-icons'

import { useTranslation } from '../../state/i18n'
import { FinanceDecisionHandlerOption } from '../../state/invoicing-ui'
import { ApplicationSummaryStatus } from '../../types/application'
import { FeeDecisionDifferenceIcons } from '../fee-decisions/FeeDecisionDifferenceIcon'
import { VoucherValueDecisionDifferenceIcons } from '../voucher-value-decisions/VoucherValueDecisionDifferenceIcon'

import { FlexRow } from './styled/containers'

interface Props {
  freeText?: string
  setFreeText?: (s: string) => void
  clearFilters: () => void
  column1?: React.JSX.Element
  column2?: React.JSX.Element
  column3?: React.JSX.Element
  searchPlaceholder?: string
  clearMargin?: number
}

interface ClearOptionsProps {
  clearMargin?: number
}

const ClearOptions = styled.div<ClearOptionsProps>`
  align-self: flex-end;

  button {
    margin-top: ${(p) => (p.clearMargin ? `${p.clearMargin}px` : '0px')};
    display: ${(p) => (p.clearMargin ? 'block' : 'inline-block')};
    padding: calc(0.375em - 1px) 0.75em;
    height: 40px;
  }
`

const FiltersContainer = styled.div`
  position: relative;
  z-index: 4;
  display: flex;
  flex-direction: column;
`

const FilterColumns = styled.div`
  display: grid;
  grid-template-columns: repeat(3, minmax(0, 1fr));
`

const Column = styled.div`
  display: flex;
  flex-direction: column;
  padding: 0.75rem;
`

export function Filters({
  freeText,
  setFreeText,
  clearFilters,
  column1,
  column2,
  column3,
  searchPlaceholder,
  clearMargin
}: Props) {
  const { i18n } = useTranslation()
  return (
    <FiltersContainer>
      {setFreeText && (
        <FreeTextSearch
          value={freeText || ''}
          setValue={setFreeText}
          placeholder={searchPlaceholder}
        />
      )}
      <Gap size="s" />
      <FilterColumns>
        <Column>{column1}</Column>
        <Column>{column2}</Column>
        <Column>{column3}</Column>
      </FilterColumns>
      <ClearOptions clearMargin={clearMargin}>
        <InlineButton
          icon={faTrash}
          onClick={clearFilters}
          text={i18n.filters.clear}
        />
      </ClearOptions>
    </FiltersContainer>
  )
}

const SearchInputContainer = styled.div`
  height: 50px;
  display: flex;
  justify-content: center;
  align-items: center;
`

const SearchInput = styled.input<{ background?: string }>`
  width: 100%;
  border: none;
  background: ${(p) => p.background ?? colors.grayscale.g4};
  display: flex;
  flex-direction: row;
  flex-wrap: nowrap;
  padding: 0.75rem;
  padding-left: 55px;
  font-size: 17px;
  outline: none;
  margin-left: -38px;
  margin-right: -25px;
  color: ${colors.grayscale.g100};

  &::placeholder {
    font-style: italic;
    font-size: 17px;
    color: ${colors.grayscale.g70};
  }

  &:focus {
    border-width: 2px;
    border-radius: 2px;
    border-style: solid;
    border-color: ${colors.main.m2Focus};
    margin-top: -2px;
    padding-left: 53px;
    margin-bottom: -2px;
  }
`

const CustomIcon = styled(FontAwesomeIcon)`
  color: ${colors.grayscale.g70};
  margin: 0 0.5rem;
  position: relative;
  left: 10px;
  font-size: 22px;
`

const CustomIconButton = styled(IconButton)`
  float: right;
  position: relative;
  color: ${colors.grayscale.g35};
  right: 20px;
`

type FreeTextSearchProps = {
  value: string
  setValue: (s: string) => void
  placeholder?: string
  background?: string
}

export function FreeTextSearch({
  value,
  setValue,
  placeholder,
  background
}: FreeTextSearchProps) {
  const clear = useCallback(() => setValue(''), [setValue])
  const { i18n } = useTranslation()

  return (
    <SearchInputContainer>
      <CustomIcon icon={faSearch} />
      <SearchInput
        placeholder={placeholder}
        value={value}
        onChange={(e) => setValue(e.target.value)}
        data-qa="free-text-search-input"
        background={background}
      />
      <CustomIconButton
        icon={faTimes}
        onClick={clear}
        size="m"
        aria-label={i18n.common.clear}
      />
    </SearchInputContainer>
  )
}

interface AreaFilterProps {
  areas: DaycareCareArea[]
  toggled: string[]
  toggle: (area: string) => () => void
  showAll?: boolean
}

export function AreaFilter({
  areas,
  toggled,
  toggle,
  showAll
}: AreaFilterProps) {
  const { i18n } = useTranslation()

  return (
    <>
      <Label>{i18n.filters.area}</Label>
      <Gap size="xs" />
      <FixedSpaceColumn spacing="xs">
        {areas
          .sort((a, b) => (a.name > b.name ? 1 : b.name > a.name ? -1 : 0))
          .map(({ name, shortName }) => (
            <Checkbox
              key={shortName}
              label={name}
              checked={toggled.includes(shortName) || toggled.includes('All')}
              onChange={toggle(shortName)}
              data-qa={`area-filter-${shortName}`}
            />
          ))}
        {showAll && (
          <Checkbox
            label={i18n.common.all}
            checked={toggled.includes('All')}
            onChange={toggle('All')}
            data-qa="area-filter-All"
          />
        )}
      </FixedSpaceColumn>
    </>
  )
}

interface UnitFilterProps {
  units: { id: string; label: string }[]
  selected?: { id: string; label: string }
  select: (unit?: string) => void
}

export const UnitFilter = React.memo(function UnitFilter({
  units,
  selected,
  select
}: UnitFilterProps) {
  const { i18n } = useTranslation()
  const options = units.map(({ id, label }) => ({ id, label, value: id }))
  return (
    <>
      <Label>{i18n.filters.unit}</Label>
      <Gap size="xs" />
      <Combobox
        items={options}
        placeholder={i18n.filters.unitPlaceholder}
        selectedItem={selected ?? null}
        onChange={(option) => select(option?.id)}
        clearable
        fullWidth
        getItemLabel={(item) => item.label}
      />
    </>
  )
})

interface FinanceDecisionHandlerFilterProps {
  financeDecisionHandlers: FinanceDecisionHandlerOption[]
  selected?: FinanceDecisionHandlerOption
  select: (decisionHandler?: string) => void
}

export const FinanceDecisionHandlerFilter = React.memo(
  function FinanceDecisionHandlerFilter({
    financeDecisionHandlers,
    selected,
    select
  }: FinanceDecisionHandlerFilterProps) {
    const { i18n } = useTranslation()
    const options = financeDecisionHandlers.map(
      ({ value: id, label: name }) => ({
        id: id,
        name: name,
        value: id,
        label: name
      })
    )
    return (
      <>
        <Label>{i18n.filters.financeDecisionHandler}</Label>
        <Gap size="xs" />
        <Combobox
          items={options}
          placeholder={i18n.filters.financeDecisionHandlerPlaceholder}
          selectedItem={selected ?? null}
          onChange={(option) => select(option?.value)}
          clearable
          fullWidth
          getItemLabel={(item) => item.label}
        />
      </>
    )
  }
)

interface FeeDecisionDifferenceFilterProps {
  toggled: FeeDecisionDifference[]
  toggle: (difference: FeeDecisionDifference) => void
}

export function FeeDecisionDifferenceFilter({
  toggled,
  toggle
}: FeeDecisionDifferenceFilterProps) {
  const { i18n } = useTranslation()

  return (
    <>
      <Label>{i18n.filters.difference}</Label>
      <Gap size="xs" />
      <FixedSpaceRow spacing="xxs" flexWrap="wrap">
        <FeeDecisionDifferenceIcons
          difference={feeDecisionDifferences}
          toggle={toggle}
          toggled={toggled}
        />
      </FixedSpaceRow>
    </>
  )
}

interface VoucherValueDecisionDifferenceFilterProps {
  toggled: VoucherValueDecisionDifference[]
  toggle: (difference: VoucherValueDecisionDifference) => void
}

export function VoucherValueDecisionDifferenceFilter({
  toggled,
  toggle
}: VoucherValueDecisionDifferenceFilterProps) {
  const { i18n } = useTranslation()

  return (
    <>
      <Label>{i18n.filters.difference}</Label>
      <Gap size="xs" />
      <FixedSpaceRow spacing="xxs" flexWrap="wrap">
        <VoucherValueDecisionDifferenceIcons
          difference={voucherValueDecisionDifferences}
          toggle={toggle}
          toggled={toggled}
        />
      </FixedSpaceRow>
    </>
  )
}

interface FeeDecisionStatusFilterProps {
  toggled: FeeDecisionStatus[]
  toggle: (status: FeeDecisionStatus) => () => void
}

export function FeeDecisionStatusFilter({
  toggled,
  toggle
}: FeeDecisionStatusFilterProps) {
  const { i18n } = useTranslation()

  const statuses: FeeDecisionStatus[] = [
    'DRAFT',
    'WAITING_FOR_SENDING',
    'WAITING_FOR_MANUAL_SENDING',
    'SENT',
    'ANNULLED',
    'IGNORED'
  ]

  return (
    <>
      <Label>{i18n.filters.status}</Label>
      <Gap size="xs" />
      <FixedSpaceColumn spacing="xs">
        {statuses.map((id) => (
          <Checkbox
            key={id}
            label={i18n.feeDecision.status[id]}
            checked={toggled.includes(id)}
            onChange={toggle(id)}
            data-qa={`fee-decision-status-filter-${id}`}
          />
        ))}
      </FixedSpaceColumn>
    </>
  )
}

interface FeeDecisionDistinctionsFilterProps {
  toggled: DistinctiveParams[]
  toggle: (distinctiveDetails: DistinctiveParams) => () => void
}

export function FeeDecisionDistinctionsFilter({
  toggled,
  toggle
}: FeeDecisionDistinctionsFilterProps) {
  const { i18n } = useTranslation()

  return (
    <>
      <Label>{i18n.filters.distinctiveDetails}</Label>
      <Gap size="xs" />
      <FixedSpaceColumn spacing="xs">
        {feeDecisionDistinctiveParams.map((id) => {
          if (
            id === 'PRESCHOOL_CLUB' &&
            !featureFlags.feeDecisionPreschoolClubFilter
          ) {
            return null
          }
          return (
            <Checkbox
              key={id}
              label={i18n.feeDecision.distinctiveDetails[id]}
              checked={toggled.includes(id)}
              onChange={toggle(id)}
              data-qa={`fee-decision-distinction-filter-${id}`}
            />
          )
        })}
      </FixedSpaceColumn>
    </>
  )
}

interface FeeDecisionDateFilterProps {
  startDate: LocalDate | undefined
  setStartDate: (startDate: LocalDate | undefined) => void
  endDate: LocalDate | undefined
  setEndDate: (endDate: LocalDate | undefined) => void
  searchByStartDate: boolean
  setSearchByStartDate: (searchByStartDate: boolean) => void
}

export function FeeDecisionDateFilter({
  startDate,
  setStartDate,
  endDate,
  setEndDate,
  searchByStartDate,
  setSearchByStartDate
}: FeeDecisionDateFilterProps) {
  const { i18n } = useTranslation()

  return (
    <>
      <Label>{i18n.filters.validityPeriod}</Label>
      <FlexRow>
        <DatePickerClearableDeprecated
          date={startDate}
          onChange={setStartDate}
          data-qa="fee-decisions-start-date"
          onCleared={() => setStartDate(undefined)}
        />
        <Gap horizontal size="xs" />
        <DatePickerClearableDeprecated
          date={endDate}
          onChange={setEndDate}
          data-qa="fee-decisions-start-date"
          onCleared={() => setEndDate(undefined)}
        />
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
      <Gap size="s" />
      <Checkbox
        label={i18n.filters.searchByStartDate}
        checked={searchByStartDate}
        onChange={setSearchByStartDate}
        data-qa="fee-decision-search-by-start-date"
      />
    </>
  )
}

export const ValueDecisionStatusFilter = React.memo(
  function ValueDecisionStatusFilter({
    toggled,
    toggle
  }: {
    toggled: VoucherValueDecisionStatus[]
    toggle(v: VoucherValueDecisionStatus): () => void
  }) {
    const { i18n } = useTranslation()

    const statuses: VoucherValueDecisionStatus[] = [
      'DRAFT',
      'WAITING_FOR_SENDING',
      'WAITING_FOR_MANUAL_SENDING',
      'SENT',
      'ANNULLED',
      'IGNORED'
    ]

    return (
      <>
        <Label>{i18n.filters.status}</Label>
        <Gap size="xs" />
        <FixedSpaceColumn spacing="xs">
          {statuses.map((id) => (
            <Checkbox
              key={id}
              label={i18n.valueDecision.status[id]}
              checked={toggled.includes(id)}
              onChange={toggle(id)}
              data-qa={`value-decision-status-filter-${id}`}
            />
          ))}
        </FixedSpaceColumn>
      </>
    )
  }
)

interface ValueDecisionDateFilterProps {
  startDate: LocalDate | undefined
  setStartDate: (startDate: LocalDate | undefined) => void
  endDate: LocalDate | undefined
  setEndDate: (endDate: LocalDate | undefined) => void
  searchByStartDate: boolean
  setSearchByStartDate: (searchByStartDate: boolean) => void
}

export function ValueDecisionDateFilter({
  startDate,
  setStartDate,
  endDate,
  setEndDate,
  searchByStartDate,
  setSearchByStartDate
}: ValueDecisionDateFilterProps) {
  const { i18n } = useTranslation()

  return (
    <>
      <Label>{i18n.filters.validityPeriod}</Label>
      <FlexRow>
        <DatePickerClearableDeprecated
          date={startDate}
          onChange={setStartDate}
          data-qa="value-decisions-start-date"
          onCleared={() => setStartDate(undefined)}
        />
        <Gap horizontal size="xs" />
        <DatePickerClearableDeprecated
          date={endDate}
          onChange={setEndDate}
          data-qa="value-decisions-end-date"
          onCleared={() => setEndDate(undefined)}
        />
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

      <Gap size="s" />

      <Checkbox
        label={i18n.filters.searchByStartDate}
        checked={searchByStartDate}
        onChange={setSearchByStartDate}
        data-qa="value-decision-search-by-start-date"
      />
    </>
  )
}

interface VoucherValueDecisionDistinctionsFilterProps {
  toggled: VoucherValueDecisionDistinctiveParams[]
  toggle: (
    distinctiveDetails: VoucherValueDecisionDistinctiveParams
  ) => () => void
}

export function VoucherValueDecisionDistinctionsFilter({
  toggled,
  toggle
}: VoucherValueDecisionDistinctionsFilterProps) {
  const { i18n } = useTranslation()

  return (
    <>
      <Label>{i18n.filters.distinctiveDetails}</Label>
      <Gap size="xs" />
      <FixedSpaceColumn spacing="xs">
        {voucherValueDecisionDistinctiveParams.map((id) => (
          <Checkbox
            key={id}
            label={i18n.feeDecision.distinctiveDetails[id]}
            checked={toggled.includes(id)}
            onChange={toggle(id)}
            data-qa={`voucher-value-decision-distinction-filter-${id}`}
          />
        ))}
      </FixedSpaceColumn>
    </>
  )
}

interface InvoiceStatusFilterProps {
  toggled: InvoiceStatus
  toggle: (status: InvoiceStatus) => () => void
}

export function InvoiceStatusFilter({
  toggled,
  toggle
}: InvoiceStatusFilterProps) {
  const { i18n } = useTranslation()

  const statuses: InvoiceStatus[] = ['DRAFT', 'WAITING_FOR_SENDING', 'SENT']

  return (
    <>
      <Label>{i18n.filters.status}</Label>
      <Gap size="xs" />
      <FixedSpaceColumn spacing="xs">
        {statuses.map((id) => (
          <Radio
            key={id}
            label={i18n.invoice.status[id]}
            checked={toggled === id}
            onChange={toggle(id)}
            data-qa={`invoice-status-filter-${id}`}
            small
          />
        ))}
      </FixedSpaceColumn>
    </>
  )
}

interface InvoiceDateFilterProps {
  startDate: LocalDate | undefined
  setStartDate: (startDate: LocalDate | undefined) => void
  endDate: LocalDate | undefined
  setEndDate: (endDate: LocalDate | undefined) => void
  searchByStartDate: boolean
  setUseCustomDatesForInvoiceSending: (
    useCustomDatesForInvoiceSending: boolean
  ) => void
}

export function InvoiceDateFilter({
  startDate,
  setStartDate,
  endDate,
  setEndDate,
  searchByStartDate,
  setUseCustomDatesForInvoiceSending
}: InvoiceDateFilterProps) {
  const { i18n } = useTranslation()

  return (
    <>
      <Label>{i18n.filters.invoiceDate}</Label>
      <FlexRow>
        <DatePickerClearableDeprecated
          date={startDate}
          onChange={setStartDate}
          data-qa="invoices-start-date"
          onCleared={() => setStartDate(undefined)}
        />
        <Gap horizontal size="xs" />
        <DatePickerClearableDeprecated
          date={endDate}
          onChange={setEndDate}
          data-qa="invoices-end-date"
          onCleared={() => setEndDate(undefined)}
        />
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

      <Gap size="s" />

      <Checkbox
        label={i18n.filters.invoiceSearchByStartDate}
        checked={searchByStartDate}
        onChange={setUseCustomDatesForInvoiceSending}
        data-qa="invoice-search-by-start-date"
      />
    </>
  )
}

interface InvoiceDistinctionsFilterProps {
  toggled: InvoiceDistinctiveParams[]
  toggle: (distinctiveDetails: InvoiceDistinctiveParams) => () => void
}

export function InvoiceDistinctionsFilter({
  toggled,
  toggle
}: InvoiceDistinctionsFilterProps) {
  const { i18n } = useTranslation()

  const distinctiveDetails: InvoiceDistinctiveParams[] = ['MISSING_ADDRESS']

  return (
    <>
      <Label>{i18n.filters.distinctiveDetails}</Label>
      <Gap size="xs" />
      {distinctiveDetails.map((id) => (
        <Checkbox
          key={id}
          label={i18n.invoice.distinctiveDetails[id]}
          checked={toggled.includes(id)}
          onChange={toggle(id)}
          data-qa={`fee-decision-distinction-filter-${id}`}
        />
      ))}
    </>
  )
}

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

const CustomDiv = styled(FixedSpaceColumn)`
  margin-left: 18px;
  padding-left: 32px;
  border-left: 1px solid ${colors.grayscale.g70};
`

const ApplicationOpenIcon = styled(FontAwesomeIcon)`
  margin-left: 10px;
`

export function ApplicationTypeFilter({
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
          id != 'PRESCHOOL' ? (
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

interface ApplicationStatusFilterProps {
  toggled: ApplicationSummaryStatusOptions
  toggledAllStatuses: ApplicationSummaryStatusAllOptions[]
  toggle: (status: ApplicationSummaryStatusOptions) => () => void
  toggleAllStatuses: (status: ApplicationSummaryStatusAllOptions) => () => void
}

export type ApplicationSummaryStatusAllOptions =
  | ApplicationSummaryStatus
  | 'ALL'

export type ApplicationSummaryStatusOptions =
  | 'SENT'
  | 'WAITING_PLACEMENT'
  | 'WAITING_DECISION'
  | 'WAITING_UNIT_CONFIRMATION'
  | 'ALL'

const CustomDivWithMargin = styled(CustomDiv)`
  margin-top: ${defaultMargins.xs};
`

export const applicationSummaryStatuses: ApplicationSummaryStatusOptions[] = [
  'SENT',
  'WAITING_PLACEMENT',
  'WAITING_DECISION',
  'WAITING_UNIT_CONFIRMATION',
  'ALL'
]

export const applicationSummaryAllStatuses: ApplicationSummaryStatusAllOptions[] =
  [
    'SENT',
    'WAITING_PLACEMENT',
    'WAITING_DECISION',
    'WAITING_UNIT_CONFIRMATION',
    'WAITING_MAILING',
    'WAITING_CONFIRMATION',
    'REJECTED',
    'ACTIVE',
    'CANCELLED'
  ]

export function ApplicationStatusFilter({
  toggled,
  toggledAllStatuses,
  toggle,
  toggleAllStatuses
}: ApplicationStatusFilterProps) {
  const { i18n } = useTranslation()

  return (
    <>
      <Label>{i18n.application.state.title}</Label>
      <Gap size="xs" />
      <FixedSpaceColumn spacing="xs">
        {applicationSummaryStatuses.map(
          (id: ApplicationSummaryStatusOptions) => (
            <Radio
              key={id}
              label={
                <>
                  {i18n.application.statuses[id]}
                  {id === 'ALL' ? (
                    <ApplicationOpenIcon
                      icon={toggled === id ? faAngleUp : faAngleDown}
                      size="lg"
                      color={colors.grayscale.g70}
                    />
                  ) : undefined}
                </>
              }
              ariaLabel={i18n.application.statuses[id]}
              checked={toggled === id}
              onChange={toggle(id)}
              data-qa={`application-status-filter-${id}`}
              small
            />
          )
        )}
      </FixedSpaceColumn>
      {toggled === 'ALL' && (
        <CustomDivWithMargin spacing="xs">
          {applicationSummaryAllStatuses.map((id) => (
            <Checkbox
              key={id}
              label={i18n.application.statuses[id]}
              checked={toggledAllStatuses.includes(id)}
              onChange={toggleAllStatuses(id)}
              data-qa={`application-status-filter-all-${id}`}
            />
          ))}
          <InlineButton
            onClick={toggle('ALL')}
            text={
              toggledAllStatuses.length > 0
                ? i18n.application.unselectAll
                : i18n.application.selectAll
            }
          />
        </CustomDivWithMargin>
      )}
    </>
  )
}

interface ApplicationDateFilterProps {
  startDate: LocalDate | undefined
  setStartDate: (startDate: LocalDate | undefined) => void
  endDate: LocalDate | undefined
  setEndDate: (endDate: LocalDate | undefined) => void
  toggled: ApplicationDateType[]
  toggle: (applicationDateType: ApplicationDateType) => () => void
}

export type ApplicationDateType = 'DUE' | 'START' | 'ARRIVAL'

export function ApplicationDateFilter({
  startDate,
  setStartDate,
  endDate,
  setEndDate,
  toggled,
  toggle
}: ApplicationDateFilterProps) {
  const { i18n } = useTranslation()

  const dates: ApplicationDateType[] = ['DUE', 'START', 'ARRIVAL']

  return (
    <>
      <Label>{i18n.common.date}</Label>
      <Gap size="xs" />
      <FixedSpaceColumn spacing="xs">
        {dates.map((dateType) => (
          <Checkbox
            label={i18n.application.date[dateType]}
            checked={toggled.includes(dateType)}
            onChange={toggle(dateType)}
            key={dateType}
            data-qa={`applications-search-by-${dateType}`}
          />
        ))}
      </FixedSpaceColumn>
      <Gap size="s" />
      <FlexRow>
        <DatePickerClearableDeprecated
          date={startDate}
          onChange={setStartDate}
          data-qa="applications-start-date"
          onCleared={() => setStartDate(undefined)}
        />
        <span>-</span>
        <DatePickerClearableDeprecated
          date={endDate}
          onChange={setEndDate}
          data-qa="applications-end-date"
          onCleared={() => setEndDate(undefined)}
        />
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

export type ApplicationBasis =
  | 'ADDITIONAL_INFO'
  | 'SIBLING_BASIS'
  | 'ASSISTANCE_NEED'
  | 'CLUB_CARE'
  | 'DAYCARE'
  | 'EXTENDED_CARE'
  | 'DUPLICATE_APPLICATION'
  | 'URGENT'
  | 'HAS_ATTACHMENTS'

interface ApplicationBasisFilterProps {
  toggled: ApplicationBasis[]
  toggle: (applicationDateType: ApplicationBasis) => () => void
}

export function ApplicationBasisFilter({
  toggled,
  toggle
}: ApplicationBasisFilterProps) {
  const { i18n } = useTranslation()
  return (
    <>
      <Label>{i18n.applications.basis}</Label>
      <Gap size="xs" />
      <FixedSpaceRow spacing="xxs">
        <Tooltip
          tooltip={i18n.applications.basisTooltip.ADDITIONAL_INFO}
          position="top"
        >
          <RoundIcon
            content="L"
            color={applicationBasisColors.ADDITIONAL_INFO}
            size="m"
            onClick={toggle('ADDITIONAL_INFO')}
            active={toggled.includes('ADDITIONAL_INFO')}
          />
        </Tooltip>
        <Tooltip
          tooltip={i18n.applications.basisTooltip.SIBLING_BASIS}
          position="top"
        >
          <RoundIcon
            content="S"
            color={applicationBasisColors.SIBLING_BASIS}
            size="m"
            onClick={toggle('SIBLING_BASIS')}
            active={toggled.includes('SIBLING_BASIS')}
          />
        </Tooltip>
        <Tooltip
          tooltip={i18n.applications.basisTooltip.ASSISTANCE_NEED}
          position="top"
        >
          <RoundIcon
            content="T"
            color={applicationBasisColors.ASSISTANCE_NEED}
            size="m"
            onClick={toggle('ASSISTANCE_NEED')}
            active={toggled.includes('ASSISTANCE_NEED')}
          />
        </Tooltip>
        <Tooltip
          tooltip={i18n.applications.basisTooltip.CLUB_CARE}
          position="top"
        >
          <RoundIcon
            content="K"
            color={applicationBasisColors.CLUB_CARE}
            size="m"
            onClick={toggle('CLUB_CARE')}
            active={toggled.includes('CLUB_CARE')}
          />
        </Tooltip>
        <Tooltip
          tooltip={i18n.applications.basisTooltip.DAYCARE}
          position="top"
        >
          <RoundIcon
            content="P"
            color={applicationBasisColors.DAYCARE}
            size="m"
            onClick={toggle('DAYCARE')}
            active={toggled.includes('DAYCARE')}
          />
        </Tooltip>
        <Tooltip
          tooltip={i18n.applications.basisTooltip.EXTENDED_CARE}
          position="top"
        >
          <RoundIcon
            content="V"
            color={applicationBasisColors.EXTENDED_CARE}
            size="m"
            onClick={toggle('EXTENDED_CARE')}
            active={toggled.includes('EXTENDED_CARE')}
          />
        </Tooltip>
        <Tooltip
          tooltip={i18n.applications.basisTooltip.DUPLICATE_APPLICATION}
          position="top"
        >
          <RoundIcon
            content="2"
            color={applicationBasisColors.DUPLICATE_APPLICATION}
            size="m"
            onClick={toggle('DUPLICATE_APPLICATION')}
            active={toggled.includes('DUPLICATE_APPLICATION')}
            data-qa="application-basis-DUPLICATE_APPLICATION"
          />
        </Tooltip>
        <Tooltip tooltip={i18n.applications.basisTooltip.URGENT} position="top">
          <RoundIcon
            content="!"
            color={applicationBasisColors.URGENT}
            size="m"
            onClick={toggle('URGENT')}
            active={toggled.includes('URGENT')}
          />
        </Tooltip>
        <Tooltip
          tooltip={i18n.applications.basisTooltip.HAS_ATTACHMENTS}
          position="top"
        >
          <RoundIcon
            content={faPaperclip}
            color={applicationBasisColors.HAS_ATTACHMENTS}
            size="m"
            onClick={toggle('HAS_ATTACHMENTS')}
            active={toggled.includes('HAS_ATTACHMENTS')}
          />
        </Tooltip>
      </FixedSpaceRow>
    </>
  )
}

interface Option {
  id: string
  name: string
}

function getOptionId(option: Option) {
  return option.id
}

function getOptionLabel(option: Option) {
  return option.name
}

interface MultiUnitsProps {
  units: Option[]
  selectedUnits: string[]
  onChange: (v: string[]) => void
  'data-qa': string
}

export function MultiSelectUnitFilter({
  units,
  selectedUnits,
  onChange,
  'data-qa': dataQa
}: MultiUnitsProps) {
  const { i18n } = useTranslation()
  const value = useMemo(
    () => units.filter((unit) => selectedUnits.includes(unit.id)),
    [selectedUnits, units]
  )
  const handleChange = useCallback(
    (selected: Option[]) => onChange(selected.map(({ id }) => id)),
    [onChange]
  )
  return (
    <div data-qa={dataQa}>
      <Label>{i18n.filters.unit}</Label>
      <Gap size="xs" />
      <MultiSelect
        placeholder={i18n.filters.unitPlaceholder}
        value={value}
        options={units}
        onChange={handleChange}
        getOptionId={getOptionId}
        getOptionLabel={getOptionLabel}
      />
    </div>
  )
}

export type ApplicationDistinctions = 'SECONDARY'

interface ApplicationDistinctionsFilterProps {
  toggled: ApplicationDistinctions[]
  toggle: (distinctions: ApplicationDistinctions) => () => void
  disableSecondary: boolean
}

export function ApplicationDistinctionsFilter({
  toggle,
  toggled,
  disableSecondary
}: ApplicationDistinctionsFilterProps) {
  const { i18n } = useTranslation()
  return (
    <Fragment>
      {disableSecondary ? (
        <Tooltip tooltip={i18n.applications.secondaryTooltip} position="top">
          <Checkbox
            label={i18n.applications.distinctiveDetails.SECONDARY}
            checked={toggled.includes('SECONDARY')}
            onChange={toggle('SECONDARY')}
            data-qa="application-distiction-SECONDARY"
            disabled
          />
        </Tooltip>
      ) : (
        <Checkbox
          label={i18n.applications.distinctiveDetails.SECONDARY}
          checked={toggled.includes('SECONDARY')}
          onChange={toggle('SECONDARY')}
          data-qa="application-distiction-SECONDARY"
        />
      )}
    </Fragment>
  )
}

export function TransferApplicationsFilter({
  selected,
  setSelected
}: {
  selected: TransferApplicationFilter
  setSelected: (v: TransferApplicationFilter) => void
}) {
  const { i18n } = useTranslation()
  return (
    <>
      <Label>{i18n.applications.list.transferFilter.title}</Label>
      <Gap size="xs" />
      <FixedSpaceColumn spacing="xs">
        <Radio
          data-qa="filter-transfer-only"
          label={i18n.applications.list.transferFilter.transferOnly}
          checked={selected === 'TRANSFER_ONLY'}
          onChange={() => setSelected('TRANSFER_ONLY')}
          small
        />
        <Radio
          data-qa="filter-transfer-exclude"
          label={i18n.applications.list.transferFilter.hideTransfer}
          checked={selected === 'NO_TRANSFER'}
          onChange={() => setSelected('NO_TRANSFER')}
          small
        />
        <Radio
          data-qa="filter-transfer-all"
          label={i18n.applications.list.transferFilter.all}
          checked={selected === 'ALL'}
          onChange={() => setSelected('ALL')}
          small
        />
      </FixedSpaceColumn>
    </>
  )
}

interface DateFilterProps {
  title: string
  startDate: LocalDate | undefined
  setStartDate: (startDate: LocalDate | undefined) => void
  endDate: LocalDate | undefined
  setEndDate: (endDate: LocalDate | undefined) => void
}

export function DateFilter({
  title,
  startDate,
  setStartDate,
  endDate,
  setEndDate
}: DateFilterProps) {
  const { i18n } = useTranslation()

  return (
    <>
      <Label>{title}</Label>
      <FlexRow>
        <DatePickerClearableDeprecated
          date={startDate}
          onChange={setStartDate}
          data-qa="start-date-filter-input"
          onCleared={() => setStartDate(undefined)}
        />
        <Gap horizontal size="xs" />
        <DatePickerClearableDeprecated
          date={endDate}
          onChange={setEndDate}
          data-qa="end-date-filter-input"
          onCleared={() => setEndDate(undefined)}
        />
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

interface ProviderTypeFilterProps {
  toggled: ProviderType[]
  toggle: (providerType: ProviderType) => () => void
}

export function ProviderTypeFilter({
  toggled,
  toggle
}: ProviderTypeFilterProps) {
  const { i18n } = useTranslation()

  return (
    <>
      <Label>{i18n.filters.providerType}</Label>
      <Gap size="xs" />
      {unitProviderTypes.map((id) => (
        <Checkbox
          key={id}
          label={i18n.common.providerType[id]}
          checked={toggled.includes(id)}
          onChange={toggle(id)}
          data-qa={`provider-type-filter-${id}`}
        />
      ))}
    </>
  )
}
