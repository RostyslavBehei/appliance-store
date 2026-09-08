package com.epam.rd.autocode.assessment.appliances.service.impl;

import com.epam.rd.autocode.assessment.appliances.dto.manufacturer.ManufacturerCreateRequest;
import com.epam.rd.autocode.assessment.appliances.dto.manufacturer.ManufacturerResponse;
import com.epam.rd.autocode.assessment.appliances.dto.manufacturer.ManufacturerUpdateRequest;
import com.epam.rd.autocode.assessment.appliances.exception.AlreadyExistsException;
import com.epam.rd.autocode.assessment.appliances.exception.NotFoundException;
import com.epam.rd.autocode.assessment.appliances.model.Manufacturer;
import com.epam.rd.autocode.assessment.appliances.repository.ManufacturerRepository;
import com.epam.rd.autocode.assessment.appliances.service.ManufacturerService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.cache.annotation.Caching;
import org.springframework.context.MessageSource;
import org.springframework.context.i18n.LocaleContextHolder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Locale;

@Slf4j
@Service
@RequiredArgsConstructor
public class ManufacturerServiceImpl implements ManufacturerService {

    private final ManufacturerRepository manufacturerRepository;

    private final MessageSource messageSource;

    @Override
    @Transactional(readOnly = true)
    @Cacheable(value = "manufacturers", key = "'names'")
    public List<String> getAllManufacturerNames() {
        log.debug("Getting all manufacturer names");
        return manufacturerRepository.findAll().stream()
                .map(Manufacturer::getName)
                .toList();
    }

    @Override
    @Transactional(readOnly = true)
    @Cacheable(value = "manufacturers", key = "'all'")
    public List<ManufacturerResponse> getAllManufacturer() {
        log.debug("Getting all manufacturers");
        return manufacturerRepository.findAll().stream()
                .map(ManufacturerResponse::fromEntity)
                .toList();
    }

    @Override
    @Transactional(readOnly = true)
    @Cacheable(value = "manufacturers", key = "'all'", condition = "#keyword == null || #keyword.trim().isEmpty()")
    public List<ManufacturerResponse> getAllManufacturer(String keyword) {
        log.debug("Searching manufacturers by keyword: '{}'", keyword);
        List<Manufacturer> manufacturers;

        if (keyword != null && !keyword.trim().isEmpty()) {
            manufacturers = manufacturerRepository.searchManufacturers(keyword);
        } else {
            manufacturers = manufacturerRepository.findAll();
        }

        return manufacturers.stream()
                .map(ManufacturerResponse::fromEntity)
                .toList();
    }

    @Override
    @Transactional(readOnly = true)
    @Cacheable(value = "manufacturer", key = "#manufacturerId")
    public ManufacturerResponse getManufacturerById(Long manufacturerId) {
        log.debug("Fetching manufacturer by id: '{}'", manufacturerId);
        Manufacturer manufacturer = manufacturerRepository.findById(manufacturerId)
                .orElseThrow(() -> {
                    log.warn("Failed to fetch: Manufacturer not found with id: '{}'", manufacturerId);
                    return new NotFoundException(messageSource.getMessage("error.manufacturer.not.found", new Object[]{manufacturerId}, getLocale()));
                });
        return ManufacturerResponse.fromEntity(manufacturer);
    }

    @Override
    @Transactional
    @Caching(evict = {
            @CacheEvict(value = "manufacturers", allEntries = true),
            @CacheEvict(value = "dashboards", allEntries = true)
    })
    public void createManufacturer(ManufacturerCreateRequest request) {
        log.debug("Creating manufacturer with name: '{}'", request.name());
        if (manufacturerRepository.findByName(request.name()).isPresent()) {
            log.warn("Failed to create: Manufacturer with name '{}' already exists", request.name());
            throw new AlreadyExistsException(
                    messageSource.getMessage("error.manufacturer.name.exists", new Object[]{request.name()}, getLocale())
            );
        }

        Manufacturer manufacturer = Manufacturer.builder()
                .name(request.name())
                .build();

        Manufacturer savedManufacturer = manufacturerRepository.save(manufacturer);

        log.info("Manufacturer successfully created with name: {}", savedManufacturer);
    }

    @Override
    @Transactional
    @CacheEvict(value = "manufacturers", allEntries = true)
    public void updateManufacturer(ManufacturerUpdateRequest request) {
        log.info("Updating manufacturer with id: '{}', name: '{}'", request.id(), request.name());
        Manufacturer manufacturer = manufacturerRepository.findById(request.id())
                .orElseThrow(() -> {
                    log.warn("Failed to update: Manufacturer not found with id: '{}'", request.id());
                    return new NotFoundException(messageSource.getMessage("error.manufacturer.not.found", new Object[]{request.id()}, getLocale()));
                });

        if (!manufacturer.getName().equals(request.name())) {
            if (manufacturerRepository.findByName(request.name()).isPresent()) {
                log.warn("Failed to update: Manufacturer with name '{}' already exists", request.name());
                throw new AlreadyExistsException(
                        messageSource.getMessage("error.manufacturer.name.exists", new Object[]{request.name()}, getLocale())
                );
            }
        }

        manufacturer.setName(request.name());

        log.info("Manufacturer successfully updated with id: '{}', name: {}", manufacturer.getId(), manufacturer.getName());
    }

    @Override
    @Transactional
    @Caching(evict = {
            @CacheEvict(value = "manufacturers", allEntries = true),
            @CacheEvict(value = "dashboards", allEntries = true)
    })
    public void deleteManufacturerById(Long manufacturerId) {
        log.info("Attempting to delete manufacturer with id: {}", manufacturerId);

        Manufacturer manufacturer = manufacturerRepository.findById(manufacturerId)
                .orElseThrow(() -> {
                    log.warn("Failed to delete: Manufacturer with id {} not found", manufacturerId);
                    return new NotFoundException(
                            messageSource.getMessage("error.manufacturer.not.found", new Object[]{manufacturerId}, getLocale())
                    );
                });

        manufacturerRepository.delete(manufacturer);
        log.info("Manufacturer with id: {} successfully deleted", manufacturerId);
    }

    private Locale getLocale() {
        return LocaleContextHolder.getLocale();
    }
}