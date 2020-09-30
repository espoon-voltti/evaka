<!--
SPDX-FileCopyrightText: 2017-2020 City of Espoo

SPDX-License-Identifier: LGPL-2.1-or-later
-->

<template>
  <div class="unit-details">
    <div class="unit-title-wrapper">
      <h5 class="title is-5">
        {{ unit.name }}
      </h5>
      <h6 class="subtitle is-6">{{ unit.address }}, Espoo</h6>
    </div>

    <div class="www-provider-details">
      <a
        v-if="!!unit.url"
        :href="unit.url"
        class="www-link strong"
        target="_blank"
        rel="noopener"
      >
        www
        <font-awesome-icon :icon="['fal', 'link']"></font-awesome-icon>
      </a>
      <span v-else class="www-link"></span>

      <c-chip-text v-if="isNotClubType">{{ provider }}</c-chip-text>
      <c-chip-text v-if="isNotClubType">{{ language }}</c-chip-text>
    </div>
  </div>
</template>

<script>
  import { mapGetters } from 'vuex'
  import { UNIT_TYPE } from '@/constants'

  export default {
    props: {
      unit: {
        type: Object,
        required: true
      }
    },
    methods: {
      routeTo(unit) {
        this.$emit('routeTo', unit)
      }
    },
    computed: {
      ...mapGetters(['selectedLocation', 'activeTerm']),
      services() {
        return this.unit.daycareTypes
      },
      isNotClubType() {
        return !this.unit.type.includes(UNIT_TYPE.CLUB)
      },
      provider() {
        return this.unit.provider_type
          ? this.$t(
              `constants.provider-type.${this.unit.provider_type.toLowerCase()}`,
              { returnObjects: true }
            )
          : this.$t('constants.provider-type.municipal')
      },
      language() {
        return this.unit.language
          ? this.$t(
              `constants.daycare-language.${this.unit.language.toLowerCase()}`,
              { returnObjects: true }
            )
          : this.$t('constants.daycare-language.fi')
      },
      activeGroups() {
        return this.unit.groups.filter(
          (group) =>
            this.activeTerm && this.activeTerm.groups.includes(group.id)
        )
      }
    }
  }
</script>

<style lang="scss" scoped>
  .vue-map {
    .unit-details {
      position: relative;
      small {
        font-size: 12px;
      }
    }
  }

  .provider-type {
    display: inline-block;
    font-size: 0.675rem;
    padding: 0 0.25rem;
    border-radius: 6px;
    border: 1px solid #999;
    margin-bottom: 0.25rem;
  }

  .unit-title-wrapper {
    position: relative;
    display: flex;
    flex-direction: column;
    flex: 1 0 auto;
    padding: 0 42px 1rem 0;

    .subtitle {
      margin-bottom: 0;
    }

    .title-wrapper {
      flex: 1 0 auto;
    }
  }

  .service-type {
    display: inline-block;
    height: 0.75rem;
    width: 1.5rem;
    border-radius: 0.5rem;
    cursor: help;

    &:not(:last-child) {
      margin-right: 0.125rem;
    }

    &.centre {
      background: $color-daycare-centre;
    }

    &.family {
      background: $color-daycare-family;
    }

    &.group {
      background: $color-daycare-group-family;
    }

    &.club {
      background: $color-daycare-club;
    }
  }

  .directions-to-btn {
    color: #3273dc;
    display: inline-block;
    font-weight: 600;
    transition: all ease-out 0.2s;
    font-size: 0.875rem;

    i {
      font-size: 1.125rem;
      vertical-align: middle;
    }

    &:hover {
      opacity: 0.75;
    }
  }

  .link {
    a:before {
      content: '\f0c1';
      display: inline-block;
      font-family: FontAwesome;
      vertical-align: middle;
    }
  }

  .www-provider-details {
    display: flex;
    justify-content: space-between;
    align-items: center;
    margin-bottom: 0.75rem;

    .www-link {
      color: #3273dc;
      min-width: 60px;
    }
  }

  .club-groups-wrapper {
    margin-bottom: 0.75rem;
  }

  .club-group {
    font-size: 0.875rem;
    display: flex;
    padding: 5px 0;

    .description {
      flex-basis: 45%;
    }

    .schedule {
      flex-basis: 55%;
    }
  }

  @keyframes grow {
    0% {
      transform: scale(1);
    }
    50% {
      transform: scale(1.25);
    }
    100% {
      transform: scale(1);
    }
  }
</style>

<style lang="scss">
  .unit-details {
    .popper {
      border-radius: 0.5rem;
      box-shadow: none;
      border: none;
      font-size: 0.875rem;
      background-color: #3273dc;
      color: white;
    }
  }
</style>
