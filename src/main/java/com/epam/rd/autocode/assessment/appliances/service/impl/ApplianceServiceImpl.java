package com.epam.rd.autocode.assessment.appliances.service.impl;

import com.epam.rd.autocode.assessment.appliances.dto.appliance.ApplianceResponse;
import com.epam.rd.autocode.assessment.appliances.dto.appliance.ApplianceSummaryResponse;
import com.epam.rd.autocode.assessment.appliances.dto.appliance.ApplianceUpdateRequest;
import com.epam.rd.autocode.assessment.appliances.exception.NotFoundException;
import com.epam.rd.autocode.assessment.appliances.model.Appliance;
import com.epam.rd.autocode.assessment.appliances.model.Manufacturer;
import com.epam.rd.autocode.assessment.appliances.model.enums.Category;
import com.epam.rd.autocode.assessment.appliances.model.enums.PowerType;
import com.epam.rd.autocode.assessment.appliances.repository.ApplianceRepository;
import com.epam.rd.autocode.assessment.appliances.repository.ManufacturerRepository;
import com.epam.rd.autocode.assessment.appliances.service.ApplianceService;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;

@Service
@RequiredArgsConstructor
public class ApplianceServiceImpl implements ApplianceService {

    private final ApplianceRepository applianceRepository;
    private final ManufacturerRepository manufacturerRepository;

    @Override
    @Transactional(readOnly = true)
    public Page<ApplianceSummaryResponse> getAllAppliance(String name,
                                                          Category category,
                                                          String manufacturerName,
                                                          PowerType powerType,
                                                          Integer power,
                                                          BigDecimal minPrice,
                                                          BigDecimal maxPrice,
                                                          Pageable pageable) {
        return applianceRepository.findAllWithFilter(name, category, manufacturerName, powerType, power, minPrice, maxPrice, pageable)
                .map(ApplianceSummaryResponse::fromEntity);
    }

    @Override
    @Transactional(readOnly = true)
    public Page<ApplianceSummaryResponse> getAllApplianceByCategory(Category category, Pageable pageable) {
        return applianceRepository.findByCategory(category, pageable)
                .map(ApplianceSummaryResponse::fromEntity);
    }

    @Override
    @Transactional(readOnly = true)
    public Page<ApplianceSummaryResponse> getAllApplianceByManufacturerName(String manufacturerName, Pageable pageable) {
        return applianceRepository.findByManufacturerName(manufacturerName, pageable)
                .map(ApplianceSummaryResponse::fromEntity);
    }

    @Override
    @Transactional(readOnly = true)
    public ApplianceResponse getApplianceById(Long id) {
        Appliance appliance = applianceRepository.findById(id)
                .orElseThrow(() -> new NotFoundException("Appliance with id " + id + " not found"));

        return ApplianceResponse.fromEntity(appliance);
    }

    @Override
    @Transactional
    public void updateAppliance(ApplianceUpdateRequest request) {
        Appliance appliance;

        if (request.id() != null) {
            appliance = applianceRepository.findById(request.id())
                    .orElseThrow(() -> new NotFoundException("Appliance with id " + request.id() + " not found"));
        } else {
            appliance = new Appliance();
        }

        if (request.name() != null) appliance.setName(request.name());
        if (request.category() != null) appliance.setCategory(request.category());
        if (request.model() != null) appliance.setModel(request.model());
        if (request.manufacturerId() != null) {
            Manufacturer manufacturer = manufacturerRepository.findById(request.manufacturerId())
                    .orElseThrow(() -> new NotFoundException("Manufacturer with id " + request.manufacturerId() + " not found"));
            appliance.setManufacturer(manufacturer);
        }
        if (request.powerType() != null) appliance.setPowerType(request.powerType());
        if (request.characteristic() != null) appliance.setCharacteristic(request.characteristic());
        if (request.description() != null) appliance.setDescription(request.description());
        if (request.power() != null) appliance.setPower(request.power());
        if (request.price()!= null) appliance.setPrice(request.price());

        applianceRepository.save(appliance);
    }

    @Override
    @Transactional
    public void deleteApplianceById(Long applianceId) {
        if (applianceRepository.findById(applianceId).isPresent()) {
            applianceRepository.deleteById(applianceId);
        } else {
            throw new NotFoundException("Appliance with id " + applianceId + " not found");
        }
    }
}
