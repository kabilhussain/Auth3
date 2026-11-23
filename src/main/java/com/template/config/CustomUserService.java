package com.template.config;

import java.util.Arrays;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.userdetails.User;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.stereotype.Component;

import com.template.dao.UserRepository;
import com.template.model.CustomUserDetails;
import com.template.model.UserEntity;

@Component
public class CustomUserService implements UserDetailsService {
	
	@Autowired
	UserRepository userRepository;

	@Override
	public UserDetails loadUserByUsername(String username) throws UsernameNotFoundException {
		UserEntity userEntity = null;
		try {
			Long mobileNumber = Long.valueOf(username);
			userEntity = userRepository.findByMobileNumber(mobileNumber);
		} catch (NumberFormatException e) {
			userEntity = userRepository.findByEmail(username);
		}
		
		return new CustomUserDetails(String.valueOf(userEntity.getMobileNumber()), "password", true, true,
				true, true, Arrays.asList(new SimpleGrantedAuthority("ROLE")), userEntity.getPasswordSalt());
	}

}
