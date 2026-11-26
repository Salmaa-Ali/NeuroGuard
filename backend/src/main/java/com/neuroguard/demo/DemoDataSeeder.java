package com.neuroguard.demo;

import com.neuroguard.appointment.entity.Appointment;
import com.neuroguard.appointment.entity.AppointmentStatus;
import com.neuroguard.appointment.repository.AppointmentRepository;
import com.neuroguard.billing.entity.Invoice;
import com.neuroguard.billing.entity.InvoiceItem;
import com.neuroguard.billing.entity.InvoiceStatus;
import com.neuroguard.billing.entity.Payment;
import com.neuroguard.billing.entity.PaymentStatus;
import com.neuroguard.billing.entity.ServiceItem;
import com.neuroguard.billing.repository.InvoiceItemRepository;
import com.neuroguard.billing.repository.InvoiceRepository;
import com.neuroguard.billing.repository.PaymentRepository;
import com.neuroguard.billing.repository.ServiceItemRepository;
import com.neuroguard.billing.service.Money;
import com.neuroguard.imaging.entity.MriOrder;
import com.neuroguard.imaging.entity.MriOrderStatus;
import com.neuroguard.imaging.repository.MriOrderRepository;
import com.neuroguard.imaging.service.AccessionNumberGenerator;
import com.neuroguard.patient.entity.Patient;
import com.neuroguard.patient.repository.PatientRepository;
import com.neuroguard.staff.entity.BaseRole;
import com.neuroguard.staff.entity.PermissionScope;
import com.neuroguard.staff.entity.Staff;
import com.neuroguard.staff.repository.StaffRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneOffset;
import java.time.temporal.ChronoUnit;
import java.util.Comparator;
import java.util.List;

/**
 * Opt-in demo history for patients that already exist, so a demo never starts from an empty account.
 * Runs only in the dev profile AND when DEMO_DATA_ENABLED=true (app.demo-data.enabled), so teammates'
 * normal startups never write it.
 *
 * <p>Per patient: two completed visits, one cancelled visit, one upcoming visit; a REPORTED MRI order on the
 * first visit and an ORDERED one on the upcoming visit (accession numbers from the normal generator); a PAID
 * invoice with its payment and an UNPAID invoice. Every seeded appointment reason, order indication and the
 * consultation service start with "Demo:" / are clearly named, so the rows are easy to find and remove.
 * A patient who already has a "Demo:" appointment is skipped, so running it again is safe.
 */
@Component
@Profile("dev")
@ConditionalOnProperty(name = "app.demo-data.enabled", havingValue = "true")
public class DemoDataSeeder implements ApplicationRunner {

    private static final Logger log = LoggerFactory.getLogger(DemoDataSeeder.class);
    static final String MARKER = "Demo:";

    private final PatientRepository patients;
    private final StaffRepository staff;
    private final AppointmentRepository appointments;
    private final MriOrderRepository orders;
    private final AccessionNumberGenerator accessionNumbers;
    private final ServiceItemRepository services;
    private final InvoiceRepository invoices;
    private final InvoiceItemRepository invoiceItems;
    private final PaymentRepository payments;
    private final Clock clock;

    public DemoDataSeeder(PatientRepository patients, StaffRepository staff, AppointmentRepository appointments,
                          MriOrderRepository orders, AccessionNumberGenerator accessionNumbers,
                          ServiceItemRepository services, InvoiceRepository invoices,
                          InvoiceItemRepository invoiceItems, PaymentRepository payments, Clock clock) {
        this.patients = patients;
        this.staff = staff;
        this.appointments = appointments;
        this.orders = orders;
        this.accessionNumbers = accessionNumbers;
        this.services = services;
        this.invoices = invoices;
        this.invoiceItems = invoiceItems;
        this.payments = payments;
        this.clock = clock;
    }

    @Override
    @Transactional
    public void run(ApplicationArguments args) {
        List<Staff> neurologists = staff.findAll().stream()
                .filter(s -> s.getBaseRole() == BaseRole.CLINICAL_STAFF
                        && s.getPermissionScope() == PermissionScope.NEUROLOGIST)
                .sorted(Comparator.comparing(Staff::getName))
                .toList();
        if (neurologists.isEmpty()) {
            log.warn("Demo data skipped: no neurologist exists to own the appointments");
            return;
        }
        List<Appointment> existing = appointments.findAll();
        ServiceItem consultation = service("Neurology consultation", "500.00");
        ServiceItem mri = services.findAll().stream()
                .filter(s -> s.getName().toLowerCase().contains("mri"))
                .min(Comparator.comparing(ServiceItem::getName))
                .orElseGet(() -> service("Brain MRI", "1500.00"));

        List<Patient> all = patients.findAll().stream().sorted(Comparator.comparing(Patient::getPatientNumber)).toList();
        int seeded = 0;
        for (int i = 0; i < all.size(); i++) {
            Patient p = all.get(i);
            boolean done = existing.stream().anyMatch(a -> a.getPatient().getId().equals(p.getId())
                    && a.getReason() != null && a.getReason().startsWith(MARKER));
            if (done) {
                continue;
            }
            seedPatient(p, neurologists.get(i % neurologists.size()), i, consultation, mri);
            seeded++;
        }
        log.info("Demo data added for {} patient(s)", seeded);
    }

    private void seedPatient(Patient p, Staff neurologist, int index, ServiceItem consultation, ServiceItem mri) {
        int hour = 9 + (index % 7);   // spread patients across the day so no two share a slot
        Appointment first = appointment(p, neurologist, at(-28, hour), AppointmentStatus.COMPLETED,
                MARKER + " initial neurology consultation (recurrent headaches)");
        Appointment review = appointment(p, neurologist, at(-10, hour), AppointmentStatus.COMPLETED,
                MARKER + " MRI results review");
        appointment(p, neurologist, at(-4, hour), AppointmentStatus.CANCELLED,
                MARKER + " follow-up visit (cancelled by patient)");
        Appointment upcoming = appointment(p, neurologist, freeSlot(p, neurologist, at(6, hour)),
                AppointmentStatus.SCHEDULED, MARKER + " follow-up visit");

        order(first, MriOrderStatus.REPORTED, MARKER + " recurrent headaches, rule out structural lesion");
        order(upcoming, MriOrderStatus.ORDERED, MARKER + " follow-up brain MRI with contrast");

        Invoice paid = invoice(p, first, List.of(consultation, mri));
        pay(paid, first.getScheduledTime().plus(Duration.ofDays(1)));
        invoice(p, review, List.of(consultation));
    }

    private Instant at(int daysFromToday, int hourUtc) {
        return LocalDate.now(clock.withZone(ZoneOffset.UTC)).plusDays(daysFromToday)
                .atTime(hourUtc, 0).toInstant(ZoneOffset.UTC);
    }

    /** Only SCHEDULED appointments can clash; move an hour later until both diaries are free. */
    private Instant freeSlot(Patient p, Staff neurologist, Instant start) {
        Instant t = start;
        while (appointments.staffHasConflict(neurologist.getId(), t, t.plus(30, ChronoUnit.MINUTES))
                || appointments.patientHasConflict(p.getId(), t, t.plus(30, ChronoUnit.MINUTES))) {
            t = t.plus(1, ChronoUnit.HOURS);
        }
        return t;
    }

    private Appointment appointment(Patient p, Staff neurologist, Instant when, AppointmentStatus status,
                                    String reason) {
        Appointment a = new Appointment();
        a.setPatient(p);
        a.setStaff(neurologist);
        a.setScheduledTime(when);
        a.setDurationMinutes(30);
        a.setStatus(status);
        a.setReason(reason);
        return appointments.saveAndFlush(a);
    }

    private void order(Appointment appointment, MriOrderStatus status, String indication) {
        MriOrder o = new MriOrder();
        o.setAppointment(appointment);
        o.setPatient(appointment.getPatient());
        o.setAccessionNumber(accessionNumbers.next());
        o.setIndication(indication);
        o.setStatus(status);
        orders.saveAndFlush(o);
    }

    /** Same rules as InvoiceService: unit price snapshot, line totals and the total computed here. */
    private Invoice invoice(Patient p, Appointment appointment, List<ServiceItem> lines) {
        BigDecimal total = Money.of(BigDecimal.ZERO);
        for (ServiceItem s : lines) {
            total = total.add(Money.of(s.getPrice()));
        }
        Invoice inv = new Invoice();
        inv.setPatient(p);
        inv.setAppointment(appointment);
        inv.setStatus(InvoiceStatus.UNPAID);
        inv.setIssueDate(LocalDate.ofInstant(appointment.getScheduledTime(), ZoneOffset.UTC));
        inv.setTotalAmount(total);
        Invoice saved = invoices.saveAndFlush(inv);
        for (ServiceItem s : lines) {
            InvoiceItem line = new InvoiceItem();
            line.setInvoice(saved);
            line.setService(s);
            line.setQuantity(1);
            line.setUnitPrice(Money.of(s.getPrice()));
            line.setLineTotal(Money.of(s.getPrice()));
            invoiceItems.save(line);
        }
        invoiceItems.flush();
        return saved;
    }

    private void pay(Invoice invoice, Instant when) {
        Payment payment = new Payment();
        payment.setInvoice(invoice);
        payment.setAmountPaid(Money.of(invoice.getTotalAmount()));
        payment.setStatus(PaymentStatus.COMPLETED);
        payment.setPaymentDate(when.truncatedTo(ChronoUnit.MICROS));
        payments.save(payment);
        invoice.setStatus(InvoiceStatus.PAID);
        invoices.saveAndFlush(invoice);
    }

    private ServiceItem service(String name, String price) {
        return services.findAll().stream()
                .filter(s -> s.getName().trim().equalsIgnoreCase(name))
                .findFirst()
                .orElseGet(() -> {
                    ServiceItem s = new ServiceItem();
                    s.setName(name);
                    s.setPrice(new BigDecimal(price));
                    return services.saveAndFlush(s);
                });
    }
}
