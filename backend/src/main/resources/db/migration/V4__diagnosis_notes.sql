-- NeuroGuard V4: free-text clinical notes on a diagnosis (optional, so existing rows stay valid).
ALTER TABLE diagnosis ADD COLUMN notes TEXT NULL;
