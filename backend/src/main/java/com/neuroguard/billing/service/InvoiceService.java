package com.neuroguard.billing.service;

import com.neuroguard.appointment.entity.Appointment;
import com.neuroguard.appointment.repository.AppointmentRepository;
import com.neuroguard.audit.AuditAction;
import com.neuroguard.audit.service.AuditService;
import com.neuroguard.billing.dto.CreateInvoiceRequest;
import com.neuroguard.billing.dto.InvoiceItemResponse;
import com.neuroguard.billing.dto.InvoiceResponse;
import com.neuroguard.billing.dto.PaymentResponse;
import com.neuroguard.billing.entity.Invoice;
import com.neuroguard.billing.entity.InvoiceItem;
import com.neuroguard.billing.entity.InvoiceStatus;
import com.neuroguard.billing.entity.ServiceItem;
import com.neuroguard.billing.repository.InvoiceItemRepository;
import com.neuroguard.billing.repository.InvoiceRepository;
import com.neuroguard.billing.repository.PaymentRepository;
import com.neuroguard.billing.repository.ServiceItemRepository;
import com.neuroguard.common.BadRequestException;
import com.neuroguard.common.ConflictException;
import com.neuroguard.common.PageResponse;
import com.neuroguard.common.Pages;
import com.neuroguard.common.PatientRecordAccess;
import com.neuroguard.common.ResourceNotFoundException;
import com.neuroguard.icd10.service.Icd10Service;
import com.neuroguard.patient.entity.Patient;
import com.neuroguard.patient.repository.PatientRepository;
import com.neuroguard.security.CurrentUser;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.ZoneOffset;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.stream.Collectors;

@Service
public class InvoiceService {

    private static final Sort NEWEST_FIRST = Sort.by(Sort.Order.desc("issueDate"), Sort.Order.desc("createdAt"),
            Sort.Order.asc("id"));

    private final InvoiceRepository invoiceRepository;
    private final InvoiceItemRepository itemRepository;
    private final ServiceItemRepository serviceRepository;
    private final PaymentRepository paymentRepository;
    private final PatientRepository patientRepository;
    private final AppointmentRepository appointmentRepository;
    private final Icd10Service icd10Service;
    private final AuditService auditService;

    public InvoiceService(InvoiceRepository invoiceRepository, InvoiceItemRepository itemRepository,
                          ServiceItemRepository serviceRepository, PaymentRepository paymentRepository,
                          PatientRepository patientRepository,
                          AppointmentRepository appointmentRepository, Icd10Service icd10Service,
                          AuditService auditService) {
        this.invoiceRepository = invoiceRepository;
        this.itemRepository = itemRepository;
        this.serviceRepository = serviceRepository;
        this.paymentRepository = paymentRepository;
        this.patientRepository = patientRepository;
        this.appointmentRepository = appointmentRepository;
        this.icd10Service = icd10Service;
        this.auditService = auditService;
    }

    /**
     * All amounts are computed here from the catalogue so a client can never set what a patient owes.
     * Each line snapshots the current service price into unit_price; that is what keeps old invoices
     * unchanged when the catalogue price changes later. Amounts that would not fit NUMERIC(10,2) are
     * rejected with 400 before anything is written, instead of failing in the database.
     */
    @Transactional
    public InvoiceResponse create(CreateInvoiceRequest request) {
        Patient patient = patientRepository.findById(request.patientId())
                .orElseThrow(() -> new BadRequestException("Unknown patient: " + request.patientId(), "patientId"));
        Appointment appointment = null;
        if (request.appointmentId() != null) {
            appointment = appointmentRepository.findById(request.appointmentId())
                    .orElseThrow(() -> new BadRequestException("Unknown appointment: " + request.appointmentId(),
                            "appointmentId"));
            if (!appointment.getPatient().getId().equals(patient.getId())) {
                throw new BadRequestException("The appointment belongs to a different patient", "appointmentId");
            }
        }

        List<InvoiceItem> lines = new ArrayList<>();
        BigDecimal total = Money.of(BigDecimal.ZERO);
        for (int i = 0; i < request.items().size(); i++) {
            CreateInvoiceRequest.Item item = request.items().get(i);
            String field = "items[" + i + "]";
            ServiceItem service = serviceRepository.findById(item.serviceId())
                    .orElseThrow(() -> new BadRequestException("Unknown service: " + item.serviceId(), field + ".serviceId"));
            String icd10Code = null;
            if (item.icd10Code() != null && !item.icd10Code().isBlank()) {
                icd10Code = icd10Service.find(item.icd10Code())
                        .orElseThrow(() -> new BadRequestException("Unknown ICD-10 code: " + item.icd10Code(),
                                field + ".icd10Code"))
                        .getCode();
            }
            BigDecimal unitPrice = Money.of(service.getPrice());
            BigDecimal lineTotal = Money.of(unitPrice.multiply(BigDecimal.valueOf(item.quantity())));
            if (Money.exceedsMax(lineTotal)) {
                throw new BadRequestException("Line total " + lineTotal.toPlainString() + " exceeds the maximum of "
                        + Money.MAX.toPlainString(), field + ".quantity");
            }
            total = total.add(lineTotal);

            InvoiceItem line = new InvoiceItem();
            line.setService(service);
            line.setIcd10Code(icd10Code);
            line.setQuantity(item.quantity());
            line.setUnitPrice(unitPrice);
            line.setLineTotal(lineTotal);
            lines.add(line);
        }
        if (Money.exceedsMax(total)) {
            throw new BadRequestException("Invoice total " + total.toPlainString() + " exceeds the maximum of "
                    + Money.MAX.toPlainString(), "items");
        }

        Invoice invoice = new Invoice();
        invoice.setPatient(patient);
        invoice.setAppointment(appointment);
        invoice.setStatus(InvoiceStatus.UNPAID);
        invoice.setIssueDate(LocalDate.now(ZoneOffset.UTC));
        invoice.setTotalAmount(total);
        Invoice saved = invoiceRepository.save(invoice);
        lines.forEach(line -> line.setInvoice(saved));
        itemRepository.saveAll(lines);
        itemRepository.flush();

        CurrentUser actor = CurrentUser.require();
        auditService.record(actor.actorType(), actor.id(), AuditAction.INVOICE_CREATED, "INVOICE", saved.getId());
        return InvoiceResponse.from(saved, lines.stream().map(InvoiceItemResponse::from).toList(), null);
    }

    /**
     * Only UNPAID can be cancelled: PAID and CANCELLED are final, so a cancelled invoice cannot be paid later
     * and a paid one cannot disappear from revenue. The row lock makes this safe against a concurrent payment.
     */
    @Transactional
    public InvoiceResponse cancel(UUID id) {
        Invoice invoice = invoiceRepository.findForUpdate(id)
                .orElseThrow(() -> new ResourceNotFoundException("Invoice", id));
        if (invoice.getStatus() != InvoiceStatus.UNPAID) {
            throw new ConflictException("Only UNPAID invoices can be cancelled; this invoice is " + invoice.getStatus());
        }
        invoice.setStatus(InvoiceStatus.CANCELLED);
        invoice = invoiceRepository.saveAndFlush(invoice);

        CurrentUser actor = CurrentUser.require();
        auditService.record(actor.actorType(), actor.id(), AuditAction.INVOICE_CANCELLED, "INVOICE", invoice.getId());
        return withItems(invoice);
    }

    /** Clerk and admin overview; {@code status} null = all. */
    @Transactional(readOnly = true)
    public PageResponse<InvoiceResponse> list(InvoiceStatus status, int page, int size) {
        var pageable = Pages.of(page, size, NEWEST_FIRST);
        Page<Invoice> invoices = status == null ? invoiceRepository.findAll(pageable)
                : invoiceRepository.findByStatus(status, pageable);
        return toPage(invoices);
    }

    /**
     * Backs both the clerk's per-patient view and the patient's "My invoices" page; 404 for an unknown
     * patient so staff can tell "no invoices" from a wrong id (patients never get here for other ids).
     */
    @Transactional(readOnly = true)
    public PageResponse<InvoiceResponse> listForPatient(UUID patientId, InvoiceStatus status, int page, int size) {
        if (!patientRepository.existsById(patientId)) {
            throw new ResourceNotFoundException("Patient", patientId);
        }
        var pageable = Pages.of(page, size, NEWEST_FIRST);
        Page<Invoice> invoices = status == null ? invoiceRepository.findByPatientId(patientId, pageable)
                : invoiceRepository.findByPatientIdAndStatus(patientId, status, pageable);
        return toPage(invoices);
    }

    /** Loaded by invoice id, so a patient's "own" rule is enforced here. */
    @Transactional(readOnly = true)
    public InvoiceResponse get(UUID id) {
        Invoice invoice = PatientRecordAccess.requireReadable(invoiceRepository.findById(id),
                inv -> inv.getPatient().getId(), "Invoice", id);
        return withItems(invoice);
    }

    InvoiceResponse withItems(Invoice invoice) {
        List<UUID> ids = List.of(invoice.getId());
        return InvoiceResponse.from(invoice, itemsByInvoice(ids).getOrDefault(invoice.getId(), List.of()),
                paymentsByInvoice(ids).get(invoice.getId()));
    }

    private PageResponse<InvoiceResponse> toPage(Page<Invoice> invoices) {
        List<UUID> ids = invoices.getContent().stream().map(Invoice::getId).toList();
        Map<UUID, List<InvoiceItemResponse>> items = itemsByInvoice(ids);
        Map<UUID, PaymentResponse> payments = paymentsByInvoice(ids);
        return PageResponse.of(invoices, inv -> InvoiceResponse.from(inv, items.getOrDefault(inv.getId(), List.of()),
                payments.get(inv.getId())));
    }

    /** At most one payment per invoice: pay() only accepts UNPAID invoices and PAID is final. */
    private Map<UUID, PaymentResponse> paymentsByInvoice(List<UUID> invoiceIds) {
        if (invoiceIds.isEmpty()) {
            return Map.of();
        }
        return paymentRepository.findByInvoiceIdIn(invoiceIds).stream()
                .collect(Collectors.toMap(p -> p.getInvoice().getId(), PaymentResponse::from, (a, b) -> a));
    }

    private Map<UUID, List<InvoiceItemResponse>> itemsByInvoice(List<UUID> invoiceIds) {
        if (invoiceIds.isEmpty()) {
            return Map.of();
        }
        return itemRepository.findByInvoiceIdInOrderByCreatedAtAscIdAsc(invoiceIds).stream()
                .collect(Collectors.groupingBy(i -> i.getInvoice().getId(),
                        Collectors.mapping(InvoiceItemResponse::from, Collectors.toList())));
    }
}
