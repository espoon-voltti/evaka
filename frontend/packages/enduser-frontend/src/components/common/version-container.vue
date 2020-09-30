<!--
SPDX-FileCopyrightText: 2017-2020 City of Espoo

SPDX-License-Identifier: LGPL-2.1-or-later
-->

<template>
  <div class="evaka">
    <h2>
      Järjestelmän versiotiedot
    </h2>
    <template v-for="(item, index) in this.versionInfos">
      <version :info="item" :key="index"></version>
    </template>
  </div>
</template>

<script>
  import versions from '@/api/versions'
  import Version from '@/components/common/version.vue'

  export default {
    components: {
      Version
    },
    data: () => {
      return {
        versionInfos: [],
        systemComponents: ['api-gateway', 'application', 'map']
      }
    },
    methods: {
      ownVersion() {
        return versions.ownVersion()
      },
      getAllVersions() {
        return Promise.all(
          this.systemComponents.map((item) => {
            return versions.getVersion(item)
          })
        )
      }
    },
    async created() {
      this.versionInfos.push(await this.ownVersion())
      this.versionInfos = this.versionInfos.concat(await this.getAllVersions())
    }
  }
</script>

<!-- Add "scoped" attribute to limit CSS to this component only -->
<style lang="scss" scoped="true">
  h2 {
    font-size: larger;
  }
</style>
