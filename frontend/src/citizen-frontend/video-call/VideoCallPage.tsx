// SPDX-FileCopyrightText: 2017-2026 City of Espoo
//
// SPDX-License-Identifier: LGPL-2.1-or-later

import React, { useEffect, useRef, useState } from 'react'
import styled from 'styled-components'
import { useLocation, useRoute } from 'wouter'

import type { VideoCallRoomInfo } from 'lib-common/generated/api-types/videocall'
import { startSession } from 'lib-common/video-call/webrtc'
import type { VideoCallSession } from 'lib-common/video-call/webrtc'
import { Button } from 'lib-components/atoms/buttons/Button'

import {
  acceptCall,
  citizenGetSignals,
  citizenPostSignal,
  endCitizen,
  getRoomCitizen
} from '../generated/api-clients/videocall'

type Phase = 'loading' | 'ready' | 'live' | 'ended' | 'error'

export default React.memo(function CitizenVideoCallPage() {
  const [, params] = useRoute<{ roomId: string }>('/video-call/:roomId')
  const [, setLocation] = useLocation()
  const roomId = params?.roomId
  const [phase, setPhase] = useState<Phase>('loading')
  const [room, setRoom] = useState<VideoCallRoomInfo | null>(null)
  const [error, setError] = useState<string | null>(null)
  const [session, setSession] = useState<VideoCallSession | null>(null)
  const localVideoRef = useRef<HTMLVideoElement>(null)
  const remoteVideoRef = useRef<HTMLVideoElement>(null)
  const sessionRef = useRef<VideoCallSession | null>(null)

  useEffect(() => {
    if (!roomId) return
    let cancelled = false
    void (async () => {
      try {
        const info = await getRoomCitizen({ roomId })
        if (cancelled) return
        setRoom(info)
        setPhase(info.status === 'ENDED' ? 'ended' : 'ready')
      } catch {
        if (!cancelled) {
          setError('Puhelua ei löytynyt')
          setPhase('error')
        }
      }
    })()
    return () => {
      cancelled = true
    }
  }, [roomId])

  useEffect(() => {
    return () => {
      sessionRef.current?.stop()
    }
  }, [])

  const join = async () => {
    if (!roomId) return
    try {
      await acceptCall({ roomId })
    } catch {
      setError('Puheluun ei voi liittyä')
      setPhase('error')
      return
    }
    try {
      const s = await startSession('CITIZEN', {
        postSignal: (body) => citizenPostSignal({ roomId, body }),
        getSignals: (since) => citizenGetSignals({ roomId, since })
      })
      sessionRef.current = s
      setSession(s)
      setPhase('live')
    } catch {
      setError('Kameraa tai mikrofonia ei voi käyttää')
      setPhase('error')
    }
  }

  useEffect(() => {
    if (!session) return
    if (localVideoRef.current) {
      localVideoRef.current.srcObject = session.localStream
      void localVideoRef.current.play().catch(() => undefined)
    }
    const attachRemote = (stream: MediaStream) => {
      const el = remoteVideoRef.current
      if (!el) return
      if (el.srcObject !== stream) el.srcObject = stream
      void el.play().catch(() => undefined)
    }
    const onTrack = (e: RTCTrackEvent) => {
      if (e.streams[0]) attachRemote(e.streams[0])
    }
    session.pc.addEventListener('track', onTrack)
    return () => {
      session.pc.removeEventListener('track', onTrack)
    }
  }, [session])

  const hangup = async () => {
    sessionRef.current?.stop()
    sessionRef.current = null
    if (roomId) {
      try {
        await endCitizen({ roomId })
      } catch {
        // ignore
      }
    }
    setPhase('ended')
    setLocation('/')
  }

  return (
    <Wrapper>
      {phase === 'loading' && <StatusText>Ladataan…</StatusText>}
      {phase === 'ready' && room && (
        <Centered>
          <H1>Videopuhelu</H1>
          <Info>
            {room.employeeName} soittaa — aihe: {room.childName}
          </Info>
          <Button text="Vastaa" primary onClick={() => void join()} />
        </Centered>
      )}
      {phase === 'live' && (
        <VideoGrid>
          <RemoteVideo ref={remoteVideoRef} autoPlay playsInline />
          <LocalVideo ref={localVideoRef} autoPlay playsInline muted />
          <Controls>
            <Button
              text="Lopeta"
              appearance="inline"
              onClick={() => void hangup()}
            />
          </Controls>
        </VideoGrid>
      )}
      {phase === 'ended' && <StatusText>Puhelu päättyi</StatusText>}
      {phase === 'error' && <StatusText>{error ?? 'Virhe'}</StatusText>}
    </Wrapper>
  )
})

const Wrapper = styled.div`
  min-height: 100vh;
  background: #000;
  color: #fff;
  display: flex;
  align-items: center;
  justify-content: center;
`

const Centered = styled.div`
  display: flex;
  flex-direction: column;
  gap: 1.5rem;
  align-items: center;
  padding: 2rem;
`

const H1 = styled.h1`
  margin: 0;
  font-size: 1.5rem;
`

const Info = styled.div`
  font-size: 1rem;
  opacity: 0.85;
`

const StatusText = styled.div`
  font-size: 1.2rem;
`

const VideoGrid = styled.div`
  position: relative;
  width: 100%;
  height: 100vh;
`

const RemoteVideo = styled.video`
  width: 100%;
  height: 100%;
  object-fit: cover;
  background: #111;
`

const LocalVideo = styled.video`
  position: absolute;
  right: 1rem;
  bottom: 6rem;
  width: 140px;
  height: 200px;
  object-fit: cover;
  border-radius: 8px;
  border: 2px solid white;
  background: #222;
`

const Controls = styled.div`
  position: absolute;
  bottom: 1rem;
  left: 0;
  right: 0;
  display: flex;
  justify-content: center;
  gap: 1rem;
`
