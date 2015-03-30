package com.cm4j.test.groovy

import groovyx.net.http.HTTPBuilder
import groovyx.net.http.Method
import net.sf.json.JSON

def http() {
    def http = new HTTPBuilder('http://127.0.0.1:8009')

    // perform a GET request, expecting JSON response data
    http.request(Method.POST, JSON) {
        uri.path = '/game/interface/oper.do'
        uri.query = [g_m: 'gmAdmin_bojoy_global', m: 'reloadDict', t: -1]

        // response handler for a success response code:
        response.success = { resp, json ->
            println resp.status

            // parse the JSON response object:
            json.responseData.results.each {
                println it
            }
        }

        // handler for any failure status code:
        response.failure = { resp ->
            println "Unexpected error: ${resp.status} : ${resp.statusLine.reasonPhrase}"
        }
    }
}

http()