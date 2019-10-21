/**
 * Copyright (c) 2019. CreatedBy Wassim KALBOUSSI.  All rights reserved.
 */

package com.wkprojects.uaa.service.interfaces.gateway;

import com.wkprojects.uaa.domain.users.User;
import com.wkprojects.uaa.service.dto.emails.EmailDto;

public interface IMailingService {

    void sendActivationEmail(User user);
}
