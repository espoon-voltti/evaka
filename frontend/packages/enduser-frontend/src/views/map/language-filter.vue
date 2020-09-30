<!--
SPDX-FileCopyrightText: 2017-2020 City of Espoo

SPDX-License-Identifier: LGPL-2.1-or-later
-->

<template>
  <div class="search-group">
    <span>{{ $t('map-filters.language') }}:</span>
    <multiselect
      v-model="selectedLanguages"
      :options="languages"
      :close-on-select="false"
      :clear-on-select="false"
      :hide-selected="true"
      label="label"
      track-by="value"
      :multiple="true"
      :placeholder="$t('map-view.language-placeholder')"
      selectLabel=""
    >
    </multiselect>
  </div>
</template>

<script>
  import { mapGetters } from 'vuex'
  import Multiselect from 'vue-multiselect'

  export default {
    data() {
      return {
        selectedLanguages: []
      }
    },
    components: {
      Multiselect
    },
    computed: {
      ...mapGetters(['filters']),
      languages() {
        return Object.values(
          this.$t('constants.language', { returnObjects: true })
        )
      }
    },
    watch: {
      selectedLanguages() {
        const filter = this.selectedLanguages.map((option) => option.value)
        this.$emit('languageFilterChanged', filter)
      },
      filters() {
        if (!this.filters.language.length) {
          this.selectedLanguages = []
        }
      }
    }
  }
</script>
