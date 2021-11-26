# std libs
from datetime import datetime
import logging
import json
import math
import urllib2

# third-party
from decorators import retry

# our libs
import summerlogger

def get_relevant_streets(latitude, longitude, propagation_value=None):
    """
    This function is responsible for reverse-geocoding from a lat/long
    combination and creating a set of relevant_streets. By relevant streets, we     
    mean the way-id given in the OSM file.

    :param lattitude: The latitude of the sensor reading
    :type latitude: string
    :param longitude: The longitude of the sensor reading
    :type longitude: string
    :param propagation_value: The number of metres upto which the sensor's
    readings must be propagated
    :type sensor_name: string
    :returns: A set of all streets within propagation distance of the sensor, in 
    the form of openstreetmap's way-id
    """
    logger = logging.getLogger('summer.utils.get_relevant_streets')
    prefix_url = \
    'http://services.gisgraphy.com/street/streetsearch?format=json'
    lat_url = '&lat='
    long_url = '&lng='

    full_url = ''.join([prefix_url,lat_url, str(latitude), long_url, str(longitude)])
    logger.debug("Connecting to: %s"%(full_url))
    relevant_streets = set()
    try:
        geo_response = urlopen_with_retry(full_url)
    except urllib2.URLError as ue:
        logger.warn("Could not get reverse geo-coded info. Giving up for this location")
    else:
            if geo_response.code == 200:
                logger.debug("Received reverse geocoding information")
                json_geo = json.loads(geo_response.read())
                num_results = int(json_geo[u'numFound'])
                logger.info ("Number of results retrieved: %d"%(num_results,))
                all_streets = json_geo[u'result']
                for street in all_streets:
                    street_distance = int(street[u'distance'])
                    if street_distance > propagation_value:
                        continue
                    else:
                        try:
                            relevant_streets.add(street[u'name'].__str__())
                            logger.debug("For openstreetmapid:%s"%(street[u'openstreetmapId']))
                            logger.debug("Found street:%s"%(street[u'name']))
                        except KeyError:
                            logger.debug("No name found for openstreetmapId:%s"%(street[u'openstreetmapId']))
                            continue
                        
            else:
                logger.warn("Could not retrieve reverse geocoding information")

    return relevant_streets

def calc_gps_distance(lat1, long1, lat2, long2):
    """
    All calculations need to be done in radians, instead of degrees. Since most 
    GPS coordinates tend to use degrees, we convert to radians first, and then 
    use the Haversine formula. The Haversine formula gives the shortest 
    great-circle distance between any two points, i.e. as-the-crow-flies 
    distance using a reasonably focussed crow

    WARNING: The calculation is done in Kilometres. But, at the street level,
    kilometres is not useful. So, we convert to metres and return!

    >>> calc_gps_distance(53.34376885732333,-6.240988668839767,53.34376349, \
            -6.24099402)
    0000.6945396560484981

    >>> calc_gps_distance(53.34376885732333,-6.240988668839767,0,0)
    5959609.740337647

    >>> calc_gps_distance(90,0,0,0)
    10007543.398

    """
    radius_of_earth = 6371 # in Kilometres

    delta_latitude = math.radians(lat2 - lat1)
    delta_longitude = math.radians(long2 - long1)
    rad_lat1 = math.radians(lat1)
    rad_lat2 = math.radians(lat2)

    a = math.sin(delta_latitude / 2) * math.sin(delta_latitude / 2) + \
        math.cos(rad_lat1) * math.cos(rad_lat2) * math.sin(delta_longitude / 2) \
        * math.sin(delta_longitude / 2)

    c = 2 * math.atan2(math.sqrt(a), math.sqrt(1-a))

    distance = radius_of_earth * c
    distance_in_metres = distance * 1000
    return distance_in_metres


@retry(urllib2.URLError, tries=5, delay=7, backoff=2)
def urlopen_with_retry(full_url):
    """
    This function exists because we're using a free reverse geo-coding service,
    and it tends to throw us off, if we try too many times. We use an
    exponential backoff mechanism (7, 14, 28...) to wait a little bit and then
    try again. The ideal solution is to run our own reverse geocoding service
    """
    return urllib2.urlopen(full_url)

def is_date_older(date1_str, date2_str):
    """
    Returns true if date1_str is older than date2_str

    :param date1_str: The first date
    :type date1_str: string
    :param date2_str: The second date
    :type date2_str: string
    :returns: boolean
    """
    first_date = datetime.strptime(date1_str, "%Y-%m-%dT%H:%M:%SZ")
    second_date = datetime.strptime(date2_str, "%Y-%m-%dT%H:%M:%SZ")
    return first_date <= second_date
