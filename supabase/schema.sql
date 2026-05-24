-- Wildlife FieldOps Schema v3
-- Run this in Supabase SQL Editor (completely idempotent / safe to re-run)

create extension if not exists pgcrypto;

-- ============================================
-- TECHS
-- ============================================
create table if not exists techs (
  id uuid primary key default gen_random_uuid(),
  name text not null,
  phone text,
  role text,
  created_at timestamp with time zone default now()
);

alter table techs enable row level security;

-- Drop existing policies so we can recreate them safely
do $$
begin
  if exists (select 1 from pg_policies where schemaname = 'public' and tablename = 'techs' and policyname = 'Allow all ops on techs') then
    drop policy "Allow all ops on techs" on techs;
  end if;
end $$;

create policy "Allow all ops on techs"
  on techs for all
  using (true) with check (true);

-- ============================================
-- JOBS
-- ============================================
create table if not exists jobs (
  id uuid primary key default gen_random_uuid(),
  customer text not null,
  phone text,
  email text,
  address text not null,
  town text,
  species text,
  status text default 'Active',
  assigned_tech text,
  notes text,
  ai_notes text,
  latitude text,
  longitude text,
  subtotal numeric default 0,
  tax_rate numeric default 0,
  tax_amount numeric default 0,
  grand_total numeric default 0,
  created_at timestamp with time zone default now()
);

alter table jobs enable row level security;

do $$
begin
  if exists (select 1 from pg_policies where schemaname = 'public' and tablename = 'jobs' and policyname = 'Allow all ops on jobs') then
    drop policy "Allow all ops on jobs" on jobs;
  end if;
end $$;

create policy "Allow all ops on jobs"
  on jobs for all
  using (true) with check (true);

-- ============================================
-- SERVICES
-- ============================================
create table if not exists services (
  id uuid primary key default gen_random_uuid(),
  job_id uuid references jobs(id) on delete cascade not null,
  service text not null,
  qty numeric default 1,
  unit_price numeric default 0,
  total numeric default 0,
  created_at timestamp with time zone default now()
);

alter table services enable row level security;

do $$
begin
  if exists (select 1 from pg_policies where schemaname = 'public' and tablename = 'services' and policyname = 'Allow all ops on services') then
    drop policy "Allow all ops on services" on services;
  end if;
end $$;

create policy "Allow all ops on services"
  on services for all
  using (true) with check (true);

-- ============================================
-- INSPECTIONS
-- ============================================
create table if not exists inspections (
  id uuid primary key default gen_random_uuid(),
  job_id uuid references jobs(id) on delete cascade not null,
  inspection_type text,
  notes text,
  created_at timestamp with time zone default now()
);

alter table inspections enable row level security;

do $$
begin
  if exists (select 1 from pg_policies where schemaname = 'public' and tablename = 'inspections' and policyname = 'Allow all ops on inspections') then
    drop policy "Allow all ops on inspections" on inspections;
  end if;
end $$;

create policy "Allow all ops on inspections"
  on inspections for all
  using (true) with check (true);

-- ============================================
-- PHOTOS
-- ============================================
create table if not exists photos (
  id uuid primary key default gen_random_uuid(),
  job_id uuid references jobs(id) on delete cascade not null,
  image_url text,
  tag text,
  notes text,
  created_at timestamp with time zone default now()
);

alter table photos enable row level security;

do $$
begin
  if exists (select 1 from pg_policies where schemaname = 'public' and tablename = 'photos' and policyname = 'Allow all ops on photos') then
    drop policy "Allow all ops on photos" on photos;
  end if;
end $$;

create policy "Allow all ops on photos"
  on photos for all
  using (true) with check (true);

-- ============================================
-- PDF DOCUMENTS
-- ============================================
create table if not exists pdf_documents (
  id uuid primary key default gen_random_uuid(),
  job_id uuid references jobs(id) on delete cascade not null,
  type text,
  file_path text,
  public_url text,
  created_at timestamp with time zone default now()
);

alter table pdf_documents enable row level security;

do $$
begin
  if exists (select 1 from pg_policies where schemaname = 'public' and tablename = 'pdf_documents' and policyname = 'Allow all ops on pdf_documents') then
    drop policy "Allow all ops on pdf_documents" on pdf_documents;
  end if;
end $$;

create policy "Allow all ops on pdf_documents"
  on pdf_documents for all
  using (true) with check (true);

-- ============================================
-- ANALYTICS / METRICS VIEWS
-- ============================================
create or replace view job_stats as
select
  count(*) filter (where status != 'Closed') as active_jobs,
  count(*) filter (where status = 'Closed') as closed_jobs,
  count(*) as total_jobs,
  coalesce(sum(grand_total), 0) as total_revenue,
  coalesce(sum(grand_total) filter (where created_at >= date_trunc('month', now())), 0) as month_revenue,
  coalesce(sum(grand_total) filter (where created_at >= date_trunc('week', now())), 0) as week_revenue
from jobs;

create or replace view tech_stats as
select
  t.id,
  t.name,
  count(j.id) filter (where j.status != 'Closed') as active_jobs,
  count(j.id) as total_jobs,
  coalesce(sum(j.grand_total), 0) as total_revenue
from techs t
left join jobs j on j.assigned_tech = t.name
group by t.id, t.name;

create or replace view species_stats as
select
  species,
  count(*) as job_count,
  coalesce(sum(grand_total), 0) as total_revenue
from jobs
where species is not null and species != ''
group by species
order by job_count desc;
