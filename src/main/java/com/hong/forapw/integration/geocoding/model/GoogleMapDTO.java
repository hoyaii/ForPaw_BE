package com.hong.forapw.integration.geocoding.model;

import java.util.List;

public class GoogleMapDTO {

    public record MapDTO(List<ResultDTO> results,
                         String status) {
    }

    public record ResultDTO(List<AddrComponentDTO> address_components,
                            String formatted_address,
                            GeometryDTO geometry,
                            String place_id,
                            PlusCodeDTO plus_code,
                            List<String> types) {
    }

    public record AddrComponentDTO(String long_name,
                                   String short_name,
                                   List<String> types) {
    }

    public record GeometryDTO(LocationDTO location,
                              String location_type,
                              ViewportDTO viewport) {
    }

    public record LocationDTO(Double lat,
                              Double lng) {
    }

    public record ViewportDTO(NortheastDTO northeast,
                              SouthwestDTO southwest) {
    }

    public record NortheastDTO(Double lat,
                               Double lng) {
    }

    public record SouthwestDTO(Double lat,
                               Double lng) {
    }

    public record PlusCodeDTO(String compound_code,
                              String global_code) {
    }
}
