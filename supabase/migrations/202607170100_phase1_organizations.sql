begin;

create extension if not exists pgcrypto;

create table if not exists public.organizations (
    id uuid primary key default gen_random_uuid(),
    name text not null check (char_length(trim(name)) between 2 and 120),
    owner_user_id uuid not null references auth.users(id) on delete cascade,
    created_at timestamptz not null default now(),
    updated_at timestamptz not null default now()
);

create table if not exists public.organization_members (
    organization_id uuid not null references public.organizations(id) on delete cascade,
    user_id uuid not null references auth.users(id) on delete cascade,
    role text not null default 'member' check (role in ('owner','admin','dispatcher','technician','member')),
    created_at timestamptz not null default now(),
    primary key (organization_id, user_id)
);

create index if not exists organization_members_user_id_idx
    on public.organization_members(user_id);

alter table public.organizations enable row level security;
alter table public.organization_members enable row level security;

create or replace function public.is_organization_member(target_organization_id uuid)
returns boolean
language sql
stable
security definer
set search_path = public
as $$
    select exists (
        select 1
        from public.organization_members om
        where om.organization_id = target_organization_id
          and om.user_id = auth.uid()
    );
$$;

revoke all on function public.is_organization_member(uuid) from public;
grant execute on function public.is_organization_member(uuid) to authenticated;

create or replace function public.create_organization(organization_name text)
returns public.organizations
language plpgsql
security definer
set search_path = public
as $$
declare
    created_org public.organizations;
begin
    if auth.uid() is null then
        raise exception 'Authentication required';
    end if;

    if char_length(trim(organization_name)) < 2 then
        raise exception 'Organization name is required';
    end if;

    insert into public.organizations(name, owner_user_id)
    values (trim(organization_name), auth.uid())
    returning * into created_org;

    insert into public.organization_members(organization_id, user_id, role)
    values (created_org.id, auth.uid(), 'owner');

    return created_org;
end;
$$;

grant execute on function public.create_organization(text) to authenticated;

create policy organizations_select_members
on public.organizations for select
to authenticated
using (public.is_organization_member(id));

create policy organizations_update_admins
on public.organizations for update
to authenticated
using (
    exists (
        select 1 from public.organization_members om
        where om.organization_id = id
          and om.user_id = auth.uid()
          and om.role in ('owner','admin')
    )
)
with check (
    exists (
        select 1 from public.organization_members om
        where om.organization_id = id
          and om.user_id = auth.uid()
          and om.role in ('owner','admin')
    )
);

create policy organization_members_select_members
on public.organization_members for select
to authenticated
using (public.is_organization_member(organization_id));

create policy organization_members_manage_admins
on public.organization_members for all
to authenticated
using (
    exists (
        select 1 from public.organization_members admin_membership
        where admin_membership.organization_id = organization_members.organization_id
          and admin_membership.user_id = auth.uid()
          and admin_membership.role in ('owner','admin')
    )
)
with check (
    exists (
        select 1 from public.organization_members admin_membership
        where admin_membership.organization_id = organization_members.organization_id
          and admin_membership.user_id = auth.uid()
          and admin_membership.role in ('owner','admin')
    )
);

-- Enforce tenant ownership on core business tables when they already exist.
do $$
declare
    table_name text;
begin
    foreach table_name in array array[
        'customers','jobs','inspections','estimates','invoices','inventory_items',
        'expenses','photos','appointments','routes','documents'
    ] loop
        if to_regclass('public.' || table_name) is not null then
            execute format('alter table public.%I add column if not exists organization_id uuid references public.organizations(id) on delete cascade', table_name);
            execute format('create index if not exists %I on public.%I(organization_id)', table_name || '_organization_id_idx', table_name);
            execute format('alter table public.%I enable row level security', table_name);
            execute format('drop policy if exists %I on public.%I', table_name || '_organization_members', table_name);
            execute format(
                'create policy %I on public.%I for all to authenticated using (public.is_organization_member(organization_id)) with check (public.is_organization_member(organization_id))',
                table_name || '_organization_members', table_name
            );
        end if;
    end loop;
end $$;

commit;
