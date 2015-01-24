(ns clj-sockets.core
  (:require [clojure.core.typed :as t :refer [ann defn fn]]
            [clojure.java.io :refer [writer reader]])
  (:refer-clojure :exclude [defn fn read-line])
  (:import (java.net Socket ServerSocket)
           (java.io BufferedWriter OutputStream InputStream BufferedReader)
           (clojure.lang Seqable)))


(defn create-socket [^String hostname :- String
                     ^Integer port :- Integer] :- Socket
  (Socket. hostname port))

(ann ^:no-check clojure.java.io/writer [Socket -> BufferedWriter])
(ann ^:no-check clojure.java.io/reader [Socket -> BufferedReader])
(ann ^:no-check clojure.core/line-seq [BufferedReader -> (Seqable String)])

(defn close-socket [^Socket socket :- Socket] :- nil
  (.close socket))

(defn write-to-buffer [^BufferedWriter output-stream :- BufferedWriter
                       ^String string :- String] :- nil
  (.write output-stream string)
  (.flush output-stream))

(ann write-to [Socket String -> nil])
(defn write-to [socket message]
  (write-to-buffer (writer socket) message))

(ann write-line [Socket String -> nil])
(defn write-line [socket message]
  (write-to socket (str message "\n")))

; this is memoize so that we always get the same reader for
; a given socket. otherwise the temporary readers could have text
; loaded into their buffers and then never used
(ann get-reader [Socket -> BufferedReader])
(def get-reader
  (memoize (fn [^Socket socket :- Socket] :- BufferedReader
             (reader socket))))

(ann read-char [Socket -> Character])
(defn read-char [socket]
  (let [read-from-buffer (fn [^BufferedReader input-stream :- BufferedReader] :- Integer
                           (.read input-stream))]
    (-> socket
        get-reader
        read-from-buffer
        char)))

(ann read-lines [Socket -> (Seqable String)])
(defn read-lines [socket]
  (line-seq (get-reader socket)))

(t/non-nil-return java.io.BufferedReader/readLine :all)
(ann read-line [Socket -> String])
(defn read-line
  "Read a line of textual data from the given socket"
  [^Socket socket]
  (let [read-line-from-reader (fn [^BufferedReader reader :- BufferedReader] :- String
                                (.readLine reader))]
    (read-line-from-reader (get-reader socket))))


(defn create-server [^Integer port :- Integer] :- ServerSocket
  (ServerSocket. port))

(defn connected? [^Socket socket :- Socket] :- Boolean
  (.isConnected socket))

(t/non-nil-return java.net.ServerSocket/accept :all)
(defn listen [^ServerSocket server-socket :- ServerSocket] :- Socket
  "Waits for a connection to come through, then returns a now-connected Socket"
  {:post connected?}
  (.accept server-socket))
