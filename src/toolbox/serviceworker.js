// https://gist.github.com/ngokevin/7eb03d90987c0ed03b873530c3b4c53c

var VERSION = $VERSION;

var SERVICEWORKER_PATH = 'serviceworker.js';

var PRE_CACHE = [
    // paths
    'ui/',
    'main.js',
    'manifest.json',
    'favicon.ico',
    'favicon.png',
    $PRE_CACHE
];

var NO_CACHE = [
    // regexex
    SERVICEWORKER_PATH,
    $NO_CACHE
];

var CACHE_FIRST = [
    // regexes
    $CACHE_FIRST
];

////////////////////////////////////////////////////////////////////////////

var NAME = 'swcache_' + VERSION;

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

function strategyForUrl(url) {
    if (PRE_CACHE.indexOf(url) !== -1) {
        return cacheElseNetwork;
    }

    if (matchesAnyPattern(url, CACHE_FIRST)) {
        return cacheElseNetwork;
    }

    return networkElseCache;
}

self.addEventListener('fetch', event => {
    if (event.request.method !== 'GET') {
        // NOP
    } else if (matchesAnyPattern(event.request.url, NO_CACHE)) {
        console.log("SW fetch", event.request.url, "-> NO_CACHE");
        event.respondWith(fetch(event.request));
    } else {
        var f = strategyForUrl(event.request.url);
        console.log("SW fetch", event.request.url, "->", f);
        event.respondWith(f(event));
    }
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
            return response;
        }) || response;
    });
}
