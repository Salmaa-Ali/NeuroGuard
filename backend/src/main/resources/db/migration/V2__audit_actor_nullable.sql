-- Failed logins with an unknown email have no known actor, so actor_id must be nullable.
ALTER TABLE audit_log ALTER COLUMN actor_id DROP NOT NULL;
