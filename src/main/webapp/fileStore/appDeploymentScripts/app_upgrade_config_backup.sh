#!/bin/bash
##########################################################################
# If not stated otherwise in this file or this component's Licenses.txt
# file the following copyright and licenses apply:
#
# Copyright 2025 RDK Management
#
# Licensed under the Apache License, Version 2.0 (the "License");
# you may not use this file except in compliance with the License.
# You may obtain a copy of the License at
#
# http://www.apache.org/licenses/LICENSE-2.0
#
# Unless required by applicable law or agreed to in writing, software
# distributed under the License is distributed on an "AS IS" BASIS,
# WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
# See the License for the specific language governing permissions and
# limitations under the License.
#########################################################################
# Default backup directory

# Default backup directory
default_backup_dir="/mnt/TM_BACKUP"

# Use the provided argument if given, otherwise use the default
backup_dir="${1:-$default_backup_dir}"

log_dir="${2:-$backup_dir/deployment_logs}"

# Create the log directory if it doesn't exist
mkdir -p "$log_dir"

# Log file path
log_file="/log_dir/deployment_log_$(date +%Y%m%d_%H%M%S).log"
echo "Starting script execution at $(date)" | tee -a "$log_file"

# ...existing code...
source_file_video="/opt/tomcat/webapps/tdkservice/fileStore/tdkvRDKServiceConfig/Video_Accelerator.config"
backup_dir_video="$backup_dir/tdkvRDKServiceConfig"
destination_dir_video="/opt/tomcat/webapps/tdkservice/fileStore/tdkvRDKServiceConfig/"
source_file_device="/opt/tomcat/webapps/tdkservice/fileStore/tdkvDeviceConfig/sampleThunderDisabled.config"
backup_dir_device="$backup_dir/tdkvDeviceConfig"
destination_dir_device="/opt/tomcat/webapps/tdkservice/fileStore/tdkvDeviceConfig/"
source_file_device_capabilities="/opt/tomcat/webapps/tdkservice/fileStore/tdkvDeviceCapabilities/Video_Accelerator_deviceCapability.ini"
backup_dir_device_capabilities="$backup_dir/tdkvDeviceCapabilities"
destination_dir_device_capabilities="/opt/tomcat/webapps/tdkservice/fileStore/tdkvDeviceCapabilities/"
backup_file_browser_validation="$backup_dir/BrowserPerformanceVariables.py"
source_file_browser_validation="/opt/tomcat/webapps/tdkservice/fileStore/BrowserPerformanceVariables.py"
destination_dir_browser_validation="/opt/tomcat/webapps/tdkservice/fileStore/"
backup_file_media_validation="$backup_dir/MediaValidationVariables.py"
source_file_media_validation="/opt/tomcat/webapps/tdkservice/fileStore/MediaValidationVariables.py"
destination_dir_media_validation="/opt/tomcat/webapps/tdkservice/fileStore/"
backup_file_performance_validation="$backup_dir/PerformanceTestVariables.py"
source_file_performance_validation="/opt/tomcat/webapps/tdkservice/fileStore/PerformanceTestVariables.py"
destination_dir_performance_validation="/opt/tomcat/webapps/tdkservice/fileStore/"
backup_file_stability_validation="$backup_dir/StabilityTestVariables.py"
source_file_stability_validation="/opt/tomcat/webapps/tdkservice/fileStore/StabilityTestVariables.py"
destination_dir_stability_validation="/opt/tomcat/webapps/tdkservice/fileStore/"
backup_file_ipchange_validation="$backup_dir/IPChangeDetectionVariables.py"
source_file_ipchange_validation="/opt/tomcat/webapps/tdkservice/fileStore/IPChangeDetectionVariables.py"
destination_dir_ipchange_validation=

process_and_compare_backup_files() {
    local backup_file=$1
    local source_file=$2
    local destination_dir=$3
    echo "Processing backup file: $backup_file" | tee -a "$log_file"
    echo "Using source file: $source_file" | tee -a "$log_file"
    echo "Destination directory: $destination_dir" | tee -a "$log_file"
    if [ ! -f "$backup_file" ]; then
        echo "Warning: Backup file not found: $backup_file" | tee -a "$log_file"
        return
    fi
    declare -A backup_values
    declare -A section_map
    current_section=""
    # Read the backup file and store values in a map
    while IFS= read -r line; do
        [[ -z "$line" ]] && continue  # Skip empty lines
        if [[ "$line" =~ ^\[.*\]$ ]]; then
            current_section="$line"  # Store section header
            section_map["$current_section"]="$current_section"
        elif [[ $line != \#* ]]; then
            key=$(echo "$line" | cut -d '=' -f 1 | xargs)
            value=$(echo "$line" | cut -d '=' -f 2- | xargs)
            if [[ -n "$key" ]]; then
                backup_values["$current_section|$key"]="$value"
            fi
        fi
    done < "$backup_file"
    local tmp_file=$(mktemp)
    current_section=""
    # Process source file and update backup file
    while IFS= read -r line; do
        if [[ -z "$line" ]]; then
            echo "" >> "$tmp_file"
            continue
        fi
        if [[ "$line" =~ ^\[.*\]$ ]]; then
            current_section="$line"  # Capture current section
            echo "$current_section" >> "$tmp_file"
        elif [[ $line != \#* ]]; then
            key=$(echo "$line" | cut -d '=' -f 1 | xargs)
            value=$(echo "$line" | cut -d '=' -f 2- | xargs)
            if [[ -n "${backup_values[$current_section|$key]}" ]]; then
                value="${backup_values[$current_section|$key]}"
                echo "$key = $value" >> "$tmp_file"
            else
                echo "$key = $value" >> "$tmp_file"
            fi
        else
            echo "$line" >> "$tmp_file"
        fi
    done < "$source_file"
    mv "$tmp_file" "$backup_file"
    cp "$backup_file" "$destination_dir"
    echo "Updated backup file: $backup_file" | tee -a "$log_file"
    echo "Copied to destination: $destination_dir" | tee -a "$log_file"
}


process_and_compare_backup_python_files() {
    local backup_file=$1
    local source_file=$2
    local destination_dir=$3

    echo "Processing backup file: $backup_file" | tee -a "$log_file"
    echo "Using source file: $source_file" | tee -a "$log_file"
    echo "Destination directory: $destination_dir" | tee -a "$log_file"

    if [ ! -f "$backup_file" ]; then
        echo "Warning: Backup file not found: $backup_file" | tee -a "$log_file"
        return
    fi

    # Read source file into a map
    declare -A source_values
    while IFS= read -r line || [[ -n $line ]]; do
        if [[ -n "$line" && $line != \#* && "$line" == *"="* ]]; then
            key=$(echo "$line" | cut -d '=' -f 1 | xargs)
            value=$(echo "$line" | cut -d '=' -f 2- | xargs)
            if [[ -n "$key" && -n "$value" ]]; then
                source_values["$key"]="$value"
            fi
        fi
    done < "$source_file"

    # Prepare the temporary file for the updated content
    local tmp_file=$(mktemp)

    # Process the backup file line by line and preserve its structure
    declare -A processed_keys
    while IFS= read -r line || [[ -n $line ]]; do
        if [[ -z "$line" ]]; then
            echo "" >> "$tmp_file"  # Preserve blank lines
        elif [[ $line == \#* ]]; then
            echo "$line" >> "$tmp_file"  # Preserve comments
        else
            key=$(echo "$line" | cut -d '=' -f 1 | xargs)
            if [[ -n "$key" && -n "${source_values[$key]}" && -z "${processed_keys[$key]}" ]]; then
                value="${source_values[$key]}"
                # Handle quoting and numeric checks
                if [[ $value =~ ^[0-9]+$ ]]; then
                    # Do not add quotes for numeric values
                    value="$value"
                elif [[ $value == *"+"* ]]; then
                    value=$(echo "$value" | sed -E 's/([^ ]+)\s*\+\s*([^"]+)/\1 + "\2"/')
                elif [[ $value != \"*\" ]]; then
                    value="\"$value\""
                fi
                echo "$key = $value" >> "$tmp_file"
                unset source_values["$key"]
                processed_keys["$key"]=1
            else
                echo "$line" >> "$tmp_file"
            fi
        fi
    done < "$backup_file"

    # Add remaining keys from the source file
    for key in "${!source_values[@]}"; do
        value="${source_values[$key]}"
        if [[ $value =~ ^[0-9]+$ ]]; then
            value="$value"
        elif [[ $value == *"+"* ]]; then
            value=$(echo "$value" | sed -E 's/([^ ]+)\s*\+\s*([^"]+)/\1 + "\2"/')
        elif [[ $value != \"*\" ]]; then
            value="\"$value\""
        fi
        echo "$key = $value" >> "$tmp_file"
    done

    # Replace the backup file with the updated content
    mv "$tmp_file" "$backup_file"

    # Copy the updated file to the destination directory
    cp "$backup_file" "$destination_dir"

    echo "Updated backup file: $backup_file" | tee -a "$log_file"
    echo "Copied to destination: $destination_dir" | tee -a "$log_file"
}

process_all_pythons() {
    local backup_file=$1
    local source_file=$2
    local destination_dir=$3

    process_and_compare_backup_python_files "$backup_file" "$source_file" "$destination_dir"
}

# Excluded files
exclude_files=("sample_CI_Exec.config" "VA_SampleHP.config" "VA_SampleLP.config" "sample_deviceCapability.ini" "sampleThunderEnabled.config")
process_all_configs() {
    local backup_dir=$1
    local source_file=$2
    local destination_dir=$3
    local suffix=$4

    # Iterate over backup files in the directory
    for backup_file in "$backup_dir"/*"$suffix"; do
        # Extract the base name of the backup file
        base_name=$(basename "$backup_file")

        # Check if the file is in the exclude list
        if [[ " ${exclude_files[@]} " =~ " $base_name " ]]; then
            echo "Skipping excluded file: $base_name" | tee -a "$log_file"
            continue
        fi

        process_and_compare_backup_files "$backup_file" "$source_file" "$destination_dir"
    done
}


# Run config file processing in parallel
process_all_configs "$backup_dir_video" "$source_file_video" "$destination_dir_video" ".config" &
process_all_configs "$backup_dir_device" "$source_file_device" "$destination_dir_device" ".config" &
process_all_configs "$backup_dir_device_capabilities" "$source_file_device_capabilities" "$destination_dir_device_capabilities" "_deviceCapability.ini" &

# Run Python file processing in parallel
process_all_pythons "$backup_file_browser_validation" "$source_file_browser_validation" "$destination_dir_browser_validation" &
process_all_pythons "$backup_file_media_validation" "$source_file_media_validation" "$destination_dir_media_validation" &
process_all_pythons "$backup_file_performance_validation" "$source_file_performance_validation" "$destination_dir_performance_validation" &
process_all_pythons "$backup_file_stability_validation" "$source_file_stability_validation" "$destination_dir_stability_validation" &
process_all_pythons "$backup_file_ipchange_validation" "$source_file_ipchange_validation" "$destination_dir_ipchange_validation" &

# Wait for all background processes to complete
wait

echo "All tasks completed."