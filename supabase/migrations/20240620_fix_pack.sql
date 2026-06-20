-- ═══════════════════════════════════════════════════════════════════════════════
-- Wildlife FieldOps Fix Pack
-- Generated: June 20, 2026
-- Run this in Supabase SQL Editor to enable inspections + photos
-- ═══════════════════════════════════════════════════════════════════════════════

-- ─── PHOTOS TABLE ──────────────────────────────────────────────────────────────

create table if not exists photos (
    id text primary key,
    job_id text not null,
    storage_path text,
    public_url text,
    tag text default '',
    notes text default '',
    file_size integer,
    content_type text,
    created_at timestamptz default now()
);

-- Enable RLS on photos
alter table photos enable row level security;

-- Allow authenticated users to access photos (adjust for your auth model)
create policy if not exists "Allow authenticated access to photos"
    on photos for all
    to authenticated
    using (true)
    with check (true);

-- Index for fast job-based lookups
create index if not exists idx_photos_job_id on photos(job_id);

-- ─── INSPECTIONS TABLE ─────────────────────────────────────────────────────────

create table if not exists inspections (
    id text primary key,
    customer_name text not null,
    phone text,
    address text,
    town text,
    species text,
    priority text default 'Normal',
    status text default 'scheduled',
    scheduled_date timestamptz,
    notes text,
    job_id text references jobs(id),
    created_at timestamptz default now(),
    updated_at timestamptz default now()
);

-- Enable RLS on inspections
alter table inspections enable row level security;

-- Allow authenticated users to access inspections
create policy if not exists "Allow authenticated access to inspections"
    on inspections for all
    to authenticated
    using (true)
    with check (true);

-- Indexes for common queries
create index if not exists idx_inspections_status on inspections(status);
create index if not exists idx_inspections_scheduled_date on inspections(scheduled_date);
create index if not exists idx_inspections_job_id on inspections(job_id);

-- ─── STORAGE BUCKET SETUP ──────────────────────────────────────────────────────
-- NOTE: Create the "job-photos" bucket manually in Supabase Storage UI:
-- 1. Go to Storage → Buckets
-- 2. Click "New Bucket"
-- 3. Name: job-photos
-- 4. Check "Public bucket" (for testing)
-- 5. Click "Save"
--
-- Then run this policy:

-- Allow authenticated uploads to job-photos bucket
create policy if not exists "Allow authenticated uploads"
    on storage.objects for insert
    to authenticated
    with check (bucket_id = 'job-photos');

create policy if not exists "Allow authenticated reads"
    on storage.objects for select
    to authenticated
    using (bucket_id = 'job-photos');

create policy if not exists "Allow authenticated deletes"
    on storage.objects for delete
    to authenticated
    using (bucket_id = 'job-photos');

-- ═══════════════════════════════════════════════════════════════════════════════
-- Verification Queries (run after setup to confirm everything is working)
-- ═══════════════════════════════════════════════════════════════════════════════

-- Check inspections table exists and has correct columns
select column_name, data_type, is_nullable
from information_schema.columns
where table_name = 'inspections'
order by ordinal_position;

-- Check photos table exists and has correct columns
select column_name, data_type, is_nullable
from information_schema.columns
where table_name = 'photos'
order by ordinal_position;
