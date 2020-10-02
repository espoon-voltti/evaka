// SPDX-FileCopyrightText: 2017-2020 City of Espoo
//
// SPDX-License-Identifier: LGPL-2.1-or-later

import React, { useCallback, Fragment } from 'react'
import styled from 'styled-components'
import { faSearch, faTimes, faTrash } from '@evaka/icons'
import LocalDate from '@evaka/lib-common/src/local-date'
import { Button, Icon, Radio } from '~components/shared/alpha'
import { DatePickerClearable } from '~components/common/DatePicker'
import Select from '../common/Select'
import { useTranslation } from '~state/i18n'
import {
  DecisionDistinctiveDetails,
  FeeDecisionStatus,
  InvoiceStatus,
  InvoiceDistinctiveDetails
} from '~types/invoicing'
import './Filters.scss'
import { ApplicationType, ApplicationSummaryStatus } from '~types/application'
import {
  FixedSpaceColumn,
  FixedSpaceRow
} from 'components/shared/layout/flex-helpers'
import Checkbox from '~components/shared/atoms/form/Checkbox'
import Radio1 from '~components/shared/atoms/form/Radio'
import { Gap, DefaultMargins } from '~components/shared/layout/white-space'
import { FlexRow } from './styled/containers'
import RoundIcon from '~components/shared/atoms/RoundIcon'
import IconButton from '~components/shared/atoms/buttons/IconButton'
import Colors from '~components/shared/Colors'
import { faAngleDown, faAngleUp } from '@evaka/icons'
import { FontAwesomeIcon } from '@fortawesome/react-fontawesome'
import MultiSelect, { SelectOptionProp } from './MultiSelect'
import Tooltip from '~components/common/Tooltip'
import { CareArea } from '~types/unit'

const LabelText = styled.span`
  font-weight: 600;
  font-size: 16px;
`

const Label = styled.label`
  margin-bottom: 8px;
`

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
  }
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
    <div className="filters-container">
      <FreeTextSearch
        value={freeText}
        setValue={setFreeText}
        placeholder={searchPlaceholder}
      />
      <div className="filters">
        <div className="column c1">{column1}</div>
        <div className="column c2">{column2}</div>
        <div className="column c3">{column3}</div>
      </div>
      <ClearOptions clearMargin={clearMargin}>
        <Button plain icon={faTrash} onClick={clearFilters}>
          {i18n.filters.clear}
        </Button>
      </ClearOptions>
    </div>
  )
}

type FreeTextSearchProps = {
  value: string
  setValue: (s: string) => void
  placeholder: string
}

const SearchInputContainer = styled.div`
  height: 50px;
  display: flex;
  justify-content: center;
  align-items: center;
`

const SearchInput = styled.input`
  width: 100%;
  border: none;
  font-size: 1rem;
  background: ${Colors.greyscale.lightest};
  width: 100%;
  display: flex;
  flex-direction: row;
  flex-wrap: nowrap;
  padding: 0.75rem;
  padding-left: 55px;
  font-size: 17px;
  background: ${Colors.greyscale.lightest};
  outline: none;
  margin-left: -38px;
  margin-right: -25px;
  color: ${Colors.greyscale.darkest};

  &::placeholder {
    font-style: italic;
    font-size: 17px;
    color: ${Colors.greyscale.dark};
  }

  &:focus {
    border-width: 2px;
    border-radius: 2px;
    border-style: solid;
    border-color: ${Colors.accents.petrol};
    margin-top: -2px;
    padding-left: 53px;
    margin-bottom: -2px;
  }
`

const CustomIcon = styled(Icon)`
  color: ${Colors.greyscale.dark};
  margin: 0 0.5rem;
  position: relative;
  left: 10px;
  font-size: 22px;
`

const CustomIconButton = styled(IconButton)`
  float: right;
  position: relative;
  color: ${Colors.greyscale.medium};
  right: 20px;
`

function FreeTextSearch({ value, setValue, placeholder }: FreeTextSearchProps) {
  const clear = useCallback(() => setValue(''), [setValue])

  return (
    <SearchInputContainer>
      <CustomIcon icon={faSearch} />
      <SearchInput
        placeholder={placeholder}
        value={value}
        onChange={(e) => setValue(e.target.value)}
        data-qa="free-text-search-input"
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
  selected: string
  select: (unit: string) => void
}

export function UnitFilter({ units, selected, select }: UnitFilterProps) {
  const { i18n } = useTranslation()

  return (
    <>
      <Label>
        <LabelText>{i18n.filters.unit}</LabelText>
        <Select
          placeholder={i18n.filters.unitPlaceholder}
          options={units}
          value={selected}
          onChange={(e) => select(e.target.value)}
        />
      </Label>
    </>
  )
}

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
      {statuses.map((id) => (
        <Radio
          key={id}
          id={id}
          label={i18n.feeDecision.status[id]}
          value={id}
          model={toggled}
          onChange={toggle(id)}
          dataQa={`fee-decision-status-filter-${id}`}
        />
      ))}
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
        <DatePickerClearable
          date={startDate}
          onChange={setStartDate}
          dataQa="fee-decisions-start-date"
          onCleared={() => setStartDate(undefined)}
        />
        <Gap horizontal size="xs" />
        <DatePickerClearable
          date={endDate}
          onChange={setEndDate}
          dataQa="fee-decisions-start-date"
          onCleared={() => setEndDate(undefined)}
        />
      </FlexRow>

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
      {statuses.map((id) => (
        <Radio
          key={id}
          id={id}
          label={i18n.invoice.status[id]}
          value={id}
          model={toggled}
          onChange={toggle(id)}
          dataQa={`invoice-status-filter-${id}`}
        />
      ))}
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
        <DatePickerClearable
          date={startDate}
          onChange={setStartDate}
          dataQa="invoices-start-date"
          onCleared={() => setStartDate(undefined)}
        />
        <Gap horizontal size="xs" />
        <DatePickerClearable
          date={endDate}
          onChange={setEndDate}
          dataQa="invoices-end-date"
          onCleared={() => setEndDate(undefined)}
        />
      </FlexRow>

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
  border-left: 1px solid ${Colors.greyscale.dark};
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
            <Radio1
              key={id}
              label={i18n.applications.types[id]}
              checked={toggled === id}
              onChange={toggle(id)}
              dataQa={`application-type-filter-${id}`}
              small
            />
          ) : (
            <Fragment key={id}>
              <Radio1
                key={id}
                label={i18n.applications.types[id]}
                labelIcon={
                  <ApplicationOpenIcon
                    icon={toggled === id ? faAngleUp : faAngleDown}
                    size={'lg'}
                    color={Colors.greyscale.dark}
                  />
                }
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
  margin-top: ${DefaultMargins.xs};
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
          <Radio1
            key={id}
            label={i18n.application.statuses[id]}
            labelIcon={
              id === 'ALL' ? (
                <ApplicationOpenIcon
                  icon={toggled === id ? faAngleUp : faAngleDown}
                  size={'lg'}
                  color={Colors.greyscale.dark}
                />
              ) : undefined
            }
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
        <DatePickerClearable
          date={startDate}
          onChange={setStartDate}
          dataQa="applications-start-date"
          onCleared={() => setStartDate(undefined)}
        />
        <span>-</span>
        <DatePickerClearable
          date={endDate}
          onChange={setEndDate}
          dataQa="applications-end-date"
          onCleared={() => setEndDate(undefined)}
        />
      </FlexRow>
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
            color={Colors.blues.dark}
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
            color={Colors.accents.green}
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
            color={Colors.accents.water}
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
            color={Colors.accents.red}
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
            color={Colors.accents.orange}
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
            color={Colors.accents.petrol}
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
            color={Colors.accents.emerald}
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
  units: { id: string; label: string }[]
  onSelect: (joku: SelectOptionProp[]) => void
  onRemove: (joku: SelectOptionProp[]) => void
}

export function MultiSelectUnitFilter({
  units,
  onSelect,
  onRemove
}: MultiUnitsProps) {
  const { i18n } = useTranslation()
  return (
    <Fragment>
      <Label>
        <LabelText>{i18n.filters.unit}</LabelText>
      </Label>
      <MultiSelect
        placeholder={i18n.filters.unitPlaceholder}
        options={units}
        onSelect={onSelect}
        onRemove={onRemove}
        data-qa="application-units-filter"
      />
    </Fragment>
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
      <Label>
        <LabelText>{i18n.applications.distinctions}</LabelText>
      </Label>
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
        <Radio1
          dataQa="filter-transfer-only"
          label={i18n.applications.list.transferFilter.transferOnly}
          checked={selected === 'TRANSFER_ONLY'}
          onChange={() => setSelected('TRANSFER_ONLY')}
          small
        />
        <Radio1
          dataQa="filter-transfer-exclude"
          label={i18n.applications.list.transferFilter.hideTransfer}
          checked={selected === 'NO_TRANSFER'}
          onChange={() => setSelected('NO_TRANSFER')}
          small
        />
        <Radio1
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
