"""
This daemon, once started, will periodically check the city-based config files, 
and process the sensor-data corresponding to each config file. Since, we 
currently depend on external servers for our reverse-geocoding information, we 
try to be polite and only run 2-3 times a day.
"""
# stdlib
import glob
import logging
import os
import time

# third-party libs

# our libs
import process_sensor
import summerlogger

# Typically, we would need to modify only the 'HOURS_TO_SLEEP' variable, but 
# the daemon actually uses seconds, so further granularity is available if 
# needed
HOURS_TO_SLEEP = 6
MINUTES_TO_SLEEP = 60 * HOURS_TO_SLEEP
SECONDS_TO_SLEEP = 60 * MINUTES_TO_SLEEP 

logger = logging.getLogger('summer.process_sensor_data_daemon')
while True:
    try:
        # os.getenv returns a default value, whereas os.environ.get does not
        config_file_pattern = os.getenv('CONFIG_PATTERN', '../sensors-config-files/dublin.config')
        logger.info("Config file pattern to be globbed: \
                                                %s"%(config_file_pattern))
        config_files = glob.glob(config_file_pattern)
        for config_file in config_files:
                print("Processing config file for city: %s"%(config_file))
                logger.info("Processing config file for city: %s"%(config_file))
                process_sensor.update_streets_with_sensor_data(os.path.abspath(config_file))
                logger.info("Done processing config file for city: %s"%(config_file))
    except Exception as e:
        logger.warn("Did not call sensor processing daemon: %s"%(e))
        logger.info("Will try again in %s minutes"%(MINUTES_TO_SLEEP))

    time.sleep(SECONDS_TO_SLEEP)





