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
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.context.MessageSource;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;

import java.math.BigDecimal;
import java.util.List;
import java.util.Locale;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class ApplianceServiceImplTest {

    @Mock
    private ApplianceRepository applianceRepository;

    @Mock
    private ManufacturerRepository manufacturerRepository;

    @Mock
    private MessageSource messageSource;

    @InjectMocks
    private ApplianceServiceImpl applianceService;

    private Appliance testAppliance;
    private Manufacturer testManufacturer;
    private Pageable pageable;

    @BeforeEach
    void setUp() {
        testManufacturer = Manufacturer.builder()
                .id(1L)
                .name("Samsung")
                .build();

        testAppliance = Appliance.builder()
                .id(10L)
                .name("Microwave")
                .category(Category.BIG)
                .manufacturer(testManufacturer)
                .price(BigDecimal.valueOf(150.00))
                .power(800)
                .powerType(PowerType.AC220)
                .build();

        pageable = PageRequest.of(0, 10);

        lenient().when(messageSource.getMessage(anyString(), any(), any(Locale.class)))
                .thenAnswer(invocation -> invocation.getArgument(0).toString());
    }

    @Test
    @DisplayName("Get All Appliance - Should Return All Appliances")
    void getAllAppliances_ShouldReturnAllAppliances() {
        String name = "Micro";
        Category category = Category.BIG;
        String manufacturerName = "Samsung";
        PowerType powerType = PowerType.AC220;
        Integer power = 800;
        BigDecimal minPrice = BigDecimal.valueOf(100.00);
        BigDecimal maxPrice = BigDecimal.valueOf(500.00);

        Page<Appliance> appliancePage = new PageImpl<>(List.of(testAppliance));

        when(applianceRepository.findAllWithFilter(
                name, category, manufacturerName, powerType, power, minPrice, maxPrice, pageable
        )).thenReturn(appliancePage);

        Page<ApplianceSummaryResponse> result = applianceService.getAllAppliance(
                name, category, manufacturerName, powerType, power, minPrice, maxPrice, pageable
        );

        assertNotNull(result);
        assertEquals(1, result.getTotalElements());
        assertEquals("Microwave", result.getContent().get(0).name());

        verify(applianceRepository, times(1)).findAllWithFilter(
                name, category, manufacturerName, powerType, power, minPrice, maxPrice, pageable
        );
    }

    @Test
    @DisplayName("Get All Appliance - Should successfully return result without filters")
    void getAllAppliances_ShouldReturnResultWithoutFilters() {
        Page<Appliance> appliancePage = new PageImpl<>(List.of(testAppliance));

        when(applianceRepository.findAllWithFilter(
                null, null, null, null, null, null, null, pageable
        )).thenReturn(appliancePage);

        Page<ApplianceSummaryResponse> result = applianceService.getAllAppliance(
                null, null, null, null, null, null, null, pageable
        );

        assertNotNull(result);
        assertEquals(1, result.getTotalElements());

        verify(applianceRepository, times(1)).findAllWithFilter(
                null, null, null, null, null, null, null, pageable
        );
    }

    @Test
    @DisplayName("Get All Appliance By Category - Should successfully return Appliances")
    void getAllAppliancesByCategory_ShouldReturnAppliances() {
        Page<Appliance> appliancePage = new PageImpl<>(List.of(testAppliance));
        Category category = Category.BIG;

        when(applianceRepository.findByCategory(category, pageable))
                .thenReturn(appliancePage);

        Page<ApplianceSummaryResponse> result = applianceService.getAllApplianceByCategory(category, pageable);

        assertNotNull(result);
        assertEquals(1, result.getTotalElements());
        assertEquals("Microwave", result.getContent().get(0).name());

        verify(applianceRepository, times(1)).findByCategory(category, pageable);
    }

    @Test
    @DisplayName("Get All Appliance By Category - Shouldn't return result if a category not the same")
    void getAllAppliancesByCategory_ShouldReturnAppliancesWithoutFilter() {
        when(applianceRepository.findByCategory(null, pageable))
                .thenReturn(Page.empty());

        Page<ApplianceSummaryResponse> result = applianceService.getAllApplianceByCategory(null, pageable);

        assertNotNull(result);
        assertEquals(0, result.getTotalElements());

        verify(applianceRepository, times(1)).findByCategory(null, pageable);
    }

    @Test
    @DisplayName("Get All Appliance By Manufacturer Name - Should successfully return Appliance")
    void getAllAppliancesByManufacturer_ShouldReturnAppliances() {
        Page<Appliance> appliancePage = new PageImpl<>(List.of(testAppliance));
        String manufacturerName = "Samsung";

        when(applianceRepository.findByManufacturerName(manufacturerName, pageable))
                .thenReturn(appliancePage);

        Page<ApplianceSummaryResponse> result = applianceService.getAllApplianceByManufacturerName(manufacturerName, pageable);

        assertNotNull(result);
        assertEquals(1, result.getTotalElements());
        assertEquals("Microwave", result.getContent().get(0).name());

        verify(applianceRepository, times(1)).findByManufacturerName(manufacturerName, pageable);
    }

    @Test
    @DisplayName("Get All Appliance By Manufacturer Name - Shouldn't return result if a manufacturer name not the same")
    void getAllApplianceByManufacturer_ShouldNotReturnResultIfManufacturerNameIsNotTheSame() {
        when(applianceRepository.findByManufacturerName(null, pageable))
                .thenReturn(Page.empty());

        Page<ApplianceSummaryResponse> result = applianceService.getAllApplianceByManufacturerName(null, pageable);

        assertNotNull(result);
        assertEquals(0, result.getTotalElements());

        verify(applianceRepository, times(1)).findByManufacturerName(null, pageable);
    }

    @Test
    @DisplayName("Get Appliance By Id - Should return Appliance")
    void getAppliancesById_ShouldReturnAppliances() {
        Long id = 10L;

        when(applianceRepository.findById(id)).thenReturn(Optional.of(testAppliance));

        ApplianceResponse result = applianceService.getApplianceById(id);

        assertNotNull(result);
        assertEquals("Microwave", result.name());

        verify(applianceRepository, times(1)).findById(id);
    }

    @Test
    @DisplayName("Get Appliance By Id - Should throw NotFoundException if appliance not exist")
    void getAppliancesById_ShouldThrowNotFoundExceptionIfApplianceNotExist() {
        Long id = 99L;

        when(applianceRepository.findById(id)).thenReturn(Optional.empty());

        NotFoundException result = assertThrows(NotFoundException.class, () -> applianceService.getApplianceById(99L));

        assertEquals("error.appliance.not.found", result.getMessage());

        verify(applianceRepository, times(1)).findById(id);
    }

    @Test
    @DisplayName("Update Appliance - Should successfully update Appliance")
    void updateAppliances_ShouldUpdateAppliances() {
        Long applianceId = 10L;
        Long manufacturerId = 1L;
        String name = "Updated Microwave";


        ApplianceUpdateRequest request = new ApplianceUpdateRequest(
                applianceId, name, null, null, manufacturerId, null, null, null, null, null
        );

        when(applianceRepository.findById(applianceId)).thenReturn(Optional.of(testAppliance));
        when(manufacturerRepository.findById(manufacturerId)).thenReturn(Optional.of(testManufacturer));

        applianceService.updateAppliance(request);

        assertEquals("Updated Microwave", testAppliance.getName());

        verify(applianceRepository, times(1)).findById(applianceId);
        verify(applianceRepository, times(1)).save(any(Appliance.class));
        verify(manufacturerRepository, times(1)).findById(manufacturerId);
    }

    @Test
    @DisplayName("Update Appliance - Should throw NotFoundException if an appliance not exist")
    void updateAppliances_ShouldThrowNotFoundExceptionIfApplianceNotExist() {
        Long id = 99L;

        ApplianceUpdateRequest request = new ApplianceUpdateRequest(
                id, null, null, null, null, null, null, null, null, null
        );

        when(applianceRepository.findById(id)).thenReturn(Optional.empty());

        NotFoundException result = assertThrows(NotFoundException.class, () -> applianceService.updateAppliance(request));

        assertEquals("error.appliance.not.found", result.getMessage());

        verify(applianceRepository, times(1)).findById(id);
        verify(applianceRepository, times(0)).save(any(Appliance.class));
        verify(manufacturerRepository, times(0)).findById(anyLong());
    }

    @Test
    @DisplayName("Update Appliance - Should throw NotFoundException if a manufacturer not exist")
    void updateAppliances_ShouldThrowNotFoundExceptionIfManufacturerNotExist() {
        Long applianceId = 10L;
        Long manufacturerId = 99L;

        ApplianceUpdateRequest request = new ApplianceUpdateRequest(
                applianceId, null, null, null, manufacturerId, null, null, null, null, null
        );

        when(applianceRepository.findById(applianceId)).thenReturn(Optional.of(testAppliance));
        when(manufacturerRepository.findById(manufacturerId)).thenReturn(Optional.empty());

        NotFoundException result = assertThrows(NotFoundException.class, () -> applianceService.updateAppliance(request));

        assertEquals("error.manufacturer.not.found", result.getMessage());

        verify(applianceRepository, times(1)).findById(applianceId);
        verify(applianceRepository, times(0)).save(any(Appliance.class));
        verify(manufacturerRepository, times(1)).findById(manufacturerId);
    }

    @Test
    @DisplayName("Delete Appliance By Id - Should successfully delete appliance")
    void deleteAppliancesById_ShouldDeleteAppliances() {
        Long id = 10L;

        when(applianceRepository.findById(id)).thenReturn(Optional.of(testAppliance));

        applianceService.deleteApplianceById(id);

        verify(applianceRepository, times(1)).findById(id);
        verify(applianceRepository, times(1)).delete(testAppliance);
    }

    @Test
    @DisplayName("Delete Appliance By Id - Should throw NotFoundException if appliance not exists")
    void deleteAppliancesById_ShouldThrowNotFoundExceptionIfApplianceNotExist() {
        Long id = 99L;

        when(applianceRepository.findById(id)).thenReturn(Optional.empty());

        NotFoundException result = assertThrows(NotFoundException.class, () -> applianceService.deleteApplianceById(id));

        assertEquals("error.appliance.not.found", result.getMessage());

        verify(applianceRepository, times(1)).findById(id);
        verify(applianceRepository, times(0)).deleteById(id);
    }
}