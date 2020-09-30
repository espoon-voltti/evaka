<!--
SPDX-FileCopyrightText: 2017-2020 City of Espoo

SPDX-License-Identifier: LGPL-2.1-or-later
-->

<template>
  <c-view-container>
    <c-view-header
      v-if="title !== undefined && text !== undefined"
      :title="title"
      :text="text"
    />
    <c-view-content>
      <slot />

      <c-view-toolbar>
        <slot name="toolbar" />
      </c-view-toolbar>
    </c-view-content>
  </c-view-container>
</template>
<script lang="ts">
  import Vue from 'vue'
  export default Vue.extend({
    props: {
      title: String as () => string,
      text: Array as () => string[]
    }
  })
</script>
