-- Emergency migration: add missing columns to existing jobs table
-- Run this FIRST if you get "column does not exist" errors

-- Drop views that depend on the missing columns (safe to re-run)
drop view if exists job_stats;
drop view if exists tech_stats;
drop view if exists species_stats;

-- Add missing columns to jobs table (will skip if already exists)
alter table jobs add column if not exists subtotal numeric default 0;
alter table jobs add column if not exists tax_rate numeric default 0;
alter table jobs add column if not exists tax_amount numeric default 0;
alter table jobs add column if not exists grand_total numeric default 0;
alter table jobs add column if not exists ai_notes text;

-- Verify columns exist (should return 1 row per column)
select column_name, data_type 
from information_schema.columns 
where table_name = 'jobs' 
  and column_name in ('subtotal','tax_rate','tax_amount','grand_total','ai_notes');
