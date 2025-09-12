#!/bin/bash
 
# Usage: ./app_upgrade_deploy_new_war.sh {backupPath} {newWarPath}
 
backupPath="$1"
newWarPath="$2"
webapps_dir="/opt/tomcat/webapps"
war_name="tdkservice.war"
app_dir_name="tdkservice"
supervisor_service_name="tomcat"  # Change if your supervisor program name is different
 
log_dir="/mnt/backup/deploymentLogs"
mkdir -p "$log_dir"
log_file="$log_dir/deployment_log_$(date +%Y%m%d_%H%M%S).log"
echo "Starting script execution at $(date)" | tee -a "$log_file"
 
if [ -z "$backupPath" ] || [ -z "$newWarPath" ]; then
    echo "Usage: $0 {backupPath} {newWarPath}" | tee -a "$log_file"
    exit 1
fi
 
echo "Stopping Tomcat using Supervisor..." | tee -a "$log_file"
supervisorctl stop $supervisor_service_name | tee -a "$log_file"
 
 
# Check if backup path exists and is writable
if [ ! -d "$backupPath" ]; then
    echo "ERROR: Backup directory $backupPath does not exist!" | tee -a "$log_file"
    mkdir -p "$backupPath"
    echo "Created backup directory $backupPath" | tee -a "$log_file"
fi 

echo "Backing up current WAR and exploded directory..." | tee -a "$log_file"
cp "$webapps_dir/$war_name" "$backupPath/" | tee -a "$log_file"
cp -r "$webapps_dir/$app_dir_name" "$backupPath/" | tee -a "$log_file"
 
sleep 10
 
echo "Removing old WAR and exploded directory..." | tee -a "$log_file"
rm -rf "$webapps_dir/$war_name" "$webapps_dir/$app_dir_name" | tee -a "$log_file"
 
echo "Copying new WAR from $newWarPath to $webapps_dir..." | tee -a "$log_file"
cp "$newWarPath/$war_name" "$webapps_dir/" | tee -a "$log_file"
 
sleep 5
 
echo "Starting Tomcat using Supervisor..." | tee -a "$log_file"
supervisorctl start $supervisor_service_name | tee -a "$log_file"
 
max_attempts=12
attempt=1
while [ $attempt -le $max_attempts ]; do
    sleep 10
    health_status=$(curl -s http://52.206.149.108:8443/tdkservice/actuator/health | grep '"status":"UP"')
    if [ -n "$health_status" ]; then
        echo "Application is UP after $((attempt*10)) seconds." | tee -a "$log_file"
        break
    else
        echo "Waiting for application to become healthy... ($((attempt*10))s)" | tee -a "$log_file"
    fi
    attempt=$((attempt+1))
done
 
if [ -z "$health_status" ]; then
    echo "Application failed to start after $((max_attempts*10)) seconds." | tee -a "$log_file"
fi
 
echo "Calling app_upgrade_config_backup.sh with backupPath: $backupPath" | tee -a "$log_file"
./app_upgrade_config_backup.sh "$backupPath" "$log_file"
 
echo "Upgrade and deployment script completed." | tee -a "$log_file"
