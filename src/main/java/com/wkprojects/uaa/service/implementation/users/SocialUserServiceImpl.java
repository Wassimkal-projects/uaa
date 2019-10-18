/**
 * Copyright (c) 2019. CreatedBy Wassim KALBOUSSI.  All rights reserved.
 */

package com.wkprojects.uaa.service.implementation.users;

import com.google.gson.Gson;
import com.wkprojects.uaa.domain.users.User;
import com.wkprojects.uaa.model.FacebookUserModel;
import com.wkprojects.uaa.model.GoogleUserModel;
import com.wkprojects.uaa.model.SocialToken;
import com.wkprojects.uaa.repository.users.UserRepository;
import com.wkprojects.uaa.security.jwt.TokenProvider;
import com.wkprojects.uaa.service.interfaces.users.ISocialUserService;
import com.wkprojects.uaa.utils.GoogleApiUtils;
import com.wkprojects.uaa.web.rest.errors.BadRequestException;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.authority.AuthorityUtils;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.social.facebook.api.Facebook;
import org.springframework.social.facebook.api.impl.FacebookTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.io.IOException;
import java.security.GeneralSecurityException;
import java.util.Optional;

@Service
@Transactional
public class SocialUserServiceImpl implements ISocialUserService {
    private static final Logger logger = LogManager.getLogger(SocialUserServiceImpl.class);
    private final UserRepository userRepository;
    private final TokenProvider tokenProvider;
    private Gson gson = new Gson();

    public SocialUserServiceImpl(UserRepository userRepository, TokenProvider tokenProvider) {
        this.userRepository = userRepository;
        this.tokenProvider = tokenProvider;
    }


    @Override
    public String authenticateFacebookUser(SocialToken fbToken) {
        logger.info("token: {}", fbToken.getAccessToken());
        Facebook facebook = new FacebookTemplate(fbToken.getAccessToken());
        String[] fields = {"id", "name", "email", "first_name", "last_name"};

        FacebookUserModel facebookUser = gson.fromJson(facebook.fetchObject("me", String.class, fields), FacebookUserModel.class);
        return authenticateSocialUser(facebookUser.getEmail());

    }

    @Override
    public String authenticateGoogleUser(SocialToken googleToken) throws GeneralSecurityException, IOException {
        GoogleUserModel googleUser = GoogleApiUtils.checkIfGoogleTokenIsValid(googleToken.getAccessToken()).orElseThrow(
                () -> new BadRequestException("Google Authenticaiton failded, Token not valid")
        );
        return authenticateSocialUser(googleUser.getEmail());
    }

    private String authenticateSocialUser(String email) {
        Optional<User> user = userRepository.findOneByEmail(email);
        if (user.isPresent()) {
            Authentication authentication = new UsernamePasswordAuthenticationToken(user.get().getEmail(), null,
                    AuthorityUtils.createAuthorityList("ROLE_USER"));
            SecurityContextHolder.getContext().setAuthentication(authentication);
            return tokenProvider.createToken(authentication, false);
        }
        return null;
    }
}
