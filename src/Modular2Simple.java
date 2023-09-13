import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;
import org.xml.sax.InputSource;
import org.xml.sax.SAXException;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;
import javax.xml.transform.OutputKeys;
import javax.xml.transform.Transformer;
import javax.xml.transform.TransformerException;
import javax.xml.transform.TransformerFactory;
import javax.xml.transform.dom.DOMSource;
import javax.xml.transform.stream.StreamResult;
import java.io.*;
import java.nio.file.Files;
import java.nio.file.StandardCopyOption;
import java.util.*;
import java.util.logging.*;
import java.util.logging.Formatter;
import java.util.zip.ZipEntry;
import java.util.zip.ZipInputStream;
import java.util.zip.ZipOutputStream;

public class Modular2Simple {

    private static final String COMMAND_HELP_FULL = "--help";
    private static final String COMMAND_HELP_SHORT = "-h";
    private static final String COMMAND_XOSC_TO_MOSC_FULL = "--package-xosc-into-mosc";
    private static final String COMMAND_XOSC_TO_MOSC_SHORT = "-cxm";
    private static final String COMMAND_MOSC_TO_XOSC_FULL = "--convert-mosc-to-xosc";
    private static final String COMMAND_MOSC_TO_XOSC_SHORT = "-cmx";
    private static final String MODULAR_FILE_EXTENSION = ".mosc";
    private static final String SIMPLE_FILE_EXTENSION = ".xosc";
    private static final String MAIN_FILE_NAME = "main" + SIMPLE_FILE_EXTENSION;
    private static final String SIMPLE_SCENARIO_PATH_ENV_VAR = "SIMPLE_SCENARIO_PATH";
    private static final String EXTRACTION_PATH = "tmp/";
    private static final String CHANGEABLE_ATTRIBUTES_KEY = "parameterAttributes";
    private static final Logger LOGGER = Logger.getLogger(Modular2Simple.class.getName());
    private static final Level LOGGER_LEVEL = Level.INFO;

    private static List<String> processedMoscFiles = new ArrayList<>();

    public static void main(String[] args) {
        checkCommandLineArguments(args);

        switch (args[0]) {
            case COMMAND_HELP_FULL, COMMAND_HELP_SHORT -> printHelp();
            case COMMAND_XOSC_TO_MOSC_FULL, COMMAND_XOSC_TO_MOSC_SHORT -> packageXoscIntoMosc(args);
            case COMMAND_MOSC_TO_XOSC_FULL, COMMAND_MOSC_TO_XOSC_SHORT -> convertMoscToXosc(args);
            default -> System.out.println("No such option: " + args[0] + ". Use --help to see available options.");
        }
    }

    private static void checkCommandLineArguments(String[] args) {
        switch (args[0]) {
            case COMMAND_XOSC_TO_MOSC_FULL, COMMAND_XOSC_TO_MOSC_SHORT -> {
                for (int i = 1; i < args.length - 1; i++) {
                    if (!args[i].endsWith(SIMPLE_FILE_EXTENSION)) {
                        handleExceptionShutdown("Invalid command-line arguments. Expected: java Modular2Simple %s <input_%s_files> <output_%s_file>"
                                .formatted(args[0], SIMPLE_FILE_EXTENSION.replace(".", ""), MODULAR_FILE_EXTENSION.replace(".", "")));
                    }
                }
                if (!args[args.length - 1].endsWith(MODULAR_FILE_EXTENSION)) {
                    handleExceptionShutdown("Invalid command-line arguments. Expected: java Modular2Simple %s <input_%s_files> <output_%s_file>"
                            .formatted(args[0], SIMPLE_FILE_EXTENSION.replace(".", ""), MODULAR_FILE_EXTENSION.replace(".", "")));
                }
                if (Arrays.stream(args).distinct().count() != args.length) {
                    handleExceptionShutdown("Invalid command-line arguments. File names must be unique");
                }
                if (Arrays.stream(args).filter(arg -> arg.equals(MAIN_FILE_NAME)).toList().size() != 1) {
                    handleExceptionShutdown("Invalid command-line arguments. One of the input files must be named '%s'"
                            .formatted(MAIN_FILE_NAME));
                }
            }
            case COMMAND_MOSC_TO_XOSC_FULL, COMMAND_MOSC_TO_XOSC_SHORT -> {
                if (args.length != 3) {
                    handleExceptionShutdown("Invalid command-line arguments. Expected: java Modular2Simple %s <input_%s_file> <output_%s_file>"
                            .formatted(COMMAND_MOSC_TO_XOSC_FULL, MODULAR_FILE_EXTENSION.replace(".", ""), SIMPLE_FILE_EXTENSION.replace(".", "")));
                }

                if (!args[1].endsWith(MODULAR_FILE_EXTENSION) || !args[2].endsWith(SIMPLE_FILE_EXTENSION)) {
                    handleExceptionShutdown("Invalid command-line arguments. Expected extensions: '%s' and '%s'"
                            .formatted(MODULAR_FILE_EXTENSION, SIMPLE_FILE_EXTENSION));
                }
            }
        }
    }

    private static void printHelp() {
        System.out.println("\n" +
                "Usage:" + "\n" +
                "  java Modular2Simple <command> [options]" + "\n\n" +
                "Commands:" + "\n" +
                "  " + COMMAND_HELP_FULL + ", " + COMMAND_HELP_SHORT + "\t\t\t\t\t\t\t\t" + "Display help message." + "\n" +
                "  " + COMMAND_XOSC_TO_MOSC_FULL + ", " + COMMAND_XOSC_TO_MOSC_SHORT + " <input_xosc_files> <output_mosc_file>" + "\t\t\t\t" + "Package .xosc files into .mosc." + "\n" +
                "  " + COMMAND_MOSC_TO_XOSC_FULL + ", " + COMMAND_MOSC_TO_XOSC_SHORT + " <input_mosc_file> <output_xosc_file>" + "\t" + "Convert a .mosc file to .xosc file." + "\n" +
                "\n" +
                "Examples:" + "\n" +
                "  java Modular2Simple " + COMMAND_HELP_SHORT + "\t\t\t\t\t\t" + "Display help message." + "\n" +
                "  java Modular2Simple " + COMMAND_HELP_FULL + "\t\t\t\t\t\t" + "Display help message." + "\n" +
                "  java Modular2Simple " + COMMAND_XOSC_TO_MOSC_SHORT + " input_1.xosc input_2.xosc output.mosc" + "\t\t\t\t\t" + "Package input_1.xosc input_2.xosc into output.mosc." + "\n" +
                "  java Modular2Simple " + COMMAND_XOSC_TO_MOSC_FULL + " input_1.xosc input_2.xosc output.mosc" + "\t\t\t" + "Package input_1.xosc input_2.xosc into output.mosc." + "\n" +
                "  java Modular2Simple " + COMMAND_MOSC_TO_XOSC_SHORT + " input.mosc output.xosc" + "\t\t\t" + "Convert input.mosc to output.xosc file." + "\n" +
                "  java Modular2Simple " + COMMAND_MOSC_TO_XOSC_FULL + " input.mosc output.xosc" + "\t" + "Convert input.mosc to output.xosc file." + "\n"
        );
    }

    private static void convertMoscToXosc(String[] args) {
        String moscFileName = args[1];
        String resultingFileName = args[2];

        extractFilesFromMosc(moscFileName);

        File fileModular = new File(EXTRACTION_PATH + MAIN_FILE_NAME);
        String fileModular_Content = readContentFromFile(fileModular, moscFileName);

        String fileModular_ModifiedContent = replaceManeuverGroupTags(fileModular_Content, moscFileName, fileModular.getName());

        writeContentToFileResult(new File(resultingFileName), fileModular_ModifiedContent);

        printConversionSuccessLog(fileModular, resultingFileName);

        Runtime.getRuntime().addShutdownHook(new Thread(Modular2Simple::deleteTemporaryExtractionPath));
    }

    // helper methods

    private static void packageXoscIntoMosc(String[] args) {
        String[] xoscFileNames = Arrays.copyOfRange(args, 1, args.length - 1);
        String moscFileName = args[args.length - 1];

        try (ZipOutputStream zipOutputStream = new ZipOutputStream(new FileOutputStream(moscFileName))) {
            for (String xoscFileName : xoscFileNames) {
                checkIfFileExists(xoscFileName);
                File xoscFile = new File(xoscFileName);

                zipOutputStream.putNextEntry(new ZipEntry(xoscFile.getName()));
                try (FileInputStream fileInputStream = new FileInputStream(xoscFile)) {
                    byte[] buffer = new byte[1024];
                    int bytesRead;
                    while ((bytesRead = fileInputStream.read(buffer)) != -1) {
                        zipOutputStream.write(buffer, 0, bytesRead);
                    }
                }
                zipOutputStream.closeEntry();
            }
            LOGGER.info("Files %s packaged into '%s'".formatted(Arrays.toString(xoscFileNames), moscFileName));
        } catch (IOException e) {
            handleExceptionShutdown("Error packaging XOSC files into MOSC", e);
        }
    }

    private static void checkIfFileExists(String filePath) {
        File file = new File(filePath);
        if (!file.exists()) {
            handleExceptionShutdown("File '%s' not found".formatted(file));
        }
    }

    private static void extractFilesFromMosc(String moscFilePath) {
        checkIfFileExists(moscFilePath);

        try (ZipInputStream zipInputStream = new ZipInputStream(new FileInputStream(moscFilePath))) {
            ZipEntry entry;
            while ((entry = zipInputStream.getNextEntry()) != null) {
                String entryName = entry.getName();
                if (!entry.isDirectory()) {
                    File entryFile = new File(EXTRACTION_PATH, entryName);
                    Files.createDirectories(entryFile.getParentFile().toPath());
                    Files.copy(zipInputStream, entryFile.toPath(), StandardCopyOption.REPLACE_EXISTING);
                } else {
                    Files.createDirectories(new File(EXTRACTION_PATH, entryName).toPath());
                }
                zipInputStream.closeEntry();
            }
        } catch (IOException e) {
            handleExceptionShutdown("An error occurred while extracting the " + MODULAR_FILE_EXTENSION + " archive", e);
        }
        LOGGER.fine("Folder for temporary files created");
    }

    private static String readContentFromFile(File file, String parentFileName) {
        if (!file.exists()) {
            handleExceptionShutdown("File '%s' must contain main scenario file '%s'".formatted(parentFileName, file.getName()));
        }

        if (file.getName().equals(MAIN_FILE_NAME)) {
            LOGGER.fine("- Handling modular scenario: '%s'".formatted(file.getName()));
        } else {
            LOGGER.fine("-- Handling modular scenario: '%s'".formatted(file.getName()));
        }

        StringBuilder fileContent = new StringBuilder();

        try (BufferedReader reader = new BufferedReader(new FileReader(file))) {
            String line;
            while ((line = reader.readLine()) != null) {
                fileContent.append(line).append("\n");
            }
        } catch (IOException e) {
            handleExceptionShutdown("An error occurred while reading the file: " + file.getName(), e);
        }

        return fileContent.toString();
    }

    private static String replaceManeuverGroupTags(String fileModularContent, String moscFileName, String fileModularName) {
        try {
            String fileModularPathAndName = moscFileName + "/" + fileModularName;

            DocumentBuilderFactory factory = DocumentBuilderFactory.newInstance();
            DocumentBuilder builder = factory.newDocumentBuilder();
            InputSource modularInputSource = new InputSource(new StringReader(fileModularContent));
            Document modularDocument = builder.parse(modularInputSource);

            NodeList modularNodeList = modularDocument.getElementsByTagName("ScenarioReference");

            List<Node> modularScenarioReferenceNodes = new ArrayList<>();
            for (int i = 0; i < modularNodeList.getLength(); i++) {
                modularScenarioReferenceNodes.add(modularNodeList.item(i));
            }

            for (Node modularScenarioReferenceNode : modularScenarioReferenceNodes) {
                Node modularManeuverGroupNode = modularScenarioReferenceNode.getParentNode();

                String simpleScenarioFileName = getSimpleScenarioFileName(modularScenarioReferenceNode);

                if (simpleScenarioFileName.endsWith(MODULAR_FILE_EXTENSION)) {
                    LOGGER.fine("-- Handling modular scenario: '%s'".formatted(simpleScenarioFileName));
                    simpleScenarioFileName = convertRecursiveModularToSimple(simpleScenarioFileName, fileModularPathAndName);
                }
                if (simpleScenarioFileName.endsWith(SIMPLE_FILE_EXTENSION)) {
                    processedMoscFiles.clear();

                    LOGGER.fine("--- Handling simple scenario: '%s'".formatted(simpleScenarioFileName));

                    NodeList simpleManeuverGroupNodeList = extractManeuverGroupNodesFromSimpleScenario(simpleScenarioFileName, fileModularPathAndName);

                    List<Node> listOfSimpleManeuverGroupNodes = new ArrayList<>();
                    if (simpleManeuverGroupNodeList != null) {
                        for (int i = 0; i < simpleManeuverGroupNodeList.getLength(); i++) {
                            replaceEntityRef(modularManeuverGroupNode, simpleManeuverGroupNodeList.item(i));

                            replaceParameterRefs(modularManeuverGroupNode, simpleManeuverGroupNodeList.item(i));

                            listOfSimpleManeuverGroupNodes.add(simpleManeuverGroupNodeList.item(i));
                        }
                    }
                    replaceNode(modularManeuverGroupNode, listOfSimpleManeuverGroupNodes);
                } else {
                    handleExceptionShutdown("File '%s' refers to a scenario file '%s' with invalid extension. Expected: %s or %s.".formatted(fileModularPathAndName, simpleScenarioFileName, SIMPLE_FILE_EXTENSION, MODULAR_FILE_EXTENSION));
                }
            }
            return nodeToString(modularDocument.getDocumentElement(), false);
        } catch (IOException | ParserConfigurationException | SAXException e) {
            handleExceptionShutdown("An error occurred while processing XML content.", e);
        }

        return "";
    }

    private static void writeContentToFileResult(File fileResult, String fileModular_ModifiedContent) {
        try (BufferedWriter writer = new BufferedWriter(new FileWriter(fileResult))) {
            writer.write(fileModular_ModifiedContent);
        } catch (IOException e) {
            handleExceptionShutdown("An error occurred while writing the file: " + fileResult.getName(), e);
        }
    }

    private static void printConversionSuccessLog(File fileModular, String resultingFileName) {
        if (fileModular.getName().equals(MAIN_FILE_NAME)) {
            LOGGER.fine("- Modular scenario '" + fileModular.getName() + "' converted to a simple scenario");
        } else {
            LOGGER.fine("-- Modular scenario '" + fileModular.getName() + "' converted to a simple scenario");
        }
        LOGGER.info("File conversion completed. Modified content written to '%s'".formatted(resultingFileName));
    }

    private static void deleteTemporaryExtractionPath() {
        File extractionPath = new File(EXTRACTION_PATH);
        deleteFileOrDirectoryRecursively(extractionPath);

        LOGGER.fine("Folder for temporary files deleted");
    }

    private static File getFileFromEnvironment(String fileSimpleName, String fileModularPathAndName) {
        String[] environmentVariableValues = getEnvironmentVariableValues();

        for (String oneEnvironmentVariableValue : environmentVariableValues) {
            File directory = new File(oneEnvironmentVariableValue);
            File candidateFile = new File(directory, fileSimpleName);

            if (candidateFile.exists()) {
                return candidateFile;
            }
        }

        handleExceptionShutdown("File '%s' refers to file '%s' that is not found in the specified directories.".formatted(fileModularPathAndName, fileSimpleName), new FileNotFoundException());
        return null;
    }

    private static String[] getEnvironmentVariableValues() {
        String value = System.getenv(SIMPLE_SCENARIO_PATH_ENV_VAR);
        if (value == null) {
            LOGGER.warning("Environment variable '" + SIMPLE_SCENARIO_PATH_ENV_VAR + "' is not set.");
            return new String[0];
        } else {
            return value.split(";");
        }
    }

    private static String getSimpleScenarioFileName(Node modularScenarioReferenceNode) {
        return modularScenarioReferenceNode.getAttributes().getNamedItem("scenarioFileName").getNodeValue();
    }

    private static NodeList extractManeuverGroupNodesFromSimpleScenario(String simpleScenarioFileName, String fileModularPathAndName) {
        File fileSimple = new File(EXTRACTION_PATH + simpleScenarioFileName);
        if (!fileSimple.exists()) {
            fileSimple = getFileFromEnvironment(simpleScenarioFileName, fileModularPathAndName);
        }
        String fileSimpleContent = "";
        if (fileSimple != null) {
            fileSimpleContent = readContentFromFile(fileSimple, fileModularPathAndName);
        }

        try {
            DocumentBuilderFactory factory = DocumentBuilderFactory.newInstance();
            DocumentBuilder builder = factory.newDocumentBuilder();
            InputSource inputSource = new InputSource(new StringReader(fileSimpleContent));
            Document document = builder.parse(inputSource);

            return document.getElementsByTagName("ManeuverGroup");
        } catch (ParserConfigurationException | SAXException | IOException e) {
            handleExceptionShutdown("An error occurred while extracting ManeuverGroup tag from simple scenario.", e);
        }
        return null;
    }

    private static String convertRecursiveModularToSimple(String scenarioFileName, String fileModularPathAndName) {
        File moscFile = new File(EXTRACTION_PATH + scenarioFileName);
        if (!moscFile.exists()) {
            moscFile = getFileFromEnvironment(scenarioFileName, fileModularPathAndName);
        }

        String resultingFileName = EXTRACTION_PATH + scenarioFileName.replace(MODULAR_FILE_EXTENSION, SIMPLE_FILE_EXTENSION);

        if (moscFile != null) {
            checkCircularReferences(moscFile.getAbsolutePath());

            main(new String[]{COMMAND_MOSC_TO_XOSC_FULL, moscFile.getAbsolutePath(), resultingFileName});
        }

        return scenarioFileName.replace(MODULAR_FILE_EXTENSION, SIMPLE_FILE_EXTENSION);
    }

    private static void checkCircularReferences(String fileNameAndPath) {
        if (processedMoscFiles.contains(fileNameAndPath)) {
            handleExceptionShutdown("Circular references detected in the following files:" + "\n" + String.join("\n", processedMoscFiles));
        } else {
            processedMoscFiles.add(fileNameAndPath);
        }
    }

    private static void replaceEntityRef(Node modularManeuverGroupNode, Node simpleManeuverGroupNode) {
        Node entityRefNode = ((Element) modularManeuverGroupNode).getElementsByTagName("EntityRef").item(0);
        String entityRef = entityRefNode.getAttributes().getNamedItem("entityRef").getNodeValue();

        Node simpleEntityRefNode = ((Element) simpleManeuverGroupNode).getElementsByTagName("EntityRef").item(0);
        simpleEntityRefNode.getAttributes().getNamedItem("entityRef").setNodeValue(entityRef);
    }

    private static void replaceParameterRefs(Node modularManeuverGroupNode, Node simpleManeuverGroupNode) {
        Map<String, String> parameterReferenceKeyValuePairs =
                getParameterReferenceKeyValuePairs(modularManeuverGroupNode);
        replaceParameterAttributesWithParameterReferenceKeys(simpleManeuverGroupNode, parameterReferenceKeyValuePairs);
    }

    private static Map<String, String> getParameterReferenceKeyValuePairs(Node node) {
        NodeList parameterReferenceNodeList = ((Element) node).getElementsByTagName("ParameterReference");

        Map<String, String> parameterReferenceKeyValuePairs = new HashMap<>();
        for (int i = 0; i < parameterReferenceNodeList.getLength(); i++) {
            Node parameterReferenceNode = parameterReferenceNodeList.item(i);
            String parameterReferenceKey = parameterReferenceNode.getAttributes().item(0).getNodeValue();
            String parameterReferenceValue = parameterReferenceNode.getAttributes().item(1).getNodeValue();

            parameterReferenceKeyValuePairs.put(parameterReferenceKey, parameterReferenceValue);
        }

        return parameterReferenceKeyValuePairs;
    }

    private static void replaceParameterAttributesWithParameterReferenceKeys(Node node, Map<String, String> parameterReferenceKeyValuePairs) {
        NodeList childNodes = node.getChildNodes();
        for (int j = 0; j < childNodes.getLength(); j++) {
            Node childNode = childNodes.item(j);
            if (childNode instanceof Element childElement) {
                String parameterAttributes = childElement.getAttribute(CHANGEABLE_ATTRIBUTES_KEY);
                if (!parameterAttributes.isEmpty()) {
                    childElement.removeAttribute(CHANGEABLE_ATTRIBUTES_KEY);
                    Arrays.stream(parameterAttributes.split(","))
                            .forEach(attribute -> processAttribute(parameterReferenceKeyValuePairs, attribute.trim(), childElement));
                }
                replaceParameterAttributesWithParameterReferenceKeys(childNode, parameterReferenceKeyValuePairs);
            }
            LOGGER.fine("---- Node " + nodeToString(childNode, true) + " processed");
        }
    }

    private static void processAttribute(Map<String, String> parameterReferenceKeyValuePairs, String attribute, Element childElement) {
        if (childElement.hasAttribute(attribute)) {
            if (parameterReferenceKeyValuePairs.containsKey(attribute)) {
                String newValue = parameterReferenceKeyValuePairs.get(attribute);
                childElement.setAttribute(attribute, newValue);
            } else {
                parameterReferenceKeyValuePairs.keySet()
                        .forEach(key -> processParameterReferenceKey(parameterReferenceKeyValuePairs, attribute, childElement, key));
            }
        }
    }

    private static void processParameterReferenceKey(Map<String, String> parameterReferenceKeyValuePairs, String attribute, Element childElement, String key) {
        if (key.endsWith(attribute)) {
            String[] parentsArrayWithKey = key.split("\\.");
            String[] parentsArray = new String[parentsArrayWithKey.length - 1];
            if (parentsArray.length - 1 >= 0) {
                System.arraycopy(parentsArrayWithKey, 0, parentsArray, 0, parentsArray.length);
            }
            String attributeWithParents = parentsArray[parentsArray.length - 1] + "." + attribute;
            if (key.equals(attributeWithParents) &&
                    parentsArray[parentsArray.length - 1].equals(childElement.getNodeName())) {
                String newValue = parameterReferenceKeyValuePairs.get(attributeWithParents);
                childElement.setAttribute(attribute, newValue);
            } else if (key.endsWith(attributeWithParents) &&
                    parentsArray[parentsArray.length - 1].equals(childElement.getNodeName())) {
                Node tempNode = childElement.getParentNode();
                for (int i = 2; i <= parentsArray.length; i++) {
                    attributeWithParents = parentsArray[parentsArray.length - i] + "." + attributeWithParents;
                    if (key.equals(attributeWithParents) &&
                            parentsArray[parentsArray.length - i].equals(tempNode.getNodeName())) {
                        String newValue = parameterReferenceKeyValuePairs.get(attributeWithParents);
                        childElement.setAttribute(attribute, newValue);
                    } else {
                        tempNode = tempNode.getParentNode();
                    }
                }
            }
        }
    }

    private static void replaceNode(Node oldNode, List<Node> newNodes) {
        Document ownerDocument = oldNode.getOwnerDocument();
        Node parent = oldNode.getParentNode();

        if (parent != null) {
            for (int i = 0; i < newNodes.size(); i++) {
                Node newNode = ownerDocument.importNode(newNodes.get(i), true);
                parent.insertBefore(newNode, oldNode);

                if (i < newNodes.size() - 1) {
                    parent.insertBefore(ownerDocument.createTextNode("\n\n        "), oldNode);
                }
            }

            parent.removeChild(oldNode);
        }
    }

    private static String nodeToString(Node node, Boolean omitXmlDeclaration) {
        try {
            StringWriter writer = new StringWriter();
            TransformerFactory transformerFactory = TransformerFactory.newInstance();
            Transformer transformer = transformerFactory.newTransformer();
            if (omitXmlDeclaration) {
                transformer.setOutputProperty(OutputKeys.OMIT_XML_DECLARATION, "yes");
            }
            transformer.transform(new DOMSource(node), new StreamResult(writer));
            return writer.toString()
                    .replace("&lt;", "<")
                    .replace("&gt;", ">");
        } catch (TransformerException e) {
            handleExceptionShutdown("An error occurred while transforming a Node to String", e);
        }

        return "";
    }

    private static void handleExceptionShutdown(String message, Exception e) {
        StringWriter sw = new StringWriter();
        e.printStackTrace(new PrintWriter(sw));
        String exceptionStackTrace = sw.toString();

        LOGGER.log(Level.SEVERE, message + "\n\n" + exceptionStackTrace);
        deleteTemporaryExtractionPath();
        System.exit(1);
    }

    private static void handleExceptionShutdown(String message) {
        LOGGER.log(Level.SEVERE, message);
        deleteTemporaryExtractionPath();
        System.exit(1);
    }

    private static void deleteFileOrDirectoryRecursively(File fileOrDirectory) {
        if (fileOrDirectory.isDirectory()) {
            File[] files = fileOrDirectory.listFiles();
            if (files != null) {
                for (File file : files) {
                    deleteFileOrDirectoryRecursively(file);
                }
            }
        }
        try {
            Files.deleteIfExists(fileOrDirectory.toPath());
        } catch (IOException e) {
            handleExceptionShutdown("An error occurred while deleting file/directory: " + fileOrDirectory.getAbsolutePath(), e);
        }
    }

    static {
        Formatter formatter = new SimpleFormatter() {
            @Override
            public String format(LogRecord record) {
                return record.getLevel() + ": " + record.getMessage() + "\n";
            }
        };

        ConsoleHandler handler = new ConsoleHandler();
        handler.setFormatter(formatter);
        handler.setLevel(LOGGER_LEVEL);

        LOGGER.setUseParentHandlers(false);
        LOGGER.addHandler(handler);
        LOGGER.setLevel(LOGGER_LEVEL);
    }
}

//// introduce and check new environment variable
// export SIMPLE_SCENARIO_PATH="/Users/Nikolai/Desktop/ScenarioLibrary"
// printenv

//// run the program
// javac Modular2Simple.java
// java Modular2Simple --cmx modular.mosc resultingScenario.xosc
