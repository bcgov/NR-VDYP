<template>
  <v-empty-state icon="mdi-magnify" class="mt-10 empty-state-title" title="The page you are looking for cannot be found.">
    <div class="mt-4 empty-state-content">
      <p>The page you're looking for might have been removed, moved, or is temporarily unavailable.</p>
      <p class="mb-5">Suggestions to help you find what you're looking for:</p>
      <ul class="ml-7">
        <li>Check that the web URL has been entered correctly</li>
        <li>Go to our Home and browse through our topics for the information you want</li>
      </ul>
    </div>
  </v-empty-state>
</template>
<script setup lang="ts"></script>
<style scoped>
.empty-state-title {
  font-size: 1.5rem;
  font-weight: 600;
}

.empty-state-content {
  font-size: 1rem;
  font-weight: 500;
}
</style>
