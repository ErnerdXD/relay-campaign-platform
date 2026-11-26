import { createContext, useContext, useState, type ReactNode } from 'react'
import { useQuery } from '@tanstack/react-query'
import { getSession } from './api'
import type { Tenant } from './types'

interface TenantState {
  tenants: Tenant[]
  tenant: Tenant
  selectTenant: (id: string) => void
}

const TenantContext = createContext<TenantState | null>(null)

export function TenantProvider({ children }: { children: ReactNode }) {
  const { data, isLoading, error } = useQuery({
    queryKey: ['session'],
    queryFn: getSession,
    staleTime: Infinity,
  })
  const [selectedId, setSelectedId] = useState(() => localStorage.getItem('relay.tenant') ?? '')

  if (isLoading) return <div className="loading">Connecting to Relay…</div>
  if (error || !data) {
    return (
      <div className="container">
        <div className="error-box">Could not reach the API. Start it on port 8081 and refresh.</div>
      </div>
    )
  }

  const tenant = data.tenants.find((item) => item.id === selectedId) ?? data.tenants[0]
  if (!tenant) return <div className="loading">No demo tenants found.</div>

  const selectTenant = (id: string) => {
    localStorage.setItem('relay.tenant', id)
    setSelectedId(id)
  }

  return (
    <TenantContext.Provider value={{ tenants: data.tenants, tenant, selectTenant }}>
      {children}
    </TenantContext.Provider>
  )
}

export function useTenant(): TenantState {
  const state = useContext(TenantContext)
  if (!state) throw new Error('useTenant must be used inside TenantProvider')
  return state
}
