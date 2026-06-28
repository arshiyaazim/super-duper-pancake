from __future__ import annotations
import asyncio
import logging
import os
import asyncpg
from contextlib import asynccontextmanager
from fastapi import FastAPI

from system_agent.config import get_settings
from system_agent.guardian.monitor import Guardian
from system_agent.scheduler.proactive import ProactiveScheduler

log = logging.getLogger("system_agent.main")

_pool: asyncpg.Pool | None = None
_guardian: Guardian | None = None
_scheduler: ProactiveScheduler | None = None


@asynccontextmanager
async def lifespan(app: FastAPI):
    global _pool, _guardian, _scheduler
    settings = get_settings()

    _pool = await asyncpg.create_pool(settings.database_url, min_size=2, max_size=10)

    _guardian = Guardian(_pool, settings)
    _guardian.start()

    _scheduler = ProactiveScheduler(_pool, settings)
    _scheduler.start()

    log.info("system_agent monitoring-only mode started")
    yield

    if _guardian:
        await _guardian.stop()
    if _scheduler:
        await _scheduler.stop()
    if _pool:
        await _pool.close()


app = FastAPI(title="Fazle System Agent", lifespan=lifespan)


@app.get("/health")
async def health():
    settings = get_settings()
    open_incidents = 0
    guardian_ok = _guardian is not None
    if _pool:
        try:
            async with _pool.acquire() as conn:
                row = await conn.fetchrow(
                    "SELECT COUNT(*) AS n FROM agent.incidents WHERE resolved_at IS NULL"
                )
                open_incidents = row["n"] if row else 0
        except Exception:
            pass
    return {
        "status": "ok",
        "mode": "monitoring_only",
        "dry_run": True,
        "open_incidents": open_incidents,
        "guardian": guardian_ok,
        "guardian_state": _guardian.state if _guardian else {},
    }


@app.get("/whoami/{phone}")
async def whoami(phone: str):
    if _pool is None:
        return {"error": "pool not ready"}
    async with _pool.acquire() as conn:
        row = await conn.fetchrow(
            """SELECT COALESCE(keep_reason, keep_tier, 'contact') AS role,
                      COALESCE(display_name, '') AS name
                 FROM wbom_contacts
                WHERE whatsapp_number = $1
                LIMIT 1""",
            phone,
        )
    if not row:
        return {"phone": phone, "role": "unknown", "name": None}
    return {"phone": phone, "role": row["role"], "name": row["name"]}


@app.post("/admin/proactive/run")
async def proactive_run():
    if _scheduler is None:
        return {"error": "scheduler not ready"}
    results = await _scheduler.evaluate_once()
    return {"status": "ok", "results": results}


@app.post("/admin/inbox")
@app.get("/admin/inbox")
async def inbox_redirect():
    return {"status": "monitoring_only", "reply_generated": False, "sent": False}
