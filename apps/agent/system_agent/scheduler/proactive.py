from __future__ import annotations

import asyncio
from contextlib import suppress


class ProactiveScheduler:
    """Read-only incident evaluator; monitoring-only mode never sends messages."""

    def __init__(self, pool, settings):
        self.pool = pool
        self.settings = settings
        self._task: asyncio.Task | None = None

    def start(self) -> None:
        if self._task is None or self._task.done():
            self._task = asyncio.create_task(self._run(), name="monitoring-scheduler")

    async def stop(self) -> None:
        if self._task:
            self._task.cancel()
            with suppress(asyncio.CancelledError):
                await self._task
            self._task = None

    async def _run(self) -> None:
        while True:
            await asyncio.sleep(300)

    async def evaluate_once(self) -> dict:
        async with self.pool.acquire() as conn:
            rows = await conn.fetch(
                """SELECT severity, source, title, COUNT(*) AS occurrences,
                          MAX(created_at) AS last_seen_at
                     FROM agent.incidents
                    WHERE resolved_at IS NULL
                    GROUP BY severity, source, title
                    ORDER BY last_seen_at DESC"""
            )
        return {
            "mode": "monitoring_only",
            "sent": False,
            "incidents": [dict(row) for row in rows],
        }
