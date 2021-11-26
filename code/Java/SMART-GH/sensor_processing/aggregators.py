"""
This module contains the aggregators for sensor data. When a new sensor is added, a 
aggregator must be added to calculate how the new data affects the historical
data already inside Redis. An aggregator must return a hash that can be stored
back into Redis.
"""
# std libs
import collections
import logging
import os

# third-party libs

# our libs
import summerlogger
import utils

def noop_aggregator(sensor_name, newly_parsed_hash, historical_data_hash=None):
    """
    This is the default aggregator that is invoked for sensors that do not have
    any other aggregators. It does no calculations and simply returns the
    newly_parsed_hash as the resulting hash to be stored

    :param newly_parsed_hash: The hash that has just been created by the
    sensorparser module
    :type newly_parsed_hash: hash
    :param historical_data_hash: The hash that has been retrieved from Redis
    containing previous readings from the sensor
    :returns: newly_parsed_hash
    """
    logger = logging.getLogger('summer.aggregator.noop_aggregator')
    logger.debug("No-Op aggregator. Returning newly_parsed_hash")
    return newly_parsed_hash

def exponential_weighted_moving_average(sensor_name, newly_parsed_hash, \
        historical_data_hash):
    """
    This implements the Exponential Weighted Moving Average filter, the most
    common filter for sensor readings. Setting alpha to 0 makes the filter not
    do any filtering, while setting alpha to 1 makes it completely insensitive
    to current data. Between 0.2 and 0.3 seems to be generally recommended in
    literature.

    :param sensor_name: The name of the sensor that is being aggregated
    :type sensor_name: string
    :param newly_parsed_hash: The hash that has just been created by the
    sensorparser module
    :type newly_parsed_hash: hash
    :param historical_data_hash: The hash that has been retrieved from Redis
    containing previous readings from the sensor
    :returns: newly_parsed_hash
    """
    ALPHA = 0.25

    logger = logging.getLogger('summer.aggregator.ewma')
    if len(historical_data_hash) == 0:
        logger.debug("No historical data present. Returning new hash")
        return newly_parsed_hash
    else:
        logger.debug(historical_data_hash)
        # Do a sanity check to see if current data is newer than historical
        historical_date = historical_data_hash["timestamp"]
        current_date = newly_parsed_hash["timestamp"]
        if utils.is_date_older(historical_date, current_date):
            # continue processing
            new_sensor_value = float(newly_parsed_hash["value"])
            logger.debug("new sensor value: %s"%(new_sensor_value))
            historical_value = float(historical_data_hash["value"])
            logger.debug("historical sensor value: %s"%(historical_value))
            aggregated_value = new_sensor_value + ALPHA * (historical_value - \
                    new_sensor_value)
            logger.debug("Aggregated sensor value: %s"%(aggregated_value))
            newly_parsed_hash["value"] = aggregated_value
            newly_parsed_hash["timestamp"] = current_date
            return newly_parsed_hash
        else:
            # The newly_parsed_hash contains old sensor data. Ignore it and 
            # return historical_data_hash
            logger.debug("Somehow parsed old sensor data. Retaining historical \
                    data")
            return historical_data_hash
        



