package com.example.bookingapi.features.invoice.service.impl;

import com.example.bookingapi.common.exception.BadRequestException;
import com.example.bookingapi.common.exception.ResourceNotFoundException;
import com.example.bookingapi.common.security.UserPrincipal;
import com.example.bookingapi.features.booking.model.BookedRoom;
import com.example.bookingapi.features.booking.model.Booking;
import com.example.bookingapi.features.hotel.model.Hotel;
import com.example.bookingapi.features.hotel.repository.HotelRepository;
import com.example.bookingapi.features.invoice.dto.request.TaxConfigRequest;
import com.example.bookingapi.features.invoice.dto.response.BookingTaxResponse;
import com.example.bookingapi.features.invoice.dto.response.InvoiceLineResponse;
import com.example.bookingapi.features.invoice.dto.response.InvoiceResponse;
import com.example.bookingapi.features.invoice.dto.response.TaxConfigResponse;
import com.example.bookingapi.features.invoice.model.BookingTax;
import com.example.bookingapi.features.invoice.model.Invoice;
import com.example.bookingapi.features.invoice.model.InvoiceLine;
import com.example.bookingapi.features.invoice.model.TaxConfig;
import com.example.bookingapi.features.invoice.model.enums.InvoiceLineType;
import com.example.bookingapi.features.invoice.model.enums.InvoiceStatus;
import com.example.bookingapi.features.invoice.model.enums.TaxApplyType;
import com.example.bookingapi.features.invoice.repository.BookingTaxRepository;
import com.example.bookingapi.features.invoice.repository.InvoiceRepository;
import com.example.bookingapi.features.invoice.repository.TaxConfigRepository;
import com.example.bookingapi.features.invoice.service.InvoiceService;
import com.example.bookingapi.features.payment.model.Payment;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDateTime;
import java.time.temporal.ChronoUnit;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.ThreadLocalRandom;

@Service
@Transactional(readOnly = true)
public class InvoiceServiceImpl implements InvoiceService {

    private final InvoiceRepository invoiceRepository;
    private final TaxConfigRepository taxConfigRepository;
    private final BookingTaxRepository bookingTaxRepository;
    private final HotelRepository hotelRepository;

    public InvoiceServiceImpl(
            InvoiceRepository invoiceRepository,
            TaxConfigRepository taxConfigRepository,
            BookingTaxRepository bookingTaxRepository,
            HotelRepository hotelRepository
    ) {
        this.invoiceRepository = invoiceRepository;
        this.taxConfigRepository = taxConfigRepository;
        this.bookingTaxRepository = bookingTaxRepository;
        this.hotelRepository = hotelRepository;
    }

    @Override
    @Transactional
    public InvoiceResponse generatePaidInvoiceForPayment(Payment payment) {
        return invoiceRepository.findByPayment_Id(payment.getId())
                .map(invoice -> {
                    markPaid(invoice, payment);
                    return toInvoiceResponse(invoiceRepository.save(invoice));
                })
                .orElseGet(() -> toInvoiceResponse(createPaidInvoice(payment)));
    }

    @Override
    public InvoiceResponse getInvoice(UUID invoiceId, UserPrincipal currentUser) {
        Invoice invoice = invoiceRepository.findById(invoiceId)
                .orElseThrow(() -> new ResourceNotFoundException("Invoice", "id", invoiceId));
        assertCanView(invoice, currentUser);
        return toInvoiceResponse(invoice);
    }

    @Override
    public InvoiceResponse getInvoiceByPayment(UUID paymentId, UserPrincipal currentUser) {
        Invoice invoice = invoiceRepository.findByPayment_Id(paymentId)
                .orElseThrow(() -> new ResourceNotFoundException("Invoice", "paymentId", paymentId));
        assertCanView(invoice, currentUser);
        return toInvoiceResponse(invoice);
    }

    @Override
    public List<TaxConfigResponse> getTaxConfigs() {
        return taxConfigRepository.findAll().stream()
                .map(this::toTaxConfigResponse)
                .toList();
    }

    @Override
    @Transactional
    public TaxConfigResponse createTaxConfig(TaxConfigRequest request) {
        TaxConfig taxConfig = new TaxConfig();
        mapTaxConfig(taxConfig, request);
        return toTaxConfigResponse(taxConfigRepository.save(taxConfig));
    }

    @Override
    @Transactional
    public TaxConfigResponse updateTaxConfig(UUID id, TaxConfigRequest request) {
        TaxConfig taxConfig = taxConfigRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("TaxConfig", "id", id));
        mapTaxConfig(taxConfig, request);
        return toTaxConfigResponse(taxConfigRepository.save(taxConfig));
    }

    private Invoice createPaidInvoice(Payment payment) {
        Booking booking = payment.getBooking();
        long nights = ChronoUnit.DAYS.between(
                booking.getCheckInDateTime().toLocalDate(),
                booking.getCheckOutDateTime().toLocalDate()
        );
        long subtotalMinor = booking.getBookedRooms().stream()
                .mapToLong(bookedRoom -> toMinor(
                        bookedRoom.getUnitPrice()
                                .multiply(BigDecimal.valueOf(nights))
                                .multiply(BigDecimal.valueOf(bookedRoom.getQuantity())),
                        "Booked room total must be integer VND"
                ))
                .sum();
        long discountMinor = toMinor(booking.getDiscountAmount(), "Booking discount must be integer VND");
        long taxableSubtotalMinor = Math.max(0L, subtotalMinor - discountMinor);

        Invoice invoice = new Invoice();
        invoice.setBooking(booking);
        invoice.setPayment(payment);
        invoice.setInvoiceNo(generateInvoiceNo());
        invoice.setCurrency(payment.getCurrency());
        invoice.setSubtotalMinor(subtotalMinor);
        invoice.setDiscountMinor(discountMinor);
        invoice.setStatus(InvoiceStatus.PAID);
        invoice.setIssuedAt(LocalDateTime.now());
        invoice.setPaidAt(payment.getPaidAt() == null ? LocalDateTime.now() : payment.getPaidAt());

        for (BookedRoom bookedRoom : booking.getBookedRooms()) {
            invoice.addLine(buildRoomLine(bookedRoom, nights));
        }
        if (discountMinor > 0) {
            invoice.addLine(buildDiscountLine(booking, discountMinor));
        }

        long taxMinor = 0L;
        for (TaxConfig taxConfig : applicableTaxes(booking)) {
            long amountMinor = calculateTaxMinor(taxConfig, taxableSubtotalMinor, booking, nights);
            BookingTax bookingTax = snapshotBookingTax(booking, taxConfig, amountMinor);
            bookingTaxRepository.save(bookingTax);
            taxMinor += taxConfig.getInclusive() ? 0L : amountMinor;
            if (amountMinor > 0) {
                invoice.addLine(buildTaxLine(taxConfig, amountMinor));
            }
        }

        invoice.setTaxMinor(taxMinor);
        invoice.setTotalMinor(subtotalMinor - invoice.getDiscountMinor() + taxMinor);
        return invoiceRepository.save(invoice);
    }

    private void markPaid(Invoice invoice, Payment payment) {
        if (invoice.getStatus() != InvoiceStatus.PAID) {
            invoice.setStatus(InvoiceStatus.PAID);
            invoice.setPaidAt(payment.getPaidAt() == null ? LocalDateTime.now() : payment.getPaidAt());
        }
    }

    private InvoiceLine buildRoomLine(BookedRoom bookedRoom, long nights) {
        long unitMinor = toMinor(bookedRoom.getUnitPrice().multiply(BigDecimal.valueOf(nights)), "Booked room price must be integer VND");
        InvoiceLine line = new InvoiceLine();
        line.setLineType(InvoiceLineType.ROOM);
        line.setDescription(resolveRoomDescription(bookedRoom, nights));
        line.setQuantity(bookedRoom.getQuantity());
        line.setUnitMinor(unitMinor);
        line.setTotalMinor(unitMinor * bookedRoom.getQuantity());
        line.setMetadata("roomTypeId=" + bookedRoom.getRoomType().getId());
        return line;
    }

    private InvoiceLine buildTaxLine(TaxConfig taxConfig, long amountMinor) {
        InvoiceLine line = new InvoiceLine();
        line.setLineType(InvoiceLineType.TAX);
        line.setDescription(taxConfig.getName());
        line.setQuantity(1);
        line.setUnitMinor(amountMinor);
        line.setTotalMinor(amountMinor);
        line.setMetadata("taxConfigId=" + taxConfig.getId());
        return line;
    }

    private InvoiceLine buildDiscountLine(Booking booking, long discountMinor) {
        InvoiceLine line = new InvoiceLine();
        line.setLineType(InvoiceLineType.DISCOUNT);
        line.setDescription(booking.getDiscountCodeSnapshot() == null
                ? "Booking discount"
                : "Booking discount " + booking.getDiscountCodeSnapshot());
        line.setQuantity(1);
        line.setUnitMinor(-discountMinor);
        line.setTotalMinor(-discountMinor);
        line.setMetadata(booking.getDiscount() == null ? null : "discountId=" + booking.getDiscount().getId());
        return line;
    }

    private BookingTax snapshotBookingTax(Booking booking, TaxConfig taxConfig, long amountMinor) {
        BookingTax bookingTax = new BookingTax();
        bookingTax.setBooking(booking);
        bookingTax.setTaxConfig(taxConfig);
        bookingTax.setTaxName(taxConfig.getName());
        bookingTax.setApplyType(taxConfig.getApplyType());
        bookingTax.setRate(taxConfig.getRate());
        bookingTax.setAmountMinor(amountMinor);
        bookingTax.setInclusive(taxConfig.getInclusive());
        return bookingTax;
    }

    private List<TaxConfig> applicableTaxes(Booking booking) {
        UUID hotelId = resolveHotelId(booking);
        List<TaxConfig> globalTaxes = taxConfigRepository.findByActiveTrue().stream()
                .filter(taxConfig -> taxConfig.getHotel() == null)
                .toList();
        if (hotelId == null) {
            return globalTaxes;
        }
        List<TaxConfig> hotelTaxes = taxConfigRepository.findByHotel_IdAndActiveTrue(hotelId);
        return java.util.stream.Stream.concat(globalTaxes.stream(), hotelTaxes.stream()).toList();
    }

    private UUID resolveHotelId(Booking booking) {
        return booking.getBookedRooms().stream()
                .findFirst()
                .map(bookedRoom -> bookedRoom.getRoomType().getHotel().getId())
                .orElse(null);
    }

    private long calculateTaxMinor(TaxConfig taxConfig, long subtotalMinor, Booking booking, long nights) {
        if (taxConfig.getApplyType() == TaxApplyType.PERCENTAGE) {
            BigDecimal rate = taxConfig.getRate() == null ? BigDecimal.ZERO : taxConfig.getRate();
            return BigDecimal.valueOf(subtotalMinor)
                    .multiply(rate)
                    .divide(BigDecimal.valueOf(100), 0, RoundingMode.HALF_UP)
                    .longValueExact();
        }
        if (taxConfig.getApplyType() == TaxApplyType.FIXED_PER_BOOKING) {
            return taxConfig.getAmountMinor() == null ? 0L : taxConfig.getAmountMinor();
        }
        long roomNights = booking.getBookedRooms().stream()
                .mapToLong(bookedRoom -> (long) bookedRoom.getQuantity() * nights)
                .sum();
        return (taxConfig.getAmountMinor() == null ? 0L : taxConfig.getAmountMinor()) * roomNights;
    }

    private void mapTaxConfig(TaxConfig taxConfig, TaxConfigRequest request) {
        taxConfig.setName(request.getName().trim());
        taxConfig.setApplyType(request.getApplyType());
        taxConfig.setRate(request.getRate());
        taxConfig.setAmountMinor(request.getAmountMinor());
        taxConfig.setInclusive(Boolean.TRUE.equals(request.getInclusive()));
        taxConfig.setActive(request.getActive() == null || request.getActive());
        taxConfig.setHotel(request.getHotelId() == null
                ? null
                : hotelRepository.findById(request.getHotelId())
                .orElseThrow(() -> new ResourceNotFoundException("Hotel", "id", request.getHotelId())));
        validateTaxConfig(taxConfig);
    }

    private void validateTaxConfig(TaxConfig taxConfig) {
        if (taxConfig.getApplyType() == TaxApplyType.PERCENTAGE && taxConfig.getRate() == null) {
            throw new BadRequestException("Percentage tax requires rate");
        }
        if (taxConfig.getApplyType() != TaxApplyType.PERCENTAGE && taxConfig.getAmountMinor() == null) {
            throw new BadRequestException("Fixed tax requires amountMinor");
        }
    }

    private void assertCanView(Invoice invoice, UserPrincipal currentUser) {
        boolean isOwner = invoice.getBooking().getUser().getId().equals(currentUser.getId());
        boolean isAdmin = currentUser.getAuthorities().stream()
                .anyMatch(authority -> "ROLE_ADMIN".equals(authority.getAuthority()));
        if (!isOwner && !isAdmin) {
            throw new AccessDeniedException("You don't have permission to view this invoice");
        }
    }

    private InvoiceResponse toInvoiceResponse(Invoice invoice) {
        return new InvoiceResponse(
                invoice.getId(),
                invoice.getBooking().getId(),
                invoice.getPayment() == null ? null : invoice.getPayment().getId(),
                invoice.getInvoiceNo(),
                invoice.getStatus().name(),
                invoice.getSubtotalMinor(),
                invoice.getDiscountMinor(),
                invoice.getTaxMinor(),
                invoice.getTotalMinor(),
                invoice.getCurrency(),
                invoice.getIssuedAt(),
                invoice.getPaidAt(),
                invoice.getLines().stream().map(this::toLineResponse).toList(),
                bookingTaxRepository.findByBooking_Id(invoice.getBooking().getId()).stream()
                        .map(this::toBookingTaxResponse)
                        .toList()
        );
    }

    private InvoiceLineResponse toLineResponse(InvoiceLine line) {
        return new InvoiceLineResponse(
                line.getId(),
                line.getLineType().name(),
                line.getDescription(),
                line.getQuantity(),
                line.getUnitMinor(),
                line.getTotalMinor(),
                line.getMetadata()
        );
    }

    private BookingTaxResponse toBookingTaxResponse(BookingTax tax) {
        return new BookingTaxResponse(
                tax.getId(),
                tax.getTaxConfig() == null ? null : tax.getTaxConfig().getId(),
                tax.getTaxName(),
                tax.getApplyType().name(),
                tax.getRate(),
                tax.getAmountMinor(),
                tax.getInclusive()
        );
    }

    private TaxConfigResponse toTaxConfigResponse(TaxConfig taxConfig) {
        Hotel hotel = taxConfig.getHotel();
        return new TaxConfigResponse(
                taxConfig.getId(),
                hotel == null ? null : hotel.getId(),
                taxConfig.getName(),
                taxConfig.getApplyType().name(),
                taxConfig.getRate(),
                taxConfig.getAmountMinor(),
                taxConfig.getInclusive(),
                taxConfig.getActive()
        );
    }

    private long toMinor(BigDecimal value, String errorMessage) {
        try {
            return value.setScale(0, RoundingMode.UNNECESSARY).longValueExact();
        } catch (ArithmeticException ex) {
            throw new BadRequestException(errorMessage);
        }
    }

    private String resolveRoomDescription(BookedRoom bookedRoom, long nights) {
        String name = bookedRoom.getRoomTypeNameSnapshot();
        if (name == null || name.isBlank()) {
            name = "Room " + bookedRoom.getRoomType().getId();
        }
        return name + " x " + nights + " night(s)";
    }

    private String generateInvoiceNo() {
        String invoiceNo;
        do {
            invoiceNo = "INV" + System.currentTimeMillis() + ThreadLocalRandom.current().nextInt(100, 1000);
        } while (invoiceRepository.existsByInvoiceNo(invoiceNo));
        return invoiceNo;
    }
}
