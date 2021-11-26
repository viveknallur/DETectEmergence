"""
This contains all the constants needed for the daemons to run
"""

LOGGING_CONSTANTS = {
        'LOGFILE' : 'summer.log',
        'MAX_LOG_SIZE' : 1048576, # 1 MEG
        'BACKUP_COUNT' : 5
}
def getLoggingConstants(constant):
    """
    Returns various constants needing by the logging module
    """
    return LOGGING_CONSTANTS.get(constant, False)
