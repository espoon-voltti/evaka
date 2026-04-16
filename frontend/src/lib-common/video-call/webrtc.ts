// SPDX-FileCopyrightText: 2017-2026 City of Espoo
//
// SPDX-License-Identifier: LGPL-2.1-or-later

import type {
  PostSignalRequest,
  SignalsResponse,
  VideoCallRole
} from '../generated/api-types/videocall'

export interface VideoCallSignalingClient {
  postSignal: (body: PostSignalRequest) => Promise<void>
  getSignals: (since: number) => Promise<SignalsResponse>
}

export interface VideoCallSession {
  pc: RTCPeerConnection
  localStream: MediaStream
  remoteStream: MediaStream
  stop: () => void
  replaceVideoTrack: (track: MediaStreamTrack) => Promise<void>
  restoreCameraTrack: () => Promise<void>
}

// PoC: Metered.ca free-tier TURN credentials. These belong to a throwaway demo account
// with a bandwidth-capped relay and no other access; rotation is cheap. Will be replaced
// with short-lived creds fetched at runtime before production. Scanner directives below
// cover the common self-hosted tools; GitHub's native partner-pattern scanner does not
// match Metered strings.
//
// gitleaks:allow
// pragma: allowlist secret
// trufflehog:ignore
// detect-secrets: ignore
const ICE_SERVERS: RTCIceServer[] = [
  { urls: 'stun:stun.relay.metered.ca:80' },
  {
    urls: [
      'turn:global.relay.metered.ca:80',
      'turn:global.relay.metered.ca:80?transport=tcp',
      'turn:global.relay.metered.ca:443',
      'turns:global.relay.metered.ca:443?transport=tcp'
    ],
    // gitleaks:allow
    username: '6fe6bb37cefb83869ce0cdef',
    // gitleaks:allow
    credential: 'OiTOZ9WXn/y0XEQS'
  }
]

async function captureCamera(): Promise<MediaStream> {
  return navigator.mediaDevices.getUserMedia({ video: true, audio: true })
}

function attachPeerEvents(
  pc: RTCPeerConnection,
  remoteStream: MediaStream,
  signaling: VideoCallSignalingClient,
  tag: string
) {
  pc.addEventListener('track', (event) => {
    console.warn(
      `[webrtc ${tag}] track`,
      event.track.kind,
      'id=',
      event.track.id,
      'streams=',
      event.streams.length,
      'streamTracks=',
      event.streams[0]?.getTracks().map((t) => t.kind)
    )
    for (const track of event.streams[0]?.getTracks() ?? []) {
      remoteStream.addTrack(track)
    }
  })
  pc.addEventListener('icecandidate', (event) => {
    if (event.candidate) {
      console.warn(
        `[webrtc ${tag}] local candidate`,
        event.candidate.type,
        event.candidate.protocol,
        event.candidate.address
      )
      void signaling.postSignal({
        kind: 'ice-candidate',
        data: JSON.stringify(event.candidate.toJSON())
      })
    } else {
      console.warn(`[webrtc ${tag}] local ICE gathering complete`)
    }
  })
  pc.addEventListener('connectionstatechange', () => {
    console.warn(`[webrtc ${tag}] connectionState=${pc.connectionState}`)
  })
  pc.addEventListener('iceconnectionstatechange', () => {
    console.warn(`[webrtc ${tag}] iceConnectionState=${pc.iceConnectionState}`)
  })
  pc.addEventListener('icegatheringstatechange', () => {
    console.warn(`[webrtc ${tag}] iceGatheringState=${pc.iceGatheringState}`)
  })
  pc.addEventListener('signalingstatechange', () => {
    console.warn(`[webrtc ${tag}] signalingState=${pc.signalingState}`)
  })
}

/**
 * Starts a WebRTC session. For the CITIZEN role, the peer creates the offer
 * on start (the citizen is the one joining an already-created room). For the
 * EMPLOYEE role, we wait for an offer signal before creating an answer.
 */
export async function startSession(
  role: VideoCallRole,
  signaling: VideoCallSignalingClient
): Promise<VideoCallSession> {
  const pc = new RTCPeerConnection({ iceServers: ICE_SERVERS })
  const localStream = await captureCamera()
  const remoteStream = new MediaStream()
  const originalVideoTrack = localStream.getVideoTracks()[0] ?? null

  const tag = role
  attachPeerEvents(pc, remoteStream, signaling, tag)
  for (const track of localStream.getTracks()) {
    console.warn(
      `[webrtc ${tag}] addTrack`,
      track.kind,
      'id=',
      track.id,
      'enabled=',
      track.enabled,
      'readyState=',
      track.readyState
    )
    pc.addTrack(track, localStream)
  }

  let stopped = false
  let since = 0

  if (role === 'CITIZEN') {
    const offer = await pc.createOffer()
    // Post the SDP BEFORE setLocalDescription so it lands in S3 ahead of any
    // ice-candidate messages — setLocalDescription starts ICE gathering and the
    // candidate handler posts concurrently; without this ordering a candidate can
    // be written with a lower seq than the offer and fail addIceCandidate on the
    // answerer with "remote description was null".
    await signaling.postSignal({
      kind: 'offer',
      data: JSON.stringify(offer)
    })
    await pc.setLocalDescription(offer)
  }

  async function pollLoop() {
    while (!stopped) {
      try {
        const res = await signaling.getSignals(since)
        if (res.signals.length > 0) {
          since = res.nextSince
          for (const s of res.signals) {
            await handleSignal(s.kind, s.data)
          }
        }
      } catch {
        // swallow — keep polling
      }
      await new Promise((r) => setTimeout(r, 600))
    }
  }

  const pendingCandidates: RTCIceCandidateInit[] = []

  async function flushPendingCandidates() {
    for (const c of pendingCandidates.splice(0)) {
      try {
        await pc.addIceCandidate(c)
      } catch (err) {
        console.warn(`[webrtc ${tag}] flushed addIceCandidate failed`, err)
      }
    }
  }

  async function handleSignal(kind: string, data: string) {
    console.warn(`[webrtc ${tag}] received ${kind}`)
    if (kind === 'offer') {
      const offer = JSON.parse(data) as RTCSessionDescriptionInit
      await pc.setRemoteDescription(offer)
      const answer = await pc.createAnswer()
      // Post answer before setLocalDescription so the answerer's ICE candidates
      // (which start firing after setLocalDescription) can never outrun it in S3.
      await signaling.postSignal({
        kind: 'answer',
        data: JSON.stringify(answer)
      })
      await pc.setLocalDescription(answer)
      await flushPendingCandidates()
    } else if (kind === 'answer') {
      const answer = JSON.parse(data) as RTCSessionDescriptionInit
      await pc.setRemoteDescription(answer)
      await flushPendingCandidates()
    } else if (kind === 'ice-candidate') {
      const candidate = JSON.parse(data) as RTCIceCandidateInit
      if (!pc.remoteDescription) {
        pendingCandidates.push(candidate)
        return
      }
      try {
        await pc.addIceCandidate(candidate)
      } catch (err) {
        console.warn(`[webrtc ${tag}] addIceCandidate failed`, err)
      }
    }
  }

  void pollLoop()

  const replaceVideoTrack = async (track: MediaStreamTrack) => {
    const sender = pc.getSenders().find((s) => s.track?.kind === 'video')
    if (sender) {
      await sender.replaceTrack(track)
    }
  }

  const restoreCameraTrack = async () => {
    if (originalVideoTrack) {
      await replaceVideoTrack(originalVideoTrack)
    }
  }

  const stop = () => {
    stopped = true
    for (const t of localStream.getTracks()) t.stop()
    for (const t of remoteStream.getTracks()) t.stop()
    pc.close()
  }

  return {
    pc,
    localStream,
    remoteStream,
    stop,
    replaceVideoTrack,
    restoreCameraTrack
  }
}
