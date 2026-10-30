// Platform detection utility for Derbis
// Ensures iOS / IPA builds only display features native and supported on Apple devices
export const isIos = typeof window !== "undefined" && (
  /iPad|iPhone|iPod/.test(navigator.userAgent) ||
  (navigator.platform === "MacIntel" && navigator.maxTouchPoints > 1) ||
  window.Capacitor?.getPlatform?.() === "ios"
);

export const isNativeIpa = typeof window !== "undefined" && (
  window.Capacitor?.getPlatform?.() === "ios" ||
  window.location.protocol === "derbis:" ||
  window.location.protocol === "capacitor:" ||
  window.navigator.standalone === true
);

export const isAppleDevice = typeof window !== "undefined" && (
  isIos || /Macintosh|MacIntel|MacPPC|Mac68K/.test(navigator.userAgent)
);
