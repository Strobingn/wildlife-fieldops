-- Add to your existing schema.sql (append at the end, safe to run)

-- Compliance: Extend techs with license tracking
alter table public.techs add column if not exists license_number text;
alter table public.techs add column if not exists license_type text;
alter table public.techs add column if not exists license_expiry date;
alter table public.techs add column if not exists license_state text;
alter table public.techs add column if not exists license_notes text;

-- Reviews / Marketing automation table
create table if not exists public.reviews (
  id uuid primary key default gen_random_uuid(),
  job_id uuid references public.jobs(id) on delete cascade,
  customer_id uuid references public.customers(id) on delete set null,
  rating integer check (rating between 1 and 5),
  review_text text,
  source text default 'manual', -- manual, auto, google, etc.
  requested_at timestamptz,
  completed_at timestamptz,
  created_at timestamptz not null default now()
);

create index if not exists idx_reviews_job_id on public.reviews(job_id);
create index if not exists idx_reviews_rating on public.reviews(rating);

-- Trigger for reviews on jobs close (example)
-- You can extend the existing log_change or create specific trigger

-- Add to RLS if needed (example)
alter table public.reviews enable row level security;
create policy if not exists "reviews_select" on public.reviews for select using (true);
create policy if not exists "reviews_insert" on public.reviews for insert with check (true);

-- Analytics views already exist (job_stats, species_stats, tech_stats, weekly_revenue)
-- Use them directly in Dashboard or new analytics Edge Function

-- IoT / Smart trap webhook stub table (future)
create table if not exists public.iot_events (
  id uuid primary key default gen_random_uuid(),
  device_id text,
  event_type text,
  payload jsonb,
  created_at timestamptz not null default now()
);

create index if not exists idx_iot_events_device on public.iot_events(device_id);
create index if not exists idx_iot_events_created on public.iot_events(created_at desc);