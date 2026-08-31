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
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@RequiredArgsConstructor
public class ManufacturerServiceImpl implements ManufacturerService {

    private final ManufacturerRepository manufacturerRepository;

    @Override
    @Transactional(readOnly = true)
    public List<String> getAllManufacturerNames() {
        return manufacturerRepository.findAll().stream()
                .map(Manufacturer::getName)
                .toList();
    }

    @Override
    @Transactional(readOnly = true)
    public List<ManufacturerResponse> getAllManufacturer() {
        return manufacturerRepository.findAll().stream()
                .map(ManufacturerResponse::fromEntity)
                .toList();
    }

    @Override
    @Transactional(readOnly = true)
    public List<ManufacturerResponse> getAllManufacturer(String keyword) {
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
    public ManufacturerResponse getManufacturerById(Long manufacturerId) {
        Manufacturer manufacturer = manufacturerRepository.findById(manufacturerId)
                .orElseThrow(() -> new NotFoundException("Manufacturer with id: " + manufacturerId + " not found"));
        return ManufacturerResponse.fromEntity(manufacturer);
    }

    @Override
    @Transactional
    public void createManufacturer(ManufacturerCreateRequest request) {
        if (manufacturerRepository.findByName(request.name()).isPresent()) {
            throw new AlreadyExistsException("Manufacturer with name " + request.name() + " already exists");
        }

        Manufacturer manufacturer = Manufacturer.builder()
                .name(request.name())
                .build();

        Manufacturer savedManufacturer = manufacturerRepository.save(manufacturer);

        ManufacturerResponse.fromEntity(savedManufacturer);
    }

    @Override
    @Transactional
    public void updateManufacturer(ManufacturerUpdateRequest request) {
        Manufacturer manufacturer = manufacturerRepository.findById(request.id())
                .orElseThrow(() -> new NotFoundException("Manufacturer with id " + request.id() + " not found"));

        if (!manufacturer.getName().equals(request.name())) {
            if (manufacturerRepository.findByName(request.name()).isPresent()) {
                throw new NotFoundException("Manufacturer with name " + request.name() + " not found");
            }
        }

        manufacturer.setName(request.name());

        ManufacturerResponse.fromEntity(manufacturerRepository.save(manufacturer));
    }

    @Override
    @Transactional
    public void deleteManufacturerById(Long manufacturerId) {
        if (manufacturerRepository.findById(manufacturerId).isPresent()) {
            manufacturerRepository.deleteById(manufacturerId);
        } else {
            throw new NotFoundException("Manufacturer with id " + manufacturerId + " not found");
        }
    }
}
