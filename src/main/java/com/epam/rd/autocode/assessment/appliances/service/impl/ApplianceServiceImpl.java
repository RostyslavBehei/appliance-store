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
import lombok.extern.slf4j.Slf4j;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.cache.annotation.Caching;
import org.springframework.context.MessageSource;
import org.springframework.context.i18n.LocaleContextHolder;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.Locale;

@Slf4j
@Service
@RequiredArgsConstructor
public class ApplianceServiceImpl implements ApplianceService {

    private final ApplianceRepository applianceRepository;
    private final ManufacturerRepository manufacturerRepository;
    private final MessageSource messageSource;

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

        log.debug("Fetching appliances with filter: name='{}', category='{}', manufacturerName='{}', powerType='{}', power='{}', minPrice='{}', maxPrice='{}'",
                name, category, manufacturerName, powerType, power, minPrice, maxPrice
        );

        return applianceRepository.findAllWithFilter(name, category, manufacturerName, powerType, power, minPrice, maxPrice, pageable)
                .map(ApplianceSummaryResponse::fromEntity);
    }

    @Override
    @Transactional(readOnly = true)
    public Page<ApplianceSummaryResponse> getAllApplianceByCategory(Category category, Pageable pageable) {
        log.debug("Fetching appliances by category: {}", category);
        return applianceRepository.findByCategory(category, pageable)
                .map(ApplianceSummaryResponse::fromEntity);
    }

    @Override
    @Transactional(readOnly = true)
    public Page<ApplianceSummaryResponse> getAllApplianceByManufacturerName(String manufacturerName, Pageable pageable) {
        log.debug("Fetching appliances by manufacturer name: {}", manufacturerName);
        return applianceRepository.findByManufacturerName(manufacturerName, pageable)
                .map(ApplianceSummaryResponse::fromEntity);
    }

    @Override
    @Transactional(readOnly = true)
    @Cacheable(value = "appliances", key = "#id")
    public ApplianceResponse getApplianceById(Long id) {
        log.debug("Fetching appliance by ID: {}", id);
        Appliance appliance = applianceRepository.findById(id)
                .orElseThrow(() -> {
                        log.warn("Appliance not found with id: '{}'", id);
                        return new NotFoundException(messageSource.getMessage("error.appliance.not.found", new Object[]{id}, getLocale()));
                });

        return ApplianceResponse.fromEntity(appliance);
    }

    @Override
    @Transactional
    @Caching(evict = {
            @CacheEvict(value = "appliances", key = "#request.id()", condition = "#request.id() != null"),
            @CacheEvict(value = "dashboards", allEntries = true)
    })
    public void updateAppliance(ApplianceUpdateRequest request) {
        if (request == null) {
            throw new IllegalArgumentException("ApplianceUpdateRequest must not be null");
        }

        boolean isNew = request.id() == null;
        Appliance appliance;

        if (!isNew) {
            log.info("Updating appliance with id: '{}'", request.id());
            appliance = applianceRepository.findById(request.id())
                    .orElseThrow(() -> {
                        log.warn("Appliance not found with id: '{}'", request.id());
                        return new NotFoundException(
                                messageSource.getMessage("error.appliance.not.found", new Object[]{request.id()}, getLocale())
                        );
                    });
        } else {
            log.info("Creating appliance with model: '{}'", request.model());
            appliance = new Appliance();
        }

        if (request.name() != null) appliance.setName(request.name());
        if (request.category() != null) appliance.setCategory(request.category());
        if (request.model() != null) appliance.setModel(request.model());
        if (request.manufacturerId() != null) {
            Manufacturer manufacturer = manufacturerRepository.findById(request.manufacturerId())
                    .orElseThrow(() -> {
                        log.warn("Failed to set manufacturer: Manufacturer with id {} not found", request.manufacturerId());
                        return new NotFoundException(
                                messageSource.getMessage("error.manufacturer.not.found", new Object[]{request.manufacturerId()}, getLocale())
                        );
                    });
            appliance.setManufacturer(manufacturer);
        }
        if (request.powerType() != null) appliance.setPowerType(request.powerType());
        if (request.characteristic() != null) appliance.setCharacteristic(request.characteristic());
        if (request.description() != null) appliance.setDescription(request.description());
        if (request.power() != null) appliance.setPower(request.power());
        if (request.price() != null) appliance.setPrice(request.price());

        applianceRepository.save(appliance);
        log.info("Appliance successfully {} with id '{}'", isNew ? "created" : "updated", appliance.getId());
    }

    @Override
    @Transactional
    @Caching(evict = {
            @CacheEvict(value = "appliances", key = "#applianceId"),
            @CacheEvict(value = "dashboards", allEntries = true)
    })
    public void deleteApplianceById(Long applianceId) {
        log.info("Attempting to delete appliance with id: '{}'", applianceId);

        Appliance appliance = applianceRepository.findById(applianceId)
                .orElseThrow(() -> {
                    log.warn("Failed to delete: Appliance with id {} not found", applianceId);
                    return new NotFoundException(
                            messageSource.getMessage("error.appliance.not.found", new Object[]{applianceId}, getLocale())
                    );
                });

        applianceRepository.delete(appliance);
        log.info("Appliance successfully deleted with id: '{}'", applianceId);
    }

    private Locale getLocale() {
        return LocaleContextHolder.getLocale();
    }
}