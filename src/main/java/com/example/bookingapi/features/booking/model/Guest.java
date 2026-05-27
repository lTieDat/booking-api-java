package com.example.bookingapi.features.booking.model;

import com.example.bookingapi.common.audit.DateAudit;
import com.example.bookingapi.features.hotel.model.Location;
import jakarta.persistence.*;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.util.HashSet;
import java.util.Set;
import java.util.UUID;

@Entity
@Table(name = "guests",
    uniqueConstraints = {
        @UniqueConstraint(columnNames = "identify_card_no")
    })
@Getter
@Setter
@NoArgsConstructor
public class Guest extends DateAudit {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @NotBlank
    @Column(length = 40, nullable = false, name = "first_name")
    private String firstName;

    @NotBlank
    @Column(length = 40, nullable = false, name = "last_name")
    private String lastName;

    @Column(length = 40, nullable = true, name = "middle_name")
    private String middleName;

    @NotBlank
    @Column(length = 20, name = "identify_card_no", nullable = false, unique = true)
    private String identifyCardNo;

    @NotBlank
    @Column(length = 20, name = "phone_number", nullable = false)
    private String phoneNumber;

    @OneToOne(fetch = FetchType.LAZY, cascade = CascadeType.ALL)
    private Location location;
}
