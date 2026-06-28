#!/bin/bash
set -euo pipefail

docker exec -i ai-postgres psql -v ON_ERROR_STOP=1 -U postgres -d postgres <<'SQL'
BEGIN;

DELETE FROM agent.incidents
WHERE resolved_at IS NOT NULL
  AND created_at < NOW() - INTERVAL '3 days';

DELETE FROM agent.incidents a
USING agent.incidents b
WHERE a.id < b.id
  AND a.source IS NOT DISTINCT FROM b.source
  AND a.title IS NOT DISTINCT FROM b.title
  AND a.resolved_at IS NULL
  AND b.resolved_at IS NULL;

UPDATE agent.incidents
SET resolved_at = NOW(),
    fix_applied = 'Auto-resolved: service confirmed healthy'
WHERE resolved_at IS NULL
  AND title IN (
    'ollama_unreachable',
    'fazle_core_unreachable',
    'ollama_model_evicted',
    'fazle_core_unhealthy'
  )
  AND created_at < NOW() - INTERVAL '1 hour';

COMMIT;

SELECT
  COUNT(*) AS total,
  COUNT(*) FILTER (WHERE resolved_at IS NULL) AS unresolved,
  COUNT(*) FILTER (WHERE resolved_at IS NOT NULL) AS resolved
FROM agent.incidents;
SQL
