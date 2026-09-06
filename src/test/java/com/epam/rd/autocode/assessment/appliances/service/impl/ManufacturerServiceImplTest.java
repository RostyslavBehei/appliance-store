package com.epam.rd.autocode.assessment.appliances.service.impl;

import com.epam.rd.autocode.assessment.appliances.dto.manufacturer.ManufacturerCreateRequest;
import com.epam.rd.autocode.assessment.appliances.dto.manufacturer.ManufacturerResponse;
import com.epam.rd.autocode.assessment.appliances.dto.manufacturer.ManufacturerUpdateRequest;
import com.epam.rd.autocode.assessment.appliances.exception.AlreadyExistsException;
import com.epam.rd.autocode.assessment.appliances.exception.NotFoundException;
import com.epam.rd.autocode.assessment.appliances.model.Manufacturer;
import com.epam.rd.autocode.assessment.appliances.repository.ManufacturerRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.context.MessageSource;

import java.util.List;
import java.util.Locale;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class ManufacturerServiceImplTest {

    @Mock
    private ManufacturerRepository manufacturerRepository;

    @Mock
    private MessageSource messageSource;

    @InjectMocks
    private ManufacturerServiceImpl manufacturerService;

    private Manufacturer testManufacturer;

    @BeforeEach
    void setUp() {
        testManufacturer = Manufacturer.builder()
                .id(1L)
                .name("Samsung")
                .build();

        lenient().when(messageSource.getMessage(anyString(), any(), any(Locale.class)))
                .thenAnswer(invocation -> invocation.getArgument(0).toString());
    }

    @Test
    @DisplayName("Get All Manufacturer Names - Should return list of names")
    void getAllManufacturerNames_ShouldReturnNamesList() {
        when(manufacturerRepository.findAll()).thenReturn(List.of(testManufacturer));

        List<String> result = manufacturerService.getAllManufacturerNames();

        assertNotNull(result);
        assertEquals(1, result.size());
        assertEquals("Samsung", result.get(0));

        verify(manufacturerRepository, times(1)).findAll();
    }

    @Test
    @DisplayName("Get All Manufacturer - Should return list of ManufacturerResponse")
    void getAllManufacturer_ShouldReturnResponsesList() {
        when(manufacturerRepository.findAll()).thenReturn(List.of(testManufacturer));

        List<ManufacturerResponse> result = manufacturerService.getAllManufacturer();

        assertNotNull(result);
        assertEquals(1, result.size());
        assertEquals("Samsung", result.get(0).name());

        verify(manufacturerRepository, times(1)).findAll();
    }

    @Test
    @DisplayName("Get All Manufacturer With Keyword - Should successfully return filtered Manufacturers")
    void getAllManufacturerWithKeyword_ShouldReturnFilteredManufacturers() {
        String keyword = "Sam";

        when(manufacturerRepository.searchManufacturers(keyword)).thenReturn(List.of(testManufacturer));

        List<ManufacturerResponse> result = manufacturerService.getAllManufacturer(keyword);

        assertNotNull(result);
        assertEquals(1, result.size());
        assertEquals("Samsung", result.get(0).name());

        verify(manufacturerRepository, times(1)).searchManufacturers(keyword);
        verify(manufacturerRepository, never()).findAll();
    }

    @Test
    @DisplayName("Get All Manufacturer With Keyword - Should return all if keyword is null or blank")
    void getAllManufacturerWithKeyword_ShouldReturnAllIfKeywordIsNullOrBlank() {
        when(manufacturerRepository.findAll()).thenReturn(List.of(testManufacturer));

        List<ManufacturerResponse> resultNull = manufacturerService.getAllManufacturer(null);
        List<ManufacturerResponse> resultBlank = manufacturerService.getAllManufacturer("   ");

        assertNotNull(resultNull);
        assertNotNull(resultBlank);
        assertEquals(1, resultNull.size());
        assertEquals(1, resultBlank.size());

        verify(manufacturerRepository, times(2)).findAll();
        verify(manufacturerRepository, never()).searchManufacturers(anyString());
    }

    @Test
    @DisplayName("Get Manufacturer By Id - Should return Manufacturer")
    void getManufacturerById_ShouldReturnManufacturer() {
        Long id = 1L;

        when(manufacturerRepository.findById(id)).thenReturn(Optional.of(testManufacturer));

        ManufacturerResponse result = manufacturerService.getManufacturerById(id);

        assertNotNull(result);
        assertEquals("Samsung", result.name());

        verify(manufacturerRepository, times(1)).findById(id);
    }

    @Test
    @DisplayName("Get Manufacturer By Id - Should throw NotFoundException if manufacturer does not exist")
    void getManufacturerById_ShouldThrowNotFoundExceptionIfManufacturerNotExist() {
        Long id = 99L;

        when(manufacturerRepository.findById(id)).thenReturn(Optional.empty());

        NotFoundException result = assertThrows(NotFoundException.class, () -> manufacturerService.getManufacturerById(id));

        assertEquals("error.manufacturer.not.found", result.getMessage());
        verify(manufacturerRepository, times(1)).findById(id);
    }

    @Test
    @DisplayName("Create Manufacturer - Should successfully create Manufacturer")
    void createManufacturer_ShouldSuccessfullyCreateManufacturer() {
        ManufacturerCreateRequest request = new ManufacturerCreateRequest("Apple");

        when(manufacturerRepository.findByName("Apple")).thenReturn(Optional.empty());

        Manufacturer savedManufacturer = Manufacturer.builder().id(2L).name("Apple").build();
        when(manufacturerRepository.save(any(Manufacturer.class))).thenReturn(savedManufacturer);

        manufacturerService.createManufacturer(request);

        verify(manufacturerRepository, times(1)).findByName("Apple");
        verify(manufacturerRepository, times(1)).save(argThat(m -> m.getName().equals("Apple")));
    }

    @Test
    @DisplayName("Create Manufacturer - Should throw AlreadyExistsException if name exists")
    void createManufacturer_ShouldThrowAlreadyExistsExceptionIfNameExists() {
        ManufacturerCreateRequest request = new ManufacturerCreateRequest("Samsung");

        when(manufacturerRepository.findByName("Samsung")).thenReturn(Optional.of(testManufacturer));

        AlreadyExistsException result = assertThrows(AlreadyExistsException.class, () -> manufacturerService.createManufacturer(request));

        assertEquals("error.manufacturer.name.exists", result.getMessage());
        verify(manufacturerRepository, times(1)).findByName("Samsung");
        verify(manufacturerRepository, never()).save(any(Manufacturer.class));
    }

    @Test
    @DisplayName("Update Manufacturer - Should successfully update Manufacturer when name is changed")
    void updateManufacturer_ShouldSuccessfullyUpdateManufacturer() {
        Long id = 1L;
        String newName = "Apple";
        ManufacturerUpdateRequest request = new ManufacturerUpdateRequest(id, newName);

        when(manufacturerRepository.findById(id)).thenReturn(Optional.of(testManufacturer));
        when(manufacturerRepository.findByName(newName)).thenReturn(Optional.empty());

        manufacturerService.updateManufacturer(request);

        assertEquals("Apple", testManufacturer.getName());
        verify(manufacturerRepository, times(1)).findById(id);
        verify(manufacturerRepository, times(1)).findByName(newName);
    }

    @Test
    @DisplayName("Update Manufacturer - Should not check unique name if name is unchanged")
    void updateManufacturer_ShouldNotCheckUniqueName_WhenNameUnchanged() {
        Long id = 1L;
        ManufacturerUpdateRequest request = new ManufacturerUpdateRequest(id, "Samsung");

        when(manufacturerRepository.findById(id)).thenReturn(Optional.of(testManufacturer));

        manufacturerService.updateManufacturer(request);

        assertEquals("Samsung", testManufacturer.getName());
        verify(manufacturerRepository, times(1)).findById(id);
        verify(manufacturerRepository, never()).findByName(anyString());
    }

    @Test
    @DisplayName("Update Manufacturer - Should throw NotFoundException if manufacturer not found")
    void updateManufacturer_ShouldThrowNotFoundExceptionIfManufacturerNotExist() {
        Long id = 99L;
        ManufacturerUpdateRequest request = new ManufacturerUpdateRequest(id, "Apple");

        when(manufacturerRepository.findById(id)).thenReturn(Optional.empty());

        NotFoundException result = assertThrows(NotFoundException.class, () -> manufacturerService.updateManufacturer(request));

        assertEquals("error.manufacturer.not.found", result.getMessage());
        verify(manufacturerRepository, times(1)).findById(id);
        verify(manufacturerRepository, never()).findByName(anyString());
    }

    @Test
    @DisplayName("Update Manufacturer - Should throw AlreadyExistsException if new name is already taken")
    void updateManufacturer_ShouldThrowAlreadyExistsExceptionIfNameIsTaken() {
        Long id = 1L;
        String newName = "LG";
        ManufacturerUpdateRequest request = new ManufacturerUpdateRequest(id, newName);

        Manufacturer existingManufacturer = Manufacturer.builder().id(2L).name(newName).build();

        when(manufacturerRepository.findById(id)).thenReturn(Optional.of(testManufacturer));
        when(manufacturerRepository.findByName(newName)).thenReturn(Optional.of(existingManufacturer));

        AlreadyExistsException result = assertThrows(AlreadyExistsException.class, () -> manufacturerService.updateManufacturer(request));

        assertEquals("error.manufacturer.name.exists", result.getMessage());
        verify(manufacturerRepository, times(1)).findById(id);
        verify(manufacturerRepository, times(1)).findByName(newName);
    }

    @Test
    @DisplayName("Delete Manufacturer By Id - Should successfully delete manufacturer")
    void deleteManufacturerById_ShouldDeleteManufacturer() {
        Long id = 1L;

        when(manufacturerRepository.findById(id)).thenReturn(Optional.of(testManufacturer));

        manufacturerService.deleteManufacturerById(id);

        verify(manufacturerRepository, times(1)).findById(id);
        verify(manufacturerRepository, times(1)).delete(testManufacturer);
        verify(manufacturerRepository, never()).deleteById(anyLong());
    }

    @Test
    @DisplayName("Delete Manufacturer By Id - Should throw NotFoundException if manufacturer not exists")
    void deleteManufacturerById_ShouldThrowNotFoundExceptionIfManufacturerNotExist() {
        Long id = 99L;

        when(manufacturerRepository.findById(id)).thenReturn(Optional.empty());

        NotFoundException result = assertThrows(NotFoundException.class, () -> manufacturerService.deleteManufacturerById(id));

        assertEquals("error.manufacturer.not.found", result.getMessage());
        verify(manufacturerRepository, times(1)).findById(id);
        verify(manufacturerRepository, never()).delete(any(Manufacturer.class));
        verify(manufacturerRepository, never()).deleteById(anyLong());
    }
}