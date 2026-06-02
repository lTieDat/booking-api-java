package com.example.bookingapi.features.receptionist.service;

import com.example.bookingapi.common.response.ApiMessageResponse;
import com.example.bookingapi.common.response.PagedResponse;
import com.example.bookingapi.common.security.UserPrincipal;
import com.example.bookingapi.features.booking.dto.response.BookingResponse;
import com.example.bookingapi.features.booking.model.enums.BookingStatus;
import com.example.bookingapi.features.receptionist.dto.request.ReceptionistAssignmentRequest;
import com.example.bookingapi.features.receptionist.dto.response.ReceptionistAssignmentResponse;
import com.example.bookingapi.features.receptionist.dto.response.ReceptionistHotelResponse;

import java.util.List;
import java.util.UUID;

public interface ReceptionistService {
    ReceptionistAssignmentResponse assignReceptionist(
            ReceptionistAssignmentRequest request,
            UserPrincipal currentUser
    );

    PagedResponse<ReceptionistAssignmentResponse> getAssignments(
            UUID userId,
            UUID hotelId,
            Boolean active,
            int page,
            int size
    );

    ApiMessageResponse deactivateAssignment(UUID id);

    List<ReceptionistHotelResponse> getMyHotels(UserPrincipal currentUser);

    PagedResponse<BookingResponse> getHotelBookings(
            UUID hotelId,
            BookingStatus status,
            int page,
            int size,
            UserPrincipal currentUser
    );
}
