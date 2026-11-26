package com.neuroguard.auth.service;

import com.neuroguard.audit.AuditAction;
import com.neuroguard.audit.entity.ActorType;
import com.neuroguard.audit.service.AuditService;
import com.neuroguard.auth.dto.LoginRequest;
import com.neuroguard.auth.dto.LoginResponse;
import com.neuroguard.auth.dto.MeResponse;
import com.neuroguard.auth.dto.RegisterPatientRequest;
import com.neuroguard.common.ConflictException;
import com.neuroguard.common.Emails;
import com.neuroguard.common.InvalidCredentialsException;
import com.neuroguard.patient.entity.Patient;
import com.neuroguard.patient.repository.PatientRepository;
import com.neuroguard.security.CurrentUser;
import com.neuroguard.security.JwtService;
import com.neuroguard.security.Role;
import com.neuroguard.security.UserType;
import com.neuroguard.staff.entity.Staff;
import com.neuroguard.staff.repository.StaffRepository;
import org.springframework.security.authentication.AuthenticationCredentialsNotFoundException;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

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

    /** Single-endpoint login for the shared form: checks both account tables without a failed probe request. */
    public LoginResponse loginAny(LoginRequest request) {
        String email = Emails.normalize(request.email());
        Optional<Staff> staff = staffRepository.findByEmail(email);
        Optional<Patient> patient = patientRepository.findByEmail(email);
        boolean staffMatches = passwordMatches(request.password(), staff.map(Staff::getPasswordHash));
        boolean patientMatches = passwordMatches(request.password(), patient.map(Patient::getPasswordHash));
        if (staffMatches) {
            Staff account = staff.orElseThrow();
            auditService.recordIndependent(ActorType.STAFF, account.getId(), AuditAction.LOGIN_SUCCESS, "STAFF", account.getId());
            Role role = Role.valueOf(account.getBaseRole().name());
            JwtService.IssuedToken token = jwtService.issue(UserType.STAFF, account.getId(), role, account.getPermissionScope());
            return LoginResponse.bearer(token.value(), token.expiresInSeconds(), UserType.STAFF, role, account.getPermissionScope());
        }
        if (patientMatches) {
            Patient account = patient.orElseThrow();
            auditService.recordIndependent(ActorType.PATIENT, account.getId(), AuditAction.LOGIN_SUCCESS, "PATIENT", account.getId());
            JwtService.IssuedToken token = jwtService.issue(UserType.PATIENT, account.getId(), Role.PATIENT, null);
            return LoginResponse.bearer(token.value(), token.expiresInSeconds(), UserType.PATIENT, Role.PATIENT, null);
        }
        if (staff.isPresent()) {
            auditService.recordIndependent(ActorType.STAFF, staff.get().getId(), AuditAction.LOGIN_FAILED, null, null);
        } else {
            auditService.recordIndependent(ActorType.PATIENT, patient.map(Patient::getId).orElse(null), AuditAction.LOGIN_FAILED, null, null);
        }
        throw new InvalidCredentialsException();
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

    @Transactional
    public LoginResponse registerPatient(RegisterPatientRequest request) {
        String email = Emails.normalize(request.email());
        if (patientRepository.existsByEmail(email)) {
            throw new ConflictException("A patient account with this email already exists");
        }

        long seq = patientRepository.nextPatientNumberSeq();
        String patientNumber = String.format("NG-%05d", seq);

        Patient patient = new Patient();
        patient.setPatientNumber(patientNumber);
        patient.setName(request.name().trim());
        patient.setEmail(email);
        patient.setPasswordHash(passwordEncoder.encode(request.password()));
        patient.setDateOfBirth(request.dateOfBirth());
        patient.setPhone(request.phone() != null && !request.phone().isBlank() ? request.phone().trim() : null);
        patient.setNeurologyHistory(
                request.neurologyHistory() != null && !request.neurologyHistory().isBlank()
                        ? request.neurologyHistory().trim()
                        : null);
        patient = patientRepository.saveAndFlush(patient);

        auditService.recordIndependent(ActorType.PATIENT, patient.getId(), AuditAction.PATIENT_REGISTERED,
                "PATIENT", patient.getId());

        JwtService.IssuedToken token = jwtService.issue(UserType.PATIENT, patient.getId(), Role.PATIENT, null);
        return LoginResponse.bearer(token.value(), token.expiresInSeconds(), UserType.PATIENT, Role.PATIENT, null);
    }

    /** The caller's identity. Name and email come from the database, role and scope from the token. */
    public MeResponse me() {
        CurrentUser user = CurrentUser.require();
        if (user.isStaff()) {
            Staff staff = staffRepository.findById(user.id())
                    .orElseThrow(() -> new AuthenticationCredentialsNotFoundException("Account no longer exists"));
            return new MeResponse(staff.getId(), staff.getName(), staff.getEmail(),
                    user.userType(), user.role(), user.scope());
        }
        Patient patient = patientRepository.findById(user.id())
                .orElseThrow(() -> new AuthenticationCredentialsNotFoundException("Account no longer exists"));
        return new MeResponse(patient.getId(), patient.getName(), patient.getEmail(),
                user.userType(), user.role(), user.scope());
    }

    /** Always performs one BCrypt comparison, whether or not the account exists. */
    private boolean passwordMatches(String rawPassword, Optional<String> storedHash) {
        boolean matches = passwordEncoder.matches(rawPassword, storedHash.orElse(dummyHash));
        return storedHash.isPresent() && matches;
    }
}
