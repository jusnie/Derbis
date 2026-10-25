# Derbis web companion

A dependency-free static website for the Derbis project. It is deliberately **not** a replacement for the Android/Desktop music apps.

- Responsive at mobile, tablet, laptop, and desktop widths.
- Reuses the existing Derbis diamond asset from the Android project.
- Plays user-selected local audio with native browser HTML5 audio—**VLC is not required** and no player plug-in is bundled.
- Offers a compliant Google/YouTube connection through Google Identity Services and the official YouTube Data API v3.

## What the Google connection means

The connection is intentionally limited to official Google/YouTube account data allowed by the configured OAuth scopes. It may show the authorized Google profile and public YouTube channel metadata.

It **does not** claim to support YouTube Music web playback, YouTube Music library synchronization, account-cookie login, copied mobile app sessions, scraping, or private Google APIs. Those are not enabled here.

Authorization tokens are used in memory to query Google and the small non-secret profile display state is kept only in `sessionStorage`. Closing the browser session or pressing **Disconnect** clears it.

## Configure real Google / YouTube sign-in

A Google OAuth client ID is required. It is a public identifier; a browser SPA must never contain a Google OAuth client secret.

1. In Google Cloud Console, create/select the website project.
2. Enable **YouTube Data API v3**.
3. Configure the OAuth consent screen for the appropriate test/production audience.
4. Create an OAuth 2.0 **Web application** client.
5. Add the deployed HTTPS site origin exactly under **Authorized JavaScript origins**, for example:

   ```text
   https://derbis.example.com
   ```

   For local development add the exact local origin, such as `http://localhost:4173`.

6. Edit [`config.js`](config.js) and set the public client ID:

   ```js
   window.DERBIS_WEB_CONFIG = {
     googleOAuthClientId: "YOUR_WEB_CLIENT_ID.apps.googleusercontent.com",
     googleOAuthScopes: [
       "openid",
       "email",
       "profile",
       "https://www.googleapis.com/auth/youtube.readonly"
     ]
   };
   ```

Do not add an API key, OAuth client secret, password, browser cookie, or a YouTube Music session to `config.js` or to version control.

## Run locally

No package install is necessary:

```bash
cd website
python3 -m http.server 4173
```

Then open `http://localhost:4173`. Use an HTTP server rather than opening `index.html` directly so Google can validate the origin.

## Deploy to Vercel

The folder is static and contains `vercel.json`; no build step, VLC installation, Node module, server-side player, or desktop runtime is required.

1. Import the `website` directory as the Vercel project root.
2. Framework preset: **Other**.
3. Build command: leave empty.
4. Output directory: `.`.
5. Add the final `https://…` deployment origin to the Google OAuth client’s authorized JavaScript origins.
6. Set the same public OAuth client ID in `config.js` before deployment, or generate this public config file in your deployment process.

The Google client ID must be available to the browser. Treat it as public; protect only server-side credentials, which this static site does not use.
