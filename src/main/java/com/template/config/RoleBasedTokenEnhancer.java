package com.template.config;

import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

import javax.transaction.Transactional;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.core.userdetails.User;
import org.springframework.security.oauth2.common.DefaultOAuth2AccessToken;
import org.springframework.security.oauth2.common.OAuth2AccessToken;
import org.springframework.security.oauth2.common.exceptions.UnapprovedClientAuthenticationException;
import org.springframework.security.oauth2.provider.OAuth2Authentication;
import org.springframework.security.oauth2.provider.token.TokenEnhancer;

import com.template.dao.UserRepository;
import com.template.model.Module;
import com.template.model.UserEntity;


public class RoleBasedTokenEnhancer implements TokenEnhancer {
	
	@Autowired
	UserRepository userRepository;

	@Override
	@Transactional
	public OAuth2AccessToken enhance(OAuth2AccessToken accessToken, OAuth2Authentication authentication) {
		User user = (User) authentication.getPrincipal();
		try {
			final List<String> allowedServicesList = new ArrayList<>();

			UserEntity userEntity = null;
			
			try {
				Long mobileNumber = Long.valueOf(user.getUsername());
				userEntity = userRepository.findByMobileNumber(mobileNumber);
			} catch (NumberFormatException e) {
				userEntity = userRepository.findByEmail(user.getUsername());
			}

			Set<Module> modulesList = userEntity.getRole().getModules();

			modulesList.forEach(module -> allowedServicesList.add(module.getModuleName()));
			
			//Set additional information
			final Map<String, Object> additionalInfo = new HashMap<>();
			additionalInfo.put("authGenTime", new Date().getTime());
			additionalInfo.put("allowedServices", allowedServicesList);

			((DefaultOAuth2AccessToken) accessToken).setAdditionalInformation(additionalInfo);
		}
		catch (Exception e) {
			throw new UnapprovedClientAuthenticationException(e.getMessage());
		}
		return accessToken;
	}

}
