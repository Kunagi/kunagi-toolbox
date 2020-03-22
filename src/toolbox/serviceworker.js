
var BASE_URL = $BASE_URL;
var VERSION = "9.$VERSION";

var CACHING_DISABLED = $CACHING_DISABLED;

var PRE_CACHE = [
    // paths
    BASE_URL + '/ui/',
    BASE_URL + '/main.js',
    BASE_URL + '/manifest.json',
    BASE_URL + '/favicon.ico',
    BASE_URL + '/favicon.png',
    $PRE_CACHE
];

var NO_CACHE = [
    // regexex
    /serviceworker.js/,
    $NO_CACHE
];

var CACHE_FIRST = [
    // regexes
    $CACHE_FIRST
];

////////////////////////////////////////////////////////////////////////////

var NAME = 'serviceworker_cache_' + VERSION;

self.addEventListener('install', event => {
    console.log("SW install", event);
    if (CACHING_DISABLED) return;
    event.waitUntil(
        caches.open(NAME).then(cache => {
            return cache.addAll(PRE_CACHE);
        })
    );
});

function matchesAnyPattern(s, patterns) {
    var i;
    for (i = 0; i < patterns.length; i++) {
        if (s.match(patterns[i])) return true;
    }
    return false;
}

function strategyForUrl(url) {
    if (PRE_CACHE.indexOf(url) > -1) {
        return cacheElseNetwork;
    }

    if (matchesAnyPattern(url, CACHE_FIRST)) {
        return cacheElseNetwork;
    }

    return networkElseCache;
}

if (!CACHING_DISABLED) self.addEventListener('fetch', event => {
    if (event.request.method !== 'GET') return;

    if (!event.request.url.startsWith('https://')) return;

    /*
    if (event.request.url === BASE_URL + '/') {
        //console.log("SW fetch", event.request.url, "-> redirect to /ui/");
        event.respondWith(function () {
            return new Response('Server Request Failed', {
                status: 301,
                statusText: 'UI is at /ui/',
                headers: new Headers({
                    "Location": "/ui/"
                })
            });
        });
        return;
    }
    */

    // var path = event.request.url;
    // var sepIdx = path.indexOf('/', 9);

    // if (sepIdx > -1) {
    //     var baseUrl = path.substring(0, sepIdx);
    //     if (baseUrl === BASE_URL) path = path.substring(sepIdx);
    // }

    if (matchesAnyPattern(event.request.url, NO_CACHE)) {
        //console.log("SW fetch", event.request.url, "-> matches NO_CACHE");
        event.respondWith(fetch(event.request));
        return;
    }

    var f = strategyForUrl(event.request.url);
    //console.log("SW fetch", event.request.url, "->", f);
    event.respondWith(f(event.request));
});


function putToCache(request, response) {
    caches.open(NAME).then(function(cache) {
        cache.put(request, response);
    });
}

function fetchAndCache(request) {
    return fetch(request)
        .then(function(response) {
            if (response.ok) putToCache(request, response.clone());
            return response;
        }, offlineResponse)
        .catch(offlineResponse);
}

function cacheElseNetwork(request) {
    return caches.match(request)
        .then(function(responseFromCache) {
            if (responseFromCache) {
                // cache hit
                fetchAndCache(request);
                return responseFromCache;
            } else {
                // cache miss
                return fetchAndCache(request);
            }
        });
}

function networkElseCache(request) {
    return fetch(request)
        .then(function(responseFromRemote) {
            if (responseFromRemote.ok) putToCache(request, responseFromRemote.clone());
            return responseFromRemote;
        }, function() {return getFromCache(request);})
        .catch(function() {return getFromCache(request);});
}

function getFromCache(request) {
    return caches.match(request)
        .then(function(responseFromCache) {
            if (responseFromCache) {
                // cache hit
                return responseFromCache;
            } else {
                // cache miss
                return offlineResponse();
            }
        });
}

function offlineResponse() {
    return new Response('Server Request Failed', {
        status: 503,
        statusText: 'Service Unavailable',
        headers: new Headers({
            'Content-Type': 'text/html'
        })
    });
}


this.addEventListener('activate', function(event) {
    console.log("SW activate", event);
    if (CACHING_DISABLED) return;
    event.waitUntil(
        caches.keys().then(function(keyList) {
            return Promise.all(keyList.map(function(key) {
                if (key !== NAME) {
                    return caches.delete(key);
                }
                return null;
            }));
        })
    );
});
