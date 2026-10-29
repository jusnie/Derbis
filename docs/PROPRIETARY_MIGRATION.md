# Derbis Proprietary Migration Plan

**Status: discovery and rewrite phase. The current repository and releases remain GPL-3.0.**

Derbis cannot truthfully or safely be relicensed as a proprietary combined application
until every GPL/incompatible component is removed, replaced, or separately licensed,
and every necessary copyright holder has authorized the change. Earlier GPL grants are
irrevocable for copies already received under GPL.

## Current blockers found in the source tree

| Blocker | Current use | Required proprietary-release action |
| --- | --- | --- |
| `third_party/DerbisExtractor` / NewPipe lineage | Included Gradle build; Android and desktop extraction, search, stream resolution, comments, Shorts, and playback support | Remove from production builds and replace with an independently authored, appropriately licensed implementation or authorized official provider APIs. |
| `libs.newpipe.extractor` | Direct Android production dependency | Remove after all NewPipe API call sites are rewritten behind a proprietary provider gateway. |
| `third_party/DerbisNexus` | Shared Android/Desktop core, presently labelled GPL because of the repository license | Audit provenance and relicensing authority; retain only after Yasuda confirms all code is original or separately compatible. |
| Desktop extractor integration | Uses DerbisExtractor and includes DerbisNexus source directly | Replace with the same independent provider gateway before a proprietary desktop build. |

## Phase 0 — preserve legal clarity

- Do **not** replace the root `LICENSE` yet.
- Do **not** describe Derbis as proprietary, closed source, or all-rights-reserved yet.
- Retain `THIRD_PARTY_NOTICES.md` and all upstream notices while GPL-derived material exists.
- Preserve the existing GPL tags/releases; do not try to revoke their permissions.
- Keep the proposed future documents as drafts only:
  - [`DERBIS_PROPRIETARY_LICENSE_DRAFT.md`](DERBIS_PROPRIETARY_LICENSE_DRAFT.md)
  - [`DERBIS_TRADEMARK_POLICY_DRAFT.md`](DERBIS_TRADEMARK_POLICY_DRAFT.md)

## Phase 1 — freeze and inventory

- Freeze additions copied, adapted, or translated from GPL/AGPL projects.
- Create a software bill of materials for Android, desktop, native, and build-time
  dependencies, including license and provenance for each item.
- Obtain written contributor assignments/consents for every non-Yasuda contribution.
- Have counsel review whether any current “inspired by” implementation contains
  protectable GPL-derived expression.

## Phase 2 — replace GPL functionality

### Android

1. Introduce a provider-agnostic `MusicCatalogGateway` and `PlaybackSourceGateway` owned by
   Derbis.
2. Reimplement or remove the Android functionality currently importing NewPipe APIs:
   runtime/bootstrap, search/Shorts, stream resolving, comments, metadata enrichment,
   playback compatibility, and YouTube-specific decoding/session handling.
3. Use only independently authored code and APIs/services whose contracts permit the intended
   use. Do not copy GPL source or use AI output that reproduces it.
4. Remove `libs.newpipe.extractor`, `includeBuild("third_party/DerbisExtractor")`, and related
   substitutions only after replacement tests pass.

### Desktop

1. Replace desktop `YoutubeStreamResolver`, `ExtractorRuntime`, downloader, and catalog
   integrations with the approved proprietary gateway.
2. Remove the desktop DerbisExtractor composite build and dependency.
3. Audit DerbisNexus independently before relicensing or folding original code into an owned
   module.

### UI and assets

1. Preserve Derbis-owned design briefs and source history for the Apple Music 13 player, lyrics,
   queue, artwork, and visual-deck surfaces.
2. Audit future design work for third-party source or asset incorporation before release.

## Phase 3 — verify before changing the license

A proprietary release may proceed only when all items below are complete:

- [ ] No GPL/AGPL production dependency or incorporated source remains.
- [ ] All third-party dependency licenses are compatible with proprietary distribution.
- [ ] Copyright ownership/assignments are documented.
- [ ] A qualified Philippine IP/software lawyer approves the final license, EULA,
      trademark policy, AI restriction, privacy terms, and notices.
- [ ] The official source repository is made private or removed from public distribution.
- [ ] Official binaries display the final EULA/acceptance flow where appropriate.
- [ ] Root `LICENSE`, README badge, Terms of Service, desktop packaging metadata, and
      third-party notices are updated together.
- [ ] A clean build, SBOM scan, and legal-notice review pass for Android and desktop.

## Publication sequence after verification

1. Archive the final GPL release and preserve its source/notice obligations.
2. Make future proprietary source private before publishing new code.
3. Replace the root `LICENSE` with the lawyer-approved final proprietary license.
4. Publish the trademark policy and EULA with the first proprietary release.
5. Release a new version number; do not relabel an existing GPL release.
