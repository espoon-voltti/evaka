// SPDX-FileCopyrightText: 2017-2026 City of Espoo
//
// SPDX-License-Identifier: LGPL-2.1-or-later

import { useTranslation } from '../../state/i18n'

/**
 * Translations for the MCP pages. They live here instead of lib-customizations because the
 * feature is only available in non-production environments and needs no municipality-specific
 * customization.
 */
const fi = {
  title: 'Tekoälytyökalut (MCP)',
  description:
    'Tekoälyavustajat (esim. Claude tai GitHub Copilot) voivat luoda tähän testiympäristöön testidataa MCP-palvelimen kautta. Tällä sivulla näet, mitkä työkalut on valtuutettu toimimaan puolestasi, ja voit poistaa MCP:n kautta luodun testidatan.',
  serverUrl: 'MCP-palvelimen osoite',
  serverUrlInfo:
    'Lisää tämä osoite tekoälytyökaluun HTTP-tyyppisenä MCP-palvelimena (esim. Claude Code: claude mcp add --transport http evaka <osoite>). Työkalu ohjaa sinut kirjautumaan eVakaan ja hyväksymään valtuutuksen.',
  authorizations: {
    title: 'Valtuutetut tekoälytyökalut',
    empty: 'Et ole valtuuttanut yhtään tekoälytyökalua.',
    client: 'Työkalu',
    createdAt: 'Valtuutettu',
    expiresAt: 'Voimassa',
    lastUsedAt: 'Käytetty viimeksi',
    status: 'Tila',
    statusActive: 'Voimassa',
    statusExpired: 'Vanhentunut',
    statusRevoked: 'Poistettu',
    statusPending: 'Odottaa',
    never: 'Ei koskaan',
    revoke: 'Poista valtuutus',
    revokeConfirmTitle: 'Poistetaanko valtuutus?',
    revokeConfirmText:
      'Työkalu ei voi enää käyttää eVakaa puolestasi. Voit valtuuttaa sen uudelleen myöhemmin.'
  },
  testData: {
    title: 'MCP:n kautta luotu testidata',
    description:
      'Jokainen tekoälytyökalun luoma testidatakokonaisuus on oma joukkonsa. Joukon poistaminen poistaa kaikki siihen luodut tiedot sekä niihin liittyvät tiedot (esim. sijoitukset, hakemukset ja viestit).',
    empty: 'MCP:n kautta ei ole luotu testidataa.',
    name: 'Nimi',
    createdBy: 'Luonut',
    client: 'Työkalu',
    createdAt: 'Luotu',
    contents: 'Sisältö',
    total: 'Yhteensä',
    details: 'Näytä sisältö',
    hideDetails: 'Piilota sisältö',
    delete: 'Poista joukko',
    deleteConfirmTitle: 'Poistetaanko testidatajoukko?',
    deleteConfirmText:
      'Kaikki joukkoon kuuluvat tiedot ja niistä riippuvat tiedot poistetaan pysyvästi.',
    deleteConfirmCheckbox: 'Ymmärrän, että poisto on pysyvä',
    deletePreviewTotal: (rows: number, tracked: number) =>
      `Poistetaan yhteensä ${rows} tietokantariviä, joista ${tracked} on joukkoon merkittyjä tietoja.`,
    deletePreviewUntracked: (rows: number) =>
      `Poisto ulottuu myös ${rows} riviin, joita ei ole luotu MCP:n kautta, esimerkiksi käyttöliittymässä testidatan päälle tehtyihin tietoihin:`,
    deletePreviewConflict:
      'Joukkoa ei voi poistaa, koska sen tietoihin viittaa toinen testidatajoukko. Poista ensin se.',
    deletePreviewFailed: 'Poiston esikatselu epäonnistui.',
    entityTypes: {
      care_area: 'Palvelualueet',
      daycare: 'Yksiköt',
      daycare_group: 'Ryhmät',
      person: 'Henkilöt',
      employee: 'Työntekijät',
      placement: 'Sijoitukset',
      application: 'Hakemukset'
    } as Record<string, string>
  },
  authorize: {
    title: 'Valtuuta tekoälytyökalu',
    loginRequired: 'Kirjaudu sisään eVakaan hyväksyäksesi valtuutuksen.',
    login: 'Kirjaudu sisään',
    adminRequired:
      'Vain pääkäyttäjät voivat valtuuttaa tekoälytyökaluja käyttämään eVakaa.',
    invalidRequest: 'Valtuutuspyyntö on virheellinen.',
    redirectUriNotRegistered:
      'Paluuosoitetta ei ole rekisteröity tälle työkalulle, joten valtuutuspyyntöä ei voi käsitellä.',
    unknownClient:
      'Tekoälytyökalun rekisteröinti ei ole enää voimassa. Poista tunnistautuminen tekoälytyökalusta ja yhdistä uudelleen.',
    clientRequests: 'pyytää lupaa toimia eVakassa puolestasi.',
    permissions: 'Työkalu voi puolestasi:',
    permissionList: [
      'lukea tämän testiympäristön yksiköitä, palvelualueita ja palveluntarpeita',
      'luoda testidataa: yksiköitä, perheitä, työntekijöitä, sijoituksia, hakemuksia ja muita eVakan perustietoja',
      'listata ja poistaa MCP:n kautta luotua testidataa'
    ],
    redirectUri: 'Paluuosoite',
    validity: 'Valtuutus on voimassa',
    validityDays: (days: number) => (days === 1 ? '1 päivä' : `${days} päivää`),
    approve: 'Hyväksy',
    deny: 'Hylkää',
    approved: 'Valtuutus hyväksytty. Voit palata tekoälytyökaluun.',
    environmentWarning:
      'Muista, että tekoälytyökalu toimii sinun käyttöoikeuksillasi.'
  }
}

const sv: typeof fi = {
  title: 'AI-verktyg (MCP)',
  description:
    'AI-assistenter (t.ex. Claude eller GitHub Copilot) kan skapa testdata i den här testmiljön via MCP-servern. På den här sidan ser du vilka verktyg som har behörighet att agera för din räkning, och du kan ta bort testdata som skapats via MCP.',
  serverUrl: 'MCP-serverns adress',
  serverUrlInfo:
    'Lägg till den här adressen i AI-verktyget som en MCP-server av typen HTTP (t.ex. Claude Code: claude mcp add --transport http evaka <adress>). Verktyget leder dig till att logga in i eVaka och godkänna behörigheten.',
  authorizations: {
    title: 'Behöriga AI-verktyg',
    empty: 'Du har inte gett behörighet åt något AI-verktyg.',
    client: 'Verktyg',
    createdAt: 'Behörighet given',
    expiresAt: 'Giltig till',
    lastUsedAt: 'Senast använd',
    status: 'Status',
    statusActive: 'Giltig',
    statusExpired: 'Utgången',
    statusRevoked: 'Återkallad',
    statusPending: 'Väntar',
    never: 'Aldrig',
    revoke: 'Återkalla behörighet',
    revokeConfirmTitle: 'Återkalla behörigheten?',
    revokeConfirmText:
      'Verktyget kan inte längre använda eVaka för din räkning. Du kan ge behörigheten på nytt senare.'
  },
  testData: {
    title: 'Testdata skapad via MCP',
    description:
      'Varje testdatahelhet som ett AI-verktyg skapat är en egen grupp. När gruppen tas bort raderas all data som skapats i den samt data som är beroende av den (t.ex. placeringar, ansökningar och meddelanden).',
    empty: 'Ingen testdata har skapats via MCP.',
    name: 'Namn',
    createdBy: 'Skapad av',
    client: 'Verktyg',
    createdAt: 'Skapad',
    contents: 'Innehåll',
    total: 'Totalt',
    details: 'Visa innehåll',
    hideDetails: 'Dölj innehåll',
    delete: 'Ta bort gruppen',
    deleteConfirmTitle: 'Ta bort testdatagruppen?',
    deleteConfirmText:
      'All data i gruppen och all data som är beroende av den raderas permanent.',
    deleteConfirmCheckbox: 'Jag förstår att raderingen är permanent',
    deletePreviewTotal: (rows: number, tracked: number) =>
      `Sammanlagt ${rows} databasrader raderas, varav ${tracked} är data som hör till gruppen.`,
    deletePreviewUntracked: (rows: number) =>
      `Raderingen omfattar också ${rows} rader som inte skapats via MCP, till exempel data som lagts till ovanpå testdatan i användargränssnittet:`,
    deletePreviewConflict:
      'Gruppen kan inte tas bort eftersom en annan testdatagrupp hänvisar till dess data. Ta bort den gruppen först.',
    deletePreviewFailed: 'Förhandsgranskningen av raderingen misslyckades.',
    entityTypes: {
      care_area: 'Serviceområden',
      daycare: 'Enheter',
      daycare_group: 'Grupper',
      person: 'Personer',
      employee: 'Anställda',
      placement: 'Placeringar',
      application: 'Ansökningar'
    }
  },
  authorize: {
    title: 'Ge AI-verktyg behörighet',
    loginRequired: 'Logga in i eVaka för att godkänna behörigheten.',
    login: 'Logga in',
    adminRequired:
      'Endast administratörer kan ge AI-verktyg behörighet att använda eVaka.',
    invalidRequest: 'Behörighetsbegäran är felaktig.',
    redirectUriNotRegistered:
      'Returadressen är inte registrerad för det här verktyget, så behörighetsbegäran kan inte behandlas.',
    unknownClient:
      'AI-verktygets registrering är inte längre giltig. Ta bort autentiseringen i AI-verktyget och anslut på nytt.',
    clientRequests: 'begär tillstånd att agera i eVaka för din räkning.',
    permissions: 'Verktyget kan för din räkning:',
    permissionList: [
      'läsa testmiljöns enheter, serviceområden och servicebehov',
      'skapa testdata: enheter, familjer, anställda, placeringar, ansökningar och andra grunduppgifter i eVaka',
      'lista och ta bort testdata som skapats via MCP'
    ],
    redirectUri: 'Returadress',
    validity: 'Behörigheten är giltig',
    validityDays: (days: number) => (days === 1 ? '1 dag' : `${days} dagar`),
    approve: 'Godkänn',
    deny: 'Avvisa',
    approved: 'Behörigheten godkänd. Du kan återgå till AI-verktyget.',
    environmentWarning:
      'Kom ihåg att AI-verktyget agerar med dina användarrättigheter.'
  }
}

export type McpTranslations = typeof fi

export function useMcpTranslation(): McpTranslations {
  const { lang } = useTranslation()
  return lang === 'sv' ? sv : fi
}
