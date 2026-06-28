from __future__ import annotations

import asyncio
import json
import logging
import os
from contextlib import suppress

import httpx

log = logging.getLogger("system_agent.guardian")

CHECK_INTERVAL_S = int(os.getenv("GUARDIAN_CHECK_INTERVAL_S", "30"))


class Guardian:
    def __init__(self, pool, settings):
        self.pool = pool
        self.s = settings
        self._task: asyncio.Task | None = None
        self.state = {"core": "unknown", "ollama": "unknown"}

    def start(self) -> None:
        if self._task is None or self._task.done():
            self._task = asyncio.create_task(self._run(), name="monitoring-guardian")

    async def stop(self) -> None:
        if self._task:
            self._task.cancel()
            with suppress(asyncio.CancelledError):
                await self._task
            self._task = None

    async def _run(self) -> None:
        while True:
            await self._tick()
            await asyncio.sleep(CHECK_INTERVAL_S)

    async def _tick(self) -> dict:
        core, ollama = await asyncio.gather(self._check_core(), self._check_ollama())
        return {"core": core, "ollama": ollama}

    async def _check_core(self) -> dict:
        base = self.s.fazle_core_url.rstrip("/")
        targets = [f"{base}/health"]
        fallback = "http://127.0.0.1:8200/health"
        if targets[0] != fallback:
            targets.append(fallback)

        last_error = "unknown"
        for target in targets:
            try:
                # trust_env=False prevents local health checks from being routed
                # through proxy environment variables.
                async with httpx.AsyncClient(timeout=8.0, trust_env=False) as client:
                    response = await client.get(target)
                if response.status_code < 500:
                    self.state["core"] = "healthy"
                    await self._resolve_open("fazle_core_unreachable", "core_recovered")
                    return {
                        "status": "healthy",
                        "http_status": response.status_code,
                        "target": target,
                    }
                last_error = f"{target} returned HTTP {response.status_code}"
            except Exception as exc:
                last_error = f"{target} -> {str(exc)[:220]}"

        self.state["core"] = "unreachable"
        await self._record_once(
            "critical", "guardian", "fazle_core_unreachable",
            {"error": last_error[:300]},
        )
        return {"status": "unreachable", "error": last_error[:300]}

    async def _check_ollama(self) -> dict:
        try:
            async with httpx.AsyncClient(timeout=8.0) as client:
                response = await client.get(f"{self.s.ollama_url}/api/tags")
            response.raise_for_status()
            models = [m.get("name") for m in response.json().get("models", [])]
            if self.s.ollama_model not in models:
                self.state["ollama"] = "model_missing"
                await self._record_once(
                    "warn", "guardian", "ollama_model_missing",
                    {"expected": self.s.ollama_model, "available": models},
                )
                return {"status": "model_missing", "available": models}

            self.state["ollama"] = "idle_or_loaded"
            await self._resolve_open("ollama_unreachable", "ollama_recovered")
            await self._resolve_open("ollama_model_missing", "model_available")
            await self._resolve_open("ollama_model_evicted", "false_alarm_idle_state")
            return {"status": "healthy", "state": "idle_or_loaded", "available": models}
        except Exception as exc:
            self.state["ollama"] = "unreachable"
            await self._record_once(
                "critical", "guardian", "ollama_unreachable",
                {"error": str(exc)[:300]},
            )
            return {"status": "unreachable", "error": str(exc)[:300]}

    async def _record_once(self, severity: str, source: str, title: str, detail: dict) -> None:
        detail_json = json.dumps(detail, sort_keys=True, default=str)
        async with self.pool.acquire() as conn:
            await conn.execute(
                """INSERT INTO agent.incidents (severity, source, title, detail)
                   SELECT $1, $2, $3, $4::jsonb
                    WHERE NOT EXISTS (
                        SELECT 1 FROM agent.incidents
                         WHERE source=$2 AND title=$3 AND detail=$4::jsonb
                           AND resolved_at IS NULL
                    )""",
                severity, source, title, detail_json,
            )

    async def _resolve_open(self, title: str, fix: str) -> None:
        async with self.pool.acquire() as conn:
            await conn.execute(
                """UPDATE agent.incidents
                      SET resolved_at=NOW(), fix_applied=COALESCE(fix_applied, $2)
                    WHERE title=$1 AND resolved_at IS NULL""",
                title, fix,
            )
