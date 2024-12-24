package com.hong.forapw.integration.geocoding.model;


import java.util.List;

public class KakaoMapDTO {

    public record MapDTO(List<DocumentDTO> documents,
                         MetaDTO meta) {
    }

    public record DocumentDTO(AddressDTO address,
                              String address_name,
                              String address_type,
                              RoadAddressDTO road_address,
                              String x,
                              String y) {
    }

    public record AddressDTO(String address_name,
                             String b_code,
                             String h_code,
                             String main_address_no,
                             String mountain_yn,
                             String region_1depth_name,
                             String region_2depth_name,
                             String region_3depth_h_name,
                             String region_3depth_name,
                             String sub_address_no,
                             String x,
                             String y) {
    }

    public record RoadAddressDTO(String address_name,
                                 String building_name,
                                 String main_building_no,
                                 String region_1depth_name,
                                 String region_2depth_name,
                                 String region_3depth_name,
                                 String road_name,
                                 String sub_building_no,
                                 String underground_yn,
                                 String x,
                                 String y,
                                 String zone_no) {
    }

    public record MetaDTO(Boolean is_end,
                          Integer pageable_count,
                          Integer total_count) {
    }
}
