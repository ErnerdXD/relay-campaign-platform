import { useDeferredValue, useState } from 'react'
import { useQuery } from '@tanstack/react-query'
import { Link } from 'react-router-dom'
import { getCampaigns } from '../api'
import StatusBadge from '../components/StatusBadge'
import { useTenant } from '../TenantContext'

const PAGE_SIZE = 20

export default function CampaignsPage() {
  const { tenant } = useTenant()
  const [search, setSearch] = useState('')
  const [status, setStatus] = useState('')
  const [channel, setChannel] = useState('')
  const [page, setPage] = useState(0)
  const deferredSearch = useDeferredValue(search)

  const { data, isLoading, isError, error, isFetching } = useQuery({
    queryKey: ['campaigns', tenant.id, deferredSearch, status, channel, page],
    queryFn: () =>
      getCampaigns(tenant.token ?? '', {
        search: deferredSearch,
        status,
        channel,
        page,
        pageSize: PAGE_SIZE,
      }),
    enabled: Boolean(tenant.token),
    refetchInterval: 4000,
  })

  const total = data?.total ?? 0
  const pageCount = Math.max(1, Math.ceil(total / PAGE_SIZE))

  return (
    <>
      <div className="page-head">
        <div>
          <h1>Campaigns</h1>
          <p>
            {tenant.name} · {total.toLocaleString()} campaigns
            {isFetching ? ' · refreshing…' : ''}
          </p>
        </div>
        <div className="spacer" />
        <Link className="button primary" to="/campaigns/new">
          New campaign
        </Link>
      </div>

      <div className="filters">
        <input
          placeholder="Search name or campaign id"
          value={search}
          onChange={(event) => {
            setSearch(event.target.value)
            setPage(0)
          }}
        />
        <select
          value={status}
          onChange={(event) => {
            setStatus(event.target.value)
            setPage(0)
          }}
        >
          <option value="">All statuses</option>
          <option value="SCHEDULED">Scheduled</option>
          <option value="RUNNING">Running</option>
          <option value="COMPLETED">Completed</option>
          <option value="FAILED">Failed</option>
        </select>
        <select
          value={channel}
          onChange={(event) => {
            setChannel(event.target.value)
            setPage(0)
          }}
        >
          <option value="">All channels</option>
          <option value="EMAIL">Email</option>
          <option value="SMS">SMS</option>
          <option value="PUSH">Push</option>
        </select>
        <span />
      </div>

      <div className="card">
        {isLoading ? (
          <div className="loading">Loading campaigns…</div>
        ) : isError ? (
          <div className="error-box">{error instanceof Error ? error.message : 'Could not load campaigns.'}</div>
        ) : data && data.items.length === 0 ? (
          <div className="empty">No campaigns match this view. Create one to get started.</div>
        ) : (
          <div className="table-wrap">
            <table>
              <thead>
                <tr>
                  <th>Campaign</th>
                  <th>Channel</th>
                  <th>Recipients</th>
                  <th>Sent</th>
                  <th>Failed</th>
                  <th>Status</th>
                  <th>Created</th>
                </tr>
              </thead>
              <tbody>
                {data?.items.map((campaign) => (
                  <tr key={campaign.id}>
                    <td>
                      <Link to={`/campaigns/${campaign.id}`}>
                        <div className="campaign-name">{campaign.name}</div>
                        <div className="campaign-id">{campaign.id.slice(0, 8)}</div>
                      </Link>
                    </td>
                    <td>{campaign.channel}</td>
                    <td>{campaign.totalRecipients.toLocaleString()}</td>
                    <td>{campaign.sent.toLocaleString()}</td>
                    <td>{campaign.failed.toLocaleString()}</td>
                    <td>
                      <StatusBadge status={campaign.status} />
                    </td>
                    <td className="muted">{new Date(campaign.createdAt).toLocaleString()}</td>
                  </tr>
                ))}
              </tbody>
            </table>
          </div>
        )}

        <div className="pagination">
          <span>
            Page {page + 1} of {pageCount}
          </span>
          <div>
            <button className="button" disabled={page === 0} onClick={() => setPage((value) => value - 1)}>
              Previous
            </button>
            <button
              className="button"
              disabled={page + 1 >= pageCount}
              onClick={() => setPage((value) => value + 1)}
            >
              Next
            </button>
          </div>
        </div>
      </div>
    </>
  )
}
