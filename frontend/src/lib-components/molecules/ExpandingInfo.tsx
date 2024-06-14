// SPDX-FileCopyrightText: 2017-2022 City of Espoo
//
// SPDX-License-Identifier: LGPL-2.1-or-later

import { FontAwesomeIcon } from '@fortawesome/react-fontawesome'
import React, {
  ReactNode,
  useCallback,
  useContext,
  useEffect,
  useState
} from 'react'
import styled, { useTheme } from 'styled-components'

import Container, { ContentArea } from 'lib-components/layout/Container'
import { FixedSpaceRow } from 'lib-components/layout/flex-helpers'
import { fasInfo, faTimes } from 'lib-icons'

import RoundIcon from '../atoms/RoundIcon'
import { IconOnlyButton } from '../atoms/buttons/IconOnlyButton'
import { desktopMin } from '../breakpoints'
import { useTranslations } from '../i18n'
import { defaultMargins, SpacingSize } from '../white-space'

const InfoBoxContainer = styled(Container)<{
  width?: 'fixed' | 'full' | 'auto'
}>`
  ${({ width }) =>
    width === 'auto'
      ? 'width: auto'
      : width === 'full'
        ? 'width: 100%'
        : undefined};

  @keyframes open {
    from {
      max-height: 0;
    }
    to {
      max-height: 100px;
    }
  }

  background-color: ${(p) => p.theme.colors.main.m4};
  overflow: hidden;
  ${({ width }) =>
    width === 'full'
      ? `margin: ${defaultMargins.s} 0px;`
      : `margin: ${defaultMargins.s} -${defaultMargins.s} ${defaultMargins.xs};`};

  @media (min-width: ${desktopMin}) {
    animation-name: open;
    animation-duration: 0.2s;
    animation-timing-function: ease-out;
    ${({ width }) =>
      width === 'full'
        ? `margin: ${defaultMargins.s} 0px;`
        : `margin: ${defaultMargins.s} -${defaultMargins.L} ${defaultMargins.xs};`}
  }
`

const InfoBoxContentArea = styled(ContentArea)`
  display: flex;
`

const InfoContainer = styled.div`
  flex-grow: 1;
  color: ${(p) => p.theme.colors.grayscale.g100};
  padding: 0 ${defaultMargins.s};
  white-space: pre-wrap;
`

const RoundIconButton = styled.button<{ margin: SpacingSize }>`
  display: inline-flex;
  justify-content: center;
  align-items: center;
  padding: 0;
  margin: 0;
  margin-top: ${({ margin }) => defaultMargins[margin]};
  cursor: pointer;
  background-color: ${(p) => p.theme.colors.main.m2};
  color: ${(p) => p.theme.colors.grayscale.g0};
  border-radius: 100%;
  border: none;
  width: 20px;
  height: 20px;
  min-width: 20px;
  min-height: 20px;
  max-width: 20px;
  max-height: 20px;
  font-size: 12px;
  line-height: normal;

  @media (hover: hover) {
    &:hover {
      background-color: ${(p) => p.theme.colors.main.m2Hover};
    }
  }

  &:focus {
    box-shadow:
      0 0 0 2px ${(p) => p.theme.colors.grayscale.g0},
      0 0 0 4px ${(p) => p.theme.colors.main.m2Focus};
  }
`

type ExpandingInfoProps = {
  children: React.ReactNode
  info: ReactNode
  width?: 'fixed' | 'full' | 'auto'
  margin?: SpacingSize
  'data-qa'?: string
  inlineChildren?: boolean
}

const ExpandingInfoToggleContext = React.createContext<
  | {
      margin?: SpacingSize
      dataQa?: string
      expanded: boolean
      toggleExpanded: () => void
      hasSlot: (has: boolean) => void
    }
  | undefined
>(undefined)

const ExpandingInfoGroupContext = React.createContext<{
  onOpen: () => void
  addExpandingInfo: (close: () => void) => () => void
}>({
  onOpen: () => undefined,
  addExpandingInfo: () => () => undefined
})

export default React.memo(function ExpandingInfo({
  children,
  info,
  width = 'fixed',
  margin,
  'data-qa': dataQa,
  inlineChildren
}: ExpandingInfoProps) {
  const i18n = useTranslations()
  const group = useContext(ExpandingInfoGroupContext)

  const [expanded, setExpanded] = useState<boolean>(false)
  const toggleExpanded = useCallback(() => {
    if (!expanded) {
      group.onOpen()
    }
    setExpanded(!expanded)
    // eslint-disable-next-line react-hooks/exhaustive-deps
  }, [expanded, group.onOpen])
  const close = useCallback(() => setExpanded(false), [])

  useEffect(
    () => group.addExpandingInfo(close),
    // eslint-disable-next-line react-hooks/exhaustive-deps
    [group.addExpandingInfo, close]
  )

  const [hasSlot, setHasSlot] = useState(false)

  if (!info) {
    return <div>{children}</div>
  }

  const content = hasSlot ? (
    children
  ) : inlineChildren ? (
    <div>
      {children}
      <InlineInfoButton
        onClick={toggleExpanded}
        aria-label={i18n.common.openExpandingInfo}
        margin={margin ?? 'zero'}
        data-qa={dataQa}
        open={expanded}
      />
    </div>
  ) : (
    <FixedSpaceRow spacing="xs" alignItems="baseline">
      <div>{children}</div>
      <InfoButton
        onClick={toggleExpanded}
        aria-label={i18n.common.openExpandingInfo}
        margin={margin ?? 'zero'}
        data-qa={dataQa}
        open={expanded}
      />
    </FixedSpaceRow>
  )

  return (
    <ExpandingInfoToggleContext.Provider
      value={{
        toggleExpanded,
        margin,
        dataQa,
        hasSlot: setHasSlot,
        expanded
      }}
    >
      <span aria-live="polite">
        {content}
        {expanded && (
          <ExpandingInfoBox
            info={info}
            width={width}
            close={close}
            data-qa={dataQa}
          />
        )}
      </span>
    </ExpandingInfoToggleContext.Provider>
  )
})

export const ExpandingInfoButtonSlot = React.memo(
  function ExpendingInfoButtonSlot() {
    const i18n = useTranslations()
    const info = useContext(ExpandingInfoToggleContext)

    useEffect(() => {
      info?.hasSlot(true)

      return () => {
        info?.hasSlot(false)
      }
    }, [info])

    if (!info) {
      return null
    }

    return (
      <InlineInfoButton
        onClick={(ev) => {
          ev.stopPropagation()
          info.toggleExpanded()
        }}
        aria-label={i18n.common.openExpandingInfo}
        margin={info.margin ?? 'zero'}
        data-qa={info.dataQa}
        open={info.expanded}
      />
    )
  }
)

export const InfoButton = React.memo(function InfoButton({
  onClick,
  'aria-label': ariaLabel,
  margin,
  className,
  'data-qa': dataQa,
  open
}: {
  onClick: React.MouseEventHandler<HTMLButtonElement>
  'aria-label': string
  margin?: SpacingSize
  className?: string
  'data-qa'?: string
  open?: boolean
}) {
  const { colors } = useTheme()

  return (
    <RoundIconButton
      className={className}
      data-qa={dataQa}
      margin={margin ?? 'zero'}
      color={colors.status.info}
      onClick={onClick}
      type="button"
      role="button"
      aria-label={ariaLabel}
      aria-expanded={open}
    >
      <FontAwesomeIcon icon={fasInfo} />
    </RoundIconButton>
  )
})

export const InlineInfoButton = styled(InfoButton)`
  margin-left: ${defaultMargins.xs};
`

export const ExpandingInfoBox = React.memo(function ExpandingInfoBox({
  info,
  close,
  width = 'fixed',
  className,
  'data-qa': dataQa
}: {
  info: ReactNode
  close: () => void
  width?: 'fixed' | 'full' | 'auto'
  className?: string
  'data-qa'?: string
}) {
  const i18n = useTranslations()
  const { colors } = useTheme()

  return (
    <InfoBoxContainer className={className} width={width}>
      <InfoBoxContentArea opaque={false}>
        <RoundIcon content={fasInfo} color={colors.status.info} size="s" />

        <InfoContainer data-qa={dataQa ? `${dataQa}-text` : undefined}>
          {info}
        </InfoContainer>

        <IconOnlyButton
          onClick={close}
          icon={faTimes}
          aria-label={i18n.common.close}
          color="gray"
        />
      </InfoBoxContentArea>
    </InfoBoxContainer>
  )
})

interface ExpandingInfoGroupProps {
  children: React.ReactNode
}

export const ExpandingInfoGroup = React.memo(function ExpandingInfoGroup({
  children
}: ExpandingInfoGroupProps) {
  const [closingCallbacks, setClosingCallbacks] = useState<(() => void)[]>([])

  return (
    <ExpandingInfoGroupContext.Provider
      value={{
        addExpandingInfo: useCallback((close) => {
          setClosingCallbacks((cbs) => [...cbs, close])
          return () =>
            setClosingCallbacks((cbs) => cbs.filter((cb) => cb !== close))
        }, []),
        onOpen: useCallback(() => {
          closingCallbacks.forEach((cb) => cb())
        }, [closingCallbacks])
      }}
    >
      {children}
    </ExpandingInfoGroupContext.Provider>
  )
})
