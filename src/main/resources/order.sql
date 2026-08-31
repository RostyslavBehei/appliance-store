INSERT INTO orders (client_id, employee_id, total_price, approved, created_at) VALUES (5, 1, 150.00, true, '2026-07-15 10:00:00');
INSERT INTO orders (client_id, employee_id, total_price, approved, created_at) VALUES (6, 2, 171.00, true, '2026-07-22 14:30:00');
INSERT INTO orders (client_id, employee_id, total_price, approved, created_at) VALUES (7, 1, 120.00, false, '2026-07-28 16:45:00');
INSERT INTO orders (client_id, employee_id, total_price, approved, created_at) VALUES (8, 3, 450.00, true, '2026-08-05 09:00:00');
INSERT INTO orders (client_id, employee_id, total_price, approved, created_at) VALUES (9, 2, 540.00, true, '2026-08-12 11:30:00');
INSERT INTO orders (client_id, employee_id, total_price, approved, created_at) VALUES (10, 4, 230.00, true, '2026-08-20 15:20:00');
INSERT INTO orders (client_id, employee_id, total_price, approved, created_at) VALUES (11, 1, 320.00, false, '2026-08-22 18:00:00');



INSERT INTO order_rows (order_id, appliance_id, amount, number, created_at) VALUES (1, 1, 150.00, 1, '2026-07-15 10:00:00');
INSERT INTO order_rows (order_id, appliance_id, amount, number, created_at) VALUES (2, 2, 171.00, 2, '2026-07-22 14:30:00');
INSERT INTO order_rows (order_id, appliance_id, amount, number, created_at) VALUES (3, 3, 120.00, 1, '2026-07-28 16:45:00');
INSERT INTO order_rows (order_id, appliance_id, amount, number, created_at) VALUES (4, 4, 450.00, 1, '2026-08-05 09:00:00');
INSERT INTO order_rows (order_id, appliance_id, amount, number, created_at) VALUES (5, 3, 240.00, 2, '2026-08-12 11:30:00');
INSERT INTO order_rows (order_id, appliance_id, amount, number, created_at) VALUES (5, 7, 300.00, 1, '2026-08-12 11:30:00');
INSERT INTO order_rows (order_id, appliance_id, amount, number, created_at) VALUES (6, 1, 150.00, 1, '2026-08-20 15:20:00');
INSERT INTO order_rows (order_id, appliance_id, amount, number, created_at) VALUES (7, 7, 320.00, 1, '2026-08-22 18:00:00');