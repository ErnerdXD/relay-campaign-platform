import { Link, NavLink, Outlet } from 'react-router-dom'
import { useTenant } from '../TenantContext'

export default function Layout() {
  const { tenants, tenant, selectTenant } = useTenant()

  return (
    <div className="app">
      <header className="topbar">
        <Link className="brand" to="/">
          <strong>Relay</strong>
          <span>Ops</span>
        </Link>
        <nav className="nav">
          <NavLink to="/" end className={({ isActive }) => (isActive ? 'active' : '')}>
            Campaigns
          </NavLink>
          <NavLink to="/campaigns/new" className={({ isActive }) => (isActive ? 'active' : '')}>
            New campaign
          </NavLink>
        </nav>
        <div className="spacer" />
        <label className="tenant-select">
          <span>Workspace</span>
          <select value={tenant.id} onChange={(event) => selectTenant(event.target.value)}>
            {tenants.map((item) => (
              <option key={item.id} value={item.id}>
                {item.name}
              </option>
            ))}
          </select>
        </label>
      </header>
      <main className="container">
        <Outlet />
      </main>
    </div>
  )
}
