-- Seed reference data for stations and products

-- Products
INSERT INTO product (code, name) VALUES
    ('GAS_UNL', 'Gasoline Unleaded'),
    ('GAS_PREM', 'Gasoline Premium'),
    ('DSL', 'Diesel');

-- A small set of stations across different states with coordinates
INSERT INTO station (code, name, state, latitude, longitude) VALUES
    ('TX-0001', 'Houston Central', 'TX', 29.7604, -95.3698),
    ('TX-0002', 'Dallas North', 'TX', 32.7767, -96.7970),
    ('IL-0001', 'Chicago Loop', 'IL', 41.8781, -87.6298),
    ('CA-0001', 'Los Angeles West', 'CA', 34.0522, -118.2437),
    ('NY-0001', 'NYC Midtown', 'NY', 40.7549, -73.9840),
    ('FL-0001', 'Miami Beach', 'FL', 25.7907, -80.1300),
    ('WA-0001', 'Seattle Center', 'WA', 47.6062, -122.3321),
    ('CO-0001', 'Denver Central', 'CO', 39.7392, -104.9903),
    ('AZ-0001', 'Phoenix East', 'AZ', 33.4484, -112.0740),
    ('GA-0001', 'Atlanta Downtown', 'GA', 33.7490, -84.3880);