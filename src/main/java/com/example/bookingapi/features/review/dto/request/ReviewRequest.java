package com.example.bookingapi.features.review.dto.request;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class ReviewRequest {

    @Min(1)
    @Max(5)
    @NotNull
    private Integer rating;

    @Size(max = 100)
    private String title;

    @Size(max = 1000)
    private String comment;
}
