from __future__ import annotations
import os
from functools import lru_cache
from pathlib import Path
from dotenv import load_dotenv

ENV_PATH = Path('/home/azim/agent/.env')
RUNTIME_ENV_PATH = Path('/home/azim/secure-env-backup/runtime-services.env')


class Settings:
    """Environment + authority configuration."""

    def __init__(self):
        load_dotenv(ENV_PATH, override=False)
        if RUNTIME_ENV_PATH.exists():
            load_dotenv(RUNTIME_ENV_PATH, override=True)

        pg_template = os.getenv('PG_DSN_TEMPLATE', '')
        self.database_url: str = (
            os.getenv('DATABASE_URL')
            or pg_template.replace('__HOST__', '127.0.0.1')
        )

        ollama_template = os.getenv('OLLAMA_URL_TEMPLATE', 'http://127.0.0.1:11434')
        self.ollama_url: str = ollama_template.replace('__HOST__', '127.0.0.1')

        self.ollama_model: str = os.getenv('OLLAMA_MODEL', 'qwen2.5:3b')
        self.redis_url: str = os.getenv('REDIS_URL', 'redis://127.0.0.1:6379/3')
        self.fazle_core_url: str = os.getenv('FAZLE_CORE_URL', 'http://127.0.0.1:8200')
        self.fazle_internal_key: str = os.getenv('FAZLE_INTERNAL_KEY', '')
        self.owner_phone: str = os.getenv('OWNER_PHONE', '')
        self.admin_phones: list[str] = [
            p.strip() for p in os.getenv('ADMIN_PHONES', '').split(',') if p.strip()
        ]
        self.dry_run: bool = os.getenv('DRY_RUN', 'true').lower() == 'true'
        self.internet_allowed: bool = os.getenv('INTERNET_ALLOWED', 'false').lower() == 'true'
        self.tier3_llm_allowed: bool = os.getenv('TIER3_LLM_ALLOWED', 'false').lower() == 'true'


@lru_cache(maxsize=1)
def get_settings() -> Settings:
    return Settings()
