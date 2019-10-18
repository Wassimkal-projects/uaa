/**
 * Copyright (c) 2019. CreatedBy Wassim KALBOUSSI.  All rights reserved.
 */

package com.wkprojects.uaa.service.interfaces.users;

import com.wkprojects.uaa.model.SocialToken;

public interface ISocialUserService {
    String authenticateFacebookUser(SocialToken fbToken);

    String authenticateGoogleUser(SocialToken googleToken) throws Exception;
}
