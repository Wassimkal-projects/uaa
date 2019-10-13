/**
 * Copyright (c) 2019. CreatedBy Wassim KALBOUSSI.  All rights reserved.
 */

package com.wkprojects.uaa.mapper.users;

import com.wkprojects.uaa.domain.users.Authority;
import com.wkprojects.uaa.domain.users.User;
import com.wkprojects.uaa.service.dto.users.UserDto;
import org.mapstruct.Mapper;
import org.mapstruct.ReportingPolicy;

import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

@Mapper(unmappedTargetPolicy = ReportingPolicy.IGNORE, componentModel = "spring")
public interface UserMapper {

    UserDto userToUserDto(User user);

    User userDtoToUser(UserDto userDto);

    List<UserDto> userToUserDtoList(List<User> userList);

    default Set<String> autoritiesToString(Set<Authority> authorities) {
        return authorities.stream().map(Authority::getName)
                .collect(Collectors.toSet());
    }

    default Set<Authority> stringsToAuthorities(Set<String> strings) {
        return strings.stream().map(string -> {
            Authority auth = new Authority();
            auth.setName(string);
            return auth;
        }).collect(Collectors.toSet());
    }
}
