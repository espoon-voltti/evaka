// SPDX-FileCopyrightText: 2017-2024 City of Espoo
//
// SPDX-License-Identifier: LGPL-2.1-or-later

import axios from 'axios'

import { openaiApiKey } from '../../shared/config.ts'
import { toRequestHandler } from '../../shared/express.ts'
import type { RedisClient } from '../../shared/redis-client.ts'

const SYSTEM_PROMPT = `Olet varhaiskasvatuksen viestinnän tiukka laatutarkastaja. Arvioit päiväkodin tiedotetta, jonka henkilökunta on lähettämässä huoltajille eVaka-järjestelmän kautta.

Huoltajat saavat paljon tiedotteita päivässä. Heidän pitää pystyä näkemään yhdellä vilkaisulla, mitä viesti koskee ja mitä heiltä odotetaan. Oletusvastauksesi on ok=false. Hyväksy ok=true vain jos KAIKKI alla olevat kriteerit täyttyvät.

HYLKÄYSKRITEERIT – vastaa ok=false jos mikä tahansa seuraavista pätee:

1. OTSIKKO on yleinen eikä kerro asiaa. Hylkää otsikot kuten "Hei", "Tervehdys", "Tärkeää asiaa", "Pieni muistutus", "Huomio", "Kevätkuulumiset", "Ajankohtaista", "Tiedoksi".

2. VIESTIN ENSIMMÄINEN VIRKE ei kerro viestin pääasiaa. Jos ensimmäinen virke on tervehdys, säätilan kuvaus, vuodenajan kuvaus, yleinen kuulumisten vaihto tai kuvaus lasten arjesta, hylkää viesti.

3. VIESTI SISÄLTÄÄ TÄYTETEKSTIÄ ennen pääasiaa. Esimerkkejä täytetekstistä: kuvaukset vuodenajan vaihtumisesta, auringon paistamisesta, lumen sulamisesta, lasten ihastuksesta tai innostuksesta, pihan tilasta, askartelusta, lauluista, maalaamisesta, yleisistä keskusteluista. Jos tällaista on enemmän kuin yksi virke ennen pääasiaa, hylkää viesti.

4. PÄÄASIA ON HAUDATTU LOPPUUN. Jos konkreettinen toimenpide tai tärkeä tieto tulee vasta viimeisessä kappaleessa tai viimeisissä virkkeissä, hylkää viesti.

5. VIESTI KERTOO ASIOITA, JOTKA EIVÄT VAADI HUOLTAJALTA MITÄÄN. Jos viesti on pelkkää kuvausta lasten arjesta ilman konkreettista toimenpidettä tai tärkeää tietoa, hylkää viesti. Tällainen sisältö kuuluu muihin kanaviin, ei tiedotteeseen.

HYVÄKSYMISKRITEERIT – hyväksy ok=true vain jos kaikki pätevät:
- Otsikko kertoo heti mistä on kyse ja sisältää konkreettisen asian tai päivämäärän
- Ensimmäinen virke sisältää viestin pääasian (mitä, milloin, mitä huoltajan pitää tehdä)
- Viestissä ei ole täytetekstiä, tervehdyksiä tai yleistä kuvausta ennen pääasiaa
- Viestin pituus on oikeassa suhteessa sen sisältöön – yhden lauseen asiaa ei ole pakattu kolmeen kappaleeseen

Vastaa JSON-muodossa:
{
  "ok": true/false,
  "feedback": "Jos ok=false: selitä suomeksi lyhyesti (1–3 virkettä) mitkä hylkäyskriteerit täyttyvät JA anna konkreettinen parannusehdotus, esimerkiksi ehdotus tiiviistä korvaavasta tekstistä. Tyhjä merkkijono jos ok=true."
}

Ole tiukka. Jos epäröit hyväksymisen ja hylkäämisen välillä, hylkää.`

// Runtime-configured secret: the OpenAI key lives in Redis so staging deploys
// don't need a new SSM parameter or IAM grant. Set it once from an admin host
// with `redis-cli SET openai:api_key <key>`. Falls back to the OPENAI_API_KEY
// env var (loaded from `.env.openai` in local dev via ecosystem.config.js) so
// the local flow keeps working without Redis.
const OPENAI_KEY_REDIS_KEY = 'openai:api_key'
const KEY_CACHE_TTL_MS = 60_000

async function resolveOpenAiKey(
  redisClient: RedisClient,
  cache: { value: string | null; expiresAt: number } | null
): Promise<{
  key: string | null
  cache: { value: string | null; expiresAt: number }
}> {
  const now = Date.now()
  if (cache && cache.expiresAt > now) {
    return { key: cache.value, cache }
  }
  let key: string | null = null
  try {
    key = await redisClient.get(OPENAI_KEY_REDIS_KEY)
  } catch {
    // Redis unavailable — fall through to env fallback
  }
  if (!key) {
    key = openaiApiKey ?? null
  }
  return {
    key,
    cache: { value: key, expiresAt: now + KEY_CACHE_TTL_MS }
  }
}

export const bulletinAiReview = (redisClient: RedisClient) => {
  let cache: { value: string | null; expiresAt: number } | null = null
  return toRequestHandler(async (req, res) => {
    const resolved = await resolveOpenAiKey(redisClient, cache)
    cache = resolved.cache
    const apiKey = resolved.key

    if (!apiKey) {
      res.json({ ok: true, feedback: '' })
      return
    }

    const { title, content } = req.body as {
      title: string
      content: string
    }

    if (!title || !content) {
      res.status(400).json({ error: 'title and content are required' })
      return
    }

    const userMessage = `Otsikko: ${title}\n\nViesti:\n${content}`

    interface OpenAiChatCompletion {
      choices: { message: { content: string } }[]
    }

    const response = await axios.post<OpenAiChatCompletion>(
      'https://api.openai.com/v1/chat/completions',
      {
        model: 'gpt-4o-mini',
        messages: [
          { role: 'system', content: SYSTEM_PROMPT },
          { role: 'user', content: userMessage }
        ],
        temperature: 0,
        response_format: { type: 'json_object' }
      },
      {
        headers: {
          Authorization: `Bearer ${apiKey}`,
          'Content-Type': 'application/json'
        },
        timeout: 15000
      }
    )

    const aiResponse = JSON.parse(response.data.choices[0].message.content) as {
      ok: boolean
      feedback: string
    }

    res.json({
      ok: aiResponse.ok,
      feedback: aiResponse.feedback || ''
    })
  })
}
