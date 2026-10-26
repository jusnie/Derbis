#!/usr/bin/env python3
"""Shared prompt-to-skill router for Derbis coding runtimes."""

from __future__ import annotations

import argparse
import json
import re
import sys
from dataclasses import dataclass
from typing import Any

MAX_ACTIVE_SKILLS = 2


@dataclass(frozen=True)
class Route:
    skill: str
    topic: str
    pattern: re.Pattern[str]


def route(skill: str, topic: str, pattern: str) -> Route:
    return Route(skill, topic, re.compile(pattern, re.I))


ROUTES = (
    route("derbis-project-manager", "requirements, roadmap, acceptance criteria, or implementation handoff", r"requirements?|requisit|roadmap|acceptance criteria|criteri.*accett|active phase|fase attiv|task status|stato.*task|milestone|implementation handoff|handoff.*implement|docs/project/(?:spec|roadmap|tasks)"),
    route("derbis-openclaw-orchestrator", "OpenClaw delegation or worker/reviewer coordination", r"openclaw|derbis-worker|derbis-reviewer|derbis-ci|delegat.*(?:agent|runtime|review|ci)|orchestrat"),
    route("derbis-real-engineering", "complex engineering workflow", r"architecture|architett|cross.?domain|multi.?step|pi[uù].*modul|across.*module|concurrenc|race condition|root cause|causa radice|specification|specifica|grill-with-docs|wayfinder|to-spec|to-tickets|large unresolved|unclear defect|complex refactor|riprogett"),
    route("derbis-player", "playback, queue, Media3, or MediaSession", r"player|playback|riproduz|queue|coda|media3|mediasession|android auto|notification|notifica|prefetch|gapless|crossfade|\bseek\b|\bskip\b|traccia|brano|audio mode|video mode"),
    route("derbis-extractor", "stream extraction or network fallback", r"extractor|estrattor|innertube|newpipe|youtube|stream|resolver|player.?config|(?:youtube|innertube|player|po)[ -]?token|potoken|visitor data|scraping|403|throttl"),
    route("derbis-database", "Room storage, persistence, or schema", r"\broom\b|\bdao\b|entity|entità|migration|migrazion|schema|database|\bdb\b|backup|persist"),
    route("derbis-compose", "Android Compose UI, accessibility, lifecycle, or state projection", r"compose|composable|\bui\b|\bscreen\b|schermat|theme|\btema\b|layout|recomposition|ricompos|scroll|layout inspector|talkback|\bsemantics\b|touch target|accessibilit|rtl|localizzazion|localization|string resource"),
    route("derbis-android-performance", "Android runtime profiling, memory stability, or measured performance", r"perfetto|system trace|trace processor|frame miss|frame drop|jank|latency|latenza|startup|avvio lento|cpu schedul|thread state|runnable|blocked thread|binder wait|binder spam|binder storm|lock contention|renderthread|frame timeline|gpu memory|texture upload|memory pressure|memory leak|memory growth|memory churn|native memory|native heap|\bram\b|\boom\b|out of memory|low memory|\bpss\b|\brss\b|dumpsys meminfo|heapprofd|bufferpool|bytebuffer|bitmap memory|allocat|i/o stall|io stall|d-state|lmkd|psi|wakelock|power rail|power trace|battery trace|runtime performance|performance runtime|profiling android"),
    route("derbis-r8-proguard", "R8, Proguard, minification, shrinking, or release-only behavior", r"\br8\b|proguard|minif|shrink resources|resource shrink|resource shrinking|keep rule|consumer rule|mapping\.txt|missing rules|missing class|missing classes|release.?only.*(?:crash|fail)|apk size|aab size|obfuscat|shrinker|dontwarn|keepattributes|javascriptinterface|jni.*(?:keep|shrink)|reflection.*(?:keep|shrink)|serialization.*(?:keep|shrink)|kotlin metadata"),
    route("derbis-android-intent-security", "Android Intent, PendingIntent, deep-link, or component security", r"pendingintent|pending intent|onnewintent|nested intent|intent redirection|intent redirect|intent sanitizer|intentsanitizer|android:exported|exported (?:activity|service|receiver|provider|component)|attivit[aà] esportat|servizio esportat|receiver esportat|provider esportat|mutable pendingintent|immutable pendingintent|flag_mutable|flag_immutable|uri grant|granturipermission|grant uri|fileprovider|contentprovider|signature permission|binder caller|callinguid|caller verification|deep.?link.*(?:intent|security|exported|permission)|intent.*(?:security|sicurezz|exported|permission|forward|redirect|nested)|component boundary|component-boundary"),
    route("derbis-design-taste", "visual design, hierarchy, polish, or anti-template UI", r"visual design|ui design|design ui|grafica|interfaccia|ui polish|visual polish|gerarchia visual|visual hierarchy|spacing|spaziatur|typograph|tipograf|color palette|palette colori|corner radius|motion ui|ui motion|animazion|design reference|ui reference|riferiment.*(?:grafica|ui|schermat)|(?:ui|screen|schermat|layout|design|grafica|interfaccia).{0,30}screenshot|screenshot.{0,30}(?:ui|screen|schermat|layout|design|grafica|interfaccia|reference|riferimento)|pi[uù] bella|pi[uù] professionale|(?:\bscreen\b|schermat\w*|\bui\b|\bdesign\b|grafica|interfaccia|\blayout\b|\bplayer\b|\bhome\b|\bnow playing\b).{0,40}\b(?:premium|modern|clean|cinematic|less generic)\b|\b(?:premium|modern|clean|cinematic|less generic)\b.{0,40}(?:\bscreen\b|schermat\w*|\bui\b|\bdesign\b|grafica|interfaccia|\blayout\b|\bplayer\b|\bhome\b|\bnow playing\b)|^\s*(?:premium|modern|clean|cinematic|less generic)\s*[.!?]*\s*$|material[ -]?3|m3 expressive|expressive (?:ui|design)|anti.?ai.?slop|glassmorphism|bento"),
    route("derbis-motion-artwork", "motion artwork", r"motion artwork|animated artwork|animated cover|canvas|copertin.*animat|cover art.*animat"),
    route("derbis-desktop", "Windows Desktop, Compose Multiplatform, libvlc, packaging, or desktop updates", r"\bdesktop\b|windows client|compose multiplatform|libvlc|mini player|mini-player|protocol registration|wix|msi|desktop update|desktop release"),
    route("derbis-ci-workflows", "CI, Gradle/Kotlin tooling, or build performance", r"github actions|\bworkflow\b|\bci\b|fdroid|f-droid|\bgradle\b|\bagp\b|\bksp\b|kotlin(?: compiler| plugin| toolchain| version)|(?:upgrade|update|bump|migrat).{0,24}\bkotlin\b|\bkotlin\b.{0,24}(?:gradle|compiler|ksp|toolchain)|build performance|slow build|build lento|configuration cache|build cache|compile time|compilation time|build logic|gradle\.properties|artifact"),
    route("derbis-security-review", "security, privacy, trust-boundary, or supply-chain review", r"security|sicurezz|secret|segret|credential|cookie|auth|ssrf|redirect|permission|permess|privacy|\bmime\b|keystore|vulnerab|exploit|threat model|trust boundary|attack surface|dependency|dipendenz|supply.?chain|workflow permission|action pin|signature|checksum|integrity|update security|deep.?link|pendingintent|pending intent|onnewintent|android:exported|fileprovider|contentprovider|uri grant|path traversal|injection|cve|token leak|data leak"),
    route("derbis-pr-review", "final branch, commit, diff, or pull-request review", r"review|revision|pull request|\bpr\b|merge|\bdiff\b|commit review|branch review|before merging|prima di merg"),
    route("derbis-humanizer", "pull request description", r"(?:open|create|draft|prepare|write|update|edit|rewrite|publish|apr|crea|prepara|scriv|aggiorn|modific|riscriv).{0,40}(?:pull request|\bpr\b)|(?:pull request|\bpr\b).{0,30}(?:description|body|descrizion)"),
    route("derbis-release-check", "runtime, pre-merge, or release validation", r"release|rilasci|versionname|versioncode|app version|release version|versione (?:app|release)|\bapk\b|signing|firma|tag\b|publish|pubblic|emulator|emulatore|physical device|device test|test dispositivo|\badb\b|connectedcheck|smoke test|runtime verification|runtime validation|verifica runtime|pre.?merge"),
    route("derbis-engineering", "cross-domain work or repository architecture orientation", r"cross.?domain|pi[uù].*sottosistem|multiple subsystems|several subsystems|repository orientation|architecture orientation|orient.*(?:repo|codebase|architett)"),
    route("derbis-android-reverse-engineering", "Android artifact decompilation, compiled API extraction, or binary analysis", r"decompil|reverse[ -]?engineer|jadx|smali|bundletool|apktool|binary analysis|r8 metadata|kotlin metadata recovery|(?:apk|xapk|aab|dex|jar|aar).{0,40}(?:analy[sz]|inspect|extract|decompil)|(?:analy[sz]|inspect|extract|decompil).{0,40}(?:apk|xapk|aab|dex|jar|aar)|extract.{0,30}(?:api|endpoint).{0,30}(?:apk|xapk|aab|dex|jar|aar)"),
    route("derbis-codex-bootstrap", "Claude/Codex bootstrap, skill routing, or token-efficiency infrastructure", r"claude|codex|agent skill|skill rout|token usage|token budget|jcodemunch|rtk|bootstrap|\.agents/|\.claude/|\.codex/"),
)

AUTOMATED_MARKERS = ("<github-webhook-activity", "<untrusted_external_data", "<system-reminder")
DEFERRED_SKILLS = {"derbis-pr-review", "derbis-humanizer", "derbis-release-check"}
CORE_RULE_SKILLS = {"derbis-mode", "derbis-context-efficiency"}


def _matched(prompt: str) -> list[tuple[str, str]]:
    result: list[tuple[str, str]] = []
    seen: set[str] = set()
    for item in ROUTES:
        if item.pattern.search(prompt) and item.skill not in seen:
            result.append((item.skill, item.topic))
            seen.add(item.skill)
    return result


def _add(result: list[tuple[str, str]], skill: str, topic: str) -> None:
    if all(existing != skill for existing, _ in result):
        result.append((skill, topic))


def route_prompt(prompt: str) -> list[tuple[str, str]]:
    text = prompt.strip().lower()
    if not text or any(marker in text for marker in AUTOMATED_MARKERS):
        return []

    matched = [(skill, topic) for skill, topic in _matched(text) if skill not in CORE_RULE_SKILLS]
    implementation = [(skill, topic) for skill, topic in matched if skill not in DEFERRED_SKILLS]
    if implementation:
        matched = implementation

    skills = {skill for skill, _ in matched}
    if "derbis-android-intent-security" in skills:
        _add(matched, "derbis-security-review", "Android component-boundary security review")
    if "derbis-design-taste" in skills and "derbis-desktop" not in skills and re.search(r"android|compose|player|now playing|home|screen|schermat|ui|grafica|interfaccia", text):
        _add(matched, "derbis-compose", "Android visual implementation")
    if "derbis-openclaw-orchestrator" in skills:
        _add(matched, "derbis-project-manager", "delegated acceptance criteria and handoff")
    if "derbis-android-reverse-engineering" in skills:
        if re.search(r"\br8\b|proguard|obfuscat|mapping\.txt|kotlin metadata", text):
            _add(matched, "derbis-r8-proguard", "obfuscation and Kotlin/R8 metadata recovery")
        else:
            _add(matched, "derbis-security-review", "artifact trust-boundary review")

    priority = {
        "derbis-android-reverse-engineering": 0, "derbis-android-intent-security": 0, "derbis-design-taste": 0, "derbis-openclaw-orchestrator": 0,
        "derbis-r8-proguard": 1, "derbis-security-review": 1, "derbis-android-performance": 1, "derbis-compose": 1, "derbis-desktop": 1,
        "derbis-player": 1, "derbis-extractor": 1, "derbis-database": 1, "derbis-ci-workflows": 1, "derbis-motion-artwork": 1, "derbis-project-manager": 1,
        "derbis-engineering": 2, "derbis-real-engineering": 2, "derbis-codex-bootstrap": 2, "derbis-pr-review": 3, "derbis-release-check": 3, "derbis-humanizer": 4,
    }
    indexed = list(enumerate(matched))
    indexed.sort(key=lambda value: (priority.get(value[1][0], 9), value[0]))
    return [item for _, item in indexed[:MAX_ACTIVE_SKILLS]]


def context_for(prompt: str) -> str:
    matched = route_prompt(prompt)
    if not matched:
        return ""
    lines = [f"Mandatory skill load for this phase (max {MAX_ACTIVE_SKILLS}):"]
    for skill, topic in matched:
        lines.append(f"- {topic} -> .agents/skills/{skill}/SKILL.md")
    lines.append("Do not preload other skill bodies; route the next phase only when it starts.")

    active = {skill for skill, _ in matched}
    is_android_ui = bool(active & {"derbis-compose", "derbis-design-taste"}) and (
        "derbis-desktop" not in active or bool(re.search(r"\bandroid\b", prompt, re.I))
    )
    kotlin_domains = {
        "derbis-compose", "derbis-design-taste", "derbis-player",
        "derbis-extractor", "derbis-database", "derbis-desktop",
        "derbis-ci-workflows", "derbis-android-performance",
    }
    if is_android_ui:
        lines.append(
            "Later UI compliance phase: consult installed upstream material-3 SKILL.md "
            "(hamen/material-3-skill), then only relevant references; verify Derbis APIs."
        )
    if active & kotlin_domains:
        lines.append(
            "Later Kotlin review phase: consult the matching installed "
            "maxrave-dev/kotlin-footguns area SKILL.md and only relevant trap files; "
            "run footgun-scan if useful. Never exceed two skill bodies per phase."
        )
    return "\n".join(lines)


def _payload() -> dict[str, Any]:
    try:
        value = json.load(sys.stdin)
    except Exception:
        return {}
    return value if isinstance(value, dict) else {}


def main() -> int:
    parser = argparse.ArgumentParser()
    parser.add_argument("--prompt")
    parser.add_argument("--plain", action="store_true")
    args = parser.parse_args()
    data = {} if args.prompt is not None else _payload()
    prompt = args.prompt if args.prompt is not None else str(data.get("prompt") or "")
    context = context_for(prompt)
    if args.plain:
        print(context)
        return 0
    print(json.dumps({"hookSpecificOutput": {"hookEventName": "UserPromptSubmit", "additionalContext": context}}))
    return 0


if __name__ == "__main__":
    raise SystemExit(main())
