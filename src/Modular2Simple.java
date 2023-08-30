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

public class Modular2Simple {

    private static final String COMMAND_HELP_FULL = "--help";
    private static final String COMMAND_HELP_SHORT = "-h";
    private static final String COMMAND_ZIP_TO_MOSC_FULL = "--convert-zip-to-mosc";
    private static final String COMMAND_ZIP_TO_MOSC_SHORT = "-czm";
    private static final String COMMAND_MOSC_TO_XOSC_FULL = "--convert-mosc-to-xosc";
    private static final String COMMAND_MOSC_TO_XOSC_SHORT = "-cmx";
    private static final String ZIP_FILE_EXTENSION = ".zip";
    private static final String MODULAR_FILE_EXTENSION = ".mosc";
    private static final String SIMPLE_FILE_EXTENSION = ".xosc";
    private static final String MAIN_FILE_NAME = "main" + SIMPLE_FILE_EXTENSION;
    private static final String SIMPLE_SCENARIO_PATH_ENV_VAR = "SIMPLE_SCENARIO_PATH";
    private static final String EXTRACTION_PATH = "tmp/";
    private static final Logger LOGGER = Logger.getLogger(Modular2Simple.class.getName());
    private static final Level LOGGER_LEVEL = Level.INFO;

    private static List<String> processedMoscFiles = new ArrayList<>();

    public static void main(String[] args) {
        checkCommandLineArguments(args);

        switch (args[0]) {
            case COMMAND_HELP_FULL, COMMAND_HELP_SHORT -> printHelp();
            case COMMAND_ZIP_TO_MOSC_FULL, COMMAND_ZIP_TO_MOSC_SHORT -> convertZipToMosc(args[1]);
            case COMMAND_MOSC_TO_XOSC_FULL, COMMAND_MOSC_TO_XOSC_SHORT -> convertMoscToXosc(args[1], args[2]);
            default -> System.out.println("No such option: " + args[0] + ". Use --help to see available options.");
        }
    }

    private static void checkCommandLineArguments(String[] args) {
        switch (args[0]) {
            case COMMAND_ZIP_TO_MOSC_FULL, COMMAND_ZIP_TO_MOSC_SHORT -> {
                if (args.length != 2) {
                    handleExceptionShutdown("Invalid command-line arguments. Expected: java Modular2Simple %s <input_%s_file>"
                            .formatted(COMMAND_ZIP_TO_MOSC_FULL, ZIP_FILE_EXTENSION.replace(".", "")));
                }

                if (!args[1].endsWith(ZIP_FILE_EXTENSION)) {
                    handleExceptionShutdown("Invalid command-line arguments. Expected extension: " + ZIP_FILE_EXTENSION);
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
                "  " + COMMAND_ZIP_TO_MOSC_FULL + ", " + COMMAND_ZIP_TO_MOSC_SHORT + " <input_zip_file>" + "\t\t\t\t" + "Convert a .zip file to .mosc format." + "\n" +
                "  " + COMMAND_MOSC_TO_XOSC_FULL + ", " + COMMAND_MOSC_TO_XOSC_SHORT + " <input_mosc_file> <output_xosc_file>" + "\t" + "Convert a .mosc file to .xosc file." + "\n" +
                "\n" +
                "Examples:" + "\n" +
                "  java Modular2Simple " + COMMAND_HELP_SHORT + "\t\t\t\t\t\t" + "Display help message." + "\n" +
                "  java Modular2Simple " + COMMAND_HELP_FULL + "\t\t\t\t\t\t" + "Display help message." + "\n" +
                "  java Modular2Simple " + COMMAND_ZIP_TO_MOSC_SHORT + " input.zip" + "\t\t\t\t\t" + "Convert input.zip to .mosc format." + "\n" +
                "  java Modular2Simple " + COMMAND_ZIP_TO_MOSC_FULL + " input.zip" + "\t\t\t" + "Convert input.zip to .mosc format." + "\n" +
                "  java Modular2Simple " + COMMAND_MOSC_TO_XOSC_SHORT + " input.mosc output.xosc" + "\t\t\t" + "Convert input.mosc to output.xosc file." + "\n" +
                "  java Modular2Simple " + COMMAND_MOSC_TO_XOSC_FULL + " input.mosc output.xosc" + "\t" + "Convert input.mosc to output.xosc file." + "\n"
        );
    }

    private static void convertZipToMosc(String... fileNames) {
        String zipFilePath = fileNames[0];
        checkIfFileExists(zipFilePath);

        File zipFile = new File(zipFilePath);
        File moscFile = new File(zipFile.getAbsolutePath().replace(ZIP_FILE_EXTENSION, MODULAR_FILE_EXTENSION));

        if (zipFile.renameTo(moscFile)) {
            LOGGER.info("File conversion completed. Modified file: '%s'".formatted(moscFile.getName()));
        } else {
            handleExceptionShutdown("Failed to convert '%s' to '%s'".formatted(zipFilePath, moscFile.getName()));
        }
    }

    private static void convertMoscToXosc(String... fileNames) {
        extractFilesFromMosc(fileNames[0]);

        File fileModular = new File(EXTRACTION_PATH + MAIN_FILE_NAME);
        String fileModular_Content = readContentFromFile(fileModular, fileNames[0]);

        String fileModular_ModifiedContent = replaceManeuverGroupTags(fileModular_Content, fileNames[0], fileModular.getName());

        writeContentToFileResult(new File(fileNames[1]), fileModular_ModifiedContent);

        printConversionSuccessLog(fileModular, fileNames[1]);

        Runtime.getRuntime().addShutdownHook(new Thread(Modular2Simple::deleteTemporaryExtractionPath));
    }

    // helper methods

    private static void checkIfFileExists(String filePath) {
        File moscFile = new File(filePath);
        if (!moscFile.exists()) {
            handleExceptionShutdown("File '%s' not found".formatted(moscFile));
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
        processedMoscFiles.add(fileNameAndPath);

        if (processedMoscFiles.size() % 50 == 0) {
            List<String> cyclingFiles = findCyclingFiles();

            boolean isCircularReferences = cyclingFiles.stream()
                    .filter(oneCyclingFile -> Collections.frequency(processedMoscFiles, oneCyclingFile) >= 15)
                    .count() == cyclingFiles.size();

            if (isCircularReferences) {
                String cyclingFileNames = String.join("\n", cyclingFiles);
                handleExceptionShutdown("Circular references detected in the following files:" + "\n" + cyclingFileNames);
            }
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
            if (childNode instanceof Element) {
                String parameterAttributes = ((Element) childNode).getAttribute("parameterAttributes");
                if (!parameterAttributes.isEmpty()) {
                    ((Element) childNode).removeAttribute("parameterAttributes");
                    String[] parameterAttributesArray = parameterAttributes.split(",");
                    for (String attribute : parameterAttributesArray) {
                        attribute = attribute.trim();
                        if (((Element) childNode).hasAttribute(attribute)) {
                            String newValue = parameterReferenceKeyValuePairs.get(attribute);
                            ((Element) childNode).setAttribute(attribute, newValue);
                        }
                    }
                }
                replaceParameterAttributesWithParameterReferenceKeys(childNode, parameterReferenceKeyValuePairs);
            }
            LOGGER.fine("---- Node " + nodeToString(childNode, true) + " processed");
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

    private static List<String> findCyclingFiles() {
        for (int i = 0; i < processedMoscFiles.size(); i++) {
            List<String> sequence = new ArrayList<>();
            int currentIndex = i;

            while (currentIndex < processedMoscFiles.size()) {
                String currentFile = processedMoscFiles.get(currentIndex);
                sequence.add(currentFile);

                if (sequence.size() > 1 && sequence.get(0).equals(sequence.get(sequence.size() - 1))) {
                    return sequence.subList(0, sequence.size() - 1);
                }

                currentIndex++;
            }
        }
        return new ArrayList<>();
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
