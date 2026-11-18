-- 修正 V15 的编号生成顺序问题：先补领域，再按领域内序号重排
UPDATE t_api t
SET domain_code = COALESCE((SELECT s.domain_code FROM t_system s WHERE s.code = t.system_code LIMIT 1), 'IT')
WHERE t.domain_code IS NULL;

DO $$
DECLARE r RECORD;
BEGIN
  FOR r IN
    SELECT id, domain_code,
           row_number() OVER (PARTITION BY domain_code ORDER BY id) AS n
    FROM t_api
  LOOP
    UPDATE t_api
    SET api_no = 'API-' || r.domain_code || '-' || lpad(r.n::text, 4, '0')
    WHERE id = r.id;
  END LOOP;
END $$;
