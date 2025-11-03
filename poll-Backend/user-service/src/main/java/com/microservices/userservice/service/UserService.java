package com.microservices.userservice.service;

import java.util.Optional;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import com.microservices.userservice.model.User;
import com.microservices.userservice.payload.auth.UserDetailsResponse;
import com.microservices.userservice.payload.user.UserDetailsRequest;
import com.microservices.userservice.repository.UserRepository;
import com.microservices.userservice.util.enums.Role;

@Service
public class UserService {

	@Autowired
	private UserRepository userRepository;

	public UserDetailsResponse getUserDetails(User user) {

		UserDetailsResponse response = new UserDetailsResponse();

		if (user != null) {

			response.setId(user.getId());
			response.setName(user.getName());
			response.setEmail(user.getEmail());
			// pick a primary role if present
			if (user.getRoles() != null && !user.getRoles().isEmpty()) {
				Role r = user.getRoles().iterator().next();
				response.setRoleName(r);
			}

		}

		return response;
	}

	public UserDetailsResponse getUserDetails(UserDetailsRequest request) {

		UserDetailsResponse response = new UserDetailsResponse();

		Optional<User> user = request.getId() != null ? userRepository.findById(request.getId()) : Optional.empty();

		if (!user.isPresent() && request.getUsernameOrEmail() != null) {
			user = userRepository.findByEmail(request.getUsernameOrEmail());
		}

		if (user.isPresent()) {
			response = getUserDetails(user.get());
		}
		return response;
	}

	public UserDetailsResponse getUserDetails(Long id) {

		UserDetailsResponse response = new UserDetailsResponse();

		Optional<User> user = id != null ? userRepository.findById(id) : Optional.empty();

		if (user.isPresent()) {
			response = getUserDetails(user.get());
		}

		return response;
	}

}
