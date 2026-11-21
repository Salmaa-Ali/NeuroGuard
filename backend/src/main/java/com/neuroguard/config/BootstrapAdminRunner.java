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

/** Creates the first ADMIN from BOOTSTRAP_ADMIN_EMAIL / BOOTSTRAP_ADMIN_PASSWORD if no ADMIN exists. */
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
        if (staffRepository.existsByBaseRole(BaseRole.ADMIN)) {
            return;
        }
        String email = properties.email();
        String password = properties.password();
        if (email == null || email.isBlank() || password == null || password.isBlank()) {
            log.warn("No ADMIN exists and BOOTSTRAP_ADMIN_EMAIL / BOOTSTRAP_ADMIN_PASSWORD are not set: "
                    + "no admin was created, so nobody can manage staff.");
            return;
        }
        if (password.length() < 8) {
            log.warn("BOOTSTRAP_ADMIN_PASSWORD must be at least 8 characters: no admin was created.");
            return;
        }
        Staff admin = new Staff();
        admin.setName("Administrator");
        admin.setEmail(Emails.normalize(email));
        admin.setPasswordHash(passwordEncoder.encode(password));
        admin.setBaseRole(BaseRole.ADMIN);
        staffRepository.save(admin);
        log.info("Bootstrap admin created for {}", admin.getEmail());
    }
}
