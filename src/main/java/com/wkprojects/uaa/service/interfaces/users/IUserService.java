/**
 * Copyright (c) 2019. CreatedBy Wassim KALBOUSSI.  All rights reserved.
 */

package com.wkprojects.uaa.service.interfaces.users;

import com.wkprojects.uaa.domain.users.User;
import com.wkprojects.uaa.model.JwtToken;
import com.wkprojects.uaa.model.LoginModel;
import com.wkprojects.uaa.model.UserModel;
import com.wkprojects.uaa.service.dto.users.UserDto;
import com.wkprojects.uaa.web.rest.users.AccountResource;
import com.wkprojects.uaa.web.rest.vm.ResetPasswordVM;

public interface IUserService {
    UserDto registerUser(UserDto userDto);

    String authenticateUser(LoginModel loginModel);

    UserDto activateUser(String key);

    String getCurrentUserEmail();

    UserDto getCurrentUser();

    void deactivateUser(String email);

    void changePassword(String currentPassword, String newPassword);

    void requestResetPassword(String email);

    void finishResetPassword(ResetPasswordVM resetPasswordVM);

    void deleteUser(String email);

    UserDto updateUser(UserDto user);

    void logout();
}
