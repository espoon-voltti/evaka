<!--
SPDX-FileCopyrightText: 2017-2020 City of Espoo

SPDX-License-Identifier: LGPL-2.1-or-later
-->

<template>
  <div class="search-group">
    <span>{{ $t('map-filters.care-type') }}:</span>
    <multiselect
      v-model="modelValue"
      class="club-type-select"
      :options="clubOptions"
      label="label"
      track-by="value"
      :multiple="false"
      placeholder=""
      selectLabel=""
      deselectLabel=""
      :searchable="false"
      :allowEmpty="false"
      selectedLabel=""
      open-direction="bottom"
    >
      <template slot="singleLabel" slot-scope="props">
        <span class="select-label">
          <span class="label-blip" :class="props.option.value" />
          <span class="lable-text">
            {{ props.option.label }}
          </span>
        </span>
      </template>

      <template slot="option" slot-scope="props">
        <span class="select-label">
          <span class="label-blip" :class="props.option.value" />
          <span class="lable-text">
            {{ props.option.label }}
          </span>
        </span>
      </template>
    </multiselect>
  </div>
</template>

<script>
  import { mapGetters } from 'vuex'
  import Multiselect from 'vue-multiselect'

  export default {
    components: {
      Multiselect
    },
    computed: {
      ...mapGetters(['filters']),
      modelValue: {
        get() {
          return this.filters.daycareType
            ? _.find(this.clubOptions, ['value', this.filters.daycareType])
            : null
        },
        set(value) {
          this.$emit('daycareFilterChanged', value.value)
        }
      },
      clubOptions() {
        return Object.values(
          this.$t('map-filters.care-type-options', { returnObjects: true })
        )
      }
    }
  }
</script>

<style lang="scss" scoped>
  .label-blip {
    display: inline-block;
    width: 0.75rem;
    height: 0.75rem;
    border-radius: 50%;

    &.centre {
      background-color: tint($blue, 20%);
    }

    &.family {
      background-color: $color-daycare-family;
    }

    &.group {
      background-color: $color-daycare-group-family;
    }

    &.club {
      background-color: $orange;
    }

    &.preschool {
      background-color: $observatory;
    }
  }
</style>

<style lang="scss">
  .search-group {
    .multiselect__option--selected {
      background: $white;
      font-weight: normal;
      color: inherit;
      opacity: 0.5;
    }
  }
</style>
