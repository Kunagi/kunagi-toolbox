// https://gist.github.com/ngokevin/7eb03d90987c0ed03b873530c3b4c53c

var BASE_URL = $BASE_URL;
var VERSION = $VERSION;

var PRE_CACHE = [
    // paths
    '/ui/',
    '/main.js',
    '/manifest.json',
    '/favicon.ico',
    '/favicon.png',
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

function strategyForPath(path) {
    if (PRE_CACHE.indexOf(path) > -1) {
        return cacheElseNetwork;
    }

    if (matchesAnyPattern(path, CACHE_FIRST)) {
        return cacheElseNetwork;
    }

    return networkElseCache;
}

self.addEventListener('fetch', event => {
    if (event.request.method !== 'GET') return;

    var path = event.request.url;
    var sepIdx = path.indexOf('/', 9);

    if (sepIdx > -1) {
        var baseUrl = path.substring(0, sepIdx);
        if (baseUrl === BASE_URL) path = path.substring(sepIdx);
    }

    if (matchesAnyPattern(path, NO_CACHE)) {
        console.log("SW fetch", path, "-> matches NO_CACHE");
        event.respondWith(fetch(event.request));
        return;
    }

    var f = strategyForPath(path);
    console.log("SW fetch", path, "->", f);
    event.respondWith(f(event));
});


// If cache else network.
// For images and assets that are not critical to be fully up-to-date.
// developers.google.com/web/fundamentals/instant-and-offline/offline-cookbook/
// #cache-falling-back-to-network
function cacheElseNetwork(event) {
    return caches.match(event.request).then(response => {
        function fetchAndCache() {
            return fetch(event.request).then(response => {
                // Update cache.
                caches.open(NAME).then(cache => cache.put(event.request, response.clone()));
                return response.clone();
            });
        }

        // If not exist in cache, fetch.
        if (!response) {
            return fetchAndCache();
        }

        // If exists in cache, return from cache while updating cache in background.
        fetchAndCache();
        return response;
    });
}

// If network else cache.
// For assets we prefer to be up-to-date (i.e., JavaScript file).
function networkElseCache(event) {
    return caches.match(event.request).then(match => {
        if (!match) {
            return fetch(event.request);
        }
        return fetch(event.request).then(response => {
            // Update cache.
            caches.open(NAME).then(cache => cache.put(event.request, response.clone()));
            return response.clone();
        }) || response;
    });
}
