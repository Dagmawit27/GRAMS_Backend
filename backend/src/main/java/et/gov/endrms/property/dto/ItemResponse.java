package et.gov.endrms.dto;

public record ItemResponse(
    Long id,
    String name,
    String description,
    Double price
) {}