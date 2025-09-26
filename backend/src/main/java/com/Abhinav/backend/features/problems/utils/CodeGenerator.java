package com.Abhinav.backend.features.problems.utils;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

public class CodeGenerator {

    public static final String USER_CODE_PLACEHOLDER = "#### USER CODE HERE ####";

    public static Map<String, String> generateFullBoilerplates(String genericSignature) {
        SignatureInfo signatureInfo = parseSignature(genericSignature);
        Map<String, String> boilerplates = new HashMap<>();

        boilerplates.put("cpp", generateCppFullBoilerplate(signatureInfo));
        boilerplates.put("java", generateJavaFullBoilerplate(signatureInfo));
        boilerplates.put("python", generatePythonFullBoilerplate(signatureInfo));

        return boilerplates;
    }

    public static Map<String, String> generateUserBoilerplates(String genericSignature) {
        SignatureInfo signatureInfo = parseSignature(genericSignature);
        Map<String, String> boilerplates = new HashMap<>();

        boilerplates.put("cpp", generateCppUserBoilerplate(signatureInfo));
        boilerplates.put("java", generateJavaUserBoilerplate(genericSignature));
        boilerplates.put("python", generatePythonUserBoilerplate(signatureInfo));

        return boilerplates;
    }

    private static SignatureInfo parseSignature(String signature) {
        Pattern pattern = Pattern.compile("([\\w<>,\\s\\[\\]]+?)\\s+(\\w+)\\s*\\((.*)\\)");
        Matcher matcher = pattern.matcher(signature.trim());

        if (!matcher.matches()) {
            throw new IllegalArgumentException("Invalid method signature format: " + signature);
        }

        String returnType = matcher.group(1).trim();
        String methodName = matcher.group(2).trim();
        String paramsString = matcher.group(3).trim();

        List<Parameter> parameters = new ArrayList<>();
        if (!paramsString.isEmpty()) {
            parameters = Arrays.stream(paramsString.split(","))
                    .map(String::trim)
                    .map(paramStr -> {
                        int lastSpace = paramStr.lastIndexOf(' ');
                        if (lastSpace == -1) {
                            throw new IllegalArgumentException("Invalid parameter format: " + paramStr);
                        }
                        String type = paramStr.substring(0, lastSpace).trim();
                        String name = paramStr.substring(lastSpace + 1).trim();
                        return new Parameter(type, name);
                    })
                    .collect(Collectors.toList());
        }

        return new SignatureInfo(returnType, methodName, parameters);
    }

    private static String javaTypeToCpp(String javaType) {
        return switch (javaType) {
            case "int" -> "int";
            case "long" -> "long long";
            case "double" -> "double";
            case "boolean" -> "bool";
            case "String" -> "std::string";
            case "int[]", "List<Integer>" -> "std::vector<int>";
            case "String[]", "List<String>" -> "std::vector<std::string>";
            default ->
                    javaType;
        };
    }

    private static String javaTypeToPython(String javaType) {
        return switch (javaType) {
            case "int", "long" -> "int";
            case "double" -> "float";
            case "boolean" -> "bool";
            case "String" -> "str";
            case "int[]", "List<Integer>" -> "list[int]";
            case "String[]", "List<String>" -> "list[str]";
            default -> "any";
        };
    }


    private static String generateJavaUserBoilerplate(String genericSignature) {
        // We will now recommend the user also includes imports in their submission
        // for clarity, although our new full boilerplate can handle it.
        return "import java.util.*;\n\n" +
                "class Solution {\n    public " + genericSignature + " {\n        // Your code here\n    }\n}";
    }

    private static String generateCppUserBoilerplate(SignatureInfo info) {
        String returnType = javaTypeToCpp(info.returnType());
        String params = info.parameters().stream()
                .map(p -> javaTypeToCpp(p.type()) + " " + p.name())
                .collect(Collectors.joining(", "));

        return "#include <iostream>\n#include <vector>\n#include <string>\n\n" +
                "class Solution {\npublic:\n    " +
                returnType + " " + info.methodName() + "(" + params + ") {\n" +
                "        // Your code here\n" +
                "    }\n};";
    }

    private static String generatePythonUserBoilerplate(SignatureInfo info) {
        String params = info.parameters().stream()
                .map(p -> p.name() + ": " + javaTypeToPython(p.type()))
                .collect(Collectors.joining(", "));
        String returnType = javaTypeToPython(info.returnType());

        return "class Solution:\n    def " +
                info.methodName() + "(self, " + params + ") -> " + returnType + ":\n" +
                "        # Your code here\n";
    }

    private static String generateCppFullBoilerplate(SignatureInfo info) {
        String userBoilerplate = generateCppUserBoilerplate(info).replace("        // Your code here\n", USER_CODE_PLACEHOLDER);

        StringBuilder sb = new StringBuilder();
        sb.append("#include <sstream>\n\n");
        sb.append(userBoilerplate).append("\n\n");
        sb.append("int main() {\n");
        sb.append("    Solution sol;\n");

        info.parameters.forEach(p -> sb.append(getCppInputReading(p.type(), p.name())));

        String paramsList = info.parameters.stream().map(Parameter::name).collect(Collectors.joining(", "));
        sb.append("    auto result = sol.").append(info.methodName()).append("(").append(paramsList).append(");\n");

        sb.append(getCppOutputPrinting("result", info.returnType()));
        sb.append("    return 0;\n");
        sb.append("}\n");

        return sb.toString();
    }

    private static String getCppInputReading(String type, String name) {
        return switch (type) {
            case "int" -> "    int " + name + ";\n    std::cin >> " + name + ";\n";
            case "long" -> "    long long " + name + ";\n    std::cin >> " + name + ";\n";
            case "double" -> "    double " + name + ";\n    std::cin >> " + name + ";\n";
            case "String" -> "    std::string " + name + ";\n    std::cin >> " + name + ";\n";
            case "boolean" -> "    bool " + name + ";\n    std::cin >> " + name + ";\n";
            case "int[]", "List<Integer>" -> """
                    std::vector<int> %s;
                    std::string line_%s;
                    std::getline(std::cin >> std::ws, line_%s);
                    std::stringstream ss_%s(line_%s);
                    int num_%s;
                    while (ss_%s >> num_%s) { %s.push_back(num_%s); }
                """.formatted(name, name, name, name, name, name, name, name, name, name);
            case "String[]", "List<String>" -> """
                    std::vector<std::string> %s;
                    std::string line_%s;
                    std::getline(std::cin >> std::ws, line_%s);
                    std::stringstream ss_%s(line_%s);
                    std::string word_%s;
                    while (ss_%s >> word_%s) { %s.push_back(word_%s); }
                """.formatted(name, name, name, name, name, name, name, name, name, name);
            default -> "    // TODO: Implement C++ input for type " + type + "\n";
        };
    }

    private static String getCppOutputPrinting(String varName, String returnType) {
        return switch (returnType) {
            case "boolean" -> "    std::cout << (" + varName + " ? \"true\" : \"false\") << std::endl;\n";
            case "int[]", "List<Integer>", "String[]", "List<String>" -> """
                    for (size_t i = 0; i < %s.size(); ++i) {
                        std::cout << %s[i] << (i == %s.size() - 1 ? "" : " ");
                    }
                    std::cout << std::endl;
                """.formatted(varName, varName, varName);
            default -> "    std::cout << " + varName + " << std::endl;\n";
        };
    }

    // =========================================================================
    //  START OF THE FIX
    // =========================================================================
    private static String generateJavaFullBoilerplate(SignatureInfo info) {
        StringBuilder sb = new StringBuilder();

        // Step 1: Add standard boilerplate imports first.
        sb.append("import java.io.*;\n");
        sb.append("import java.util.*;\n");
        sb.append("import java.util.stream.Collectors;\n\n");

        // Step 2: Place the placeholder for the user's code HERE.
        // This allows the user's code (which includes its own imports and the Solution class)
        // to be placed at the top of the file, making the final code syntactically valid.
        sb.append(USER_CODE_PLACEHOLDER).append("\n\n");

        // Step 3: Add the Main class which will act as the driver for the Solution class.
        sb.append("public class Main {\n");
        sb.append("    public static void main(String[] args) throws IOException {\n");
        sb.append("        BufferedReader br = new BufferedReader(new InputStreamReader(System.in));\n");
        sb.append("        Solution sol = new Solution();\n\n");

        info.parameters().forEach(p -> sb.append(getJavaInputReading(p.type(), p.name())));

        String paramsList = info.parameters().stream().map(Parameter::name).collect(Collectors.joining(", "));
        sb.append("        ").append(info.returnType()).append(" result = sol.").append(info.methodName())
                .append("(").append(paramsList).append(");\n\n");

        sb.append(getJavaOutputPrinting("result", info.returnType()));

        sb.append("    }\n");
        sb.append("}\n");

        return sb.toString();
    }
    // =========================================================================
    //  END OF THE FIX
    // =========================================================================

    private static String getJavaInputReading(String type, String name) {
        return switch (type) {
            case "int" -> "        int " + name + " = Integer.parseInt(br.readLine());\n";
            case "long" -> "        long " + name + " = Long.parseLong(br.readLine());\n";
            case "double" -> "        double " + name + " = Double.parseDouble(br.readLine());\n";
            case "String" -> "        String " + name + " = br.readLine();\n";
            case "boolean" -> "        boolean " + name + " = Boolean.parseBoolean(br.readLine());\n";
            case "int[]" -> """
                        String[] %sStr = br.readLine().split(" ");
                        int[] %s = new int[%sStr.length];
                        for (int i = 0; i < %sStr.length; i++) {
                            %s[i] = Integer.parseInt(%sStr[i]);
                        }
                    """.formatted(name, name, name, name, name, name);
            case "List<Integer>" -> "        List<Integer> " + name + " = Arrays.stream(br.readLine().split(\" \"))"
                    + ".map(Integer::parseInt).collect(Collectors.toList());\n";
            case "String[]" -> "        String[] " + name + " = br.readLine().split(\" \");\n";
            case "List<String>" -> "        List<String> " + name + " = Arrays.stream(br.readLine().split(\" \"))"
                    + ".collect(Collectors.toList());\n";
            default -> "        // TODO: Implement Java input for type " + type + "\n";
        };
    }

    private static String getJavaOutputPrinting(String varName, String returnType) {
        return switch (returnType) {
            case "int[]" -> "        System.out.println(Arrays.toString(" + varName + ").replaceAll(\"\\\\[|\\\\]|,|\", \"\"));\n";
            case "String[]" -> "        System.out.println(String.join(\" \", " + varName + "));\n";
            case "List<Integer>", "List<String>" -> "        String output = " + varName + ".stream()"
                    + ".map(Object::toString).collect(Collectors.joining(\" \"));\n"
                    + "        System.out.println(output);\n";
            default -> "        System.out.println(" + varName + ");\n";
        };
    }

    private static String generatePythonFullBoilerplate(SignatureInfo info) {
        StringBuilder sb = new StringBuilder();
        sb.append("import sys\n\n");
        sb.append("# ").append(USER_CODE_PLACEHOLDER).append("\n\n");
        sb.append("if __name__ == '__main__':\n");
        sb.append("    sol = Solution()\n");

        info.parameters().forEach(p -> sb.append(getPythonInputReading(p.type(), p.name())));

        String paramsList = info.parameters().stream().map(Parameter::name).collect(Collectors.joining(", "));
        sb.append("    result = sol.").append(info.methodName()).append("(").append(paramsList).append(")\n");
        sb.append(getPythonOutputPrinting("result", info.returnType()));
        return sb.toString();
    }

    private static String getPythonInputReading(String type, String name) {
        return switch (type) {
            case "int" -> "    " + name + " = int(sys.stdin.readline().strip())\n";
            case "long" -> "    " + name + " = int(sys.stdin.readline().strip())\n"; // Python 'int' handles large numbers
            case "double" -> "    " + name + " = float(sys.stdin.readline().strip())\n";
            case "String" -> "    " + name + " = sys.stdin.readline().strip()\n";
            case "boolean" -> "    " + name + " = sys.stdin.readline().strip().lower() == 'true'\n";
            case "int[]", "List<Integer>" -> "    " + name + " = list(map(int, sys.stdin.readline().strip().split()))\n";
            case "String[]", "List<String>" -> "    " + name + " = sys.stdin.readline().strip().split()\n";
            default -> "    # TODO: Implement Python input for type " + type + "\n";
        };
    }

    private static String getPythonOutputPrinting(String varName, String returnType) {
        return switch (returnType) {
            case "boolean" -> "    print(str(" + varName + ").lower())\n";
            case "int[]", "List<Integer>", "String[]", "List<String>" -> "    print(' '.join(map(str, " + varName + ")))\n";
            default -> "    print(" + varName + ")\n";
        };
    }

    private record SignatureInfo(String returnType, String methodName, List<Parameter> parameters) {}

    private record Parameter(String type, String name) {}
}