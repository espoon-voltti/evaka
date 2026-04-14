// SPDX-FileCopyrightText: 2017-2024 City of Espoo
//
// SPDX-License-Identifier: LGPL-2.1-or-later

import axios from 'axios'

import { openaiApiKey } from '../../shared/config.ts'
import { toRequestHandler } from '../../shared/express.ts'

const SYSTEM_PROMPT = `Olet varhaiskasvatuksen viestinnän asiantuntija. Arvioi päiväkodin tiedoteviesti, jonka henkilökunta on lähettämässä huoltajille eVaka-järjestelmän kautta.

Huoltajat saavat paljon tiedotteita ja joutuvat avaamaan jokaisen erikseen. Huonosti kirjoitetut tiedotteet turhauttavat heitä. Arvioi viesti seuraavien kriteerien perusteella:

1. OTSIKKO: Onko otsikko informatiivinen ja konkreettinen? Huonoja esimerkkejä: "Tärkeää asiaa", "Pieni muistutus", "Huomio", "Hei!", "Tervehdys". Hyvä otsikko kertoo heti mistä on kyse, esim. "Metsäretki ti 15.4 – eväät ja säänmukaiset vaatteet mukaan".

2. YTIMEKKYYS: Meneekö viesti suoraan asiaan vai onko siinä turhaa täytettä? Pitkät johdannot vuodenaikojen vaihtumisesta, lasten puuhista tai yleisestä kuulumisten vaihdosta eivät kuulu tiedotteeseen. Tiedotteen pitää kertoa konkreettinen asia heti alussa.

3. TARPEELLISUUS: Onko viestissä konkreettista toimenpidettä tai tärkeää tietoa huoltajalle? Jos viestin ainoa konkreettinen asia on yksi lause lopussa, koko viesti pitäisi tiivistää siihen.

4. RAKENNE: Onko tärkein asia viestin alussa vai haudattuna loppuun?

Vastaa JSON-muodossa:
{
  "ok": true/false,
  "feedback": "Selitys suomeksi miksi viesti ei ole hyvä tiedote ja konkreettinen parannusehdotus. Tyhjä merkkijono jos ok=true."
}

Vastaa ok=true vain jos viesti on selkeä, ytimekäs tiedote jossa otsikko kertoo asian ja teksti menee suoraan asiaan. Ole kriittinen.`

export const bulletinAiReview = toRequestHandler(async (req, res) => {
  const apiKey = openaiApiKey
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

  const response = await axios.post(
    'https://api.openai.com/v1/chat/completions',
    {
      model: 'gpt-4o-mini',
      messages: [
        { role: 'system', content: SYSTEM_PROMPT },
        { role: 'user', content: userMessage }
      ],
      temperature: 0.3,
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

  const aiResponse = JSON.parse(
    response.data.choices[0].message.content
  ) as {
    ok: boolean
    feedback: string
  }

  res.json({
    ok: aiResponse.ok,
    feedback: aiResponse.feedback || ''
  })
})
