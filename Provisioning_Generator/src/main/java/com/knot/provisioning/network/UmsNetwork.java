
package com.knot.provisioning.network;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import com.knot.jwt.security.JwtService;

@Component
public class UmsNetwork {

	private JwtService jwtService;

	@Autowired
	public void setJwtService(JwtService jwtService) {
		this.jwtService = jwtService;
	}

	public String generateSuperUserToken() {
		return jwtService.generateToken("Super", null);
	}

}
