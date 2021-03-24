// SPDX-FileCopyrightText: 2017-2020 City of Espoo
//
// SPDX-License-Identifier: LGPL-2.1-or-later

import React, { useCallback, Fragment } from 'react'
import styled from 'styled-components'
import { FontAwesomeIcon } from '@fortawesome/react-fontawesome'
import ReactSelect from 'react-select'

import { faSearch, faTimes, faTrash, fasExclamationTriangle } from 'lib-icons'
import LocalDate from 'lib-common/local-date'
import InlineButton from 'lib-components/atoms/buttons/InlineButton'
import { DatePickerClearableDeprecated } from 'lib-components/molecules/DatePickerDeprecated'
import { useTranslation } from '../../state/i18n'
import {
  DecisionDistinctiveDetails,
  FeeDecisionStatus,
  VoucherValueDecisionStatus,
  InvoiceStatus,
  InvoiceDistinctiveDetails
} from '../../types/invoicing'
import { ApplicationSummaryStatus } from '../../types/application'
import {
  FixedSpaceColumn,
  FixedSpaceRow
} from 'lib-components/layout/flex-helpers'
import Checkbox from 'lib-components/atoms/form/Checkbox'
import Radio from 'lib-components/atoms/form/Radio'
import { Gap, defaultMargins } from 'lib-components/white-space'
import { FlexRow } from './styled/containers'
import RoundIcon from 'lib-components/atoms/RoundIcon'
import IconButton from 'lib-components/atoms/buttons/IconButton'
import colors from 'lib-components/colors'
import { faAngleDown, faAngleUp } from 'lib-icons'
import Tooltip from '../../components/common/Tooltip'
import { CareArea } from '../../types/unit'
import { Label, LabelText } from '../../components/common/styled/common'
import { FinanceDecisionHandlerOption } from '../../state/invoicing-ui'
import { ApplicationType } from 'lib-common/api-types/application/enums'

interface Props {
  freeText: string
  setFreeText: (s: string) => void
  clearFilters: () => void
  column1?: JSX.Element
  column2?: JSX.Element
  column3?: JSX.Element
  searchPlaceholder: string
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
      <FreeTextSearch
        value={freeText}
        setValue={setFreeText}
        placeholder={searchPlaceholder}
      />
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
  font-size: 1rem;
  background: ${(p) => p.background ?? colors.greyscale.lightest};
  width: 100%;
  display: flex;
  flex-direction: row;
  flex-wrap: nowrap;
  padding: 0.75rem;
  padding-left: 55px;
  font-size: 17px;
  outline: none;
  margin-left: -38px;
  margin-right: -25px;
  color: ${colors.greyscale.darkest};

  &::placeholder {
    font-style: italic;
    font-size: 17px;
    color: ${colors.greyscale.dark};
  }

  &:focus {
    border-width: 2px;
    border-radius: 2px;
    border-style: solid;
    border-color: ${colors.accents.petrol};
    margin-top: -2px;
    padding-left: 53px;
    margin-bottom: -2px;
  }
`

const CustomIcon = styled(FontAwesomeIcon)`
  color: ${colors.greyscale.dark};
  margin: 0 0.5rem;
  position: relative;
  left: 10px;
  font-size: 22px;
`

const CustomIconButton = styled(IconButton)`
  float: right;
  position: relative;
  color: ${colors.greyscale.medium};
  right: 20px;
`

type FreeTextSearchProps = {
  value: string
  setValue: (s: string) => void
  placeholder: string
  background?: string
}

export function FreeTextSearch({
  value,
  setValue,
  placeholder,
  background
}: FreeTextSearchProps) {
  const clear = useCallback(() => setValue(''), [setValue])

  return (
    <SearchInputContainer>
      <CustomIcon icon={faSearch} />
      <SearchInput
        placeholder={placeholder}
        value={value}
        onChange={(e) => setValue(e.target.value)}
        data-qa="free-text-search-input"
        background={background}
      ></SearchInput>
      <CustomIconButton icon={faTimes} onClick={clear} size={'m'} />
    </SearchInputContainer>
  )
}

interface AreaFilterProps {
  areas: CareArea[]
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
      <Label>
        <LabelText>{i18n.filters.area}</LabelText>
      </Label>
      <FixedSpaceColumn spacing={'xs'}>
        {areas
          .sort((a, b) => (a.name > b.name ? 1 : b.name > a.name ? -1 : 0))
          .map(({ name, shortName }) => (
            <Checkbox
              key={shortName}
              label={name}
              checked={toggled.includes(shortName) || toggled.includes('All')}
              onChange={toggle(shortName)}
              dataQa={`area-filter-${shortName}`}
            />
          ))}
        {showAll && (
          <Checkbox
            label={i18n.common.all}
            checked={toggled.includes('All')}
            onChange={toggle('All')}
            dataQa={`area-filter-All`}
          />
        )}
      </FixedSpaceColumn>
    </>
  )
}

interface UnitFilterProps {
  units: { id: string; label: string }[]
  selected?: { id: string; label: string }
  select: (unit: string) => void
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
      <Label>
        <LabelText>{i18n.filters.unit}</LabelText>
        <ReactSelect
          placeholder={i18n.filters.unitPlaceholder}
          options={options}
          onChange={(option) =>
            option && 'id' in option ? select(option.id) : undefined
          }
          value={selected}
        />
      </Label>
    </>
  )
})

interface FinanceDecisionHandlerFilterProps {
  financeDecisionHandlers: FinanceDecisionHandlerOption[]
  selected?: FinanceDecisionHandlerOption
  select: (decisionHandler: string) => void
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
        <Label>
          <LabelText>{i18n.filters.financeDecisionHandler}</LabelText>
          <ReactSelect
            placeholder={i18n.filters.financeDecisionHandlerPlaceholder}
            options={options}
            value={selected}
            onChange={(option) =>
              option && 'value' in option ? select(option.value) : undefined
            }
          />
        </Label>
      </>
    )
  }
)

interface FeeDecisionStatusFilterProps {
  toggled: FeeDecisionStatus
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
    'ANNULLED'
  ]

  return (
    <>
      <Label>
        <LabelText>{i18n.filters.status}</LabelText>
      </Label>
      <FixedSpaceColumn spacing={'xs'}>
        {statuses.map((id) => (
          <Radio
            key={id}
            label={i18n.feeDecision.status[id]}
            checked={toggled === id}
            onChange={toggle(id)}
            dataQa={`fee-decision-status-filter-${id}`}
            small
          />
        ))}
      </FixedSpaceColumn>
    </>
  )
}

interface FeeDecisionDistinctionsFilterProps {
  toggled: DecisionDistinctiveDetails[]
  toggle: (distinctiveDetails: DecisionDistinctiveDetails) => () => void
}

export function FeeDecisionDistinctionsFilter({
  toggled,
  toggle
}: FeeDecisionDistinctionsFilterProps) {
  const { i18n } = useTranslation()

  const distinctiveDetails: DecisionDistinctiveDetails[] = [
    'UNCONFIRMED_HOURS',
    'EXTERNAL_CHILD',
    'RETROACTIVE'
  ]

  return (
    <>
      <Label>
        <LabelText>{i18n.filters.distinctiveDetails}</LabelText>
      </Label>
      <FixedSpaceColumn spacing={'xs'}>
        {distinctiveDetails.map((id) => (
          <Checkbox
            key={id}
            label={i18n.feeDecision.distinctiveDetails[id]}
            checked={toggled.includes(id)}
            onChange={toggle(id)}
            dataQa={`fee-decision-distinction-filter-${id}`}
          />
        ))}
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
      <Label>
        <LabelText>{i18n.filters.validityPeriod}</LabelText>
      </Label>

      <FlexRow>
        <DatePickerClearableDeprecated
          date={startDate}
          onChange={setStartDate}
          dataQa="fee-decisions-start-date"
          onCleared={() => setStartDate(undefined)}
        />
        <Gap horizontal size="xs" />
        <DatePickerClearableDeprecated
          date={endDate}
          onChange={setEndDate}
          dataQa="fee-decisions-start-date"
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
              color={colors.accents.orange}
            />
          </span>
        </>
      ) : null}

      <Gap size="s" />

      <Checkbox
        label={i18n.filters.searchByStartDate}
        checked={searchByStartDate}
        onChange={setSearchByStartDate}
        dataQa={`fee-decision-search-by-start-date`}
      />
    </>
  )
}

export const ValueDecisionStatusFilter = React.memo(
  function ValueDecisionStatusFilter({
    toggled,
    toggle
  }: {
    toggled: VoucherValueDecisionStatus
    toggle(v: VoucherValueDecisionStatus): () => void
  }) {
    const { i18n } = useTranslation()

    const statuses: VoucherValueDecisionStatus[] = [
      'DRAFT',
      'WAITING_FOR_SENDING',
      'WAITING_FOR_MANUAL_SENDING',
      'SENT',
      'ANNULLED'
    ]

    return (
      <>
        <Label>
          <LabelText>{i18n.filters.status}</LabelText>
        </Label>
        <FixedSpaceColumn spacing={'xs'}>
          {statuses.map((id) => (
            <Radio
              key={id}
              label={i18n.valueDecision.status[id]}
              checked={toggled === id}
              onChange={toggle(id)}
              dataQa={`value-decision-status-filter-${id}`}
              small
            />
          ))}
        </FixedSpaceColumn>
      </>
    )
  }
)

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
      <Label>
        <LabelText>{i18n.filters.status}</LabelText>
      </Label>
      <FixedSpaceColumn spacing={'xs'}>
        {statuses.map((id) => (
          <Radio
            key={id}
            label={i18n.invoice.status[id]}
            checked={toggled === id}
            onChange={toggle(id)}
            dataQa={`invoice-status-filter-${id}`}
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
      <Label>
        <LabelText>{i18n.filters.invoiceDate}</LabelText>
      </Label>

      <FlexRow>
        <DatePickerClearableDeprecated
          date={startDate}
          onChange={setStartDate}
          dataQa="invoices-start-date"
          onCleared={() => setStartDate(undefined)}
        />
        <Gap horizontal size="xs" />
        <DatePickerClearableDeprecated
          date={endDate}
          onChange={setEndDate}
          dataQa="invoices-end-date"
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
              color={colors.accents.orange}
            />
          </span>
        </>
      ) : null}

      <Gap size="s" />

      <Checkbox
        label={i18n.filters.invoiceSearchByStartDate}
        checked={searchByStartDate}
        onChange={setUseCustomDatesForInvoiceSending}
        dataQa={`invoice-search-by-start-date`}
      />
    </>
  )
}

interface InvoiceDistinctionsFilterProps {
  toggled: InvoiceDistinctiveDetails[]
  toggle: (distinctiveDetails: InvoiceDistinctiveDetails) => () => void
}

export function InvoiceDistinctionsFilter({
  toggled,
  toggle
}: InvoiceDistinctionsFilterProps) {
  const { i18n } = useTranslation()

  const distinctiveDetails: InvoiceDistinctiveDetails[] = ['MISSING_ADDRESS']

  return (
    <>
      <Label>
        <LabelText>{i18n.filters.distinctiveDetails}</LabelText>
      </Label>
      {distinctiveDetails.map((id) => (
        <Checkbox
          key={id}
          label={i18n.invoice.distinctiveDetails[id]}
          checked={toggled.includes(id)}
          onChange={toggle(id)}
          dataQa={`fee-decision-distinction-filter-${id}`}
        />
      ))}
    </>
  )
}

export type ApplicationTypeToggle = ApplicationType | 'ALL'

export const preschoolTypes = [
  'PRESCHOOL_ONLY',
  'PRESCHOOL_DAYCARE',
  'PREPARATORY_ONLY',
  'PREPARATORY_DAYCARE',
  'DAYCARE_ONLY'
] as const

export type PreschoolType = typeof preschoolTypes[number]

interface ApplicationTypeFilterProps {
  toggled: ApplicationTypeToggle
  toggledPreschool: PreschoolType[]
  toggle: (type: ApplicationTypeToggle) => () => void
  togglePreschool: (type: PreschoolType) => () => void
}

const CustomDiv = styled(FixedSpaceColumn)`
  margin-left: 18px;
  padding-left: 32px;
  border-left: 1px solid ${colors.greyscale.dark};
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

  const types: ApplicationTypeToggle[] = ['DAYCARE', 'PRESCHOOL', 'CLUB', 'ALL']

  return (
    <>
      <Label>
        <LabelText>{i18n.applications.list.type}</LabelText>
      </Label>
      <FixedSpaceColumn spacing={'xs'}>
        {types.map((id) => {
          return id != 'PRESCHOOL' ? (
            <Radio
              key={id}
              label={i18n.applications.types[id]}
              checked={toggled === id}
              onChange={toggle(id)}
              dataQa={`application-type-filter-${id}`}
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
                      size={'lg'}
                      color={colors.greyscale.dark}
                    />
                  </>
                }
                ariaLabel={i18n.applications.types[id]}
                checked={toggled === id}
                onChange={toggle(id)}
                dataQa={`application-type-filter-${id}`}
                small
              />
              {toggled === id && (
                <CustomDiv spacing={'xs'}>
                  {preschoolTypes.map((type) => (
                    <Checkbox
                      key={type}
                      label={i18n.applications.types[type]}
                      checked={toggledPreschool.includes(type)}
                      onChange={togglePreschool(type)}
                      dataQa={`application-type-filter-preschool-${type}`}
                    />
                  ))}
                </CustomDiv>
              )}
            </Fragment>
          )
        })}
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

export function ApplicationStatusFilter({
  toggled,
  toggledAllStatuses,
  toggle,
  toggleAllStatuses
}: ApplicationStatusFilterProps) {
  const { i18n } = useTranslation()

  const statuses: ApplicationSummaryStatusOptions[] = [
    'SENT',
    'WAITING_PLACEMENT',
    'WAITING_DECISION',
    'WAITING_UNIT_CONFIRMATION',
    'ALL'
  ]

  const allStatuses: ApplicationSummaryStatusAllOptions[] = [
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

  return (
    <>
      <Label>
        <LabelText>{i18n.application.state.title}</LabelText>
      </Label>
      <FixedSpaceColumn spacing={'xs'}>
        {statuses.map((id: ApplicationSummaryStatusOptions) => (
          <Radio
            key={id}
            label={
              <>
                {i18n.application.statuses[id]}
                {id === 'ALL' ? (
                  <ApplicationOpenIcon
                    icon={toggled === id ? faAngleUp : faAngleDown}
                    size={'lg'}
                    color={colors.greyscale.dark}
                  />
                ) : undefined}
              </>
            }
            ariaLabel={i18n.application.statuses[id]}
            checked={toggled === id}
            onChange={toggle(id)}
            dataQa={`application-status-filter-${id}`}
            small
          />
        ))}
      </FixedSpaceColumn>
      {toggled === 'ALL' && (
        <CustomDivWithMargin spacing={'xs'}>
          {allStatuses.map((id) => (
            <Checkbox
              key={id}
              label={i18n.application.statuses[id]}
              checked={toggledAllStatuses.includes(id)}
              onChange={toggleAllStatuses(id)}
              dataQa={`application-status-filter-all-${id}`}
            />
          ))}
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
      <Label>
        <LabelText>{i18n.common.date}</LabelText>
      </Label>

      <FixedSpaceColumn spacing={'xs'}>
        {dates.map((dateType) => (
          <Checkbox
            label={i18n.application.date[dateType]}
            checked={toggled.includes(dateType)}
            onChange={toggle(dateType)}
            key={dateType}
            dataQa={`applications-search-by-${dateType}`}
          />
        ))}
      </FixedSpaceColumn>
      <Gap size="s" />
      <FlexRow>
        <DatePickerClearableDeprecated
          date={startDate}
          onChange={setStartDate}
          dataQa="applications-start-date"
          onCleared={() => setStartDate(undefined)}
        />
        <span>-</span>
        <DatePickerClearableDeprecated
          date={endDate}
          onChange={setEndDate}
          dataQa="applications-end-date"
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
              color={colors.accents.orange}
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
      <Label>
        <LabelText>{i18n.applications.basis}</LabelText>
      </Label>
      <Gap horizontal size="xs" />
      <FixedSpaceRow spacing="xxs">
        <Tooltip
          tooltipId={'application-basis-ADDITIONAL_INFO'}
          tooltipText={i18n.applications.basisTooltip.ADDITIONAL_INFO}
          place={'top'}
          className={'application-basis-tooltip'}
          delayShow={750}
        >
          <RoundIcon
            content="L"
            color={colors.blues.dark}
            size="m"
            onClick={toggle('ADDITIONAL_INFO')}
            active={toggled.includes('ADDITIONAL_INFO')}
          />
        </Tooltip>
        <Tooltip
          tooltipId={'application-basis-SIBLING_BASIS'}
          tooltipText={i18n.applications.basisTooltip.SIBLING_BASIS}
          place={'top'}
          className={'application-basis-tooltip'}
          delayShow={750}
        >
          <RoundIcon
            content="S"
            color={colors.accents.green}
            size="m"
            onClick={toggle('SIBLING_BASIS')}
            active={toggled.includes('SIBLING_BASIS')}
          />
        </Tooltip>
        <Tooltip
          tooltipId={'application-basis-ASSISTANCE_NEED'}
          tooltipText={i18n.applications.basisTooltip.ASSISTANCE_NEED}
          place={'top'}
          className={'application-basis-tooltip'}
          delayShow={750}
        >
          <RoundIcon
            content="T"
            color={colors.accents.water}
            size="m"
            onClick={toggle('ASSISTANCE_NEED')}
            active={toggled.includes('ASSISTANCE_NEED')}
          />
        </Tooltip>
        <Tooltip
          tooltipId={'application-basis-CLUB_CARE'}
          tooltipText={i18n.applications.basisTooltip.CLUB_CARE}
          place={'top'}
          className={'application-basis-tooltip'}
          delayShow={750}
        >
          <RoundIcon
            content="K"
            color={colors.accents.red}
            size="m"
            onClick={toggle('CLUB_CARE')}
            active={toggled.includes('CLUB_CARE')}
          />
        </Tooltip>
        <Tooltip
          tooltipId={'application-basis-DAYCARE'}
          tooltipText={i18n.applications.basisTooltip.DAYCARE}
          place={'top'}
          className={'application-basis-tooltip'}
          delayShow={750}
        >
          <RoundIcon
            content="P"
            color={colors.accents.orange}
            size="m"
            onClick={toggle('DAYCARE')}
            active={toggled.includes('DAYCARE')}
          />
        </Tooltip>
        <Tooltip
          tooltipId={'application-basis-EXTENDED_CARE'}
          tooltipText={i18n.applications.basisTooltip.EXTENDED_CARE}
          place={'top'}
          className={'application-basis-tooltip'}
          delayShow={750}
        >
          <RoundIcon
            content="V"
            color={colors.accents.petrol}
            size="m"
            onClick={toggle('EXTENDED_CARE')}
            active={toggled.includes('EXTENDED_CARE')}
          />
        </Tooltip>
        <Tooltip
          tooltipId={'application-basis-DUPLICATE_APPLICATION'}
          tooltipText={i18n.applications.basisTooltip.DUPLICATE_APPLICATION}
          place={'top'}
          className={'application-basis-tooltip'}
          delayShow={750}
        >
          <RoundIcon
            content="2"
            color={colors.accents.emerald}
            size="m"
            onClick={toggle('DUPLICATE_APPLICATION')}
            active={toggled.includes('DUPLICATE_APPLICATION')}
          />
        </Tooltip>
      </FixedSpaceRow>
    </>
  )
}

interface MultiUnitsProps {
  units: { id: string; name: string }[]
  selectedUnits: string[]
  onChange: (v: string[]) => void
  dataQa?: string
}

export function MultiSelectUnitFilter({
  units,
  selectedUnits,
  onChange,
  dataQa
}: MultiUnitsProps) {
  const { i18n } = useTranslation()
  return (
    <div data-qa={dataQa}>
      <Label>
        <LabelText>{i18n.filters.unit}</LabelText>
      </Label>
      <ReactSelect
        isMulti
        placeholder={i18n.filters.unitPlaceholder}
        value={units.filter((unit) => selectedUnits.includes(unit.id))}
        options={units}
        onChange={(selected) => {
          selected && 'length' in selected
            ? onChange(selected.map(({ id }) => id))
            : onChange([])
        }}
        getOptionValue={(option) => option.id}
        getOptionLabel={(option) => option.name}
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
        <Tooltip
          tooltipId={'application-distinctions-SECONDARY'}
          tooltipText={i18n.applications.secondaryTooltip}
          place={'top'}
          className={'application-distinctions-SECONDARY-tooltip'}
          delayShow={250}
        >
          <Checkbox
            label={i18n.applications.distinctiveDetails['SECONDARY']}
            checked={toggled.includes('SECONDARY')}
            onChange={toggle('SECONDARY')}
            dataQa={'application-distiction-SECONDARY'}
            disabled
          />
        </Tooltip>
      ) : (
        <Checkbox
          label={i18n.applications.distinctiveDetails['SECONDARY']}
          checked={toggled.includes('SECONDARY')}
          onChange={toggle('SECONDARY')}
          dataQa={'application-distiction-SECONDARY'}
        />
      )}
    </Fragment>
  )
}

export type TransferApplicationFilter = 'TRANSFER_ONLY' | 'NO_TRANSFER' | 'ALL'

export function TransferApplicationsFilter({
  selected,
  setSelected
}: {
  selected: TransferApplicationFilter
  setSelected: React.Dispatch<React.SetStateAction<TransferApplicationFilter>>
}) {
  const { i18n } = useTranslation()
  return (
    <>
      <Label>
        <LabelText>{i18n.applications.list.transferFilter.title}</LabelText>
      </Label>
      <FixedSpaceColumn spacing={'xs'}>
        <Radio
          dataQa="filter-transfer-only"
          label={i18n.applications.list.transferFilter.transferOnly}
          checked={selected === 'TRANSFER_ONLY'}
          onChange={() => setSelected('TRANSFER_ONLY')}
          small
        />
        <Radio
          dataQa="filter-transfer-exclude"
          label={i18n.applications.list.transferFilter.hideTransfer}
          checked={selected === 'NO_TRANSFER'}
          onChange={() => setSelected('NO_TRANSFER')}
          small
        />
        <Radio
          dataQa="filter-transfer-all"
          label={i18n.applications.list.transferFilter.all}
          checked={selected === 'ALL'}
          onChange={() => setSelected('ALL')}
          small
        />
      </FixedSpaceColumn>
    </>
  )
}
