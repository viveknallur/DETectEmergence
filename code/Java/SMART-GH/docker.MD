docker run --name some-redis -d redis
#If you want a prepoluated databased
#docker run --name some-redis -d barais/redissmartgh
docker run -t --link some-redis:redis  -i barais/smartgh python /opt/SMART-GH/sensor_processing/process_sensor_data_daemon.py
docker run -t --link some-redis:redis -p 8989:8989 -i barais/smartgh /opt/SMART-GH/graphhopper.sh web /opt/SMART-GH/maps/dublin-m50.osm


