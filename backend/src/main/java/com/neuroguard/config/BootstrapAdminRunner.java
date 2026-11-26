package com.neuroguard.config;

import com.neuroguard.common.Emails;
import com.neuroguard.staff.entity.BaseRole;
import com.neuroguard.staff.entity.Staff;
import com.neuroguard.staff.repository.StaffRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;

/** Creates or updates the ADMIN from BOOTSTRAP_ADMIN_EMAIL / BOOTSTRAP_ADMIN_PASSWORD. */
@Component
public class BootstrapAdminRunner implements ApplicationRunner {

    private static final Logger log = LoggerFactory.getLogger(BootstrapAdminRunner.class);

    private final StaffRepository staffRepository;
    private final PasswordEncoder passwordEncoder;
    private final BootstrapAdminProperties properties;

    public BootstrapAdminRunner(StaffRepository staffRepository, PasswordEncoder passwordEncoder,
                                BootstrapAdminProperties properties) {
        this.staffRepository = staffRepository;
        this.passwordEncoder = passwordEncoder;
        this.properties = properties;
    }

    @Override
    public void run(ApplicationArguments args) {
        String email = properties.email();
        String password = properties.password();

        log.info("Current staff accounts in DB: {}",
                staffRepository.findAll().stream().map(s -> s.getEmail() + " (" + s.getBaseRole() + ")").toList());

        if (email == null || email.isBlank() || password == null || password.isBlank()) {
            if (!staffRepository.existsByBaseRole(BaseRole.ADMIN)) {
                log.warn("No ADMIN exists and BOOTSTRAP_ADMIN_EMAIL / BOOTSTRAP_ADMIN_PASSWORD are not set: "
                        + "no admin was created, so nobody can manage staff.");
            }
            return;
        }

        if (password.length() < 8) {
            log.warn("BOOTSTRAP_ADMIN_PASSWORD must be at least 8 characters: no admin was created.");
            return;
        }

        String normalizedEmail = Emails.normalize(email);
        Staff admin = staffRepository.findByEmail(normalizedEmail).orElseGet(() -> {
            Staff s = new Staff();
            s.setName("Administrator");
            s.setEmail(normalizedEmail);
            return s;
        });

        admin.setBaseRole(BaseRole.ADMIN);
        admin.setPermissionScope(null);
        admin.setPasswordHash(passwordEncoder.encode(password));
        staffRepository.save(admin);
        log.info("Bootstrap admin ready: {} with configured password", admin.getEmail());
    }
}
