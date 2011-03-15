(ns serial-port
  (:import
   (gnu.io CommPortIdentifier
           SerialPort
           SerialPortEventListener
           SerialPortEvent)
   (java.io OutputStream
            InputStream)))

(def PORT-OPEN-TIMEOUT 2000)
(defrecord Port [path raw-port out-stream in-stream])

(defn port-ids
  "Returns a seq representing all port identifiers visible to the system"
  []
  (enumeration-seq (CommPortIdentifier/getPortIdentifiers)))

(defn port-at
  "Returns the name of the serial port at idx."
  [idx]
  (.getName (nth (port-ids) idx)))

(defn list-ports
  "Print out the available ports with an index number for future reference
  with (port-at <i>)."
  []
  (loop [ports (port-ids)
         idx 0]
    (when ports
      (println idx ":" (.getName (first ports)))
      (recur (next ports) (inc idx)))))

(defn close
  "Closes an open port."
  [port]
  (let [raw-port (:raw-port port)]
    (.removeEventListener raw-port)
    (.close raw-port)))

(defn open
  "Returns an opened serial port. Allows you to specify the baud-rate.
  (open \"/dev/ttyUSB0\")
  (open \"/dev/ttyUSB0\" 9200)"
  ([path] (open path 115200))
  ([path baud-rate]
     (try
       (let [uuid     (.toString (java.util.UUID/randomUUID))
             port-id  (first (filter #(= path (.getName %)) (port-ids)))
             raw-port (.open port-id uuid PORT-OPEN-TIMEOUT)
             out      (.getOutputStream raw-port)
             in       (.getInputStream  raw-port)
             _        (.setSerialPortParams raw-port baud-rate
                                            SerialPort/DATABITS_8
                                            SerialPort/STOPBITS_1
                                            SerialPort/PARITY_NONE)]

         (Port. path raw-port out in))
       (catch Exception e
         (throw (Exception. (str "Sorry, couldn't connect to the port with path " path )))))))

(defn write
  "Write a byte array to a port"
  [port bytes]
  (.write ^OutputStream (:out-stream port) ^bytes bytes))

(defn- compose-byte-array [bytes]
  (byte-array (count bytes) (map #(.byteValue ^Integer %) bytes)))

(defn write-int-seq
  "Write a seq of Integers as bytes to the port."
  [port ints]
  (write port (compose-byte-array ints)))

(defn write-int
  "Write an Integer as a byte to the port."
  [port int]
  (write port (compose-byte-array (list int))))

(defn listen
  "Register a function to be called for every byte received on the specified port."
  ([port handler] (listen port handler true))
  ([port handler skip-buffered]
     (let [raw-port  (:raw-port port)
           in-stream (:in-stream port)
           listener  (reify SerialPortEventListener
                       (serialEvent [_ event] (when (= SerialPortEvent/DATA_AVAILABLE (.getEventType event))
                                                (handler in-stream))))]

       (if skip-buffered
         (let [to-drop (.available in-stream)]
           (.skip in-stream to-drop)))

       (.addEventListener raw-port listener)
       (.notifyOnDataAvailable raw-port true))))

(defn remove-listener
  "De-register the listening fn for the specified port"
  [port]
  (.removeEventListener (:raw-port port)))

(defn on-n-bytes
  "Partitions the incoming byte stream into seqs of size n and calls handler passing each partition."
  ([port n handler] (on-n-bytes port n handler true))
  ([port n handler skip-buffered]
     (listen port (fn [^InputStream in-stream]
                    (if (>= (.available in-stream) n)
                      (handler (doall (repeatedly n #(.read in-stream))))))
             skip-buffered)))

(defn on-byte
  "Calls handler for each byte received"
  ([port handler] (on-byte port handler true))
  ([port handler skip-buffered]
     (listen port (fn [^InputStream in-stream]
                    (handler (.read in-stream)))
             skip-buffered)))

