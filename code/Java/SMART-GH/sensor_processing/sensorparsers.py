"""
This module contains the parsers for sensor data. When a new sensor is added, a 
parser must be added to read through the data dumped by the web-service, and 
create a hash, that can be returned.
"""
# std libs
import collections
import ConfigParser
import jsonpickle
import logging
import os
import time
import urllib2
import xml.etree.ElementTree as ET

# third-party libs

# our libs
import constants
import summerlogger
import utils

def create_noise_sensor_hash(hash_prefix, sensor_name, sensor_file, sensor_propagation):
    """
    This function reads an XML file created by NoiseTube, finds the appropriate
    streets that the readings pertain to, and creates a hash that can be
    inserted inside Redis. It ignores measurements that have no location 
    associated with them. If the same lat/long combination have multiple 
    measurements, the last one is taken

    :returns: Hash containing Way-id and sensor value and timestamp
    """
    logger = logging.getLogger('summer.sensorparsers.create_noise_sensor_hash')
    logger.info("Parsing sensor data for: %s"%(sensor_name,))
    sensor_file = os.path.abspath(sensor_file)
    logger.info("Sensor data being grabbed from:%s"%(sensor_file))
    sensor_hash = collections.defaultdict(dict)
    # check if it's a JSON file or an XML file
    file_ext = os.path.splitext(sensor_file)[1]
    if file_ext == '.xml':
            prev_latitude = 0
            prev_longitude = 0
            try:
                    for event, elem in ET.iterparse(sensor_file):
                        if elem.tag == "measurement":
                            loudness = elem.get('loudness')
                            timestamp = elem.get('timeStamp')
                            geoloc = elem.get('location')
                            if geoloc is None:
                                continue
                            lat,long = geoloc.lstrip("geo:").split(",")
                            if not relevant_noise_measurement(prev_latitude, prev_longitude, \
                                                                lat, long, sensor_propagation):
                                logger.debug("Prev: %s,%s | curr: %s,%s"%(prev_latitude, \
                                        prev_longitude, lat, long))
                                logger.debug("Skipping this measurement")
                                continue
                            else:
                                prev_latitude, prev_longitude = lat, long
                                relevant_streets = utils.get_relevant_streets(lat, long,\
                                        sensor_propagation)
                                # sleep for a little while, just to be polite
                                time.sleep(3)
                                for street in relevant_streets:
                                    street_hash_name = ''.join([hash_prefix,'_',street])
                                    logger.debug("Updating sensor value for %s"%(street_hash_name))
                                    sensor_hash[street_hash_name].update({"value":loudness, "timestamp": timestamp})
            except ET.ParseError as pe:
                logger.warn("Malformed XML. Skipping rest of the file")
    elif file_ext == '.json':
        # decode the json file and essentially do the same as above
        readings_from_file = open(sensor_file, "r").read()
        readings_data = jsonpickle.decode(readings_from_file)
        logger.info("Number of entries in file: %s"%(len(readings_data)))
        prev_latitude = 0
        prev_longitude = 0
        for reading in readings_data:
            loudness = reading['loudness']
            lat = reading['lat']
            long = reading['lng']
            timestamp = reading['made_at']
            if lat is None or long is None:
                continue
            logger.debug("Prev: %s,%s | curr: %s,%s"%(prev_latitude, \
                                        prev_longitude, lat, long))
            if not relevant_noise_measurement(prev_latitude, prev_longitude, \
                    lat, long, sensor_propagation):
                logger.debug("Skipping this measurement")
                continue
            else:
                prev_latitude, prev_longitude = lat, long
                relevant_streets = utils.get_relevant_streets(lat, long,\
                                        sensor_propagation)
                # sleep for a little while, just to be polite
                time.sleep(3)
                for street in relevant_streets:
                    street_hash_name = ''.join([hash_prefix,'_',street])
                    logger.debug("Updating sensor value for %s"%(street_hash_name))
                    sensor_hash[street_hash_name].update({"value":loudness, \
                        "timestamp": timestamp})
    else:
        # We don't understand this file format. Give up
        logger.warn("File format not understood. Cowardly giving up")
    logger.info("Finished processing %s"%(sensor_file))
    logger.info("Number of streets affected by sensor update: %d"%(len(sensor_hash)))
    return sensor_hash

def create_air_sensor_hash(hash_prefix, sensor_name, sensor_file, 
        sensor_propagation):
    """
    This function reads an CSV file downloaded from Dublin City Council, finds 
    the appropriate streets that the readings pertain to, and creates a hash that can be
    inserted inside Redis. Since the air sensor is fixed, we don't need to 
    geo-locate multiple times. We simply geo-locate the sensor locations once, 
    and then update the hash with all the values

    :returns: Hash containing Way-id and sensor value and timestamp
    """
    import csv
    import datetime

    logger = logging.getLogger('summer.sensorparsers.create_noise_sensor_hash')
    logger.info("Parsing sensor data for: %s"%(sensor_name,))
    sensor_file = os.path.abspath(sensor_file)
    logger.info("Sensor data being grabbed from:%s"%(sensor_file))
    sensor_hash = collections.defaultdict(dict)
    sensor_locations = {'Ringsend': '53.3419408,-6.3005284', \
                    'Cabra': '53.3674743,-6.376151',\
                    'Crumlin': '53.3285095,-6.3786803', \
                    'Finglas': '53.3904046,-6.3722169'}

    # check if it's a CSV file
    file_ext = os.path.splitext(sensor_file)[1]
    if file_ext == '.csv':
            csvfile = csv.DictReader(open (sensor_file, 'rb'))
            areawise = collections.defaultdict(list)
            for row in csvfile:
                for key, value in row.iteritems():
                    areawise[key].append(value)
            logger.debug("Finished iterating through csv file")
            for area, readings in areawise.iteritems():
                logger.debug("Number of readings for %s is %d"%(area, \
                    len(readings)))
                logger.debug(readings)
                readings = [int(item) for item in readings if item != 'No Data']
                areawise[area] = readings
                logger.debug("After filtering for 'No Data'...")
                logger.debug("Number of readings for %s is %d"%(area, \
                    len(readings)))
                lat, long = sensor_locations[area].split(',')
                # We don't get readings on a continuing basis. Since all we 
                # have is one dataset, we'll need to call some aggregating 
                # function for the values here, instead of the aggregating 
                # filter
                aggregated_value = exponential_moving_average(readings[0], \
                        readings[1:])
                timestamp = str(datetime.datetime.utcnow()).split('.')[0]
                relevant_streets = utils.get_relevant_streets(lat, long, \
                    sensor_propagation)
                for street in relevant_streets:
                    street_hash_name = ''.join([hash_prefix,'_',street])
                    logger.debug("Updating sensor value for %s"%(street_hash_name))
                    sensor_hash[street_hash_name].update({"value":aggregated_value, \
                        "timestamp": timestamp})
    else:
        # We don't understand this file format. Give up
        logger.warn("File format not understood. Cowardly giving up")
    logger.info("Finished processing %s"%(sensor_file))
    logger.info("Number of streets affected by sensor update: %d"%(len(sensor_hash)))
    return sensor_hash


def relevant_noise_measurement(prev_lat, prev_long, cur_lat, cur_long, \
        sensor_propagation):
    """
    This helper function checks if the new measurement is sufficiently distant 
    from the previous measurement, to warrant a reverse geo-coding. If the new 
    measurement is less than sensor_propagation-distance away from the previous 
    measurement, then return False. Else, return True

    """
    logger = \
        logging.getLogger('summer.sensorparsers.relevant_noise_measurement')
    prev_lat = float(prev_lat)
    prev_long = float(prev_long)
    cur_lat = float(cur_lat)
    cur_long = float(cur_long)
    sensor_propagation = float(sensor_propagation)

    gps_distance = utils.calc_gps_distance(prev_lat, prev_long, cur_lat, \
            cur_long)
    logger.debug("GPS distance between prev & current measurment: \
            %f"%(gps_distance))

    return gps_distance >= sensor_propagation


def exponential_moving_average(old_value, list_new_values):
    """
    This function calculates the exponential moving average for a list of 
    values
    :param old_value: The original/current value
    :type old_value: number
    :param list_new_values: A list containing all the incoming values over 
    which EMA is being calculated
    :type list_new_values: list
    """
    logger = \
    logging.getLogger('summer.sensorparsers.exponential_moving_average')
    logger.debug("Calculating exponential moving average")
    ALPHA = 0.25
    aggregated_value = old_value
    logger.debug("First value: %s"%(aggregated_value))
    for new_value in list_new_values:
        aggregated_value = new_value + ALPHA * (aggregated_value - new_value)
    logger.debug("Exponential Moving Average of list: %s"%(aggregated_value))
    return aggregated_value

