create extension if not exists "pgcrypto";

create table if not exists profiles (
  id uuid primary key references auth.users(id) on delete cascade,
  email text unique,
  name text,
  phone text,
  role text not null default 'technician',
  active boolean not null default true,
  created_at timestamptz default now()
);

create table if not exists properties (
  id uuid primary key default gen_random_uuid(),
  address text not null,
  town text,
  lat double precision,
  lng double precision,
  notes text,
  created_by uuid references profiles(id),
  created_at timestamptz default now()
);

create table if not exists jobs (
  id uuid primary key default gen_random_uuid(),
  property_id uuid references properties(id) on delete set null,
  customer_name text not null,
  customer_phone text,
  customer_email text,
  address text,
  town text,
  species text not null,
  title text not null,
  scope text,
  status text not null default 'Active',
  priority text default 'Normal',
  assigned_to uuid references profiles(id),
  created_by uuid references profiles(id),
  created_at timestamptz default now(),
  updated_at timestamptz default now()
);

create table if not exists visits (
  id uuid primary key default gen_random_uuid(),
  job_id uuid references jobs(id) on delete cascade,
  technician_id uuid references profiles(id),
  visit_type text not null,
  notes text,
  animals_removed integer default 0,
  visit_at timestamptz default now(),
  created_at timestamptz default now()
);

create table if not exists repairs (
  id uuid primary key default gen_random_uuid(),
  job_id uuid references jobs(id) on delete cascade,
  location text,
  status text default 'Open',
  severity text default 'Medium',
  materials text,
  notes text,
  warranty_eligible boolean default false,
  created_at timestamptz default now()
);

create table if not exists warranties (
  id uuid primary key default gen_random_uuid(),
  job_id uuid references jobs(id) on delete cascade,
  terms text,
  covered_areas text,
  exclusions text,
  start_date date,
  end_date date,
  created_at timestamptz default now()
);

create table if not exists estimates (
  id uuid primary key default gen_random_uuid(),
  job_id uuid references jobs(id) on delete set null,
  customer_name text,
  customer_email text,
  species text,
  severity text,
  total numeric,
  body text,
  emailed boolean default false,
  created_by uuid references profiles(id),
  created_at timestamptz default now()
);

create table if not exists job_photos (
  id uuid primary key default gen_random_uuid(),
  job_id uuid references jobs(id) on delete cascade,
  path text,
  public_url text,
  tag text,
  notes text,
  uploaded_by uuid references profiles(id),
  created_at timestamptz default now()
);

create table if not exists ai_plans (
  id uuid primary key default gen_random_uuid(),
  job_id uuid references jobs(id) on delete set null,
  species text,
  structure_area text,
  season text,
  field_notes text,
  plan text,
  created_by uuid references profiles(id),
  created_at timestamptz default now()
);

alter table profiles enable row level security;
alter table properties enable row level security;
alter table jobs enable row level security;
alter table visits enable row level security;
alter table repairs enable row level security;
alter table warranties enable row level security;
alter table estimates enable row level security;
alter table job_photos enable row level security;
alter table ai_plans enable row level security;

drop policy if exists "authenticated profiles access" on profiles;
drop policy if exists "authenticated properties access" on properties;
drop policy if exists "authenticated jobs access" on jobs;
drop policy if exists "authenticated visits access" on visits;
drop policy if exists "authenticated repairs access" on repairs;
drop policy if exists "authenticated warranties access" on warranties;
drop policy if exists "authenticated estimates access" on estimates;
drop policy if exists "authenticated photos access" on job_photos;
drop policy if exists "authenticated ai plans access" on ai_plans;

create policy "authenticated profiles access"
on profiles
for all
using (auth.role() = 'authenticated')
with check (auth.role() = 'authenticated');

create policy "authenticated properties access"
on properties
for all
using (auth.role() = 'authenticated')
with check (auth.role() = 'authenticated');

create policy "authenticated jobs access"
on jobs
for all
using (auth.role() = 'authenticated')
with check (auth.role() = 'authenticated');

create policy "authenticated visits access"
on visits
for all
using (auth.role() = 'authenticated')
with check (auth.role() = 'authenticated');

create policy "authenticated repairs access"
on repairs
for all
using (auth.role() = 'authenticated')
with check (auth.role() = 'authenticated');

create policy "authenticated warranties access"
on warranties
for all
using (auth.role() = 'authenticated')
with check (auth.role() = 'authenticated');

create policy "authenticated estimates access"
on estimates
for all
using (auth.role() = 'authenticated')
with check (auth.role() = 'authenticated');

create policy "authenticated photos access"
on job_photos
for all
using (auth.role() = 'authenticated')
with check (auth.role() = 'authenticated');

create policy "authenticated ai plans access"
on ai_plans
for all
using (auth.role() = 'authenticated')
with check (auth.role() = 'authenticated');

insert into storage.buckets (id, name, public)
values ('job-photos', 'job-photos', true)
on conflict (id) do nothing;

drop policy if exists "authenticated storage read job photos" on storage.objects;
drop policy if exists "authenticated storage insert job photos" on storage.objects;
drop policy if exists "authenticated storage update job photos" on storage.objects;
drop policy if exists "authenticated storage delete job photos" on storage.objects;

create policy "authenticated storage read job photos"
on storage.objects
for select
using (
  bucket_id = 'job-photos'
  and auth.role() = 'authenticated'
);

create policy "authenticated storage insert job photos"
on storage.objects
for insert
with check (
  bucket_id = 'job-photos'
  and auth.role() = 'authenticated'
);

create policy "authenticated storage update job photos"
on storage.objects
for update
using (
  bucket_id = 'job-photos'
  and auth.role() = 'authenticated'
);

create policy "authenticated storage delete job photos"
on storage.objects
for delete
using (
  bucket_id = 'job-photos'
  and auth.role() = 'authenticated'
);
