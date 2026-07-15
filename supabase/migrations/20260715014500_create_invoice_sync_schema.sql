create table if not exists public.invoices (
  id text primary key,
  invoice_number text not null default '',
  job_id text not null default '',
  customer_id text not null default '',
  customer_name text not null default '',
  customer_email text not null default '',
  customer_address text not null default '',
  issue_date bigint not null,
  due_date bigint not null,
  status text not null default 'DRAFT',
  subtotal double precision not null default 0,
  tax_rate double precision not null default 0,
  tax_amount double precision not null default 0,
  discount_amount double precision not null default 0,
  total_amount double precision not null default 0,
  amount_paid double precision not null default 0,
  balance_due double precision not null default 0,
  line_items text not null default '[]',
  notes text not null default '',
  terms text not null default '',
  technician_signature text not null default '',
  customer_signature text not null default '',
  pdf_path text not null default '',
  created_at bigint not null,
  updated_at bigint not null
);

create index if not exists invoices_job_id_idx on public.invoices(job_id);
create index if not exists invoices_customer_id_idx on public.invoices(customer_id);
create index if not exists invoices_status_idx on public.invoices(status);
create index if not exists invoices_updated_at_idx on public.invoices(updated_at desc);

alter table public.invoices enable row level security;

drop policy if exists anon_select_invoices on public.invoices;
drop policy if exists anon_insert_invoices on public.invoices;
drop policy if exists anon_update_invoices on public.invoices;
drop policy if exists anon_delete_invoices on public.invoices;
drop policy if exists authenticated_invoices_all on public.invoices;

create policy anon_select_invoices on public.invoices for select to anon using (true);
create policy anon_insert_invoices on public.invoices for insert to anon with check (true);
create policy anon_update_invoices on public.invoices for update to anon using (true) with check (true);
create policy anon_delete_invoices on public.invoices for delete to anon using (true);
create policy authenticated_invoices_all on public.invoices for all to authenticated using (true) with check (true);

grant select, insert, update, delete on public.invoices to anon, authenticated;
