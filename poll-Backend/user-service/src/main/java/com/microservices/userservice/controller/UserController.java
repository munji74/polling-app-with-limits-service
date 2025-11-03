package com.microservices.userservice.controller;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import com.microservices.userservice.payload.auth.UserDetailsResponse;
import com.microservices.userservice.payload.user.UserDetailsRequest;
import com.microservices.userservice.service.UserService;
import org.springframework.web.server.ResponseStatusException;
import org.springframework.http.HttpStatus;

@RestController
@RequestMapping("/user")
public class UserController {

	@Autowired
	private UserService userService;

	/*
	 * Find User Details by Payload Request
	 */
	@PostMapping("/get-user-details")
	public UserDetailsResponse getUserDetails(@RequestBody UserDetailsRequest request) {
		return userService.getUserDetails(request);
	}

	/*
	 * Find User Details by id path variable
	 */
	@PostMapping("/get-user-details/{id}")
	public UserDetailsResponse getUserDetailsById(@PathVariable Long id) {
		return userService.getUserDetails(id);
	}

	// New: Get current user by X-User-Id header (used when gateway forwards user id)
	@GetMapping("/me")
	public UserDetailsResponse getCurrentUser(@RequestHeader(name = "X-User-Id", required = false) String userIdHeader) {
		if (userIdHeader == null || userIdHeader.isBlank()) {
			throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Missing X-User-Id header");
		}
		try {
			Long id = Long.valueOf(userIdHeader);
			return userService.getUserDetails(id);
		} catch (NumberFormatException ex) {
			throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Invalid X-User-Id header");
		}
	}

}
