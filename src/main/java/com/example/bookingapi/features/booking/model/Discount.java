package com.example.bookingapi.features.booking.model;

import com.example.bookingapi.common.audit.UserDateAudit;
import com.example.bookingapi.features.booking.model.enums.DiscountType;
import jakarta.persistence.*;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Positive;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.util.Date;
import java.util.UUID;

@Entity
@Table(
        name = "discounts",
        uniqueConstraints = {
                 @UniqueConstraint(columnNames = "code")
        }
)
@Getter
@Setter
@NoArgsConstructor
public class Discount extends UserDateAudit {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @Column(name = "code", nullable = false, length = 20, unique = true)
    private String code;

    @NotBlank
    @Column(name = "name", nullable = false, length = 100)
    private String name;

    @Column(name = "discount_type", nullable = false, length = 30)
    @Enumerated(EnumType.STRING)
    private DiscountType discountType = DiscountType.FIXED_AMOUNT;

    @Column(name = "min_order_value", nullable = false)
    private Integer minOrderValue;

    @Column(name = "max_order_value", nullable = false)
    private Integer maxOrderValue;

    @Column(name = "start_date", nullable = false)
    private Date startDate;

    @Column(name = "end_date", nullable = false)
    private Date endDate;

    @Column(name = "is_active")
    private Boolean isActive = false;

    @Column(name = "max_usage")
    @Positive
    private Integer maxUsage;

    @Column(name = "used_count")
    @Positive
    private Integer usedCount;
}
