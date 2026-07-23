package et.gov.endrms.mapper;

import et.gov.endrms.dto.*;
import et.gov.endrms.Entity.*;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;

@Mapper(componentModel = "spring")
public interface ItemMapper {

    @Mapping(target = "id", ignore = true)
    Item toEntity(ItemRequest request);

    ItemResponse toResponse(Item entity);
}
