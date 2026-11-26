INSERT INTO tenants (id, name, token, timezone, monthly_quota) VALUES
    ('northstar', 'Northstar Bank', 'demo-northstar-local', 'Asia/Kuala_Lumpur', 100000),
    ('meridian', 'Meridian Air', 'demo-meridian-local', 'Asia/Singapore', 50000);

INSERT INTO suppressions (tenant_id, recipient_ref, channel, reason) VALUES
    ('northstar', 'demo-unsubscribed', 'EMAIL', 'unsubscribed'),
    ('meridian', 'demo-unsubscribed', 'SMS', 'unsubscribed');
