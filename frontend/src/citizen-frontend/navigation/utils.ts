// SPDX-FileCopyrightText: 2017-2022 City of Espoo
//
// SPDX-License-Identifier: LGPL-2.1-or-later

import sum from 'lodash/sum'
import type { KeyboardEvent } from 'react'
import { useCallback, useMemo } from 'react'

import type { ChildId } from 'lib-common/generated/api-types/shared'
import { useQuery } from 'lib-common/query'

import type { User } from '../auth/state'
import { useUser } from '../auth/state'
import { unreadChildDocumentsCountQuery } from '../child-documents/queries'
import { childrenQuery } from '../children/queries'
import { unreadPedagogicalDocumentsCountQuery } from '../children/sections/pedagogical-documents/queries'
import { applicationNotificationsQuery } from '../decisions/queries'

const empty = {}

export function useUnreadChildNotifications() {
  const loggedIn = useUser() !== undefined
  const { data: unreadPedagogicalDocumentsCount = empty } = useQuery(
    unreadPedagogicalDocumentsCountQuery(),
    { enabled: loggedIn }
  )
  const { data: unreadChildDocumentsCount = empty } = useQuery(
    unreadChildDocumentsCountQuery(),
    { enabled: loggedIn }
  )

  const unreadChildNotifications = useMemo(() => {
    const counts: Record<ChildId, number> = {}
    const addCounts = (countRecord: Record<ChildId, number>) =>
      Object.entries(countRecord).forEach(([id, count]) => {
        counts[id as ChildId] = (counts[id as ChildId] ?? 0) + count
      })

    addCounts(unreadPedagogicalDocumentsCount)
    addCounts(unreadChildDocumentsCount)

    return counts
  }, [unreadPedagogicalDocumentsCount, unreadChildDocumentsCount])

  const totalUnreadChildNotifications = useMemo(
    () => sum(Object.values(unreadChildNotifications)),
    [unreadChildNotifications]
  )

  return { unreadChildNotifications, totalUnreadChildNotifications }
}

export function useChildrenWithOwnPage() {
  const { data } = useQuery(childrenQuery())
  return useMemo(() => {
    if (!data) return []
    return data.filter((child) => child.upcomingPlacementType !== null)
  }, [data])
}

export function useUnreadDecisions() {
  const loggedIn = useUser() !== undefined
  const { data: decisionWaitingConfirmationCount = 0 } = useQuery(
    applicationNotificationsQuery(),
    { enabled: loggedIn }
  )

  return decisionWaitingConfirmationCount
}

export const isPersonalDetailsIncomplete = (user: User) => !user.email

export const useOnEscape = (action: () => void) => {
  return useCallback(
    (event: KeyboardEvent<HTMLElement>) => {
      if (event.key === 'Escape') {
        action()
      }
    },
    [action]
  )
}

export const useMenubarKeyboardNavigation = (
  menubarRef: React.RefObject<HTMLElement | null>
) => {
  return useCallback(
    (event: KeyboardEvent<HTMLElement>) => {
      if (!menubarRef.current) return

      const target = event.target as HTMLElement
      const isOnDropdownButton = target.getAttribute('aria-haspopup') === 'true'
      const isDropdownOpen = target.getAttribute('aria-expanded') === 'true'

      const topLevelMenuItems = Array.from(
        menubarRef.current.querySelectorAll<HTMLElement>('[role="menuitem"]')
      ).filter((item) => !item.closest('[role="menu"]'))

      if (topLevelMenuItems.length === 0) return

      const currentIndex = topLevelMenuItems.findIndex(
        (item) => item === target
      )
      if (currentIndex === -1) return

      const updateRovingTabindex = (nextIndex: number) => {
        topLevelMenuItems.forEach((item, index) => {
          item.tabIndex = index === nextIndex ? 0 : -1
        })
      }

      switch (event.key) {
        case 'ArrowDown':
          if (isOnDropdownButton && !isDropdownOpen) {
            event.preventDefault()
            target.click()
          }
          break

        case 'ArrowLeft':
        case 'ArrowRight': {
          const nextIndex =
            event.key === 'ArrowLeft'
              ? currentIndex > 0
                ? currentIndex - 1
                : topLevelMenuItems.length - 1
              : currentIndex < topLevelMenuItems.length - 1
                ? currentIndex + 1
                : 0

          event.preventDefault()
          event.stopPropagation()

          updateRovingTabindex(nextIndex)
          topLevelMenuItems[nextIndex]?.focus()
          break
        }

        case 'Home':
          event.preventDefault()
          event.stopPropagation()
          updateRovingTabindex(0)
          topLevelMenuItems[0]?.focus()
          break

        case 'End':
          event.preventDefault()
          event.stopPropagation()
          updateRovingTabindex(topLevelMenuItems.length - 1)
          topLevelMenuItems[topLevelMenuItems.length - 1]?.focus()
          break
      }
    },
    [menubarRef]
  )
}

export const useDropdownMenuKeyboardNavigation = (
  containerRef: React.RefObject<HTMLElement | null>,
  isOpen: boolean,
  onClose: () => void
) => {
  return useCallback(
    (event: KeyboardEvent<HTMLElement>) => {
      if (!containerRef.current || !isOpen) return

      const target = event.target as HTMLElement
      const menuItems = Array.from(
        containerRef.current.querySelectorAll<HTMLElement>(
          '[role="menuitem"], [role="menuitemradio"]'
        )
      ).filter((item) => {
        const dropdown = item.closest('[role="menu"]')
        return dropdown?.parentElement === containerRef.current
      })

      if (menuItems.length === 0) return

      const currentIndex = menuItems.findIndex((item) => item === target)

      const focusMenuItem = (index: number) => {
        menuItems[index]?.focus()
      }

      const focusTopLevelItem = (
        direction: 'current' | 'next' | 'previous'
      ) => {
        const menubar = containerRef.current?.closest('[role="menubar"]')
        if (!menubar) return

        const topLevelItems = Array.from(
          menubar.querySelectorAll<HTMLElement>('[role="menuitem"]')
        ).filter((item) => !item.closest('[role="menu"]'))

        const currentTopLevelItem = topLevelItems.find((item) =>
          containerRef.current?.contains(item)
        )

        if (currentTopLevelItem) {
          const currentIndex = topLevelItems.indexOf(currentTopLevelItem)
          const nextIndex =
            direction === 'current'
              ? currentIndex
              : direction === 'previous'
                ? Math.max(0, currentIndex - 1)
                : Math.min(topLevelItems.length - 1, currentIndex + 1)

          event.preventDefault()
          event.stopPropagation()

          topLevelItems.forEach((item, index) => {
            item.tabIndex = index === nextIndex ? 0 : -1
          })

          onClose()
          requestAnimationFrame(() => {
            topLevelItems[nextIndex]?.focus()
          })
        }
      }

      switch (event.key) {
        case 'Tab':
          onClose()
          break

        case 'Escape':
          focusTopLevelItem('current')
          break

        case 'ArrowLeft':
          focusTopLevelItem('previous')
          break

        case 'ArrowRight': {
          focusTopLevelItem('next')
          break
        }

        case 'ArrowDown':
          event.preventDefault()
          focusMenuItem(
            currentIndex < menuItems.length - 1 ? currentIndex + 1 : 0
          )
          break

        case 'ArrowUp':
          event.preventDefault()
          focusMenuItem(
            currentIndex > 0 ? currentIndex - 1 : menuItems.length - 1
          )
          break

        case 'Home':
          event.preventDefault()
          focusMenuItem(0)
          break

        case 'End':
          event.preventDefault()
          focusMenuItem(menuItems.length - 1)
          break
      }
    },
    [containerRef, isOpen, onClose]
  )
}
