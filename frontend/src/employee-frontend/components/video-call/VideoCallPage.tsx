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
  employeeGetSignals,
  employeePostSignal,
  endEmployee,
  getRoomEmployee
} from '../../generated/api-clients/videocall'

type Phase = 'loading' | 'ringing' | 'live' | 'ended' | 'error'

export default React.memo(function EmployeeVideoCallPage() {
  const [, params] = useRoute<{ roomId: string }>('/video-call/:roomId')
  const [, setLocation] = useLocation()
  const roomId = params?.roomId
  const [phase, setPhase] = useState<Phase>('loading')
  const [room, setRoom] = useState<VideoCallRoomInfo | null>(null)
  const [error, setError] = useState<string | null>(null)
  const [sharingScreen, setSharingScreen] = useState(false)
  const [session, setSession] = useState<VideoCallSession | null>(null)
  const localVideoRef = useRef<HTMLVideoElement>(null)
  const remoteVideoRef = useRef<HTMLVideoElement>(null)
  const sessionRef = useRef<VideoCallSession | null>(null)
  const pollRef = useRef<number | null>(null)

  useEffect(() => {
    if (!roomId) return
    let cancelled = false
    const start = async () => {
      try {
        const info = await getRoomEmployee({ roomId })
        if (cancelled) return
        setRoom(info)
        setPhase(info.status === 'ENDED' ? 'ended' : 'ringing')

        const s = await startSession('EMPLOYEE', {
          postSignal: (body) => employeePostSignal({ roomId, body }),
          getSignals: (since) => employeeGetSignals({ roomId, since })
        })
        if (cancelled) {
          s.stop()
          return
        }
        sessionRef.current = s
        setSession(s)

        pollRef.current = window.setInterval(() => {
          void (async () => {
            try {
              const updated = await getRoomEmployee({ roomId })
              setRoom(updated)
              if (updated.status === 'ACTIVE') setPhase('live')
              if (updated.status === 'ENDED') {
                setPhase('ended')
                sessionRef.current?.stop()
                sessionRef.current = null
              }
            } catch {
              // ignore
            }
          })()
        }, 2000)
      } catch {
        if (!cancelled) {
          setError('Puhelua ei voi aloittaa')
          setPhase('error')
        }
      }
    }
    void start()
    return () => {
      cancelled = true
      if (pollRef.current !== null) {
        window.clearInterval(pollRef.current)
        pollRef.current = null
      }
      sessionRef.current?.stop()
      sessionRef.current = null
    }
  }, [roomId])

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
        await endEmployee({ roomId })
      } catch {
        // ignore
      }
    }
    setPhase('ended')
    setLocation('/')
  }

  const toggleScreenShare = async () => {
    const session = sessionRef.current
    if (!session) return
    if (sharingScreen) {
      await session.restoreCameraTrack()
      setSharingScreen(false)
      return
    }
    try {
      const display = await navigator.mediaDevices.getDisplayMedia({
        video: true
      })
      const screenTrack = display.getVideoTracks()[0]
      if (!screenTrack) return
      await session.replaceVideoTrack(screenTrack)
      setSharingScreen(true)
      screenTrack.addEventListener('ended', () => {
        void session.restoreCameraTrack()
        setSharingScreen(false)
      })
    } catch {
      // user cancelled picker
    }
  }

  const showCall = phase === 'ringing' || phase === 'live'
  return (
    <Wrapper>
      {phase === 'loading' && <StatusText>Ladataan…</StatusText>}
      {phase === 'ended' && <StatusText>Puhelu päättyi</StatusText>}
      {phase === 'error' && <StatusText>{error ?? 'Virhe'}</StatusText>}
      {showCall && (
        <VideoGrid>
          <RemoteVideo ref={remoteVideoRef} autoPlay playsInline />
          <LocalVideo ref={localVideoRef} autoPlay playsInline muted />
          {phase === 'ringing' && room && (
            <RingingOverlay>Soitetaan — {room.childName}</RingingOverlay>
          )}
          <Controls>
            {phase === 'live' && (
              <Button
                text={sharingScreen ? 'Lopeta jakaminen' : 'Jaa näyttö'}
                onClick={() => void toggleScreenShare()}
              />
            )}
            <Button
              text="Lopeta"
              appearance="inline"
              onClick={() => void hangup()}
            />
          </Controls>
        </VideoGrid>
      )}
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

const RingingOverlay = styled.div`
  position: absolute;
  top: 40%;
  left: 0;
  right: 0;
  text-align: center;
  font-size: 1.4rem;
`

const LocalVideo = styled.video`
  position: absolute;
  right: 1rem;
  bottom: 6rem;
  width: 160px;
  height: 220px;
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
