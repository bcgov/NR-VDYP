<template>
  <v-navigation-drawer app v-model="drawer" permanent>
    <v-list density="compact" class="nv-project-menu">
      <router-link to="/" class="link-no-decoration">
        <v-list-item
          link
          prepend-icon="mdi-clipboard-text"
          class="nv-menu-item"
          value="projects"
        >
          <span class="nv-menu-item-title">Projects</span>
        </v-list-item>
      </router-link>
    </v-list>

    <div class="nv-drawer-footer">
      <span class="ml-2">Release: {{ appVersion }}</span>
    </div>
  </v-navigation-drawer>

  <div class="main-layout-container">
    <!-- <div class="content"> -->
    <slot />
    <!-- </div> -->
  </div>
</template>

<script setup lang="ts">
import { ref } from 'vue'

const appVersion = import.meta.env.PACKAGE_VERSION
const drawer = ref(true)
</script>

<style scoped>
/* v-navigation-drawer whole box */
.v-navigation-drawer {
  /* min-width: 256px; */
  background-color: #f6f6f6;
}

.nv-project-menu {
  margin-top: 20px;
}

.nv-menu-item {
  background-color: #e3e0d8;
  display: flex;
  justify-content: center;
  align-items: center;
  text-align: center;
}

.nv-menu-item-title {
  font-size: 14px !important;
  font-weight: bold !important;
}

.main-layout-container {
  display: flex;
  flex-direction: column;
  flex: none;
  margin-bottom: 0px;
}

.nv-drawer-footer {
  text-align: center;
  font-size: 12px;
  color: #9fa2a2;
  padding: 10px 0;
  position: absolute;
  bottom: 0;
  width: 100%;
}

/* .content {
  flex: none;
  margin-bottom: 0px;
} */
</style>
