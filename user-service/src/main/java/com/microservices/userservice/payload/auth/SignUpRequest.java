package com.microservices.userservice.payload.auth;

import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@NoArgsConstructor
public class SignUpRequest {

	private String name;

	private String email;

	private String password;

}
