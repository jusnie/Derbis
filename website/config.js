/*
 * Public browser configuration only. A Google OAuth Web Client ID is safe to expose;
 * do not put a client secret, account cookie, or API key here.
 *
 * Create a Web application OAuth client in Google Cloud, enable YouTube Data API v3,
 * add your deployed https origin under Authorized JavaScript origins, then set its ID.
 */
window.DERBIS_WEB_CONFIG = {
  googleOAuthClientId: "584141503005-h3lamjrir2ehbpjl3pruojg52u0hvu2j.apps.googleusercontent.com",
  googleOAuthScopes: [
    "openid",
    "email",
    "profile",
    "https://www.googleapis.com/auth/youtube.readonly"
  ]
};
