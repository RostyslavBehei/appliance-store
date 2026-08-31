package com.epam.rd.autocode.assessment.appliances.service;

import com.epam.rd.autocode.assessment.appliances.dto.manufacturer.ManufacturerCreateRequest;
import com.epam.rd.autocode.assessment.appliances.dto.manufacturer.ManufacturerResponse;
import com.epam.rd.autocode.assessment.appliances.dto.manufacturer.ManufacturerUpdateRequest;

import java.util.List;

public interface ManufacturerService {
    List<String> getAllManufacturerNames();
    List<ManufacturerResponse> getAllManufacturer();
    List<ManufacturerResponse> getAllManufacturer(String keyword);
    ManufacturerResponse getManufacturerById(Long manufacturerId);
    void createManufacturer(ManufacturerCreateRequest request);
    void updateManufacturer(ManufacturerUpdateRequest request);
    void deleteManufacturerById(Long manufacturerId);
}
