// SPDX-FileCopyrightText: 2017-2026 City of Espoo
//
// SPDX-License-Identifier: LGPL-2.1-or-later

import React, { useContext, useEffect, useState } from 'react'
import styled from 'styled-components'
import { useLocation } from 'wouter'

import type { PendingVideoCall } from 'lib-common/generated/api-types/videocall'
import { Button } from 'lib-components/atoms/buttons/Button'

import { AuthContext } from '../auth/state'
import { listPending } from '../generated/api-clients/videocall'

const POLL_INTERVAL_MS = 4000

export default React.memo(function IncomingVideoCallBanner() {
  const { user } = useContext(AuthContext)
  const hasUser = user.map((u) => !!u).getOrElse(false)
  const [, setLocation] = useLocation()
  const [pending, setPending] = useState<PendingVideoCall | null>(null)
  const [dismissed, setDismissed] = useState<Set<string>>(new Set())

  useEffect(() => {
    if (!hasUser) {
      setPending(null)
      return
    }
    let cancelled = false
    const tick = async () => {
      try {
        const calls = await listPending()
        if (cancelled) return
        const next = calls.find((c) => !dismissed.has(c.roomId)) ?? null
        setPending(next)
      } catch {
        // swallow — keep polling
      }
    }
    void tick()
    const id = window.setInterval(() => void tick(), POLL_INTERVAL_MS)
    return () => {
      cancelled = true
      window.clearInterval(id)
    }
  }, [hasUser, dismissed])

  if (!pending) return null

  const answer = () => {
    setLocation(`/video-call/${pending.roomId}`)
  }
  const dismiss = () => {
    setDismissed((prev) => {
      const next = new Set(prev)
      next.add(pending.roomId)
      return next
    })
    setPending(null)
  }

  return (
    <Banner data-qa="incoming-video-call-banner">
      <Title>Videopuhelu: {pending.employeeName}</Title>
      <Info>Haluaa soittaa lapsestanne {pending.childName}</Info>
      <Actions>
        <Button text="Vastaa" primary onClick={answer} />
        <Button text="Hylkää" appearance="inline" onClick={dismiss} />
      </Actions>
    </Banner>
  )
})

const Banner = styled.div`
  position: fixed;
  top: 1rem;
  left: 50%;
  transform: translateX(-50%);
  background: #fff;
  color: #222;
  border: 1px solid #ddd;
  border-radius: 12px;
  box-shadow: 0 8px 24px rgba(0, 0, 0, 0.15);
  padding: 1rem 1.5rem;
  z-index: 9999;
  display: flex;
  flex-direction: column;
  gap: 0.5rem;
  min-width: 320px;
  max-width: 90vw;
`

const Title = styled.div`
  font-weight: 600;
  font-size: 1rem;
`

const Info = styled.div`
  font-size: 0.9rem;
  opacity: 0.8;
`

const Actions = styled.div`
  display: flex;
  gap: 0.5rem;
  margin-top: 0.25rem;
`
