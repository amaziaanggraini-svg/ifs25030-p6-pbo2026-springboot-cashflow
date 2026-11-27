// Perbaikan aksesibilitas (axe-core "region"): <vaadin-connection-indicator> milik Vaadin
// ditempelkan langsung ke <body>, di luar landmark. Menandainya sebagai live region
// membuat axe memperlakukannya sebagai region yang sah.
const markLiveRegion = () => {
  document.querySelectorAll('vaadin-connection-indicator').forEach((el) => {
    if (!el.hasAttribute('aria-live')) {
      el.setAttribute('aria-live', 'polite');
    }
  });
};

markLiveRegion();
new MutationObserver(markLiveRegion).observe(document.body, { childList: true });
