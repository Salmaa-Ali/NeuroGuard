package com.neuroguard.auth.service;

import com.neuroguard.audit.AuditAction;
import com.neuroguard.audit.entity.ActorType;
import com.neuroguard.audit.service.AuditService;
import com.neuroguard.auth.dto.LoginRequest;
import com.neuroguard.auth.dto.LoginResponse;
import com.neuroguard.common.Emails;
import com.neuroguard.common.InvalidCredentialsException;
import com.neuroguard.patient.entity.Patient;
import com.neuroguard.patient.repository.PatientRepository;
import com.neuroguard.security.JwtService;
import com.neuroguard.security.Role;
import com.neuroguard.security.UserType;
import com.neuroguard.staff.entity.Staff;
import com.neuroguard.staff.repository.StaffRepository;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

import java.util.Optional;

@Service
public class AuthService {

    private final StaffRepository staffRepository;
    private final PatientRepository patientRepository;
    private final PasswordEncoder passwordEncoder;
    private final JwtService jwtService;
    private final AuditService auditService;
    /** Checked when the email is unknown, so response time does not reveal whether an account exists. */
    private final String dummyHash;

    public AuthService(StaffRepository staffRepository, PatientRepository patientRepository,
                       PasswordEncoder passwordEncoder, JwtService jwtService, AuditService auditService) {
        this.staffRepository = staffRepository;
        this.patientRepository = patientRepository;
        this.passwordEncoder = passwordEncoder;
        this.jwtService = jwtService;
        this.auditService = auditService;
        this.dummyHash = passwordEncoder.encode("neuroguard-timing-equaliser");
    }

    public LoginResponse loginStaff(LoginRequest request) {
        Optional<Staff> found = staffRepository.findByEmail(Emails.normalize(request.email()));
        boolean ok = passwordMatches(request.password(), found.map(Staff::getPasswordHash));
        if (!ok) {
            auditService.recordIndependent(ActorType.STAFF, found.map(Staff::getId).orElse(null),
                    AuditAction.LOGIN_FAILED, null, null);
            throw new InvalidCredentialsException();
        }
        Staff staff = found.get();
        auditService.recordIndependent(ActorType.STAFF, staff.getId(), AuditAction.LOGIN_SUCCESS,
                "STAFF", staff.getId());
        Role role = Role.valueOf(staff.getBaseRole().name());
        JwtService.IssuedToken token = jwtService.issue(UserType.STAFF, staff.getId(), role, staff.getPermissionScope());
        return LoginResponse.bearer(token.value(), token.expiresInSeconds(), UserType.STAFF, role,
                staff.getPermissionScope());
    }

    public LoginResponse loginPatient(LoginRequest request) {
        Optional<Patient> found = patientRepository.findByEmail(Emails.normalize(request.email()));
        boolean ok = passwordMatches(request.password(), found.map(Patient::getPasswordHash));
        if (!ok) {
            auditService.recordIndependent(ActorType.PATIENT, found.map(Patient::getId).orElse(null),
                    AuditAction.LOGIN_FAILED, null, null);
            throw new InvalidCredentialsException();
        }
        Patient patient = found.get();
        auditService.recordIndependent(ActorType.PATIENT, patient.getId(), AuditAction.LOGIN_SUCCESS,
                "PATIENT", patient.getId());
        JwtService.IssuedToken token = jwtService.issue(UserType.PATIENT, patient.getId(), Role.PATIENT, null);
        return LoginResponse.bearer(token.value(), token.expiresInSeconds(), UserType.PATIENT, Role.PATIENT, null);
    }

    /** Always performs one BCrypt comparison, whether or not the account exists. */
    private boolean passwordMatches(String rawPassword, Optional<String> storedHash) {
        boolean matches = passwordEncoder.matches(rawPassword, storedHash.orElse(dummyHash));
        return storedHash.isPresent() && matches;
    }
}
