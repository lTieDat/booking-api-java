package com.example.bookingapi.features.receptionist.service.impl;

import com.example.bookingapi.common.security.UserPrincipal;
import com.example.bookingapi.features.booking.model.Booking;
import com.example.bookingapi.features.receptionist.repository.ReceptionistAssignmentRepository;
import com.example.bookingapi.features.receptionist.service.ReceptionistAccessService;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;

@Service
@Transactional(readOnly = true)
public class ReceptionistAccessServiceImpl implements ReceptionistAccessService {

    private final ReceptionistAssignmentRepository receptionistAssignmentRepository;

    public ReceptionistAccessServiceImpl(ReceptionistAssignmentRepository receptionistAssignmentRepository) {
        this.receptionistAssignmentRepository = receptionistAssignmentRepository;
    }

    @Override
    public void requireCanManageBooking(Booking booking, UserPrincipal currentUser) {
        if (hasRole(currentUser, "ROLE_ADMIN")) {
            return;
        }
        if (!hasRole(currentUser, "ROLE_RECEPTIONIST")) {
            throw new AccessDeniedException("Only admin or receptionist can manage booking operations");
        }

        Set<UUID> hotelIds = booking.getBookedRooms().stream()
                .map(bookedRoom -> bookedRoom.getRoomType().getHotel().getId())
                .collect(Collectors.toSet());
        if (hotelIds.isEmpty()) {
            throw new AccessDeniedException("Booking has no hotel scope");
        }

        for (UUID hotelId : hotelIds) {
            boolean assigned = receptionistAssignmentRepository
                    .existsByUser_IdAndHotel_IdAndIsActiveTrue(currentUser.getId(), hotelId);
            if (!assigned) {
                throw new AccessDeniedException("Receptionist is not assigned to this booking's hotel");
            }
        }
    }

    private boolean hasRole(UserPrincipal currentUser, String roleName) {
        return currentUser.getAuthorities().stream()
                .anyMatch(authority -> roleName.equals(authority.getAuthority()));
    }
}
