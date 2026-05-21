create table if not exists techs (
  id uuid primary key default gen_random_uuid(),
  name text,
  phone text,
  role text,
  created_at timestamp default now()
);

create table if not exists jobs (
  id uuid primary key default gen_random_uuid(),
  customer text,
  phone text,
  email text,
  address text,
  town text,
  species text,
  status text,
  assigned_tech text,
  estimate numeric default 0,
  tax_rate numeric default 0,
  tax_amount numeric default 0,
  grand_total numeric default 0,
  notes text,
  latitude text,
  longitude text,
  created_at timestamp default now()
);

create table if not exists services (
  id uuid primary key default gen_random_uuid(),
  job_id uuid references jobs(id) on delete cascade,
  service text,
  qty numeric,
  unit_price numeric,
  total numeric,
  created_at timestamp default now()
);

create table if not exists inspections (
  id uuid primary key default gen_random_uuid(),
  job_id uuid references jobs(id) on delete cascade,
  inspection_type text,
  notes text,
  created_at timestamp default now()
);

create table if not exists photos (
  id uuid primary key default gen_random_uuid(),
  job_id uuid references jobs(id) on delete cascade,
  image_url text,
  tag text,
  notes text,
  created_at timestamp default now()
);
