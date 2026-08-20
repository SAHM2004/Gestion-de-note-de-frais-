package com.ids.expense.config;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.CommandLineRunner;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Component;

@Component
public class DbFixer implements CommandLineRunner {

    @Autowired
    private JdbcTemplate jdbcTemplate;

    @Autowired
    private org.springframework.security.crypto.password.PasswordEncoder passwordEncoder;

    @Override
    public void run(String... args) throws Exception {
        try {
            jdbcTemplate.execute("ALTER TABLE utilisateurs ADD COLUMN active BOOLEAN DEFAULT true");
        } catch (Exception ignored) {
        }

        try {
            jdbcTemplate.execute("ALTER TABLE utilisateurs ADD COLUMN force_password_change BOOLEAN DEFAULT true");
        } catch (Exception ignored) {
        }

        try {
            String defaultHash = passwordEncoder.encode("password");
            jdbcTemplate.execute("UPDATE utilisateurs SET password = '" + defaultHash + "' WHERE password IS NULL OR password = ''");
            jdbcTemplate.execute("UPDATE utilisateurs SET email = LOWER(REPLACE(email, ' ', '')) WHERE email LIKE '% %'");
            jdbcTemplate.execute("UPDATE utilisateurs SET email = REPLACE(email, 'pacaline', 'pascaline') WHERE email LIKE '%pacaline%'");
            jdbcTemplate.execute("UPDATE utilisateurs SET email = REPLACE(email, 'mounsahm', 'mounasahm') WHERE email LIKE '%mounsahm%'");
            jdbcTemplate.execute("UPDATE utilisateurs SET active = true WHERE active IS NULL");
            jdbcTemplate.execute("UPDATE utilisateurs SET force_password_change = true WHERE force_password_change IS NULL");
            // Associer le workflow technique (avec DT) aux services ALVANET, SLF et SCR
            jdbcTemplate.execute("UPDATE departements SET workflow_template_id = (SELECT id FROM modeles_workflow WHERE name LIKE '%Technique%' LIMIT 1) WHERE name LIKE '%ALVANET%' OR name LIKE '%SLF%' OR name LIKE '%SCR%'");
            // Associer le workflow direction générale (sans DT) à tous les autres services
            jdbcTemplate.execute("UPDATE departements SET workflow_template_id = (SELECT id FROM modeles_workflow WHERE name LIKE '%Générale%' LIMIT 1) WHERE name NOT LIKE '%ALVANET%' AND name NOT LIKE '%SLF%' AND name NOT LIKE '%SCR%'");
        } catch (Exception ignored) {
        }
    }
}
