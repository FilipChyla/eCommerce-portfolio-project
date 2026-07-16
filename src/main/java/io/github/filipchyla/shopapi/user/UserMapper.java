package io.github.filipchyla.shopapi.user;

import io.github.filipchyla.shopapi.user.dto.PatchUserRequest;
import io.github.filipchyla.shopapi.user.dto.UserResponse;
import org.mapstruct.BeanMapping;
import org.mapstruct.Mapper;
import org.mapstruct.MappingTarget;
import org.mapstruct.NullValuePropertyMappingStrategy;

@Mapper(componentModel = "spring")
public interface UserMapper {

    @BeanMapping(nullValuePropertyMappingStrategy = NullValuePropertyMappingStrategy.IGNORE)
    void updateFromPatchRequest(PatchUserRequest request,
                                @MappingTarget User user);

    UserResponse toResponse(User user);
}
