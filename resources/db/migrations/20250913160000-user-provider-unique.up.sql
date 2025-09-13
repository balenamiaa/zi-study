-- Ensure unique provider+provider_id pairs when provider_id is present
CREATE UNIQUE INDEX IF NOT EXISTS app_user_provider_provider_id_uq
  ON app_user (provider, provider_id)
  WHERE provider_id IS NOT NULL;

