begin;

create table if not exists public.organization_invitations (
    id uuid primary key default gen_random_uuid(),
    organization_id uuid not null references public.organizations(id) on delete cascade,
    email text not null,
    role text not null default 'technician' check (role in ('admin','dispatcher','technician','member')),
    invited_by uuid not null references auth.users(id) on delete cascade,
    accepted_by uuid references auth.users(id) on delete set null,
    accepted_at timestamptz,
    expires_at timestamptz not null default (now() + interval '7 days'),
    created_at timestamptz not null default now(),
    unique (organization_id, email)
);

create index if not exists organization_invitations_email_idx
    on public.organization_invitations(lower(email));
create index if not exists organization_invitations_org_idx
    on public.organization_invitations(organization_id);

alter table public.organization_invitations enable row level security;

create or replace function public.is_organization_admin(target_organization_id uuid)
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
          and om.role in ('owner','admin')
    );
$$;

revoke all on function public.is_organization_admin(uuid) from public;
grant execute on function public.is_organization_admin(uuid) to authenticated;

create policy organization_invitations_admin_select
on public.organization_invitations for select
to authenticated
using (
    public.is_organization_admin(organization_id)
    or lower(email) = lower(coalesce(auth.jwt() ->> 'email', ''))
);

create policy organization_invitations_admin_insert
on public.organization_invitations for insert
to authenticated
with check (
    public.is_organization_admin(organization_id)
    and invited_by = auth.uid()
);

create policy organization_invitations_admin_update
on public.organization_invitations for update
to authenticated
using (
    public.is_organization_admin(organization_id)
    or lower(email) = lower(coalesce(auth.jwt() ->> 'email', ''))
)
with check (
    public.is_organization_admin(organization_id)
    or lower(email) = lower(coalesce(auth.jwt() ->> 'email', ''))
);

create policy organization_invitations_admin_delete
on public.organization_invitations for delete
to authenticated
using (public.is_organization_admin(organization_id));

create or replace function public.invite_organization_member(
    target_organization_id uuid,
    target_email text,
    target_role text default 'technician'
)
returns public.organization_invitations
language plpgsql
security definer
set search_path = public
as $$
declare
    invitation public.organization_invitations;
    normalized_email text := lower(trim(target_email));
begin
    if auth.uid() is null then
        raise exception 'Authentication required';
    end if;
    if not public.is_organization_admin(target_organization_id) then
        raise exception 'Organization administrator access required';
    end if;
    if normalized_email !~ '^[^@[:space:]]+@[^@[:space:]]+\.[^@[:space:]]+$' then
        raise exception 'A valid email address is required';
    end if;
    if target_role not in ('admin','dispatcher','technician','member') then
        raise exception 'Unsupported organization role';
    end if;

    insert into public.organization_invitations(
        organization_id, email, role, invited_by, expires_at, accepted_by, accepted_at
    ) values (
        target_organization_id, normalized_email, target_role, auth.uid(), now() + interval '7 days', null, null
    )
    on conflict (organization_id, email) do update
    set role = excluded.role,
        invited_by = auth.uid(),
        expires_at = now() + interval '7 days',
        accepted_by = null,
        accepted_at = null
    returning * into invitation;

    return invitation;
end;
$$;

grant execute on function public.invite_organization_member(uuid, text, text) to authenticated;

create or replace function public.accept_organization_invitation(invitation_id uuid)
returns public.organization_members
language plpgsql
security definer
set search_path = public
as $$
declare
    invitation public.organization_invitations;
    membership public.organization_members;
    current_email text := lower(coalesce(auth.jwt() ->> 'email', ''));
begin
    if auth.uid() is null then
        raise exception 'Authentication required';
    end if;

    select * into invitation
    from public.organization_invitations oi
    where oi.id = invitation_id
    for update;

    if invitation.id is null then
        raise exception 'Invitation not found';
    end if;
    if invitation.accepted_at is not null then
        raise exception 'Invitation has already been accepted';
    end if;
    if invitation.expires_at <= now() then
        raise exception 'Invitation has expired';
    end if;
    if lower(invitation.email) <> current_email then
        raise exception 'Invitation email does not match the signed-in account';
    end if;

    insert into public.organization_members(organization_id, user_id, role)
    values (invitation.organization_id, auth.uid(), invitation.role)
    on conflict (organization_id, user_id) do update
    set role = excluded.role
    returning * into membership;

    update public.organization_invitations
    set accepted_by = auth.uid(), accepted_at = now()
    where id = invitation.id;

    return membership;
end;
$$;

grant execute on function public.accept_organization_invitation(uuid) to authenticated;

create or replace function public.update_organization_member_role(
    target_organization_id uuid,
    target_user_id uuid,
    target_role text
)
returns public.organization_members
language plpgsql
security definer
set search_path = public
as $$
declare
    membership public.organization_members;
begin
    if not public.is_organization_admin(target_organization_id) then
        raise exception 'Organization administrator access required';
    end if;
    if target_role not in ('admin','dispatcher','technician','member') then
        raise exception 'Unsupported organization role';
    end if;
    if exists (
        select 1 from public.organization_members
        where organization_id = target_organization_id
          and user_id = target_user_id
          and role = 'owner'
    ) then
        raise exception 'The organization owner role cannot be changed';
    end if;

    update public.organization_members
    set role = target_role
    where organization_id = target_organization_id
      and user_id = target_user_id
    returning * into membership;

    if membership.user_id is null then
        raise exception 'Organization member not found';
    end if;
    return membership;
end;
$$;

grant execute on function public.update_organization_member_role(uuid, uuid, text) to authenticated;

create or replace function public.remove_organization_member(
    target_organization_id uuid,
    target_user_id uuid
)
returns boolean
language plpgsql
security definer
set search_path = public
as $$
begin
    if not public.is_organization_admin(target_organization_id) then
        raise exception 'Organization administrator access required';
    end if;
    if exists (
        select 1 from public.organization_members
        where organization_id = target_organization_id
          and user_id = target_user_id
          and role = 'owner'
    ) then
        raise exception 'The organization owner cannot be removed';
    end if;

    delete from public.organization_members
    where organization_id = target_organization_id
      and user_id = target_user_id;

    return found;
end;
$$;

grant execute on function public.remove_organization_member(uuid, uuid) to authenticated;

commit;
