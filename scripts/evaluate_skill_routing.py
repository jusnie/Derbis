#!/usr/bin/env python3
from __future__ import annotations

import json
from dataclasses import asdict, dataclass
from typing import Iterable

try:
    from scripts.agent_skill_router import context_for, route_prompt
except ModuleNotFoundError:
    from agent_skill_router import context_for, route_prompt


@dataclass(frozen=True)
class EvalCase:
    name: str
    prompt: str
    required: tuple[str, ...] = ()
    forbidden: tuple[str, ...] = ()
    max_skills: int = 2
    max_context_bytes: int = 2048


@dataclass(frozen=True)
class EvalResult:
    name: str
    status: str
    prompt: str
    selected: tuple[str, ...]
    missing: tuple[str, ...]
    forbidden_selected: tuple[str, ...]
    context_bytes: int
    max_skills: int
    max_context_bytes: int
    error: str | None = None


CASES = (
    EvalCase(
        "owner-action-mode",
        "VAI, intervieni e apri la PR",
        required=("derbis-pr-review", "derbis-humanizer"),
        forbidden=("derbis-mode", "derbis-context-efficiency"),
    ),
    EvalCase(
        "owner-action-playback",
        "VAI sistema il playback crash",
        required=("derbis-player",),
        forbidden=("derbis-mode", "derbis-real-engineering", "derbis-context-efficiency"),
        max_skills=1,
    ),
    EvalCase(
        "implementation-defers-pr-phase",
        "VAI fix playback crash, test release APK, review and open PR",
        required=("derbis-player",),
        forbidden=(
            "derbis-mode",
            "derbis-real-engineering",
            "derbis-context-efficiency",
            "derbis-pr-review",
            "derbis-humanizer",
            "derbis-release-check",
        ),
        max_skills=1,
    ),
    EvalCase(
        "owner-mode-near-miss-edit",
        "Modifica il README",
        forbidden=("derbis-mode", "derbis-context-efficiency"),
        max_skills=1,
    ),
    EvalCase(
        "owner-mode-near-miss-compose",
        "Implementa una piccola modifica Compose",
        required=("derbis-compose",),
        forbidden=("derbis-mode", "derbis-context-efficiency"),
        max_skills=1,
    ),
    EvalCase(
        "context-near-miss-analysis",
        "Analyze this function",
        forbidden=("derbis-context-efficiency", "derbis-mode"),
        max_skills=1,
    ),
    EvalCase(
        "playback-crash",
        "Fix a playback crash when skipping tracks",
        required=("derbis-player",),
        forbidden=("derbis-real-engineering", "derbis-design-taste", "derbis-android-reverse-engineering"),
        max_skills=1,
    ),
    EvalCase(
        "compose-jank",
        "Compose jank while scrolling the album screen",
        required=("derbis-compose", "derbis-android-performance"),
        forbidden=("derbis-real-engineering", "derbis-design-taste"),
    ),
    EvalCase(
        "compose-semantics",
        "Fix Compose semantics for TalkBack",
        required=("derbis-compose",),
        forbidden=("derbis-release-check",),
        max_skills=1,
    ),
    EvalCase(
        "pending-intent-security",
        "Audit mutable PendingIntent handling",
        required=("derbis-android-intent-security", "derbis-security-review"),
    ),
    EvalCase(
        "reverse-r8",
        "Decompile this APK and recover Kotlin R8 metadata",
        required=("derbis-android-reverse-engineering", "derbis-r8-proguard"),
        forbidden=("derbis-security-review", "derbis-release-check"),
    ),
    EvalCase(
        "normal-release-apk",
        "Build and validate the release APK",
        required=("derbis-release-check",),
        forbidden=("derbis-context-efficiency", "derbis-android-reverse-engineering"),
        max_skills=1,
    ),
    EvalCase(
        "visual-premium",
        "Make the Now Playing screen more premium",
        required=("derbis-design-taste", "derbis-compose"),
        forbidden=("derbis-ci-workflows",),
    ),
    EvalCase(
        "design-near-miss-kotlin",
        "Upgrade to a modern Kotlin compiler version",
        required=("derbis-ci-workflows",),
        forbidden=("derbis-design-taste", "derbis-release-check"),
        max_skills=1,
    ),
    EvalCase(
        "design-near-miss-gradle",
        "Clean Gradle build outputs",
        required=("derbis-ci-workflows",),
        forbidden=("derbis-context-efficiency", "derbis-design-taste"),
        max_skills=1,
    ),
    EvalCase(
        "design-near-miss-subscription",
        "Integrate a premium subscription API",
        forbidden=("derbis-design-taste",),
        max_skills=1,
    ),
    EvalCase(
        "design-near-miss-screenshot",
        "Inspect this screenshot of a CI stack trace",
        required=("derbis-ci-workflows",),
        forbidden=("derbis-context-efficiency", "derbis-design-taste", "derbis-compose"),
        max_skills=1,
    ),
    EvalCase(
        "extractor-near-miss-agent-token",
        "Reduce Claude token usage in the coding agents",
        required=("derbis-codex-bootstrap",),
        forbidden=("derbis-extractor", "derbis-context-efficiency"),
        max_skills=1,
    ),
    EvalCase(
        "extractor-player-token",
        "Investigate an InnerTube player token failure",
        required=("derbis-player", "derbis-extractor"),
    ),
    EvalCase(
        "plain-kotlin-question",
        "Explain Kotlin sealed classes",
        forbidden=("derbis-ci-workflows", "derbis-release-check"),
        max_skills=1,
    ),
    EvalCase(
        "semantic-version-near-miss",
        "Prepare a semantic version release",
        required=("derbis-release-check",),
        forbidden=("derbis-compose",),
        max_skills=1,
    ),
    EvalCase(
        "debugging-route",
        "Debug an intermittent queue state corruption",
        required=("derbis-player",),
        forbidden=("derbis-real-engineering",),
        max_skills=1,
    ),
    EvalCase(
        "project-manager",
        "Update roadmap acceptance criteria for the active phase",
        required=("derbis-project-manager",),
        forbidden=("derbis-real-engineering",),
        max_skills=1,
    ),
    EvalCase(
        "openclaw-handoff",
        "Delegate this Derbis fix through OpenClaw",
        required=("derbis-openclaw-orchestrator", "derbis-project-manager"),
        forbidden=("derbis-context-efficiency",),
    ),
    EvalCase(
        "cross-domain",
        "Investigate a cross-domain architecture issue across subsystems",
        required=("derbis-real-engineering", "derbis-engineering"),
        forbidden=("derbis-context-efficiency",),
    ),
    EvalCase(
        "security-review",
        "Review this code for a concrete security vulnerability",
        required=("derbis-security-review",),
        forbidden=("derbis-pr-review",),
        max_skills=1,
    ),
)


def evaluate_case(case: EvalCase) -> EvalResult:
    try:
        selected = tuple(skill for skill, _ in route_prompt(case.prompt))
        context_bytes = len(context_for(case.prompt).encode("utf-8"))
    except Exception as exc:
        return EvalResult(
            name=case.name,
            status="ERROR",
            prompt=case.prompt,
            selected=(),
            missing=case.required,
            forbidden_selected=(),
            context_bytes=0,
            max_skills=case.max_skills,
            max_context_bytes=case.max_context_bytes,
            error=f"{type(exc).__name__}: {exc}",
        )

    selected_set = set(selected)
    missing = tuple(skill for skill in case.required if skill not in selected_set)
    forbidden_selected = tuple(skill for skill in case.forbidden if skill in selected_set)
    over_budget = len(selected) > case.max_skills or context_bytes > case.max_context_bytes
    duplicates = len(selected) != len(selected_set)
    status = "PASS" if not (missing or forbidden_selected or over_budget or duplicates) else "FAIL"

    return EvalResult(
        name=case.name,
        status=status,
        prompt=case.prompt,
        selected=selected,
        missing=missing,
        forbidden_selected=forbidden_selected,
        context_bytes=context_bytes,
        max_skills=case.max_skills,
        max_context_bytes=case.max_context_bytes,
    )


def evaluate_all(cases: Iterable[EvalCase] = CASES) -> tuple[EvalResult, ...]:
    return tuple(evaluate_case(case) for case in cases)


def summary(results: Iterable[EvalResult]) -> dict[str, float | int]:
    items = tuple(results)
    passed = sum(result.status == "PASS" for result in items)
    failed = sum(result.status == "FAIL" for result in items)
    errors = sum(result.status == "ERROR" for result in items)
    context_values = [result.context_bytes for result in items if result.status != "ERROR"]
    return {
        "total": len(items),
        "passed": passed,
        "failed": failed,
        "errors": errors,
        "avg_context_bytes": round(sum(context_values) / len(context_values), 2) if context_values else 0,
        "max_context_bytes": max(context_values, default=0),
    }


def main() -> int:
    results = evaluate_all()
    payload = {"summary": summary(results), "results": [asdict(result) for result in results]}
    print(json.dumps(payload, indent=2))
    return 0 if all(result.status == "PASS" for result in results) else 1


if __name__ == "__main__":
    raise SystemExit(main())
