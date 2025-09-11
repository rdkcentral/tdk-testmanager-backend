/*
* If not stated otherwise in this file or this component's Licenses.txt file the
* following copyright and licenses apply:
*
* Copyright 2024 RDK Management
*
* Licensed under the Apache License, Version 2.0 (the "License");
* you may not use this file except in compliance with the License.
* You may obtain a copy of the License at
*
*
http://www.apache.org/licenses/LICENSE-2.0
*
* Unless required by applicable law or agreed to in writing, software
* distributed under the License is distributed on an "AS IS" BASIS,
* WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
* See the License for the specific language governing permissions and
* limitations under the License.
*/
package com.rdkm.tdkservice.util;

/*
 * 	This class is used to store the constants used in the application
 */
public class Constants {

	public static final String LINE_STRING = "======";
	// Default user role for the user if not provided
	public static final String DEFAULT_USER_ROLE = "tester";

	// Default user group for the user if not provided
	public static final String DEFAULT_THEME_NAME = "DARK";

	// Default user group for the user if not provided
	public static final String HMACSHA256 = "HmacSHA256";

	// File store location
	public static final String FILESTORE_LOCATION = "classpath:/filestore/";

	// Default device config file name in filestore
	public static final String DEFAULT_DEVICE_CONFIG_FILE = "sampleDevice.config";

	// Default device config file name in filestore
	public static final String THUNDER_DEVICE_CONFIG_FILE = "sampleDevice.config";

	// Config file extension
	public static final String CONFIG_FILE_EXTENSION = ".config";

	// Base filestore directory
	public static final String BASE_FILESTORE_DIR = "fileStore";

	// Slash as file path seperator
	public static final String FILE_PATH_SEPERATOR = "/";

	// TDKV device config directory inside filestore
	public static final String TDKV_DEVICE_CONFIG_DIR = "tdkvDeviceConfig";

	// Empty string
	public static final String EMPTY_STRING = "";

	// Category
	public static final String CATEGORY = "Category";

	// Oem Id
	public static final String OEM_ID = "oem Id";

	// Oem Name
	public static final String OEM_NAME = "oem Name";

	// Soc Id
	public static final String SOC_ID = "Soc Id";

	// Soc name
	public static final String SOC_NAME = "Soc";

	// User group
	public static final String USER_GROUP = "User Group";
	// User group id
	public static final String USER_GROUP_ID = "User Group Id";
	// User Role
	public static final String USER_ROLE = "User Role";

	// User Role Id
	public static final String USER_ROLE_ID = "User Role Id";

	// Device type
	public static final String DEVICE_TYPE = "Device type";

	// device type id
	public static final String DEVICE_TYPE_ID = "Device type id";
	// device type type
	public static final String DEVICE_TYPE_TYPE = "DeviceType type";
	// user name
	public static final String USER_NAME = "User Name";
	// User id
	public static final String USER_ID = "User Id";

	// Email
	public static final String EMAIL = "Email";

	public static final String DEVICE_XML_FILE_EXTENSION = ".xml";
	// Device file extension
	public static final String DEVICE_FILE_EXTENSION_ZIP = ".zip";
	// XML tag
	public static final String XML_TAG_ROOT = "xml";
	// device
	public static final String XML_TAG_DEVICE = "device";
	public static final String XML_TAG_DEVICE_NAME = "device_name";
	// gatewayname
	public static final String XML_TAG_GATEWAY_NAME = "gateway_name";
	// cameraname
	public static final String XML_TAG_CAMERA_NAME = "camera_name";
	// device ip
	public static final String XML_TAG_DEVICE_IP = "device_ip";
	// gatewayip
	public static final String XML_TAG_GATEWAY_IP = "gateway_ip";
	// cameraip
	public static final String XML_TAG_CAMERA_IP = "camera_ip";
	// mac address
	public static final String XML_TAG_MAC_ADDR = "mac_addr";
	// is thunder enabled
	public static final String XML_TAG_IS_THUNDER_ENABLED = "isThunderEnabled";
	// thunder port
	public static final String XML_TAG_THUNDER_PORT = "thunderPort";
	// xml device type
	public static final String XML_TAG_Device_TYPE = "device_type";
	// xml oem
	public static final String XML_TAG_OEM = "oem";
	// xml soc
	public static final String XML_TAG_SOC = "soc";
	// xml category
	public static final String XML_TAG_CATEGORY = "category";

	// Db file name
	public static final String DB_FILE_NAME = "classpath:data.sql";
	// script tag name
	public static final String SCRIPT_TAG_NAME = "Script tag name";

	// script tag is
	public static final String SCRIPT_TAG_ID = "Script tag id";

	// build version
	public static final String RDK_VERSION_NAME = "Build version";

	// build version id
	public static final String RDK_VERSION_ID = "Build version id";

	// Module name
	public static final String MODULE_NAME = "Module name";

	// function name
	public static final String FUNCTION_NAME = "Function name";

	// ParameterType name
	public static final String PARAMETER_NAME = "Parameter name";

	// Primitive test name
	public static final String PRIMITIVE_TEST_NAME = "Primitive test name";

	// Primitive test id
	public static final String PRIMITIVE_TEST_ID = "Primitive test id";

	// Primitive test with module name
	public static final String PRIMITIVE_TEST_WITH_MODULE_NAME = "Primitive test with module name";

	// xml extension
	public static final String XML_EXTENSION = ".xml";

	// xml module tag
	public static final String XML_MODULE_TAG = "module";

	// xml module name
	public static final String XML_MODULE_NAME = "moduleName";

	// xml module id
	public static final String XML_MODULE_ID = "moduleId";

	// Identifier
	public static final String ID = "id";

	// xml primitive test id
	public static final String XML_PRIMITIVE_TEST_ID = "primitiveTestId";

	// xml primitive test name
	public static final String XML_PRIMITIVE_TEST_NAME = "primitiveTestName";

	// xml primitivetestparameter
	public static final String XML_PRIMITIVE_TEST_PARAMETER = "primitiveTestParameter";

	// xml value
	public static final String XML_PARAMETER_VALUE = "value";

	// xml value primitive tests
	public static final String XML_PRIMITIVE_TESTS = "primitivetests";

	// xml value primitive test
	public static final String XML_PRIMITIVE_TEST = "primitivetest";

	// xml primitive test parameters
	public static final String XML_PRIMITIVE_TEST_PARAMETERS = "primitivetestparameters";

	// xml module element
	public static final String XML_MODULE_ELEMENT = "moduleName";

	// xml execution time out
	public static final String XML_TAG_EXECUTION_TIME_OUT = "executionTimeOut";

	// xml test group
	public static final String XML_TAG_TEST_GROUP = "testGroup";

	// xml log file names
	public static final String XML_TAG_LOG_FILE_NAMES = "logFileNames";

	// xml crash file names
	public static final String XML_TAG_CRASH_FILE_NAMES = "crashFileNames";

	// xml functions
	public static final String XML_FUNCTIONS = "functions";

	// xml function
	public static final String XML_FUNCTION = "function";

	// name
	public static final String NAME = "name";

	// xml parameter name
	public static final String XML_PARAMETER_NAME = "parameterName";

	// xml parameter
	public static final String XML_PARAMETER = "parameter";

	// xml function name of of parameter
	public static final String XML_PARAMETER_FUN_NAME = "funName";

	// xml parameter type
	public static final String XML_PARAMETER_TYPE = "parameterType";

	// xml range value
	public static final String XML_PARAMETER_RANGE = "range";

	// xml
	public static final String XML = "xml";
	// xml parameters
	public static final String XML_PARAMETERS = "parameters";

	// comma separator
	public static final String COMMA_SEPARATOR = ",";

	// yes value
	public static final String YES = "yes";

	// Script name
	public static final String SCRIPT_NAME = "Script name";

	// Script location for RDKV
	public static final String RDKV_FOLDER_NAME = "testscriptsRDKV";

	// Script location for RDKB
	public static final String RDKB_FOLDER_NAME = "testscriptsRDKB";

	// Script location for RDKC
	public static final String RDKC_FOLDER_NAME = "testscriptsRDKC";

	// Python file extension
	public static final String PYTHON_FILE_EXTENSION = ".py";

	// Script ID
	public static final String SCRIPT_ID = "Script id";

	// Script
	public static final String SCRIPT = "Script";

	// Module name
	public static final String MODULE = "Module";

	// Excel file extension
	public static final String EXCEL_FILE_EXTENSION = ".xlsx";

	// Script group name
	public static final String TEST_SUITE = "Test suite";

	// Script group id
	public static final String TEST_SUITE_ID = "Test suite id";

	// No value
	public static final String NO = "no";

	// XML file extension
	public static final String XML_FILE_EXTENSION = ".xml";

	// Python content type
	public static final String PYTHON_CONTENT = "text/x-python";

	// User directory
	public static final String USER_DIRECTORY = "user.dir";

	// Base filestore folder
	public static final String BASE_FILESTORE_FOLDER = "src/main/webapp/filestore";

	// Pending status for a user
	public static final String USER_PENDING = "PENDING";

	// Active status for a user
	public static final String USER_ACTIVE = "ACTIVE";

	// Zip file extension
	public static final String ZIP_EXTENSION = ".zip";

	// config file extension
	public static final String CONFIG_FILE = ".config";
	// config file path
	public static final String RDK_CERTIFICATION_CONFIG_PATH = "rdkCertificationConfigs";
	// Header finder to add HEader template
	public static final String HEADER_FINDER = "If not stated otherwise in this file or this component's Licenses.txt";
	// Header template
	public static final String HEADER_TEMPLATE = "##########################################################################\r\n"
			+ "# If not stated otherwise in this file or this component's Licenses.txt\r\n"
			+ "# file the following copyright and licenses apply:\r\n" + "#\r\n"
			+ "# Copyright CURRENT_YEAR RDK Management\r\n" + "#\r\n"
			+ "# Licensed under the Apache License, Version 2.0 (the \"License\");\r\n"
			+ "# you may not use this file except in compliance with the License.\r\n"
			+ "# You may obtain a copy of the License at\r\n" + "#\r\n"
			+ "# http://www.apache.org/licenses/LICENSE-2.0\r\n" + "#\r\n"
			+ "# Unless required by applicable law or agreed to in writing, software\r\n"
			+ "# distributed under the License is distributed on an \"AS IS\" BASIS,\r\n"
			+ "# WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.\r\n"
			+ "# See the License for the specific language governing permissions and\r\n"
			+ "# limitations under the License.\r\n"
			+ "##########################################################################\r\n" + "";

	// User theme constant
	public static final String USER_THEME = "User Theme";

	// TDK utility file location
	public static final String TDK_UTIL_FILE_LOCATION = "/tdkUtilFiles/HeaderFile.txt";

	// New line character
	public static final String NEW_LINE = "\n";

	// Python default command
	public static final String PYTHON = "python";

	// Busy status
	public static final String BUSY = "BUSY";

	// Free status
	public static final String FREE = "FREE";

	// Not found status
	public static final String NOT_FOUND = "NOT_FOUND";

	// HANG status
	public static final String HANG = "HANG";

	// TDK_DISABLED status
	public static final String TDK_DISABLED = "TDK_DISABLED";

	// IPV6_INTERFACE
	public static final String IPV6 = "IPV6";

	// IPV4_INTERFACE
	public static final String IPV4 = "IPV4";

	// PERCENTAGE
	public static final String PERCENTAGE = "%";

	// Colon character
	public static final String COLON = ":";

	// Key for last day
	public static final String KEY_LASTDAY = "L";
	// Key for new line
	public static final String KEY_ENTERNEW_LINE = "\r\n";
	// Greater than symbol
	public static final String GREATERTHAN = ">";
	// Less than symbol
	public static final String LESSTHAN = "<";
	// HTML greater than symbol
	public static final String HTML_GREATERTHAN = "&gt;";
	// HTML less than symbol
	public static final String HTML_LESSTHAN = "&lt;";
	// HTML replace BR tag pattern
	public static final String HTML_REPLACEBR = "</?br\\b[^>]*>";
	// HTML span pattern
	public static final String HTML_PATTERN = "<span(.*?)</span>";
	// HTML pattern after span
	public static final String HTML_PATTERN_AFTERSPAN = "</?span\\b[^>]*>";
	// CURLY_BRACKET_OPEN
	public static final String CURLY_BRACKET_OPEN = "{";

	// Status none
	public static final String STATUS_NONE = "none";

	// Configure test case replace token
	public static final String CONFIGURE_TESTCASE_REPLACE_TOKEN = "configureTestCase(ip,port,";
	// Replace by token
	public static final String REPLACE_BY_TOKEN = ",ip,port,";
	// Left parenthesis
	public static final String LEFT_PARANTHESIS = "(";
	// Right parenthesis
	public static final String RIGHT_PARANTHESIS = ")";
	// Curly bracket close
	public static final String CURLY_BRACKET_CLOSE = "}";
	// Square bracket open
	public static final String SQUARE_BRACKET_OPEN = "[";
	// Square bracket close
	public static final String SQUARE_BRACKET_CLOSE = "]";

	// Comma separator
	public static final String COMMA_SEPERATOR = ",";
	// Single quotes
	public static final String SINGLE_QUOTES = "'";
	// Underscore
	public static final String UNDERSCORE = "_";

	// Method token
	public static final String METHOD_TOKEN = "configureTestCase";

	// Logs
	public static final String LOGS = "logs";

	// Temp script file keyword
	public static final String TEMP_SCRIPT_FILE_KEYWORD = "tempScript";

	// Port replace token
	public static final String PORT_REPLACE_TOKEN = "<port>";

	// IP replace token
	public static final String IP_REPLACE_TOKEN = "<ipaddress>";

	// Execution keyword
	public static final String EXECUTION_KEYWORD = "execution";

	// Result
	public static final String RESULT = "result";

	// Execution logs
	public static final String EXECUTION_LOGS = "executionLogs";

	// Rerun appender
	public static final String RERUN_APPENDER = "_RERUN";

	// Script end Python code
	public static final String SCRIPT_END_PY_CODE = "\nprint(\"SCRIPTEND#!@~\");";

	// Error tag Python comment
	public static final String ERROR_TAG_PY_COMMENT = "#TDK_@error";

	// End tag Python comment
	public static final String END_TAG_PY_COMMENT = "SCRIPTEND#!@~";

	// IPv6 interface
	public static final String IPV6_INTERFACE = "ipv6.interface";
	// IPv4 interface
	public static final String IPV4_INTERFACE = "ipv4.interface";
	// Python command
	public static final String PYTHON_COMMAND = "python_command";
	// Console file upload script
	public static final String CONSOLE_FILE_UPLOAD_SCRIPT = "callConsoleLogUpload.py";

	// Log upload IPv4
	public static final String LOG_UPLOAD_IPV4 = "log.upload.ipv4";
	// Log upload IPv6
	public static final String LOG_UPLOAD_IPV6 = "log.upload.ipv6";
	// REST mechanism
	public static final String REST_MECHANISM = "REST";
	// TM URL
	public static final String TM_URL = "tmURL";
	// File transfer script RDK service
	public static final String FILE_TRANSFER_SCRIPT_RDKSERVICE = "transfer_thunderdevice_logs.py"; // Update path
	// Slash version text file
	public static final String SLASH_VERSION_TXT_FILE = "/version.txt"; // Update as necessary
	// Root string
	public static final String ROOT_STRING = "root"; // Example path, change as necessary
	// None string
	public static final String NONE_STRING = "none";
	// TM config file
	public static final String TM_CONFIG_FILE = "tm.config";
	// AgentConsole
	public static final String AGENT_CONSOLE_LOG_FILE = "AgentConsole.log";
	// logs.path
	public static final String LOGS_PATH = "logs.path";
	// HTML line break
	public static final String HTML_BR = "<br/>";

	// Multi test suite constant
	public static final String MULTI_TEST_SUITE = "MultiTestSuite";
	// Device logs constant
	public static final String DEVICE_LOGS = "deviceLogs";
	// Agent logs constant
	public static final String AGENT_LOGS = "agentLogs";
	// Device info logs constant
	public static final String DEVICE_INFO_LOGS = "deviceinfo";

	// Thunder default port constant
	public static final String THUNDER_DEFAULT_PORT = "9998";
	// File upload script constant
	public static final String FILE_UPLOAD_SCRIPT = "fileupload.py";
	// Python3 command constant
	public static final String PYTHON3 = "python3";

	// Build name failed message constant
	public static final String BUILD_NAME_FAILED = "Build name fetching failed";
	// Default IPv4 interface constant
	public static final String DEFAULT_IPV4_INTERFACE = "eth0";
	// Default IPv6 interface constant
	public static final String DEFAULT_IPV6_INTERFACE = "sit1";

	// The total keyword to be used in DTO
	public static final String TOTAL_KEYWORD = "Total";
	// Directory for Thunder device configuration
	public static final String THUNDER_DEVICE_CONFIG_DIR = "tdkvRDKServiceConfig";

	// Application version
	public static final String APP_VERSION = "app.version";

	// The device log type
	public static final String DEVICELOGTYPE = "deviceLog";

	// The agent log type
	public static final String AGENTLOGTYPE = "agentLog";

	// The crash log type
	public static final String CRASHLOGTYPE = "crashLog";
	// Multiple keyword constant
	public static final String MULTIPLE_KEY_WORD = "Multiple";

	// Job constant
	public static final String JOB = "Job_";
	// crash logs
	public static final String CRASH_LOGS = "crashLogs";

	// Test variable file
	public static final String RDK_CERTIFICATION_FILE_LIST = "rdkCertificationFileNames.config";
	// Bug issue type
	public static final String BUG_ISSUE_TYPE = "Bug";
	// Issue analyser configuration file rdkv
	public static final String ISSUE_ANALYSER_CONFIG_RDKV = "issueAnalysisRDKV.config";
	// Issue analyser configuration file rdkb
	public static final String ISSUE_ANALYSER_CONFIG_RDKB = "issueAnalysisRDKB.config";
	// Ticket handler URL
	public static final String TICKET_HANDLER_URL = "ticketHandlerUrl";
	// Project IDs
	public static final String PROJECT_IDS = "projectIDS";
	// Platform project IDs
	public static final String PLATFORM_PROJECT_IDS = "platformProjectIDs";
	// Labels
	public static final String LABELS = "labels";
	// Release versions
	public static final String RELEASE_VERSIONS = "releaseVersions";
	// Hardware configurations
	public static final String HARDWARE_CONFIGURATIONS = "hardwareConfigurations";
	// Impacted platforms
	public static final String IMPACTED_PLATFORMS = "impactedPlatforms";
	// Severities
	public static final String SEVERITIES = "severities";
	// Fixed in versions
	public static final String FIXED_IN_VERSIONS = "fixedInVersions";
	// Components impacted
	public static final String COMPONENTS_IMPACTED = "componentsImpacted";
	// Priorities
	public static final String PRIORITIES = "priorities";
	// Create API endpoint
	public static final String CREATE_API_ENDPOINT = "create";
	// Search summary API endpoint
	public static final String SEARCH_SUMMARY_API_ENDPOINT = "searchSummary";
	// Attachment API endpoint
	public static final String ATTACHMENT_API_ENDPOINT = "attachFile";
	// Update API endpoint
	public static final String UPDATE_API_ENDPOINT = "updateTicket";
	// Jira automation
	public static final String JIRA_AUTOMATION = "jira.integrated";
	// Tdk project id
	public static final String TDK_PROJECT_NAME = "TDK";
	// Left Paranthesis
	public static final String LEFT_PARENTHESIS = "(";
	// Right Paranthesis
	public static final String RIGHT_PARENTHESIS = ")";

	// Log file extension
	public static final String LOG_FILE_EXTENSION = ".log";
	// TDK portal service name
	public static final String TDK_PORTAL_SERVICE = "TDK";

	// CI callback URL
	public static final String CI_CALLBACK_URL = "ci_callback_url";

	// Rdkcertification diagnostics script file
	public static final String RDK_DIAGNOSIS_LOG_SCRIPT = "rdk_cerfiticate_diagnosis.py";

	// Script for rest agent
	public static final String REST_AGENT_SCRIPT = "callResetAgent.py";

	// Key word FALSE
	public static final String FALSE = "FALSE";

	// Key word TRUE
	public static final String TRUE = "TRUE";

	// Script for reboot device
	public static final String REBOOT_ON_CRASH_SCRIPT_FILE = "callRebootOnCrash.py";

	// Based log file path in the config file
	public static final String LOGS_PATH_KEY_CONFIG_FILE = "logs_path";

	// CI image boxtype config file
	public static final String CI_IMAGE_BOXTYPE_CONFIG_FILE = "ci_image_boxtype.config";

	// TM URL
	public static final String TM_URL_EXEC_KEY_CONFIG_FILE = "tmURL_exec";

	// VTS package
	public static final String VTS = "VTS";
	// TDK package
	public static final String TDK = "TDK";
	// Rdk version
	public static final String RDK_VERSIONS = "rdkVersions";
	// Upgrade file location key in tm.config
	public static final String UPGRADE_FILE_LOCATION = "upgrade.fileLocation";
	// Default backup location if not specified
	public static final String DEFAULT_BACKUP_LOCATION = "/mnt/backup";
	// Webapps directory name
	public static final String WEBAPPS_DIR = "webapps";
	// TDK service war name
	public static final String TDKSERVICE_WAR = "tdkservice.war";
	// TDK service directory name
	public static final String TDKSERVICE_DIR = "tdkservice";
	// Backup prefix for backup directory
	public static final String BACKUP_PREFIX = "backup_";
	// deployment log path
	public static final String DEPLOYMENT_LOG_PATH = "/mnt/backup/deploymentLogs/";
	// app upgrage shell script file
	public static final String APP_UPGRADE_WAR_FILE = "app_upgrade_deploy_new_war.sh";

}
