/*
 * Public browser configuration only. A Google OAuth Web Client ID and a browser-restricted
 * YouTube Data API key are safe to expose. Never put a client secret, account cookie,
 * service-account JSON, or an unrestricted API key here.
 *
 * Restrict the YouTube key to the deployed HTTPS origin and YouTube Data API v3 only.
 */
window.DERBIS_WEB_CONFIG = {
  googleOAuthClientId: "584141503005-h3lamjrir2ehbpjl3pruojg52u0hvu2j.apps.googleusercontent.com",
  youtubeDataApiKey: "AIzaSyAdv_xV2Tmy-8afPYvRUS5-8bevlvhpHpM",
  googleOAuthScopes: [
    "openid",
    "email",
    "profile",
    "https://www.googleapis.com/auth/youtube.readonly"
  ]
};
