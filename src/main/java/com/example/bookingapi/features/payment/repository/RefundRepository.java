package com.example.bookingapi.features.payment.repository;

import com.example.bookingapi.features.payment.model.Refund;
import com.example.bookingapi.features.payment.model.enums.RefundStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Collection;
import java.util.List;
import java.util.UUID;

@Repository
public interface RefundRepository extends JpaRepository<Refund, UUID> {
    List<Refund> findByPayment_Id(UUID paymentId);
    List<Refund> findByPayment_IdAndStatusIn(UUID paymentId, Collection<RefundStatus> statuses);
}
