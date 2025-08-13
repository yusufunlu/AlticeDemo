CREATE OR REPLACE PROCEDURE summarize_hours IS
BEGIN
    DBMS_OUTPUT.PUT_LINE('Employee ID | Total Hours');
    DBMS_OUTPUT.PUT_LINE('--------------------------');

    FOR rec IN (
        SELECT e.employee_id,
               e.first_name || ' ' || e.last_name AS employee_name,
               SUM(w.hours_worked) AS total_hours
        FROM employees e
                 JOIN work_logs w ON e.employee_id = w.employee_id
        GROUP BY e.employee_id, e.first_name, e.last_name
        ) LOOP
            DBMS_OUTPUT.PUT_LINE(rec.employee_id || ' - ' || rec.employee_name || ' : ' || rec.total_hours);
        END LOOP;
END;
/