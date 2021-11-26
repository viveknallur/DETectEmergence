// IE fix
if (!window.console) {
    var console = {
        log: function() {
        },
        warn: function() {
        },
        error: function() {
        },
        time: function() {
        },
        timeEnd: function() {
        }
    };
}

GHRequest = function(host) {
    this.min_path_precision = 1;
    this.host = host;
    this.from = new GHInput("");
    this.to = new GHInput("");
    this.vehicle = "car";
    this.weighting = "fastest";
    this.points_encoded = true;
    this.instructions = true;
    this.elevation = false;
    this.features = {};
    this.debug = false;
    this.locale = "en";
    this.do_zoom = true;
    // use jsonp here if host allows CORS
    this.dataType = "json";
    this.key = "K1KVyGYve5choCJAbyejstj5Ho0dEa6RbBnwHbSw";
};

GHRequest.prototype.init = function(params) {
    //    for(var key in params) {
    //        var val = params[key];
    //        if(val === "false")
    //            val = false;
    //        else if(val === "true")
    //            val = true;
    //        else {            
    //            if(parseFloat(val) != NaN)
    //                val = parseFloat(val)
    //        }
    //        this[key] = val;
    //    } 
    if (params.minPathPrecision)
        this.minPathPrecision = params.minPathPrecision;
    if (params.vehicle)
        this.vehicle = params.vehicle;
    if (params.weighting)
        this.weighting = params.weighting;
    if (params.algorithm)
        this.algorithm = params.algorithm;
    if (params.locale)
        this.locale = params.locale;
    //@Amal ELgammal: commented to take the elevation value based on the user selection
    //if (params.elevation)
    this.elevation = params.elevation;
    //
    if ('do_zoom' in params)
        this.do_zoom = params.do_zoom;
    if ('instructions' in params)
        this.instructions = params.instructions;
    if ('points_encoded' in params)
        this.points_encoded = params.points_encoded;

    //@Amal Elgammal commented
    //this.elevation = false;

    var featureSet = this.features[this.vehicle];
    console.log("featureSet.tostring() = " + featureSet.toString());
    console.log("this.elevation in ghrequest.init = " + this.elevation);

    if (featureSet && featureSet.elevation) {
        if ('elevation' in params)
        {
            this.elevation = params.elevation;
            //Amal
            console.log("feature set=true and elevation is in params and this.elevation =" + this.elevation);
        }
        else
            this.elevation = false;
    }

    if (params.q) {
        var qStr = params.q;
        if (!params.point)
            params.point = [];
        var indexFrom = qStr.indexOf("from:");
        var indexTo = qStr.indexOf("to:");
        if (indexFrom >= 0 && indexTo >= 0) {
            // google-alike query schema            
            if (indexFrom < indexTo) {
                params.point.push(qStr.substring(indexFrom + 5, indexTo).trim());
                params.point.push(qStr.substring(indexTo + 3).trim());
            } else {
                params.point.push(qStr.substring(indexTo + 3, indexFrom).trim());
                params.point.push(qStr.substring(indexFrom + 5).trim());
            }
        } else {
            var points = qStr.split("p:");
            for (var i = 0; i < points.length; i++) {
                var str = points[i].trim();
                if (str.length === 0)
                    continue;

                params.point.push(str);
            }
        }
    }
};

GHRequest.prototype.initVehicle = function(vehicle) {
    this.vehicle = vehicle;
    var featureSet = this.features[this.vehicle];
    if (featureSet && featureSet.elevation)
        this.elevation = true;
    else
        this.elevation = false;
};

GHRequest.prototype.hasElevation = function() {
    console.log("hasEleveation function returns this.elevation = " + this.elevation);
    return this.elevation;
};

GHRequest.prototype.createGeocodeURL = function(host) {
    var tmpHost = this.host;
    if (host)
        tmpHost = host;
    return this.createPath(tmpHost + "/geocode?limit=8&type=" + this.dataType + "&key=" + this.key);
};

GHRequest.prototype.createURL = function(demoUrl) {
    return this.createPath(this.host + "/route?" + demoUrl + "&type=" + this.dataType + "&key=" + this.key);
};

GHRequest.prototype.createGPXURL = function() {
    // use points instead of strings
    var str = "point=" + encodeURIComponent(this.from.toString()) + "&point=" + encodeURIComponent(this.to.toString());
    return this.createPath(this.host + "/route?" + str + "&type=gpx&key=" + this.key);
};

GHRequest.prototype.createFullURL = function() {
    var str = "?point=" + encodeURIComponent(this.from.input) + "&point=" + encodeURIComponent(this.to.input);
    return this.createPath(str);
};

GHRequest.prototype.createPath = function(url) {

    //@Amal ELgammal: commented to include vechile in the URl anyway
    if (this.vehicle/* && this.vehicle !== "car"*/)
        url += "&vehicle=" + this.vehicle;

    // fastest or shortest
    //@Amal ELgammal: commented to include weighting in the URL anyway
    if (this.weighting /*&& this.weighting !== "fastest"*/)
        url += "&weighting=" + this.weighting;
    if (this.locale && this.locale !== "en")
        url += "&locale=" + this.locale;
    // dijkstra, dijkstrabi, astar, astarbi
    if (this.algorithm && this.algorithm !== "dijkstrabi")
        url += "&algorithm=" + this.algorithm;
    if (this.min_path_precision !== 1)
        url += "&min_path_precision=" + this.min_path_precision;
    if (!this.instructions)
        url += "&instructions=false";
    if (!this.points_encoded)
        url += "&points_encoded=false";

    //@Amal Elgammal: Add Elevation anyway in the request
    //url += "&elevation="+this.elevation;
    if (this.elevation)
        url += "&elevation=true";


    if (this.debug)
        url += "&debug=true";
    return url;
};

function decodePath(encoded, is3D) {
    // var start = new Date().getTime();
    var len = encoded.length;
    var index = 0;
    var array = [];
    var lat = 0;
    var lng = 0;
    var ele = 0;

    while (index < len) {
        var b;
        var shift = 0;
        var result = 0;
        do {
            b = encoded.charCodeAt(index++) - 63;
            result |= (b & 0x1f) << shift;
            shift += 5;
        } while (b >= 0x20);
        var deltaLat = ((result & 1) ? ~(result >> 1) : (result >> 1));
        lat += deltaLat;

        shift = 0;
        result = 0;
        do {
            b = encoded.charCodeAt(index++) - 63;
            result |= (b & 0x1f) << shift;
            shift += 5;
        } while (b >= 0x20);
        var deltaLon = ((result & 1) ? ~(result >> 1) : (result >> 1));
        lng += deltaLon;

        if (is3D) {
            // elevation
            shift = 0;
            result = 0;
            do
            {
                b = encoded.charCodeAt(index++) - 63;
                result |= (b & 0x1f) << shift;
                shift += 5;
            } while (b >= 0x20);
            var deltaEle = ((result & 1) ? ~(result >> 1) : (result >> 1));
            ele += deltaEle;
            array.push([lng * 1e-5, lat * 1e-5, ele / 100]);
        } else
            array.push([lng * 1e-5, lat * 1e-5]);
    }
    // var end = new Date().getTime();
    // console.log("decoded " + len + " coordinates in " + ((end - start) / 1000) + "s");
    return array;
}

GHRequest.prototype.doRequest = function(url, callback) {
    var that = this;
    $.ajax({
        //@Amal Elgammal: commented to increarse the timeout as leastnoisy calculations takes longer
        //"timeout": 30000,
        "timeout": 60000,
        "url": url,
        "success": function(json) {
            if (json.paths) {
                for (var i = 0; i < json.paths.length; i++) {
                    var path = json.paths[i];
                    // convert encoded polyline to geo json
                    if (path.points_encoded) {
                        var tmpArray = decodePath(path.points, that.hasElevation());
                        path.points = {
                            "type": "LineString",
                            "coordinates": tmpArray
                        };
                    }
                }
            }
            callback(json);
        },
        "error": function(err) {
            // problematic: this callback is not invoked when using JSONP!
            // http://stackoverflow.com/questions/19035557/jsonp-request-error-handling
            var msg = "API did not respond! ";
            if (err && err.statusText && err.statusText !== "OK")
                msg += err.statusText;

            console.log(msg + " " + JSON.stringify(err));
            var details = "Error for " + url;
            var json = {
                "info": {
                    "errors": [{
                            "message": msg,
                            "details": details
                        }]
                }
            };
            callback(json);
        },
        "type": "GET",
        "dataType": this.dataType
    });
};

GHRequest.prototype.getInfo = function() {
    var url = this.host + "/info?type=" + this.dataType + "&key=" + this.key;
    console.log("url created inside getInfo" + url);
    return $.ajax({
        "url": url,
        "timeout": 3000,
        "type": "GET",
        "dataType": this.dataType
    });
};

GHInput = function(str) {
    // either text or coordinates
    this.input = str;
    try {
        var index = str.indexOf(",");
        if (index >= 0) {
            this.lat = round(parseFloat(str.substr(0, index)));
            this.lng = round(parseFloat(str.substr(index + 1)));
            if (!isNaN(this.lat) && !isNaN(this.lng)) {
                this.input = this.toString();
            } else {
                this.lat = undefined;
                this.lng = undefined;
            }
        }
    } catch (ex) {
    }
};

GHInput.prototype.isResolved = function() {
    return this.lat && this.lng;
};

GHInput.prototype.setCoord = function(lat, lng) {
    this.lat = round(lat);
    this.lng = round(lng);
    this.input = this.lat + "," + this.lng;
};

GHInput.prototype.toString = function() {
    if (this.lat !== undefined && this.lng !== undefined)
        return this.lat + "," + this.lng;
    return undefined;
};

GHRequest.prototype.setLocale = function(locale) {
    if (locale)
        this.locale = locale;
};

GHRequest.prototype.fetchTranslationMap = function(urlLocaleParam) {
    if (!urlLocaleParam)
        // let servlet figure out the locale from the Accept-Language header
        urlLocaleParam = "";
    var url = this.host + "/i18n/" + urlLocaleParam + "?type=" + this.dataType + "&key=" + this.key;
    console.log("URL created inside fetchTranslationMap =" + url);
    return $.ajax({
        "url": url,
        "timeout": 3000,
        "type": "GET",
        "dataType": this.dataType
    });
};