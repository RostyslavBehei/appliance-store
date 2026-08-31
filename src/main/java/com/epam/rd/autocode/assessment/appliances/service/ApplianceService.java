package com.epam.rd.autocode.assessment.appliances.service;

import com.epam.rd.autocode.assessment.appliances.dto.appliance.ApplianceResponse;
import com.epam.rd.autocode.assessment.appliances.dto.appliance.ApplianceSummaryResponse;
import com.epam.rd.autocode.assessment.appliances.dto.appliance.ApplianceUpdateRequest;
import com.epam.rd.autocode.assessment.appliances.model.enums.Category;
import com.epam.rd.autocode.assessment.appliances.model.enums.PowerType;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;

@Service
public interface ApplianceService {
    Page<ApplianceSummaryResponse> getAllAppliance(String name,
                                                   Category category,
                                                   String manufacturerName,
                                                   PowerType powerType,
                                                   Integer power,
                                                   BigDecimal minPrice,
                                                   BigDecimal maxPrice,
                                                   Pageable pageable);

    Page<ApplianceSummaryResponse> getAllApplianceByCategory(Category category, Pageable pageable);
    Page<ApplianceSummaryResponse> getAllApplianceByManufacturerName(String manufacturerName, Pageable pageable);

    ApplianceResponse getApplianceById(Long id);

    void updateAppliance(ApplianceUpdateRequest request);
    void deleteApplianceById(Long applianceId);
}
