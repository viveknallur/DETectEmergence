#import stdlib
import logging
import logging.handlers
import tempfile
import os

# import our code
import constants

LOGFILE = constants.getLoggingConstants('LOGFILE')
MAXLOGSIZE = constants.getLoggingConstants('MAX_LOG_SIZE')
BACKUPCOUNT = constants.getLoggingConstants('BACKUPCOUNT')


# set up logging to file 
LOGFILE = ''.join([tempfile.gettempdir(), os.path.sep, LOGFILE])
logging.basicConfig(level=logging.DEBUG,
                    format='%(asctime)s %(name)-12s %(levelname)-8s %(message)s',
                    datefmt='%d-%m-%y %H:%M',
                    filename=LOGFILE,
                    filemode='w')

# define a Handler which writes INFO messages or higher to the sys.stderr
console = logging.StreamHandler()
console.setLevel(logging.INFO)

# set a format which is simpler for console use
formatter = logging.Formatter('%(name)-12s: %(levelname)-8s %(message)s')

# tell the handler to use this format
console.setFormatter(formatter)
# add the handler to the root logger
logging.getLogger('').addHandler(console)

# Indicate on console where the app_logger is logging to
logging.getLogger('').info("Logging to file: %s"%(LOGFILE,))

app_logger = logging.getLogger('summer')
app_logger.setLevel(logging.DEBUG)

handler = logging.handlers.RotatingFileHandler(
                LOGFILE, maxBytes=MAXLOGSIZE, backupCount=BACKUPCOUNT)

app_logger.addHandler(handler)
