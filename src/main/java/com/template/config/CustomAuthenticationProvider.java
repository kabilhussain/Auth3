package com.template.config;

import java.util.Arrays;
import java.util.Base64;
import java.util.Collection;

import javax.transaction.Transactional;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.authentication.AuthenticationProvider;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.security.oauth2.common.exceptions.UnapprovedClientAuthenticationException;
import org.springframework.stereotype.Component;

import com.template.dao.UserRepository;
import com.template.model.CustomUserDetails;
import com.template.model.RoleEntity;
import com.template.model.UserEntity;

@Component
public class CustomAuthenticationProvider implements AuthenticationProvider {

	@Autowired
	private UserRepository userRepository;
	
	@Override
	@Transactional
	public Authentication authenticate(Authentication authentication) {
		String username = authentication.getName();
		String password = (String) authentication.getCredentials();

		// Get user entity from DB
		UserEntity userEntity = loadUserEntityByUsername(username);

		// Base-64 decode the string
		String decodedPassword = new String(Base64.getDecoder().decode(password));

		// Check if the given password matches the salted password hash
		if (!PasswordUtil.isExpectedPassword(decodedPassword.toCharArray(),
				Base64.getDecoder().decode(userEntity.getPasswordSalt()),
				Base64.getDecoder().decode(userEntity.getPassword()))) {
			throw new BadCredentialsException("Wrong password.");
		}

		// Prepare Custom User Details
		CustomUserDetails customUserDetails = null;

		try {
			if (userEntity.getStatus() == 1 || userEntity.getStatus() == 2) {

				RoleEntity roleEntity = userEntity.getRole();

				// assign grant authority based on role
				GrantedAuthority authority = new SimpleGrantedAuthority(roleEntity.getName());

				customUserDetails = new CustomUserDetails(String.valueOf(userEntity.getMobileNumber()), password, true, true,
						true, true, Arrays.asList(authority), userEntity.getPasswordSalt());

			} else {
				throw new UnapprovedClientAuthenticationException("Username " + username + " not Active");
			}
		} catch (Exception e) {
			throw new UsernameNotFoundException("Username " + username + " not found");
		}

		Collection<? extends GrantedAuthority> authorities = customUserDetails.getAuthorities();

		return new UsernamePasswordAuthenticationToken(customUserDetails, password, authorities);
	}

	public boolean supports(Class<?> arg0) {
		return true;
	}
	
	public UserEntity loadUserEntityByUsername(String username) {
		UserEntity userEntity = null;
		try {
			Long mobileNumber = Long.valueOf(username);
			userEntity = userRepository.findByMobileNumber(mobileNumber);
		} catch (NumberFormatException e) {
			userEntity = userRepository.findByEmail(username);
		}

		return userEntity;
	}
	
}