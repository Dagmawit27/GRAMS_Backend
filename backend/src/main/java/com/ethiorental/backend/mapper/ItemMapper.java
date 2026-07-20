package com.ethiorental.backend.mapper;

import com.ethiorental.backend.dto.*;
import com.ethiorental.backend.Entity.*;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;

@Mapper(componentModel = "spring")
public interface ItemMapper {

    @Mapping(target = "id", ignore = true)
    Item toEntity(ItemRequest request);

    ItemResponse toResponse(Item entity);
}
