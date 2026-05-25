package com.example.bookingapi.controller;

import com.example.bookingapi.payload.response.UserIdentityAvailability;
import com.example.bookingapi.payload.response.UserProfile;
import com.example.bookingapi.payload.response.UserSummary;
import com.example.bookingapi.security.CurrentUser;
import com.example.bookingapi.security.UserPrincipal;
import com.example.bookingapi.service.UserService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;

@RestController
@RequestMapping("/api/users")
@Tag(name = "Users", description = "User profile and availability endpoints")
public class UserController {

    @Autowired
    private UserService userService;

    @GetMapping("/me")
    @SecurityRequirement(name = "bearerAuth")
    @Operation(summary = "Get current user", description = "Return the currently authenticated user's summary.")
    @ApiResponses({
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200", description = "Current user returned successfully.",
                    content = @Content(schema = @Schema(implementation = UserSummary.class))),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "401", ref = "#/components/responses/Unauthorized"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "500", ref = "#/components/responses/InternalServerError")
    })
    public ResponseEntity<UserSummary> getCurrentUser(@Parameter(hidden = true) @CurrentUser UserPrincipal currentUser) {
        return ResponseEntity.ok(userService.getCurrentUser(currentUser));
    }

    @GetMapping("/checkUsernameAvailability")
    @Operation(summary = "Check username availability", description = "Check whether a username is available.")
    @ApiResponses({
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200", description = "Username availability returned successfully.",
                    content = @Content(schema = @Schema(implementation = UserIdentityAvailability.class))),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "400", ref = "#/components/responses/BadRequest"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "500", ref = "#/components/responses/InternalServerError")
    })
    public ResponseEntity<UserIdentityAvailability> checkUsernameAvailability(@RequestParam String username) {
        return ResponseEntity.ok(userService.checkUsernameAvailability(username));
    }

    @GetMapping("/checkEmailAvailability")
    @Operation(summary = "Check email availability", description = "Check whether an email is available.")
    @ApiResponses({
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200", description = "Email availability returned successfully.",
                    content = @Content(schema = @Schema(implementation = UserIdentityAvailability.class))),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "400", ref = "#/components/responses/BadRequest"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "500", ref = "#/components/responses/InternalServerError")
    })
    public ResponseEntity<UserIdentityAvailability> checkEmailAvailability(@RequestParam String email) {
        return ResponseEntity.ok(userService.checkEmailAvailability(email));
    }

    @GetMapping("/{username}")
    @Operation(summary = "Get user profile", description = "Return a public profile for a given username.")
    @ApiResponses({
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200", description = "User profile returned successfully.",
                    content = @Content(schema = @Schema(implementation = UserProfile.class))),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "400", ref = "#/components/responses/BadRequest"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "404", ref = "#/components/responses/NotFound"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "500", ref = "#/components/responses/InternalServerError")
    })
    public ResponseEntity<UserProfile> getUserProfile(@PathVariable String username) {
        return ResponseEntity.ok(userService.getUserProfile(username));
    }
}
