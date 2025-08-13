-- Customers & products
INSERT INTO customers (name, email) VALUES ('John Doe', 'john@example.com');
INSERT INTO customers (name, email) VALUES ('Jane Smith', 'jane@example.com');

INSERT INTO products (name, price, stock_qty) VALUES ('Laptop', 1200, 50);
INSERT INTO products (name, price, stock_qty) VALUES ('Headphones', 200, 200);

-- One order with two items
INSERT INTO orders (customer_id, total_amount) VALUES (1, 1400);
INSERT INTO order_items (order_id, product_id, quantity, unit_price) VALUES (1, 1, 1, 1200);
INSERT INTO order_items (order_id, product_id, quantity, unit_price) VALUES (1, 2, 1, 200);

-- Employees & logs
INSERT INTO employees (name) VALUES ('Alice Brown');
INSERT INTO employees (name) VALUES ('Bob Johnson');

INSERT INTO work_logs (employee_id, work_date, hours_worked) VALUES (1, DATE '2025-08-01', 8);
INSERT INTO work_logs (employee_id, work_date, hours_worked) VALUES (1, DATE '2025-08-02', 7.5);
INSERT INTO work_logs (employee_id, work_date, hours_worked) VALUES (2, DATE '2025-08-01', 9);

COMMIT;
