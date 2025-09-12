package com.Abhinav.backend.features.problemManagement.utils;

import org.springframework.stereotype.Component;

import java.util.Map;
import java.util.stream.Collectors;

@Component
public class BoilerplateGenerator {

    private static final Map<String, String> LANGUAGE_TEMPLATES = Map.of(
            "71", // Python
            "class Solution:\n    def {{method_signature}}:\n        # Your code here\n        pass",
            "62", // Java
            "class Solution {\n    public {{method_signature}} {\n        // Your code here\n    }\n}",
            "63", // JavaScript
            "/**\n * @param ... \n * @return ...\n */\nvar {{method_signature}} = function(",
            "54", // C++
            "#include <vector>\n#include <string>\n\nclass Solution {\npublic:\n    {{method_signature}} {\n        // Your code here\n    }\n};"
    );

    public Map<String, String> generateUserBoilerplates(Map<String, String> methodSignatures) {
        return methodSignatures.entrySet().stream()
                .collect(Collectors.toMap(
                        Map.Entry::getKey,
                        entry -> {
                            String template = LANGUAGE_TEMPLATES.get(entry.getKey());
                            if (template == null) {
                                throw new IllegalArgumentException("Unsupported language ID: " + entry.getKey());
                            }
                            // Replace the placeholder with the actual method signature
                            return template.replace("{{method_signature}}", entry.getValue());
                        }
                ));
    }
}