-- Wildlife Whisperer FieldOps - Supabase schema.sql
-- Drop this file in: supabase/schema.sql
-- Safe to re-run. Designed for the current repo's src/main.js data calls.

create extension if not exists pgcrypto;

-- =========================
-- Helpers
-- =========================

create or replace function public.set_updated_at()
returns trigger
language plpgsql
as $$
begin
  new.updated_at = now();
  return new;
end;
$$;

-- =========================
-- Core tables
-- =========================

create table if not exists public.techs (
  id uuid primary key default gen_random_uuid(),
  name text not null,
  phone text,
  role text,
  active boolean not null default true,
  notes text,
  created_at timestamptz not null default now(),
  updated_at timestamptz not null default now()
);

create table if not exists public.jobs (
  id uuid primary key default gen_random_uuid(),

  customer text not null,
  phone text,
  email text,
  address text not null,
  town text,

  species text,
  status text not null default 'Active',
  assigned_tech text,

  notes text,
  ai_notes text,

  latitude text,
  longitude text,

  estimate numeric(12,2) not null default 0,
  subtotal numeric(12,2) not null default 0,
  tax_rate numeric(6,3) not null default 0,
  tax_amount numeric(12,2) not null default 0,
  grand_total numeric(12,2) not null default 0,

  scheduled_start timestamptz,
  scheduled_end timestamptz,
  completed_at timestamptz,

  created_at timestamptz not null default now(),
  updated_at timestamptz not null default now(),

  constraint jobs_status_check check (
    status in ('Active', 'Scheduled', 'In Progress', 'Needs Follow-up', 'Closed', 'Cancelled')
  )
);

create table if not exists public.services (
  id uuid primary key default gen_random_uuid(),
  job_id uuid not null references public.jobs(id) on delete cascade,

  service text not null,
  qty numeric(10,2) not null default 1,
  unit_price numeric(12,2) not null default 0,
  total numeric(12,2) not null default 0,
  notes text,

  created_at timestamptz not null default now(),
  updated_at timestamptz not null default now()
);

create table if not exists public.inspections (
  id uuid primary key default gen_random_uuid(),
  job_id uuid not null references public.jobs(id) on delete cascade,

  inspection_type text not null,
  notes text,
  findings jsonb not null default '{}'::jsonb,

  created_at timestamptz not null default now(),
  updated_at timestamptz not null default now()
);

create table if not exists public.photos (
  id uuid primary key default gen_random_uuid(),
  job_id uuid not null references public.jobs(id) on delete cascade,

  image_url text,
  storage_path text,
  tag text,
  notes text,

  created_at timestamptz not null default now(),
  updated_at timestamptz not null default now()
);

create table if not exists public.pdf_documents (
  id uuid primary key default gen_random_uuid(),
  job_id uuid not null references public.jobs(id) on delete cascade,

  type text,
  file_path text,
  public_url text,

  created_at timestamptz not null default now(),
  updated_at timestamptz not null default now()
);

-- Future-proof scheduling/calendar table.
create table if not exists public.appointments (
  id uuid primary key default gen_random_uuid(),
  job_id uuid references public.jobs(id) on delete cascade,

  title text not null,
  starts_at timestamptz not null,
  ends_at timestamptz,
  location text,
  assigned_tech text,
  status text not null default 'Scheduled',
  notes text,

  created_at timestamptz not null default now(),
  updated_at timestamptz not null default now(),

  constraint appointments_status_check check (
    status in ('Scheduled', 'Completed', 'Cancelled', 'No Show')
  )
);

-- Offline sync / audit-friendly queue table.
create table if not exists public.sync_events (
  id uuid primary key default gen_random_uuid(),
  device_id text,
  entity_type text not null,
  entity_id uuid,
  action text not null,
  payload jsonb not null default '{}'::jsonb,
  synced_at timestamptz,
  created_at timestamptz not null default now()
);

-- =========================
-- Compatibility migrations
-- These keep older installs from breaking.
-- =========================

alter table public.jobs add column if not exists estimate numeric(12,2) not null default 0;
alter table public.jobs add column if not exists subtotal numeric(12,2) not null default 0;
alter table public.jobs add column if not exists tax_rate numeric(6,3) not null default 0;
alter table public.jobs add column if not exists tax_amount numeric(12,2) not null default 0;
alter table public.jobs add column if not exists grand_total numeric(12,2) not null default 0;
alter table public.jobs add column if not exists ai_notes text;
alter table public.jobs add column if not exists scheduled_start timestamptz;
alter table public.jobs add column if not exists scheduled_end timestamptz;
alter table public.jobs add column if not exists completed_at timestamptz;
alter table public.jobs add column if not exists updated_at timestamptz not null default now();

alter table public.techs add column if not exists active boolean not null default true;
alter table public.techs add column if not exists notes text;
alter table public.techs add column if not exists updated_at timestamptz not null default now();

alter table public.services add column if not exists notes text;
alter table public.services add column if not exists updated_at timestamptz not null default now();

alter table public.inspections add column if not exists findings jsonb not null default '{}'::jsonb;
alter table public.inspections add column if not exists updated_at timestamptz not null default now();

alter table public.photos add column if not exists storage_path text;
alter table public.photos add column if not exists updated_at timestamptz not null default now();

alter table public.pdf_documents add column if not exists updated_at timestamptz not null default now();

-- =========================
-- Indexes
-- =========================

create index if not exists idx_jobs_created_at on public.jobs(created_at desc);
create index if not exists idx_jobs_status on public.jobs(status);
create index if not exists idx_jobs_species on public.jobs(species);
create index if not exists idx_jobs_assigned_tech on public.jobs(assigned_tech);
create index if not exists idx_jobs_scheduled_start on public.jobs(scheduled_start);

create index if not exists idx_services_job_id on public.services(job_id);
create index if not exists idx_inspections_job_id on public.inspections(job_id);
create index if not exists idx_photos_job_id on public.photos(job_id);
create index if not exists idx_pdf_documents_job_id on public.pdf_documents(job_id);
create index if not exists idx_appointments_job_id on public.appointments(job_id);
create index if not exists idx_appointments_starts_at on public.appointments(starts_at);

-- =========================
-- Updated-at triggers
-- =========================

drop trigger if exists set_techs_updated_at on public.techs;
create trigger set_techs_updated_at
before update on public.techs
for each row execute function public.set_updated_at();

drop trigger if exists set_jobs_updated_at on public.jobs;
create trigger set_jobs_updated_at
before update on public.jobs
for each row execute function public.set_updated_at();

drop trigger if exists set_services_updated_at on public.services;
create trigger set_services_updated_at
before update on public.services
for each row execute function public.set_updated_at();

drop trigger if exists set_inspections_updated_at on public.inspections;
create trigger set_inspections_updated_at
before update on public.inspections
for each row execute function public.set_updated_at();

drop trigger if exists set_photos_updated_at on public.photos;
create trigger set_photos_updated_at
before update on public.photos
for each row execute function public.set_updated_at();

drop trigger if exists set_pdf_documents_updated_at on public.pdf_documents;
create trigger set_pdf_documents_updated_at
before update on public.pdf_documents
for each row execute function public.set_updated_at();

drop trigger if exists set_appointments_updated_at on public.appointments;
create trigger set_appointments_updated_at
before update on public.appointments
for each row execute function public.set_updated_at();

-- =========================
-- Useful dashboard views
-- =========================

create or replace view public.job_stats as
select
  count(*)::int as total_jobs,
  count(*) filter (where status <> 'Closed')::int as active_jobs,
  count(*) filter (where status = 'Closed')::int as closed_jobs,
  coalesce(sum(grand_total), 0)::numeric(12,2) as quoted_value,
  coalesce(sum(tax_amount), 0)::numeric(12,2) as tax_total
from public.jobs;

create or replace view public.tech_stats as
select
  coalesce(assigned_tech, 'Unassigned') as assigned_tech,
  count(*)::int as job_count,
  count(*) filter (where status <> 'Closed')::int as active_job_count,
  coalesce(sum(grand_total), 0)::numeric(12,2) as quoted_value
from public.jobs
group by coalesce(assigned_tech, 'Unassigned');

create or replace view public.species_stats as
select
  coalesce(species, 'Unknown') as species,
  count(*)::int as job_count,
  coalesce(sum(grand_total), 0)::numeric(12,2) as quoted_value
from public.jobs
group by coalesce(species, 'Unknown');

-- =========================
-- Realtime
-- =========================

alter publication supabase_realtime add table public.jobs;
alter publication supabase_realtime add table public.techs;
alter publication supabase_realtime add table public.services;
alter publication supabase_realtime add table public.inspections;
alter publication supabase_realtime add table public.photos;
alter publication supabase_realtime add table public.appointments;

-- =========================
-- RLS
-- Current app uses the anon client without login screens.
-- These policies allow the app to work immediately.
-- For production, replace with authenticated user policies.
-- =========================

alter table public.techs enable row level security;
alter table public.jobs enable row level security;
alter table public.services enable row level security;
alter table public.inspections enable row level security;
alter table public.photos enable row level security;
alter table public.pdf_documents enable row level security;
alter table public.appointments enable row level security;
alter table public.sync_events enable row level security;

drop policy if exists "anon_select_techs" on public.techs;
drop policy if exists "anon_insert_techs" on public.techs;
drop policy if exists "anon_update_techs" on public.techs;
drop policy if exists "anon_delete_techs" on public.techs;
create policy "anon_select_techs" on public.techs for select to anon using (true);
create policy "anon_insert_techs" on public.techs for insert to anon with check (true);
create policy "anon_update_techs" on public.techs for update to anon using (true) with check (true);
create policy "anon_delete_techs" on public.techs for delete to anon using (true);

drop policy if exists "anon_select_jobs" on public.jobs;
drop policy if exists "anon_insert_jobs" on public.jobs;
drop policy if exists "anon_update_jobs" on public.jobs;
drop policy if exists "anon_delete_jobs" on public.jobs;
create policy "anon_select_jobs" on public.jobs for select to anon using (true);
create policy "anon_insert_jobs" on public.jobs for insert to anon with check (true);
create policy "anon_update_jobs" on public.jobs for update to anon using (true) with check (true);
create policy "anon_delete_jobs" on public.jobs for delete to anon using (true);

drop policy if exists "anon_select_services" on public.services;
drop policy if exists "anon_insert_services" on public.services;
drop policy if exists "anon_update_services" on public.services;
drop policy if exists "anon_delete_services" on public.services;
create policy "anon_select_services" on public.services for select to anon using (true);
create policy "anon_insert_services" on public.services for insert to anon with check (true);
create policy "anon_update_services" on public.services for update to anon using (true) with check (true);
create policy "anon_delete_services" on public.services for delete to anon using (true);

drop policy if exists "anon_select_inspections" on public.inspections;
drop policy if exists "anon_insert_inspections" on public.inspections;
drop policy if exists "anon_update_inspections" on public.inspections;
drop policy if exists "anon_delete_inspections" on public.inspections;
create policy "anon_select_inspections" on public.inspections for select to anon using (true);
create policy "anon_insert_inspections" on public.inspections for insert to anon with check (true);
create policy "anon_update_inspections" on public.inspections for update to anon using (true) with check (true);
create policy "anon_delete_inspections" on public.inspections for delete to anon using (true);

drop policy if exists "anon_select_photos" on public.photos;
drop policy if exists "anon_insert_photos" on public.photos;
drop policy if exists "anon_update_photos" on public.photos;
drop policy if exists "anon_delete_photos" on public.photos;
create policy "anon_select_photos" on public.photos for select to anon using (true);
create policy "anon_insert_photos" on public.photos for insert to anon with check (true);
create policy "anon_update_photos" on public.photos for update to anon using (true) with check (true);
create policy "anon_delete_photos" on public.photos for delete to anon using (true);

drop policy if exists "anon_select_pdf_documents" on public.pdf_documents;
drop policy if exists "anon_insert_pdf_documents" on public.pdf_documents;
drop policy if exists "anon_update_pdf_documents" on public.pdf_documents;
drop policy if exists "anon_delete_pdf_documents" on public.pdf_documents;
create policy "anon_select_pdf_documents" on public.pdf_documents for select to anon using (true);
create policy "anon_insert_pdf_documents" on public.pdf_documents for insert to anon with check (true);
create policy "anon_update_pdf_documents" on public.pdf_documents for update to anon using (true) with check (true);
create policy "anon_delete_pdf_documents" on public.pdf_documents for delete to anon using (true);

drop policy if exists "anon_select_appointments" on public.appointments;
drop policy if exists "anon_insert_appointments" on public.appointments;
drop policy if exists "anon_update_appointments" on public.appointments;
drop policy if exists "anon_delete_appointments" on public.appointments;
create policy "anon_select_appointments" on public.appointments for select to anon using (true);
create policy "anon_insert_appointments" on public.appointments for insert to anon with check (true);
create policy "anon_update_appointments" on public.appointments for update to anon using (true) with check (true);
create policy "anon_delete_appointments" on public.appointments for delete to anon using (true);

drop policy if exists "anon_select_sync_events" on public.sync_events;
drop policy if exists "anon_insert_sync_events" on public.sync_events;
drop policy if exists "anon_update_sync_events" on public.sync_events;
drop policy if exists "anon_delete_sync_events" on public.sync_events;
create policy "anon_select_sync_events" on public.sync_events for select to anon using (true);
create policy "anon_insert_sync_events" on public.sync_events for insert to anon with check (true);
create policy "anon_update_sync_events" on public.sync_events for update to anon using (true) with check (true);
create policy "anon_delete_sync_events" on public.sync_events for delete to anon using (true);
