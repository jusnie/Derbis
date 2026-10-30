# Relicensing Derbis as GPL-3.0

Derbis moved from a bespoke end user licence agreement to the **GNU General Public License,
version 3** (`GPL-3.0-only`).

## Why

1. The app links or embeds code that is already `GPL-3.0-only`, most importantly the bundled
   ByeDPI components (`app/src/main/java/io/github/dovecoteescapee/byedpi`, `app/src/upstream/jniLibs`)
   and the NewPipe extractor dependency used by the Android and desktop providers. A proprietary
   combined distribution of those pieces is not permitted by their licences.
2. The previous plan was to strip every GPL dependency before publishing a closed build. That would
   have meant replacing the extractor, rewiring the desktop pipeline and dropping the DPI bridge,
   with no shipped benefit.
3. GPL matches how the project is actually built and shared, and it lets other people contribute
   without a per-contribution negotiation.

## What changed in the repository

- `LICENSE` now carries the GPL-3.0 text plus the copyright notice for Yasuda.
- `REUSE.toml` declares `GPL-3.0-only` for everything that does not carry an override.
- `README.md`, `THIRD_PARTY_NOTICES.md` and `third_party/DerbisNexus/LICENSE-NOTICE.md` describe GPL.
- `CONTRIBUTING.md` documents how contributions are licensed.

## What did not change

Third-party components keep their own licences: Ionicons (MIT), Lucide (ISC), Apache-2.0 Media3
parts, the Android Open Source Project files, and the ByeDPI bridge (`GPL-3.0-only AND MIT`). The
full list is in `THIRD_PARTY_NOTICES.md` and `REUSE.toml`.

## Scope

Releases published before this change stay under the terms they shipped with. Everything from this
commit forward is GPL-3.0-only.
