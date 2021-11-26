# stdlib imports
import collections
import ConfigParser
import glob
import importlib
import json
import logging
import urllib2
import xml.etree.ElementTree as ET

# third-party imports
import jsonpickle
import redis

# our code imports
import constants
import sensorparsers
import summerlogger



def update_streets_with_sensor_data(city_config_file):
    """
    This method opens a connection to Redis, and for each sensor available
    updates street information with the sensor data that is read. Existing data
    about that street is overwritten.

    """
    logger = \
    logging.getLogger('summer.process_sensor.update_streets_with_sensor_data')
    try:
        config = ConfigParser.SafeConfigParser()
        config.read(city_config_file)

        redis_url = config.get('ConnectionSettings', 'REDIS_URL')
        logger.debug("Connecting to Redis at: %s"%(redis_url,))
        redis_server = redis.Redis(redis_url)
        if not redis_server:
            logger.warn("Could not connect to Redis. Is it running?")
            raise IOError
    except IOError as ioe:
        logger.critical("Cannot connect to datastore Redis: %s"%(ioe))
    else:
        logger.info("Connected to Redis")
        # Get currently held data from redis
        city_prefix = config.get('ConnectionSettings', 'CITY_PREFIX')
        city_way_set = ''.join([city_prefix, '_set'])
        city_way_data = redis_server.smembers(city_way_set)
        logger.debug("Number of items inside set %s is: %d"%(city_way_set, len(city_way_data)))

        all_sensors = config.options('SensorsAvailable')
        logger.info("Number of sensors available: %d"%(len(all_sensors)))
        
        for sensor in all_sensors:
            sensor_name = config.get('SensorsAvailable', sensor)
            logger.info("Starting with sensor %s"%(sensor_name))
            configvals = config.items(sensor_name)
            sensor_config = collections.defaultdict(str)
            for val in configvals:
                sensor_config[val[0]] = val[1]
            logger.debug(sensor_config)
            sensor_type = sensor_config['type']
            hash_prefix = ''.join([city_prefix, '_', sensor_type])
            logger.info("Type of sensor: %s"%(sensor_type))
            websvc_module, websvc_func = sensor_config['webservice'].split('.')
            websvclib = importlib.import_module(websvc_module)
            websvcfunc = getattr(websvclib, websvc_func)
            if websvcfunc(sensor_config):
                logger.info("Successfully got data from webservice")
                logger.info("Moving on to parsing data...")
            else:
                logger.warn("Could not get data from webservice")
                logger.warn("Giving up on this sensor's data")
                return False
            parser_module, parser_func = sensor_config['parser'].split('.')
            parserlib = importlib.import_module(parser_module)
            sensor_parser = getattr(parserlib, parser_func)
            sensor_propagation = int(sensor_config['propagation'])
            sensor_file_pattern = ''.join([sensor_config['dirname'], \
                                        sensor_config['filepattern']])
            logger.info("Globbing all files with pattern: \
                            %s"%(sensor_file_pattern))
            sensor_files = glob.glob(sensor_file_pattern)
            for sensor_file in sensor_files:
                    sensor_data = sensor_parser(hash_prefix, sensor_name, sensor_file, sensor_propagation)
                    logger.debug(sensor_data)
                    # Get the aggregator module and func for this sensor
                    agg_module, agg_func = sensor_config['aggregator'].split('.')
                    agg_lib= importlib.import_module(agg_module)
                    agg_gator = getattr(agg_lib, agg_func)
                    # Iterate through all the streets we found this time
                    for street, street_data in sensor_data.items():
                        logger.debug("Adding data for street: %s"%(street))
                        city_way_data.add(street)
                        redis_server.sadd(city_way_set, street)
                        # Get existing data for each street for this sensor
                        historical_street_data = redis_server.hgetall(street)
                        logger.debug("Historical data: %s"%(historical_street_data))
                        # Send current and historical data to aggregator for statistical
                        # munging
                        street_data = agg_gator(sensor_name, street_data, \
                                historical_street_data)
                        # Write the hash data back to Redis
                        for key,value in street_data.items():
                            redis_server.hset(street, key, value)
                            logger.debug("Inserted hash: %s with %s=>%s"%(street, key, value))
        
            logger.info("Finished with sensor: %s"%(sensor_name))
        return True

       
if __name__ == '__main__':
    import os
    update_streets_with_sensor_data(os.path.abspath('../sensors-config-files/dublin.config'))
