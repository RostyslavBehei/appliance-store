package com.epam.rd.autocode.assessment.appliances.model.enums;

import java.util.Arrays;
import java.util.List;

public enum Category {
    BIG,
    SMALL;

    public static List<Category> getCategories() {
        return Arrays.asList(Category.values());
    }
}
