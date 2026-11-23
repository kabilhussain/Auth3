package com.template.config;

import java.text.ParseException;
import java.util.Arrays;
import java.util.Map;

import javax.sql.DataSource;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.PropertySource;
import org.springframework.core.env.Environment;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.oauth2.common.OAuth2AccessToken;
import org.springframework.security.oauth2.config.annotation.configurers.ClientDetailsServiceConfigurer;
import org.springframework.security.oauth2.config.annotation.web.configuration.AuthorizationServerConfigurerAdapter;
import org.springframework.security.oauth2.config.annotation.web.configuration.EnableAuthorizationServer;
import org.springframework.security.oauth2.config.annotation.web.configurers.AuthorizationServerEndpointsConfigurer;
import org.springframework.security.oauth2.config.annotation.web.configurers.AuthorizationServerSecurityConfigurer;
import org.springframework.security.oauth2.provider.OAuth2Authentication;
import org.springframework.security.oauth2.provider.token.TokenEnhancer;
import org.springframework.security.oauth2.provider.token.TokenEnhancerChain;
import org.springframework.security.oauth2.provider.token.TokenStore;
import org.springframework.security.oauth2.provider.token.store.JwtAccessTokenConverter;
import org.springframework.security.oauth2.provider.token.store.JwtTokenStore;

import com.nimbusds.jose.EncryptionMethod;
import com.nimbusds.jose.JOSEException;
import com.nimbusds.jose.JWEAlgorithm;
import com.nimbusds.jose.JWEHeader;
import com.nimbusds.jose.JWEObject;
import com.nimbusds.jose.Payload;
import com.nimbusds.jose.crypto.RSADecrypter;
import com.nimbusds.jose.crypto.RSAEncrypter;
import com.nimbusds.jose.jwk.JWK;
import com.nimbusds.jose.jwk.RSAKey;
import com.nimbusds.jwt.EncryptedJWT;
import com.nimbusds.jwt.SignedJWT;

/**
 * @author karthikshastry
 *
 */
@Configuration
@EnableAuthorizationServer
@PropertySource("classpath:application.properties")
@PropertySource("classpath:jwt.yml")
public class OAuthConfiguration extends AuthorizationServerConfigurerAdapter {

	@Autowired
	private Environment env;

	@Autowired
	@Qualifier("authenticationManagerBean")
	private AuthenticationManager authenticationManager;

	@Value("${privateKey}")
	private String privateKey;

	@Value("${publicKey}")
	private String publicKey;

	@Autowired
	SecurityConfiguration secConfig;
	
	@Autowired
	DataSource dataSource;

	@Override
	public void configure(AuthorizationServerSecurityConfigurer oauthServer) throws Exception {
		oauthServer.tokenKeyAccess("permitAll()").checkTokenAccess("isAuthenticated()");
	}

	@Override
	public void configure(ClientDetailsServiceConfigurer clients) throws Exception {
		clients.jdbc(dataSource);
	}

	@Override
	public void configure(AuthorizationServerEndpointsConfigurer endpoints) throws Exception {
		TokenEnhancerChain tokenEnhancerChain = new TokenEnhancerChain();
		tokenEnhancerChain.setTokenEnhancers(Arrays.asList(jwtTokenEnhancer()));
		endpoints.tokenStore(tokenStore())
				 .tokenEnhancer(tokenEnhancerChain);
	}

	@Bean
	public TokenStore tokenStore() {
		return new JwtTokenStore(jwtTokenEnhancer());
	}

	@Bean
	protected JwtAccessTokenConverter jwtTokenEnhancer() {
		JwtAccessTokenConverter converter = new JwtJweAccessTokenConverter();
		converter.setSigningKey(privateKey);
		converter.setVerifierKey(publicKey);
		return converter;
	}

	@Bean
	public TokenEnhancer tokenEnhancer() {
		return new RoleBasedTokenEnhancer();
	}

	public class JwtJweAccessTokenConverter extends JwtAccessTokenConverter {

		RSAKey recipientPublicJWK;
		RSAKey recipientJWK;

		public JwtJweAccessTokenConverter() {
			try {
				JWK parsedKey = JWK.parseFromPEMEncodedObjects(publicKey);
				recipientPublicJWK = parsedKey.toRSAKey();
				
				JWK parsedPrivateKey = RSAKey.parseFromPEMEncodedObjects(privateKey);
				recipientJWK = parsedPrivateKey.toRSAKey();
			} catch (Exception e) {
				e.printStackTrace();
			}
		}

		@Override
		protected String encode(OAuth2AccessToken accessToken, OAuth2Authentication authentication) {
			String jwt = super.encode(accessToken, authentication);
			try {
				// jwt is already signed at this point (by JwtAccessTokenConverter)
				SignedJWT parsed = SignedJWT.parse(jwt);

				// Create JWE object with signed JWT as payload
				JWEObject jweObject = new JWEObject(
						new JWEHeader.Builder(JWEAlgorithm.RSA_OAEP_256, EncryptionMethod.A256GCM).contentType("JWT")
								.build(),
						new Payload(parsed));

				// Encrypt with the recipient's public key
				jweObject.encrypt(new RSAEncrypter(recipientPublicJWK));

				// Serialise to JWE compact form
				return jweObject.serialize();
			} catch (Exception e) {
				e.printStackTrace();
			}

			return jwt;
		}
		
		@Override
		protected Map<String, Object> decode(String token) {
			try {
				// basically treat the incoming token as an encrypted JWT
				EncryptedJWT parse = EncryptedJWT.parse(token);
				// decrypt it
				RSADecrypter dec = new RSADecrypter(recipientJWK);
				parse.decrypt(dec);
				// content of the encrypted token is a signed JWT (signed by
				// JwtAccessTokenConverter)
				SignedJWT signedJWT = parse.getPayload().toSignedJWT();
				// pass on the serialized, signed JWT to JwtAccessTokenConverter
				return super.decode(signedJWT.serialize());
			} catch (ParseException e) {
				e.printStackTrace();
			} catch (JOSEException e) {
				e.printStackTrace();
			}

			return super.decode(token);
		}

	}
}