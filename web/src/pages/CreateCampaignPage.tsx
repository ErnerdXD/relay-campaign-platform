import { useState, type FormEvent } from 'react'
import { useNavigate } from 'react-router-dom'
import { createCampaign } from '../api'
import { useTenant } from '../TenantContext'
import type { Channel } from '../types'

const SAMPLE_CSV = `recipientId,email,phone,timezone
ada,ada@example.com,,Asia/Kuala_Lumpur
grace,grace@example.com,+60123456789,
linus,linus@example.com,+6591234567,Asia/Singapore
`

function localDateTimeValue(date: Date) {
  const pad = (value: number) => String(value).padStart(2, '0')
  return `${date.getFullYear()}-${pad(date.getMonth() + 1)}-${pad(date.getDate())}T${pad(date.getHours())}:${pad(date.getMinutes())}`
}

export default function CreateCampaignPage() {
  const { tenant } = useTenant()
  const navigate = useNavigate()
  const [name, setName] = useState('')
  const [channel, setChannel] = useState<Channel>('EMAIL')
  const [template, setTemplate] = useState('Hello {{recipientId}}, your statement is ready.')
  const [transactional, setTransactional] = useState(false)
  const [mode, setMode] = useState<'now' | 'later'>('now')
  const [scheduledAt, setScheduledAt] = useState(() => localDateTimeValue(new Date(Date.now() + 3_600_000)))
  const [file, setFile] = useState<File | null>(null)
  const [preview, setPreview] = useState<string[][]>([])
  const [progress, setProgress] = useState(0)
  const [submitting, setSubmitting] = useState(false)
  const [error, setError] = useState('')

  const handleFile = async (nextFile: File | null) => {
    setFile(nextFile)
    setError('')
    if (!nextFile) {
      setPreview([])
      return
    }
    const text = await nextFile.slice(0, 64 * 1024).text()
    setPreview(
      text
        .split(/\r?\n/)
        .filter(Boolean)
        .slice(0, 6)
        .map((line) => line.split(',')),
    )
  }

  const submit = async (event: FormEvent) => {
    event.preventDefault()
    setError('')

    if (!file) {
      setError('Choose a recipient CSV before creating the campaign.')
      return
    }
    if (mode === 'later' && new Date(scheduledAt).getTime() <= Date.now()) {
      setError('Scheduled time must be in the future.')
      return
    }

    setSubmitting(true)
    setProgress(0)
    try {
      const created = await createCampaign(
        tenant.token ?? '',
        {
          tenantId: tenant.id,
          name: name.trim(),
          channel,
          template,
          scheduledAt: mode === 'now' ? 'now' : new Date(scheduledAt).toISOString(),
          transactional,
          file,
        },
        setProgress,
      )
      navigate(`/campaigns/${created.id}`)
    } catch (uploadError) {
      setError(uploadError instanceof Error ? uploadError.message : 'Upload failed.')
    } finally {
      setSubmitting(false)
    }
  }

  return (
    <>
      <div className="page-head">
        <div>
          <h1>New campaign</h1>
          <p>Upload recipients, choose a channel, and send now or schedule it.</p>
        </div>
      </div>

      <form className="form-grid" onSubmit={submit}>
        <div className="card form-card">
          <div className="field">
            <label htmlFor="name">Campaign name</label>
            <input
              id="name"
              required
              maxLength={160}
              value={name}
              onChange={(event) => setName(event.target.value)}
              placeholder="September statement"
            />
          </div>

          <div className="row">
            <div className="field">
              <label htmlFor="channel">Channel</label>
              <select id="channel" value={channel} onChange={(event) => setChannel(event.target.value as Channel)}>
                <option value="EMAIL">Email</option>
                <option value="SMS">SMS</option>
                <option value="PUSH">Push (simulated)</option>
              </select>
            </div>
            <div className="field">
              <label htmlFor="schedule">Schedule</label>
              <select id="schedule" value={mode} onChange={(event) => setMode(event.target.value as 'now' | 'later')}>
                <option value="now">Send now</option>
                <option value="later">Schedule</option>
              </select>
            </div>
          </div>

          {mode === 'later' && (
            <div className="field">
              <label htmlFor="scheduledAt">Send at</label>
              <input
                id="scheduledAt"
                type="datetime-local"
                value={scheduledAt}
                onChange={(event) => setScheduledAt(event.target.value)}
              />
            </div>
          )}

          <div className="field">
            <label htmlFor="template">Message template</label>
            <textarea
              id="template"
              required
              maxLength={2000}
              value={template}
              onChange={(event) => setTemplate(event.target.value)}
            />
          </div>

          <label className="check">
            <input
              type="checkbox"
              checked={transactional}
              onChange={(event) => setTransactional(event.target.checked)}
            />
            Transactional message (bypasses SMS and push quiet hours)
          </label>

          <div className="field">
            <label>Recipient CSV</label>
            <label className={`drop ${file ? 'active' : ''}`}>
              <input
                type="file"
                accept=".csv,text/csv"
                onChange={(event) => void handleFile(event.target.files?.[0] ?? null)}
              />
              {file ? `${file.name} · ${(file.size / 1024).toFixed(1)} KB` : 'Choose a CSV or drop it here'}
            </label>
            <span className="muted" style={{ fontSize: 12 }}>
              Header: recipientId,email,phone with optional timezone. Email is required for email, E.164 phone for SMS.
            </span>
          </div>

          {preview.length > 0 && (
            <div className="preview card">
              <div className="table-wrap">
                <table>
                  <tbody>
                    {preview.map((row, index) => (
                      <tr key={index}>
                        {row.map((cell, cellIndex) => (
                          <td key={cellIndex}>{cell}</td>
                        ))}
                      </tr>
                    ))}
                  </tbody>
                </table>
              </div>
            </div>
          )}

          {submitting && (
            <div className="field">
              <div className="progress">
                <span style={{ width: `${progress}%` }} />
              </div>
              <span className="muted" style={{ fontSize: 12 }}>
                Uploading and validating… {progress}%
              </span>
            </div>
          )}

          {error && <div className="error-box">{error}</div>}

          <div className="actions">
            <button className="button primary" type="submit" disabled={submitting}>
              {submitting ? 'Creating…' : 'Create campaign'}
            </button>
            <a
              className="button"
              download="sample-recipients.csv"
              href={`data:text/csv;charset=utf-8,${encodeURIComponent(SAMPLE_CSV)}`}
            >
              Download sample CSV
            </a>
          </div>
        </div>

        <aside className="card side-card">
          <h3>What happens next</h3>
          <ul>
            <li>The API streams and validates every row before accepting the campaign.</li>
            <li>A durable job is created per recipient, then the worker delivers them.</li>
            <li>Email, SMS, and push are simulated with 50–200 ms latency and a 20% failure rate.</li>
            <li>Failed sends retry with exponential backoff; the detail page exposes the queue.</li>
            <li>SMS and push wait until 08:00 local time when they hit quiet hours.</li>
          </ul>
        </aside>
      </form>
    </>
  )
}
