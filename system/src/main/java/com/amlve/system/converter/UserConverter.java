package com.amlve.system.converter;

import com.amlve.system.dto.request.UserCreateRequest;
import com.amlve.system.dto.response.UserInfoResponse;
import com.amlve.system.tableEnt.User;
import org.mapstruct.Mapper;

@Mapper(componentModel = "spring")
public interface UserConverter {

    User toEntity(UserCreateRequest request);

    UserInfoResponse toResponse(User user);
}
