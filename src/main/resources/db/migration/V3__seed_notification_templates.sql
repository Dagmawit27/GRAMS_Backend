-- Flyway Migration V3: Seed starter notification templates for GRAMS event types

INSERT INTO notification_templates (id, type, channel, subject, body_template) VALUES
-- PROPERTY_SUBMITTED
('a1000000-0000-0000-0000-000000000001', 'PROPERTY_SUBMITTED', 'IN_APP', NULL, 'Your property registration for {entityId} has been submitted and is under review.'),
('a1000000-0000-0000-0000-000000000002', 'PROPERTY_SUBMITTED', 'EMAIL', 'GRAMS: Property Submission Received ({entityId})', 'Hello, your property submission (ID: {entityId}) was successfully received and queued for officer verification.'),
('a1000000-0000-0000-0000-000000000003', 'PROPERTY_SUBMITTED', 'SMS', NULL, 'GRAMS: Property submission {entityId} received for verification.'),

-- PROPERTY_APPROVED
('a2000000-0000-0000-0000-000000000001', 'PROPERTY_APPROVED', 'IN_APP', NULL, 'Congratulations! Your property {entityId} has been approved and listed.'),
('a2000000-0000-0000-0000-000000000002', 'PROPERTY_APPROVED', 'EMAIL', 'GRAMS: Property {entityId} Approved', 'Great news! Your property (ID: {entityId}) has been approved by the Woreda officer and is now active.'),
('a2000000-0000-0000-0000-000000000003', 'PROPERTY_APPROVED', 'SMS', NULL, 'GRAMS: Property {entityId} is approved and listed.'),

-- AGREEMENT_SIGNED
('a3000000-0000-0000-0000-000000000001', 'AGREEMENT_SIGNED', 'IN_APP', NULL, 'Rental agreement {entityId} has been signed by both parties.'),
('a3000000-0000-0000-0000-000000000002', 'AGREEMENT_SIGNED', 'EMAIL', 'GRAMS: Rental Agreement Signed ({entityId})', 'Rental agreement {entityId} has been signed successfully. Details: {message}'),
('a3000000-0000-0000-0000-000000000003', 'AGREEMENT_SIGNED', 'SMS', NULL, 'GRAMS: Rental agreement {entityId} has been signed.'),

-- TAX_PAYMENT_DUE
('a4000000-0000-0000-0000-000000000001', 'TAX_PAYMENT_DUE', 'IN_APP', NULL, 'Notice: Rental tax payment for property {entityId} is due soon.'),
('a4000000-0000-0000-0000-000000000002', 'TAX_PAYMENT_DUE', 'EMAIL', 'GRAMS Tax Notice: Payment Due for {entityId}', 'Dear taxpayer, rental tax payment for property {entityId} is due. {message}'),
('a4000000-0000-0000-0000-000000000003', 'TAX_PAYMENT_DUE', 'SMS', NULL, 'GRAMS: Tax payment due for {entityId}. Please complete payment on the portal.'),

-- COMPLAINT_SUBMITTED
('a4500000-0000-0000-0000-000000000001', 'COMPLAINT_SUBMITTED', 'IN_APP', NULL, 'Your complaint {entityId} has been submitted and is awaiting review.'),
('a4500000-0000-0000-0000-000000000002', 'COMPLAINT_SUBMITTED', 'EMAIL', 'GRAMS Support: Complaint {entityId} Received', 'Your complaint (ID: {entityId}) has been received and will be reviewed shortly.'),
('a4500000-0000-0000-0000-000000000003', 'COMPLAINT_SUBMITTED', 'SMS', NULL, 'GRAMS: Complaint {entityId} received.'),

-- COMPLAINT_ASSIGNED
('a5000000-0000-0000-0000-000000000001', 'COMPLAINT_ASSIGNED', 'IN_APP', NULL, 'Complaint {entityId} has been assigned to an officer for investigation.'),
('a5000000-0000-0000-0000-000000000002', 'COMPLAINT_ASSIGNED', 'EMAIL', 'GRAMS Support: Complaint {entityId} Assigned', 'Your submitted complaint (ID: {entityId}) has been assigned to an officer. Status update: {message}'),
('a5000000-0000-0000-0000-000000000003', 'COMPLAINT_ASSIGNED', 'SMS', NULL, 'GRAMS: Complaint {entityId} assigned to officer.'),

-- COMPLAINT_RESOLVED
('a6000000-0000-0000-0000-000000000001', 'COMPLAINT_RESOLVED', 'IN_APP', NULL, 'Complaint {entityId} has been resolved.'),
('a6000000-0000-0000-0000-000000000002', 'COMPLAINT_RESOLVED', 'EMAIL', 'GRAMS Support: Complaint {entityId} Resolved', 'Your complaint (ID: {entityId}) has been resolved. {message}'),
('a6000000-0000-0000-0000-000000000003', 'COMPLAINT_RESOLVED', 'SMS', NULL, 'GRAMS: Complaint {entityId} resolved.')
ON CONFLICT (type, channel) DO NOTHING;
