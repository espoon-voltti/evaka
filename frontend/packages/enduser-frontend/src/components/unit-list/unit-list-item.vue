<!--
SPDX-FileCopyrightText: 2017-2020 City of Espoo

SPDX-License-Identifier: LGPL-2.1-or-later
-->

<template>
  <div v-if="unit" class="unit">
    <span class="selection-number">
      <span class="selection-number-content">
        {{ order }}
      </span>
    </span>
    <div class="unit-container">
      <div class="unit-title-container">
        <span class="unit-title">
          {{ unit.name }}
        </span>
        <span
          class="remove"
          role="button"
          v-if="removable"
          @click.prevent="removeElement($event)"
          @keyup.enter.prevent="removeElement($event)"
          tabindex="0"
          :aria-label="$t('general.remove-from-apply-list') + ` ${name}`"
        >
          {{ $t('general.remove-from-apply-list') }}
          <font-awesome-icon :icon="['fal', 'times']"></font-awesome-icon>
        </span>
      </div>
      <div class="details">{{ unit.description }}</div>
      <div class="address">{{ unit.streetAddress }}, {{ espooLocalized }}</div>
      <div class="chips">
        <c-chip-text
          :teal="providerIsMunicipal"
          :green="providerIsPurchased"
          :blue="providerIsPrivate"
          >{{ provider }}</c-chip-text
        >
        <c-chip-text
          :raisedOrange="isSwedishUnit"
          :raisedBlue="!isSwedishUnit"
          >{{ language }}</c-chip-text
        >
      </div>
    </div>
  </div>
</template>

<script>
  import { mapGetters } from 'vuex'
  import { LANGUAGES } from '@/constants'
  import {
    providerIsMunicipal,
    providerIsPrivate,
    providerIsPurchased
  } from '@/utils/helpers'

  export default {
    props: {
      unitId: {
        type: String,
        required: true
      },
      order: {
        type: Number,
        required: true
      },
      removable: {
        type: Boolean,
        default: true
      }
    },
    data() {
      return {
        description: '',
        schedule: ''
      }
    },
    computed: {
      ...mapGetters(['applicationUnits']),
      espooLocalized() {
        if (this.unit && this.unit.language) {
          return this.$t(
            `form.preschool-application.preferredUnits.units.espoo-${this.unit.language.toLowerCase()}`
          )
        } else {
          return this.$t(
            'form.preschool-application.preferredUnits.units.espoo-fi'
          )
        }
      },
      language() {
        if (
          this.unit.language &&
          this.unit.language.toLowerCase() === LANGUAGES.SV
        ) {
          return this.$t(
            'form.preschool-application.preferredUnits.units.daycare-lang-sv'
          )
        } else {
          return this.$t(
            'form.preschool-application.preferredUnits.units.daycare-lang-fi'
          )
        }
      },
      unit() {
        return this.applicationUnits.data.find(({ id }) => id === this.unitId)
      },
      name() {
        return this.unit ? this.unit.name : ''
      },
      address() {
        return this.unit ? this.unit.address : ''
      },
      provider() {
        if (this.unit && this.unit.provider_type) {
          return this.$t(
            `constants.provider-type.${this.unit.provider_type.toLowerCase()}`
          )
        } else {
          return this.$t('constants.provider-type.municipal')
        }
      },
      isSwedishUnit() {
        return (
          this.unit.language &&
          this.unit.language.toLowerCase() === LANGUAGES.SV
        )
      },
      providerIsMunicipal() {
        return providerIsMunicipal(this.unit.provider_type)
      },
      providerIsPrivate() {
        return providerIsPrivate(this.unit.provider_type)
      },
      providerIsPurchased() {
        return providerIsPurchased(this.unit.provider_type)
      }
    },
    methods: {
      removeElement(mouseEvent) {
        const isPrimaryButton = mouseEvent.button === 0
        if (isPrimaryButton) {
          this.$emit('remove', this.unitId)
        }
      }
    }
  }
</script>

<style lang="scss" scoped>
  .unit {
    position: relative;
    padding: 0.75rem 0.75rem 0.75rem 46px;
    background-color: white;
    display: flex;
    flex-direction: column;

    .selection-number {
      position: absolute;
      left: 8px;
      top: 1em;
      display: inline-block;
      width: 1.75rem;
      height: 1.75rem;
      line-height: 1.75rem;
      vertical-align: top;
      text-align: center;
      background-color: tint($blue, 20%);
      border-radius: 50%;
      color: white;
      font-weight: bold;
      font-family: sans-serif;
    }
  }

  .groups {
    flex: 0 1 100%;
    margin-top: 0.3rem;

    @include onMobile() {
      padding: 0.5rem 0.5rem 0 0.5rem;
    }

    .group {
      display: flex;
      &-name {
        flex-basis: 40%;
        font-weight: 600;
      }
      &-schedule {
        margin-left: 1.5rem;
      }
    }
  }

  .unit-container {
    .unit-title-container {
      align-items: center;
      display: flex;
      justify-content: space-between;

      .unit-title {
        color: #6e6e6e;
        font-size: 1.25rem;
        font-weight: 500;
      }
      .remove {
        color: $grey;
        cursor: pointer;
        font-size: 14px;
        padding: 0.5rem 0 0.5rem 0.5rem;
        white-space: nowrap;
      }
    }
    .details {
      color: $grey-darker;
      font-size: 15px;
    }
    .address {
      color: #6e6e6e;
      font-size: 15px;
    }
    .chips :nth-child(2) {
      margin-left: 0.5rem;
    }
  }
</style>
