(ns juxt.warp.request-test
  (:require
   [clojure.java.io :as io]
   [clojure.test :refer [deftest is testing]]
   [ring.mock.request :as mock]
   [juxt.warp.request :refer [handler]]
   [juxt.warp.yaml :as yaml]))

(defn call-handler-sync [handler request]
  (deref
   (let [p (promise)]
     (handler
      request
      (fn [response] (deliver p response))
      (fn [err] (deliver p err)))
     p)))

(defn get-property [propname cb]
  (future
    (Thread/sleep 100)
    (cb "dave")))

#_@(let [api (yaml/parse-string (slurp (io/resource "juxt/warp/openapi-examples/petstore-expanded.yaml")))
      h (handler api {:properties-fn get-property})]
  (let [result (call-handler-sync h (mock/request :get "http://example.org"))]
    result))

(deftest responds-with-404-test
  (let [api (yaml/parse-string (slurp (io/resource "juxt/warp/openapi-examples/petstore-expanded.yaml")))
        h (handler api {})]
    (is (= 404 (:status (call-handler-sync h (mock/request :get "http://example.org")))))
    (is (= 404 (:status (call-handler-sync h (mock/request :get "http://petstore.swagger.io/api/dummy")))))))

(deftest responds-with-405-test
  (testing "DELETE method is not allowed resulting in a 405 response"
    (let [api (yaml/parse-string (slurp (io/resource "juxt/warp/openapi-examples/petstore-expanded.yaml")))
          h (handler api {})
          req (mock/request :delete "http://petstore.swagger.io/api/pets")]
      (is (= 405 (:status (call-handler-sync h req)))))))

(deftest responds-with-406-test
  (let [api (yaml/parse-string (slurp (io/resource "juxt/warp/openapi-examples/petstore-expanded.yaml")))
        h (handler api {})]
    (is (= 406 (:status (call-handler-sync
                         h (-> (mock/request :get "http://petstore.swagger.io/api/pets")
                               (mock/header "accept" "application/yaml"))))))))


;; have a properties 'database' that is slow, and has a callback API
