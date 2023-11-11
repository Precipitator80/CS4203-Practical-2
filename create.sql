CREATE OR REPLACE VIEW view_standard_dinner AS
SELECT count(*) AS "Number of Guests with Standard Dietary Requirements",
    p1.table_no AS "Table Number"
FROM person AS p1
    /* Ensure only the people at the table and that don't have an associated guest_diet entry are counted. */
WHERE NOT EXISTS (
        SELECT *
        FROM person,
            guest_diet
        WHERE p1.id = guest_diet.person_id
    )
GROUP BY p1.table_no
ORDER BY p1.table_no;