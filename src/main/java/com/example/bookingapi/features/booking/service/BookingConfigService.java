package com.example.bookingapi.features.booking.service;

import com.example.bookingapi.common.response.ApiMessageResponse;
import com.example.bookingapi.common.response.PagedResponse;
import com.example.bookingapi.features.booking.dto.request.CancellationPolicyRequest;
import com.example.bookingapi.features.booking.dto.request.DiscountRequest;
import com.example.bookingapi.features.booking.dto.response.CancellationPolicyResponse;
import com.example.bookingapi.features.booking.dto.response.DiscountResponse;

import java.util.UUID;

public interface BookingConfigService {
    DiscountResponse createDiscount(DiscountRequest request);
    DiscountResponse updateDiscount(UUID id, DiscountRequest request);
    PagedResponse<DiscountResponse> getDiscounts(Boolean active, int page, int size);
    ApiMessageResponse deactivateDiscount(UUID id);

    CancellationPolicyResponse createCancellationPolicy(CancellationPolicyRequest request);
    CancellationPolicyResponse updateCancellationPolicy(UUID id, CancellationPolicyRequest request);
    PagedResponse<CancellationPolicyResponse> getCancellationPolicies(UUID hotelId, int page, int size);
    ApiMessageResponse deactivateCancellationPolicy(UUID id);
}
