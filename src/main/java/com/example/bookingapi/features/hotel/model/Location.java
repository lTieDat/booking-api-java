package com.example.bookingapi.features.hotel.model;

import com.example.bookingapi.common.audit.DateAudit;
import jakarta.persistence.*;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

@Entity
@Table(name = "locations")
@Getter
@Setter
@NoArgsConstructor
public class Location extends DateAudit {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @NotBlank
    @Size(max = 100)
    @Column(nullable = false, length = 100)
    private String country;

    @NotBlank
    @Size(max = 100)
    @Column(nullable = false, length = 100)
    private String city;

    @Size(max = 100)
    @Column(length = 100)
    private String province;

    @Size(max = 100)
    @Column(length = 100)
    private String district;

    @Size(max = 250)
    @Column(length = 250)
    private String detail;

    @OneToMany(mappedBy = "location")
    private List<Hotel> hotels = new ArrayList<>();
}
