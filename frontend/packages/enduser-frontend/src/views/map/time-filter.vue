<!--
SPDX-FileCopyrightText: 2017-2020 City of Espoo

SPDX-License-Identifier: LGPL-2.1-or-later
-->

<template>
  <div class="search-group">
    <label>
      <input type="checkbox" v-model="roundTheClock" />
      <span>{{ $t('map-filters.shift-care') }}</span>
    </label>
  </div>
</template>

<script>
  export default {
    data() {
      return {
        roundTheClock: undefined
      }
    },
    watch: {
      roundTheClock() {
        this.$emit('timeFilterChanged', this.roundTheClock)
      }
    }
  }
</script>

<style lang="scss" scoped>
  label,
  input {
    cursor: pointer;
  }
</style>
