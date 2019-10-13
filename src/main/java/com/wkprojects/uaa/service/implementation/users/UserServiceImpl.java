/**
 * Copyright (c) 2019. CreatedBy Wassim KALBOUSSI.  All rights reserved.
 */

package com.wkprojects.uaa.service.implementation.users;

import com.wkprojects.uaa.domain.users.Authority;
import com.wkprojects.uaa.domain.users.User;
import com.wkprojects.uaa.mapper.users.UserMapper;
import com.wkprojects.uaa.model.LoginModel;
import com.wkprojects.uaa.repository.users.AuthorityRepository;
import com.wkprojects.uaa.repository.users.UserRepository;
import com.wkprojects.uaa.security.AuthoritiesConstants;
import com.wkprojects.uaa.security.SecurityUtils;
import com.wkprojects.uaa.security.jwt.TokenProvider;
import com.wkprojects.uaa.service.dto.users.UserDto;
import com.wkprojects.uaa.service.interfaces.users.IUserService;
import com.wkprojects.uaa.utils.RandomUtil;
import com.wkprojects.uaa.web.rest.errors.BadRequestException;
import com.wkprojects.uaa.web.rest.errors.FormatNotValidException;
import com.wkprojects.uaa.web.rest.errors.ResourceAlreadyExistsException;
import com.wkprojects.uaa.web.rest.errors.ResourceNotFoundException;
import com.wkprojects.uaa.web.rest.vm.ResetPasswordVM;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.HashSet;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;

@Service
@Transactional
public class UserServiceImpl implements IUserService {

    private final UserRepository userRepository;
    private final UserMapper userMapper;
    private final AuthorityRepository authorityRepository;
    private final PasswordEncoder passwordEncoder;
    private final AuthenticationManager authenticationManager;
    private final TokenProvider tokenProvider;

    public UserServiceImpl(UserRepository userRepository, UserMapper userMapper, AuthorityRepository authorityRepository,
                           PasswordEncoder passwordEncoder, AuthenticationManager authenticationManager,
                           TokenProvider tokenProvider) {
        this.userRepository = userRepository;
        this.userMapper = userMapper;
        this.authorityRepository = authorityRepository;
        this.passwordEncoder = passwordEncoder;
        this.authenticationManager = authenticationManager;
        this.tokenProvider = tokenProvider;
    }

    @Override
    public UserDto registerUser(UserDto userDto) {
        userRepository.findOneByEmail(userDto.getEmail()).ifPresent(u -> {
            throw new ResourceAlreadyExistsException("User already exists");
        });
        User userToSave = userMapper.userDtoToUser(userDto);
        Set<Authority> authorities;
        if (SecurityUtils.isCurrentUserInRole(AuthoritiesConstants.SUPER_ADMIN)) {
            authorities = userDto.getAuthorities().stream()
                    .map(authorityRepository::findById)
                    .filter(Optional::isPresent)
                    .map(Optional::get)
                    .collect(Collectors.toSet());
        } else {
            authorities = new HashSet<>();
            Authority authority = new Authority();
            authority.setName(AuthoritiesConstants.USER);
            authorities.add(authority);
        }
        userToSave.setAuthorities(authorities);
        userToSave.setPassword(passwordEncoder.encode(userDto.getPassword()));
        userToSave.setActivated(false);
        userToSave.setActivationKey(RandomUtil.generateActivationKey());
        SecurityUtils.getCurrentUserEmail().ifPresent(userToSave::setCreatedBy);
        return userMapper.userToUserDto(userRepository.save(userToSave));
    }

    @Override
    public String authenticateUser(LoginModel loginModel) {
        try {
            UsernamePasswordAuthenticationToken authenticationToken = new UsernamePasswordAuthenticationToken(loginModel.getEmail(), loginModel.getPassword());
            /* Authenticating user: uses UserDetailsService Bean to fetch user from db,check if activated
            * Create SpringSecurity userDetails with email,authorities, actual password*/
            Authentication authentication = this.authenticationManager.authenticate(authenticationToken);
            SecurityContextHolder.getContext().setAuthentication(authentication);
            boolean rememberMe = (loginModel.getRememberMe() == null) ? false : loginModel.getRememberMe();
            return tokenProvider.createToken(authentication, rememberMe);
        } catch (Exception e){
            throw new BadRequestException("Bad credentials");
        }
    }

    @Override
    public UserDto activateUser(String key) {
        User user = userRepository.findOneByActicationKey(key).orElseThrow(
                () -> new ResourceNotFoundException("User not found")
        );
        user.setActivated(true);
        user.setActivationKey(null);
        return userMapper.userToUserDto(user);
    }

    @Override
    public String getCurrentUserEmail() {
        return SecurityUtils.getCurrentUserEmail().orElseThrow(
                () -> new BadRequestException("No user authenticated")
        );
    }

    @Override
    public UserDto getCurrentUser() {
        String email = SecurityUtils.getCurrentUserEmail().orElseThrow(
                () -> new BadRequestException("No user authenticated")
        );
        User user = userRepository.findOneByUserEmail(email).orElseThrow(
                () -> new ResourceNotFoundException("User not found")
        );
        return userMapper.userToUserDto(user);
    }

    @Override
    public void deactivateUser(String email) {
        User user = userRepository.findOneByEmailIgnoreCase(email.toLowerCase()).orElseThrow(
                () -> new ResourceNotFoundException("User not found")
        );
        user.setActivated(false);
    }

    @Override
    public void changePassword(String currentClearTextPassword, String newPassword) {
        Optional<String> email = SecurityUtils.getCurrentUserEmail();
        email.flatMap(userRepository::findOneByEmailIgnoreCase)
                .ifPresent(user -> {
                    String currentEncryptedPassword = user.getPassword();
                    if (!passwordEncoder.matches(currentClearTextPassword, currentEncryptedPassword)) {
                        throw new BadRequestException("Wrong password");
                    }
                    String encryptedPassword = passwordEncoder.encode(newPassword);
                    user.setPassword(encryptedPassword);
                });
    }

    @Override
    public void requestResetPassword(String email) {
        User user = userRepository.findOneByEmailIgnoreCase(email).filter(User::getActivated).orElseThrow(
                () -> new ResourceNotFoundException("User not found")
        );
        user.setResetKey(RandomUtil.generateResetKey());
        user.setResetDate(Instant.now());
        //TODO send resetKey by email
    }

    @Override
    public void finishResetPassword(ResetPasswordVM resetPasswordVM) {
        User user = userRepository.findOneByResetKey(resetPasswordVM.getResetKey()).orElseThrow(
                () -> new ResourceNotFoundException("No user found for this reset key")
        );

        user.setPassword(passwordEncoder.encode(resetPasswordVM.getNewPassword()));
        user.setResetKey(null);
        user.setResetDate(null);
    }

    @Override
    public void deleteUser(String email) {
        User user = userRepository.findOneByEmail(email).orElseThrow(
                () -> new ResourceNotFoundException("User not found")
        );
        userRepository.delete(user);
    }

    @Override
    public UserDto updateUser(UserDto userDto) {
        if (userDto.getId() == null) {
            throw new BadRequestException("Id is required to update user");
        }

        User foundUser = userRepository.findById(userDto.getId()).orElseThrow(
                () -> new ResourceNotFoundException("User not found")
        );

        if(!passwordEncoder.matches(userDto.getPassword(),foundUser.getPassword())){
            throw new BadRequestException("Wrong password");
        }

        Optional<User> existingUser = userRepository.findOneByEmailIgnoreCase(userDto.getEmail().toLowerCase());
        if (existingUser.isPresent() && (!existingUser.get().getId().equals(userDto.getId()))) {
            throw new ResourceAlreadyExistsException("Email already exists!");
        }

        Set<Authority> authorities;
        if (SecurityUtils.isCurrentUserInRole(AuthoritiesConstants.SUPER_ADMIN)) {
            if (userDto.getAuthorities().size() == 0) {
                throw new FormatNotValidException("Authority list should not by empty!");
            }
            authorities = userDto.getAuthorities().stream()
                    .map(authorityRepository::findById)
                    .filter(Optional::isPresent)
                    .map(Optional::get)
                    .collect(Collectors.toSet());
        } else {
            authorities = new HashSet<>();
            Authority authority = new Authority();
            authority.setName(AuthoritiesConstants.USER);
            authorities.add(authority);
        }

        foundUser.setEmail(userDto.getEmail());
        foundUser.setAuthorities(authorities);
        return userMapper.userToUserDto(foundUser);
    }

    @Override
    public void logout() {
        SecurityContextHolder.getContext().setAuthentication(null);
    }
}
