-- Sample Customers
INSERT INTO customers (first_name, last_name, email) VALUES ('John', 'Doe', 'john@example.com');
INSERT INTO customers (first_name, last_name, email) VALUES ('Jane', 'Smith', 'jane@example.com');

-- Sample Products
INSERT INTO products (name, description, price, stock_qty) VALUES ('Laptop', '15-inch display', 1200.00, 50);
INSERT INTO products (name, description, price, stock_qty) VALUES ('Headphones', 'Noise cancelling', 200.00, 200);

-- Sample Orders
INSERT INTO orders (customer_id, total_amount) VALUES (1, 1400.00);

-- Sample Order Items
INSERT INTO order_items (order_id, product_id, quantity, unit_price) VALUES (1, 1, 1, 1200.00);
INSERT INTO order_items (order_id, product_id, quantity, unit_price) VALUES (1, 2, 1, 200.00);

-- Sample Employees
INSERT INTO employees (first_name, last_name) VALUES ('Alice', 'Brown');
INSERT INTO employees (first_name, last_name) VALUES ('Bob', 'Johnson');

-- Sample Work Logs
INSERT INTO work_logs (employee_id, work_date, hours_worked) VALUES (1, DATE '2025-08-01', 8);
INSERT INTO work_logs (employee_id, work_date, hours_worked) VALUES (1, DATE '2025-08-02', 7.5);
INSERT INTO work_logs (employee_id, work_date, hours_worked) VALUES (2, DATE '2025-08-01', 9);
