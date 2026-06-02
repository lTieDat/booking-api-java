package com.example.bookingapi.features.booking.service.impl;

import com.example.bookingapi.common.exception.BadRequestException;
import com.example.bookingapi.common.exception.ResourceNotFoundException;
import com.example.bookingapi.common.response.ApiMessageResponse;
import com.example.bookingapi.common.response.PagedResponse;
import com.example.bookingapi.features.booking.dto.request.CancellationPolicyRequest;
import com.example.bookingapi.features.booking.dto.request.DiscountRequest;
import com.example.bookingapi.features.booking.dto.response.CancellationPolicyResponse;
import com.example.bookingapi.features.booking.dto.response.DiscountResponse;
import com.example.bookingapi.features.booking.model.CancellationPolicy;
import com.example.bookingapi.features.booking.model.Discount;
import com.example.bookingapi.features.booking.model.enums.CancellationPenaltyType;
import com.example.bookingapi.features.booking.model.enums.DiscountType;
import com.example.bookingapi.features.booking.repository.CancellationPolicyRepository;
import com.example.bookingapi.features.booking.repository.DiscountRepository;
import com.example.bookingapi.features.booking.service.BookingConfigService;
import com.example.bookingapi.features.hotel.model.Hotel;
import com.example.bookingapi.features.hotel.repository.HotelRepository;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.UUID;

@Service
@Transactional(readOnly = true)
public class BookingConfigServiceImpl implements BookingConfigService {

    private final DiscountRepository discountRepository;
    private final CancellationPolicyRepository cancellationPolicyRepository;
    private final HotelRepository hotelRepository;

    public BookingConfigServiceImpl(
            DiscountRepository discountRepository,
            CancellationPolicyRepository cancellationPolicyRepository,
            HotelRepository hotelRepository
    ) {
        this.discountRepository = discountRepository;
        this.cancellationPolicyRepository = cancellationPolicyRepository;
        this.hotelRepository = hotelRepository;
    }

    @Override
    @Transactional
    public DiscountResponse createDiscount(DiscountRequest request) {
        String code = normalizeCode(request.getCode());
        if (discountRepository.existsByCodeIgnoreCase(code)) {
            throw new BadRequestException("Discount code already exists");
        }
        Discount discount = new Discount();
        mapDiscount(discount, request, code);
        discount.setUsedCount(0);
        return toDiscountResponse(discountRepository.save(discount));
    }

    @Override
    @Transactional
    public DiscountResponse updateDiscount(UUID id, DiscountRequest request) {
        Discount discount = discountRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Discount", "id", id));
        String code = normalizeCode(request.getCode());
        discountRepository.findByCodeIgnoreCase(code)
                .filter(existing -> !existing.getId().equals(id))
                .ifPresent(existing -> {
                    throw new BadRequestException("Discount code already exists");
                });
        mapDiscount(discount, request, code);
        if (discount.getUsedCount() == null) {
            discount.setUsedCount(0);
        }
        return toDiscountResponse(discountRepository.save(discount));
    }

    @Override
    public PagedResponse<DiscountResponse> getDiscounts(Boolean active, int page, int size) {
        Pageable pageable = PageRequest.of(page, size, Sort.Direction.DESC, "createdAt");
        Page<Discount> discounts = active == null
                ? discountRepository.findAll(pageable)
                : discountRepository.findByIsActive(active, pageable);
        List<DiscountResponse> content = discounts.getContent().stream()
                .map(this::toDiscountResponse)
                .toList();
        return new PagedResponse<>(content, discounts.getNumber(), discounts.getSize(),
                discounts.getTotalElements(), discounts.getTotalPages(), discounts.isLast());
    }

    @Override
    @Transactional
    public ApiMessageResponse deactivateDiscount(UUID id) {
        Discount discount = discountRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Discount", "id", id));
        discount.setIsActive(false);
        discountRepository.save(discount);
        return new ApiMessageResponse(true, "Discount deactivated successfully");
    }

    @Override
    @Transactional
    public CancellationPolicyResponse createCancellationPolicy(CancellationPolicyRequest request) {
        CancellationPolicy policy = new CancellationPolicy();
        mapCancellationPolicy(policy, request);
        return toPolicyResponse(cancellationPolicyRepository.save(policy));
    }

    @Override
    @Transactional
    public CancellationPolicyResponse updateCancellationPolicy(UUID id, CancellationPolicyRequest request) {
        CancellationPolicy policy = cancellationPolicyRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("CancellationPolicy", "id", id));
        mapCancellationPolicy(policy, request);
        return toPolicyResponse(cancellationPolicyRepository.save(policy));
    }

    @Override
    public PagedResponse<CancellationPolicyResponse> getCancellationPolicies(UUID hotelId, int page, int size) {
        Pageable pageable = PageRequest.of(page, size, Sort.Direction.DESC, "createdAt");
        Page<CancellationPolicy> policies = hotelId == null
                ? cancellationPolicyRepository.findAll(pageable)
                : cancellationPolicyRepository.findByHotel_Id(hotelId, pageable);
        List<CancellationPolicyResponse> content = policies.getContent().stream()
                .map(this::toPolicyResponse)
                .toList();
        return new PagedResponse<>(content, policies.getNumber(), policies.getSize(),
                policies.getTotalElements(), policies.getTotalPages(), policies.isLast());
    }

    @Override
    @Transactional
    public ApiMessageResponse deactivateCancellationPolicy(UUID id) {
        CancellationPolicy policy = cancellationPolicyRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("CancellationPolicy", "id", id));
        policy.setIsActive(false);
        cancellationPolicyRepository.save(policy);
        return new ApiMessageResponse(true, "Cancellation policy deactivated successfully");
    }

    private void mapDiscount(Discount discount, DiscountRequest request, String code) {
        validateDiscountRequest(request);
        discount.setCode(code);
        discount.setName(request.getName().trim());
        discount.setDiscountType(request.getDiscountType());
        discount.setDiscountValue(request.getDiscountValue());
        discount.setMinOrderValue(request.getMinOrderValue() == null ? 0 : request.getMinOrderValue());
        discount.setMaxOrderValue(request.getMaxOrderValue() == null ? 0 : request.getMaxOrderValue());
        discount.setStartDate(request.getStartDate());
        discount.setEndDate(request.getEndDate());
        discount.setIsActive(Boolean.TRUE.equals(request.getActive()));
        discount.setMaxUsage(request.getMaxUsage());
    }

    private void validateDiscountRequest(DiscountRequest request) {
        if (!request.getStartDate().isBefore(request.getEndDate())) {
            throw new BadRequestException("Discount startDate must be before endDate");
        }
        if (request.getDiscountType() == DiscountType.PERCENTAGE && request.getDiscountValue() > 100) {
            throw new BadRequestException("Percentage discount value must be at most 100");
        }
        int min = request.getMinOrderValue() == null ? 0 : request.getMinOrderValue();
        int max = request.getMaxOrderValue() == null ? 0 : request.getMaxOrderValue();
        if (max > 0 && max < min) {
            throw new BadRequestException("maxOrderValue must be greater than or equal to minOrderValue");
        }
    }

    private void mapCancellationPolicy(CancellationPolicy policy, CancellationPolicyRequest request) {
        Hotel hotel = request.getHotelId() == null
                ? null
                : hotelRepository.findById(request.getHotelId())
                .orElseThrow(() -> new ResourceNotFoundException("Hotel", "id", request.getHotelId()));
        validateCancellationPolicy(request);
        policy.setHotel(hotel);
        policy.setName(request.getName().trim());
        policy.setDescription(request.getDescription() == null ? null : request.getDescription().trim());
        policy.setFreeCancellationHours(request.getFreeCancellationHours());
        policy.setPenaltyType(request.getPenaltyType());
        policy.setPenaltyValue(request.getPenaltyValue());
        policy.setIsActive(Boolean.TRUE.equals(request.getActive()));
    }

    private void validateCancellationPolicy(CancellationPolicyRequest request) {
        if (request.getPenaltyType() == CancellationPenaltyType.NONE && request.getPenaltyValue() > 0) {
            throw new BadRequestException("Penalty value must be 0 when penalty type is NONE");
        }
        if (request.getPenaltyType() == CancellationPenaltyType.PERCENTAGE && request.getPenaltyValue() > 100) {
            throw new BadRequestException("Percentage penalty value must be at most 100");
        }
    }

    private String normalizeCode(String code) {
        return code.trim().toUpperCase();
    }

    private DiscountResponse toDiscountResponse(Discount discount) {
        return new DiscountResponse(
                discount.getId(),
                discount.getCode(),
                discount.getName(),
                discount.getDiscountType(),
                discount.getDiscountValue(),
                discount.getMinOrderValue(),
                discount.getMaxOrderValue(),
                discount.getStartDate(),
                discount.getEndDate(),
                discount.getIsActive(),
                discount.getMaxUsage(),
                discount.getUsedCount()
        );
    }

    private CancellationPolicyResponse toPolicyResponse(CancellationPolicy policy) {
        Hotel hotel = policy.getHotel();
        return new CancellationPolicyResponse(
                policy.getId(),
                hotel == null ? null : hotel.getId(),
                hotel == null ? null : hotel.getName(),
                policy.getName(),
                policy.getDescription(),
                policy.getFreeCancellationHours(),
                policy.getPenaltyType(),
                policy.getPenaltyValue(),
                policy.getIsActive()
        );
    }
}
