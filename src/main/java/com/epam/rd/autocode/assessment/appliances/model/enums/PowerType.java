package com.epam.rd.autocode.assessment.appliances.model.enums;

import java.util.Arrays;
import java.util.List;

public enum PowerType {
    AC220,
    AC110,
    ACCUMULATOR;

    public static List<PowerType> getPowerTypes() {
        return Arrays.asList(PowerType.values());
    }
}
