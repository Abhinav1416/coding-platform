package com.Abhinav.backend.features.submissions.model;


import lombok.Getter;
import lombok.RequiredArgsConstructor;

@Getter
@RequiredArgsConstructor
public enum Language {
    JAVA("java", 62),
    PYTHON("python", 71),
    CPP("cpp", 54);

    private final String slug;
    private final int judge0Id;

    public static Language fromSlug(String slug) {
        for (Language lang : Language.values()) {
            if (lang.slug.equalsIgnoreCase(slug)) {
                return lang;
            }
        }
        throw new IllegalArgumentException("Unknown language slug: " + slug);
    }
}