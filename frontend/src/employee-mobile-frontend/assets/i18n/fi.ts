// SPDX-FileCopyrightText: 2017-2021 City of Espoo
//
// SPDX-License-Identifier: LGPL-2.1-or-later

export const fi = {
  common: {
    loadingFailed: 'Tietojen haku epäonnistui',
    cancel: 'Peruuta',
    confirm: 'Vahvista',
    all: 'Kaikki',
    statuses: {
      active: 'Aktiivinen',
      coming: 'Tulossa',
      completed: 'Päättynyt',
      conflict: 'Konflikti'
    },
    types: {
      CLUB: 'Kerho',
      FAMILY: 'Perhepäivähoito',
      GROUP_FAMILY: 'Ryhmäperhepäivähoito',
      CENTRE: 'Päiväkoti',
      PRESCHOOL: 'Esiopetus',
      DAYCARE: 'Varhaiskasvatus',
      PRESCHOOL_DAYCARE: 'Liittyvä varhaiskasvatus',
      PREPARATORY_EDUCATION: 'Valmistava esiopetus',
      PREPARATORY_DAYCARE: 'Liittyvä varhaiskasvatus',
      DAYCARE_5YO_FREE: '5v maksuton varhaiskasvatus',
      DAYCARE_5YO_PAID: 'Varhaiskasvatus (maksullinen)'
    },
    placement: {
      CLUB: 'Kerho',
      DAYCARE: 'Varhaiskasvatus',
      DAYCARE_PART_TIME: 'Osa-aikainen varhaiskasvatus',
      PRESCHOOL: 'Esiopetus',
      PRESCHOOL_DAYCARE: 'Liittyvä varhaiskasvatus',
      PREPARATORY: 'Valmistava',
      PREPARATORY_DAYCARE: 'Valmistava',
      TEMPORARY_DAYCARE: 'Väliaikainen',
      TEMPORARY_DAYCARE_PART_DAY: 'Väliaikainen osa-aikainen'
    },
    code: 'Koodi',
    children: 'Lapset',
    staff: 'Henkilökunta',
    messages: 'Viestit',
    back: 'Takaisin',
    hours: 'Tuntia',
    remove: 'Poista',
    clear: 'Tyhjennä'
  },
  absences: {
    title: 'Poissaolomerkinnät',
    absenceTypes: {
      OTHER_ABSENCE: 'Muu poissaolo',
      SICKLEAVE: 'Sairaus',
      UNKNOWN_ABSENCE: 'Ilmoittamaton poissaolo',
      PLANNED_ABSENCE: 'Suunniteltu poissaolo / vuorohoito',
      TEMPORARY_RELOCATION: 'Lapsi varasijoitettuna muualla',
      TEMPORARY_VISITOR: 'Varalapsi läsnä',
      PARENTLEAVE: 'Isyysvapaa',
      FORCE_MAJEURE: 'Maksuton päivä',
      PRESENCE: 'Ei poissaoloa'
    },
    careTypes: {
      PRESCHOOL: 'Esiopetus',
      PRESCHOOL_DAYCARE: 'Liittyvä varhaiskasvatus',
      DAYCARE_5YO_FREE: '5-vuotiaiden varhaiskasvatus',
      DAYCARE: 'Varhaiskasvatus',
      CLUB: 'Kerho'
    },
    absence: 'Poissaolo'
  },
  attendances: {
    types: {
      COMING: 'Tulossa',
      PRESENT: 'Läsnä',
      DEPARTED: 'Lähtenyt',
      ABSENT: 'Poissa'
    },
    status: {
      COMING: 'Tulossa',
      PRESENT: 'Saapunut',
      DEPARTED: 'Lähtenyt',
      ABSENT: 'Poissa'
    },
    groupSelectError: 'Valitun ryhmän nimeä ei löytynyt',
    actions: {
      markAbsent: 'Merkitse poissaolevaksi',
      markPresent: 'Merkitse saapuneeksi',
      markDeparted: 'Merkitse lähteneeksi',
      returnToComing: 'Palauta tulossa oleviin',
      returnToPresent: 'Palauta läsnäoleviin'
    },
    timeLabel: 'Merkintä',
    departureTime: 'Lähtöaika',
    arrivalTime: 'Saapumisaika',
    chooseGroup: 'Valitse ryhmä',
    chooseGroupInfo: 'Lapsia: Läsnä nyt/Ryhmässä yhteensä',
    searchPlaceholder: 'Etsi lapsen nimellä',
    noAbsences: 'Ei poissaoloja',
    missingFrom: 'Poissa seuraavasta toimintamuodosta',
    missingFromPlural: 'Poissa seuraavista toimintamuodoista',
    timeError: 'Virheellinen aika',
    serviceTime: {
      serviceToday: (start: string, end: string) =>
        `Varhaiskasvatusaika tänään ${start}-${end}`,
      noServiceToday: 'Ei varattua varhaiskasvatusaikaa tänään'
    },
    notes: {
      dailyNotes: 'Päivän muistiinpanot',
      labels: {
        note: 'Päivän tapahtumia (ei terveystietoja)',
        feedingNote: 'Lapsi söi tänään',
        sleepingNote: 'Lapsi nukkui tänään',
        reminderNote: 'Muistettavia asioita',
        groupNotesHeader: 'Koko ryhmää koskeva muistiinpano'
      },
      values: {
        GOOD: 'Hyvin',
        MEDIUM: 'Vähän',
        NONE: 'Ei ollenkaan'
      },
      reminders: {
        DIAPERS: 'Lisää vaippoja',
        CLOTHES: 'Lisää varavaatteita',
        LAUNDRY: 'Repussa pyykkiä'
      },
      placeholders: {
        note: 'Mitä tänään opin, leikin, oivalsin.',
        reminderNote: 'Muuta muistutettavaa esim. Aurinkovoide.',
        sleepingTime: 'Esim 1,5'
      },
      noNotes: 'Ei merkintöjä tälle päivälle',
      clearTitle: 'Haluatko tyhjentää merkinnät tältä päivältä?'
    },
    absenceTitle: 'Poissaolomerkintä',
    pin: {
      header: 'Lukituksen avaaminen',
      info: 'Anna PIN-koodi avataksesi lapsen tiedot',
      selectStaff: 'Valitse käyttäjä',
      staff: 'Käyttäjä',
      noOptions: 'Ei vaihtoehtoja',
      pinCode: 'PIN-koodi'
    },
    childInfo: {
      header: 'Lapsen tiedot',
      personalInfoHeader: 'Lapsen henkilötiedot',
      childName: 'Lapsen nimi',
      preferredName: 'Kutsumanimi',
      ssn: 'Hetu',
      address: 'Lapsen kotiosoite',
      type: 'Sijoitusmuoto',
      allergiesHeader: 'Allergiat, ruokavalio, lääkitys',
      allergies: 'Allergiat',
      diet: 'Ruokavalio',
      medication: 'Lääkitys',
      contactInfoHeader: 'Yhteystiedot',
      contact: 'Yhteyshenkilö',
      name: 'Nimi',
      phone: 'Puhelinnumero',
      backupPhone: 'Varapuhelinnumero',
      email: 'Sähköpostiosoite',
      backupPickup: 'Varahakija',
      backupPickupName: 'Varahakijan nimi'
    }
  },
  mobile: {
    landerText1:
      'Tervetuloa käyttämään Espoon varhaiskasvatuksen mobiilisovellusta!',
    landerText2:
      'Ottaaksesi sovelluksen käyttöön valitse alta ‘Lisää laite’ ja rekisteröi mobiililaite eVakassa oman yksikkösi sivulla.',
    actions: {
      ADD_DEVICE: 'Lisää laite',
      START: 'Aloitetaan'
    },
    wizard: {
      text1:
        'Mene eVakassa yksikkösi sivulle ja syötä sieltä saatava 10-merkkinen koodi kenttään alla.',
      text2: 'Syötä alla oleva vahvistuskoodi yksikkösi sivulle eVakaan.',
      title1: 'eVaka-mobiilin käyttöönotto, vaihe 1/3',
      title2: 'eVaka-mobiilin käyttöönotto, vaihe 2/3',
      title3: 'Tervetuloa käyttämään eVaka-mobiilia!',
      text3: 'eVaka-mobiili on nyt käytössä tässä laitteessa.',
      text4:
        'Turvataksesi lasten tiedot muistathan asettaa laitteeseen pääsykoodin, jos et ole sitä vielä tehnyt.'
    },
    emptyList: {
      no: 'Ei',
      status: {
        COMING: 'tulossa olevia',
        ABSENT: 'poissaolevia',
        PRESENT: 'läsnäolevia',
        DEPARTED: 'lähteneitä'
      },
      children: 'lapsia'
    }
  }
}
